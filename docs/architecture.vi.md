# SourceGuard: Kiến trúc build chống lộ source khi compiler bị RCE

> Phiên bản: 0.2 (draft để code). Ngày: 2026-11-15
> Trạng thái: thiết kế và giả thuyết nghiên cứu. Các tính chất "bất biến output" ở mục 5 **chưa được kiểm chứng**. Phase 1 tồn tại để kiểm chứng chúng.

---

## 1. Mục tiêu và phạm vi

### 1.1 Mục tiêu bảo mật

**G1.** Kẻ tấn công thực thi mã tùy ý **bên trong compiler** khi build Project A không mang được ra ngoài thông tin nào của source A, ngoài phần thông tin **bất biến qua mọi biến thể** (xem mục 5.3).

**G2.** Forgejo, CI orchestrator, DB và storage bị chiếm hoàn toàn cũng không lộ plaintext.

**G3.** Mọi artifact được phát hành đều chứng minh được nguồn gốc (provenance) và đã qua kiểm tra chênh lệch giữa các cell.

### 1.2 Ngoài phạm vi (ghi rõ thành giới hạn)

| Không bảo vệ | Lý do |
|---|---|
| Máy developer bị chiếm | Plaintext gốc nằm ở đó |
| Variant Generator hoặc Key Broker bị chiếm | Đây là TCB |
| Side-channel ngoài artifact: timing, pass/fail | Chỉ giảm được băng thông (mục 7), không loại bỏ |
| Thông tin bất biến (logic sau chuẩn hóa) | Theo định nghĩa, nó đi vào binary |
| Backdoor nằm trong artifact, chạy lúc runtime | Thuộc bài toán supply chain integrity, không phải build confidentiality |
| Tất cả cell dùng chung một implant **không phụ thuộc source** | Không gây lộ source, nhưng phải phát hiện bằng DDC hoặc diverse toolchain |

### 1.3 Ý tưởng cốt lõi (đóng góp mới)

So hash nhiều lần build **cùng một source** không bắt được RCE đến từ input: mọi cell chạy cùng exploit trên cùng dữ liệu, nhúng cùng nội dung, nên hash vẫn trùng.

**Differential Source Variants (DSV):** mỗi build cell nhận một **biến thể source khác nhau về chữ nhưng tương đương về ngữ nghĩa**, sao cho compiler trung thực cho ra artifact **giống từng bit**. Compiler bị chiếm mà nhúng bất kỳ thông tin nào phụ thuộc vào phần khác biệt sẽ làm artifact lệch nhau, và gateway chặn.

Như vậy reproducibility, vốn là công cụ của **integrity**, được dùng làm **bộ phát hiện rò rỉ cho confidentiality**.

---

## 2. Threat model

### 2.1 Tác nhân

- **A1, input attacker:** cài dependency độc hoặc file độc vào cây source để khai thác bug compiler, đạt RCE trong tiến trình compiler. Đây là kịch bản chính.
- **A2, toolchain attacker:** compiler binary hoặc tool đi kèm (ld, strip, ar) bị cài implant từ trước.
- **A3, infra attacker:** chiếm Forgejo, orchestrator, host OS hoặc storage.
- **A4, single-cell attacker:** chiếm riêng một build cell, ví dụ qua host của cell đó.

### 2.2 Khả năng của attacker trong cell

- Đọc toàn bộ biến thể source mà cell nhận được.
- Ghi tùy ý vào artifact, log và exit code của cell.
- Kiểm soát thời gian chạy.
- **Không** có mạng, **không** có ổ đĩa bền, **không** biết biến thể của cell khác.
- Có thể biết thuật toán sinh biến thể (Kerckhoffs). Seed thì không biết.

### 2.3 Giả định tin cậy (TCB)

1. Variant Generator (VG): nhỏ, làm việc ở mức token, không chạy code của project.
2. Key Broker (KB).
3. Artifact Gateway (AG).
4. Hypervisor hoặc sandbox của cell cách ly đúng: không mạng, không disk.
5. Release Signer.

---

## 3. Tổng quan kiến trúc

