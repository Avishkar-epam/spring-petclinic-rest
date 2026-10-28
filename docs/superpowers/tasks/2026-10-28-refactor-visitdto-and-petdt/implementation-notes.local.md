## Files
pom.xml — java.version 21; schemaMappings Pet/Visit to rest.model records
src/main/java/.../rest/model/PetDto.java — Java 21 record, compact-constructor validation, copies visits
src/main/java/.../rest/model/VisitDto.java — Java 21 record, description 1..255, petId required
src/main/java/.../mapper/VisitMapper.java — no longer uses PetMapper
src/main/java/.../rest/advice/ExceptionControllerAdvice.java — HttpMessageNotReadableException handler returns 400
## Renames
rest.dto.PetDto/VisitDto → rest.model.PetDto/VisitDto (getX() → x()) — call sites updated: PetMapper, VisitMapper, Owner/Pet/VisitRestControllerV1, controller tests
## Reused
PetAgeValidation — rest/validation — stays on birthDate record component
## Tests
Records — ./mvnw.cmd -q clean test -Dtest=VisitDtoTests,PetDtoTests
Full — ./mvnw.cmd clean verify
## Standards applied
