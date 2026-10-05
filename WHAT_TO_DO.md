# ProofHold — What You Have To Do

Read **[IDEA.md](./IDEA.md)** first. Then follow this file **in order**.

This is the work plan, not the codebase. Each step has a **done when** checklist. Do not start Step N+1 until Step N is done.

**Stack (locked):** Java 21, Spring Boot 3, PostgreSQL, React + TypeScript, OpenAPI 3.

---

## How to use this file

- Treat each step as a ticket.
- After a step, you should be able to **demo or explain** it in an interview.
- Keep a `qa/` folder for test plans and bug reports as you go — QA is not “after coding.”
- If a step tempts you to add maps, SMS, or multi-tenant SaaS, stop. That is out of v1.

Suggested repo layout when you start coding:

```
ProofHold/
  IDEA.md
  WHAT_TO_DO.md
  docs/
    openapi.yaml
  qa/
    TEST_PLAN.md
    TEST_CASES.md
    BUG_TEMPLATE.md
    EXPLORATORY_CHARTER.md
  backend/          # Spring Boot
  frontend/         # React + TypeScript
```

---

## Step 0 — Set up the folder and rules

**Do**

1. Keep this `ProofHold/` folder as the project root.
2. Write a one-line rule on a sticky note: *Public search must not leak enough to fake a claim.*
3. Decide one demo location: `Library Front Desk`.
4. Decide three demo users:
   - `staff@proofhold.local` / role `STAFF`
   - `alice@proofhold.local` / role `CLAIMER`
   - `bob@proofhold.local` / role `CLAIMER` (second claimer, for conflict tests)

**Done when**

- [ ] You can explain the product in 60 seconds without notes.
- [ ] You know the three users and one location by heart.

---

## Step 1 — Write the API contract before any UI

You are practicing **RESTful API Engineer** work here. The contract is the product.

**Do**

1. Create `docs/openapi.yaml`.
2. Define resources: `Location`, `Item`, `Challenge`, `Claim`, `Handoff`, `AuditEvent`.
3. Specify every v1 path from IDEA.md.
4. For each endpoint document:
   - auth required or not
   - request body
   - success response
   - error responses (`400`, `401`, `403`, `404`, `409`, `422`)
   - whether the item payload is **redacted** or **full**
5. Define the problem-details error shape (RFC 7807), for example:

   ```json
   {
     "type": "https://proofhold.local/problems/item-already-verified",
     "title": "Item already verified",
     "status": 409,
     "detail": "Item 18 already has a verified claim.",
     "instance": "/v1/items/18/claims"
   }
   ```

6. Define pagination: `page`, `size`, `totalElements`, `totalPages` (or cursor — pick one and stick to it).
7. Define `Idempotency-Key` on `POST /v1/items/{id}/claims` and `POST /v1/items/{id}/handoffs`.
8. Define `ETag` / `If-Match` on claim decision and status changes.

**Redaction rule (write this into the OpenAPI descriptions)**

| Field | Public list/detail | Staff | Winning claimer after verify |
|---|---|---|---|
| category, location, found date | yes | yes | yes |
| photo | no | yes | yes |
| serial / unique marks | no | yes | yes |
| full description | no | yes | yes |
| challenge questions | no | yes | only while submitting a claim |
| challenge answers | never | staff only | never |

**Done when**

- [ ] Another engineer could implement the backend from `openapi.yaml` alone.
- [ ] You can point to which fields are hidden on public `GET /v1/items`.

---

## Step 2 — Data model and illegal transitions

You are practicing **Backend Software Engineer** work here.

**Do**

1. Create a Spring Boot app with PostgreSQL.
2. Tables (minimum):

   | Table | Key columns |
   |---|---|
   | `users` | id, email, password_hash, role |
   | `locations` | id, name, timezone, open_from, open_to |
   | `items` | id, location_id, status, category, hold_until, version |
   | `item_secrets` | item_id, photo_url, serial, full_description |
   | `challenges` | id, item_id, prompt, expected_answer_hash |
   | `claims` | id, item_id, claimer_id, status, idempotency_key |
   | `handoffs` | id, item_id, claim_id, slot_start, slot_end |
   | `audit_events` | id, item_id, actor_id, action, at, payload |

