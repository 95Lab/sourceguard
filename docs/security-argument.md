# Security Argument

This document states precisely what SourceGuard guarantees, under which assumptions, and what it does not. It replaces the informal proposition in [architecture §5.3](architecture.md#53-security-property-hypothesis-to-be-proven).

The main result is not the one the architecture document anticipated. **The confidentiality of comments and renamed identifiers does not come from comparing artifacts. It comes from the Variant Generator, which never passes them to a build cell.** The comparison contributes integrity and detection, not confidentiality. Sections 3 to 5 make this exact.

## 1. Model

**Programs.** A source tree `P` is a finite set of files. The Variant Generator `G(P, r)` maps a tree and a random seed `r` to a *variant*.

**Equivalence.** Two trees are *variant-equivalent*, written `P ≈ P'`, if one is obtained from the other by any combination of:

1. replacing the content of rewritable comments, keeping their byte length, line breaks and the preserved bytes (backslashes, non-ASCII bytes, and comments the toolchain interprets);
2. changing the amount of horizontal whitespace where the language rules of the generator allow it;
3. renaming the renamable identifiers (function-local names; unexported package-level variables and constants) by a bijection that preserves their lexical order and their equality pattern;
4. adding or changing the appended unreachable functions.

The equivalence class `[P]` is the **core** of `P`.

**Hidden part.** `h(P)` is exactly what the generator replaces: the bytes of comment content that T1 overwrites, and the spellings of the identifiers that T3 and T7 rename. The **layout** `λ(P)` is the original horizontal whitespace. Everything else is core, including the length and position of every comment, the bytes T1 preserves, the lexical order of renamed names, and which occurrences share a name.

**Compilers.** An honest compiler is a deterministic function `C` from trees to artifacts. An adversarial compiler is an arbitrary function `A(V, ρ)` of the variant it receives and of randomness `ρ` that all compromised cells may share. Sharing `ρ` models an implant that is identical in every cell.

**Gateway.** With `k ≥ 2` cells receiving `V_i = G(P, r_i)`, the gateway releases an artifact only if all cells report the same status and byte-identical artifacts.

## 2. Assumptions

- **A1, cross-variant invariance.** For all seeds `r, r'`: `C(G(P, r)) = C(G(P, r'))`.
  Under A1 the **scrubbed artifact** `B(P) := C(G(P, r))` is well defined. A1 does *not* say that `B(P)` equals `C(P)`, the conventional build of the unmodified source. Measured for Go: variants of T1, T2 and T7 equal the unmodified build in 54 of 54 projects; variants of T3 in none; variants of T4 in about half. A1 itself is an empirical property; the [invariance studies](results/phase1-invariance.md) measure it, including the cases where it fails.
- **A2, semantic preservation.** For every seed, `G(P, r)` has the same observable behaviour as `P` under the project's build configuration.
  A2 makes `B(P)` a correct build of `P` even where it differs from `C(P)` in bytes. It is argued per transformation, not proven for the language. T1 and T2 change no token. T3 and T7 are consistent renamings to names that occur nowhere else. T4 adds an unreferenced function. The argument fails wherever something outside the compiled code refers to a renamed name; the generator excludes the known cases (cgo, assembly, `go:linkname`, string variables that the linker flag `-X` can set, Go files in directories the Go tool ignores). The [evaluation](results/evaluation.md#behaviour-test-suites-on-original-and-variant) tests A2 with the projects' own test suites.
- **A3, fresh sampling.** The distribution of `G(P, r)` over `r` depends on `P` only through `[P]` and `λ(P)`.
  This holds by construction: new comment bytes and new names are drawn from the seed, never derived from the originals. A generated name is redrawn if it collides with an existing identifier, so the set it is checked against must not depend on the hidden part. Generated names have a fixed shape (`v_` or `g_` and ten random characters), only identifiers of that shape can collide, and the generator never renames an identifier of that shape. The collision set is therefore part of the core, and A3 holds exactly. Whitespace is the exception that `λ(P)` accounts for: T2 adds padding to existing whitespace.
- **A4, isolation.** A cell receives nothing but its variant, has no channel to other cells or to the outside, and emits only an artifact and an allow-listed status.
- **A5, independence.** Seeds are uniform, secret, and independent across cells and across builds.
- **A6, trusted base.** The Variant Generator, the Key Broker and the gateway behave as specified.

A4 to A6 are engineering assumptions. The prototype does not meet A4 at the strength a deployment needs against a strong adversary.

## 3. Theorem 1: the hidden part never reaches a cell

> **Theorem 1.** Let `P` and `P'` satisfy `[P] = [P']` and `λ(P) = λ(P')`. Then, for every `k`, the joint distribution of the variants `(V_1, …, V_k)` is the same for `P` and for `P'`. Consequently, for every adversary, everything that leaves the cells (artifacts, statuses, timing, the gateway verdict) is statistically independent of `h(P)` given `[P]` and `λ(P)`.

*Proof.* By A3 each `V_i` is distributed as a function of `[P]`, `λ(P)` and `r_i` only, and by A5 the `r_i` are independent. The outputs of the cells are functions of `(V_1, …, V_k)` and `ρ` by A4. ∎

Remarks.

- The guarantee is information-theoretic: no computational assumption, and statistical distance zero under the stated assumptions.
- **It holds for `k = 1`.** No comparison is involved. Theorem 1 is the statement "the source was scrubbed before it was sent to the compiler".
- It protects exactly `h(P)` as defined above, no more.
- It covers side channels that originate in a cell.

## 4. What is not hidden

The following stays in the core or the layout, so a compromised compiler can read it:

- all code tokens, constants and string literals;
- every name the generator does not rename: functions, methods, types, fields, exported names, package-level string variables, and all names in C, C++ and Rust;
- the **length and position** of every comment, and the line structure of every file;
- the **lexical order** of the renamed names and **which occurrences share a name**;
- the original whitespace, up to the padding added by T2;
- any file the generator passes through unchanged: cgo files, embedded files, files in ignored directories, files it cannot parse, and all but comments in a program that links `plugin`.

How much of this an honest binary already reveals is an empirical question; see [Evaluation Results](results/evaluation.md).

## 5. Theorem 2: what the comparison certifies

Let `q_A = max_a Pr_r[A(G(P, r), ρ) = a]` be the probability of the most likely artifact of an adversarial compiler over the generator's randomness, for a fixed `ρ`.

> **Theorem 2.**
> 1. *(Completeness.)* If all cells are honest, the gateway accepts and releases `B(P)`.
> 2. *(Integrity.)* If at least one cell is honest and the gateway accepts, the released artifact is exactly `B(P)`.
> 3. *(Detection.)* If all `k` cells run the same adversarial compiler, then `q_A^k ≤ Pr[accept] ≤ q_A^(k−1)`.

*Proof.* (1) Every honest cell outputs `C(G(P, r_i))`, which is `B(P)` by A1. (2) The honest cell outputs `B(P)`; acceptance requires all artifacts to be equal. (3) The artifacts `a_i = A(V_i, ρ)` are independent and identically distributed given `ρ`. `Pr[accept] = Σ_a Pr[a_1 = a]^k`, which lies between `q_A^k` and `q_A^(k−1) · Σ_a Pr[a_1 = a] = q_A^(k−1)`. ∎

With A2, the artifact of (1) and (2) is a correct build of `P`.

Consequences.

- **Naive exfiltration is caught.** A compiler that embeds its input verbatim, or after removing only comments when renaming is enabled, has `q_A` equal to the collision probability of two variants. The gateway rejects.
- **Adaptive exfiltration of the core is not prevented.** A compiler that computes a canonical form of its input and embeds that has `q_A = 1` and is always accepted.
- **One honest cell is enough for integrity.**

### Guarantees by situation

| Situation | Outcome |
|---|---|
| Git service, orchestrator or storage compromised | Ciphertext only is exposed |
| Any number of cells compromised, reading their variants | The hidden part stays confidential (Theorem 1); the core is exposed |
| Some cells compromised and altering output, at least one honest | Rejected, or the release is `B(P)` (Theorem 2.2) |
| All cells compromised, output depends on randomised content | Rejected with probability at least `1 − q_A^(k−1)` |
| All cells compromised, identical malicious artifact | Accepted; not detected |

A malicious input that exploits a compiler bug reaches every cell, so it typically compromises all of them. Then Theorem 2.2 does not apply. What the system offers is **partial source confidentiality**, unconditionally, and **conditional artifact integrity**.

## 6. Residual channels

By Theorem 1, no channel carries information about `h(P)`. Information about the core can leave in two ways:

- **Through an accepted artifact**, when no cell is honest: up to the whole core (X4).
- **Through the verdict**, even when artifacts are never released: each build yields one of a small set of outcomes, so at most `log2(2 + |error classes|)` bits per build. The rate limit on non-accepting builds bounds the total per project and day.

## 7. Why the generator replaces instead of comparing

An alternative design gives one cell the original source and the other cells scrubbed copies, and relies on the comparison to stop the first cell from leaking. That is the structure of [TightLip](related-work.md#output-comparison-and-multi-execution). Under it, Theorem 1 fails for the cell that holds the original, and the protection of `h(P)` degrades from perfect to the detection bound of Theorem 2(3), with the verdict channel now carrying information about `h(P)`.

It would also need `C(P) = B(P)`, which does not hold for all transformations. Since the scrubbed build is a correct build (A2), nothing is gained by showing the original to any cell. Sampling every variant freshly is strictly better, and it is what SourceGuard does.

## 8. Summary

| Property | Guaranteed by | Needs comparison (`k ≥ 2`)? | Strength |
|---|---|---|---|
| Comments and renamed identifiers stay confidential | Fresh sampling in the Variant Generator (Theorem 1) | No | Information-theoretic |
| The release equals the honest scrubbed build `B(P)` | A1 and one honest cell (Theorem 2.2) | Yes | Exact |
| `B(P)` behaves like a conventional build | A2, argued per transformation and tested | No | Empirical |
| Input-dependent tampering is detected | Independent variants (Theorem 2.3) | Yes | Probability `≥ 1 − q_A^(k−1)` |
| The core stays confidential | Not guaranteed | n/a | Open; bounded only by one honest cell |

The practical reading: SourceGuard removes part of the source before an untrusted toolchain sees it, in a way that provably does not change the binary an honest toolchain produces, and it uses reproducibility across differently scrubbed copies to check that the toolchain was honest.
