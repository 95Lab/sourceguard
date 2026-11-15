# Related Work and Positioning

This document places SourceGuard among earlier techniques. Read it with the [security argument](security-argument.md), which determines what can be claimed as new.

## Summary

| Line of work | Mechanism | Protects | Relation to SourceGuard |
|---|---|---|---|
| TightLip (2007) | Run the original process and a copy fed scrubbed data; compare outputs | Confidentiality of files, against buggy or misconfigured applications | Same comparison idea; SourceGuard shows no copy needs the secret |
| Shadow executions (2008), Secure Multi-Execution (2010) | One execution per security level; public outputs come from an execution that never saw secret inputs | Noninterference | Closest in principle: the release is produced from inputs that exclude the hidden part |
| N-variant systems (2006), data diversity (2008) | Run diversified variants on the same input; divergence signals an attack | Integrity of a running service | SourceGuard diversifies the *input* of an unmodified program, at build time |
| Reproducible builds, Diverse Double-Compiling | Rebuild and compare artifacts | Integrity of the build and of the compiler | Same comparison, same bytes in every rebuild; a consistent leak is invisible |
| Attestable builds, TICAL, Kettle | Build inside a trusted execution environment | Integrity, and confidentiality against the infrastructure | Complementary: the compiler inside the enclave still sees the full source |
| Source scrubbing and obfuscation | Strip or rename before handing source to a third party | Confidentiality of comments and names | The baseline; SourceGuard adds proof that the binary is unchanged, and a check on the toolchain |

## Output comparison and multi-execution

**TightLip** (Yumerefendi, Mickle and Cox, NSDI 2007) is the nearest predecessor of the comparison step. When a process reads a sensitive file, the kernel starts a *doppelganger* that receives a scrubbed version of the file. Both run side by side; if their outputs to the network are the same, the output of the original does not depend on the sensitive data, and it is allowed.

Differences:

