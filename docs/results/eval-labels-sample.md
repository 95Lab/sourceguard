# Sample used to check the comment labels

Twenty blocks per label, chosen by a fixed hash order from three blocks per project and label. Text is cut at 400 bytes.

## task marker

- `google/gops` `goprocess/goprocess.go`

  ```
   TODO(jbd): Return a list of errors.
  ```
- `google/pprof` `internal/report/source.go`

  ```
   TODO(sanjay): Report that the covered addresses are missing.
  ```
- `kubernetes-sigs/kind` `pkg/cluster/internal/providers/podman/provider.go`

  ```
   TODO: get rid of this once podman settles on how to get the port mapping using podman inspect
   This is only used to get the Kubeconfig server field
  ```
- `gohugoio/hugo` `tpl/internal/go_templates/htmltemplate/html.go`

  ```
   TODO: Split attr and element name part filters so we can recognize known attributes.
  ```
- `cli/cli` `pkg/cmd/codespace/ssh.go`

  ```
   scp treats a colon in the first path segment as a host identifier.
   Escape it by prepending "./".
   TODO(adonovan): test on Windows, including with a c:\\foo path.
  ```
- `peco/peco` `layout.go`

  ```
   XXX Do we really need this?
  ```
- `peco/peco` `layout.go`

  ```
   XXX Do we really need this?
  ```
- `jesseduffield/lazygit` `pkg/gui/gui.go`

  ```
   TODO: add filepath.Join(gui.git.RepoPaths.RepoPath(), ".lazygit.yml"),
   with trust prompt
  ```
- `hairyhenderson/gomplate` `internal/datafs/wdfs.go`

  ```
   volume name used for drive-relative paths on Windows for cases when they
   shouldn't be relative to the current working directory's volume
   TODO: validate that this is actually needed
  ```
- `FiloSottile/mkcert` `main.go`

  ```
   The system cert pool is only loaded once. After installing the root, checks
   will keep failing until the next execution. TODO: maybe execve?
   https://github.com/golang/go/issues/24540 (thanks, myself)
  ```
- `yuin/gopher-lua` `oslib.go`

  ```
   TODO yday & dst
  ```
- `zyedidia/micro` `internal/info/infobuffer.go`

  ```
   TODO: add to log?
  ```
- `coredns/coredns` `plugin/normalize.go`

  ```
   TODO(miek): this should take escaping into account.
  ```
- `restic/restic` `internal/ui/signals/signals.go`

  ```
   XXX The fact that signals is a single global variable means that only one
   listener receives each incoming signal.
  ```
- `cheat/cheat` `internal/config/validate.go`

  ```
   TODO: assert valid styles?
  ```
- `mikefarah/yq` `pkg/yqlib/encoder_lua.go`

  ```
   TODO reject invalid use as a table key
  ```
- `gitleaks/gitleaks` `cmd/scm/scm.go`

  ```
   TODO: Add others.
  ```
- `pressly/goose` `provider.go`

  ```
   TODO(mf): let's add a warn-level log here to inform users if len(global) > 0. Would like
   to add this once we're on go1.21 and leverage the new slog package.
  ```
- `dominikh/go-tools` `staticcheck/sa1029/sa1029.go`

  ```
   TODO(dh): we should probably flag all anonymous structs, as they all risk collisions
  ```
- `golang-migrate/migrate` `internal/cli/commands.go`

  ```
   TODO remove again
  ```

## person

- `google/gops` `goprocess/goprocess.go`

  ```
   TODO(jbd): Return a list of errors.
  ```
- `open-policy-agent/opa` `v1/topdown/graphql.go`

  ```
   We use strings.TrimSuffix to remove the '.' characters that the library
   authors include on most of their validation errors. This should be safe,
   since variable names in their error messages are usually quoted, and
   this affects only the last character(s) in the string.
   NOTE(philipc): We know the error location will be in the query string,
   because schema validation always happens before this f…
  ```
- `google/pprof` `internal/report/source.go`

  ```
   TODO(sanjay): Report that the covered addresses are missing.
  ```
- `go-task/task` `compiler.go`

  ```
   NOTE(@andreynering): We're manually joining these paths here because
   this is the raw task, not the compiled one.
  ```
- `mvdan/gofumpt` `gofmt.go`

  ```
   NOTE(gofumpt): the argument-walking loop below is rewritten vs upstream.
   Upstream branched on os.Stat (file vs dir); gofumpt always uses
   filepath.WalkDir and tracks `explicit := path == arg` so that:
     - explicit non-.go and explicit generated files are still formatted;
     - vendor/testdata directories and go.mod `ignore` entries are skipped
       during walks but honored when named directly (s…
  ```
- `go-task/task` `compiler.go`

  ```
   NOTE(@andreynering): If a var have a specific dir, use this instead
  ```
- `go-task/task` `internal/filepathext/filepathext.go`

  ```
   NOTE(@andreynering): If the path contains any if the special
   variables that we know are absolute, return true.
  ```
- `cli/cli` `pkg/cmd/repo/read-file/http.go`

  ```
   We use the "application/vnd.github.object+json" media type to request a unified object
   representation from the Contents API. Without this, the API returns a JSON array for
   directories and a JSON object for files.
   TODO(api-client-rollout)
   This line of code is part of a mechanical roll out of the api client.
   As a follow up, consider whether the api client can be injected to this call site, rat…
  ```
- `jesseduffield/lazygit` `pkg/commands/git_commands/submodule.go`

  ```
   .gitmodules looks like this:
   [submodule "mysubmodule"]
     path = blah/mysubmodule
     url = git@github.com:subbo.git
  ```
