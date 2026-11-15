# SourceGuard

**Keeping source code confidential even when the compiler is compromised.**

[![Status](https://img.shields.io/badge/status-research%20draft-orange)](docs/architecture.md)
[![Code License: Apache-2.0](https://img.shields.io/badge/code-Apache--2.0-blue)](LICENSE)
[![Docs License: CC BY 4.0](https://img.shields.io/badge/docs-CC%20BY%204.0-lightgrey)](LICENSE-docs)
[![Cite this work](https://img.shields.io/badge/cite-CITATION.cff-green)](CITATION.cff)

Developed by **95LAB**.

---

## Overview

Modern build pipelines assume the compiler is honest. SourceGuard asks a different question:
*if an attacker gains arbitrary code execution inside the compiler (for example through a malicious dependency that exploits a compiler bug), can they exfiltrate the source code being built?*

SourceGuard answers with **Differential Source Variants (DSV)**:

1. A small, trusted **Variant Generator** produces `k` source variants that are textually different but semantically equivalent (renamed locals, rewritten comments, whitespace changes, and similar).
2. Each variant is compiled in an isolated, single-use, network-less **build cell**.
3. An honest compiler produces **bit-identical** artifacts from all variants.
4. A trusted **Artifact Gateway** compares the artifacts byte for byte. Any compromised compiler that embeds information depending on the differing parts causes divergence, and the release is blocked.

Reproducible builds, traditionally an *integrity* tool, become a *confidentiality* leak detector.

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

## Status

This repository currently contains the **architecture and research design** (v0.2). The invariance hypotheses are **not yet validated**; Phase 1 of the roadmap exists to test them.

| Phase | Scope | Status |
|---|---|---|
| 0 | Skeleton: `sg seal/push`, signed manifest, minimal Key Broker | Planned |
| 1 | Invariance validation of transformations T1–T6 on a Go corpus | Planned |
| 2 | Full pipeline: VG, build cells, gateway, release signer | Planned |
| 3 | Adversarial evaluation (`attacks/` X1–X11) | Planned |
| 4 | C/C++/Rust, SEV-SNP/TDX attestation, diverse toolchains | Optional |

## Documentation

- [Architecture (English)](docs/architecture.md)
- [Kiến trúc (Tiếng Việt, original)](docs/architecture.vi.md)

Key sections: [Threat model](docs/architecture.md#2-threat-model) · [DSV transformations](docs/architecture.md#5-differential-source-variants-dsv) · [Residual leakage](docs/architecture.md#7-residual-leakage-channels-and-mitigations) · [Attack suite](docs/architecture.md#10-adversarial-attack-suite-attacks) · [Research questions](docs/architecture.md#11-research-questions)

## Citation

If you use SourceGuard, the DSV technique, or any part of this repository (code, design, text, figures, or ideas) in software, research, papers, products, or derivative work, **you must cite it**:

> Hieu Nguyen. *SourceGuard: Differential Source Variants for Build Confidentiality under Compiler Compromise.* 95LAB, 2026. https://github.com/hillnguyen528/sourceguard. Contact: hillnguyen528@gmail.com

BibTeX:

```bibtex
@misc{nguyen2026sourceguard,
  author       = {Nguyen, Hieu},
  title        = {{SourceGuard}: Differential Source Variants for Build Confidentiality under Compiler Compromise},
  year         = {2026},
  howpublished = {\url{https://github.com/hillnguyen528/sourceguard}},
  note         = {95LAB. Contact: hillnguyen528@gmail.com}
}
```

GitHub's **"Cite this repository"** button (from [CITATION.cff](CITATION.cff)) provides APA and BibTeX formats.
See [NOTICE](NOTICE) for the attribution that must accompany any redistribution.

## License

- **Code**: [Apache License 2.0](LICENSE). Redistributions must retain the [NOTICE](NOTICE) file.
- **Documentation and design** (`docs/`, README, figures): [Creative Commons Attribution 4.0 International](LICENSE-docs). Attribution is required.

## Contributing and Security

- Contributions: see [CONTRIBUTING.md](CONTRIBUTING.md).
- Vulnerabilities: please report privately, see [SECURITY.md](SECURITY.md).

## Contact

**Hieu Nguyen** · hillnguyen528@gmail.com
95LAB ·

Copyright © 2026 95LAB.
