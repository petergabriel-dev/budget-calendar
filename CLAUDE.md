# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

Budget Calendar is a personal finance app built with **Kotlin Multiplatform + Compose Multiplatform**. The same Kotlin/Compose UI runs on both Android and iOS — there is no SwiftUI in this project. The iOS app is a thin wrapper that embeds `MainViewController` (a Compose screen) inside a `UIViewControllerRepresentable`.

Current source of truth is source code plus `docs/engineering/`. Use Pi discuss/plan/build workflow for new work.

---

## Repository Layout

```
src/
├── mobile/                     # KMP project (all mobile code lives here)
│   ├── composeApp/             # Shared Kotlin module (Android + iOS)
│   │   └── src/
│   │       ├── commonMain/     # Shared business logic and UI
│   │       ├── androidMain/    # Android entry point (MainActivity)
│   │       └── iosMain/        # iOS entry point (MainViewController)
│   └── iosApp/                 # Xcode project wrapping ComposeApp
└── web/                        # Empty — future backend/frontend
docs/
└── engineering/                # Architecture, workflow, conventions, traps, ADRs, design-system.html
```

The Kotlin package root is `com.petergabriel.budgetcalendar`.

---

## Build & Run

All Gradle commands must be run from `src/mobile/`:

```bash
cd src/mobile

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Install on connected Android device/emulator
./gradlew :composeApp:installDebug

# Run all common tests
./gradlew :composeApp:allTests

# Run Android unit tests only
./gradlew :composeApp:testDebugUnitTest

# Run a single test class
./gradlew :composeApp:testDebugUnitTest --tests "com.petergabriel.budgetcalendar.SomeTest"
```

For iOS: open `src/mobile/iosApp/iosApp.xcodeproj` in Xcode and run from there.

---

## Architecture

**Clean Architecture + MVVM + Unidirectional Data Flow**

```
UI (Compose) → ViewModel → UseCase → Repository Interface
                                          ↓
                                  RepositoryImpl → SQLDelight
```

**Package-by-feature** structure for each feature:

```
features/[feature]/
├── data/
│   ├── local/          # SQLDelight .sq query files
│   ├── repository/     # [Feature]RepositoryImpl
│   └── mapper/         # DB entity ↔ domain model
├── domain/
│   ├── model/          # Pure Kotlin domain models
│   ├── repository/     # I[Feature]Repository interface
│   └── usecase/        # One class per use case
└── presentation/
    ├── [Feature]ViewModel.kt
    └── [Feature]UiState.kt
```

Core shared infrastructure lives in:
- `core/database/` — SQLDelight `Database.kt` and generated query interfaces
- `core/utils/` — `DateUtils.kt`, `CurrencyUtils.kt`
- `di/KoinModules.kt` — all Koin module definitions

---

## Key Conventions

**Money**: Always stored and passed as `Long` (cents). Never use `Double` or `Float` for monetary values. Display formatting happens in `CurrencyUtils`.

**Time**: All timestamps are `Long` (Unix milliseconds, UTC). Use `DateUtils` for local timezone display. `kotlinx-datetime` is the date/time library.

**State transitions**: Transactions follow `PENDING → OVERDUE → CONFIRMED | CANCELLED`. Cancelled transactions are excluded from all balance calculations. Overdue transitions happen when `current_date > transaction_date + 1 day`.

**Safe to Spend formula**: `sum(spending pool account balances) - sum(PENDING amounts) - sum(OVERDUE amounts)`

**Dependency injection**: Koin with four modules: `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`. ViewModels use `viewModel { }` scope; use cases use `factory { }`.

**SQLDelight**: All SQL lives in `.sq` files under `src/commonMain/sqldelight/`. Never construct SQL strings at runtime. Schema migrations use numbered `.sqm` files.

---

## Code Search

**Always use `cocoindex-code` (semantic search) instead of `grep`, `rg`, or `Glob` when exploring the codebase.** It finds code by meaning, not exact text — useful for locating implementations, understanding data flows, or finding related code without knowing exact names.

- Use natural language queries: `"transaction state machine"`, `"Safe to Spend calculation"`, `"Koin module wiring"`
- Start with `limit: 5`; paginate with `offset` if results are relevant
- Fall back to `Grep`/`Glob` only for exact string matches (e.g. finding a known class name or file path)

---

## Planning & Engineering Docs Workflow

- Use Pi discuss mode to clarify requirements, plan mode to produce ordered task queues, and build mode to implement one confirmed task at a time.
- **`docs/engineering/`** is the maintained engineering-docs root: architecture, workflow, conventions, invariants, traps, and ADRs.
- **`docs/engineering/design-system.html`** is the offline design-token and component catalog; Compose implementation lives under `core/designsystem/`.
- Start implementation by searching existing code with `cocoindex-code`/`ccc`; keep docs updates scoped to verified code truth.

---

## Versions (from `libs.versions.toml`)

| Library | Version |
|---------|---------|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.10.0 |
| Material3 | 1.10.0-alpha05 |
| AGP | 8.13.2 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |

SQLDelight, Koin, and kotlinx-datetime are part of the current KMP app; see `docs/engineering/dev-workflow.md` and `docs/engineering/conventions.md` for current commands and conventions.