- In TightLip one process holds the secret, so the comparison is what protects it, and the guarantee is probabilistic. In SourceGuard every build cell receives a freshly sampled variant and none holds the hidden part, so its confidentiality is unconditional ([Theorem 1](security-argument.md#3-theorem-1-the-hidden-part-never-reaches-a-cell)). [Section 7 of the security argument](security-argument.md#7-why-the-generator-replaces-instead-of-comparing) explains why this is strictly better when it is available.
- It is available because of a property TightLip cannot assume: an honest compiler's output does not depend on the scrubbed content at all. TightLip must cope with legitimate divergence between original and doppelganger; SourceGuard measures that, with the right transformations and flags, there is none.
- TightLip's adversary is an honest but careless application. SourceGuard's is a compiler under the attacker's control.

**Secure Multi-Execution** (Devriese and Piessens, IEEE S&P 2010) and **shadow executions** (Capizzi, Longo, Venkatakrishnan and Sistla, ACSAC 2008) enforce noninterference by running a program once per security level. The low execution receives default values in place of high inputs, and only it may write to low channels. There is no comparison; the low output is noninterferent by construction.

SourceGuard's confidentiality argument has exactly this shape. The released artifact is the "low output", the hidden part of the source is the "high input", and the artifact is produced by executions that were given random values in its place. What SourceGuard adds to that picture:

- the replacement values are **random and differ per execution** instead of being fixed defaults, which turns the redundant executions into a detector for input-dependent tampering ([Theorem 2](security-argument.md#5-theorem-2-what-the-comparison-certifies));
- the empirical demonstration that, for compilation, the low execution's output is **the same artifact** an honest build of the original would give, so the multi-execution is transparent rather than a change of behaviour.

In the terminology of hyperproperties (Clarkson and Schneider, 2010), noninterference of the compiler in the hidden part is a 2-safety property, and the gateway tests it on `k` sampled executions.

## Diversity

**N-variant systems** (Cox et al., USENIX Security 2006) run several automatically diversified variants of a server on the same input and monitor them for divergence; an attack that depends on a diversified property cannot succeed in all variants. **Data diversity** (Nguyen-Tuong et al., DSN 2008) applies the idea to data representations.

SourceGuard inverts the roles: the program (the toolchain) is identical in every cell and is not modified, and the diversity is in its input. This matters in practice, because the toolchain is the untrusted component and cannot be asked to cooperate.

## Build integrity

**Reproducible builds** (Lamb and Zacchiroli, IEEE Software 2022) and **Diverse Double-Compiling** (Wheeler, ACSAC 2005) detect a tampered build or compiler by comparing independently produced artifacts. They are integrity mechanisms. A toolchain that embeds the source it compiles produces the same artifact in every rebuild of the same bytes, so rebuilding cannot notice it. SourceGuard's cells compile different bytes, so that particular behaviour diverges.

**Attestable builds** (Hugenroth et al., ACM CCS 2025), **TICAL** (Krahn et al., EDCC 2024) and **Kettle** (2026) run the build inside a trusted execution environment and attest the result. They protect the build from the infrastructure. TICAL and Kettle also keep the source confidential from the infrastructure. In all three the compiler is inside the trust boundary and receives the complete source; a compiler compromised through its input is outside their threat models. The approaches compose: SourceGuard's trusted components and cells can run in enclaves.

## Source scrubbing

Removing comments and renaming identifiers before giving source to an outside party is established practice (minifiers, obfuscators, export-control filters). By [Theorem 1](security-argument.md#3-theorem-1-the-hidden-part-never-reaches-a-cell), this is where SourceGuard's confidentiality for comments and names comes from, and a paper must say so.

What scrubbing alone does not provide, and SourceGuard does:

1. **Evidence that scrubbing does not change the product.** Ordinary obfuscators make no such claim. The invariance studies show which transformations leave the binary bit-identical, on which compilers, and document the cases where a plausible transformation silently changes it.
2. **A check on the toolchain.** With randomized scrubbing and `k ≥ 2` cells, an honest cell certifies the release, and input-dependent tampering is detected.
3. **An account of what cannot be scrubbed.** Names the binary depends on, comment lengths, name order, columns in Rust, export-data hashes: each was found by measurement.

## What can be claimed

Supported by the argument and the measurements:

- A build pipeline in which a compromised compiler provably learns nothing about comments and renamed identifiers, while honest builds are bit-identical to conventional ones.
- An empirical characterization of source-level transformations that are binary-invariant for Go (five transformations, 54 projects, two architectures), with first results for C, C++ and Rust.
- Reproducibility across non-identical inputs as an integrity and detection mechanism, with an exact acceptance bound.

Not supported:

- That comparing artifacts protects the confidentiality of source code. It does not; the core of the program remains exposed to a toolchain that is compromised in every cell.
- That the idea of running redundant executions on scrubbed inputs is new. TightLip and Secure Multi-Execution precede it.

## References

- A. Yumerefendi, B. Mickle, L. Cox. *TightLip: Keeping Applications from Spilling the Beans.* NSDI 2007.
- D. Devriese, F. Piessens. *Noninterference through Secure Multi-Execution.* IEEE S&P 2010.
- R. Capizzi, A. Longo, V. N. Venkatakrishnan, A. P. Sistla. *Preventing Information Leaks through Shadow Executions.* ACSAC 2008.
- B. Cox et al. *N-Variant Systems: A Secretless Framework for Security through Diversity.* USENIX Security 2006.
- A. Nguyen-Tuong et al. *Security through Redundant Data Diversity.* DSN 2008.
- M. Clarkson, F. Schneider. *Hyperproperties.* Journal of Computer Security, 2010.
- D. A. Wheeler. *Countering Trusting Trust through Diverse Double-Compiling.* ACSAC 2005.
- C. Lamb, S. Zacchiroli. *Reproducible Builds: Increasing the Integrity of Software Supply Chains.* IEEE Software, 2022.
- D. Hugenroth et al. *Attestable Builds: Compiling Verifiable Binaries on Untrusted Systems using Trusted Execution Environments.* ACM CCS 2025. https://arxiv.org/abs/2505.02521
- R. Krahn et al. *TICAL: Trusted and Integrity-protected Compilation of AppLications.* EDCC 2024. https://arxiv.org/abs/2511.17070
- *Kettle: Attested Builds for Verifiable Software Provenance.* 2026. https://arxiv.org/abs/2605.08363
