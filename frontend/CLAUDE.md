# Frontend conventions (Angular 19)

Standalone components, signals, native control flow, `inject()`, OnPush. Follow the
`angular-developer` skill in `.claude/skills`. Agents live in `.claude/agents`.

## Structure
- `core/`: API clients, auth, http helpers, models, pure utils. Never imports from `shared/`,
  `features/` or `layout/`.
- `shared/`: presentational UI, pipes, cross-feature dialogs. `features/`: one folder per page.
- Folder layout (features, `layout/shell`, `shared/appointments`): the main component sits in the
  folder root; `services/` holds its services; `components/<name>/` holds each child component or
  dialog, and a child with its own service gets `components/<name>/services/`. Models go in `models/`.
- Every component has separate `.ts` and `.html` files (`templateUrl`, never inline `template`).

## View services
- One `@Injectable()` service per view, provided in the page component (`providers: [...]`).
  It holds all reads (`rxResource`), writes and toasts; components only bind and delegate.
- Load errors: `resourceError(resource, fallback)` from `core/http/api-error.ts`.
- Dialogs are opened with `injector: this.injector` so they reach the view service, and spread
  `...labelledByFrame()` (from `app-dialog-frame`) so the CDK dialog is labelled by its title.

## UI
- Form controls live in `shared/ui/forms`: `app-form-field`, `app-select`, `app-time-picker`,
  `app-date-picker`, `app-date-range-picker`.
- Loading placeholders: `app-skeleton`; inline error banners: `app-alert`.
- Pipes: `timeRange`, `specialtyLabel`. Clickable cards use `KeyboardActivateDirective`.

## Auth and backend
- Login sends the full username (`user@clinicsubdomain.com`) and the password.
- The clinic name and subdomain come from the JWT (`AuthService.clinic`).
- Roles: `ADMINISTRATOR`, `DOCTOR`, `ASSISTANT` (there is no PATIENT role).
- Backend error messages are shown as-is (`apiErrorMessage`); no frontend translation.

## Checks
- `npx ng build` must pass with zero errors and warnings.
- Tests (Karma/Jasmine): `npx ng test --watch=false --browsers=ChromeHeadless`.
