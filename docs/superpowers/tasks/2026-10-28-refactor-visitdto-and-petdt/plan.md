# VisitDto / PetDto Java 21 Records Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generated `PetDto`/`VisitDto` POJOs with hand-written Java 21 records (compact-constructor validation) and make the MapStruct mappers, controllers and tests work with them.

**Architecture:** Records live in `src/main/java/org/springframework/samples/petclinic/rest/model/` (outside the JaCoCo-excluded `rest/dto`). openapi-generator is told via `schemaMappings` not to generate `Pet`/`Visit` and to reference the records from `PetsApi`/`VisitsApi`/`OwnersApi`, `OwnerDto` and `PetPageDto`. `openapi.yml` is unchanged and stays the API contract.

**Tech Stack:** Java 21, Spring Boot 4.1.1 (Jackson 3), MapStruct 1.6.3 (native record support), openapi-generator-maven-plugin 7.25.0, JUnit 5/MockMvc, JaCoCo (85% line / 66% branch).

**Spec:** none (inline requirements); see `technical-analysis.md` in this directory.

## Acceptance criteria

- [ ] `java.version` is 21 and the project builds with release 21.
- [ ] `PetDto` and `VisitDto` are hand-written records; no `PetDto.java`/`VisitDto.java` is generated into `rest/dto`.
- [ ] Compact constructors reject null/out-of-bounds values (see Task 1) and null-guard/defensively copy `PetDto.visits`.
- [ ] Bean Validation, Jackson, `@Schema` and `@PetAgeValidation` annotations are preserved on the record components.
- [ ] `VisitMapper`, `PetMapper`, `OwnerMapper` compile and map correctly with records; the PetMapper<->VisitMapper circular `uses` is gone.
- [ ] Invalid Pet/Visit request bodies still return HTTP 400 (not 500).
- [ ] `OwnerDto` and all other DTOs remain generated; no custom mustache templates.
- [ ] `mvn clean verify` passes including JaCoCo gates.

## Global Constraints

- Assumption: Java 21 is set (`<java.version>21</java.version>`; JDK 21 installed); records must keep API behavior unchanged.
- Run `mvn clean ...` for builds so stale generated `rest/dto/PetDto.java` / `VisitDto.java` in `target/` are gone. Never edit `target/`.
- Do not commit or push; the orchestrator commits later. Do not stage the untracked `.vscode/` and `docs/`.
- No Copilot call and no prompt file; mappers are edited directly.

## Review Focus

- Entity with null `birthDate`/`type`/`description` mapped to a record throws at mapping time (Task 3 fixtures must be complete).
- Invalid JSON body (null name, name > 30 chars, empty visit description, negative petId) must return 400 via the new `HttpMessageNotReadableException` handler, not 500.
- `PetDto` JSON without `visits` deserializes to an empty list, never null.
- Future `birthDate` still yields 400 through `@PetAgeValidation` (annotation must land on the record field).
- `OwnerDto` JSON with nested pets/visits still serializes identically (field names, date format `yyyy-MM-dd`).

---

### Task 1: Java 21 and the two records

**Files:**
- Modify: `pom.xml:20-52` (add `<java.version>21</java.version>` to `<properties>`)
- Create: `src/main/java/org/springframework/samples/petclinic/rest/model/VisitDto.java`
- Create: `src/main/java/org/springframework/samples/petclinic/rest/model/PetDto.java`
- Test: `src/test/java/org/springframework/samples/petclinic/rest/model/VisitDtoTests.java`, `PetDtoTests.java`

**Interfaces:**
- Produces: `record VisitDto(@Nullable LocalDate date, String description, @Nullable Integer id, Integer petId)` and `record PetDto(String name, LocalDate birthDate, PetTypeDto type, @Nullable Integer id, @Nullable Integer ownerId, List<VisitDto> visits)` (`PetTypeDto` is the generated `rest.dto.PetTypeDto`; `VisitDto` is the new record).
- Component order matches the generated field order. Accessors are record-style (`name()`, `petId()`).

