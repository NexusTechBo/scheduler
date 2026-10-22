---
name: angular-project-structure
description: Folder structure and layering for an Angular (standalone, signals) SPA that consumes a REST API - core/features/shared/layout layout, API clients, feature state services with signals and rxResource, page vs dialog vs presentational components, HTTP interceptors, auth guards, error handling, models, naming conventions and routing. Use when creating a new feature/page end to end, adding an API client, component, dialog or service, scaffolding a new Angular project, or deciding where a file belongs.
---

# Angular Project Structure (standalone + signals)

A feature-first structure for Angular SPAs backed by a REST API (pairs with `java-mvc-project-structure` on the backend).
Built on standalone components, signals, `rxResource`, functional interceptors/guards, Angular CDK and Tailwind CSS.
Examples use a fictional **library** domain (`Book`, `Author`, `Member`, `Loan`).

## Folder layout

```
src/
├── main.ts
├── index.html
├── styles.css                 # Tailwind + global utility classes (btn, field-input, chip, data-table…)
├── environments/
│   ├── environment.ts         # { apiUrl: '/api/' }
│   └── environment.development.ts
└── app/
    ├── app.component.ts       # <router-outlet> + global overlays (notifications)
    ├── app.config.ts          # providers: router, HttpClient + interceptors, locale, CDK defaults
    ├── app.routes.ts          # all routes, lazy loadComponent
    ├── core/                  # singletons, no UI — used everywhere
    │   ├── api/               # one class per backend resource: books.api.ts, loans.api.ts, board.socket.ts
    │   ├── auth/              # auth.service.ts, auth.guards.ts, permissions.ts
    │   ├── http/              # api.interceptors.ts, api-error.ts, http-params.util.ts
    │   ├── models/            # api.models.ts — TS types mirroring backend DTOs
    │   ├── navigation/        # navigation.ts — HOME_URL, ROLES, nav items per role
    │   └── utils/             # pure functions: dates, labels, formatting
    ├── features/              # one folder per screen / use case
    │   └── books/
    │       ├── books-page.component.ts|html      # routed page (container)
    │       ├── services/books.service.ts          # feature state + actions
    │       ├── components/                        # pieces used only by this feature
    │       │   └── book-form-dialog/
    │       │       ├── book-form-dialog.component.ts|html
    │       │       └── services/…                 # only if the dialog has its own state
    │       └── models/books.model.ts             # view models used only here (optional)
    ├── layout/
    │   └── shell/             # authenticated frame: sidebar, topbar, <router-outlet>
    │       ├── shell.component.ts|html
    │       └── components/sidebar-nav/, topbar/
    └── shared/                # reusable, feature-agnostic
        ├── ui/                # presentational components: page-header, pagination, empty-state, skeleton,
        │   │                  # load-error, status-badge, dialog-frame, confirm-dialog, ui-icon…
        │   └── forms/         # form controls: form-field, select, date-picker, time-picker…
        ├── services/          # notification.service.ts
        ├── pipes/             # pure pipes
        ├── utils/             # validation-messages.util.ts (validators + error messages)
        └── <domain>/          # cross-feature domain pieces, e.g. shared/loans/ (detail dialog, actions service)
```

### Where does it go?

| It is…                                            | Put it in                                                            |
|---------------------------------------------------|----------------------------------------------------------------------|
| A call to a backend endpoint                      | `core/api/<resource>.api.ts`                                         |
| A type that mirrors a backend DTO                 | `core/models/api.models.ts`                                          |
| App-wide state (session, current user)            | `core/auth/` or another `core/<area>/` service, `providedIn: 'root'` |
| A routed screen                                   | `features/<feature>/<feature>-page.component.ts`                     |
| State + actions of one screen                     | `features/<feature>/services/<feature>.service.ts`                   |
| A dialog/subcomponent used by one feature         | `features/<feature>/components/<name>/`                              |
| A component used by 2+ features with domain logic | `shared/<domain>/components/`                                        |
| A dumb, domain-agnostic UI piece                  | `shared/ui/<name>/`                                                  |
| A pure helper                                     | `core/utils/` (domain) or `shared/utils/` (UI/forms)                 |

Promote code to `shared/` only when a second feature actually needs it.

## Data flow

```
Template ─(events)→ Page component ─→ Feature service ─→ core/api/*Api ─→ HttpClient
   ▲                                        │                               │ interceptors:
   └──────── signals (computed) ◄───────────┘ rxResource / signals          │ base URL, Bearer token, 401 → logout
```

