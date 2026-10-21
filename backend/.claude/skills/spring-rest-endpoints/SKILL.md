---
name: spring-rest-endpoints
description: 'Conventions for designing REST endpoints in Spring MVC: routes, verbs, status codes, @Valid, optional filters with pagination, authorization with @PreAuthorize, use of Authentication and OpenAPI documentation. Use when creating or modifying a @RestController or an endpoint.'
---

# REST endpoint structure

## Controller template

```java
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Items Controller")
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "GET /api/items — list items, filter by ?categoryId={id}&isActive={bool}")
    public ResponseEntity<Page<ItemResponse>> findAllItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(itemService.findAllItems(categoryId, isActive, pageable));
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ASSISTANT')")
    @Operation(summary = "GET /api/items/{itemId} — get an item by id")
    public ResponseEntity<ItemResponse> findItemById(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.findItemById(itemId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "POST /api/items — create an item")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createItem(request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "PUT /api/items/{itemId} — update an item")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long itemId, @Valid @RequestBody ItemUpdateRequest request) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request));
    }

    @PatchMapping("/{itemId}/confirm")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCTOR')")
    @Operation(summary = "PATCH /api/items/{itemId}/confirm — confirm a pending item")
    public ResponseEntity<ItemResponse> confirmItem(@PathVariable Long itemId, Authentication auth) {
        return ResponseEntity.ok(itemService.confirmItem(itemId, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth)));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "DELETE /api/items/{itemId} — deactivate an item (soft delete)")
    public ResponseEntity<Void> deactivateItem(@PathVariable Long itemId) {
        itemService.deactivateItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
```

## Conventions

| Operation                                | Verb        | Route                                                       | Response             |
|------------------------------------------|-------------|-------------------------------------------------------------|----------------------|
| List (filters + pagination)              | GET         | `/api/items?f1=&f2=&page=&size=&sort=`                      | 200 `Page<Response>` |
| List without pagination (small catalogs) | GET         | `/api/items`                                                | 200 `List<Response>` |
| Detail                                   | GET         | `/api/items/{itemId}`                                       | 200                  |
| Create                                   | POST        | `/api/items`                                                | **201** + body       |
| Replace/edit                             | PUT         | `/api/items/{itemId}`                                       | 200 + body           |
| State transition                         | PATCH       | `/api/items/{itemId}/<action>` (`confirm`, `cancel`)        | 200 + body           |
| Delete / deactivate                      | DELETE      | `/api/items/{itemId}`                                       | **204**              |
| Action with no returned resource         | POST/DELETE | `/api/items/children/assign`                                | 204                  |
| Sub-resource                             | GET         | `/api/owners/{ownerId}/items`                               | 200                  |
| Aggregated views                         | GET         | `/api/items/board`, `/api/items/calendar`                   | 200 `Map<...>`       |
| "Me"                                     | GET/PUT     | `/api/auth/me`, `/api/users/update` (no id: uses the token) | 200                  |

- Path variables with an explicit name (`{itemId}`, not `{id}`), matching the Java parameter.
- Method names: `findAll<X>`, `find<X>ById`, `create<X>`, `update<X>`, `deactivate<X>ById`, `<action><X>ById`.
- Delete = soft delete (`active = false`) unless the domain says otherwise.
- Every endpoint has `@Operation(summary = "VERB /route — what it does, filters ?a={a}&b={b}")`. The route and the filters in the summary must match the real mapping and `@RequestParam` names.

## Input validation

- Body: `@Valid @RequestBody XRequest` (record with constraints) → `MethodArgumentNotValidException` → 400 with an `errors[{field, message}]` list.
- Query/path: direct constraints (`@RequestParam @Min(1) @Max(12) int month`) → `HandlerMethodValidationException` → 400. In Spring 6.1+ there is no need for `@Validated` on the class.
- Dates: `@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from`.
- Enums as `@RequestParam(required = false) Status status` (an invalid value → 400 via `MethodArgumentTypeMismatchException`).

## Pagination

- `@PageableDefault(sort = "<field>", direction = Sort.Direction.ASC) Pageable pageable`, always with a stable default order.
- The client sends `?page=0&size=20&sort=name,desc`.
- Optional in `application.yaml` for a bounded page size:
  ```yaml
  spring.data.web.pageable.max-page-size: 100
  ```
  and `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to avoid the warning about serializing `PageImpl` and get a stable page JSON.

## Authorization and current user

- `@PreAuthorize("hasAnyRole('A', 'B')")` on **every** protected endpoint (requires `@EnableMethodSecurity`). No annotation = any authenticated user.
- Public endpoints are declared in `security.public-paths` (yaml), not in the controller.
- User id: `Long.parseLong(auth.getName())` (the principal is the JWT `sub`). Role: `SecurityUtils.extractRole(auth)`.
- Pass `userId` and `role` to the service; the rule "a DOCTOR only operates on their own data" is enforced by the service (`ForbiddenException`). In list endpoints the controller only forces the filter, with **one** private helper instead of duplicating `if/else` per endpoint:
  ```java
  return ResponseEntity.ok(itemService.findAllItems(scopedOwnerId(ownerId, auth), status, pageable));

  /** A DOCTOR only sees its own items, so the ownerId filter is forced to the caller. */
  private static Long scopedOwnerId(Long ownerId, Authentication auth) {
      return ERole.DOCTOR.name().equals(SecurityUtils.extractRole(auth)) ? Long.parseLong(auth.getName()) : ownerId;
  }
  ```
- Always compare roles as `ERole.X.name().equals(role)` (null-safe), never `role.equals(...)`.

## What the controller does NOT do

- No try/catch: exceptions bubble up to the `GlobalExceptionHandler`.
- No access to repositories or `TenantContext`.
- No returning entities or hand-built `Map`s for errors.
