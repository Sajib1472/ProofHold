# Hold expiry (v1)

The scheduled job runs about once a minute.

**It expires** items in `HELD` or `CLAIM_PENDING` whose `hold_until` is in the past → `EXPIRED`.

**It does not expire** `VERIFIED` or `READY_FOR_PICKUP`. If a winning claimer never books pickup, staff completes or cancels the handoff. The hold clock is not used as a silent forfeit after verification.

Staff may `POST /v1/items/{id}/donate` on `EXPIRED` items only → `DONATED`.
