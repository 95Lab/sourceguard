# Contributing to SourceGuard

Thank you for your interest in SourceGuard, a project of **95LAB**, an independent, non-profit research group.

## Ways to contribute

- **Design review:** challenge the threat model, propose new attacks, or point out flaws in the DSV security argument. Open an issue using the *Design discussion* template.
- **Invariance data (Phase 1):** report transformations that do or do not yield bit-identical output for a given compiler and project.
- **Code:** once implementation starts, pick an issue labelled `good first issue` or `help wanted`.

## Workflow

1. Fork the repository and create a branch from `main` (`feat/…`, `fix/…`, `docs/…`).
2. Keep changes focused; one logical change per pull request.
3. Use clear commit messages (Conventional Commits are welcome: `feat:`, `fix:`, `docs:`).
4. For Go code: run `gofmt`, `go vet`, and `go test ./...` before submitting.
5. Components in the TCB (`sg-variant`, `sg-gateway`, `sg-broker`) require extra scrutiny: keep them small and explain any added complexity.
6. Open a pull request and fill in the template.

## Licensing of contributions

By submitting a contribution, you agree that it is licensed under the Apache License 2.0 (code) or CC BY 4.0 (documentation), consistent with this repository, and you certify the [Developer Certificate of Origin](https://developercertificate.org/). Please sign off your commits (`git commit -s`).

## Attribution

Derivative work must keep the [NOTICE](NOTICE) file and credit 95LAB. See [CITATION.cff](CITATION.cff).

## Conduct

Please be respectful and constructive. Harassment of any kind is not tolerated. Report concerns to hillnguyen528@gmail.com.