| Layer                        | Does                                                                                                                                      | Never does                                        |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `core/api`                   | Typed HTTP calls, returns `Observable<T>`                                                                                                 | State, notifications, error messages, `subscribe` |
| Feature service              | Holds state in signals, loads with `rxResource`, filters/pagination, orchestrates API calls, confirm dialogs, success/error notifications | Touch the DOM                                     |
| Page component               | Wires template to the service, opens dialogs, focus management                                                                            | HTTP calls, business rules                        |
| Presentational (`shared/ui`) | `input()` / `output()` only, renders                                                                                                      | Inject feature services or APIs                   |
| Interceptors / guards        | Cross-cutting HTTP and navigation rules                                                                                                   | Feature logic                                     |

## Naming conventions

| Thing            | File                                             | Class / symbol                                       |
|------------------|--------------------------------------------------|------------------------------------------------------|
| API client       | `books.api.ts`                                   | `BooksApi`                                           |
| WebSocket client | `board.socket.ts`                                | `BoardSocket`                                        |
| Feature service  | `services/books.service.ts`                      | `BooksService`                                       |
| Routed page      | `books-page.component.ts`                        | `BooksPageComponent`, selector `app-books-page`      |
| Dialog           | `book-form-dialog.component.ts`                  | `BookFormDialogComponent` + `BookFormData` interface |
| UI component     | `shared/ui/empty-state/empty-state.component.ts` | `EmptyStateComponent`                                |
| Guard            | `auth.guards.ts`                                 | `authGuard`, `guestGuard`, `roleGuard(...)`          |
| Interceptor      | `api.interceptors.ts`                            | `apiUrlInterceptor`, `authInterceptor`               |
| Utils            | `labels.util.ts`                                 | plain exported functions/consts                      |
| Constants        | —                                                | `UPPER_SNAKE_CASE` (`BOOKS_PAGE_SIZE`)               |

Files in kebab-case; one component/service per file; templates in separate `.html` files.

## Models — `core/models/api.models.ts`

Mirror the backend DTOs exactly; use string-literal unions for enums.

```ts
export type Role = 'ADMIN' | 'LIBRARIAN' | 'READER';
export type LoanStatus = 'ACTIVE' | 'RETURNED' | 'OVERDUE';

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: { field: string; message: string }[] | null;
}

export interface Book {
  id: number;
  title: string;
  isbn: string;
  authorName: string;
  active: boolean;
}

export interface BookRequest {
  title: string;
  isbn: string;
  authorId: number;
}

export interface BookFilters extends PageQuery {
  search?: string;
  isActive?: boolean;
}
```

## API client — `core/api`

```ts
@Injectable({ providedIn: 'root' })
export class BooksApi {
  private readonly http = inject(HttpClient);

  list(query: BookFilters = {}): Observable<Page<Book>> {
    return this.http.get<Page<Book>>('books', { params: toHttpParams(query) });
  }

  create(request: BookRequest): Observable<Book> {
    return this.http.post<Book>('books', request);
  }

  update(id: number, request: BookRequest): Observable<Book> {
    return this.http.put<Book>(`books/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`books/${id}`);
  }

  activate(id: number): Observable<Book> {
    return this.http.patch<Book>(`books/${id}/activate`, null);
  }
}
```

- Relative URLs (`'books'`); `apiUrlInterceptor` prefixes `environment.apiUrl`.
- Method names are verbs on the resource (`list`, `get`, `create`, `update`, `deactivate`), not HTTP verbs.
- `toHttpParams` drops `null`/`undefined`/`''` so optional filters are simply omitted:

```ts
export function toHttpParams(query: object = {}): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') continue;
    params = params.set(key, String(value));
  }
  return params;
}
```

## Feature service — state with signals

Provided **at the page component** (`providers: [BooksService]`), so state lives and dies with the screen and dialogs opened with the page injector share it.

```ts
const BOOKS_PAGE_SIZE = 12;

@Injectable()
export class BooksService {
  private readonly booksApi = inject(BooksApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  private readonly _search = signal('');
  private readonly _showInactive = signal(false);
  private readonly page = signal(0);
  readonly search = this._search.asReadonly();
  readonly showInactive = this._showInactive.asReadonly();

  private readonly debouncedSearch = toSignal(
    toObservable(this._search).pipe(
      debounceTime(500),
      map((search) => search.trim()),
      distinctUntilChanged(),
      tap(() => this.page.set(0)),
    ),
    { initialValue: '' },
  );

  private readonly resource = rxResource({
    request: () => ({
      search: this.debouncedSearch(),
      isActive: this.showInactive() ? undefined : true,
      page: this.page(),
    }),
    loader: ({ request }) => this.booksApi.list({ ...request, size: BOOKS_PAGE_SIZE, sort: 'id' }),
  });

  readonly books = computed(() => this.resource.value()?.content ?? []);
  readonly pagination = computed(() => this.resource.value()?.page);
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, 'Could not load books');

  reload(): void {
    this.resource.reload();
  }

  setSearch(search: string): void {
    this._search.set(search);
  }

