# Security Argument

This document states precisely what SourceGuard guarantees, under which assumptions, and what it does not. It replaces the informal proposition in [architecture §5.3](architecture.md#53-security-property-hypothesis-to-be-proven).

The main result is not the one the architecture document anticipated. **The confidentiality of comments and renamed identifiers does not come from comparing artifacts. It comes from the Variant Generator, which never passes them to a build cell.** The comparison contributes integrity and detection, not confidentiality. Sections 3 to 5 make this exact.

## 1. Model

**Programs.** A source tree `P` is a finite set of files.

**Equivalence.** Two trees are *variant-equivalent*, written `P ≈ P'`, if one is obtained from the other by any combination of:

1. replacing the content of rewritable comments, keeping their byte length, line breaks and the preserved bytes (backslashes, non-ASCII bytes, and comments the compiler interprets);
2. changing the amount of horizontal whitespace where the language rules of the generator allow it;
3. renaming the renamable identifiers (function-local names; unexported package-level variables and constants) by a bijection that preserves their lexical order and their equality pattern;
4. adding or changing the appended unreachable functions.

The equivalence class `[P]` is the **core** of `P`. Everything that distinguishes `P` inside its class is split into the **hidden part** `h(P)` (original comment content, original renamable names) and the **layout** `λ(P)` (original whitespace).

**Variant Generator.** `G(P, r)` maps a tree and a random seed `r` to a variant.

**Compilers.** An honest compiler is a deterministic function `C` from trees to artifacts. An adversarial compiler is an arbitrary function `A(V, ρ)` of the variant it receives and of randomness `ρ` that all compromised cells may share. Sharing `ρ` models an implant that is identical in every cell.

**Gateway.** With `k ≥ 2` cells receiving `V_i = G(P, r_i)`, the gateway releases an artifact only if all cells report the same status and byte-identical artifacts.

## 2. Assumptions

- **A1, invariance.** The honest compiler is constant on equivalence classes: `P ≈ P'` implies `C(P) = C(P')`.
  This is an empirical property of a compiler and build configuration, not a theorem. It is what the [invariance studies](results/phase1-invariance.md) measure, including the cases where it fails (embedded files, columns above 255, programs linking `plugin`, columns in Rust).
- **A2, fresh sampling.** `G(P, r) ≈ P` for every `r`, and the distribution of `G(P, r)` over `r` depends on `P` only through `[P]` and `λ(P)`.
  This holds by construction: new comment bytes and new names are drawn from the seed, never derived from the originals. (A candidate name that collides with an identifier already in the file is redrawn. With 10 random characters this changes the distribution by a negligible amount, and "independent" below is meant up to that statistical distance.) Whitespace is the exception: T2 adds random padding to the existing whitespace, which is why `λ(P)` appears.
- **A3, isolation.** A cell receives nothing but its variant, has no channel to other cells or to the outside, and emits only an artifact and an allow-listed status.
- **A4, independence.** Seeds are uniform, secret, and independent across cells and across builds.
- **A5, trusted base.** The Variant Generator, the Key Broker and the gateway behave as specified.

A3 to A5 are engineering assumptions. The prototype does not meet A3 at the strength a deployment needs (containers instead of microVMs).

## 3. Theorem 1: the hidden part never reaches a cell

> **Theorem 1.** Let `P` and `P'` satisfy `[P] = [P']` and `λ(P) = λ(P')`. Then, for every `k`, the joint distribution of the variants `(V_1, …, V_k)` is the same for `P` and for `P'`. Consequently, for every adversary, everything that leaves the cells (artifacts, statuses, timing, the gateway verdict) is statistically independent of `h(P)` given `[P]` and `λ(P)`.

*Proof.* By A2 each `V_i` is distributed as a function of `[P]`, `λ(P)` and `r_i` only, and by A4 the `r_i` are independent. The outputs of the cells are functions of `(V_1, …, V_k)` and `ρ` by A3. ∎

Remarks.

- The guarantee is information-theoretic and holds for unbounded adversaries.
- **It holds for `k = 1`.** No comparison is involved. Theorem 1 is the statement "the source was scrubbed before it was sent to the compiler".
- It covers side channels that originate in a cell: build failures, timing and error classes cannot depend on `h(P)`, because the cell does not have it.
- Rebuilding gives the adversary further samples of `G(P, ·)`. They are still independent of `h(P)`.

## 4. What is not hidden

Theorem 1 is only as strong as the hidden part is large. The following stays in the core or the layout, so a compromised compiler can read it, and Theorem 1 says nothing about it:

- all code tokens, constants and string literals;
- every name the generator does not rename: functions, methods, types, fields, exported names, and all names in C, C++ and Rust;
- the **length and position** of every comment, and the line structure of every file;
- the **lexical order** of the renamed names and **which occurrences share a name** (both are preserved because the compiler output depends on them);
- the original whitespace, up to the padding added by T2;
- any file the generator passes through unchanged: cgo files, embedded files, files it cannot parse, and all but comments in a program that links `plugin`.

How much of this an honest binary already reveals is an empirical question; see [Evaluation Results](results/evaluation.md).

## 5. Theorem 2: what the comparison certifies

Let `q_A = max_a Pr_r[A(G(P, r), ρ) = a]` be the probability of the most likely artifact of an adversarial compiler over the generator's randomness, for a fixed `ρ`.

> **Theorem 2.**
> 1. *(Completeness.)* If all cells are honest, the gateway accepts and releases `C(P)`.
> 2. *(Integrity.)* If at least one cell is honest and the gateway accepts, the released artifact is exactly `C(P)`.
> 3. *(Detection.)* If all `k` cells run the same adversarial compiler, then `q_A^k ≤ Pr[accept] ≤ q_A^(k−1)`.

*Proof.* (1) By A2, `V_i ≈ P`, so by A1 every cell outputs `C(P)`. (2) The honest cell outputs `C(P)` as in (1); acceptance requires all artifacts to be equal. (3) The artifacts `a_i = A(V_i, ρ)` are independent and identically distributed given `ρ`. `Pr[accept] = Σ_a Pr[a_1 = a]^k`, which lies between `q_A^k` and `q_A^(k−1) · Σ_a Pr[a_1 = a] = q_A^(k−1)`. ∎

Consequences.

- **A released artifact is a function of the core.** By (3), an adversary that is accepted with probability at least `1 − δ` must produce one fixed artifact with probability at least `(1 − δ)^(1/(k−1))` over the variants. Its output is then, up to that probability, determined by `[P]`, `λ(P)` and `ρ`.
- **Naive exfiltration is caught.** A compiler that embeds its input verbatim (attacks X1, X2) or after removing only comments (X3, when renaming is enabled) has `q_A` equal to the collision probability of two variants, which is negligible. The gateway rejects, and the rejection is evidence that the toolchain is compromised.
- **Adaptive exfiltration of the core is not prevented.** A compiler that computes a canonical form of its input and embeds that (X4) has `q_A = 1`. It is always accepted. The measurements confirm that such a canonical form exists for every file tested.
- **One honest cell is enough for integrity.** With (2), a single uncompromised cell turns acceptance into a proof that the release is the honest artifact, and in particular that nothing was embedded. This is the reproducible-builds argument, with the added property that the cells did not compile the same bytes.

## 6. Residual channels

By Theorem 1, no channel carries information about `h(P)`. Information about the core can leave in two ways:

- **Through an accepted artifact**, when no cell is honest: up to the whole core (X4).
- **Through the verdict**, even when artifacts are never released: each build yields one of a small set of outcomes, so at most `log2(2 + |error classes|)` bits per build. The rate limit on non-accepting builds bounds the total per project and day.

## 7. Why the generator replaces instead of comparing

An alternative design gives one cell the original source and the other cells scrubbed copies, and relies on the comparison to stop the first cell from leaking. That is the structure of [TightLip](related-work.md#output-comparison-and-multi-execution). Under it, Theorem 1 fails for the cell that holds the original, and the protection of `h(P)` degrades from perfect to the detection bound of Theorem 2(3), with the verdict channel now carrying information about `h(P)`.

Since A1 says the honest artifact does not depend on `h(P)`, nothing is gained by showing the original to any cell. Sampling every variant freshly is strictly better, and it is what SourceGuard does.

## 8. Summary

| Property | Guaranteed by | Needs comparison (`k ≥ 2`)? | Strength |
|---|---|---|---|
| Comments and renamed identifiers stay confidential | Fresh sampling in the Variant Generator (Theorem 1) | No | Information-theoretic |
| The release equals an honest build | A1 and one honest cell (Theorem 2.2) | Yes | Exact |
| Input-dependent tampering is detected | Independent variants (Theorem 2.3) | Yes | Probability `≥ 1 − q_A^(k−1)` |
| The core stays confidential | Not guaranteed | n/a | Open; bounded only by one honest cell |

The practical reading: SourceGuard removes part of the source before an untrusted toolchain sees it, in a way that provably does not change the binary an honest toolchain produces, and it uses reproducibility across differently scrubbed copies to check that the toolchain was honest.