- `grafana/k6` `lib/test_state.go`

  ```
   TODO(@mstoykov): move the whole type outside of lib later
  ```
- `pressly/goose` `internal/sqlparser/parse.go`

  ```
   TODO(mf): parse is called twice, once for up and once for down. This is inefficient. It
   should be possible to parse both directions in one pass. Also, UseTx is set once (but
   returned twice), which is unnecessary and potentially error-prone if the two calls to
   parseSQL disagree based on direction.
  ```
- `dominikh/go-tools` `go/ir/builder.go`

  ```
   T(e) = T(e.X) = T(e.Y) after untyped constants have been
   eliminated.
   TODO(adonovan): not true; MyBool==MyBool yields UntypedBool.
  ```
- `pressly/goose` `provider.go`

  ```
   TODO(mf): let's add a warn-level log here to inform users if len(global) > 0. Would like
   to add this once we're on go1.21 and leverage the new slog package.
  ```
- `go-delve/delve` `pkg/proc/gdbserial/gdbserver.go`

  ```
   This is a hardcoded breakpoint, ignore.
   TODO(deparker): There's an optimization here since we will do this
   again at a higher level to determine if we've stopped at a hardcoded breakpoint.
   We could set some state here so that we don't do extra work later.
  ```
- `nats-io/nats-server` `server/stream.go`

  ```
   If we have received this message across an account we may have request information attached.
   For now remove. TODO(dlc) - Should this be opt-in or opt-out?
  ```
- `mvdan/gofumpt` `format/rewrite.go`

  ```
   NOTE(gofumpt): the original cmd/gofmt/rewrite.go is mostly stripped here.
   gofumpt drops the -r flag (use `gofmt -r` instead), so the rewrite engine
   (initRewrite, parseExpr, rewriteFile, apply, set, subst) and its
   reflect-helpers (objectPtrNil, scopePtrNil, scopePtrType) are gone. Only
   match/isWildcard remain because simplify.go still uses them to compare
   AST literals when omitting redundant …
  ```
- `nektos/act` `pkg/container/docker_cli.go`

  ```
   TODO(thaJeztah): remove in next release (v30.0, or v29.x)
  ```
- `nektos/act` `pkg/container/docker_cli.go`

  ```
   TODO(thaJeztah): we need a flag-type to handle []netip.Addr directly
  ```
- `nektos/act` `pkg/container/docker_cli.go`

  ```
   TODO(thaJeztah): we need a flag-type to handle []netip.Addr directly
  ```
- `open-policy-agent/opa` `v1/ast/term.go`

  ```
   NOTE(anders): The code below is convoluted, but necessary
   since creating objects is something we do a lot and often
   on hot paths, this avoids allocating one objectElem per
   key-value pair, in favor of a single contiguous block of
   memory. The same technique is used in (*object).Copy(),
   for the same reasons.
  ```

## tracker reference

- `projectdiscovery/httpx` `runner/headless.go`

  ```
   TODO: Remove the Chrome PID kill code in favor of using Leakless(true).
   This change will be made if there are no complaints about zombie Chrome processes.
   Reference: https://github.com/projectdiscovery/httpx/pull/1426
   pids    map[int32]struct{}
  ```
- `kubernetes-sigs/kind` `pkg/internal/apis/config/default.go`

  ```
   this comment makes golint ignore this file, feel free to edit the file.
   Code generated by not-actually-generated-but-go-away-golint. DO NOT EDIT.
   https://github.com/kubernetes/code-generator/issues/30
  ```
- `restic/restic` `internal/archiver/archiver.go`

  ```
   two is a sweet spot for almost all situations. We've done some
   experiments documented here:
   https://github.com/borgbackup/borg/issues/3500
  ```
- `go-delve/delve` `pkg/proc/amd64util/debugregs.go`

  ```
   DebugRegisters represents x86 debug registers described in the Intel 64
   and IA-32 Architectures Software Developer's Manual, Vol. 3B, section
   17.2
  ```
- `dominikh/go-tools` `go/loader/loader.go`

  ```
   If a package consists only of test files, then
   go/packages incorrectly(?) returns an empty package for
   the non-test variant. Get rid of those packages. See
   #646.
  
   Do not, however, skip packages that have errors. Those,
   too, may have no files, but we want to print the
   errors.
  ```
- `open-policy-agent/opa` `v1/format/format.go`

  ```
   Compare against the row where the expression's terms end (its
   closing-bracket row), not where they begin. For a multi-line expression
   the lone leading `with` sits on the closing-bracket line, which differs
   from the start row, and comparing against the start would eject it onto
   its own indented line (see issue #8804).
  ```
- `FiloSottile/mkcert` `main.go`

  ```
   Version can be set at link time to override debug.BuildInfo.Main.Version,
   which is "(devel)" when building from within the module. See
   golang.org/issue/29814 and golang.org/issue/29228.
  ```
- `jesseduffield/lazydocker` `pkg/commands/docker.go`

  ```
   newDockerClient creates a Docker client with the given host.
   We avoid using client.FromEnv because it includes WithVersionFromEnv() which
   sets manualOverride=true when DOCKER_API_VERSION is set, preventing API version
   negotiation even when WithAPIVersionNegotiation() is specified.
   Instead, we explicitly configure only what we need, and rely on proper
   API version negotiation to support older …
  ```
