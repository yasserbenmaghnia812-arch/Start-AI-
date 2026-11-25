# ULTRA AI PHONE AGENT — MVP Architecture

## Goal

Build an Android-first agent that turns natural language into an authorized, observable task plan. The first milestone creates seams that can be tested independently without pretending that cloud AI, Accessibility, MCP, or Godot integration already works.

## Execution cycle

```text
request -> intent -> plan -> permission check -> tool call -> observation -> evaluation -> report/recover
```

Every execution state is serializable so a long-running task can be paused, resumed, or recovered after process interruption.

## Boundaries

- Android restricted capabilities require explicit onboarding and user consent.
- File operations are scoped and must not expose unrestricted filesystem access.
- Sensitive actions require confirmation immediately before execution.
- No arbitrary shell tool is exposed.
- The backend will own model credentials; the Android client will use short-lived authenticated sessions.
- The system must distinguish planning from execution and must observe important results.

## Milestone 1 deliverables

- `ultra/mcp`: typed tool definitions and an allowlisted registry.
- `ultra/agent`: task state and deterministic initial planning seam.
- `ultra/accessibility`: service seam that reports permission state and current package.
- Documentation and CI entry points.

## Planned modules

| Module | Responsibility | Status |
|---|---|---|
| Android UI | Compose conversation and task timeline | Existing foundation; next milestone |
| Agent | Planning, execution, observation, recovery | Contracts added |
| MCP | Discovery, validation, transport, auth | Registry seam added |
| Accessibility | Semantic Android UI interaction | Service seam added |
| Vision | OCR and consented MediaProjection analysis | Planned |
| Memory | User-controlled short/long/project/task memory | Existing foundation; integration planned |
| Backend | FastAPI, WebSocket, PostgreSQL, Redis | Planned; connectors not configured |
| Godot | Scoped project tools and remote/local bridge | Planned |

## Verification policy

A feature is only marked complete after its unit/integration tests pass. Device-only capabilities require a physical Android test and are explicitly labelled otherwise.
