# Evaluation Artifact

This directory contains the tools that produced the measurements in [`docs/results`](../docs/results), as prebuilt programs for Linux (amd64 and arm64). With them the invariance and leakage experiments can be rerun on the published corpora, or on other projects.

The tools are distributed as binaries. Their source code, which includes the Variant Generator, is not public.

| File | Purpose |
|---|---|
| `bin/corpusrun-linux-*` | Go corpus: invariance per transformation set, bisection of a divergence, leakage measurement, comparison with a disassembly or with Ghidra |
| `bin/polyrun-linux-*` | C, C++ and Rust corpora: invariance with GCC, Clang and rustc, bisection |
| `ghidra/ExportStructure.java` | Ghidra script used for the decompiler comparison |
| `Dockerfile` | Image with the compilers and build tools the corpora need |
| `SHA256SUMS` | Checksums of the binaries |

Built from revision `eb76e80` of the implementation.

## Setup

```sh
git clone https://github.com/95Lab/sourceguard && cd sourceguard
(cd artifact && sha256sum -c SHA256SUMS)
docker build -t sourceguard-artifact artifact
run() { docker run --rm -v "$PWD/docs/results:/data:ro" -v "$PWD/out:/out" sourceguard-artifact "$@"; }
```

The experiments download the projects under study from GitHub and, for Go and Rust, their dependencies.

## Go: invariance

```sh
# one project, default sets (T1+T2+T3 with 10 variants; T1, T2, T3 with 5 each)
run corpusrun -corpus /data/corpus-go.lock -only tomnomnom/gron -work /out/go

# the whole corpus with all five transformations (about half an hour on 18 cores)
run corpusrun -corpus /data/corpus-go.lock -work /out/go -j 3 -sets "T1,T2,T3,T4,T7=10;T4=5;T7=5"

# with cgo enabled
run corpusrun -corpus /data/corpus-go.lock -work /out/cgo -cgo -sets "T1,T2,T3=10"

run corpusrun -report /out/go          # Markdown tables
```

A line such as `T1,T2,T3=1/10` means: 10 variants were built and produced 1 distinct artifact, that is, they are bit-identical. Transformation names: `T1` comment content, `T2` whitespace, `T3` local names, `T4` appended dead code, `T7` unexported package-level variables and constants, `NONE` no change (a control for build reproducibility).

Results are written to `<work>/results/<project>.json` and include the seed of every variant set. An existing result is not recomputed; delete the file or use `-only`.

To find the cause of a non-identical result:

```sh
run corpusrun -corpus /data/corpus-go.lock -work /out/go -diag owner/repo -set T1,T2,T3
```

## Go: behaviour

```sh
run corpusrun -corpus /data/corpus-go.lock -work /out/tests -tests T1,T2,T3,T4,T7 -only junegunn/fzf
run corpusrun -test-report /out/tests
```

This runs the project's `go test -short ./...` on the unmodified source and on one variant, and compares the outcome per package and per failing test. Differences are rerun three times on each side before they count as regressions. It must run on the platform the binary was built for (no cross-compilation), since the tests are executed.

## Go: what remains exposed

```sh
run corpusrun -corpus /data/corpus-go.lock -work /out/leak -leak -only junegunn/fzf
run corpusrun -leak-report /out/leak
```

This measures comment and name coverage, the attacker's normal form, the names present in the stripped binary, and the structural comparison with a disassembly. For the comparison with a decompiler, install Ghidra 12.2 and JDK 25 in the image and add `-ghidra /path/to/ghidra/support/analyzeHeadless`.

## C, C++ and Rust

```sh
run polyrun -corpus /data/corpus-c-rust.txt -only facebook/zstd -cc gcc   -work /out/poly
run polyrun -corpus /data/corpus-c-rust.txt -only facebook/zstd -cc clang -work /out/poly
run polyrun -corpus /data/corpus-c-rust.txt -only chmln/sd               -work /out/poly
run polyrun -corpus /data/corpus-c-rust.txt -lang c -cc gcc -work /out/poly -sets "T1,T2=6;T1=4;T2=4;T2X=2"
run polyrun -report /out/poly
run polyrun -corpus /data/corpus-c-rust.txt -only redis/redis -cc gcc -work /out/poly -diag T2
```

`T2X` pads whitespace inside lines and is a control; it is expected to change Rust binaries. Each project is built twice unmodified first, and is reported as `not_reproducible` if those builds differ.

`corpus-c-rust.txt` takes each project at the tip of its default branch, so results for projects that have changed since the study may differ. The commits used in the study are recorded in `docs/results/eval-c-rust.json`.

## What the artifact does not contain

- The build pipeline (Key Broker, build cells, gateway, signer), the Forgejo integration and the adversarial compiler used for the pipeline tests.
- Source code of the tools.

## Terms

The binaries are provided for evaluating and reproducing the published results. They are not covered by the CC BY 4.0 licence that applies to the documents and data in this repository. Copyright 2026 Dang-Hieu Nguyen (95LAB), all rights reserved. Contact: hillnguyen528@gmail.com.