- `nektos/act` `pkg/model/workflow.go`

  ```
   MaxParallel default value is `GitHub will maximize the number of jobs run in parallel depending on the available runners on GitHub-hosted virtual machines`
   So I take the liberty to hardcode default limit to 4 and this is because:
   1: tl;dr: self-hosted does only 1 parallel job - https://github.com/actions/runner/issues/639#issuecomment-825212735
   2: GH has 20 parallel job limit (for free tier) -…
  ```
- `google/go-containerregistry` `pkg/v1/remote/check.go`

  ```
   CheckPushPermission returns an error if the given keychain cannot authorize
   a push operation to the given ref.
  
   This can be useful to check whether the caller has permission to push an
   image before doing work to construct the image.
  
   TODO(#412): Remove the need for this method.
  ```
- `helm/helm` `pkg/cmd/history.go`

  ```
   This is an edge case that has happened in prod, though we don't
   know how: https://github.com/helm/helm/issues/1347
  ```
- `helm/helm` `pkg/cmd/history.go`

  ```
   This is an edge case that has happened in prod, though we don't
   know how: https://github.com/helm/helm/issues/1347
  ```
- `nats-io/nats-server` `server/auth_callout.go`

  ```
   FIX for https://github.com/nats-io/nats-server/issues/7841
   hand rolled creds on leafnode became crasher here
  ```
- `open-policy-agent/opa` `internal/methodlesstemplate/internal/fmtsort/sort.go`

  ```
   Note: this code is arranged to not panic even in the presence
   of a concurrent map update. The runtime is responsible for
   yelling loudly if that happens. See issue 33275.
  ```
- `etcd-io/bbolt` `db.go`

  ```
   Truncate and fsync to ensure file size metadata is flushed.
   https://github.com/boltdb/bolt/issues/284
  ```
- `gitleaks/gitleaks` `detect/location.go`

  ```
   Fixes: https://github.com/zricethezav/gitleaks/issues/1037
   When a fragment does NOT have any newlines, a default "newline"
   will be counted to make the subsequent location calculation logic work
   for fragments will no newlines.
  ```
- `coredns/coredns` `plugin/file/lookup.go`

  ```
   Anything else, including the REFUSED returned for a target outside every
   zone served here, keeps NOERROR: the CNAME is the answer and the resolver
   chases the target itself. See #7346.
  ```
- `go-task/task` `taskfile/node_git.go`

  ```
   repoCacheKey generates a unique cache key for the repository+ref combination.
   Unlike CacheKey() which includes the file path, this identifies the repository itself.
   Two GitNodes with the same repo+ref but different file paths will share the same cache.
  
   The identity is hashed into a single, filesystem-safe path segment. This prevents an
   attacker-controlled ref (e.g. "../../victim") from being…
  ```
- `projectdiscovery/httpx` `common/httpx/httpx.go`

  ```
   https://github.com/golang/go/issues/10069
  ```
- `pressly/goose` `migrate.go`

  ```
   The most recent record for each migration specifies
   whether it has been applied or rolled back.
   The first version we find that has been applied is the current version.
  
   TODO(mf): for historic reasons, we continue to use the is_applied column,
   but at some point we need to deprecate this logic and ideally remove
   this column.
  
   For context, see:
   https://github.com/pressly/goose/pull/131#pullre…
  ```

## URL

- `nektos/act` `pkg/runner/step_run.go`

  ```
   Reference: https://github.com/actions/runner/blob/8109c962f09d9acc473d92c595ff43afceddb347/src/Runner.Worker/Handlers/ScriptHandlerHelpers.cs#L47-L64
   Reference: https://github.com/actions/runner/blob/8109c962f09d9acc473d92c595ff43afceddb347/src/Runner.Worker/Handlers/ScriptHandlerHelpers.cs#L19-L27
  ```
- `projectdiscovery/httpx` `runner/headless.go`

  ```
   TODO: Remove the Chrome PID kill code in favor of using Leakless(true).
   This change will be made if there are no complaints about zombie Chrome processes.
   Reference: https://github.com/projectdiscovery/httpx/pull/1426
   pids    map[int32]struct{}
  ```
- `BurntSushi/toml` `doc.go`

  ```
   Package toml implements decoding and encoding of TOML files.
  
   This package supports TOML v1.0.0, as specified at https://toml.io
  
   The github.com/BurntSushi/toml/cmd/tomlv package implements a TOML validator,
   and can be used to verify if TOML document is valid. It can also be used to
   print the type of each key.
  ```
- `restic/restic` `internal/archiver/archiver.go`

  ```
   two is a sweet spot for almost all situations. We've done some
   experiments documented here:
   https://github.com/borgbackup/borg/issues/3500
  ```
- `nektos/act` `pkg/runner/step.go`

  ```
   https://github.com/github/docs/blob/3ae84420bd10997bb5f35f629ebb7160fe776eae/content/actions/reference/workflow-syntax-for-github-actions.md?plain=true#L962
  ```
- `dominikh/go-tools` `staticcheck/sa4005/sa4005.go`

  ```
   The analysis only considers the receiver and its first level
   fields. It doesn't look at other parameters, nor at nested
   fields.
  
   The analysis does not detect all kinds of dead stores, only
   those of fields that are never read after the write. That is,
   we do not flag 'a.x = 1; a.x = 2; _ = a.x'. We might explore
   this again if we add support for SROA to go/ir and implement
   https://github.com/…
  ```
- `OJ/gobuster` `gobustergcs/gobustersgcs.go`

  ```
   https://cloud.google.com/storage/docs/naming-buckets
  ```
- `FiloSottile/mkcert` `main.go`

  ```
   The system cert pool is only loaded once. After installing the root, checks
   will keep failing until the next execution. TODO: maybe execve?
   https://github.com/golang/go/issues/24540 (thanks, myself)
  ```