```
 Developer (trusted)
   │  sg-cli: mã hóa từng module bằng khóa riêng (age), ký commit
   ▼
 Forgejo (untrusted)  ── chỉ chứa ciphertext + manifest đã ký
   │
   ▼
 Orchestrator (untrusted) ── chỉ điều phối job, thấy ciphertext + metadata
   │
   ├──► Key Broker (trusted) ── cấp khóa module cho đúng job, đúng cell
   │
   ▼
 Variant Generator Cell (trusted, không mạng)
   │  giải mã module → sinh k biến thể (seed bí mật) → mã hóa từng biến thể
   │  bằng khóa riêng của từng build cell
   ▼
 ┌──────────────┐  ┌──────────────┐        ┌──────────────┐
 │ Build Cell 1 │  │ Build Cell 2 │  ...   │ Build Cell k │   (untrusted compiler)
 │ biến thể V1  │  │ biến thể V2  │        │ biến thể Vk  │   microVM dùng 1 lần
 └──────┬───────┘  └──────┬───────┘        └──────┬───────┘   no net, no disk
        └────────┬────────┴────────────────────────┘
                 ▼
 Artifact Gateway (trusted)
   │  so sánh từng byte artifact của k cell
   │  trùng hết → cho qua | lệch → chặn + báo động + lưu bằng chứng (đã mã hóa)
   │  log: chỉ giữ exit code + mã lỗi từ danh sách cho phép
   ▼
 Release Signer (trusted) ── ký provenance (in-toto/SLSA), không chạm source
```

Mặc định `k = 2`. Tùy chọn `k = 3` để giảm phần thông tin chung (mục 5.3).

---

## 4. Thành phần

### 4.1 `sg-cli` (máy developer)

- `sg init`: tạo cấu hình project, danh sách module, khóa developer.
- `sg seal`: mã hóa từng module bằng một data key riêng (age/X25519), sinh `manifest.json` đã ký.
- `sg push`: đẩy ciphertext và manifest lên Forgejo.
- `sg verify <artifact>`: kiểm tra provenance.

Định dạng lưu trên Forgejo:

```
/sg/manifest.json          # đã ký, không chứa plaintext
/sg/modules/<module_id>.age
/sg/interfaces/<module_id>.iface.age   # header/interface công khai giữa các module (tùy chọn)
```

`manifest.json`:

```json
{
  "project": "project-a",
  "commit": "<sha256 của tập ciphertext>",
  "modules": [
    {"id": "core", "ciphertext_sha256": "…", "plaintext_sha256": "…", "lang": "go"}
  ],
  "toolchain": {"image": "sg-go-1.23@sha256:…", "flags": ["-trimpath", "-buildvcs=false"]},
  "signature": "…"
}
```

Lưu ý: `plaintext_sha256` để VG kiểm tra sau khi giải mã. Hash của plaintext có entropy cao nên không làm lộ nội dung.

### 4.2 Key Broker (KB)

- Giữ data key của từng module, đã bọc bằng khóa của KB.
- API:
  - `POST /v1/jobs` (orchestrator): đăng ký job `{project, commit, module_id}` và nhận `job_id`.
  - `POST /v1/release-key` (chỉ VG gọi): `{job_id, attestation}` → data key của **một** module, dùng một lần.
  - `POST /v1/cell-keys` (VG gọi): sinh `k` cặp khóa ephemeral cho các build cell của job.
- Chính sách: một `job_id` chỉ giải mã được một module, TTL 10 phút, không cấp lại.
- **MVP:** attestation là token ngắn hạn cộng hash image. **Bản sau:** báo cáo AMD SEV-SNP hoặc Intel TDX.

### 4.3 Variant Generator (VG), thuộc TCB

Nguyên tắc: **càng nhỏ càng tốt, không parse sâu, không chạy code của project.**

- Input: plaintext module, seed bí mật (32 byte, KB sinh và chỉ VG thấy), `k`.
- Output: `k` biến thể, mỗi biến thể mã hóa bằng khóa của một cell.
- Ràng buộc bắt buộc: **giữ nguyên số dòng và vị trí mọi token có nghĩa.** Lý do: `__LINE__`, `assert`, pclntab của Go và line table đều đi vào binary.
- Ghi bản đồ biến đổi (đã mã hóa) để debug khi gateway báo lệch.

