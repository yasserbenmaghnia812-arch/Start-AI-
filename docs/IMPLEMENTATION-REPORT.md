# ToolNeuron-cai implementation report

## Scope delivered

This change set keeps ToolNeuron's existing offline Android architecture and adds an original MCP boundary around its existing plugin registry. It does not add OpenAI, Anthropic, Claude Code, Node.js, or any paid runtime dependency.

The implementation includes:

- `McpProtocol.kt`: JSON-RPC 2.0 handling for `initialize`, `ping`, `tools/list`, `tools/call`, and initialized notifications.
- `LocalMcpHttpServer.kt`: loopback-only Streamable HTTP adapter with token authentication, Origin checks, request-size limits, and socket timeout.
- `McpRuntime.kt`: explicit opt-in lifecycle API; no socket is opened automatically by `Application.onCreate`.
- `McpProtocolTest.kt`: protocol tests for negotiation, notifications, errors, and tool result envelopes.
- A `local.properties` guard in `ums` and a quoted empty `KEY_ALIAS` fallback, fixing clean-checkout Gradle configuration and Java generation.
- Explicit NDK `26.3.11579264` for all native library modules.
- `.github/workflows/android.yml` for a reproducible x86_64 GitHub build that runs unit tests and produces debug/release APK artifacts.
- `docs/MCP.md` and `NOTICE` provenance/security documentation.

## Source and license audit

| Source | License | Adapted material |
|---|---|---|
| ToolNeuron-cai | Apache-2.0 | Existing application and plugin architecture |
| `morluto/rea` | MIT | MCP contract discipline, local/evidence-oriented boundaries, security limitations |
| `affaan-m/ECC` | MIT | security gates, capability limiting, verify-before-release workflow |
| `Jakeschincariol/replica-skill` | MIT | staged recon → architecture → build → test workflow and clean-room rule |

REA's Node runtime, commercial reverse-engineering providers, GPL/LGPL components,
ECC's model-provider adapters, and Replica's Claude/Playwright runtime are not
bundled into the APK. The integration uses ideas and public contract patterns,
not copied source, branding, private APIs, or proprietary content.

## Validation

The following checks were executed in the sandbox:

- `:ums:compileDebugJavaWithJavac`: passed after the `KEY_ALIAS` fallback fix.
- Full `testDebugUnitTest`/`assembleDebug`: not completed locally because the sandbox is ARM64 and the Android SDK's `aidl`/CMake binaries are x86-64. The failure is environmental and occurs before or outside the new MCP code.
- A native ARM64 CMake 3.28 installation was added for local diagnosis, but the official Android `aidl` binary remains architecture-incompatible.
- GitHub Actions uses `ubuntu-24.04` x86_64 and installs the pinned SDK/NDK/CMake versions, so it is the authoritative full APK build lane.
- No Android device or emulator was available; loopback interoperability, native inference, and installation behavior remain device-level validation items.

## Reproduction

```bash
# Required: JDK 17, Android SDK 36, NDK 26.3.11579264, CMake 3.22.1
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

The generated APKs are under `app/build/outputs/apk/`. The CI workflow uploads them as the `toolneuron-apks` artifact. `local.properties` is machine-local and must not be committed.

## Remaining release gates

Before claiming a production APK, run the GitHub workflow on x86_64, inspect the release APK with `apkanalyzer`, install it on an Android 10+ ARM64 device, verify the existing GGUF/JNI flows, and manually exercise the MCP endpoint from a trusted local client. The endpoint must remain opt-in and must not be exposed beyond loopback without a separate authenticated/TLS design.
