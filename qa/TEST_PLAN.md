# ProofHold test plan

## Product under test

Lost-and-found desk. Public search must not leak enough to fake a claim. v1: one location (Library Front Desk), roles STAFF / CLAIMER / FINDER.

## In scope

- Auth: login, register (CLAIMER only), JWT roles
- Item log, redacted search, GET by id (public vs staff vs winning claimer)
- Claims: idempotency, scoring, staff decision, If-Match
- Handoff hours, complete → RETURNED
- Expiry of HELD / CLAIM_PENDING; donate EXPIRED
- Claimer and staff UI states (loading, empty, 401, 409, 422)

## Out of scope

SMS, maps, payments, image ML, multi-tenant SaaS, native mobile, VERIFIED auto-expiry (see `docs/EXPIRY.md`)

## Environment

Local Docker PostgreSQL (`docker-compose.yml`), API `http://localhost:8080`, UI `http://localhost:5173`. Seed users in `docs/seed.yaml`.

## Roles under test

| Role | Seed |
|---|---|
| STAFF | staff@proofhold.local / proofhold |
| CLAIMER | alice@proofhold.local / proofhold |
| CLAIMER (conflict) | bob@proofhold.local / proofhold |

## Risks (highest first)

1. Information leak on public search or error bodies (photo, serial, full description, challenge answers)
2. Authorization bypass (claimer decide/donate/audit)
3. Double-verified winner (lost If-Match / lost unique verified claim)
4. Idempotency creating duplicate claims
5. Pickup outside desk hours accepted
6. Expired hold still claimable
7. Pagination sort/filter drift

## Test types

- Smoke: login, search, one staff log
- Function: happy path log → claim → approve → pickup → return
- Negative: 401/403/409/422
- Security: redaction, role checks
- Concurrency: two approves, stale If-Match
- Regression: after each step, rerun P0

## Entry criteria

OpenAPI `docs/openapi.yaml` matches implemented `/v1` paths. Database migrated. Seed users present.

## Exit criteria

All P0 cases pass (automated or signed manual). No open P0 bugs. Playwright happy path green when stack is running.