Danh sách phép biến đổi xem mục 5.

### 4.4 Build Cell

- MicroVM dùng một lần, ví dụ Firecracker. Trên VPS không có KVM, MVP dùng `bubblewrap` hoặc `podman --network=none --read-only` (yếu hơn, ghi rõ).
- Rootfs chỉ đọc, workspace là tmpfs, không mạng (`--network=none`, không có thiết bị NIC).
- Dependency được **vendor sẵn** và nằm trong biến thể hoặc image. Không có bước tải gì lúc build.
- Thời gian build cố định: cell chạy tối đa `T_max`, gateway luôn trả kết quả tại `T_max` (giảm timing channel).
- Output duy nhất: `artifact.bin`, `exit_code`, `error_class` (enum).
- Huỷ VM ngay sau khi xuất output.

### 4.5 Artifact Gateway (AG), thuộc TCB

```
inputs: {cell_i: (artifact_i, exit_i, error_class_i)} for i in 1..k
if any exit_i != exit_1 or any error_class_i != error_class_1:  → REJECT(divergent_status)
if exit_1 != 0:                                              → FAIL(error_class_1)   # không trả log
if any sha256(artifact_i) != sha256(artifact_1):             → REJECT(divergent_artifact) + ALERT
else                                                         → ACCEPT(artifact_1)
```

- **Không chuyển tiếp log thô** của compiler ra ngoài. Developer muốn xem lỗi thì tự build lại trên máy mình.
- Rate limit số build FAIL cho mỗi project (mục 7.2).
- Khi REJECT: lưu artifact của các cell **đã mã hóa bằng khóa developer** để điều tra.

### 4.6 Release Signer

- Nhận artifact đã ACCEPT, ký provenance (in-toto attestation, SLSA v1): commit, manifest, image toolchain, `k`, hash artifact, kết quả gateway.
- Không bao giờ nhận source.

### 4.7 Orchestrator

- Có thể dùng Forgejo Actions hoặc Woodpecker. Nó chỉ chuyển ciphertext và gọi API, coi như **untrusted hoàn toàn**.

---

## 5. Differential Source Variants (DSV)

### 5.1 Phép biến đổi ứng viên

Tất cả phải **giữ nguyên số dòng**. Cột "Kỳ vọng" là giả thuyết, Phase 1 phải đo.

| ID | Phép biến đổi | Ví dụ | Kỳ vọng output giống bit | Điều kiện |
|---|---|---|---|---|
| T1 | Thay nội dung comment, giữ số dòng | `// tính phí` → `// q8Zk1…` | Có | Không có doc-comment hay directive đi vào binary (`//go:embed`, `//go:generate`, Doxygen) |
| T2 | Đổi khoảng trắng trong dòng | `a = b+c` → `a  =  b + c` | Có | Ngôn ngữ không nhạy khoảng trắng; không áp dụng trong string literal hay raw string |
| T3 | Đổi tên biến cục bộ | `total` → `v_3f9a` | Có nếu tắt debug info | C: `-g0`; Go: `-ldflags="-s -w"`; Rust: `strip=true`, `debug=0`. Tên hàm hay symbol export **không** được đổi |
| T4 | Chèn dead code riêng từng biến thể, trên cùng một dòng hoặc ở cuối file | `if (0) { … }` | Có thể | Phụ thuộc DCE của compiler và linker; **dễ hỏng nhất**, phải đo kỹ |
| T5 | Đổi tên tham số hàm (không export) | | Có nếu tắt debug info | Như T3 |
| T6 | Đổi thứ tự khai báo độc lập | | **Thường KHÔNG** | Làm đổi layout symbol, mặc định tắt |

Các thứ không được động vào: string literal, tên export hoặc public, đường dẫn file, macro dùng `#`/`##`, reflection (Go `reflect`, Java), mọi thứ có thể đi vào binary như dữ liệu.

### 5.2 Ràng buộc build để reproducible

