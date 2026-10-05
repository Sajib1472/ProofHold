# Interview rehearsal

Say these out loud before a screen share. Keep the product in one sentence, then pick the role.

## 60-second pitch

I built ProofHold, a lost-and-found API where search is public but proof of ownership is private. Staff log an item with secret questions. Search returns a redacted card — no photo, serial, or answers — so a stranger cannot scrape enough to fake a claim. A claimer answers the questions; staff approve or reject. Only one claim can win, using optimistic locking on the item version. Then we book a pickup inside desk hours. I am not building a generic CRUD catalog. The hard part is the resource design and the leak tests.

## Frontend

The UI never “hides” security by omitting fields it received. Public pages are typed as `PublicItem`. If a bug leaked `serial`, we still would not render unknown keys — but the real control is the staff vs public DTO on the server. I handled loading, empty search, session 401, 409 already verified, and 422 under inputs. Staff routes are blocked in the router **and** return 403 from the API.

## Backend

Item status lives in one class, `ItemStateMachine`. Illegal transitions throw; they become 409. Approve Alice sets the item VERIFIED and rejects every other pending claim with `another claim was verified`. A second staff approve with a stale `If-Match` is 412. Challenge expected answers are hashed (normalized case/space). Audit is append-only and must not store secrets.

## REST API

`POST /v1/claims/{id}/decision` is a decision **resource**, not `POST /approveClaim`. Verbs in paths do not scale (reject, override, undo). Collections are filtered and paginated. Creates that must not double-submit take `Idempotency-Key`. Status changes take `ETag` / `If-Match`. Errors are RFC 7807 `application/problem+json` with a typed `type` URL (`item-already-verified`, `illegal-transition`, `outside_hours`). OpenAPI is the source of truth.

## QA

Highest risk is an information leak, not a missing button. P0s: public JSON redaction, claimer cannot decide, idempotent claim, double-approve / If-Match. Traceability is IDEA rule → OpenAPI operation → `qa/TEST_CASES.md` id → `qa/AUTOMATION_MAP.md`. Exploratory charter: try to reconstruct the wallet from public search and error timing.

## Tester

Happy path: log → Alice claims → staff approves → pickup booked (`frontend/e2e/happy-path.spec.ts`). API tests cover 401/403 and problem JSON. Unit tests cover RETURNED→HELD rejected and hash normalization. Still manual: expiry job on a live clock (TC-06), pagination filter reset (TC-07), audit after reject (TC-08). Bug reports use `qa/BUG_TEMPLATE.md`.
