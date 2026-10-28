# Fix pack — 2026-10-28-refactor-visitdto-and-petdt

Snapshot at reviewed_head: re-read each file before editing. Ids and scope come from
code-review-final.local.json; this file never overrides it.

## CR-001 · code · major — Unreadable-body 400 handler body is unasserted
Location: `src/test/java/org/springframework/samples/petclinic/rest/controller/VisitRestControllerV1Tests.java:177`
Recommendation: In VisitRestControllerV1Tests.testCreateVisitError (and PetRestControllerV1Tests.testUpdatePetNullNameReturns400), add jsonPath assertions for $.title ("HttpMessageNotReadableException") and $.detail ("The request contains invalid or missing parameters"), and assert the body does not contain the record constructor message (e.g. "description must be between").
Window read (`src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java` lines 64-72, 159-169):
```java
private ProblemDetail detailBuild(Exception e, HttpStatus status, StringBuffer url, String detail) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setType(URI.create(url.toString()));
    problemDetail.setTitle(e.getClass().getSimpleName());
    problemDetail.setDetail(detail);
    ...
}
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
    ...
    ProblemDetail detail = this.detailBuild(e, status, request.getRequestURL(), ERROR_INVALID_REQUEST);
    return ResponseEntity.status(status).body(detail);
}
```
Also checked: `ExceptionControllerAdvice.java:67` — title is the exception simple name, detail is the ERROR_INVALID_REQUEST constant.
Tests: `src/test/java/org/springframework/samples/petclinic/rest/controller/VisitRestControllerV1Tests.java`, `src/test/java/org/springframework/samples/petclinic/rest/controller/PetRestControllerV1Tests.java`