- `jesseduffield/lazydocker` `pkg/commands/docker.go`

  ```
   newDockerClient creates a Docker client with the given host.
   We avoid using client.FromEnv because it includes WithVersionFromEnv() which
   sets manualOverride=true when DOCKER_API_VERSION is set, preventing API version
   negotiation even when WithAPIVersionNegotiation() is specified.
   Instead, we explicitly configure only what we need, and rely on proper
   API version negotiation to support older …
  ```
- `go-task/task` `internal/goext/meta.go`

  ```
   NOTE(@andreynering): The lists in this file were copied from:
  
   https://github.com/golang/go/blob/master/src/go/build/syslist.go
  ```
- `mvdan/sh` `syntax/parser.go`

  ```
   LangPOSIX corresponds to the POSIX Shell language, as described at
   https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html.
  
   Its string representation is "posix" or "sh".
  ```
- `tomnomnom/gron` `token.go`

  ```
   \u2028 and \u2029 are separator runes that are not valid
   in javascript strings so they must be escaped.
   See http://timelessrepo.com/json-isnt-a-javascript-subset
  ```
- `go-delve/delve` `pkg/proc/linutil/dynamic.go`

  ```
   ElfUpdateSharedObjects reads the list of dynamic libraries loaded by the
   dynamic linker from the .dynamic section and uses it to update p.BinInfo().
   See the SysV ABI for a description of how the .dynamic section works:
   https://www.sco.com/developers/gabi/latest/contents.html
  ```
- `nektos/act` `pkg/filecollector/file_collector.go`

  ```
   return on non-regular files (thanks to [kumo](https://medium.com/@komuw/just-like-you-did-fbdd7df829d3) for this suggested update)
  ```
- `go-delve/delve` `pkg/dwarf/frame/entries.go`

  ```
   ptrEnc represents a pointer encoding value, used during eh_frame decoding
   to determine how pointers were encoded.
   Least significant 4 (0xf) bytes encode the size as well as its
   signed-ness,  most significant 4 bytes (0xf0 == ptrEncFlagsMask) are flags
   describing how the value should be interpreted (absolute, relative...)
   See https://www.airs.com/blog/archives/460.
  ```
- `gohugoio/hugo` `internal/js/esbuild/options.go`

  ```
   There is/was a bug in WebKit with severe performance issue with the tracking
   of TDZ checks in JavaScriptCore.
  
   Enabling this flag removes the TDZ and `const` assignment checks and
   may improve performance of larger JS codebases until the WebKit fix
   is in widespread use.
  
   See https://bugs.webkit.org/show_bug.cgi?id=199866
   Deprecated: This no longer have any effect and will be removed.
   TODO(b…
  ```
- `FiloSottile/mkcert` `cert.go`

  ```
   Certificates last for 2 years and 3 months, which is always less than
   825 days, the limit that macOS/iOS apply to all certificates,
   including custom roots. See https://support.apple.com/en-us/HT210176.
  ```
- `yuin/gopher-lua` `pm/pm.go`

  ```
   Simple recursive virtual machine based on the
   "Regular Expression Matching: the Virtual Machine Approach" (https://swtch.com/~rsc/regexp/regexp2.html)
  ```
- `restic/restic` `internal/backend/b2/config.go`

  ```
   checkBucketName tests the bucket name against the rules at
   https://help.backblaze.com/hc/en-us/articles/217666908-What-you-need-to-know-about-B2-Bucket-names
  ```
- `jesseduffield/lazydocker` `pkg/commands/errors.go`

  ```
   ComplexError an error which carries a code so that calling code has an easier job to do
   adapted from https://medium.com/yakka/better-go-error-handling-with-xerrors-1987650e0c79
  ```

## defect or workaround

- `go-task/task` `cmd/task/complete_cmd.go`

  ```
   Best-effort: a missing or broken Taskfile must not break completion.
  ```
- `gitleaks/gitleaks` `config/config.go`

  ```
   annoying hack to set the current config with the extended path
   since if extendDepth > 0 we are operating an extended config.
  ```
- `pressly/goose` `provider_run.go`

  ```
   bug(mf): this is a potential deadlock scenario. We're running Go migrations with *sql.DB,
   but are locking the database with *sql.Conn. If the caller sets max open connections to
   1, then this will deadlock because the Go migration will try to acquire a connection from
   the pool, but the pool is exhausted because the lock is held.
  
   A potential solution is to expose a third Go register function *…
  ```
- `gohugoio/hugo` `tpl/partials/partials.go`

  ```
   This will deadlock if we continue.
  ```
- `go-task/task` `taskfile/node_git.go`

  ```
   Clear the in-memory locks map to prevent memory leak
  ```
- `cli/cli` `pkg/cmd/release/download/download.go`

  ```
   adding application/json to Accept header due to a bug in the zipball/tarball API endpoint that makes it mandatory
  ```
- `go-delve/delve` `pkg/proc/bininfo.go`

  ```
   Function is covered, no need to apply workaround
  ```
- `antonmedv/fx` `internal/engine/engine.go`

  ```
   The engine runs in its own goroutine, where a panic would kill the
   process, the UI included. Whatever goes wrong in here, a bug in fx,
   in goja or in the input conversion, is reported as an error instead.
  ```
- `ffuf/ffuf` `pkg/runner/simple.go`

  ```
   lanePool hands out preflightLanes for per-thread preflight mode. ffuf runs a
   fresh goroutine per request (bounded to -t concurrent), so there is no
   persistent worker to pin per-thread state to; instead a request borrows a lane
   for the length of its Execute call and returns it. Lanes are reused, so a
   per-thread preflight chain runs once per lane and its vars are amortized across
   every request…
  ```
