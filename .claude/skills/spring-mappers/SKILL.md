---
name: spring-mappers
description: MapStruct mapping conventions in the scheduler backend. Use when converting entities to DTO records, adding a new response field, flattening relations, or deciding whether mapping logic belongs in a mapper or a service.
---

# Spring Mappers (MapStruct)

Entity to DTO conversion is done exclusively with MapStruct interfaces in the `mapper` package. Lombok and MapStruct are wired together with the lombok-mapstruct binding, so builders and getters are visible to the generated code.

## Rules

- One mapper interface per entity, named `<Entity>Mapper`, annotated with `@Mapper(componentModel = "spring")` and injected by constructor like any other bean.
- Mappers are pure: no repository calls, no services, no security context, no clock. If a value needs a lookup or a business decision, compute it in the service and pass it as an extra mapping parameter or set it on the response afterwards.
- Methods are named `toResponse`, `toResponseList`, `toSummaryItem` and similar, describing the target, not the source.
- Flatten relations explicitly with `@Mapping` from nested source paths (for example the doctor's name or the specialty name). Every target property that does not have a same-named source must be mapped explicitly or ignored explicitly; do not rely on silent nulls.
- Request to entity: for simple entities the service may use the entity builder directly; when the request has several fields, add a mapper method with `@MappingTarget` for updates and ignore id, version, audit fields and relations that must be resolved by the service.
- Never map collections that are lazily loaded unless the repository method used by that endpoint fetches them; otherwise the mapper will trigger extra queries or fail outside the transaction. Keep fetch strategy and mapping aligned.
- Enums map by name automatically; keep enum names identical between entity and DTO.
- Prefer `List` mapping methods in the mapper instead of streaming and mapping in services.
- Records as targets: MapStruct uses the canonical constructor, so the record component names are the mapping target names.
- Set the unmapped target policy to report errors if the project adopts it globally; until then, reviewers must check new mappings for forgotten fields.

## Adding or changing a mapping (checklist)

1. Add the field to the response record.
2. Add an explicit `@Mapping` if the source path differs.
3. Verify the repository method used by the endpoint loads the needed relation.
4. Build to let the annotation processor validate the mapping, and inspect warnings about unmapped properties.
5. Test: mappers are generated code; test them indirectly through service tests, or use the generated implementation directly in a plain unit test when the mapping contains custom expressions.
