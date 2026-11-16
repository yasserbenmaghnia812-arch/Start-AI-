# ToolNeuron MCP

ToolNeuron now exposes its enabled local plugins through a small Android-native MCP implementation. It uses the MCP JSON-RPC contract and the Streamable HTTP request/response form, without a paid model API or a Node.js runtime.

## What is implemented

- `initialize`, `notifications/initialized`, `ping`, `tools/list`, and `tools/call`.
- MCP tool schemas are generated from the existing `ToolDefinitionBuilder` definitions.
- Tool execution is delegated to the existing `PluginManager`, so plugin enable/disable state and existing validation remain authoritative.
- The HTTP adapter binds only to `127.0.0.1` and uses a per-process bearer-like token in `X-ToolNeuron-Token`.
- Requests with a non-local `Origin` are rejected. Request bodies are capped at 1 MiB and sockets have a read timeout.
- The socket is opt-in. The application does not open a local MCP port during startup.

The current adapter returns one JSON response for a request and `202 Accepted` for notifications. It intentionally does not advertise an SSE stream because the on-device tools are request/response operations. A future streaming tool can add SSE without changing `McpProtocol`.

## Starting it from an Android integration

```kotlin
val endpoint = McpRuntime.start()
// endpoint.port is the loopback port; endpoint.token is required in X-ToolNeuron-Token
// Call McpRuntime.stop() when the integration is disabled or the app is destroyed.
```

Example request:

```http
POST /mcp HTTP/1.1
Host: 127.0.0.1:PORT
Content-Type: application/json
Accept: application/json, text/event-stream
MCP-Protocol-Version: 2025-06-18
Origin: http://localhost
X-ToolNeuron-Token: TOKEN

{"jsonrpc":"2.0","id":1,"method":"tools/list"}
```

The token must never be logged, placed in a URL, or exposed to another application. Because the endpoint is loopback-only, other devices cannot connect to it directly. A future settings screen should make the opt-in state visible and revoke the token on stop.

## Adapted ideas and provenance

The integration uses concepts, not copied runtime code:

- REA: explicit MCP contracts, local analysis boundaries, evidence-oriented errors, and a clear prohibition on unauthorized reverse engineering.
- ECC: security-first agent operation, capability-limited tools, explicit verification, and test/review gates.
- Replica Skill: staged recon/architecture/build/test workflow and clean-room handling of functionality rather than copying source, branding, content, or private APIs.

The source repositories remain separate development references and are not bundled as Android dependencies. Their authored code is MIT-licensed; ToolNeuron remains Apache-2.0. See `NOTICE` and the project report for attribution and dependency details.

## Build and test

Requirements are Android Studio/SDK 36+, NDK 26.x, CMake 3.22.1, JDK 17, and the checked-in Gradle wrapper. From the repository root:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APKs are written under `app/build/outputs/apk/`. A physical Android device is required to validate native GGUF inference, ABI packaging, and actual loopback-client interoperability. The repository includes protocol unit tests, but no claim of device validation should be made until an Android SDK/NDK toolchain and a real device or emulator are available.

## Security notes

MCP servers must validate origin and should bind local servers to localhost. ToolNeuron follows both rules. The application also keeps the existing plugin enablement boundary: an MCP client cannot call a disabled plugin. Do not expose this server through a reverse proxy or bind it to `0.0.0.0` without adding authenticated remote transport, TLS, replay protection, and an explicit user-facing permission flow.
