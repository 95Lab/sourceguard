<div align="center">

# SourceGuard

**Keeping source code confidential even when the compiler is compromised.**

[![Status](https://img.shields.io/badge/status-research%20draft%20v0.2-orange)](docs/architecture.md)
[![Code License: Apache-2.0](https://img.shields.io/badge/code-Apache--2.0-blue)](LICENSE)
[![Docs License: CC BY 4.0](https://img.shields.io/badge/docs-CC%20BY%204.0-lightgrey)](LICENSE-docs)
[![Cite this work](https://img.shields.io/badge/cite-CITATION.cff-green)](CITATION.cff)

A research project by **[95LAB](https://github.com/95Lab)**

</div>

---

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Project Status](#project-status)
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

SourceGuard introduces **Differential Source Variants (DSV)**, a technique that repurposes reproducible builds, traditionally an *integrity* mechanism, as a *confidentiality* leak detector.

**Security goals**

| ID | Goal |
|---|---|
| G1 | Code execution inside the compiler cannot exfiltrate source information beyond what is invariant across all variants. |
| G2 | Full compromise of the Git server, CI orchestrator, database, or storage does not reveal plaintext. |
| G3 | Every released artifact carries verifiable provenance and has passed a cross-cell divergence check. |

## How It Works

1. A small, trusted **Variant Generator** produces `k` source variants that are textually different but semantically equivalent (rewritten comments, whitespace changes, renamed local identifiers).
2. Each variant is compiled in an isolated, single-use, network-less **build cell**.
3. An honest compiler produces **bit-identical** artifacts from all variants.
4. A trusted **Artifact Gateway** compares the artifacts byte for byte. A compromised compiler that embeds any information depending on the differing parts causes divergence, and the release is blocked.

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

This repository currently contains the **architecture and research design (v0.2)**. The invariance hypotheses are **not yet validated**; Phase 1 of the roadmap exists to test them.

| Phase | Scope | Status |
|---|---|---|
| 0 | Skeleton: `sg seal/push`, signed manifest, minimal Key Broker | Planned |
| 1 | Invariance validation of transformations T1–T6 on a Go corpus | Planned |
| 2 | Full pipeline: Variant Generator, build cells, gateway, release signer | Planned |
| 3 | Adversarial evaluation (attack suite X1–X11) | Planned |
| 4 | C/C++/Rust, SEV-SNP/TDX attestation, diverse toolchains | Optional |

## Documentation

| Document | Description |
|---|---|
| [Architecture (English)](docs/architecture.md) | Full design: threat model, components, DSV, leakage analysis, roadmap |
| [Kiến trúc (Tiếng Việt)](docs/architecture.vi.md) | Original Vietnamese version |

Key sections: [Threat Model](docs/architecture.md#2-threat-model) · [DSV Transformations](docs/architecture.md#5-differential-source-variants-dsv) · [Residual Leakage](docs/architecture.md#7-residual-leakage-channels-and-mitigations) · [Attack Suite](docs/architecture.md#10-adversarial-attack-suite-attacks) · [Research Questions](docs/architecture.md#11-research-questions)

## Citation

If you use SourceGuard, the DSV technique, or any part of this repository (code, design, text, figures, or ideas) in software, research, publications, products, or derivative work, **you must cite it**:

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

**95LAB** is the research and development unit of, focused on new solutions and emerging technologies in:

- **Data Science**
- **Artificial Intelligence**
- **Security**

The team is led and operated by **Dang-Hieu Nguyen**, Head of 95LAB.

## License

| Scope | License |
|---|---|
| Source code | [Apache License 2.0](LICENSE). Redistributions must retain the [NOTICE](NOTICE) file. |
| Documentation and design (`docs/`, README, figures) | [Creative Commons Attribution 4.0 International](LICENSE-docs). Attribution is required. |

## Contributing and Security

- **Contributions:** see [CONTRIBUTING.md](CONTRIBUTING.md).
- **Vulnerabilities:** please report privately; see [SECURITY.md](SECURITY.md).

## Contact

**Dang-Hieu Nguyen**, Head of 95LAB

hillnguyen528@gmail.com

---

<div align="center">
Copyright © 2026 95LAB.
</div>
