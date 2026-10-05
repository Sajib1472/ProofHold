# ProofHold test cases

Traceability: IDEA.md product rule → docs/openapi.yaml operation → this ID → automation in `qa/AUTOMATION_MAP.md`.

## TC-01 Public search hides photo and serial

- **Type:** Security · **Priority:** P0
- **Requirement:** OpenAPI `PublicItem`; IDEA redaction table; `GET /v1/items`
- **Preconditions:** Staff logged a wallet with photoUrl and serial.
- **Steps:** Unauthenticated `GET /v1/items`. Inspect JSON.
- **Expected:** No `photoUrl`, `serial`, `fullDescription`, `challenges`, or answers.

## TC-02 Claimer cannot approve a claim

- **Type:** AuthZ · **Priority:** P0
- **Requirement:** `POST /v1/claims/{claimId}/decision` STAFF; SecurityConfig
- **Preconditions:** Alice JWT. Claim 1 exists.
- **Steps:** Alice `POST /v1/claims/1/decision` with APPROVE.
- **Expected:** 403 `application/problem+json`.

## TC-03 Duplicate Idempotency-Key does not double-claim

- **Type:** API · **Priority:** P0
- **Requirement:** `POST /v1/items/{id}/claims` Idempotency-Key
- **Preconditions:** Item HELD. Alice token. UUID K.
- **Steps:** POST claim twice with K and the same body.
- **Expected:** One claims row. Second response 200 with original id.

## TC-04 Second approve loses on version conflict

- **Type:** Concurrency · **Priority:** P0
- **Requirement:** If-Match / item `version`; OpenAPI 412
- **Preconditions:** Alice and Bob pending on one item. Version 3.
- **Steps:** Staff APPROVE Alice with If-Match `"3"`. Staff APPROVE Bob with If-Match `"3"`.
- **Expected:** Alice VERIFIED, Bob REJECTED (`another claim was verified`). Second approve 412.

## TC-05 Pickup outside open hours is 422

- **Type:** Validation · **Priority:** P1
- **Requirement:** `POST /v1/items/{id}/handoffs`; Library Front Desk 09:00–17:00 America/New_York
- **Steps:** Book 20:00–21:00 local.
- **Expected:** 422 problem, field `slotStart` code `outside_hours`.

## TC-06 Expired hold cannot be newly claimed

- **Type:** State · **Priority:** P0
- **Requirement:** Expiry job; claim rejectIfUnclaimable
- **Preconditions:** Item HELD, hold_until in the past. Job ran → EXPIRED.
- **Steps:** Alice POST claim.
- **Expected:** 409 illegal-transition. Not PENDING.

## TC-07 Pagination stays stable when filters change

- **Type:** API · **Priority:** P1
- **Requirement:** `GET /v1/items` sort foundAt desc, id desc
- **Steps:** page=0 size=10 category=WALLET then change q. Confirm page metadata resets and sort unchanged.
- **Expected:** `page.page` 0 after new filter. No secret fields in content.

## TC-08 Staff sees audit after reject

- **Type:** Function · **Priority:** P2
- **Requirement:** `GET /v1/items/{id}/audit` STAFF; AuditAction CLAIM_REJECTED
- **Steps:** Staff REJECT. GET audit.
- **Expected:** Event CLAIM_REJECTED. Payload has no answers/serial/photo.

## Additional P0 coverage

## TC-09 Public GET item hides secrets staff GET shows

- **Type:** Security · **Priority:** P0
- **Requirement:** `GET /v1/items/{id}` redaction
- **Expected:** Public JSON lacks photo/serial/description; staff JSON includes them (prompts, not answer hashes).

## TC-10 Claiming RETURNED is 409 problem+json

- **Type:** API · **Priority:** P0
- **Requirement:** item-already-verified
- **Expected:** 409, content-type application/problem+json, type contains `item-already-verified`.
