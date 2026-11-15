# Phase 1 Results: Invariance on a Corpus of Real Go Projects

Date: 2026-11-15. Research question RQ1 of the [architecture](../architecture.md#11-research-questions):
*which transformations keep the compiler output bit-identical, and at what rate on real codebases?*

## Summary

On 54 open-source Go projects (15,302 Go files, 3.70 million lines, vendored code excluded), every transformation set produced **bit-identical artifacts for all variants of all 54 projects**, on two targets:

| Transformation set | linux/arm64 | linux/amd64 | Variants built per target |
|---|---|---|---|
| T1 + T2 + T3 | 54 / 54 (100%) | 54 / 54 (100%) | 540 |
| T1 (comments) | 54 / 54 (100%) | 54 / 54 (100%) | 270 |
| T2 (whitespace) | 54 / 54 (100%) | 54 / 54 (100%) | 270 |
| T3 (local names) | 54 / 54 (100%) | 54 / 54 (100%) | 270 |

This meets the Phase 1 exit criterion (at least one combination bit-identical on 90% of the corpus).

These numbers are for the generator **after three defects found by this study were fixed**. One (finding 2) was found and fixed on the first project tried, before the full run. The first full run, before the other two fixes, gave 51 / 54 (94.4%) for the combined set. The defects and fixes are described under [Findings](#findings); the first-run data is kept in [`phase1-first-run-linux-arm64.json`](phase1-first-run-linux-arm64.json).

No project was excluded: the unmodified source of all 54 built in both environments, and no variant failed to compile.

## Method

- **Corpus.** 54 projects with a `main` package, listed in [`eval/corpusrun/corpus.txt`](../../eval/corpusrun/corpus.txt) and pinned to the commit of their latest release in [`corpus.lock`](../../eval/corpusrun/corpus.lock). Of these, 25 use `//go:embed`, 48 import `reflect`, 27 import `unsafe`, 10 vendor their dependencies, 6 contain cgo files and 3 contain assembly.
- **Build.** `go build -trimpath -buildvcs=false -ldflags="-s -w -buildid="` with `CGO_ENABLED=0`. Each project is compiled with the Go toolchain its `go.mod` requires: Go 1.26.5 for 44 projects, and 1.26.6 to 1.27.4 for the other 10.
- **Procedure per project.** Build the unmodified source (the baseline). Then, for each transformation set, generate variants from a fresh random seed, build each one, and count distinct artifact hashes: 10 variants for the combined set, 5 for each single transformation. A set counts as bit-identical only if every variant builds and all hashes are equal.
- **Environments.** linux/arm64 was cross-compiled on macOS (Apple silicon). linux/amd64 was compiled natively in containers on Modal. Both used the same pinned commits. Each environment performed 1,404 builds.
- **Tooling.** [`eval/corpusrun`](../../eval/corpusrun): `-corpus` runs the study, `-report` renders tables, `-diag` reproduces a non-identical result from its recorded seed and bisects it to one file and one declaration.

## Findings

### 1. Local names reach the binary through stack layout

With T3 alone, the variants of a project agree with each other, but in all 54 projects they differ from the unmodified build. T1 and T2 variants, in contrast, are identical to the unmodified build in all 54. Renaming local variables therefore does change the compiled code.

The cause is consistent with the Go compiler breaking ties in stack-slot layout by variable name. Two consequences for the design:

- T3 must map names **monotonically**: the lexical order of local names has to be the same in every variant.
- That order is information the binary depends on. It is part of the invariant information `I` of [architecture §5.3](../architecture.md#53-security-property-hypothesis-to-be-proven): a compromised compiler can encode it without causing divergence.

### 2. The name mapping must be monotonic across the whole tree

The first implementation preserved name order within each function. On `tomnomnom/gron` this diverged for every T3 variant. A single mapping that is monotonic over all local names of the source tree fixed it. The likely mechanism is inlining, which places variables of different functions in the same stack frame; this was not verified directly, the evidence is that the change removes the divergence.

Side effect: within one variant, equal original names map to equal new names in every function. Which locals share a name is therefore also invariant information.

### 3. Embedded Go files are program data

`zyedidia/micro` diverged for every variant of every set. Bisection pointed to `runtime/syntax/syntax_converter.go`: a Go file inside a directory that the program embeds with `//go:embed`. Its bytes are copied into the binary, so rewriting its comments changes the artifact.

Fix: the Variant Generator resolves all `//go:embed` patterns and never transforms a file they cover.

### 4. Whitespace on very long lines changes generated code

`go-delve/delve` and `helm/helm` each diverged in 1 of 5 variants, and only for the combined set. Bisection of `delve` pointed to a function whose body is one very long line (`rot` in `pkg/dwarf/op/op.go`).

The Go compiler stores source columns in 8 bits, so columns above 255 collapse to one value. Added whitespace pushed tokens across that limit in one variant and not in another. T3 contributed by lengthening the line, which is why T2 alone did not trigger it.

Fix: T2 leaves a line untouched if its rewritten form would exceed 240 bytes. T1 preserves byte lengths, and T3 gives every renamed identifier the same length in all variants, so neither can cause the effect.

Verification: replaying the seeds that had diverged, with the fixed generator, gave one hash for all five variants in all three projects. For `helm` the offending line was not identified individually; the conclusion rests on the same fix removing the divergence for the same seed.

## Limitations

- **cgo was disabled.** The six projects with cgo files were built with `CGO_ENABLED=0`, so no C code was compiled. cgo is covered by the [follow-up evaluation](evaluation.md), which found one further limitation.
- **Only one main package per project was compiled**, for one OS and two architectures. Files outside its dependency closure, files for other platforms and test files were transformed but not compiled.
- **Sample size.** Zero divergences in 540 combined variants per target bounds the per-variant divergence rate at roughly 0.6% with 95% confidence, assuming independent variants. Defects that trigger more rarely would not have been seen; finding 4 appeared at a rate of 1 in 5 for two projects.
- **Debug information is stripped.** Results apply to builds with `-s -w`. With DWARF, local names are in the binary and T3 cannot be invariant.
- **Toolchains.** Go 1.26.5 to 1.27.4 only. Other versions and compilers (gccgo, TinyGo) were not tested.
- **T3 coverage was not measured.** Names used as composite-literal keys, struct fields, package-level and exported names are deliberately not renamed. One file of 15,302 (in `traefik/yaegi`) could not be transformed and was passed through unchanged.
- **T4 (dead code) is measured in the [follow-up evaluation](evaluation.md).** T6 (declaration order) is not implemented.
- **This is invariance, not leakage.** Bit-identity shows that an honest compiler does not cause false alarms. How much invariant information a compromised compiler can still embed is RQ2 and remains open.

## Reproducing

```sh
# local, linux/arm64 or linux/amd64 depending on the host
go run ./eval/corpusrun -corpus eval/corpusrun/corpus.lock -work run/corpus -j 3
go run ./eval/corpusrun -report run/corpus

# diagnose a non-identical result
go run ./eval/corpusrun -work run/corpus -diag owner/repo -set T1,T2,T3

# on Modal, linux/amd64 (requires a Modal account)
modal run eval/modal/corpus_app.py --out run/modal
go run ./eval/corpusrun -report run/modal
```

Raw results, including the seed of every variant set: [`phase1-linux-arm64.json`](phase1-linux-arm64.json), [`phase1-linux-amd64.json`](phase1-linux-amd64.json).

## Per-project results

The table shows the linux/arm64 run. The linux/amd64 run gave `identical` in every cell as well.

| Project | Ref | Go | Go files | Lines | embed | cgo | reflect | asm | vendor | T1,T2,T3 | T1 | T2 | T3 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| antonmedv/fx | 40.0.1 | 1.26.5 | 130 | 28031 | yes |  | yes |  |  | identical | identical | identical | identical |
| BurntSushi/toml | v1.6.0 | 1.26.5 | 30 | 10165 | yes |  | yes |  |  | identical | identical | identical | identical |
| caddyserver/caddy | v2.11.9 | 1.26.5 | 389 | 122475 | yes |  | yes |  |  | identical | identical | identical | identical |
| charmbracelet/glow | v3.0.0 | 1.26.5 | 25 | 3593 |  |  |  |  |  | identical | identical | identical | identical |
| charmbracelet/gum | v2.0.2 | 1.26.7 | 59 | 5758 |  |  | yes |  |  | identical | identical | identical | identical |
| cheat/cheat | 5.1.0 | 1.26.5 | 102 | 8867 |  |  | yes |  | yes | identical | identical | identical | identical |
| cli/cli | v2.104.0 | 1.27.4 | 1098 | 336032 | yes |  | yes |  | yes | identical | identical | identical | identical |
| cloudflare/cfssl | v1.7.1 | 1.27.0 | 254 | 59144 | yes | yes | yes | yes | yes | identical | identical | identical | identical |
| coredns/coredns | v1.15.0 | 1.26.5 | 763 | 117725 |  |  | yes |  |  | identical | identical | identical | identical |
| direnv/direnv | v2.38.2 | 1.26.5 | 69 | 6381 | yes |  | yes |  |  | identical | identical | identical | identical |
| dominikh/go-tools | 2026.2.2 | 1.26.5 | 1013 | 90289 |  | yes | yes |  | yes | identical | identical | identical | identical |
| dundee/gdu | v5.39.0 | 1.26.5 | 218 | 44032 | yes |  | yes |  |  | identical | identical | identical | identical |
| etcd-io/bbolt | v1.5.0 | 1.26.5 | 121 | 25045 |  |  | yes |  |  | identical | identical | identical | identical |
| ffuf/ffuf | v2.3.0 | 1.26.5 | 96 | 12859 |  |  | yes |  |  | identical | identical | identical | identical |
| FiloSottile/age | v1.3.2 | 1.27.0 | 47 | 11199 |  |  | yes |  |  | identical | identical | identical | identical |
| FiloSottile/mkcert | v1.4.4 | 1.26.5 | 7 | 1381 |  |  |  |  |  | identical | identical | identical | identical |
| gitleaks/gitleaks | v8.30.1 | 1.26.5 | 214 | 21579 | yes |  |  |  |  | identical | identical | identical | identical |
| go-delve/delve | v1.27.2 | 1.26.5 | 515 | 123741 | yes | yes | yes | yes | yes | identical | identical | identical | identical |
| go-task/task | v3.54.0 | 1.26.5 | 158 | 25309 | yes |  | yes |  |  | identical | identical | identical | identical |
| gohugoio/hugo | v0.170.0 | 1.27.0 | 914 | 234182 | yes |  | yes |  |  | identical | identical | identical | identical |
| gokcehan/lf | r42 | 1.26.5 | 34 | 14031 | yes |  | yes |  |  | identical | identical | identical | identical |
| golang-migrate/migrate | v4.20.1 | 1.26.5 | 142 | 23328 | yes |  | yes |  |  | identical | identical | identical | identical |
| google/go-containerregistry | v0.22.1 | 1.26.6 | 323 | 54072 |  |  | yes |  | yes | identical | identical | identical | identical |
| google/gops | v0.3.29 | 1.26.5 | 22 | 1692 |  |  | yes |  |  | identical | identical | identical | identical |
| google/pprof | HEAD | 1.26.5 | 88 | 31547 | yes |  | yes |  |  | identical | identical | identical | identical |
| grafana/k6 | v2.4.0 | 1.26.5 | 911 | 229054 | yes |  | yes |  | yes | identical | identical | identical | identical |
| hairyhenderson/gomplate | v5.3.1 | 1.27.3 | 219 | 30793 |  |  | yes |  |  | identical | identical | identical | identical |
| helm/helm | v4.3.2 | 1.26.5 | 542 | 106945 |  |  | yes |  |  | identical | identical | identical | identical |
| jesseduffield/lazydocker | v0.25.2 | 1.26.5 | 87 | 12548 |  |  | yes |  | yes | identical | identical | identical | identical |
| jesseduffield/lazygit | v0.66.3 | 1.26.5 | 1287 | 169847 | yes |  | yes |  | yes | identical | identical | identical | identical |
| junegunn/fzf | v0.75.0 | 1.26.5 | 91 | 34388 | yes |  | yes | yes |  | identical | identical | identical | identical |
| kubernetes-sigs/kind | v0.33.0 | 1.26.5 | 216 | 26328 |  |  | yes |  |  | identical | identical | identical | identical |
| mikefarah/yq | v4.54.3 | 1.26.5 | 278 | 47033 |  |  | yes |  |  | identical | identical | identical | identical |
| muesli/duf | v0.9.1 | 1.26.5 | 19 | 2947 |  |  | yes |  |  | identical | identical | identical | identical |
| mvdan/gofumpt | v0.12.0 | 1.26.5 | 28 | 9857 |  | yes | yes |  |  | identical | identical | identical | identical |
| mvdan/sh | v3.14.1 | 1.26.5 | 80 | 39926 |  |  | yes |  |  | identical | identical | identical | identical |
| nats-io/nats-server | v2.15.3 | 1.26.11 | 258 | 489821 |  | yes | yes |  |  | identical | identical | identical | identical |
| nektos/act | v0.2.97 | 1.26.10 | 147 | 33315 | yes |  | yes |  |  | identical | identical | identical | identical |
| OJ/gobuster | v3.8.2 | 1.26.5 | 54 | 10450 |  |  | yes |  |  | identical | identical | identical | identical |
| open-policy-agent/opa | v1.22.4 | 1.26.5 | 1049 | 468034 | yes |  | yes |  |  | identical | identical | identical | identical |
| peco/peco | v0.6.0 | 1.26.5 | 91 | 19391 |  |  | yes |  |  | identical | identical | identical | identical |
| pressly/goose | v3.30.1 | 1.26.5 | 148 | 17490 | yes |  | yes |  |  | identical | identical | identical | identical |
| projectdiscovery/httpx | v1.12.0 | 1.26.5 | 96 | 17545 | yes |  | yes |  |  | identical | identical | identical | identical |
| prometheus/node_exporter | v1.12.1 | 1.26.5 | 193 | 31970 |  | yes | yes |  |  | identical | identical | identical | identical |
| rakyll/hey | v0.1.5 | 1.26.5 | 8 | 1285 |  |  |  |  |  | identical | identical | identical | identical |
| restic/restic | v0.19.1 | 1.26.5 | 537 | 87262 |  |  | yes |  |  | identical | identical | identical | identical |
| schollz/croc | v11.6.5 | 1.27.3 | 232 | 58388 | yes |  | yes |  |  | identical | identical | identical | identical |
| spf13/cobra-cli | v1.3.0 | 1.26.5 | 21 | 3405 |  |  |  |  |  | identical | identical | identical | identical |
| tomnomnom/gron | v0.7.1 | 1.26.5 | 12 | 2725 |  |  | yes |  |  | identical | identical | identical | identical |
| traefik/yaegi | v0.16.3 | 1.26.5 | 1601 | 252435 | yes |  | yes |  | yes | identical | identical | identical | identical |
| tsenart/vegeta | v12.13.0 | 1.26.5 | 45 | 21707 | yes |  | yes |  |  | identical | identical | identical | identical |
| wagoodman/dive | v0.13.1 | 1.26.5 | 85 | 14197 |  |  |  |  |  | identical | identical | identical | identical |
| yuin/gopher-lua | v1.1.2 | 1.26.5 | 42 | 20025 |  |  | yes |  |  | identical | identical | identical | identical |
| zyedidia/micro | v2.0.15 | 1.26.5 | 94 | 25053 | yes |  | yes |  |  | identical | identical | identical | identical |
