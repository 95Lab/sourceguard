# Evaluation Results: Dead Code, cgo, and Invariant Information

Date: 2026-11-15. This report extends the [Phase 1 invariance study](phase1-invariance.md). All experiments ran natively on linux/amd64 (containers on Modal) on the same 54 pinned projects.

## Summary

| Experiment | Result |
|---|---|
| T4, dead code (new) | Bit-identical on **54 / 54** projects, alone (270 variants) and combined with T1+T2+T3 (540 variants) |
| T1+T2+T3 with cgo enabled | Bit-identical on **53 / 54**; the exception (`helm/helm`) exposed a new limitation, see [Finding 5](#finding-5-programs-that-link-plugin-embed-hashes-of-export-data) |
| Invariant information (RQ2) | DSV hides **24.0%** of source bytes (comments) and **259,624** local names; of the names it cannot hide, about 95% of function, type and field names are already in the stripped binary, but package-level variable and constant names are not |
| T7, package-level renaming (new) | Bit-identical on **54 / 54** projects on two architectures, alone and with all other transformations; covers 90.5% of unexported package-level variables and constants |
| Agreement with the unmodified build | T1, T2 and T7 variants equal the unmodified build in 54 / 54 projects; T3 variants in none; T4 variants in 26 (amd64) or 27 (arm64) |
| Behaviour (new) | On 1,508 test packages of 52 projects, fully transformed programs pass exactly the tests the originals pass |
| Structure in the binary (RQ2) | The honest binary shows 43.5% of the call edges and 46.2% of the integer constants that the attacker's normal form contains (disassembly, 54 projects). Ghidra on stripped binaries confirms it: 41.3% of call edges on 15 projects |
| Attacker normal form (X4) | Identical across variants for all **6,606** compiled files: the adaptive attack always passes the gateway |
| Variant generation cost (RQ4) | Median **2.5%** of a cold build |

## T4: per-variant dead code

T4 appends one unexported, unreferenced function with random name and body to the end of every Go file. Existing lines do not move.

| Transformation set | Bit-identical projects | Variants built |
|---|---|---|
| T1 + T2 + T3 + T4 | 54 / 54 (100%) | 540 |
| T4 | 54 / 54 (100%) | 270 |

The architecture document expected T4 to be the most fragile transformation. For this form of dead code (a whole unreachable function, removed by the linker) it was not. Dead code *inside* live functions was not tested.

## T7: package-level variables and constants

The first leakage measurement (below) showed that the names of package-level variables and constants are invariant across variants but absent from a stripped binary. T7 renames the unexported ones, with one order-preserving mapping per package. It leaves alone:

- packages where something outside the compiler could refer to names: cgo, assembly files, `go:linkname`;
- **string variables**, because the linker flag `-X importpath.name=value` assigns to them by name and silently does nothing if the name does not exist;
- names that also occur as a composite-literal key in their package, because the parser cannot tell a field from a variable there.

| Transformation set | linux/arm64 | linux/amd64 | Variants per target |
|---|---|---|---|
| T1 + T2 + T3 + T4 + T7 | 54 / 54 | 54 / 54 | 540 |
| T4 | 54 / 54 | 54 / 54 | 270 |
| T7 | 54 / 54 | 54 / 54 | 270 |

No variant failed to compile. Coverage: of 8,840 distinct unexported package-level variable and constant names in the compiled first-party code, T7 renames 7,996 (90.5%).

## Variants versus the unmodified build

Bit-identity among variants is one property; equality with a conventional build of the unmodified source is another.

| Transformation | Projects whose variants equal the unmodified build |
|---|---|
| T1 (comments) | 54 / 54 |
| T2 (whitespace) | 54 / 54 |
| T7 (package-level names) | 54 / 54 |
| T4 (dead code) | 26 / 54 on amd64, 27 / 54 on arm64 |
| T3 (local names) | 0 / 54 |
| All five combined | 1 / 54 |

With T3 or T4 the scrubbed build yields a consistent artifact that is a different file from the conventional one. Its correctness therefore rests on the transformations preserving behaviour, which the next section tests. See the [security argument](../security-argument.md) for how the two properties enter the guarantees.

## Behaviour: test suites on original and variant

For each of the 54 Go projects, `go test -short ./...` was run on the unmodified tree and on one variant with all five transformations. In the variant every non-test file is transformed; test files only receive the renamings they need to keep compiling, because the comments and layout of a test are part of the test. A package or test that failed only on the variant was rerun three times on each side and counted as a regression only if it failed every time on the variant and passed every time on the original.

| Outcome per package | Packages |
|---|---|
| Pass on original and on variant | 1,508 (in 52 projects) |
| Fail on both (environment: missing services and the like) | 75 |
| Pass on original, fail on variant | 0 |
| Individual tests failing only on the variant | 0 |

This is the result for the final generator. The first run found regressions in two projects with one cause: `yaegi` and `delve` keep Go files in directories named `_test` and `_fixtures` and use them as inputs of their tests. The Go tool ignores directories whose names start with `_` or `.`; such files belong to no package and are data. The generator now leaves them alone, like `testdata`. Both projects were rerun and show no difference; the other 52 were not rerun, since the change only reduces what is transformed.

The check covers what the short test suites exercise, on one variant per project. It is evidence for, not a proof of, behaviour preservation.

T6 (reordering declarations) is not implemented. It moves tokens to other lines, and line numbers reach the binary, so it cannot be invariant under the line-preservation constraint.

## cgo enabled

Phase 1 built everything with `CGO_ENABLED=0`. Here all 54 projects were built with `CGO_ENABLED=1` and gcc, 10 variants each of T1+T2+T3. Files that import `"C"` receive whitespace changes only.

| Run | Bit-identical projects |
|---|---|
| First run | 53 / 54 (98.1%) |
| After the fix below (`helm/helm` reduced to T1) | 54 / 54, of which 53 with the full set |

The six projects that contain cgo files were all bit-identical in the first run.

### Finding 5: programs that link `plugin` embed hashes of export data

`helm/helm` gave a different artifact for every variant when cgo was enabled, and identical artifacts when it was disabled. Controls on the same project:

| Change between builds | Distinct artifacts |
|---|---|
| None (rebuild) | 1 of 4 |
| T1, comments | 1 of 5 |
| T2, whitespace | 4 of 4 |

Bisection reduced the T2 divergence to whitespace inside one declaration, `const APIVersionV3 = "v3"`. Linker options (`-B none`, `-linkmode=internal`) made no difference.

Cause: `helm` links the standard `plugin` package (through a dependency). Such a program carries a hash of each package's export data, so that a plugin built against different package versions is refused at load time. Export data records the column of declarations, so whitespace reaches the binary. A minimal reproduction confirms it:

| Program | `CGO_ENABLED` | Effect of changing whitespace in an imported package |
|---|---|---|
| no `plugin` import | 0 or 1 | artifact unchanged |
| imports `plugin` | 0 | artifact unchanged (the package is a stub without cgo) |
| imports `plugin` | 1 | **artifact changes** |

Export data also contains parameter names and the bodies of inlinable functions, so T3 and T4 are expected to be affected in the same way; this was not tested separately.

Consequence: for a program that links `plugin` with cgo enabled, only T1 is safe, because it preserves byte offsets. The Variant Generator now reduces such trees to T1 (it scans the tree including `vendor/`; the corpus runner asks the build graph, because the corpus projects do not vendor). With that reduction `helm` is bit-identical (10 of 10 variants).

This is a real loss of protection for those programs: with T1 alone, a compiler can strip comments and embed the rest without being detected (attack X3 of the adversarial suite).

### Related observation: Go package archives

The same export data makes a compiled Go *library* (a package archive, as opposed to a linked program) differ between variants: building a non-main package of a two-module sample project gave a different archive in each cell. Libraries therefore cannot be released as artifacts of their own; they are compiled as part of the programs that use them.

## Invariant information (RQ2)

The question: what can a compromised compiler still embed without causing divergence, and how does that compare with what an honest binary already reveals?

Method, per project, over the first-party Go files that are compiled into the binary (6,606 files, 46.0 MB in total):

- **Hidden by DSV.** Bytes inside comments (T1) and function-local names (T3).
- **Attacker normal form.** Each file is normalized the way the adaptive attacker X4 does it (comments dropped, local names canonicalized, code reprinted). Compressed size (DEFLATE) is used as a proxy for information content.
- **Already in the binary.** Every declared name that DSV does not rename is looked up in the honest, stripped binary. Names shorter than 5 characters are not checked. Local names serve as a control group: they are not expected in the binary, so their hit rate estimates chance matches.

| Quantity | Pooled | Median project | Range |
|---|---|---|---|
| Comment bytes / source bytes | 24.0% | 21.9% | 2.5% – 44.5% |
| Comment bytes that T1 replaces / source bytes | 23.0% | | |
| Replaced bytes that are licence headers / documentation comments / other comments | 8.8% / 56.1% / 35.1% | | |
| Compressed size without comments / original | 67.5% | 71.5% | |
| Compressed size of normal form / original | 67.3% | 70.9% | 46.9% – 95.9% |

| Names not renamed by DSV | Checked | Found in honest binary |
|---|---|---|
| Functions and methods | 40,642 | 95.9% |
| Struct fields and interface methods | 25,231 | 96.2% |
| Types | 11,161 | 93.7% |
| Package-level variables and constants | 15,315 | 18.0% |
| *Control: local names* | 32,024 | 30.5% |

Reading these numbers:

- **About a third of the compressed source is hidden, almost all of it comments.** The normal form is no smaller than comment-stripped source, because canonical names cost as much to encode as the originals. Compression therefore does not show the value of hiding local names; those are counted instead: 259,624 function-local names (50,067 distinct per project, summed).
- **Most remaining names are not secret from someone who has the binary.** Function, type and field names survive stripping in Go (pclntab and type metadata). The lookup is a substring match, so these rates are upper bounds; the control rate of 30.5% shows how often a name matches by chance.
- **Package-level variable and constant names were a residual leak.** Their hit rate (18.0%) is below the chance rate, so they are effectively absent from the binary, yet without T7 they are invariant across variants. [T7](#t7-package-level-variables-and-constants) now hides the unexported ones.
- **The normal form is stable.** For all 6,606 files, the normal form of two random variants equals that of the original. The X4 attack therefore always passes the gateway, as the design predicts.

### What the withheld comments contain

Sizes do not say what is withheld. Every comment block that T1 replaces, outside licence headers, was labelled by keyword and pattern: 97,269 blocks, 9,640,137 bytes. A block can carry several labels. "Fit" is the number of blocks, out of a [sample of 20 per label](eval-labels-sample.md), that fit the label when read.

| Label | Matched by | Projects | Blocks | Share of blocks | Share of bytes | Fit |
|---|---|---:|---:|---:|---:|---:|
| Task marker | `TODO`, `FIXME`, `XXX`, `HACK`, `BUG` | 45 | 1,824 | 1.9% | 3.0% | 20 / 20 |
| Person | `TODO(name)`, e-mail address | 25 | 926 | 1.0% | 1.7% | 16 / 20 |
| Tracker reference | `#1234`, `issues/123`, `ABC-123` | 41 | 666 | 0.7% | 1.9% | 17 / 20 |
| URL | `http(s)://` | 47 | 1,558 | 1.6% | 4.5% | 20 / 20 |
| Known defect or workaround | workaround, hack, deadlock, race condition, flaky, … | 45 | 356 | 0.4% | 1.1% | 16 / 20 |
| Security | vulnerability, CVE, untrusted, attack, insecure, … | 33 | 489 | 0.5% | 1.8% | 16 / 20 |
| Credential handling | password, secret, API key, private key, … | 24 | 748 | 0.8% | 1.5% | 18 / 20 |
| Rationale or constraint | because, so that, must not, invariant, caller must, … | 53 | 9,531 | 9.8% | 21.7% | 14 / 20 |
| Disabled code | text that parses as Go statements | 27 | 165 | 0.2% | 0.1% | 13 / 20 |
| **Any label** | | 54 | 13,861 | 14.3% | 28.9% | |

| Reference in withheld comments | Projects | Distinct values | Also verbatim in the stripped binary |
|---|---:|---:|---:|
| URL | 47 | 1,505 | 29 (1.9%) |
| Tracker reference | 41 | 624 | 9 (1.4%) |
| E-mail address | 7 | 12 | 1 |
| Credential-shaped string (cloud keys, forge tokens, PEM private keys) | 0 | 0 | 0 |

- **One block in seven carries a label**, and those blocks hold 29% of the withheld comment bytes.
- **Most labelled content is rationale**: why the code is as it is, and what callers must respect. This label is also the least precise.
- **The references are not in the artifact.** Under 2% of the URLs and tracker references occur in the binary; short tracker references match by chance, so that figure is an upper bound.
- **No real credential was found in a comment.**

Limits: the labels are keyword matches and several overcount, as the "Fit" column shows; the sample is 20 blocks per label; the corpus is open source, where comments are written for the public. The figures describe kinds of content, not its value.

Data: [per-project results](eval-labels-linux-amd64.json).

### Structure: normal form versus disassembly

The normal form contains the complete code structure. To estimate how much of that an honest binary also shows, three features that exist in both source and machine code were compared per function. The binary side comes from `go tool objdump` on the first-party functions.

| Feature (all present in the normal form) | In source | Also in the honest binary |
|---|---|---|
| Functions and methods that exist as a function | 59,382 | 45,742 (77.0%) |
| Call edges: (function, name of callee) | 213,284 | 92,838 (43.5%) |
| Integer constants ≥ 1000: (function, value) | 569 | 263 (46.2%) |
| String literals ≥ 8 bytes (sample) | 43,518 | 41,638 (95.7%) |

Reading these numbers:

- **Strings survive, structure does not.** Nearly all string literals are in the binary. Fewer than half of the call edges and integer constants can be read off the disassembly at the place where the source has them, mainly because the compiler inlines calls and folds constants.
- **So the normal form is worth considerably more than the binary.** A compromised compiler that embeds it (attack X4) hands over the pre-optimization structure: which function calls which, with every constant in place. This is the main residual risk of the design and it is not reduced by any transformation implemented here.

Caveats for the disassembly. The disassembler needs a symbol table, so it ran on a twin of each binary built without `-s -w`; the code is the same. Names of inlined functions remain in the pclntab, so a tool that decodes its inlining records could recover more call edges than counted here. Call edges are matched by callee name only.

### Structure: normal form versus a decompiler

The disassembly is a proxy for what a reverse engineer has. To check it, the comparison was repeated with Ghidra 12.2 on the **stripped** release binaries of 17 small and medium projects of the corpus. Ghidra's Go analyzer recovers function names from the pclntab; a script decompiles every first-party function and exports its call targets and constants from the decompiler's intermediate form.

Two projects gave no usable result: for `FiloSottile/age` (built with Go 1.27.0) Ghidra recovered no first-party function names, and for `mikefarah/yq` the import failed. On the remaining 15 projects (3,893 source functions):

| Feature (all present in the normal form) | In source | Ghidra, stripped binary | Disassembly with symbols |
|---|---|---|---|
| Functions and methods recovered as a function | 3,893 | 2,658 (68.3%) | 2,667 (68.5%) |
| Call edges | 13,609 | 5,626 (41.3%) | 5,776 (42.4%) |
| Integer constants ≥ 1000 | 50 | 30 (60.0%) | 33 (66.0%) |

The decompiler recovers slightly less than the disassembly with symbols, never more. The proxy used for the whole corpus is therefore a fair, slightly generous, estimate of the reverse engineer's view, and the conclusion stands: more than half of the call structure in the attacker's normal form cannot be read off the binary.

Limits of this check: 15 projects, all built with Go 1.26.11; the constant sample is small (50); Ghidra ran with default analysis options and a 60-second limit per function; only one decompiler was tried. Median analysis time was 135 seconds per binary.

## Cost (RQ4)

Generating one variant of a whole tree took a median of 0.17 s per project (maximum 4.4 s), a median of 2.5% of the project's cold build time. The dominant cost of the scheme is the `k` builds themselves, which run in parallel in separate cells; the end-to-end cost is measured below.

### End to end: push to signed artifact

Time from the arrival of a push at the pipeline to a signed artifact, in seconds, on one host (18 cores, Ubuntu 24.04, Forgejo 11, Firecracker 1.18; 4 GB of memory per cell). Median of three runs; in parentheses the ratio to a conventional build of the same tree with the same image, flags and a cold cache. All 72 runs were accepted.

| Project | Conventional | Containers, k=2 | k=3 | k=4 | MicroVMs, k=2 | k=3 | k=4 |
|---|---:|---:|---:|---:|---:|---:|---:|
| fzf | 2.7 | 4.7 (1.7) | 6.3 (2.3) | 7.2 (2.7) | 7.3 (2.7) | 9.4 (3.5) | 10.3 (3.8) |
| yq | 5.9 | 10.2 (1.7) | 13.5 (2.3) | 16.9 (2.9) | 14.7 (2.5) | 17.5 (3.0) | 20.5 (3.5) |
| restic | 10.4 | 20.4 (2.0) | 28.5 (2.7) | 37.5 (3.6) | 30.5 (2.9) | 38.8 (3.7) | 45.3 (4.3) |
| caddy | 13.8 | 27.2 (2.0) | 37.9 (2.7) | 48.7 (3.5) | 39.9 (2.9) | 50.9 (3.7) | 58.2 (4.2) |

- The time grows with `k` although cells run in parallel: all cells share the cores of one host, and each build already uses every core. Separate hosts were not measured.
- A microVM cell adds 2.5 to 13 s over a container cell.
- Key release, variant generation, comparison and signing together took a median of 2.7 s (1.2 to 8.8).
- Sealing and pushing on the developer's machine is not included.
- Dependencies are vendored, since cells have no network.

Data: [pipeline runs](eval-endtoend-linux-amd64.tsv), [conventional builds](eval-endtoend-baseline-linux-amd64.tsv).

The first attempt failed for restic and caddy in every configuration. The Go compiler writes scratch files to `/tmp`, which a cell limits to 256 MB. The cell reports only an error class, so the cause had to be found by reproducing the build outside the pipeline; after five failures the gateway's rate limit rejected every further build. The cell now sets `GOTMPDIR` to its workspace.

## Limitations

- One OS and architecture (linux/amd64) for these experiments; one main package per project.
- T4 covers unreachable functions only.
- The `plugin` effect on T3 and T4 is inferred from how export data works; only T2 was measured.
- The binary lookup is a substring match on names of 5 or more characters. String literals and constants were not looked up.
- Information content is approximated by compressed size.
- The structural comparison counts three features; it is not a similarity measure between source and decompiled code. The decompiler check covers 15 of the 54 projects.
- Limitations of [Phase 1](phase1-invariance.md#limitations) apply as well (debug information stripped, Go 1.26.5 to 1.27.4).

## Data

> The tools that produced these results are available as prebuilt programs, with instructions, in [`artifact/`](../../artifact/README.md). Their source code is not public.

Raw data: [`eval-all-linux-amd64.json`](eval-all-linux-amd64.json), [`eval-all-linux-arm64.json`](eval-all-linux-arm64.json), [`eval-tests-linux-amd64.json`](eval-tests-linux-amd64.json), [`eval-t4-linux-amd64.json`](eval-t4-linux-amd64.json), [`eval-cgo-linux-amd64.json`](eval-cgo-linux-amd64.json) (final), [`eval-cgo-first-run-linux-amd64.json`](eval-cgo-first-run-linux-amd64.json), [`eval-leakage-linux-amd64.json`](eval-leakage-linux-amd64.json), [`eval-decompiler-linux-amd64.json`](eval-decompiler-linux-amd64.json).

## Leakage per project

| Project | Files | Source KB | Comments | Normal form / original (compressed) | Local names | Non-local names in binary | VG s | Build s |
|---|---|---|---|---|---|---|---|---|
| antonmedv/fx | 68 | 376 | 20.3% | 73.5% | 2554 | 81.1% | 0.19 | 22 |
| BurntSushi/toml | 12 | 117 | 29.6% | 61.3% | 688 | 68.9% | 0.05 | 0 |
| caddyserver/caddy | 203 | 2275 | 38.5% | 51.2% | 9664 | 88.7% | 1.22 | 52 |
| charmbracelet/glow | 21 | 87 | 16.0% | 76.5% | 488 | 82.5% | 0.02 | 22 |
| charmbracelet/gum | 58 | 167 | 15.0% | 80.2% | 613 | 90.3% | 0.03 | 4 |
| cheat/cheat | 52 | 66 | 33.0% | 58.5% | 339 | 93.1% | 0.10 | 6 |
| cli/cli | 544 | 3597 | 17.0% | 75.9% | 19455 | 80.1% | 1.48 | 49 |
| cloudflare/cfssl | 94 | 650 | 23.2% | 67.1% | 3258 | 77.5% | 0.26 | 16 |
| coredns/coredns | 345 | 1185 | 23.6% | 67.1% | 7780 | 87.4% | 0.58 | 80 |
| direnv/direnv | 55 | 114 | 17.9% | 73.5% | 878 | 74.8% | 0.03 | 1 |
| dominikh/go-tools | 318 | 1554 | 30.0% | 59.6% | 7751 | 81.8% | 0.35 | 8 |
| dundee/gdu | 85 | 442 | 21.9% | 68.7% | 2911 | 88.6% | 0.50 | 41 |
| etcd-io/bbolt | 52 | 281 | 29.6% | 62.5% | 1970 | 85.2% | 0.31 | 2 |
| ffuf/ffuf | 46 | 230 | 19.9% | 69.1% | 1254 | 94.0% | 0.20 | 4 |
| FiloSottile/age | 23 | 185 | 22.8% | 74.7% | 1090 | 73.0% | 0.07 | 2 |
| FiloSottile/mkcert | 5 | 33 | 8.8% | 89.3% | 178 | 55.7% | 0.01 | 1 |
| gitleaks/gitleaks | 47 | 174 | 21.9% | 68.9% | 883 | 85.3% | 0.08 | 16 |
| go-delve/delve | 178 | 2149 | 20.7% | 70.2% | 12428 | 76.8% | 0.40 | 4 |
| go-task/task | 110 | 362 | 20.0% | 71.5% | 2382 | 86.0% | 0.13 | 68 |
| gohugoio/hugo | 489 | 3350 | 27.6% | 68.6% | 21510 | 85.4% | 0.98 | 68 |
| gokcehan/lf | 23 | 275 | 8.8% | 85.9% | 1726 | 84.9% | 0.07 | 2 |
| golang-migrate/migrate | 21 | 67 | 29.2% | 64.6% | 437 | 75.3% | 0.12 | 0 |
| google/go-containerregistry | 165 | 669 | 36.2% | 63.8% | 3951 | 84.7% | 0.26 | 2 |
| google/gops | 10 | 22 | 22.0% | 77.1% | 173 | 87.5% | 0.01 | 1 |
| google/pprof | 49 | 499 | 31.0% | 63.1% | 3413 | 87.5% | 0.16 | 1 |
| grafana/k6 | 520 | 3249 | 26.4% | 63.2% | 18460 | 83.8% | 1.12 | 20 |
| hairyhenderson/gomplate | 94 | 306 | 22.1% | 67.8% | 2698 | 91.3% | 0.17 | 149 |
| helm/helm | 285 | 1469 | 33.2% | 64.8% | 7124 | 82.7% | 0.93 | 97 |
| jesseduffield/lazydocker | 71 | 334 | 14.0% | 74.4% | 1770 | 96.5% | 0.07 | 6 |
| jesseduffield/lazygit | 418 | 2926 | 24.5% | 59.8% | 15612 | 88.6% | 0.77 | 11 |
| junegunn/fzf | 47 | 694 | 16.0% | 75.2% | 3831 | 80.3% | 0.18 | 2 |
| kubernetes-sigs/kind | 172 | 580 | 42.1% | 51.8% | 2557 | 88.6% | 0.19 | 3 |
| mikefarah/yq | 145 | 534 | 9.1% | 81.5% | 3874 | 80.1% | 0.20 | 8 |
| muesli/duf | 9 | 52 | 17.8% | 72.8% | 245 | 35.3% | 0.02 | 1 |
| mvdan/gofumpt | 22 | 269 | 44.5% | 46.9% | 1346 | 78.6% | 0.05 | 1 |
| mvdan/sh | 16 | 268 | 24.4% | 66.8% | 1242 | 60.4% | 0.17 | 1 |
| nats-io/nats-server | 92 | 4573 | 30.2% | 61.4% | 24921 | 76.7% | 2.73 | 12 |
| nektos/act | 83 | 567 | 10.5% | 82.6% | 3916 | 91.8% | 0.18 | 12 |
| OJ/gobuster | 45 | 575 | 2.5% | 94.5% | 671 | 92.5% | 0.05 | 2 |
| open-policy-agent/opa | 415 | 4348 | 23.6% | 66.7% | 28617 | 83.1% | 4.43 | 37 |
| peco/peco | 49 | 266 | 28.8% | 61.6% | 1815 | 82.6% | 0.10 | 2 |
| pressly/goose | 74 | 244 | 27.7% | 62.6% | 1582 | 56.8% | 0.09 | 46 |
| projectdiscovery/httpx | 72 | 333 | 10.9% | 81.2% | 1741 | 90.2% | 0.19 | 30 |
| prometheus/node_exporter | 99 | 632 | 15.9% | 83.7% | 2120 | 65.8% | 0.14 | 6 |
| rakyll/hey | 5 | 27 | 22.1% | 80.6% | 117 | 87.4% | 0.06 | 4 |
| restic/restic | 275 | 1156 | 19.9% | 72.5% | 8169 | 86.1% | 0.98 | 37 |
| schollz/croc | 107 | 873 | 13.2% | 79.2% | 6060 | 79.6% | 0.33 | 39 |
| spf13/cobra-cli | 16 | 144 | 7.5% | 95.9% | 55 | 82.0% | 0.01 | 10 |
| tomnomnom/gron | 6 | 39 | 30.0% | 62.9% | 235 | 60.2% | 0.01 | 1 |
| traefik/yaegi | 195 | 1356 | 9.6% | 81.9% | 4322 | 91.4% | 3.33 | 3 |
| tsenart/vegeta | 25 | 109 | 23.0% | 73.7% | 736 | 81.8% | 0.09 | 4 |
| wagoodman/dive | 71 | 183 | 17.6% | 74.3% | 1264 | 86.5% | 0.05 | 13 |
| yuin/gopher-lua | 32 | 349 | 12.7% | 88.2% | 2783 | 79.0% | 0.11 | 1 |
| zyedidia/micro | 73 | 544 | 20.1% | 72.9% | 3967 | 90.5% | 0.12 | 4 |

## Tests per project

| Project | Packages with tests | Pass both | Fail both | Pass→fail | Tests failing only on the variant |
|---|---|---|---|---|---|
| antonmedv/fx | 19 | 19 | 0 | 0 |  |
| BurntSushi/toml | 1 | 1 | 0 | 0 |  |
| caddyserver/caddy | 31 | 31 | 0 | 0 |  |
| charmbracelet/glow | 1 | 1 | 0 | 0 |  |
| charmbracelet/gum | 1 | 1 | 0 | 0 |  |
| cheat/cheat | 8 | 5 | 3 | 0 |  |
| cli/cli | 259 | 258 | 1 | 0 |  |
| cloudflare/cfssl | 67 | 54 | 13 | 0 |  |
| coredns/coredns | 96 | 94 | 2 | 0 |  |
| direnv/direnv | 3 | 3 | 0 | 0 |  |
| dominikh/go-tools | 191 | 189 | 2 | 0 |  |
| dundee/gdu | 19 | 13 | 6 | 0 |  |
| etcd-io/bbolt | 9 | 8 | 1 | 0 |  |
| ffuf/ffuf | 12 | 11 | 1 | 0 |  |
| FiloSottile/age | 11 | 11 | 0 | 0 |  |
| FiloSottile/mkcert | 0 | 0 | 0 | 0 |  |
| gitleaks/gitleaks | 7 | 6 | 1 | 0 |  |
| go-delve/delve | 29 | 24 | 5 | 0 |  |
| go-task/task | 19 | 18 | 1 | 0 |  |
| gohugoio/hugo | 145 | 145 | 0 | 0 |  |
| gokcehan/lf | 1 | 1 | 0 | 0 |  |
| golang-migrate/migrate | 41 | 25 | 16 | 0 |  |
| google/go-containerregistry | 39 | 38 | 1 | 0 |  |
| google/gops | 5 | 5 | 0 | 0 |  |
| google/pprof | 13 | 12 | 1 | 0 |  |
| grafana/k6 | 79 | 77 | 2 | 0 |  |
| hairyhenderson/gomplate | 27 | 26 | 1 | 0 |  |
| helm/helm | 59 | 55 | 4 | 0 |  |
| jesseduffield/lazydocker | 6 | 6 | 0 | 0 |  |
| jesseduffield/lazygit | 33 | 33 | 0 | 0 |  |
| junegunn/fzf | 4 | 4 | 0 | 0 |  |
| kubernetes-sigs/kind | 20 | 20 | 0 | 0 |  |
| mikefarah/yq | 4 | 3 | 1 | 0 |  |
| muesli/duf | 1 | 1 | 0 | 0 |  |
| mvdan/gofumpt | 2 | 2 | 0 | 0 |  |
| mvdan/sh | 9 | 8 | 1 | 0 |  |
| nats-io/nats-server | 13 | 11 | 2 | 0 |  |
| nektos/act | 14 | 13 | 1 | 0 |  |
| OJ/gobuster | 5 | 5 | 0 | 0 |  |
| open-policy-agent/opa | 127 | 126 | 1 | 0 |  |
| peco/peco | 13 | 13 | 0 | 0 |  |
| pressly/goose | 12 | 11 | 1 | 0 |  |
| projectdiscovery/httpx | 7 | 7 | 0 | 0 |  |
| prometheus/node_exporter | 3 | 2 | 1 | 0 |  |
| rakyll/hey | 2 | 2 | 0 | 0 |  |
| restic/restic | 51 | 49 | 2 | 0 |  |
| schollz/croc | 34 | 33 | 1 | 0 |  |
| spf13/cobra-cli | 1 | 0 | 1 | 0 |  |
| tomnomnom/gron | 1 | 1 | 0 | 0 |  |
| traefik/yaegi | 9 | 7 | 2 | 0 |  |
| tsenart/vegeta | 6 | 6 | 0 | 0 |  |
| wagoodman/dive | 7 | 7 | 0 | 0 |  |
| yuin/gopher-lua | 1 | 1 | 0 | 0 |  |
| zyedidia/micro | 6 | 6 | 0 | 0 |  |
