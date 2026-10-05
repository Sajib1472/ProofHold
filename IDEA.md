# ProofHold — Product Idea

**One-sentence pitch:** A lost-and-found desk where owners **prove** an item is theirs without ever seeing the photo.

This project exists so you can practice, explain, and demonstrate work for:

- Front End Software Engineer
- Backend Software Engineer
- Software QA Engineer
- Software Tester
- RESTful API Engineer

It is intentionally **small**, **unique**, and **interview-shaped**. It is not a todo app, shop, chat, or payroll product (you already have AegisPay).

---

## Why this product

Most portfolio apps leak data on purpose: public catalogs, public photos, public descriptions. ProofHold’s product rule is the opposite:

> The API must not leak enough information for someone to fake a claim.

That single constraint gives you privacy, a state machine, concurrency, redacted UI, and a clean REST contract — the four things interviewers actually probe.

Everyday settings: campus security desk, coworking lobby, gym, library, airport, hospital waiting room.

---

## Who uses it

| Role | What they do |
|---|---|
| **Finder** | Hands an item to the desk (or a staff member logs it for them). |
| **Staff** | Logs the item, writes secret challenge questions, verifies claims, books pickup. |
| **Claimer** | Searches redacted results, answers challenges, books a pickup if approved. |

v1 has one desk (one location). Multi-tenant SaaS is out of scope.

---

## Domain

| Resource | Meaning |
|---|---|
| **Location** | One physical desk (Library, Gym Front Desk, Lobby). |
| **Item** | A found object: category, colors, brand, where found, hold-until date. |
| **Challenge** | Secret questions only the true owner should know. |
| **Claim** | Someone asserting ownership of an item. |
| **Handoff** | A booked pickup window at the desk. |
| **Audit event** | Who viewed, claimed, approved, or rejected — and when. |

### Item statuses

```
LOGGED → HELD → CLAIM_PENDING → VERIFIED → READY_FOR_PICKUP → RETURNED
```

Also: `REJECTED`, `EXPIRED`, `DONATED`.

Rules that make the domain real:

- Public search never returns photos, serial numbers, or challenge answers.
- Only one **verified** claim may win.
- Two people claiming the same item at the same time is a concurrency bug, not a product feature.
- After `hold-until`, unclaimed items become `EXPIRED`, then `DONATED`.
- A `RETURNED` item cannot go back to `HELD`.

---

## Tiny example (use this in READMEs and interviews)

Staff logs: *black leather wallet, Library 2nd floor.*

Public search shows: *Wallet · Library · found Oct 5 · 2 claims pending.*

Hidden challenges: *What initials are inside?* and *About how many cards?*

Wrong answers → claim `REJECTED`.  
Right answers + staff approval → `READY_FOR_PICKUP` Tuesday 2–4pm.

---

## Suggested stack

Match the family you already use on AegisPay so skills transfer:

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3 |
| Database | PostgreSQL |
| Frontend | React + TypeScript |
| API contract | OpenAPI 3 + `application/problem+json` errors |
| API tests | JUnit + REST Assured (or Postman/Newman) |
| UI tests | Playwright |

No Redis, Kafka, or microservices in v1.

---

## What each job role practices

### Front End Software Engineer

- Public search with **redacted cards** (no photo, no serial, no exact description).
- Claim wizard: pick item → answer challenges → choose pickup slot.
- Staff queue: approve / reject with a reason.
- Loading, empty, 409 conflict, 422 validation, and expired-hold states.
- Accessible forms, keyboard flow, role-based nav (claimer vs staff).

### Backend Software Engineer

- Auth and roles: `STAFF`, `CLAIMER`, `FINDER`.
- State machine that rejects illegal transitions.
- Optimistic locking so two staff cannot verify two claims on the same item.
- Hold-until expiry as a simple scheduled task.
- Append-only audit log.
- Public endpoints never return challenge answers or full photos.

### RESTful API Engineer

Core resources (v1):

```
GET    /v1/locations
POST   /v1/items
GET    /v1/items?locationId=&category=&status=&q=&page=&size=
GET    /v1/items/{id}            // redacted unless STAFF or winning claimee
POST   /v1/items/{id}/claims
POST   /v1/claims/{id}/decision
POST   /v1/items/{id}/handoffs
GET    /v1/items/{id}/audit
```

API rules to implement and defend in interviews:

- URL versioning (`/v1`).
- Pagination and a stable sort on lists.
- Filter + search on collection resources, not random RPC names.
- `Idempotency-Key` on `POST /claims` and `POST /handoffs`.
- `409 Conflict` when the item is already verified.
- `422` with field-level validation errors.
- RFC 7807 problem details.
- `ETag` / `If-Match` on status changes.
- OpenAPI as the source of truth.
- No verbs in paths (`/approve` is a smell; use `POST .../decision`).

### Software QA Engineer

Write risk-based tests, not screen-click scripts:

- Photo or serial leaked on public search.
- Claimer can read other people’s challenge answers.
- Two claims submitted in the same second.
- Staff verifies after the hold expired.
- Pickup booked outside desk hours.
- Item donated while a claim is still pending.
- Role confusion: finder acting as staff.
- Pagination broken when filters change.

QA artifacts: test plan, requirement → test traceability, severity/priority bugs, regression suite, exploratory charter.

### Software Tester

- Manual happy path: log item → search → claim → verify → pickup.
- Boundary: hold expires at midnight in the desk’s timezone.
- Negative: empty challenge answers, duplicate email, expired JWT.
- API tests for status codes and redaction.
- Playwright smoke: claim wizard + staff decision.
- Bug reports with steps, expected, actual, evidence, environment.

---

## v1 scope (build this, then stop)

1. Login and three roles.
2. Staff logs an item with 2–3 challenge questions.
3. Public search returns redacted results.
4. Claim + staff decision.
5. Pickup slot (handoff).
6. Expiry → `EXPIRED` / `DONATED`.
7. OpenAPI + problem errors.
8. Unit tests, API tests, and one Playwright happy path.

### Not in v1

Maps, SMS, payments, image ML, multi-tenant SaaS, native mobile apps, email providers, Kafka, Redis.

Those features dilute the interview story.

---

## Interview talking point

> I built ProofHold, a lost-and-found API where search is public but proof of ownership is private. The hard part was designing resources so a claimer cannot scrape enough detail to fake a claim, while staff still get a verification queue. Items are a state machine with optimistic locking so only one claim can win.

That sentence works for frontend (redaction UX), backend (state + locking), API (resource design), and QA (information leak as a test type).

---

## Next document

Follow **[WHAT_TO_DO.md](./WHAT_TO_DO.md)** in order. Do not skip ahead to UI polish or extra features until the step you are on is done.