- `google/pprof` `profile/encode.go`

  ```
   If this a main linux kernel mapping with a relocation symbol suffix
   ("[kernel.kallsyms]_text"), extract said suffix.
   It is fairly hacky to handle at this level, but the alternatives appear even worse.
  ```
- `prometheus/node_exporter` `collector/diskstats_linux.go`

  ```
   rotationalLabel returns "1" for a rotational block device (HDD) or "0" for
   a non-rotational device (SSD/NVMe) by reading
   /sys/block/<dev>/queue/rotational via blockdevice.FS.
   On any error — for example when the device has no queue directory — it
   returns "0", preserving the backward-compatible behaviour of the previous
   code that zero-initialised BlockQueueStats on error (Rotational uint64…
  ```
- `jesseduffield/lazygit` `pkg/gui/controllers/branches_controller.go`

  ```
   I could do an explicit check here for whether the branch is tracking a remote branch
   but if we've selected it we'll already know that via Pullables and Pullables.
   Bit of a hack but I'm lazy.
  ```
- `grafana/k6` `internal/js/modules/k6/browser/common/frame_session.go`

  ```
   Executing a CDP command in the event parsing goroutine might deadlock in some cases.
   For example a deadlock happens if the content loaded in the frame that has navigated
   includes a JavaScript initiated dialog which we have to explicitly accept or dismiss
   (see onEventJavascriptDialogOpening). In that case our EvaluateGlobal call can't be
   executed, as the browser is waiting for us to accept/dis…
  ```
- `FiloSottile/mkcert` `truststore_java.go`

  ```
   execKeytool will execute a "keytool" command and if needed re-execute
   the command with commandWithSudo to work around file permissions.
  ```
- `mikefarah/yq` `pkg/yqlib/encoder_xml.go`

  ```
   hack so we can manually add newlines to procInst and directives
  ```
- `projectdiscovery/httpx` `common/httpx/domains.go`

  ```
   simple hack for android/ios package name
  ```
- `open-policy-agent/opa` `v1/ast/term.go`

  ```
   Prevents race condition around sorting.
   We can avoid (the allocation cost of) using a pointer here as all
   methods of `set` use a pointer receiver, and the `sync.Once` value
   is never copied.
  ```
- `gohugoio/hugo` `livereload/connection.go`

  ```
   There is a potential data race, especially visible with large files.
   This is protected by synchronization of the send channel's close.
  ```
- `helm/helm` `pkg/cmd/template.go`

  ```
   The following functions (writeToFile, createOrOpenFile, and ensureDirectoryForFile)
   are copied from the actions package. This is part of a change to correct a
   bug introduced by #8156. As part of the todo to refactor renderResources
   this duplicate code should be removed. It is added here so that the API
   surface area is as minimally impacted as possible in fixing the issue.
  ```
- `coredns/coredns` `plugin/forward/forward.go`

  ```
   Kick off health check to see if *our* upstream is broken.
  ```

## security

- `google/pprof` `internal/driver/webui.go`

  ```
   Pages served by the web UI need a nonce to run their scripts under the
   Content-Security-Policy, and standalone pages have no such policy.
  ```
- `open-policy-agent/opa` `v1/plugins/rest/aws.go`

  ```
   awsAssumeRoleCredentialService represents a STS credential service that uses active IAM credentials
   to obtain temporary security credentials generated by AWS STS via AssumeRole API operation
  ```
- `go-task/task` `executor.go`

  ```
   WithInsecure allows the [Executor] to make insecure connections when reading
   remote taskfiles. By default, insecure connections are rejected.
  ```
- `google/go-containerregistry` `pkg/name/options.go`

  ```
   Insecure is an Option that allows image references to be fetched without TLS.
  ```
- `google/pprof` `internal/driver/webui.go`

  ```
   Require both a loopback peer and a local Host header (to block DNS
   rebinding from untrusted web pages open in the user's browser).
  ```
- `dundee/gdu` `webui/tree.go`

  ```
   Reject ".." segments before cleaning. Archive entries (zip/tar) expose
   virtual paths built from untrusted member names; filepath.Clean would
   silently collapse a "../victim" segment into a real sibling path that
   happens to exist in the scanned tree, letting a malicious archive entry
   resolve to (and so disclose) an unrelated real node.
  ```
- `tsenart/vegeta` `lib/pacer.go`

  ```
   SinePacer is a Pacer that describes attack request rates with the equation:
  
  	R = MA sin(O+(2𝛑/P)t)
  
   Where:
  
  	R = Instantaneous attack rate at elapsed time t, hits per nanosecond
  	M = Mean attack rate over period P, sp.Mean, hits per nanosecond
  	A = Amplitude of sine wave, sp.Amp, hits per nanosecond
  	O = Offset of sine wave, sp.StartAt, radians
  	P = Period of sine wave, sp.Period, nanosecond…
  ```
- `ffuf/ffuf` `pkg/runner/simple.go`

  ```
   Bound the DECOMPRESSED read. The Content-Length guard above is not
   sufficient on its own: a malicious server can send a small compressed body
   (or a chunked response with no Content-Length, or one the transport
   transparently decoded and stripped the headers from) that expands to
   gigabytes. LimitReader caps the bytes we ever pull from the possibly-
   decompressing reader, so a decompression bomb…
  ```
