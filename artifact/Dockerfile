# Environment for the SourceGuard evaluation tools.
#   docker build -t sourceguard-artifact artifact
#   docker run --rm -v "$PWD/docs/results:/data:ro" -v "$PWD/out:/out" sourceguard-artifact \
#     corpusrun -corpus /data/corpus-go.lock -only tomnomnom/gron -work /out/go
FROM golang:1.26
ARG TARGETARCH
RUN apt-get update -qq && apt-get install -y -qq --no-install-recommends \
      clang cmake pkg-config autoconf automake libtool bison flex gettext tcl python3 curl bzip2 \
      libevent-dev libncurses-dev libssl-dev zlib1g-dev libpcre2-dev \
      libxcb1-dev libxcb-render0-dev libxcb-shape0-dev libxcb-xfixes0-dev >/dev/null \
 && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL https://sh.rustup.rs | sh -s -- -y --profile minimal --default-toolchain stable >/dev/null 2>&1
ENV PATH=/root/.cargo/bin:$PATH SG_GHIDRA_SCRIPTS=/artifact/ghidra
COPY bin/corpusrun-linux-${TARGETARCH} /usr/local/bin/corpusrun
COPY bin/polyrun-linux-${TARGETARCH} /usr/local/bin/polyrun
COPY ghidra /artifact/ghidra