3. Store challenge **answers hashed** (or normalized + hashed). Never store them as raw display text if you can avoid it.
4. Put a `version` column on `items` for optimistic locking.
5. Write the state machine in one place (a small Java class, not scattered ifs).

**Allowed transitions**

```
LOGGED            → HELD
HELD              → CLAIM_PENDING | EXPIRED
CLAIM_PENDING     → VERIFIED | HELD | EXPIRED
VERIFIED          → READY_FOR_PICKUP | HELD
READY_FOR_PICKUP  → RETURNED | EXPIRED
EXPIRED           → DONATED
```

Anything else → `409` or `422`, and an audit event.

**Done when**

- [ ] Schema is created and a seed location + three users exist.
- [ ] A unit test rejects `RETURNED → HELD`.
- [ ] A unit test allows `HELD → CLAIM_PENDING`.

---

## Step 3 — Auth and roles

**Do**

1. Register / login (JWT is enough for v1).
2. Protect staff-only endpoints: create item, view secrets, decide claim, donate.
3. Public (or authenticated-claimer) search may list redacted items.
4. Return `401` when missing token, `403` when the role is wrong.
5. Never trust a `role` field from the client body. Read it from the authenticated user.

**Done when**

- [ ] Staff can create an item; a claimer cannot.
- [ ] A claimer calling `POST /v1/claims/{id}/decision` gets `403`.
- [ ] You have API tests for 401 and 403.

---

## Step 4 — Staff logs an item

**Do**

1. `POST /v1/items` (STAFF): category, where found, hold-until, 2–3 challenges, optional photo metadata.
2. Item starts as `LOGGED`, then immediately `HELD` (or auto-transition in the same transaction).
3. Write audit: `ITEM_LOGGED`.
4. Validate: at least two challenges, hold-until in the future, category required.

**Done when**

- [ ] Staff can log “black leather wallet” with two questions.
- [ ] Public `GET /v1/items/{id}` does **not** include answers, photo, or serial.
- [ ] Staff `GET` does include those fields.

---

## Step 5 — Redacted search

**Do**

1. `GET /v1/items` with filters: `locationId`, `category`, `status`, `q`, `page`, `size`.
2. Default sort: newest found first.
3. Public/claimer response DTO is a **different type** from the staff DTO. Do not reuse one entity and “forget to hide a field.”
4. `q` may match category and **coarse** location text only — not serial, not full description.

**Done when**

- [ ] Search returns pagination metadata.
- [ ] A test asserts photo, serial, full description, and challenge answers are absent from public JSON.
- [ ] Filtering by category works.

This step is a highlight for **both** API and QA interviews (information leak).

---

## Step 6 — Submit a claim (idempotent)

**Do**

1. `POST /v1/items/{id}/claims` with answers + header `Idempotency-Key`.
2. Same key + same body → return the original claim, not a duplicate.
3. Item status → `CLAIM_PENDING` if it was `HELD`.
4. If item is already `VERIFIED` / `RETURNED` / `DONATED` → `409`.
5. Score answers simply: normalize case/space, compare to stored hash or normalized expected value.
6. Do **not** auto-verify. Staff decides. (Auto-verify is a later extra, not v1.)
7. Audit: `CLAIM_SUBMITTED`.

**Done when**

- [ ] Alice can claim item 1.
- [ ] Repeating the POST with the same idempotency key does not create a second row.
- [ ] Claiming a `RETURNED` item returns `409` with a problem-details body.

---

## Step 7 — Staff decision + optimistic locking

**Do**

1. `POST /v1/claims/{id}/decision` with `{ "decision": "APPROVE" | "REJECT", "reason": "..." }` and `If-Match` etag.
2. Approve:
   - claim → `VERIFIED`
   - item → `VERIFIED`
   - **all other pending claims** on that item → `REJECTED` with reason `another claim was verified`