- `caddyserver/caddy` `modules/caddyhttp/caddyauth/basicauth.go`

  ```
   Hasher is a type that can generate a secure hash
   given a plaintext. Hashing modules which implement
   this interface can be used with the hash-password
   subcommand as well as benefitting from anti-timing
   features. A hasher also returns a fake hash which
   can be used for timing side-channel mitigation.
  ```
- `caddyserver/caddy` `modules/caddyhttp/reverseproxy/reverseproxy.go`

  ```
   If ParseAddr fails (e.g. non-IP network like SCION), we cannot check
   if it is a trusted proxy by IP range. In this case, we ignore the
   error and treat the connection as untrusted (or retain existing status).
  ```
- `nektos/act` `cmd/actrc.go`

  ```
   projectActrcFlags lists the flags an untrusted ./.actrc may set. They
   select what to run and how to present it. Flags that reach host
   resources, change container isolation, or point act at other services are
   left out; set those in the user config or on the command line.
  ```
- `ffuf/ffuf` `pkg/runner/simple.go`

  ```
   Pin the target host: a value captured from an (untrusted) preflight response
   must not change which host this authenticated request is sent to.
  ```
- `charmbracelet/gum` `pager/command.go`

  ```
   Sanitize the input from stdin by removing backspace sequences.
  ```
- `direnv/direnv` `gzenv/gzenv.go`

  ```
   G110: Potential DoS vulnerability via decompression bomb (gosec)
   #nosec
  ```
- `google/go-containerregistry` `cmd/crane/cmd/rebase.go`

  ```
   Stupid hack to support insecure flag.
  ```
- `dominikh/go-tools` `go/ir/lift.go`

  ```
   TODO(adonovan): opt: there are many optimizations worth evaluating, and
   the conventional wisdom for SSA construction is that a simple
   algorithm well engineered often beats those of better asymptotic
   complexity on all but the most egregious inputs.
  
   Danny Berlin suggests that the Cooper et al. algorithm for
   computing the dominance frontier is superior to Cytron et al.
   Furthermore he recommen…
  ```
- `google/pprof` `internal/graph/dotgraph.go`

  ```
   Formatter formats the node's label prefix. Its output is treated as
   untrusted plain text: ComposeDot DOT-escapes each line while preserving
   newline ("\n") characters as centered DOT line breaks (\n).
  ```
- `direnv/direnv` `pkg/sri/sri.go`

  ```
   Package sri implements helper functions to calculate SubResource Integrity
   hashes.
   https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity
  ```
- `coredns/coredns` `plugin/loadbalance/weighted.go`

  ```
   #nosec G501 -- used only as a checksum for file change detection (not for security).
  ```
- `dundee/gdu` `webui/server.go`

  ```
   isLocalRequest reports whether r both originates from a loopback address
   and targets a loopback Host header. Checking RemoteAddr alone is not
   enough: a page on an attacker-controlled domain that DNS-rebinds to
   127.0.0.1 also connects from loopback, but the browser still sends the
   attacker's hostname as the Host header, which this rejects.
  ```

## credential handling

- `grafana/k6` `secretsource/manager.go`

  ```
   Manager manages secrets making certain for them to be redacted from logs
  ```
- `nats-io/nats-server` `server/errors.go`

  ```
   ErrRevocation is returned when a credential has been revoked.
  ```
- `jesseduffield/lazygit` `pkg/commands/oscommands/cmd_obj_runner.go`

  ```
   this function takes each word of output from the command and builds up a string to see if we're being asked for a password
  ```
- `schollz/croc` `src/crypt/crypt.go`

  ```
   New generates a new key based on a passphrase and salt
  ```
- `go-task/task` `taskfile/ast/cmd.go`

  ```
   Command with secrets masked (used for logging)
  ```
- `schollz/croc` `src/redact/redact.go`

  ```
   String replaces each non-empty secret, longest first.
  ```
- `coredns/coredns` `core/dnsserver/server.go`

  ```
   TSIG secrets of all served zones; must not be modified as it's concurrently accessed by DNS server.
  ```
- `jesseduffield/lazydocker` `pkg/gui/confirmation_panel.go`

  ```
   It is very important that within this function we never include the original prompt in any error messages, because it may contain e.g. a user password.
   The golangcilint unparam linter complains that handleClose is alwans nil but one day it won't be nil.
   nolint:unparam
  ```
- `caddyserver/caddy` `modules/caddyhttp/caddyauth/basicauth.go`

  ```
   Hasher is a type that can generate a secure hash
   given a plaintext. Hashing modules which implement
   this interface can be used with the hash-password
   subcommand as well as benefitting from anti-timing
   features. A hasher also returns a fake hash which
   can be used for timing side-channel mitigation.
  ```
- `gohugoio/hugo` `config/services/servicesConfig.go`

  ```
   App or Client Access Token.
   If you are using a Client Access Token, remember that you must combine it with your App ID
   using a pipe symbol (<APPID>|<CLIENTTOKEN>) otherwise the request will fail.
  ```
- `hairyhenderson/gomplate` `crypto/rsa.go`

  ```
   RSADecrypt - decrypt the ciphertext with the given private key. The key
   must be a PEM-encoded RSA private key in PKCS#1, ASN.1 DER form, typically
   beginning with "RSA PRIVATE KEY". The input text must be plain ciphertext,
   not base64-encoded.
  ```
- `restic/restic` `internal/backend/swift/config.go`

  ```
   Application Credential auth
  ```
- `projectdiscovery/httpx` `common/authprovider/authx/file.go`

  ```
   Bearer Auth token
  ```
