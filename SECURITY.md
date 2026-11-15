# Security Policy

SourceGuard is a security project, and we take reports seriously.

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Instead, either:

- use GitHub's **"Report a vulnerability"** button (Security → Advisories) on this repository, or
- email **hillnguyen528@gmail.com** with the subject `[SourceGuard Security]`.

Please include a description, affected component or design section, reproduction steps or a proof of concept, and the potential impact.

We aim to acknowledge reports within **5 business days** and to agree on a disclosure timeline with you.

## Scope

In scope:

- Flaws in the DSV security argument (Section 5.3 of the [architecture](docs/architecture.md)) that let a compromised compiler leak variant-dependent information while still passing the gateway.
- Weaknesses in TCB components: Variant Generator, Key Broker, Artifact Gateway, Release Signer.
- Sandbox or isolation escapes from build cells.
- New side channels beyond those listed in Section 7.

Out-of-scope items are listed in Section 1.2 of the architecture document. Novel attacks on them are still welcome as design discussions.

## Supported versions

The project is at the research-draft stage (v0.2). Only the latest `main` branch is supported.

---
Maintained by 95LAB. Security contact: Dang-Hieu Nguyen, Head of 95LAB.
