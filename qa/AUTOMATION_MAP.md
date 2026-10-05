# P0 automation map

| ID | Automated? | Where |
|---|---|---|
| TC-01 | Automated | `ItemViewsRedactionTest`, `ItemServiceSearchTest` |
| TC-02 | Automated | `AuthAccessApiTest.decideClaimAsClaimerIs403` |
| TC-03 | Automated | `ClaimServiceSubmitTest.sameIdempotencyKeyDoesNotCreateASecondRow` |
| TC-04 | Automated | `ClaimServiceDecisionTest` (Alice rejects Bob; stale If-Match 412) |
| TC-05 | Automated | `HandoffServiceTest.pickupOutsideOpenHoursIs422` |
| TC-06 | Manual | Needs running expiry job + live API |
| TC-09 | Automated | `ItemServiceLogTest.publicGetHidesSecretsThatStaffSee` |
| TC-10 | Automated | `ClaimServiceSubmitTest.claimingReturnedItemIs409` |

TC-07, TC-08 remain manual until a live API suite is added.

UI happy path (not a P0 ID): `frontend/e2e/happy-path.spec.ts` — staff log → Alice claim → approve → book pickup. Requires API + Vite running.

