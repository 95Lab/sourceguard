# SourceGuard: A Build Architecture That Keeps Source Confidential Under Compiler RCE

> Version: 0.2 (draft, pre-implementation). Date: 2026-11-15
> Status: design and research hypotheses. The "output invariance" properties in Section 5 are **not yet validated**. Phase 1 exists to validate them.
>
> Author: Dang-Hieu Nguyen (hillnguyen528@gmail.com), 95LAB.
> Vietnamese original: [architecture.vi.md](architecture.vi.md). See [CITATION.cff](../CITATION.cff) for how to cite this work.

---

## 1. Goals and Scope

### 1.1 Security goals

**G1.** An attacker who achieves arbitrary code execution **inside the compiler** while building Project A cannot exfiltrate any information about A's source, other than information that is **invariant across all variants** (see Section 5.3).

**G2.** Full compromise of Forgejo, the CI orchestrator, the database, or storage does not reveal plaintext.

**G3.** Every released artifact carries verifiable provenance and has passed a cross-cell divergence check.

### 1.2 Out of scope (stated as explicit limitations)

| Not protected | Reason |
|---|---|
| Compromised developer machine | The original plaintext lives there |
| Compromised Variant Generator or Key Broker | These are part of the TCB |
| Side channels outside the artifact: timing, pass/fail | Bandwidth can only be reduced (Section 7), not eliminated |
| Invariant information (logic after normalization) | By definition, it ends up in the binary |
| Backdoors inside the artifact that run at runtime | A supply-chain integrity problem, not build confidentiality |
| All cells sharing the same **source-independent** implant | Does not leak source, but must be detected with DDC or diverse toolchains |

### 1.3 Core idea (novel contribution)

Comparing hashes from multiple builds of **the same source** does not catch input-triggered RCE: every cell runs the same exploit on the same data and embeds the same content, so hashes still match.

**Differential Source Variants (DSV):** each build cell receives a **source variant that differs textually but is semantically equivalent**, chosen so that an honest compiler produces **bit-identical** artifacts. A compromised compiler that embeds any information depending on the differing parts will cause the artifacts to diverge, and the gateway blocks the release.

Reproducibility, traditionally an **integrity** tool, is thereby repurposed as a **leak detector for confidentiality**.

---

## 2. Threat Model

### 2.1 Actors

- **A1, input attacker:** plants a malicious dependency or file in the source tree to exploit a compiler bug and gain RCE inside the compiler process. This is the primary scenario.
- **A2, toolchain attacker:** the compiler binary or bundled tools (ld, strip, ar) are implanted in advance.
- **A3, infrastructure attacker:** compromises Forgejo, the orchestrator, the host OS, or storage.
- **A4, single-cell attacker:** compromises one build cell only, e.g. via that cell's host.

### 2.2 Attacker capabilities inside a cell

