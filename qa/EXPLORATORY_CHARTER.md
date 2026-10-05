# Exploratory charter — fake a claim from leaks

**Timebox:** 45 minutes  
**Persona:** Dishonest claimer who only has public search and HTTP errors.

## Charter

As a dishonest claimer, try to reconstruct the wallet from public search, error messages, and timing of 409s. Try to harvest enough detail to pass challenge questions without being the owner.

## Hunt

- Public `GET /v1/items` and `GET /v1/items/{id}`
- 401/403/409/422 bodies (do they echo serial, prompts, answers?)
- Pagination and `q=` (does `q` search serial or full description?)
- Claimer `GET /v1/items/{id}/challenges` then guess
- Staff routes with a claimer token
- Repeat Idempotency-Key with a mutated body

## Notes

Date:

Leaks found:

Could a stranger answer the challenges from public JSON alone?

Stop when: timebox ends or a P0 leak is confirmed (file a bug with `qa/BUG_TEMPLATE.md`).
