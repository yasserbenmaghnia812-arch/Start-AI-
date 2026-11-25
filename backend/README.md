# Backend boundary

The FastAPI/PostgreSQL/Redis/WebSocket backend is intentionally staged after the Android contracts.

## Required properties

- OpenAI and provider credentials remain server-side.
- Every session uses authentication, expiration, rate limiting, and audit logging.
- MCP tool discovery is authenticated and returns only an allowlisted tool schema.
- The backend cannot bypass Android user consent; the Android client remains the authority for device permissions.
- WebSocket events must expose task progress without exposing secrets.

## Planned layout

```text
backend/
├── api/
├── agents/
├── mcp/
├── security/
├── database/
└── services/
```

No backend connection is claimed in Milestone 1.