Test-first: yes — `VisitDtoTests.rejectsEmptyDescription` and `PetDtoTests.rejectsNullName` fail (class missing) until the records exist.

- [ ] **Step 1: Write failing tests.** `VisitDtoTests`: valid record builds; `description` null, `""`, and 256 chars each throw `IllegalArgumentException`; `petId` null or -1 throws; `id` -1 throws, `id` null and `date` null are accepted. `PetDtoTests`: valid record builds; `name` null or 31 chars, `birthDate` null, `type` null, `id`/`ownerId` -1 throw `IllegalArgumentException`; `visits` null becomes `List.of()`; later mutation of the passed list does not change `visits()`; `visits().add(..)` throws `UnsupportedOperationException`. Also: a Jakarta `Validator` reports a violation for a future `birthDate` (pins `@PetAgeValidation`), and a Jackson 3 `JsonMapper` round trip of each record keeps JSON property names and `yyyy-MM-dd` dates; JSON lacking `name` throws a `tools.jackson.core.JacksonException`.
- [ ] **Step 2:** Run `mvn -q clean test -Dtest='VisitDtoTests,PetDtoTests'`; expect compile failure/FAIL.
- [ ] **Step 3: Implement both records.** Copy the annotations from the generated classes onto the components: `@NotNull`/`@Size`/`@Min`/`@Valid`, `@JsonProperty("<name>")`, `@Schema(name, example, description, requiredMode/accessMode)` (values from `openapi.yml:2154-2307`), `@DateTimeFormat(iso = ISO.DATE)` on dates, `@PetAgeValidation` on `birthDate` (target is FIELD, which record components propagate to), plus record-level `@Schema(name = "Pet"/"Visit", ...)` and `@JsonTypeName`. Compact constructors throw `IllegalArgumentException` with a message naming the component; bounds: visit `description` 1..255, `petId`/`id`/`ownerId` >= 0 when non-null, pet `name` 1..30 only for null/>30 (empty name is allowed today, keep it). `PetDto`: `visits = visits == null ? List.of() : List.copyOf(visits)`. Do not re-implement the pet-age rule in the constructor.
- [ ] **Step 4:** Re-run the command from Step 2; expect PASS.

### Task 2: Generator config, mappers, controllers, 400 handler

**Files:**
- Modify: `pom.xml:369-371` (add `<schemaMappings>` with `Pet=org.springframework.samples.petclinic.rest.model.PetDto` and `Visit=org.springframework.samples.petclinic.rest.model.VisitDto`; if 7.25.0 rejects `schemaMappings`, use `importMappings` + `typeMappings` for the same two names)
- Modify: `mapper/VisitMapper.java:14-26`, `mapper/PetMapper.java:9,19`
- Modify: `rest/controller/v1/{Visit,Pet,Owner}RestControllerV1.java` (imports to `rest.model`; `getBirthDate()/getName()/getType()/getDate()/getDescription()` calls at `PetRestControllerV1.java:80-82`, `VisitRestControllerV1.java:93-94` where they touch Pet/Visit records become record accessors; `VisitFieldsDto` getters stay)
- Modify: `rest/advice/ExceptionControllerAdvice.java` (new handler near line 117)

**Interfaces:**
- Consumes: Task 1 records.
- Produces: `@ExceptionHandler(HttpMessageNotReadableException.class)` returning 400 `ProblemDetail` via `detailBuild(e, HttpStatus.BAD_REQUEST, url, ERROR_INVALID_REQUEST)`, so record constructor failures during JSON binding are 400 (the existing `Exception` handler would otherwise answer 500).

Test-first: no — build wiring that cannot compile in halves; verified by `mvn -q clean compile` here and by Task 3's tests.