- C/C++: `-g0 -ffile-prefix-map=…=. -frandom-seed=<cố định>`, `SOURCE_DATE_EPOCH`, linker `--build-id=none` hoặc build-id tính từ nội dung.
- Go: `-trimpath -buildvcs=false -ldflags="-s -w -buildid="`.
- Rust: `--remap-path-prefix`, `codegen-units=1`, `debug=0`, `strip=true`.
- Mọi cell dùng **cùng image toolchain** (ghim theo digest).

**Ngôn ngữ đề xuất cho MVP: Go.** Go reproducible gần như mặc định với `-trimpath`, toolchain đóng gói gọn, và có `go/scanner` để làm VG ở mức token.

### 5.3 Tính chất bảo mật (giả thuyết cần chứng minh)

Gọi `V_1..V_k` là các biến thể và `I = ∩ info(V_i)` là thông tin chung của mọi biến thể.

**Mệnh đề.** Nếu gateway ACCEPT thì `artifact = f(V_1) = … = f(V_k)` với `f` là hành vi của compiler (có thể bị chiếm). Một attacker không biết seed mà muốn mọi artifact trùng nhau thì chỉ có thể mã hóa vào artifact những thông tin thuộc `I`. Nếu nhúng thông tin phụ thuộc phần khác biệt, xác suất trùng chỉ cỡ va chạm hash.

Hệ quả:
- Comment, tên biến cục bộ và dead code gốc **không** thoát ra được qua artifact.
- Attacker thích nghi vẫn có thể chuẩn hóa (bỏ comment, alpha-rename, tự chạy DCE) rồi nhúng phần còn lại: cấu trúc AST, hằng số, tên export. **Khoảng cách giữa `I` và thông tin của binary trung thực** là câu hỏi nghiên cứu chính (RQ2).
- Tăng `k` không giảm được `I` qua các phép T1–T5, vì phần bất biến vẫn bất biến. `k` lớn chủ yếu giúp chống A4 (một cell bị chiếm).

### 5.4 Thiết kế mở rộng (tùy chọn)

- **Toolchain đa dạng (chống A2):** cell 1 dùng GCC, cell 2 dùng Clang. Khi đó output không giống bit nữa, nên cần so sánh ở mức ngữ nghĩa. Hướng này khó, ghi là future work.
- **Canary hash:** VG chèn vào comment một chuỗi ngẫu nhiên riêng cho từng cell, rồi gateway quét artifact tìm canary hoặc các dạng mã hóa thông dụng của nó (base64, xor đơn giản). Đây là lớp phòng thủ phụ, chỉ bắt được attacker vụng.

---

## 6. Luồng một lần build

1. Developer chạy `sg seal && sg push`.
2. Orchestrator thấy commit mới và gọi `KB /v1/jobs` cho từng module.
3. Orchestrator khởi chạy VG cell với `job_id` và ciphertext.
4. VG gọi `KB /v1/release-key` để lấy data key, giải mã, kiểm tra `plaintext_sha256`.
5. VG lấy seed và `k` khóa cell, sinh `V_1..V_k`, mã hóa từng biến thể, xóa plaintext, rồi tự hủy.
6. Orchestrator khởi chạy `k` build cell, mỗi cell nhận đúng một biến thể đã mã hóa cùng khóa riêng (được inject qua KB, orchestrator không thấy).
7. Các cell build và xuất `(artifact, exit, error_class)` cho AG.
8. AG so sánh, quyết định ACCEPT, REJECT hoặc FAIL, trả kết quả tại `T_max`.
9. Nếu ACCEPT, Release Signer ký provenance và publish.

**Multi-module:** nếu module B import A, cell build B chỉ nhận **interface** của A (Go: export data hoặc file `.a` đã build; C: header). Không giải mã source A cho cell của B.

---

## 7. Kênh rò rỉ còn lại và biện pháp giảm

### 7.1 Timing

Gateway trả kết quả tại `T_max` cố định. Cell chạy quá `T_max` thì FAIL(timeout).

### 7.2 Pass/fail (1 bit mỗi build)