3. Reject: that claim → `REJECTED`; if no pending claims remain, item → `HELD`.
4. If two staff approve two claims at once, the second update must fail on `version` / `If-Match` (`412` or `409`).
5. Audit both outcomes.

**Done when**

- [ ] Approving Alice rejects Bob automatically.
- [ ] A concurrency test (or a documented manual race) shows the second approve fails.
- [ ] Illegal decision on a `RETURNED` item fails.

This is the **backend interview** centerpiece. Be ready to draw it.

---

## Step 8 — Handoff (pickup window)

**Do**

1. After verify, `POST /v1/items/{id}/handoffs` with a slot.
2. Slot must be inside the location’s open hours.
3. Slot must be in the future.
4. Only the winning claimer or staff can create it.
5. Item → `READY_FOR_PICKUP`.
6. A later staff action `POST /v1/handoffs/{id}/complete` → item `RETURNED`.
7. Idempotency key on create.

**Done when**

- [ ] Pickup outside open hours returns `422`.
- [ ] Completing handoff sets `RETURNED`.
- [ ] `RETURNED` items disappear from public “still held” search (or show as returned only to staff).

---

## Step 9 — Expiry job

**Do**

1. A scheduled task (every minute or every 15 minutes is fine).
2. If `hold_until` is past and status is `HELD` or `CLAIM_PENDING` → `EXPIRED`.
3. Staff can `POST /v1/items/{id}/donate` on `EXPIRED` → `DONATED`.
4. Do not expire `VERIFIED` / `READY_FOR_PICKUP` without a product rule. In v1: if pickup was never completed and hold ended, keep them until staff completes or cancels. Document that choice.

**Done when**

- [ ] A test uses a hold-until in the past and the job moves the item to `EXPIRED`.
- [ ] Donate is staff-only.

---

## Step 10 — Frontend: claimer app

You are practicing **Front End Software Engineer** work here.

**Do**

1. React + TypeScript app that talks only to `/v1` (no hidden backend shortcuts).
2. Pages:
   - Login
   - Search (redacted cards, filters, pagination)
   - Item detail (redacted)
   - Claim wizard (questions, submit, success/error)
   - My claims
3. UI states you must handle, not “happy path only”:
   - loading
   - empty search
   - 401 session expired
   - 409 already verified
   - 422 field errors under the inputs
4. Do not render fields the API did not send. If the API leaks a field, the UI should still not be the thing that “hides” security — but you should also not display secrets if a bug returns them. Prefer typed DTOs.

**Done when**

- [ ] Alice can search, open a wallet card, answer questions, and see “pending staff review.”
- [ ] Keyboard can complete the claim wizard.
- [ ] Empty and error states are visible, not blank screens.

---

## Step 11 — Frontend: staff app

**Do**

Same app, role-based routes (or `/staff` section):

1. Log item form (challenges list editor).
2. Queue of `CLAIM_PENDING` items.
3. Decision panel: see secrets, see answers, approve/reject with reason.
4. Book or confirm pickup; mark returned; donate expired items.
5. Audit timeline on the item.

**Done when**

- [ ] Staff can finish the full loop: log → see Alice’s claim → approve → mark returned.
- [ ] Staff cannot get stuck when Bob’s claim was auto-rejected.
- [ ] A claimer hitting `/staff` is blocked in the UI **and** by the API.

---

## Step 12 — QA pack (Software QA Engineer)

Create these files under `qa/`. Fill them with **this** product, not generic text.

### `qa/TEST_PLAN.md`

Include:

- In-scope / out-of-scope
- Environments (local Docker)
- Roles under test
- Risk list (information leak is #1)
- Test types: smoke, function, negative, security, concurrency, regression
- Entry/exit criteria

### `qa/TEST_CASES.md`

Write cases with IDs, for example:

| ID | Title | Type | Priority |
|---|---|---|---|
| TC-01 | Public search hides photo and serial | Security | P0 |
| TC-02 | Claimer cannot approve a claim | AuthZ | P0 |
| TC-03 | Duplicate Idempotency-Key does not double-claim | API | P0 |
| TC-04 | Second approve loses on version conflict | Concurrency | P0 |
| TC-05 | Pickup outside open hours is 422 | Validation | P1 |
| TC-06 | Expired hold cannot be newly claimed | State | P0 |
| TC-07 | Pagination stays stable when filters change | API | P1 |
| TC-08 | Staff sees audit after reject | Function | P2 |

Each case: preconditions, steps, expected result, requirement id (endpoint or status rule).

### `qa/BUG_TEMPLATE.md`

```
Title:
Severity / Priority:
Environment:
Steps:
Expected:
Actual:
Evidence (status code, screenshot, response JSON):
Role used:
```

### `qa/EXPLORATORY_CHARTER.md`

Charter example: *“As a dishonest claimer, try to reconstruct the wallet from public search, error messages, and timing of 409s.”* Timebox 45 minutes. Log notes.

**Done when**

- [ ] P0 cases exist for leak, authz, idempotency, and double-approve.
- [ ] You can show a traceability line: *rule in IDEA → OpenAPI → test case*.

---

## Step 13 — Automated tests (Software Tester + engineers)

**Do**

1. **Unit:** state machine transitions; answer normalization.
2. **API (REST Assured or MockMvc):**
   - redacted vs staff JSON
   - 401/403
   - idempotent claim
   - 409 on returned item
   - problem-details `content-type` and body
3. **UI (Playwright):** one happy path  
   staff logs item → Alice claims → staff approves → pickup booked.
4. Run the same P0 cases from Step 12; mark each automated or manual.

**Done when**

- [ ] `./mvnw test` (or equivalent) is green.
- [ ] One Playwright spec is green.
- [ ] You can name which P0 cases are still manual.

---

## Step 14 — Polish for interviews (do this last)

**Do**

1. Root `README.md`: pitch, how to run, demo users, screenshot list.
2. Sequence diagram: claim → decision → handoff.
3. Known limitations (no SMS, one location, no image ML).
4. Practice out loud:
   - 60-second product pitch
   - how redaction is enforced in a **separate DTO**, not `null` hope
   - how optimistic locking prevents two winners
   - how you would test an information leak
   - why `POST /claims/{id}/decision` is more RESTful than `POST /approveClaim`

**Done when**

- [ ] A stranger can clone, run, and complete the happy path.
- [ ] You can answer follow-ups for **all five** job titles using this one repo.

---

## Definition of v1 done

All of these must be true:

1. OpenAPI matches the running API.
2. Public search cannot show photo, serial, full description, or answers.
3. Only one verified claim can exist per item.
4. Idempotent claim create works.
5. Frontend covers claimer + staff happy path plus 409/422.
6. QA pack exists with P0 cases.
7. Unit + API tests + one Playwright path pass.

If a feature is not in this list, it is not v1.

---

## Suggested build order in one week (optional)

| Day | Focus |
|---|---|
| 1 | Steps 0–2 (contract + schema + state machine) |
| 2 | Steps 3–5 (auth, log item, redacted search) |
| 3 | Steps 6–7 (claims + decision + locking) |
| 4 | Steps 8–9 (handoff + expiry) |
| 5 | Steps 10–11 (frontend) |
| 6 | Steps 12–13 (QA pack + automation) |
| 7 | Step 14 (README, diagrams, interview rehearsal) |

---

## What you should not do yet

- Do not add a second microservice.
- Do not add Slack/SMS/email vendors.
- Do not add a map.
- Do not replace the desk with a generic “AI finder.”
- Do not copy AegisPay’s payroll/compliance domain into this repo.

Stay on ProofHold until v1 is done. Then use **[IDEA.md](./IDEA.md)** as the story you tell in interviews.