- Reads the entire source variant delivered to that cell.
- Writes arbitrarily to the cell's artifact, logs, and exit code.
- Controls execution time.
- Has **no** network, **no** persistent disk, and **no** knowledge of other cells' variants.
- May know the variant generation algorithm (Kerckhoffs's principle). Does not know the seed.

### 2.3 Trust assumptions (TCB)

1. Variant Generator (VG): small, token-level, never executes project code.
2. Key Broker (KB).
3. Artifact Gateway (AG).
4. The cell hypervisor or sandbox isolates correctly: no network, no disk.
5. Release Signer.

---

## 3. Architecture Overview

```
 Developer (trusted)
   │  sg-cli: encrypts each module with its own key (age), signs the commit
   ▼
 Forgejo (untrusted)  ── stores only ciphertext + signed manifest
   │
   ▼
 Orchestrator (untrusted) ── schedules jobs only, sees ciphertext + metadata
   │
   ├──► Key Broker (trusted) ── releases module keys to the right job, the right cell
   │
   ▼
 Variant Generator Cell (trusted, no network)
   │  decrypt module → generate k variants (secret seed) → encrypt each variant
   │  with the key of its build cell
   ▼
 ┌──────────────┐  ┌──────────────┐        ┌──────────────┐
 │ Build Cell 1 │  │ Build Cell 2 │  ...   │ Build Cell k │   (untrusted compiler)
 │ variant V1   │  │ variant V2   │        │ variant Vk   │   single-use microVM
 └──────┬───────┘  └──────┬───────┘        └──────┬───────┘   no net, no disk
        └────────┬────────┴────────────────────────┘
                 ▼
 Artifact Gateway (trusted)
   │  byte-for-byte comparison of the k cell artifacts
   │  all equal → pass | diverge → block + alert + store evidence (encrypted)
   │  logs: keep only exit code + allow-listed error class
   ▼
 Release Signer (trusted) ── signs provenance (in-toto/SLSA), never touches source
```

Default `k = 2`. Optionally `k = 3` to reduce the shared-information surface (Section 5.3).

---

## 4. Components

### 4.1 `sg-cli` (developer machine)

- `sg init`: create project config, module list, developer keys.
- `sg seal`: encrypt each module with its own data key (age/X25519), produce a signed `manifest.json`.
- `sg push`: push ciphertext and manifest to Forgejo.
- `sg verify <artifact>`: verify provenance.

Layout on Forgejo:

```
/sg/manifest.json          # signed, contains no plaintext
/sg/modules/<module_id>.age
/sg/interfaces/<module_id>.iface.age   # public cross-module headers/interfaces (optional)
```

`manifest.json`:

```json
{
  "project": "project-a",
  "commit": "<sha256 of the ciphertext set>",
  "modules": [
    {"id": "core", "ciphertext_sha256": "…", "plaintext_sha256": "…", "lang": "go"}
  ],
  "toolchain": {"image": "sg-go-1.23@sha256:…", "flags": ["-trimpath", "-buildvcs=false"]},
  "signature": "…"
}
```

Note: `plaintext_sha256` lets the VG verify the plaintext after decryption. A hash of high-entropy plaintext does not reveal its content.

### 4.2 Key Broker (KB)

- Holds each module's data key, wrapped with the KB's key.
- API:
  - `POST /v1/jobs` (orchestrator): register job `{project, commit, module_id}`, receive a `job_id`.
  - `POST /v1/release-key` (VG only): `{job_id, attestation}` → the data key of **one** module, single use.
  - `POST /v1/cell-keys` (VG): generate `k` ephemeral key pairs for the job's build cells.
- Policy: one `job_id` decrypts exactly one module, 10-minute TTL, never re-issued.
- **MVP:** attestation is a short-lived token plus image hash. **Later:** AMD SEV-SNP or Intel TDX reports.

### 4.3 Variant Generator (VG), part of the TCB

Principle: **as small as possible, no deep parsing, never executes project code.**

- Input: module plaintext, secret seed (32 bytes, generated by the KB and visible only to the VG), `k`.
- Output: `k` variants, each encrypted with one cell's key.
- Hard constraint: **preserve line count and the position of every meaningful token.** Reason: `__LINE__`, `assert`, Go's pclntab, and line tables all end up in the binary.
- Records the transformation map (encrypted) for debugging when the gateway reports divergence.

See Section 5 for the list of transformations.

### 4.4 Build Cell

- Single-use microVM, e.g. Firecracker. On VPSs without KVM, the MVP uses `bubblewrap` or `podman --network=none --read-only` (weaker, documented as such).
- Read-only rootfs, tmpfs workspace, no network (`--network=none`, no NIC device).
- Dependencies are **vendored** and shipped inside the variant or image. Nothing is downloaded at build time.
- Fixed build time: a cell runs for at most `T_max`, and the gateway always returns its result at `T_max` (reduces the timing channel).
- Only outputs: `artifact.bin`, `exit_code`, `error_class` (enum).
- VM is destroyed immediately after output is emitted.

### 4.5 Artifact Gateway (AG), part of the TCB

```
inputs: {cell_i: (artifact_i, exit_i, error_class_i)} for i in 1..k
if any exit_i != exit_1 or any error_class_i != error_class_1:  → REJECT(divergent_status)
if exit_1 != 0:                                              → FAIL(error_class_1)   # no logs returned
if any sha256(artifact_i) != sha256(artifact_1):             → REJECT(divergent_artifact) + ALERT
else                                                         → ACCEPT(artifact_1)
```

- **Never forwards raw compiler logs.** Developers who need error details rebuild locally.
- Rate-limits FAIL builds per project (Section 7.2).
- On REJECT: stores all cell artifacts **encrypted to the developer's key** for investigation.

### 4.6 Release Signer

- Receives ACCEPTed artifacts and signs provenance (in-toto attestation, SLSA v1): commit, manifest, toolchain image, `k`, artifact hash, gateway verdict.
- Never receives source.

### 4.7 Orchestrator

- Forgejo Actions or Woodpecker. It only moves ciphertext and calls APIs, and is treated as **fully untrusted**.

---

## 5. Differential Source Variants (DSV)

### 5.1 Candidate transformations

All must **preserve line count**. The "Expected" column is a hypothesis that Phase 1 must measure.

| ID | Transformation | Example | Expected bit-identical output | Conditions |
|---|---|---|---|---|
| T1 | Replace comment content, keep line count | `// compute fee` → `// q8Zk1…` | Yes | No doc-comments or directives that reach the binary (`//go:embed`, `//go:generate`, Doxygen) |
| T2 | Change intra-line whitespace | `a = b+c` → `a  =  b + c` | Yes | Whitespace-insensitive language; never inside string or raw-string literals |
| T3 | Rename local variables | `total` → `v_3f9a` | Yes, with debug info off | C: `-g0`; Go: `-ldflags="-s -w"`; Rust: `strip=true`, `debug=0`. Function names and exported symbols are **never** renamed |
| T4 | Insert per-variant dead code on the same line or at end of file | `if (0) { … }` | Maybe | Depends on compiler and linker DCE; **most fragile**, must be measured carefully |
| T5 | Rename (non-exported) function parameters | | Yes, with debug info off | Same as T3 |
| T6 | Reorder independent declarations | | **Usually NOT** | Changes symbol layout; disabled by default |

Never touched: string literals, exported or public names, file paths, macros using `#`/`##`, reflection (Go `reflect`, Java), anything that may enter the binary as data.

### 5.2 Build constraints for reproducibility

- C/C++: `-g0 -ffile-prefix-map=…=. -frandom-seed=<fixed>`, `SOURCE_DATE_EPOCH`, linker `--build-id=none` or a content-derived build-id.
- Go: `-trimpath -buildvcs=false -ldflags="-s -w -buildid="`.
- Rust: `--remap-path-prefix`, `codegen-units=1`, `debug=0`, `strip=true`.
- All cells use the **same toolchain image** (pinned by digest).

**Proposed MVP language: Go.** Go is reproducible almost by default with `-trimpath`, its toolchain is self-contained, and `go/scanner` makes a token-level VG straightforward.

### 5.3 Security property (hypothesis to be proven)

Let `V_1..V_k` be the variants and `I = ∩ info(V_i)` the information common to all variants.

**Proposition.** If the gateway ACCEPTs, then `artifact = f(V_1) = … = f(V_k)`, where `f` is the (possibly compromised) compiler's behavior. An attacker who does not know the seed and wants all artifacts to match can only encode information belonging to `I`. If it embeds information that depends on the differing parts, the probability of a match is on the order of a hash collision.

Consequences:
- Comments, local variable names, and original dead code **cannot** escape through the artifact.
- An adaptive attacker can still normalize (strip comments, alpha-rename, run its own DCE) and embed what remains: AST structure, constants, exported names. **The gap between `I` and the information already present in an honest binary** is the main research question (RQ2).
- Increasing `k` does not shrink `I` under T1–T5, because the invariant part stays invariant. A larger `k` mainly helps against A4 (a single compromised cell).

### 5.4 Extended design (optional)

- **Diverse toolchains (against A2):** cell 1 uses GCC, cell 2 uses Clang. Outputs are then no longer bit-identical, so comparison must be semantic. This is hard and recorded as future work.
- **Canary hash:** the VG inserts a random per-cell string into comments, and the gateway scans artifacts for the canary or common encodings of it (base64, simple XOR). A secondary defense that only catches unsophisticated attackers.

---

## 6. Build Flow

1. Developer runs `sg seal && sg push`.
2. Orchestrator detects the new commit and calls `KB /v1/jobs` for each module.
3. Orchestrator launches a VG cell with the `job_id` and ciphertext.
4. VG calls `KB /v1/release-key` to obtain the data key, decrypts, and checks `plaintext_sha256`.
5. VG obtains the seed and `k` cell keys, generates `V_1..V_k`, encrypts each variant, wipes the plaintext, and self-destructs.
6. Orchestrator launches `k` build cells; each receives exactly one encrypted variant plus its own key (injected via the KB, invisible to the orchestrator).
7. Cells build and emit `(artifact, exit, error_class)` to the AG.
8. AG compares and decides ACCEPT, REJECT, or FAIL, returning the result at `T_max`.
9. On ACCEPT, the Release Signer signs provenance and publishes.

**Multi-module:** if module B imports A, the cell building B receives only A's **interface** (Go: export data or a prebuilt `.a`; C: headers). A's source is never decrypted for B's cells.

---

## 7. Residual Leakage Channels and Mitigations

### 7.1 Timing

The gateway returns results at a fixed `T_max`. Cells exceeding `T_max` FAIL(timeout).

### 7.2 Pass/fail (1 bit per build)

- An attacker may deliberately fail the build depending on one bit of the source.
- Mitigation: cap FAILs per commit and per day, alert on anomalous failure rates. Rebuilding the same commit uses a **fresh seed**, at most N times.
- Residual bandwidth: ≤ log2(|error_class|) bits per rate-limited build. This figure must be reported.

### 7.3 Error class

- Use a small enum (`OK`, `COMPILE_ERROR`, `LINK_ERROR`, `TIMEOUT`, `OOM`). No messages are returned.
- All cells must report the same error class, otherwise REJECT.

### 7.4 Artifact size

Already covered by byte-for-byte comparison; size is part of the artifact.

---

## 8. Proposed Repository Layout

```
sourceguard/
├── cmd/
│   ├── sg/                 # developer CLI: init, seal, push, verify
│   ├── sg-broker/          # Key Broker
│   ├── sg-variant/         # Variant Generator (TCB, keep < 2k LOC)
│   ├── sg-cell/            # agent running inside a build cell
│   ├── sg-gateway/         # Artifact Gateway (TCB)
│   └── sg-sign/            # Release Signer
├── internal/
│   ├── manifest/           # schema + signing/verification
│   ├── crypto/             # age/X25519 wrappers, no custom primitives
│   ├── variants/
│   │   ├── golang/         # T1–T5 for Go, using go/scanner
│   │   └── clang/          # T1–T5 for C (Phase 3)
│   └── attest/             # MVP token; later SEV-SNP/TDX
├── images/
│   └── go-1.23/            # toolchain Dockerfile/rootfs, pinned by digest
├── deploy/
│   ├── firecracker/
│   └── bwrap/              # fallback for VPSs without KVM
├── attacks/                # adversarial attack suite (Section 10)
├── eval/
│   ├── invariance/         # Phase 1: measure T1–T6 on a real corpus
│   └── leakage/            # measure I versus the binary
└── docs/
    └── architecture.md     # this file
```

Implementation language: Go. Libraries: `filippo.io/age`, `sigstore/cosign` or `in-toto-golang` for provenance.

---

## 9. Roadmap

### Phase 0: Skeleton (1–2 weeks)

- `sg seal/push` with age, signed manifest, Forgejo stores ciphertext only.
- Minimal KB (token, one module per job).
- **Done when:** a dump of the database and the Forgejo repository contains no plaintext, verified by grepping for inserted canary strings.

### Phase 1: Invariance validation (2–4 weeks). **Most important**

- Implement T1–T6 for Go.
- Corpus: 30–50 open-source Go projects, including ones using `//go:embed`, cgo, and reflect.
- For each project and transformation: build the original and 10 variants, compare hashes.
- **Output:** a table of bit-identical rates per transformation and project type, plus a list of divergence causes.
- **Done when:** at least one transformation combination is bit-identical on ≥ 90% of the corpus. If not, **stop and revisit the hypothesis** before continuing.

### Phase 2: Full pipeline (3–4 weeks)

- VG cell, `k = 2` build cells (bwrap first, Firecracker later), AG, Release Signer.
- Fixed `T_max`, error enum, FAIL rate limiting.
- **Done when:** a real Go project builds end-to-end and its provenance verifies.

### Phase 3: Adversarial evaluation (3–4 weeks)

- Run the `attacks/` suite (Section 10), measure detection rate and leakage bandwidth.
- **Done when:** a results table exists for the demo and white paper.

### Phase 4: Extensions (optional)

- C/C++ (GCC/Clang), Rust.
- SEV-SNP/TDX attestation for the KB and VG.
- Diverse toolchains (Section 5.4).

---

## 10. Adversarial Attack Suite (`attacks/`)

Compiler RCE is simulated with a **malicious compiler wrapper**: a wrapper around `go build` or `gcc` that reads the source and modifies the artifact. It is capability-equivalent to real RCE but easy to reproduce.

| ID | Attack | Expected |
|---|---|---|
| X1 | Embed verbatim source into a new binary section | REJECT (hash divergence) |
| X2 | Embed compressed, XOR-encrypted source | REJECT |
| X3 | Strip comments, then embed | REJECT if T3 is enabled (different variable names) |
| X4 | Strip comments, alpha-rename locals, embed normalized AST | **ACCEPT**: measure leaked information (RQ2) |
| X5 | Open an outbound socket | Fails at the sandbox layer, no NIC |
| X6 | Write to disk for a later run | No persistent disk, VM destroyed |
| X7 | Fail conditionally on one source bit | Rate-limited, measure bits/day |
| X8 | Vary runtime based on content | Fixed `T_max`, unobservable |
| X9 | Compromise only cell 1 (A4) | REJECT |
| X10 | Detect which variant it is in to coordinate | Cannot coordinate without an inter-cell channel. Must be tested |
| X11 | Source-independent implant in shared `strip`/`ld` (A2) | ACCEPT: outside G1, requires DDC (Phase 4) |

Reported metrics: detection rate, false positives (honest variants with divergent hashes), build-time overhead, residual leakage bandwidth.

---

## 11. Research Questions

- **RQ1.** Which transformations preserve bit-identical output on mainstream compilers, and at what rate on real codebases?
- **RQ2.** After normalization, how much more does an adaptive attacker obtain compared with decompiling an honest binary? Measured by similarity of the normalized AST and of decompiler (Ghidra) output to the original source.
- **RQ3.** What is the practical bandwidth of the pass/fail and timing channels after mitigation?
- **RQ4.** What is the overhead of `k = 2` and `k = 3` compared with a normal build?

---

## 12. Related Work

- Hugenroth et al., *Attestable Builds*, ACM CCS 2025: TEEs and sandboxing for source-to-binary integrity. Does not consider confidentiality under compiler compromise. https://arxiv.org/abs/2505.02521
- Krahn et al., *TICAL*, EDCC 2024: trusted compilation on SGX, covering both integrity and confidentiality against infrastructure. https://arxiv.org/abs/2511.17070
- *Kettle* (2026): confidential build flow with pre-attestation. Malicious inputs are outside its threat model. https://www.emergentmind.com/papers/2605.08363
- Wheeler, *Diverse Double-Compiling*: detecting trusting-trust attacks. https://arxiv.org/abs/1004.5534
- *Trusting-Trust Attack against an Entire Linux Distribution through Binary Manipulation* (2026): an implant in `strip` that cancels out under rebuild comparison. Direct motivation for DSV. https://arxiv.org/pdf/2607.24888
- N-variant systems (Cox et al., USENIX Security 2006): diversifying multiple variants to detect attacks at runtime. DSV carries this idea to build time and to confidentiality.

---

## 13. Infrastructure Notes (budget: VPS at ~100,000–200,000 VND/month, ≈ USD 4–8)

- Phases 0–1 run on a single VPS (2 vCPU, 4 GB RAM) or directly on a laptop.
- Firecracker requires KVM. Most low-cost VPSs lack nested virtualization, so use `bwrap --unshare-all` and **document that isolation is weaker**.
- SEV-SNP/TDX requires a cloud with confidential VMs (Azure, GCP), not available at this price point. That belongs to Phase 4.
