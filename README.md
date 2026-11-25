# ULTRA AI PHONE AGENT

Next-generation autonomous AI assistant for Android, built incrementally on top of the existing Android foundation.

## Milestone 1: Secure MVP foundation

This branch (`feature/ultra-ai-mvp`) introduces the first production-oriented foundation:

- AI-first project identity and architecture documentation.
- Strict MCP tool contracts and an allowlisted registry.
- Structured task state for planning and observation.
- An Android AccessibilityService seam that requires explicit user enablement.
- GitHub Actions build and test pipeline inherited from the Android project.

## Product direction

The assistant accepts natural-language text or voice, plans authorized actions, invokes discovered tools, observes the result, and reports progress. Destructive or sensitive actions require explicit confirmation. It never bypasses Android permissions, authentication, payment controls, or another application's security.

## Repository map

```text
android foundation: app/
architecture docs: docs/
MVP agent contracts: app/src/main/java/com/dark/tool_neuron/ultra/
future backend: backend/
tests: app/src/test/ and app/src/androidTest/
```

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/` after a successful build. A real device test is still required before claiming Android integration.

## Current limitations

- The AI backend, PostgreSQL, Redis, and remote MCP transport are not connected in this milestone.
- Accessibility must be enabled manually by the user in Android Settings.
- The six Godot development tools are contracts only at this stage; arbitrary shell execution is intentionally excluded.
- APK generation is delegated to the existing GitHub Actions workflow and is not claimed until that workflow passes.

## Security baseline

- Project changes are developed on a feature branch, not `main`.
- Tool names are allowlisted.
- Tool calls carry permission and confirmation metadata.
- Credentials and permanent administrator secrets must remain outside the APK.
- Sensitive operations stop for user confirmation.

## Roadmap

1. MVP contracts, task state, and permission seams.
2. Compose AI conversation shell and local task timeline.
3. Accessibility tree inspection and safe Android tools.
4. MCP JSON-RPC transport and tool discovery tests.
5. FastAPI backend with authenticated WebSocket streaming.
6. Godot workspace tools using a scoped project directory.
7. Device testing, recovery tests, and signed release automation.
