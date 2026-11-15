# Self-Hosting Source Code with SourceGuard

SourceGuard is designed for teams that host their own Git service and do not want that service, its backups, its CI system or the compiler toolchain to be able to read their source.

This page describes what a deployment looks like and what it changes for developers. It has been run end to end on a single Linux host with Forgejo 11, container build cells and Firecracker microVM build cells.

## The idea in one picture

```
 Developer machine            Self-hosted Forgejo           Build host
 ┌────────────────┐  push     ┌──────────────────┐ webhook  ┌───────────────────────────┐
 │ plaintext      │ ───────►  │ manifest.json    │ ───────► │ Key Broker                │
 │ sg seal        │ cipher-   │ modules/*.age    │          │ Variant Generator         │
 │ sg push        │ text      │ (nothing else)   │          │ k build cells (microVMs)  │
 └────────────────┘           │                  │ ◄─────── │ Artifact Gateway, Signer  │
        ▲    verify           │ status, release  │  result  └───────────────────────────┘
        └──────────────────── └──────────────────┘
```

## Who sees what

| Party | Sees | Never sees |
|---|---|---|
| Developer machine | Plaintext source, developer keys | |
| Git service (database, repositories, backups) | An encrypted archive per module, a signed manifest, build statuses, released artifacts | Source, any key |
| Orchestrator (CI) | The same ciphertext; verdicts | Source, any key |
| Variant Generator | Plaintext, in memory, for the duration of one build | |
| Build cell (compiler, dependencies) | One scrubbed variant of one module | Original comments and names; other modules; other cells' variants |
| Artifact Gateway, Release Signer | Artifacts and statuses | Source |

A compromise of the Git service or of the CI system therefore yields ciphertext. A compromise of the toolchain yields the code without what the Variant Generator removed; what exactly that covers is stated in the [security argument](security-argument.md).

## What a build looks like

1. The developer seals the project (one key per module) and pushes the ciphertext to a normal Forgejo repository.
2. Forgejo calls a webhook. The orchestrator registers one job per module.
3. For each module, the Variant Generator obtains the key once, produces `k` variants and encrypts each to one build cell.
4. Each cell builds its variant in isolation: no network, read-only system, its only output is an artifact and a status code. Cells can be containers or single-use Firecracker microVMs.
5. The gateway compares the artifacts byte for byte.
6. Forgejo receives a commit status per module (`ACCEPT`, `REJECT` or `FAIL`) and, if all modules are accepted, a release with the artifacts and their signed provenance, which anyone holding the signer's public key can verify.

On the test host a Go module builds in about six seconds per microVM cell, and the artifact is bit-identical to the one built in container cells.

## Compared with an ordinary Git service

| | Ordinary GitHub / GitLab / Forgejo | With SourceGuard |
|---|---|---|
| Server or backup compromised | Source exposed | Ciphertext exposed |
| CI system or runner compromised | Source exposed | Ciphertext exposed |
| Malicious compiler or build dependency | Source exposed | Comments and renamed identifiers withheld; the remaining code exposed |
| Browsing, diff, search, review on the server | Yes | No: the server cannot read the code |
| Issues, releases, statuses, access control | Yes | Yes |
| Build cost | One build | `k` builds (default 2) |
| Operations | The Git service | Plus Key Broker, Variant Generator, gateway and key management |

SourceGuard is worth its cost when the source is the main asset and the hosting or build environment is not fully under your control. If you trust your Git provider and need server-side review, an ordinary setup is the better choice.

## Current limits

- Modules of a project are built independently; one module cannot yet import another.
- Builds run one at a time.
- The orchestrator starts the cells and is therefore part of the trusted base of the host.
- Key release relies on a shared token, not on hardware attestation.
- Languages: Go with five transformations; C, C++ and Rust with comment and whitespace transformations only.

## Availability

The implementation is maintained privately by 95LAB. For access or a deployment, contact hillnguyen528@gmail.com.
