# Invariance Results for C, C++ and Rust

Date: 2026-11-15. This report extends the invariance study from Go ([Phase 1](phase1-invariance.md), [evaluation](evaluation.md)) to C and C++ with GCC and Clang, and to Rust.

For these languages the Variant Generator works at the lexical level and implements two transformations: **T1** (comment content) and **T2** (whitespace). There is no renaming, so only comment text is withheld from the compiler.

## Summary

| Compiler | Projects built reproducibly | T1 + T2 bit-identical | T1 | T2 |
|---|---|---|---|---|
| GCC 14.2.0 | 26 of 28 | 24 / 26 | 25 / 26 | 25 / 26 |
| Clang 19.1.7 | 26 of 28 | 25 / 26 | 26 / 26 | 25 / 26 |
| rustc 1.100.0 | 29 of 30 | 29 / 29 | 29 / 29 | 29 / 29 |

- The C and C++ corpus is 26 projects with about 4.3 million lines in C and C++ files, from single-file programs to `git`, `curl`, `vim` and `sqlite`.
- The two C exceptions are `redis` (T1, GCC run only) and `sqlite` (T2, both compilers). In both, a build script processes the source text; the compiler is not involved. See [Finding 2](#finding-2-build-systems-that-read-the-source).
- For Rust, a control that changes whitespace *inside* lines (T2X) changed the artifact in **29 of 29** projects. See [Finding 1](#finding-1-rust-source-columns-are-in-the-binary).
- These numbers describe the generator after three defects found during the study were fixed ([Findings 3 to 5](#finding-3-string-literals-continued-across-lines)). The data comes from three runs, which is a weakness of this report; see [Limitations](#limitations).

## Language rules

**C and C++**

- T2 only lengthens whitespace that already exists. It never inserts whitespace between tokens, because the `#` operator turns macro arguments into string literals and would record the difference.
- Preprocessor directives are left alone, except for comments on `#define`, `#if`, `#else`, `#endif` and related lines.
- Comments the compiler interprets (`fall through` and similar) are kept.
- Files named `*.in.*` are skipped: they are templates that configure-time scripts rewrite.
- Builds use `-g0 -frandom-seed=sg`, `SOURCE_DATE_EPOCH=0`, and the artifact is stripped.

**Rust**

- T2 only appends whitespace at the end of a line.
- Doc comments (`///`, `//!`, `/** */`, `/*! */`) are kept: they are attributes that macros can read.
- Files pulled in with `include_str!` or `include_bytes!` are data and are not transformed.
- Builds use the project's release profile with `debug = 0` and `strip = true`.

## Method

For each project and compiler: build the unmodified source twice and require identical artifacts (projects that fail this are not measured); then build variants per transformation set and count distinct artifacts. C and C++ projects are rebuilt from a fresh copy of the tree for every build. Rust projects keep Cargo's target directory, so dependencies are compiled once, and every source file is touched before each build so that all first-party crates are recompiled.

Variants per set: 4 for T1 + T2, 2 for T1 and for T2 (4 for T1 in the final rerun), 2 for T2X.

Each project is built with the command in [`corpus-c-rust.txt`](corpus-c-rust.txt), at the tip of its default branch on the day of the run.

## Findings

### Finding 1: Rust source columns are in the binary

Panic locations in Rust record file, line **and column**. Any whitespace change before a token on the same line therefore changes the binary. The control T2X, which pads whitespace inside lines the way T2 does for C, produced a different artifact for every variant in all 29 projects.

Consequences: for Rust, whitespace can only vary at line ends, and a future renaming transformation would have to keep every name's length.

### Finding 2: build systems that read the source

- **`redis`.** The Makefile regenerates `src/fmtargs.h` by cutting the file at a marker comment ("Everything below this line is automatically generated") and appending fresh content. With the comment rewritten, the cut does not happen, the generated part is duplicated, and the build fails. The rule only fires when the generator script is newer than the header, which depends on the order in which files were written; that is why it failed in the GCC run and not in the Clang run.
- **`sqlite`.** With T2 the build fails in `sqlite3.c`, the amalgamation that the build generates from the sources (`OP_...` opcodes undeclared). The scripts that generate the opcode table and the amalgamation match source lines textually. The exact pattern was not identified.

This is a limit of lexical variants, not of a particular compiler: where a build step other than the compiler interprets comments or layout, the transformation changes that step's input. Affected files have to be excluded per project.

### Finding 3: string literals continued across lines

A C string literal can be continued with a backslash at the end of a line; the leading whitespace of the next line is then part of the string. The first version of the lexer ended the literal at the line break and padded that whitespace. Fixed.

### Finding 4: a rewritten comment could swallow the next line

GCC and Clang treat a backslash followed by blanks and a line break as a line continuation. Comment text was replaced by random letters, digits and spaces, with backslashes kept. A comment such as `// prints \n` could therefore become `// xyz \ ` and pull the following line of code into the comment. This caused occasional build errors and, once, a silently different binary (`vim`, `memcached`). The replacement alphabet no longer contains whitespace.

### Finding 5: configure templates

`ctags` bundles gnulib, whose headers are produced from `*.in.h` templates by `sed` scripts that look for marker comments. Such templates are now skipped.

## Limitations

- **Three runs, not one.** The first run (all projects, linux/amd64 on Modal) used the generator before Findings 3 to 5 were fixed. A second run repeated the C and C++ projects with Findings 3 and 5 fixed; six of its jobs were lost when the Modal workspace was suspended. The five projects that had shown problems were then rerun locally (linux/arm64, Docker) with all fixes. The column "Measured" below says which run each row comes from. A single clean rerun of the whole corpus with the final generator has not been done.
- **`curl` and all Rust rows come from the first generator.** `curl` was bit-identical there. The later fixes do not affect the Rust rules, apart from the comment alphabet.
- **Few variants per project** (2 to 4 per set). Finding 4 shows that a defect which triggers in a small fraction of variants can pass a run unnoticed.
- **Result files.** Because of a harness defect, six per-job result files of the Modal runs were overwritten by another job's file. Outcomes for those jobs were recovered from the run logs; their file and line counts are missing.
- **Excluded projects.** `jq` and `busybox` (both compilers) and `jless` did not build in the test image. The cause is the build recipe or missing system libraries, not the transformations.
- **No renaming.** Only comment text is hidden for these languages. A compiler that strips comments and embeds the rest (attack X3) is not detected.
- One optimization level and one platform per run; no LTO-specific or debug-info builds.

## Data

> The tooling referred to in this report is part of the SourceGuard implementation, which is not public. The method, the corpus and the raw data are published here; see the [README](../../README.md#availability).

Consolidated data: [`eval-c-rust.json`](eval-c-rust.json).

## C and C++ per project

| Project | Files | Lines | GCC: T1+T2 | T1 | T2 | Clang: T1+T2 | T1 | T2 | Measured |
|---|---|---|---|---|---|---|---|---|---|
| antirez/kilo | 1 | 1308 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| curl/curl | 1073 | 301211 | identical | identical | identical | identical | identical | identical | Modal, amd64, first generator |
| DaveGamble/cJSON | 99 | 22419 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| facebook/zstd | 279 | 144631 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| fmtlib/fmt | 75 | 70908 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| gabime/spdlog | 153 | 33270 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| git/git | 990 | 447084 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| google/brotli | 131 | 47803 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| google/leveldb | 133 | 28971 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| google/snappy | 18 | 7945 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| htop-dev/htop | 283 | 48950 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| jbeder/yaml-cpp | 274 | 122523 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| libuv/libuv | 379 | 119090 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| lua/lua | 68 | 34216 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| lz4/lz4 | 80 | 38598 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| madler/zlib | 79 | 43250 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| memcached/memcached | 96 | 52357 | identical | identical | identical | identical | identical | identical | local, arm64 |
| nginx/nginx | 411 | 258031 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| ninja-build/ninja | 127 | 35017 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| redis/redis | 856 | 430937 | **build error** (4/4) | **build error** (4/4) | identical | identical | identical | identical | local, arm64 |
| simdjson/simdjson | 588 | 447674 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| sqlite/sqlite | 359 | 464612 | **build error** (4/4) | identical | **build error** (2/2) | **build error** (4/4) | identical | **build error** (2/2) | local, arm64 |
| TinyCC/tinycc | 352 | 130737 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| tmux/tmux | 217 | 122936 | identical | identical | identical | identical | identical | identical | Modal, amd64 |
| universal-ctags/ctags | 937 | 235130 | identical | identical | identical | identical | identical | identical | local, arm64 |
| vim/vim | 305 | 626187 | identical | identical | identical | identical | identical | identical | local, arm64 |

## Rust per project

| Project | Files | Lines | T1+T2 | T1 | T2 | T2X (control) |
|---|---|---|---|---|---|---|
| ajeetdsouza/zoxide | 25 | 2803 | identical | identical | identical | **divergent** (2/2) |
| bootandy/dust | 17 | 5043 | identical | identical | identical | **divergent** (2/2) |
| BurntSushi/ripgrep | 102 | 55512 | identical | identical | identical | **divergent** (2/2) |
| Byron/dua-cli | 63 | 32886 | identical | identical | identical | **divergent** (2/2) |
| Canop/broot | 239 | 41505 | identical | identical | identical | **divergent** (2/2) |
| casey/just | 277 | 66694 | identical | identical | identical | **divergent** (2/2) |
| chmln/sd | 13 | 2125 | identical | identical | identical | **divergent** (2/2) |
| ClementTsang/bottom | n/a | n/a | identical | identical | identical | **divergent** (2/2) |
| dalance/procs | 105 | 16756 | identical | identical | identical | **divergent** (2/2) |
| dandavison/delta | 85 | 34484 | identical | identical | identical | **divergent** (2/2) |
| ducaale/xh | 33 | 16612 | identical | identical | identical | **divergent** (2/2) |
| extrawurst/gitui | 162 | 50369 | identical | identical | identical | **divergent** (2/2) |
| eza-community/eza | n/a | n/a | identical | identical | identical | **divergent** (2/2) |
| lsd-rs/lsd | 54 | 14270 | identical | identical | identical | **divergent** (2/2) |
| orf/gping | 15 | 2171 | identical | identical | identical | **divergent** (2/2) |
| ouch-org/ouch | 38 | 9334 | identical | identical | identical | **divergent** (2/2) |
| pemistahl/grex | 27 | 12937 | identical | identical | identical | **divergent** (2/2) |
| rust-lang/mdBook | 102 | 18096 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/bat | 67 | 19680 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/diskus | 6 | 498 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/fd | n/a | n/a | identical | identical | identical | **divergent** (2/2) |
| sharkdp/hexyl | 7 | 3380 | identical | identical | identical | **divergent** (2/2) |
| sharkdp/hyperfine | n/a | n/a | identical | identical | identical | **divergent** (2/2) |
| sharkdp/pastel | 39 | 7275 | identical | identical | identical | **divergent** (2/2) |
| svenstaro/miniserve | n/a | n/a | identical | identical | identical | **divergent** (2/2) |
| tealdeer-rs/tealdeer | 11 | 4963 | identical | identical | identical | **divergent** (2/2) |
| theryangeary/choose | 15 | 2479 | identical | identical | identical | **divergent** (2/2) |
| watchexec/watchexec | 115 | 29089 | identical | identical | identical | **divergent** (2/2) |
| XAMPPRocky/tokei | 25 | 5025 | identical | identical | identical | **divergent** (2/2) |
