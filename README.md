<div align="center">

# SourceGuard

**Keeping source code confidential even when the compiler is compromised.**

[![Status](https://img.shields.io/badge/status-research%20prototype-orange)](docs/architecture.md)
[![Docs License: CC BY 4.0](https://img.shields.io/badge/docs-CC%20BY%204.0-lightgrey)](LICENSE)
[![Cite this work](https://img.shields.io/badge/cite-CITATION.cff-green)](CITATION.cff)

A research project by **[95LAB](https://github.com/95Lab)**, an independent, non-profit research group

</div>

---

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Project Status](#project-status)
- [Availability](#availability)
- [Documentation](#documentation)
- [Citation](#citation)
- [About 95LAB](#about-95lab)
- [License](#license)
- [Contributing and Security](#contributing-and-security)
- [Contact](#contact)

---

## Overview

Modern build pipelines assume the compiler is honest. SourceGuard addresses a harder question:

> *If an attacker gains arbitrary code execution inside the compiler, for example through a malicious dependency that exploits a compiler bug, can they exfiltrate the source code being built?*

SourceGuard introduces **Differential Source Variants (DSV)**. A trusted generator removes what the binary does not depend on (comment text, local and package-level names) and replaces it with random values, differently for each build cell. The compiler therefore never receives that part of the source, and reproducibility across the differently scrubbed copies checks that the toolchain behaved honestly.

What this does and does not guarantee is stated precisely in the [security argument](docs/security-argument.md). In short: comments and renamed identifiers are provably withheld from the compiler; the rest of the source is not protected against a toolchain that is compromised in every cell.

**Security goals**

| ID | Goal |
|---|---|
| G1 | Code execution inside the compiler learns nothing about the parts of the source that the Variant Generator replaces (comments, renamed identifiers). |
| G2 | Full compromise of the Git server, CI orchestrator, database, or storage does not reveal plaintext. |
| G3 | Every released artifact carries verifiable provenance and has passed a cross-cell divergence check. |

## How It Works

1. A small, trusted **Variant Generator** produces `k` source variants that are textually different but semantically equivalent (rewritten comments, whitespace changes, renamed local identifiers).
2. Each variant is compiled in an isolated, single-use, network-less **build cell**.
3. An honest compiler produces **bit-identical** artifacts from all variants.
4. A trusted **Artifact Gateway** compares the artifacts byte for byte. A compiler whose output depends on the differing parts causes divergence, and the release is blocked. If at least one cell is honest, an accepted release is exactly the honest build.

```
Developer ──► Forgejo (ciphertext only) ──► Orchestrator (untrusted)
                                               │
                         Key Broker ◄──────────┤
                                               ▼
                                     Variant Generator (TCB)
                                     ┌─────────┼─────────┐
                                     ▼         ▼         ▼
                                  Cell V1   Cell V2 … Cell Vk   (untrusted compiler)
                                     └─────────┼─────────┘
                                               ▼
                                     Artifact Gateway (TCB) ──► Release Signer
```

## Project Status

A prototype implements the full pipeline for Go, with lexical variants for C, C++ and Rust. It is a research test bed, not yet a product.

| Area | Status |
|---|---|
| Encrypted source store, signed manifest, Key Broker | Prototype |
| Variant Generator | Go: five transformations. C, C++, Rust: comments and whitespace |
| Build cells, Artifact Gateway, Release Signer | Prototype; cells run as containers or Firecracker microVMs |
| Invariance on real projects | Go: bit-identical on 54 / 54 projects, two architectures ([results](docs/results/phase1-invariance.md)). C/C++: 25–26 of 26 with GCC and Clang; Rust: 30 of 30 ([results](docs/results/c-rust-invariance.md)) |
| What remains exposed | Measured, including against a decompiler ([results](docs/results/evaluation.md)), and stated formally ([security argument](docs/security-argument.md)) |
| Self-hosting with Forgejo, multi-module builds, microVM cells | Working end to end on one host ([overview](docs/self-hosting.md)) |
| Dependencies between modules (interface-only, or scrubbed source for Go) | Working |
| Hardware attestation (SEV-SNP/TDX), diverse toolchains | Not started |

## Availability

This repository publishes the architecture, the security argument and the experimental results, including raw data and the lists of projects measured. The evaluation tools are provided as prebuilt programs in [`artifact/`](artifact/README.md), so that the experiments can be rerun. The source code of the implementation (Variant Generator, pipeline components and tooling) is maintained privately by 95LAB. For access, collaboration or deployment, contact hillnguyen528@gmail.com.

## Documentation

| Document | Description |
|---|---|
| [Architecture (English)](docs/architecture.md) | Full design: threat model, components, DSV, leakage analysis, roadmap |
| [Phase 1 Results](docs/results/phase1-invariance.md) | Invariance study on 54 open-source Go projects: method, findings, limitations |
| [Evaluation Results](docs/results/evaluation.md) | Dead code (T4), package-level renaming (T7), cgo, invariant information and structure (RQ2), cost |
| [C, C++ and Rust Results](docs/results/c-rust-invariance.md) | Invariance with GCC, Clang and rustc; language rules; findings |
| [Self-Hosting Overview](docs/self-hosting.md) | What a deployment looks like, who sees what, comparison with an ordinary Git service |
| [Evaluation Artifact](artifact/README.md) | Prebuilt tools and instructions to rerun the experiments |
| [Security Argument](docs/security-argument.md) | Model, assumptions, two theorems, and what is not protected |
| [Related Work and Positioning](docs/related-work.md) | TightLip, Secure Multi-Execution, N-variant systems, reproducible and attested builds |
| [Kiến trúc (Tiếng Việt)](docs/architecture.vi.md) | Original Vietnamese version |

Key sections: [Threat Model](docs/architecture.md#2-threat-model) · [DSV Transformations](docs/architecture.md#5-differential-source-variants-dsv) · [Residual Leakage](docs/architecture.md#7-residual-leakage-channels-and-mitigations) · [Attack Suite](docs/architecture.md#10-adversarial-attack-suite-attacks) · [Research Questions](docs/architecture.md#11-research-questions)

## Citation

If you use SourceGuard, the DSV technique, or any part of this repository (design, text, figures, data, or ideas) in software, research, publications, products, or derivative work, **you must cite it**:

> Dang-Hieu Nguyen. *SourceGuard: Differential Source Variants for Build Confidentiality under Compiler Compromise.* 95LAB, 2026. https://github.com/95Lab/sourceguard

```bibtex
@misc{nguyen2026sourceguard,
  author       = {Nguyen, Dang-Hieu},
  title        = {{SourceGuard}: Differential Source Variants for Build Confidentiality under Compiler Compromise},
  year         = {2026},
  organization = {95LAB},
  howpublished = {\url{https://github.com/95Lab/sourceguard}},
  note         = {Contact: hillnguyen528@gmail.com}
}
```

GitHub's **"Cite this repository"** button (generated from [CITATION.cff](CITATION.cff)) provides APA and BibTeX formats. Any redistribution must retain the attribution in [NOTICE](NOTICE).

## About 95LAB

**95LAB** is an independent, non-profit research group. It is not affiliated with any company. It studies new solutions and emerging technologies in:

- **Data Science**
- **Artificial Intelligence**
- **Security**

The group is led and operated by **Dang-Hieu Nguyen**.

## License

| Scope | License |
|---|---|
| Documents, figures and data | [Creative Commons Attribution 4.0 International](LICENSE). Attribution is required. |
| Programs in `artifact/bin` | Provided for evaluating and reproducing the results; all rights reserved. See [`artifact/README.md`](artifact/README.md#terms). |

## Contributing and Security

- **Contributions:** see [CONTRIBUTING.md](CONTRIBUTING.md).
- **Vulnerabilities:** please report privately; see [SECURITY.md](SECURITY.md).

## Contact

**Dang-Hieu Nguyen**, 95LAB
hillnguyen528@gmail.com

---

<div align="center">
Copyright © 2026 Dang-Hieu Nguyen (95LAB).
</div>
