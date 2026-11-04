---
name: angular-component-modularization
description: How components are split, composed and shared in the scheduler frontend - page vs feature components vs shared UI, inputs and outputs with signals, dialogs, forms, accessibility and Tailwind styling. Use when creating or refactoring a component, deciding whether to extract a sub-component, or promoting a component to shared.
---

# Angular Component Modularization

Components are small, standalone, `OnPush` and signal-based. Each one has a single reason to change and a clear place in the folder hierarchy described in the `angular-architecture` skill.

## Component tiers

| Tier                    | Location                                      | Knows about                                                                                          | Typical content                                                                                                  |
|-------------------------|-----------------------------------------------|------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Page (container)        | `features/<feature>/<feature>-page.component` | Its feature service, router, dialogs                                                                 | Provides the feature service, composes the page from sub-components, opens dialogs, handles focus after actions. |
| Feature component       | `features/<feature>/components/<name>/`       | Feature model types; may inject the feature service when it is a dialog or form owned by the feature | Cards, columns, panels, feature dialogs and forms.                                                               |
| Shared domain component | `shared/components/<name>/`                   | Core models and shared services                                                                      | Domain pieces reused by two or more features. May have its own small service next to it.                         |
| UI primitive            | `shared/ui/<name>/`                           | Nothing domain-specific                                                                              | Visual and interaction building blocks driven only by inputs, outputs and content projection.                    |

## When to extract a component

- A template block has its own inputs, state or interactions, or is repeated.
- The page template is hard to read in one screen.
- The same UI appears in another feature: first move it to `shared/components` (domain) or `shared/ui` (generic), never import it across features.
- Do not extract purely to reduce lines when the piece has no independent meaning.

## Component rules

- Standalone, `changeDetection: OnPush`, explicit `imports` list, separate template file (inline templates only for tiny UI primitives).
- Inputs with `input()` or `input.required()`, outputs with `output()`, two-way bindings with `model()`. No decorators for inputs and outputs.
- Dependencies with `inject()`, not constructor parameters.
- Template-facing members are `protected`; injected services that the template does not use are `private`; everything is `readonly` where possible.
- Derived values are `computed` signals, not methods called from the template.
- Presentational components never inject API classes or feature services; they emit outputs and let the parent decide.
- Host styling and attributes go in the `host` metadata object, not in host decorators.
- Use the built-in control flow blocks in templates, with `track` on every loop.
- Constants and label maps come from `core/utils` and are exposed as protected readonly fields.

## Dialogs

- Opened with the CDK `Dialog` from the page or a shared service, passing typed data and the page injector so the dialog can use the page-scoped feature service.
- The dialog content wraps itself in the shared dialog frame component, which provides title, accessible labelling and actions area.
- The dialog owns its form, a saving signal and an error signal; it closes itself on success and shows the API error message on failure.
- Confirmation flows use `ConfirmService` instead of custom confirmation dialogs.

## Forms

- Reactive forms with the non-nullable form builder, strongly typed.
- Reuse validators and error message maps from the shared validation utilities; keep validation messages in Spanish and consistent with backend rules.
- Wrap controls in the shared form field component and use the shared select and pickers instead of native controls when a primitive exists.
- Trim string values before sending; send null for optional empty values.
- Guard against double submit with the saving signal; mark all as touched on invalid submit.

## States and feedback

Every data-driven view handles four states with shared primitives: loading (skeleton), error (load error with retry), empty (empty state), and data. Success and failure notifications use `NotificationService`.

## Accessibility and styling

- Interactive non-button elements use the keyboard activate directive; dialogs are modal with labelled titles; focus is restored or moved after list mutations with the focus helper.
- Icons through the shared icon component.
- Tailwind utility classes in templates; component style files only for things utilities cannot express. Reuse the existing glass and backdrop visual language.

## Testing hooks

Keep components easy to test: inputs and outputs fully describe presentational components, and containers depend on one feature service that can be replaced by a stub in tests.
