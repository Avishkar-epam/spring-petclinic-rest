# Code review — 2026-10-28-refactor-visitdto-and-petdt (2026-10-28)

**request-changes** · confidence: low · 1 blocking · 2 deferred · 17 filtered as noise
Coverage: blind ✓ · edge-case ✓ · verification-gap ✓ · acceptance — n/a (no spec)  (3/3 applicable lenses ran)

No spec or story was supplied, so confidence is capped at low.

## Look here first

- `src/test/java/org/springframework/samples/petclinic/rest/controller/VisitRestControllerV1Tests.java:177` — [public API] new global 400 handler for unreadable bodies has no response-body assertion; every added 400 test stops at the status — CR-001

## Checked and clean

Handler detail is a constant (no Jackson or constructor message leaks today) · DB column limits match the record bounds · `java.version` is consumed by the enforcer · 2 deferred → code-review-deferred.local.md