- `mikefarah/yq` `pkg/yqlib/operator_shuffle.go`

  ```
   ignore CWE-338 gosec issue of not using crypto/rand
   this is just to shuffle an array rather generating a
   secret or something that needs proper rand.
  ```
- `cli/cli` `internal/gh/gh.go`

  ```
   ActiveTokenWithRefresh retrieves the active credential for the given hostname and, when it is refreshable and
   its access token is expiring, renews and persists it. It may return a non-empty credential with an error, such
   as when refreshing fails. The caller decides whether to use the returned credential.
  ```
- `grafana/k6` `internal/js/modules/k6/webcrypto/subtle_crypto.go`

  ```
   Sign generates a digital signature.
  
   It takes as its arguments a key to sign with, some algorithm-specific parameters, and the data to sign.
   It returns a Promise which will be fulfilled with the signature.
  
   Note that if the `algorithm` parameter identifies a public-key cryptosystem, the `key` parameter
   should be a private key.
  
   You can use the corresponding `SubtleCrypto.Verify` method to ve…
  ```
- `nektos/act` `pkg/artifacts/server.go`

  ```
   requireRuntimeToken requires a bearer token signed with authKey on every route
   except the v4 upload and download URLs, which authorize themselves with the URL signature.
   The token's run ID must match the run named by the request.
  ```
- `gohugoio/hugo` `tpl/internal/go_templates/htmltemplate/content.go`

  ```
   contentTypeUnsafe is used in attr.go for values that affect how
   embedded content and network messages are formed, vetted,
   or interpreted; or which credentials network messages carry.
  ```
- `jesseduffield/lazygit` `pkg/commands/oscommands/cmd_obj_runner.go`

  ```
   runAndDetectCredentialRequest detect a username / password / passphrase question in a command
   promptUserForCredential is a function that gets executed when this function detect you need to fill in a password or passphrase
   The promptUserForCredential argument will be "username", "password" or "passphrase" and expects the user's password/passphrase or username back
  ```
- `go-task/task` `internal/templater/secrets.go`

  ```
   Fast path: if there are no secrets to mask, resolve the template directly
   without the extra DeepCopy + masking pass.
  ```

## rationale or constraint

- `nektos/act` `pkg/container/docker_socket.go`

  ```
   Here there is no DOCKER_HOST _and_ the supplied container socket is not a valid URI (either invalid or a file path)
   Cases: 2B <- but is already handled at the top
   I.e. this path should never be taken
  ```
- `tomnomnom/gron` `ungron.go`

  ```
   a runeCheck is a function that determines if a rune is valid
   or not so that we can do complex checks against runes
  ```
- `hairyhenderson/gomplate` `internal/datafs/wdfs.go`

  ```
   create a new osfs.FS here, because we can't modify the original if
   SubVolume was already called on it.
  ```
- `mikefarah/yq` `pkg/yqlib/decoder_toml.go`

  ```
   otherwise, if there is a value, it must be some key value pairs of the
   first object in the array!
  ```
- `schollz/croc` `internal/cli/flag_string.go`

  ```
   TakesValue returns true of the flag takes a value, otherwise false
  ```
- `tsenart/vegeta` `flags.go`

  ```
   headers is the http.Header used in each target request
   it is defined here to implement the flag.Value interface
   in order to support multiple identical flags for request header
   specification
  ```
- `ffuf/ffuf` `pkg/output/stdout.go`

  ```
   Only colorize the heart when colors are enabled; otherwise a redirected
   or non-terminal stderr gets raw ANSI escape bytes in the banner.
  ```
- `yuin/gopher-lua` `alloc.go`

  ```
   check if we need a new alloc page
  ```
- `etcd-io/bbolt` `bucket.go`

  ```
   Materialize the root node if it hasn't been already so that the
   bucket will be saved during commit.
  ```
- `cloudflare/cfssl` `scan/crypto/tls/handshake_client.go`

  ```
   mutualProtocol finds the mutual Next Protocol Negotiation or ALPN protocol
   given list of possible protocols and a list of the preference order. The
   first list must not be empty. It returns the resulting protocol and flag
   indicating if the fallback case was reached.
  ```
- `google/pprof` `internal/elfexec/elfexec.go`

  ```
   Some tools may introduce a fake mapping that spans the entire
   address space. Assume that the address has already been
   adjusted, so no additional base adjustment is necessary.
  ```
- `coredns/coredns` `plugin/siit/siit.go`

  ```
   Do not modify if question is not A or not of class IN. See RFC 6147 5.1
  ```
- `jesseduffield/lazydocker` `pkg/gui/layout.go`

  ```
   view not specified in dimensions object: so create the view and hide it
   making the view take up the whole space in the background in case it needs
   to render content as soon as it appears, because lazyloaded content (via a pty task)
   cares about the size of the view.
  ```
- `golang-migrate/migrate` `migrate.go`

  ```
   DefaultPrefetchMigrations sets the number of migrations to pre-read
   from the source. This is helpful if the source is remote, but has little
   effect for a local source (i.e. file system).
   Please note that this setting has a major impact on the memory usage,
   since each pre-read migration is buffered in memory. See DefaultBufferSize.
  ```
- `golang-migrate/migrate` `database/driver.go`

  ```
   Lock should acquire a database lock so that only one migration process
   can run at a time. Migrate will call this function before Run is called.
   If the implementation can't provide this functionality, return nil.
   Return database.ErrLocked if database is already locked.
  ```