- [ ] **Step 1:** Make the pom change, drop `uses = PetMapper.class` from `VisitMapper` (it needs no PetMapper; `PetMapper` keeps `uses = VisitMapper.class`), swap the `PetDto`/`VisitDto` imports in both mappers and controllers to `rest.model`. Keep the existing `@Mapping` lines (`petId`->`pet.id`, `pet.id`->`petId`, `owner.id`->`ownerId`, `ownerId`->`owner.id`); `OwnerMapper` needs no source change. `addVisit` (`VisitRestControllerV1.java:81`) may keep reassigning its parameter.
- [ ] **Step 2:** Add the exception handler; update Javadoc style to match the neighbouring handlers.
- [ ] **Step 3:** Run `mvn -q clean compile`; expect success. Inspect `target/generated-sources/annotations/.../PetMapperImpl.java` and `VisitMapperImpl.java` to confirm they build records through the canonical constructor with no unmapped-target warnings, and confirm `target/generated-sources/openapi/.../rest/dto/` has no `PetDto.java`/`VisitDto.java` while `OwnerDto.java` imports `rest.model.PetDto`.

### Task 3: Migrate existing tests and cover mappers and 400 paths

**Files:**
- Modify: `src/test/java/.../rest/controller/{VisitRestControllerV1Tests,PetRestControllerV1Tests,OwnerRestControllerV1Tests,V2RestControllersTests}.java` (`new PetDto()`/`new VisitDto()` fluent chains become canonical-constructor calls; mutate-after-create (`newPet.setName(..)`) becomes building a new record; `OwnerDto` stays a mutable POJO)
- Create: `src/test/java/org/springframework/samples/petclinic/mapper/{PetMapperTests,VisitMapperTests}.java` (same Spring test context as `PetRestControllerV1Tests`; mapper beans are `@Autowired`)

**Interfaces:**
- Consumes: Task 1 records, Task 2 handler and mappers.

Test-first: yes — `PetRestControllerV1Tests.testUpdatePetNullNameReturns400` (raw JSON with `"name": null`) fails with 500 without the Task 2 handler; `VisitMapperTests.mapsPetIdToPetIdAndBack` fails until mappers use records.

- [ ] **Step 1:** Rewrite fixtures; `testUpdatePetError` can no longer build a null-name record, so send a JSON string/`Map` with `name` null and expect `status().isBadRequest()`. Add controller cases: name of 31 chars -> 400; visit POST with empty description -> 400; negative `petId` -> 400; future `birthDate` -> 400.
- [ ] **Step 2:** `PetMapperTests`: `toPetDto(Pet)` maps `owner.id`->`ownerId`, type, and sorted visits to `VisitDto` list; `toPet(PetDto)` maps `ownerId`->`owner.id`; `toPets`/`toPetsDto` on empty and populated collections; `toPetDto(null)` is null. `VisitMapperTests`: `toVisitDto`/`toVisit` map `petId`<->`pet.id`; `toVisit(VisitFieldsDto)` ignores id/pet; `toVisitsDto` maps a collection; a `Visit` with null description makes `toVisitDto` throw `IllegalArgumentException`.
- [ ] **Step 3:** Run `mvn clean verify`; expect all tests pass and the JaCoCo `check` goal passes (85% line / 66% branch). If coverage drops, add record/mapper test cases rather than changing the thresholds or exclusions.
- [ ] **Step 4:** Run `git status`; confirm only intended files changed (pom, mappers, controllers, advice, new records and tests) and nothing was committed.

## Negative-constraint pass

- Generator must NOT generate Pet/Visit DTOs: Task 2 `schemaMappings`.
- No custom mustache templates, `openapi.yml` untouched, `OwnerDto` and other DTOs still generated: Task 2 touches only the two schema mappings.
- No Copilot call/prompt file: mappers edited directly (Task 2).
- Do not plan a push or commit: Global Constraints, Task 3 Step 4.
- Records must not change 400 behavior: Task 2 handler plus Task 3 400 tests.