- Attacker có thể cố tình làm build fail tùy theo một bit của source.
- Biện pháp: giới hạn số FAIL trên mỗi commit và mỗi ngày, báo động khi tỷ lệ fail bất thường. Build lại cùng commit thì dùng **seed mới**, và chỉ cho tối đa N lần.
- Băng thông còn lại: ≤ log2(|error_class|) bit cho mỗi build đã được rate limit. Phải ghi con số này vào báo cáo.

### 7.3 Error class

- Dùng enum nhỏ (`OK`, `COMPILE_ERROR`, `LINK_ERROR`, `TIMEOUT`, `OOM`). Không trả về thông điệp.
- Các cell phải trả cùng error class, nếu không thì REJECT.

### 7.4 Kích thước artifact

Đã được bao quát vì so từng byte. Kích thước là một phần của artifact.

---

## 8. Cấu trúc repo đề xuất

```
sourceguard/
├── cmd/
│   ├── sg/                 # CLI developer: init, seal, push, verify
│   ├── sg-broker/          # Key Broker
│   ├── sg-variant/         # Variant Generator (TCB, giữ < 2k LOC)
│   ├── sg-cell/            # agent chạy trong build cell
│   ├── sg-gateway/         # Artifact Gateway (TCB)
│   └── sg-sign/            # Release Signer
├── internal/
│   ├── manifest/           # schema + ký/kiểm chữ ký
│   ├── crypto/             # bọc age/X25519, không tự viết primitive
│   ├── variants/
│   │   ├── golang/         # T1–T5 cho Go, dùng go/scanner
│   │   └── clang/          # T1–T5 cho C (Phase 3)
│   └── attest/             # MVP token; sau này SEV-SNP/TDX
├── images/
│   └── go-1.23/            # Dockerfile/rootfs toolchain, ghim digest
├── deploy/
│   ├── firecracker/
│   └── bwrap/              # fallback cho VPS không có KVM
├── attacks/                # bộ tấn công đối kháng (mục 10)
├── eval/
│   ├── invariance/         # Phase 1: đo T1–T6 trên corpus thật
│   └── leakage/            # đo I so với binary
└── docs/
    └── architecture.md     # file này
```

Ngôn ngữ cài đặt: Go. Thư viện: `filippo.io/age`, `sigstore/cosign` hoặc `in-toto-golang` cho provenance.

---

## 9. Lộ trình

### Phase 0: Khung (1–2 tuần)

- `sg seal/push` với age, manifest đã ký, Forgejo chỉ lưu ciphertext.
- KB tối giản (token, một module mỗi job).
- **Xong khi:** dump DB và repo trên Forgejo không có plaintext nào. Kiểm bằng cách grep chuỗi canary đã chèn.

### Phase 1: Kiểm chứng tính bất biến (2–4 tuần). **Quan trọng nhất**

- Cài T1–T6 cho Go.
- Corpus: 30–50 project Go open source, có cả project dùng `//go:embed`, cgo, reflect.
- Với mỗi project và mỗi phép biến đổi: build bản gốc và 10 biến thể, so hash.
- **Output:** bảng tỷ lệ "giống bit" theo từng phép biến đổi và từng loại project, cùng danh sách nguyên nhân lệch.
- **Xong khi:** có ít nhất một tổ hợp phép biến đổi giống bit trên ≥ 90% corpus. Nếu không đạt, **dừng lại xem lại giả thuyết** trước khi làm tiếp.

### Phase 2: Pipeline đầy đủ (3–4 tuần)

- VG cell, `k = 2` build cell (bwrap trước, Firecracker sau), AG, Release Signer.
- Cố định `T_max`, enum lỗi, rate limit FAIL.
- **Xong khi:** một project Go thật build end-to-end và provenance verify được.

### Phase 3: Tấn công đối kháng (3–4 tuần)

- Chạy bộ `attacks/` (mục 10), đo tỷ lệ phát hiện và băng thông rò rỉ.
- **Xong khi:** có bảng kết quả cho demo và white paper.

### Phase 4: Mở rộng (tùy chọn)

- C/C++ (GCC/Clang), Rust.
- Attestation SEV-SNP/TDX cho KB và VG.
- Toolchain đa dạng (mục 5.4).

---

## 10. Bộ tấn công đối kháng (`attacks/`)