- `grafana/k6` `internal/js/modules/k6/browser/common/connection.go`

  ```
   According to the WS RFC[1], we might want to wait for a response
   Control frame back from the browser here (possibly for the above
   timeout duration), but Chrom{e,ium} never sends one, even when
   the browser process exits normally after the Browser.close CDP
   command. So we don't bother waiting, since it would just needlessly
   delay the k6 iteration.
   [1]: https://www.rfc-editor.org/rfc/rfc6455#s…
  ```
- `tomnomnom/gron` `main.go`

  ```
   Go's maps do not have well-defined ordering, but we want a consistent
   output for a given input, so we must sort the statements
  ```
- `tomnomnom/gron` `main.go`

  ```
   Go's maps do not have well-defined ordering, but we want a consistent
   output for a given input, so we must sort the statements
  ```
- `grafana/k6` `internal/js/modules/k6/browser/common/frame.go`

  ```
   TextContent returns the textContent attribute of the first element found
   that matches the selector. The second return value is true if the returned
   text content is not null or empty, and false otherwise.
  ```
- `mvdan/gofumpt` `internal/govendor/go/doc/comment/text.go`

  ```
   The algorithm requires that our scoring function be concave,
   meaning that for all i₀ ≤ i₁ < j₀ ≤ j₁,
   weight(i₀, j₀) + weight(i₁, j₁) ≤ weight(i₀, j₁) + weight(i₁, j₀).
  
   Our weights are two-element pairs [hi, lo]
   ordered by elementwise comparison.
   The hi entry counts the weight for lines that are longer than max,
   and the lo entry counts the weight for lines that…
  ```

## disabled code

- `traefik/yaegi` `interp/cfg.go`

  ```
   for init; ; {}
  ```
- `BurntSushi/toml` `encode.go`

  ```
   Need to also check if all the fields are empty, otherwise something
   like this with uncomparable types will always return true:
  
     type a struct{ field b }
     type b struct{ s []string }
     s := a{field: b{s: []string{"AAA"}}}
  ```
- `dominikh/go-tools` `go/loader/loader.go`

  ```
   runtime.SetFinalizer(pkg, func(pkg *Package) {
   	log.Println("Unloading package", pkg.PkgPath)
   })
  ```
- `dominikh/go-tools` `go/loader/loader.go`

  ```
   runtime.SetFinalizer(pkg, func(pkg *Package) {
   	log.Println("Unloading package", pkg.PkgPath)
   })
  ```
- `coredns/coredns` `plugin/file/zone.go`

  ```
   r.(*dns.SRV).Target = strings.ToLower(r.(*dns.SRV).Target)
  ```
- `etcd-io/bbolt` `db.go`

  ```
   gofail: var unmapError string
   return errors.New(unmapError)
  ```
- `cloudflare/cfssl` `scan/tls_handshake.go`

  ```
  certSigAlgs = append(certSigAlgs, certs[0].SignatureAlgorithm)
  ```
- `cloudflare/cfssl` `scan/tls_handshake.go`

  ```
  certSigAlgs = append(certSigAlgs, certs[0].SignatureAlgorithm)
  ```
- `gohugoio/hugo` `hugolib/pages_capture.go`

  ```
   seen := map[types.Strings2]hugofs.FileMetaInfo{}
  ```
- `schollz/croc` `main.go`

  ```
   "github.com/pkg/profile"
   go func() {
   	for {
   		f, err := os.Create("croc.pprof")
   		if err != nil {
   			panic(err)
   		}
   		runtime.GC() // get up-to-date statistics
   		if err := pprof.WriteHeapProfile(f); err != nil {
   			panic(err)
   		}
   		f.Close()
   		time.Sleep(3 * time.Second)
   		fmt.Println("wrote profile")
   	}
   }()
  ```
- `mvdan/gofumpt` `internal/govendor/go/printer/nodes.go`

  ```
   len(lit.Value) >= 2
  ```
- `nats-io/nats-server` `server/pse/pse_linux.go`

  ```
   int64(C.sysconf(C._SC_CLK_TCK))
  ```
- `grafana/k6` `internal/js/modules/k6/browser/chromium/browser_type.go`

  ```
   Force the first page to be blank, instead of the welcome page;
   --no-first-run doesn't enforce that.
   args = append(args, common.BlankPage)
   args = append(args, "--no-startup-window")
  ```
- `grafana/k6` `internal/ds/histogram/hdr.go`

  ```
   256 = 1 << (k+1)
  ```
- `go-delve/delve` `pkg/internal/gosym/symtab.go`

  ```
  func (T *Table) GetInlineTree(f* Func, s *elf.Symbol, baseaddr uint64, progReader io.ReaderAt) ([]InlinedCall, error) {
  strver := fmt.Sprint(s.goVersion)
  goFuncValue := FuncSymName(strver)
  return T.go12line.InlineTree(f,goFuncValue,baseaddr,progReader)
  ```
- `wagoodman/dive` `runtime/ui/view/layer.go`

  ```
   err := CursorUp(v.gui, v.body)
  ```
- `mvdan/sh` `syntax/parser.go`

  ```
   a+=b
  ```
- `mvdan/sh` `syntax/parser.go`

  ```
   for i, arg := range args {
   	if arg, ok := arg.(fmt.Stringer); ok && arg != _EOF {
   		args[i] = quotedToken(arg)
   	}
   }
  ```
- `wagoodman/dive` `runtime/ui/view/filetree.go`

  ```
   var requestedWidth = int(float64(available) * (1.0 - v.requestedWidthRatio))
   return &requestedWidth
  ```
- `gohugoio/hugo` `common/hugo/hugo.go`

  ```
   program := "Hugo Static Site Generator"
  ```
