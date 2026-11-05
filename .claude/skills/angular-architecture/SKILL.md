---
name: angular-architecture
description: Architecture of the scheduler frontend (Angular 22, standalone, signals, CDK, Tailwind 4). Use when adding a feature or page, deciding where a file belongs (core, features, shared, layout), wiring HTTP APIs, stores, routes, guards, interceptors, realtime sockets or error handling.
---

# Angular Architecture

A standalone-component Angular application (no NgModules) organized by feature, with signals for state, `rxResource` for server data, Angular CDK for dialogs and overlays, and Tailwind CSS 4 for styling. All UI text is in Spanish.

## Folder map (`src/app`)

| Folder                         | Contents                                                                                                                                          | May import from             |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|
| `core/api`                     | One `*Api` class per backend resource (thin `HttpClient` wrappers returning observables) and realtime sockets (`*.socket.ts`).                    | `core/models`, `core/utils` |
| `core/auth`                    | Session service, guest session service, functional guards, role constants.                                                                        | `core`                      |
| `core/catalogs`                | App-wide cached stores (`*Store`) for catalogs shared across features (specialties, doctors), scoped to the current clinic.                       | `core`                      |
| `core/http`                    | Functional interceptors and API error helpers.                                                                                                    | `core`                      |
| `core/models`                  | Backend contract types in a single models file (requests, responses, filters, pagination, error shape).                                           | nothing                     |
| `core/navigation`              | Routes constants, page titles, sidebar navigation definitions by role.                                                                            | `core`                      |
| `core/utils`                   | Pure functions and constant maps (dates, labels, sorting, messages).                                                                              | `core/models`               |
| `features/<feature>`           | One folder per page or flow: page component, feature service, feature model, optional feature util, and a `components` folder with private components. | `core`, `shared`            |
| `shared/ui`                    | Generic, domain-agnostic UI building blocks (dialog frame, form fields, select, pickers, pagination, skeleton, empty state, icons).               | nothing domain-specific     |
| `shared/components`            | Reusable domain components used by two or more features (appointment detail dialog, patient form dialog, status badge, slot grid, doctor picker). | `core`, `shared/ui`         |
| `shared/services`              | Cross-feature UI services (notifications, confirmation, appointment actions).                                                                     | `core`, `shared`            |
| `shared/pipes`, `shared/utils` | Pure pipes and UI helpers (validation messages, overlay positions, focus management).                                                             | `core`                      |
| `layout/shell`                 | Authenticated shell with sidebar and topbar, hosting child routes.                                                                                | `core`, `shared`            |

Dependency direction is one way: features depend on shared and core; shared depends on core; core depends on nothing above it. A feature never imports from another feature; when two features need the same piece, promote it to `shared` (UI) or `core` (data).

## Data flow

1. `core/api` classes call relative URLs; the URL interceptor prefixes the API base URL from the environment, and the auth interceptor adds the bearer token (never to public paths), handles guest tokens, and expires the session when the token is expired or a request that carried it gets a 401. `AuthService` also schedules a logout at `expiresAt` and syncs sessions across tabs; the shell redirects to `/login` when the session becomes null. No `Accept-Language` is sent (the backend is fixed to Spanish).
2. A feature service (`<feature>.service.ts`) is provided at the page component level (`providers` of the page), so its state lives and dies with the page. It owns filter signals, an `rxResource` keyed on those signals, derived `computed` signals for the template, and command methods that call the API and then reload or invalidate.
3. App-wide catalogs use stores in `core/catalogs` provided in root, built on the clinic-scoped resource helper that loads lazily, caches per clinic and supports reload and invalidate. Feature services expose store signals instead of refetching catalogs.
4. Components read signals from the feature service and call its methods; they do not call APIs directly.
5. Errors are turned into user messages with the API error helpers (`apiErrorMessage`, `resourceError`), which understand the backend `ErrorResponse` contract, rate limiting and connection errors. User feedback goes through `NotificationService`; confirmations through `ConfirmService`.

## State rules

- Signals for all local and service state; `computed` for derived values; `linkedSignal` when a value resets with its source; `rxResource` for loading server data. Expose read-only signals from services.
- RxJS is used for HTTP and streams (sockets, form value changes); convert at the boundary with `toSignal` and `toObservable`, and always tear down with `takeUntilDestroyed`.
- No global state library. Do not add one without an explicit decision.
- After a mutation, either update the resource locally when cheap and safe, or reload or invalidate it. Invalidate related catalog stores when a mutation affects them.

## Routing and security

- All pages are lazy loaded with `loadComponent` in `app.routes.ts`; titles come from `PAGE_TITLES`.
- Authenticated pages are children of the shell route protected by the auth guard; role-restricted pages add the role guard with constants from `ROLES`. Public pages use the guest guard when a logged user must be redirected away.
- Sidebar entries are declared in `core/navigation` with their allowed roles; adding a page means adding both the route and the navigation entry.
- Role checks in templates use the session signals from the auth service; the backend remains the real authority.

## Realtime

- Sockets live in `core/api` as injectable classes provided at the feature level, using `@stomp/rx-stomp` with the bearer token in the connect headers, 10 second heartbeats, exponential reconnection, activation outside the Angular zone and deactivation on destroy.
- Feature services merge socket events and reconnections, debounce them and reload their resource. Payloads are treated as signals to refetch, not as the source of truth.

## Conventions

- File names in kebab case with a role suffix: `.component`, `.service`, `.api`, `.store`, `.socket`, `.model`, `.util`, `.pipe`, `.directive`, `.guards`, `.interceptors`.
- Page components end in `-page.component`; their feature service shares the feature name.
- Feature-specific types live in `<feature>.model.ts`; backend contract types live in `core/models`.
- Pure functions (formatters, text builders for dialogs or messages, form validators) used by a single feature live in `<feature>.util.ts` next to the model; when two or more features need them, they move to `core/utils` (data and dates) or `shared/utils` (UI helpers). Extract a function only when it is duplicated, when it bloats the service or component, or when it deserves its own unit tests; one- or two-line helpers used by a single file stay private in that file. Anything that touches the DOM, injects services or holds state is not a util.
- Formatting with Prettier (with organized imports): run `npm run format` in `frontend`. Validate with `npm run build`.
- Environment values only through the `environments` files; secrets never reach the frontend.