  setShowInactive(value: boolean): void {
    this._showInactive.set(value);
    this.page.set(0);
  }

  setPage(page: number): void {
    this.page.set(page);
  }

  save(book: Book | undefined, request: BookRequest): Observable<Book> {
    const save$ = book ? this.booksApi.update(book.id, request) : this.booksApi.create(request);
    return save$.pipe(
      tap(() => this.notifications.success(book ? 'Book updated' : 'Book created')),
      finalize(() => this.reload()),
    );
  }

  deactivate(book: Book): Observable<unknown> {
    return this.confirm
      .confirmThen(
        { title: 'Deactivate book', message: `${book.title} will no longer be lendable.`, confirmLabel: 'Deactivate', danger: true },
        () => this.booksApi.deactivate(book.id),
        'Book deactivated',
        'Could not deactivate the book',
      )
      .pipe(tap(() => this.reload()));
  }
}
```

Rules:
- Private writable signals (`_search`) + public `asReadonly()`; mutate only through methods (`setSearch`).
- Derived state with `computed`, never duplicated in another signal.
- Server data via `rxResource` (`request` → reloads automatically when its signals change; return `undefined` to skip loading).
  Angular 19 uses `request`/`loader`; Angular 20+ renames them to `params`/`stream`.
- Expose `isLoading`, `errorMessage`, data and pagination for the template's loading/error/empty/content states.
- Actions return `Observable` so the caller can react (close a dialog, move focus); confirmation and notifications live here.
- Errors shown to the user always go through `apiErrorMessage(error, fallback)`.

## Page component (container)

```ts
@Component({
  selector: 'app-books-page',
  imports: [FormsModule, PageHeaderComponent, PaginationComponent, EmptyStateComponent, SkeletonComponent, LoadErrorComponent],
  providers: [BooksService],
  templateUrl: './books-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BooksPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  protected readonly books = inject(BooksService);

  protected openForm(book?: Book): void {
    this.dialog.open<void, BookFormData>(BookFormDialogComponent, {
      ...labelledByFrame(),
      data: { book },
      injector: this.injector, // dialog sees the page-scoped BooksService
    });
  }
}
```

Template — always cover the four states:

```html
<app-page-header title="Books" subtitle="Library catalog">
  <button type="button" class="btn btn-primary" (click)="openForm()">New book</button>
</app-page-header>

@if (books.isLoading() && !books.books().length) {
  <app-skeleton [rows]="4" />
} @else if (books.errorMessage()) {
  <app-load-error [message]="books.errorMessage()!" (retry)="books.reload()" />
} @else if (!books.books().length) {
  <app-empty-state icon="book" title="No books" message="Add the first book to the catalog." />
} @else {
  <table class="data-table">
    @for (book of books.books(); track book.id) { … }
  </table>
}

@if (books.pagination(); as info) {
  <app-pagination [page]="info.number" [totalPages]="info.totalPages" [totalElements]="info.totalElements"
                  (pageChange)="books.setPage($event)" />
}
```

- `ChangeDetectionStrategy.OnPush` on every component.
- `inject()` instead of constructor parameters; `protected` for members used only by the template.
- Built-in control flow (`@if`, `@for` with `track`, `@switch`), no `*ngIf`/`*ngFor`.
- Layout classes on the host via `host: { class: … }`, not a wrapper `<div>`.

## Dialog component (form)

```ts
export interface BookFormData {
  book?: Book;
}

@Component({
  selector: 'app-book-form-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, FormFieldComponent, AlertComponent],
  templateUrl: './book-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookFormDialogComponent {
  private readonly books = inject(BooksService);
  protected readonly ref = inject(DialogRef);
  protected readonly book = inject<BookFormData>(DIALOG_DATA).book;

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: [this.book?.title ?? '', notBlank],
    isbn: [this.book?.isbn ?? '', [notBlank, isbnValidator]],
  });

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { title, isbn } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.books
      .save(this.book, { title: title.trim(), isbn: isbn.trim(), authorId: 1 })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.ref.close(),
        error: (error) => this.error.set(apiErrorMessage(error, 'Could not save the book')),
      });
  }
}
```

- Typed, non-nullable reactive forms; same dialog for create and edit (`book` undefined = create).
- Guard double submit with a `saving` signal; show server errors inside the dialog, not as toasts.
- Validators and their messages live in `shared/utils/validation-messages.util.ts`; `app-form-field` renders the message and wires `aria-invalid` / `aria-describedby`.
- If a dialog has substantial state of its own, give it `components/<dialog>/services/<dialog>.service.ts` provided in the dialog.

## Presentational component (`shared/ui`)

```ts
@Component({
  selector: 'app-empty-state',
  imports: [UiIconComponent],
  templateUrl: './empty-state.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  title = input.required<string>();
  message = input<string>();
  icon = input<UiIconName>('calendar');
}
```

Signal `input()` / `output()` / `model()` only; no injected domain services.

## HTTP: interceptors and errors

```ts
// core/http/api.interceptors.ts
export const apiUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (/^(https?:)?\/\//.test(req.url) || req.url.startsWith('/')) return next(req);
  return next(req.clone({ url: environment.apiUrl + req.url }));
};

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const request = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === HttpStatusCode.Unauthorized && token) auth.logout();
      return throwError(() => error);
    }),
  );
};
```

```ts
// core/http/api-error.ts — turns the backend ErrorResponse into a user message
export function apiErrorMessage(error: unknown, fallback = 'Unexpected error'): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 0) return 'Could not reach the server';
  const body = error.error as Partial<ApiError> | null;
  const details = [...new Set((body?.errors ?? []).map((e) => e.message).filter(Boolean))];
  return details.length ? details.join('. ') : (body?.message ?? fallback);
}

