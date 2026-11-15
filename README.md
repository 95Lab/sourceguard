<div align="center">

# SourceGuard

**Keeping source code confidential even when the compiler is compromised.**

[![Status](https://img.shields.io/badge/status-research%20prototype-orange)](docs/architecture.md)
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
- [Quick Start](#quick-start)
- [Repository Layout](#repository-layout)
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

A working **prototype for Go** implements the full pipeline and runs locally in Docker. It is a research test bed, not a secure deployment; see the [prototype limitations](docs/local-testing.md#prototype-limitations).

| Phase | Scope | Status |
|---|---|---|
| 0 | `sg seal`, signed manifest, Key Broker; the store holds ciphertext only | Prototype |
| 1 | Invariance validation of the transformations | **Done for Go:** T1–T3 bit-identical on 54 / 54 real projects, linux/arm64 and linux/amd64 ([results](docs/results/phase1-invariance.md)) |
| 2 | Full pipeline: Variant Generator, build cells, gateway, release signer | Prototype (containers, not microVMs) |
| 3 | Adversarial evaluation | Suite of 7 attacks implemented; leakage measurement (RQ2) outstanding |
| 4 | C/C++/Rust, SEV-SNP/TDX attestation, diverse toolchains | Not started |

## Quick Start

Requires Docker with Compose v2, and Go 1.26+ for the unit tests.

```sh
git clone https://github.com/95Lab/sourceguard.git && cd sourceguard
make test         # unit tests
make invariance   # are variants bit-identical after compilation?
make demo         # honest build: ACCEPT + verified provenance
make attacks      # malicious compiler in the build cells
```

`make attacks` shows the mechanism at work: a compiler that embeds source in the artifact is rejected, while the documented limit (embedding fully normalized source, attack X4) passes. Details: [Local Testing with Docker](docs/local-testing.md).

## Repository Layout

| Path | Content |
|---|---|
| `cmd/sg` | Developer CLI: `init`, `seal`, `verify`, `open` |
| `cmd/sg-broker` | Key Broker (TCB) |
| `cmd/sg-variant` | Variant Generator (TCB) |
| `cmd/sg-cell` | Agent inside a build cell |
| `cmd/sg-gateway` | Artifact Gateway (TCB) |
| `cmd/sg-sign` | Release Signer (TCB) |
| `internal/variants/golang` | Transformations T1 (comments), T2 (whitespace), T3/T5 (local names) |
| `attacks/evilgo` | Malicious compiler wrapper for the adversarial suite |
| `eval/invariance` | Bit-identity check on the sample project |
| `eval/corpusrun`, `eval/modal` | Phase 1 corpus study (RQ1): runner, report, divergence bisection, Modal app |
| `examples/hello` | Sample project with planted secrets |
| `compose.yaml`, `deploy/docker`, `scripts` | Local test bed |

## Documentation

| Document | Description |
|---|---|
| [Architecture (English)](docs/architecture.md) | Full design: threat model, components, DSV, leakage analysis, roadmap |
| [Local Testing with Docker](docs/local-testing.md) | Running the prototype, the attack suite, results and limitations |
| [Phase 1 Results](docs/results/phase1-invariance.md) | Invariance study on 54 open-source Go projects: method, findings, limitations |
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
