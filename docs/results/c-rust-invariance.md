# Invariance Results for C, C++ and Rust

Date: 2026-11-15. This report extends the invariance study from Go ([Phase 1](phase1-invariance.md), [evaluation](evaluation.md)) to C and C++ with GCC and Clang, and to Rust.

For these languages the Variant Generator works at the lexical level and implements two transformations: **T1** (comment content) and **T2** (whitespace at line ends). There is no renaming, so only comment text is withheld from the compiler.

## Summary

All numbers are from one run of the whole corpus with the final generator, natively on linux/amd64.

| Compiler | Projects built reproducibly | T1 + T2 bit-identical | T1 | T2 | Variants built |
|---|---|---|---|---|---|
| GCC 14.2.0 | 26 of 28 | 25 / 26 | 25 / 26 | 25 / 26 | 364 |
| Clang 19.1.7 | 26 of 28 | 26 / 26 | 26 / 26 | 25 / 26 | 364 |
| rustc 1.100.0 | 30 of 30 | 30 / 30 | 30 / 30 | 30 / 30 | 240 |

- The C and C++ corpus is 26 projects with 4.3 million lines in 8,363 C and C++ files, from single-file programs to `git`, `curl`, `vim` and `sqlite`. The Rust corpus is 30 projects with 0.55 million lines.
- **The only exception is `redis`**, and the cause is its build scripts, not a compiler ([Finding 2](#finding-2-build-scripts-that-read-the-source)). Its two passes with Clang (T1 + T2, T1) depend on a timing-dependent Makefile rule and should not be counted as robust.
- **Control (T2X): padding whitespace inside lines.** For Rust it changed the artifact in **30 of 30** projects. For C and C++ it left the artifact unchanged in 23 of 26 projects and broke the build of two others. T2 therefore never moves a token ([Finding 1](#finding-1-source-columns-matter-in-both-languages)).
- Two projects (`jq`, `busybox`) were not measured because their build recipes failed in the test image (a missing Git submodule and a missing `bzip2`).

## Language rules

Common to both: T1 replaces comment text with random letters and digits of the same byte length, keeping line breaks, backslashes and non-ASCII bytes. T2 appends zero to two spaces at the end of a line and changes nothing else.

**C and C++**

- Preprocessor directives are left alone, except for comments on `#define`, `#if`, `#else`, `#endif` and related lines.
- Nothing is appended after a backslash at the end of a line.
- Comments the compiler interprets (`fall through` and similar) are kept.
- String literals continued across lines with a backslash are left intact.
- Files named `*.in.*` are skipped: they are templates that configure-time scripts rewrite.
- Builds use `-g0 -frandom-seed=sg`, `SOURCE_DATE_EPOCH=0`, and the artifact is stripped.

**Rust**

- Doc comments (`///`, `//!`, `/** */`, `/*! */`) are kept: they are attributes that macros can read.
- Files pulled in with `include_str!` or `include_bytes!` are data and are not transformed.
- Builds use the project's release profile with `debug = 0` and `strip = true`.

## Method

For each project and compiler: build the unmodified source twice and require identical artifacts (projects that fail this are not measured); then build variants per transformation set and count distinct artifacts. C and C++ projects are rebuilt from a fresh copy of the tree for every build. Rust projects keep Cargo's target directory, so dependencies are compiled once, and every source file is touched before each build so that all first-party crates are recompiled.

Variants per set, C and C++: 6 for T1 + T2, 4 for T1, 4 for T2, 2 for T2X. Rust: 4, 2, 2, 2.

Each project is built with the command in the corpus list, at the tip of its default branch on the day of the run; commits are in the tables below.

## Findings

### Finding 1: source columns matter in both languages

**Rust.** Panic locations record file, line and column, so any change of whitespace before a token on the same line changes the binary. The control T2X produced a different artifact for every variant in all 30 projects.

**C and C++.** Columns do not reach a stripped binary: T2X was bit-identical in 23 of 26 projects. But they steer diagnostics. `nginx` compiles with `-Werror`, and changed indentation triggered `-Wmisleading-indentation` (GCC) and `-Wempty-body` (Clang), so the build failed. `sqlite` failed because its scripts that generate the opcode table and the amalgamation match source lines textually.

Consequence: T2 only appends at line ends. With that rule `nginx` and `sqlite` are bit-identical with both compilers. A future renaming transformation for these languages would have to keep the length of every name.

### Finding 2: build scripts that read the source

`redis` is not invariant under either transformation, for two independent reasons, neither involving the compiler:

- **A count derived from a textual diff is compiled in (T2).** `src/mkreleasehdr.sh` records the number of uncommitted changes with `git diff -- ../src ../deps | wc -l`. Outside a Git repository this command compares the two directories with each other, and its output length depends on file contents (521,357 lines for one variant, 521,359 for another). The number is written to `release.h` and ends up in the binary. A no-change control gave identical artifacts in 8 of 8 builds; T2 gave 4 to 5 distinct artifacts in 8.
- **A comment is used as a marker (T1).** The Makefile regenerates `src/fmtargs.h` by cutting the file at the comment "Everything below this line is automatically generated". With the comment rewritten, the generated part is duplicated and compilation fails. The rule fires only when the generator script is newer than the header, which depends on file timestamps; it fired in the GCC run and not in the Clang run.

This is a limit of lexical variants: where a build step other than the compiler interprets comments or layout, a transformation changes that step's input. Such files or steps have to be excluded per project.

### Findings 3 to 5: defects of the generator, fixed

Found in earlier runs and fixed before the run reported here:

- **String literals continued across lines.** The whitespace at the start of the continuation line belongs to the string; the first lexer treated it as layout.
- **A rewritten comment could swallow the next line.** GCC and Clang treat a backslash followed by blanks and a line break as a line continuation. Replacement text containing spaces could turn `// prints \\n` into a comment ending in backslash-blank, pulling the next line of code into the comment. It caused occasional build errors and once a silently different binary. The replacement alphabet no longer contains whitespace.
- **Configure templates.** `ctags` bundles gnulib headers generated from `*.in.h` templates by `sed` scripts that look for marker comments.

## Limitations

- **No renaming.** Only comment text is hidden for these languages. A compiler that strips comments and embeds the rest is not detected.
- **Few variants per project** (2 to 6 per set). The comment defect above triggered in a small fraction of variants and passed one earlier run unnoticed; a defect of similar rarity could remain.
- **Excluded projects.** `jq` and `busybox` were not measured, for reasons unrelated to the transformations.
- One compiler version each, one platform, one optimization configuration per project (the project's own). No debug-info builds.
- Projects are taken at the tip of their default branch, not at releases.

## Data

> The tooling referred to in this report is part of the SourceGuard implementation, which is not public. The method, the corpus and the raw data are published here.

Raw results: [`eval-c-rust.json`](eval-c-rust.json). Corpus and build commands: [`corpus-c-rust.txt`](corpus-c-rust.txt).

## C and C++ per project

| Project | Commit | Files | Lines | GCC: T1+T2 | T1 | T2 | T2X | Clang: T1+T2 | T1 | T2 | T2X |
|---|---|---|---|---|---|---|---|---|---|---|---|
| antirez/kilo | `323d93b29b` | 1 | 1308 | identical | identical | identical | identical | identical | identical | identical | identical |
| curl/curl | `9637d6b844` | 1073 | 301211 | identical | identical | identical | identical | identical | identical | identical | identical |
| DaveGamble/cJSON | `6d9f2443ab` | 99 | 22419 | identical | identical | identical | identical | identical | identical | identical | identical |
| facebook/zstd | `a599a71c4c` | 279 | 144631 | identical | identical | identical | identical | identical | identical | identical | identical |
| fmtlib/fmt | `dcdec038ab` | 75 | 70908 | identical | identical | identical | identical | identical | identical | identical | identical |
| gabime/spdlog | `6d9805df3f` | 153 | 33270 | identical | identical | identical | identical | identical | identical | identical | identical |
| git/git | `7fb6055e67` | 990 | 447084 | identical | identical | identical | identical | identical | identical | identical | identical |
| google/brotli | `6a14c07982` | 131 | 47803 | identical | identical | identical | identical | identical | identical | identical | identical |
| google/leveldb | `29465588d3` | 133 | 28971 | identical | identical | identical | identical | identical | identical | identical | identical |
| google/snappy | `9466064593` | 18 | 7945 | identical | identical | identical | identical | identical | identical | identical | identical |
| htop-dev/htop | `21bbc80c78` | 283 | 48950 | identical | identical | identical | identical | identical | identical | identical | identical |
| jbeder/yaml-cpp | `f473f3bdfe` | 274 | 122523 | identical | identical | identical | identical | identical | identical | identical | identical |
| libuv/libuv | `3febcb8561` | 379 | 119090 | identical | identical | identical | identical | identical | identical | identical | identical |
| lua/lua | `c42d820bc0` | 68 | 34216 | identical | identical | identical | identical | identical | identical | identical | identical |
| lz4/lz4 | `6b589e8053` | 80 | 38598 | identical | identical | identical | identical | identical | identical | identical | identical |
| madler/zlib | `99623ef709` | 79 | 43250 | identical | identical | identical | identical | identical | identical | identical | identical |
| memcached/memcached | `2d51e36479` | 96 | 52357 | identical | identical | identical | identical | identical | identical | identical | identical |
| nginx/nginx | `2a3a19c6fd` | 411 | 258031 | identical | identical | identical | **build error** (2/2) | identical | identical | identical | **build error** (2/2) |
| ninja-build/ninja | `1a8fee2313` | 127 | 35017 | identical | identical | identical | identical | identical | identical | identical | identical |
| redis/redis | `d92afbeb85` | 856 | 430937 | **build error** (6/6) | **build error** (4/4) | **divergent** (3/4) | **divergent** (2/2) | identical | identical | **divergent** (3/4) | **divergent** (2/2) |
| simdjson/simdjson | `a0d5ad9798` | 588 | 447674 | identical | identical | identical | identical | identical | identical | identical | identical |
| sqlite/sqlite | `e5cb4b0957` | 359 | 464612 | identical | identical | identical | **build error** (2/2) | identical | identical | identical | **build error** (2/2) |
| TinyCC/tinycc | `f830a1697e` | 352 | 130737 | identical | identical | identical | identical | identical | identical | identical | identical |
| tmux/tmux | `75fac4a77b` | 217 | 122936 | identical | identical | identical | identical | identical | identical | identical | identical |
| universal-ctags/ctags | `394badef3b` | 937 | 235130 | identical | identical | identical | identical | identical | identical | identical | identical |
| vim/vim | `a4e0c637b9` | 305 | 626187 | identical | identical | identical | identical | identical | identical | identical | identical |

## Rust per project

| Project | Commit | Files | Lines | T1+T2 | T1 | T2 | T2X (control) |
|---|---|---|---|---|---|---|---|
| ajeetdsouza/zoxide | `1bb062057b` | 25 | 2803 | identical | identical | identical | **divergent** (2/2) |
| bootandy/dust | `079c552669` | 17 | 5043 | identical | identical | identical | **divergent** (2/2) |
| BurntSushi/ripgrep | `178cc027e2` | 102 | 55512 | identical | identical | identical | **divergent** (2/2) |
| Byron/dua-cli | `a6d1a98aea` | 63 | 32886 | identical | identical | identical | **divergent** (2/2) |
| Canop/broot | `deed4752b1` | 239 | 41505 | identical | identical | identical | **divergent** (2/2) |
| casey/just | `62784c2a6e` | 277 | 66694 | identical | identical | identical | **divergent** (2/2) |
| chmln/sd | `44febdf863` | 13 | 2125 | identical | identical | identical | **divergent** (2/2) |
| ClementTsang/bottom | `a5da65c637` | 179 | 41004 | identical | identical | identical | **divergent** (2/2) |
| dalance/procs | `7ffb8c4f2e` | 105 | 16756 | identical | identical | identical | **divergent** (2/2) |
| dandavison/delta | `02ff175b7b` | 85 | 34484 | identical | identical | identical | **divergent** (2/2) |
| ducaale/xh | `8b21cc07a6` | 33 | 16612 | identical | identical | identical | **divergent** (2/2) |
| extrawurst/gitui | `d7214eccb3` | 162 | 50369 | identical | identical | identical | **divergent** (2/2) |
| eza-community/eza | `471bfbc7b0` | 72 | 18575 | identical | identical | identical | **divergent** (2/2) |
| lsd-rs/lsd | `4b6c14a110` | 54 | 14270 | identical | identical | identical | **divergent** (2/2) |
| orf/gping | `fad1ee3c02` | 15 | 2171 | identical | identical | identical | **divergent** (2/2) |
| ouch-org/ouch | `6df14b05ea` | 38 | 9334 | identical | identical | identical | **divergent** (2/2) |
| PaulJuliusMartinez/jless | `e6cdef719c` | 17 | 11295 | identical | identical | identical | **divergent** (2/2) |
| pemistahl/grex | `99cc347707` | 27 | 12937 | identical | identical | identical | **divergent** (2/2) |
| rust-lang/mdBook | `b1535b7803` | 102 | 18096 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/bat | `311969b1f1` | 67 | 19680 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/diskus | `90196e9500` | 6 | 498 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/fd | `6cf330283c` | 25 | 9253 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/hexyl | `6ecc29b9c8` | 7 | 3380 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/hyperfine | `c516152498` | 45 | 8802 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/pastel | `01d4252aa7` | 39 | 7275 | identical | identical | identical | **divergent** (2/2) |
| svenstaro/miniserve | `68b6f063cd` | 34 | 8640 | identical | identical | identical | **divergent** (2/2) |
| tealdeer-rs/tealdeer | `993a8e0232` | 11 | 4963 | identical | identical | identical | **divergent** (2/2) |
| theryangeary/choose | `f1c53eef57` | 15 | 2479 | identical | identical | identical | **divergent** (2/2) |
| watchexec/watchexec | `f71af3b18d` | 115 | 29089 | identical | identical | identical | **divergent** (2/2) |
| XAMPPRocky/tokei | `c14f744716` | 25 | 5025 | identical | identical | identical | **divergent** (2/2) |