export function resourceError(resource: { error(): unknown }, fallback: string): Signal<string | null> {
  return computed(() => (resource.error() ? apiErrorMessage(resource.error(), fallback) : null));
}
```

## Auth, guards, navigation

- `AuthService` (`providedIn: 'root'`): session in a signal decoded from the JWT (`jwt-decode`), persisted in `localStorage` (wrapped in try/catch), `token()` drops expired sessions, `hasRole(...roles)`, `login()`, `logout()`.
- Functional guards:

```ts
export const authGuard: CanActivateFn = () =>
  inject(AuthService).token() ? true : inject(Router).createUrlTree(['/login']);

export const guestGuard: CanActivateFn = () =>
  inject(AuthService).token() ? inject(Router).createUrlTree([HOME_URL]) : true;

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => (inject(AuthService).hasRole(...roles) ? true : inject(Router).createUrlTree([HOME_URL]));
}
```

- `core/navigation/navigation.ts` holds `HOME_URL`, role groups (`ROLES.admin`, `ROLES.staff`) and the nav items with the roles allowed to see them — the sidebar and the routes read from the same source.
- UI permission rules (e.g. `canReturnLoan(status)`) are pure functions in `core/auth/permissions.ts`. They hide buttons; the backend still enforces.

## Routing — `app.routes.ts`

```ts
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: HOME_URL },
      {
        path: 'books',
        title: 'Books',
        loadComponent: () => import('./features/books/books-page.component').then((m) => m.BooksPageComponent),
      },
      {
        path: 'members',
        title: 'Members',
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () => import('./features/members/members-page.component').then((m) => m.MembersPageComponent),
      },
    ],
  },
  {
    path: 'login',
    title: 'Sign in',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  { path: '**', redirectTo: '' },
];
```

Every feature is lazy (`loadComponent`); authenticated pages are children of the shell; every route has a `title`.

## App config

```ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiUrlInterceptor, authInterceptor])),
    { provide: LOCALE_ID, useValue: 'en' },
    { provide: DEFAULT_DIALOG_CONFIG, useValue: { ...new DialogConfig(), ariaModal: true } },
  ],
};
```

Dev server: `proxy.conf.js` forwards `/api` and `/ws` (with `ws: true`) to the backend, so the app always uses relative URLs and no CORS is needed locally.

## Real-time (optional)

A WebSocket client lives in `core/api/<name>.socket.ts` (e.g. `RxStomp`), is `@Injectable()` without `providedIn` and is provided by the page that needs it. It sends the JWT in `connectHeaders`, runs outside the Angular zone, deactivates on `DestroyRef`. The feature service listens to its events and simply calls `resource.reload()` (debounced) — events only signal "something changed", data still comes from REST (see `java-websocket-stomp`).

## Conventions

- Strict TypeScript; no `any`. Prettier: `printWidth: 100`, `singleQuote: true`.
- `readonly` on injected dependencies and signals.
- Accessibility basics: `type="button"`, `aria-label` on icon buttons, labelled dialogs (`labelledByFrame()`), focus restored after actions.
- Styling with Tailwind utilities; repeated patterns become global classes in `styles.css` (`btn`, `btn-primary`, `field-input`, `data-table`, `chip`).
- Unit tests next to the file (`*.spec.ts`), prioritizing pure utils and services.

## New feature checklist

1. Types in `core/models/api.models.ts` matching the backend DTOs
2. `core/api/<resource>.api.ts`
3. `features/<feature>/services/<feature>.service.ts` (signals + `rxResource` + actions)
4. `features/<feature>/<feature>-page.component.ts|html` with loading / error / empty / content states
5. Dialogs in `features/<feature>/components/`
6. Route in `app.routes.ts` (lazy, `title`, guards) and nav item in `core/navigation/navigation.ts`
7. Reuse `shared/ui` pieces; promote new ones only when a second feature needs them
