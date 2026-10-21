---
name: spring-jpa-repository
description: 'Spring Data JPA repository design: derived queries, JPQL @Query with optional filters, JOIN FETCH / @EntityGraph against N+1, pagination, exists/count, pessimistic and optimistic locking. Use when creating or modifying a repository, writing a query, filtering/paginating lists, or fixing LazyInitializationException or N+1 problems.'
---

# Repositories and JPA methods

Assumed context: `open-in-view: false` (mandatory). Every relation is `LAZY`, so **every query that feeds a DTO must explicitly fetch the relations the mapper reads**, otherwise you get `LazyInitializationException`.

## Base shape

```java
public interface ItemRepository extends JpaRepository<Item, Long> {
    // ...
}
```
Interface only, no `@Repository`. No custom implementations unless JPQL is not enough.

## 1. Derived queries (prefer while the name stays readable)

```java
Optional<User> findByUsername(String username);
boolean existsByEmail(String email);                       // uniqueness checks
boolean existsByNameIgnoreCase(String name);
boolean existsByIdAndActiveTrueAndRole(Long id, ERole role);
Optional<User> findByIdAndRoleAndActiveIsTrue(Long id, ERole role);
List<Slot> findByOwnerIdAndDayOfWeekAndActiveTrue(Long ownerId, DayOfWeek day);
List<Event> findByOwnerIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(Long ownerId, LocalDate from);
boolean existsByOwnerIdAndStatusNotAndStartTimeAfter(Long ownerId, Status status, LocalDateTime t);
```
- `existsBy...` instead of `findBy...().isPresent()` (runs `SELECT 1 ... LIMIT 1`).
- Navigate relations by id with `OwnerId` (Spring resolves `owner.id`).
- If the name grows past ~5 conditions, switch to `@Query`.

## 2. @EntityGraph to fetch relations in derived queries

```java
@EntityGraph(attributePaths = {"owner.category"})
List<Slot> findByOwnerIdAndActiveTrue(Long ownerId);

@EntityGraph(attributePaths = {"members"})           // "descriptive" name + By: Spring ignores the prefix
Optional<Team> findTeamMembersById(Long id);

// Override findById so it always fetches what the mapper uses
@NullMarked                                         // jspecify: avoids the nullness warning when overriding
@EntityGraph(attributePaths = {"owner.category", "customer"})
Optional<Booking> findById(Long id);
```
Trick: `find<Something>By...` with a different prefix lets you have several "find by id" variants with different graphs (lightweight `findById` vs `findTeamMembersById` with the collection).

## 3. JPQL @Query with optional filters + pagination

The `(:param IS NULL OR field = :param)` pattern for list endpoints with optional filters:

```java
@Query("SELECT b FROM Booking b " +
        "JOIN FETCH b.owner o " +
        "LEFT JOIN FETCH o.category c " +
        "JOIN FETCH b.customer cu " +
        "WHERE (:ownerId IS NULL OR b.owner.id = :ownerId) AND " +
        "(:customerId IS NULL OR b.customer.id = :customerId) AND " +
        "(:status IS NULL OR b.status = :status)")
Page<Booking> findAllByFilters(@Param("ownerId") Long ownerId,
                               @Param("customerId") Long customerId,
                               @Param("status") BookingStatus status,
                               Pageable pageable);
```
- `JOIN FETCH` for mandatory `@ManyToOne`, `LEFT JOIN FETCH` for optional ones.
- `JOIN FETCH` + `Page` works fine for **to-one**. For **to-many** with pagination do NOT fetch (Hibernate paginates in memory); use two queries or `@EntityGraph` on an unpaginated list.
- Enum literal in JPQL: `WHERE u.role = ERole.MEMBER` (Hibernate 6+ accepts the simple name if unique; otherwise use the FQN).
- Always explicit `@Param`.
- The `Pageable` sort comes from the client: invalid properties throw `PropertyReferenceException` / `InvalidDataAccessApiUsageException(SemanticException)` → the `GlobalExceptionHandler` maps them to 400.

### Ranges and overlaps

```java
@Query("SELECT COUNT(b) > 0 FROM Booking b " +
        "WHERE b.owner.id = :ownerId " +
        "AND b.status NOT IN :excludedStatuses " +
        "AND b.startTime < :endTime " +
        "AND b.endTime > :startTime")
boolean existsOverlapping(@Param("ownerId") Long ownerId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("excludedStatuses") List<BookingStatus> excludedStatuses);

@Query("SELECT b FROM Booking b JOIN FETCH b.owner JOIN FETCH b.customer " +
        "WHERE (:ownerId IS NULL OR b.owner.id = :ownerId) " +
        "AND b.startTime >= :from AND b.startTime < :to ORDER BY b.startTime ASC")
List<Booking> findByFiltersAndDateRange(...);
```
Half-open ranges `[from, to)`. Overlap: `a.start < b.end AND a.end > b.start`.

## 4. Concurrency

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdForUpdate(@Param("id") Long id);   // SELECT ... FOR UPDATE, inside @Transactional
```
- Pessimistic for "read, validate, modify" on a contended row (e.g. changing a role while another process books).
- Optimistic with `@Version` on the entity for everything else; the handler returns 409 on `OptimisticLockingFailureException`.
- For rules like "no two bookings in the same slot", also add a **partial unique index** in the migration (`WHERE status <> 'CANCELLED'`); the `exists` check in the service gives the nice message, the index guarantees integrity.

## 5. Usage from the service

```java
var page = repository.findAllByFilters(ownerId, customerId, status, pageable).map(mapper::toResponse);
var item = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
```
- `Page.map(mapper::toResponse)` inside the readOnly transaction.
- Validate that filter ids exist (`if (categoryId != null) getCategoryOrThrowById(categoryId)`) if you want a 404 instead of an empty list.
- For `@ManyToMany` collections, modify the owning side and `save` the owner.

## Anti-patterns

- `findAll()` + filtering in Java.
- `FetchType.EAGER` to "fix" lazy loading.
- String concatenation in `@Query` (injection); always parameters.
- `nativeQuery = true` without need (breaks portability and, with schema multi-tenancy, must not prefix tables with a schema so `search_path` is respected).