Mô phỏng RCE trong compiler bằng **compiler wrapper độc**: một wrapper bọc `go build` hoặc `gcc`, đọc source và sửa artifact. Cách này tương đương về khả năng với RCE thật, nhưng dễ lặp lại.

| ID | Tấn công | Kỳ vọng |
|---|---|---|
| X1 | Nhúng nguyên văn source vào section mới của binary | REJECT (lệch hash) |
| X2 | Nhúng source đã nén và mã hóa XOR | REJECT |
| X3 | Bỏ comment rồi nhúng | REJECT nếu T3 bật (tên biến khác) |
| X4 | Bỏ comment, alpha-rename tên cục bộ, rồi nhúng AST chuẩn hóa | **ACCEPT**: đo lượng thông tin lọt (RQ2) |
| X5 | Mở socket ra ngoài | Fail ở tầng sandbox, không có NIC |
| X6 | Ghi ra disk để lần sau đọc | Không có disk bền, VM bị hủy |
| X7 | Fail có điều kiện theo một bit của source | Rate limit, đo số bit/ngày |
| X8 | Kéo dài thời gian theo nội dung | Cố định `T_max`, không quan sát được |
| X9 | Chỉ chiếm cell 1 (A4) | REJECT |
| X10 | Phát hiện mình đang ở biến thể nào để phối hợp | Không phối hợp được vì không có kênh giữa các cell. Phải kiểm |
| X11 | Implant trong `strip`/`ld` dùng chung (A2), không phụ thuộc source | ACCEPT: ngoài phạm vi G1, cần DDC (Phase 4) |

Chỉ số báo cáo: tỷ lệ phát hiện, false positive (biến thể trung thực mà lệch hash), overhead thời gian build, băng thông rò rỉ còn lại.

---

## 11. Câu hỏi nghiên cứu

- **RQ1.** Phép biến đổi nào giữ output giống bit trên các compiler phổ biến, và tỷ lệ là bao nhiêu trên codebase thật?
- **RQ2.** Sau chuẩn hóa, attacker thích nghi lấy được thêm bao nhiêu so với việc dịch ngược binary trung thực? Đo bằng độ tương đồng giữa AST chuẩn hóa và output của decompiler (Ghidra) so với source gốc.
- **RQ3.** Băng thông thực tế của kênh pass/fail và timing sau khi áp biện pháp?
- **RQ4.** Overhead của `k = 2` và `k = 3` so với build thường?

---

## 12. Công trình liên quan

- Hugenroth et al., *Attestable Builds*, ACM CCS 2025: TEE và sandbox cho source-to-binary integrity. Không xét confidentiality khi compiler bị chiếm. https://arxiv.org/abs/2505.02521
- Krahn et al., *TICAL*, EDCC 2024: trusted compilation trên SGX, cả integrity lẫn confidentiality trước hạ tầng. https://arxiv.org/abs/2511.17070
- *Kettle* (2026): confidential build flow với pre-attestation. Input độc nằm ngoài threat model. https://www.emergentmind.com/papers/2605.08363
- Wheeler, *Diverse Double-Compiling*: phát hiện trusting-trust. https://arxiv.org/abs/1004.5534
- *Trusting-Trust Attack against an Entire Linux Distribution through Binary Manipulation* (2026): implant trong `strip` triệt tiêu khi so sánh rebuild. Động lực trực tiếp cho DSV. https://arxiv.org/pdf/2607.24888
- N-variant systems (Cox et al., USENIX Security 2006): đa dạng hóa nhiều biến thể để phát hiện tấn công lúc runtime. DSV mang ý này sang build-time và confidentiality.

---

## 13. Ghi chú hạ tầng (ngân sách VPS 100.000–200.000đ/tháng)

- Phase 0–1 chạy được trên 1 VPS (2 vCPU, 4 GB RAM) hoặc ngay trên laptop.
- Firecracker cần KVM. Đa số VPS giá rẻ không có nested virtualization, nên dùng `bwrap --unshare-all` và **ghi rõ đây là cách ly yếu hơn**.
- SEV-SNP/TDX cần cloud có confidential VM (Azure, GCP), không có ở mức giá này. Đó là việc của Phase 4.
