---
name: kmp
description: >
  Kotlin Multiplatform mobile development for Budget Calendar. Activate for any task
  involving shared business logic, Compose UI, SQLDelight schema, Koin DI, feature
  scaffolding, or cross-platform architecture. Do NOT activate for purely native XML
  layouts or standalone Swift-only tasks.
version: 1.0.0
---

# KMP Skill — Budget Calendar

## Stack Identity

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.x (Multiplatform) |
| UI | Compose Multiplatform (commonMain) |
| Database | SQLDelight 2.x (SQLite, offline-first) |
| DI | Koin 3.x (lightweight, no annotation processing) |
| Async | Kotlin Coroutines + Flow |
| Date/Time | kotlinx-datetime |
| Serialization | kotlinx-serialization-json (future API) |
| HTTP | Ktor Client (future API) |
| Package | `com.petergabriel.budgetcalendar` |

## Source Set Rules

**CRITICAL**: Violating these causes compilation failures.

| What | Where | Why |
|------|-------|-----|
| Business logic, domain models, use cases | `commonMain` | Must compile on all targets |
| All Compose UI | `commonMain` | Compose Multiplatform is shared |
| SQLDelight queries, repository interfaces | `commonMain` | Cross-platform data layer |
| `java.io`, `java.util.UUID`, Android Context | NEVER in `commonMain` | JVM-only APIs |
| SQLDelight Android driver, Koin Android setup | `androidMain` | Android-specific |
| SQLDelight native driver, Koin iOS setup | `iosMain` | iOS-specific |
| `expect` declarations | `commonMain` | Compile contract for all targets |
| `actual` implementations | `androidMain` + `iosMain` | Per-target fulfillment |

Use `expect/actual` only for **stateless utility functions** (e.g., UUID generation, platform name).
Use **interface + Koin injection** for anything with lifecycle, state, or external dependencies.

## Package-by-Feature Structure

Every feature lives entirely under its own package. Never organize by layer.

```
commonMain/kotlin/com/petergabriel/budgetcalendar/
├── features/
│   └── [feature-name]/
│       ├── data/
│       │   ├── local/         # SQLDelight generated query interfaces
│       │   ├── repository/    # IXxxRepository implementation
│       │   └── mapper/        # DatabaseEntity.toDomain() extensions
│       ├── domain/
│       │   ├── model/         # Pure Kotlin domain classes
│       │   ├── repository/    # IXxxRepository interface (abstraction only)
│       │   └── usecase/       # One class per use case
│       └── presentation/
│           ├── XxxViewModel.kt
│           └── XxxUiState.kt
├── core/
│   ├── database/              # Database.kt, SQLDelight setup
│   └── di/                    # KoinModules.kt (assembles all feature modules)
└── navigation/
    └── NavigationGraph.kt
```

## Dependency Remapping

Never use the Android-native default. Always use the KMP equivalent.

| Android Default | KMP Equivalent | Notes |
|----------------|----------------|-------|
| Room | **SQLDelight** | Write `.sq` files, not annotations |
| Hilt / Dagger | **Koin** | DSL modules, no codegen |
| Retrofit / OkHttp | **Ktor Client** | Future use; inject platform engine |
| Gson / Moshi | **kotlinx-serialization** | Annotate DTOs with `@Serializable` |
| SharedPreferences | **MultiplatformSettings** | If key-value store needed |
| Glide / Picasso | **Coil 3.x** | If image loading needed |
| ViewModel (AndroidX) | **`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`** | KMP-compatible ViewModel |
| LiveData | **StateFlow / SharedFlow** | Coroutines-native, cross-platform |

## Non-Negotiable Coding Rules

1. **Money as Long cents** — `15000L` = ₱150.00. Never `Double` or `Float` for money.
2. **Timestamps as Long ms** — Unix milliseconds. Never `String` dates in DB.
3. **No `!!` operator** — Use `?.`, `?: error(...)`, or `requireNotNull()` with a message.
4. **Constructor injection only** — Never call `get()` inside a class body. All dependencies arrive via constructor.
5. **UDF state** — `ViewModel` exposes `val uiState: StateFlow<XxxUiState>`. UI never mutates state directly.
6. **Result wrapping** — Use cases return `Result<T>` or `Flow<T>`. No raw exceptions across layer boundaries.
7. **Immutable data classes** — All domain models and UiState are `data class` with `val` fields only.
8. **SQLite booleans** — `INTEGER NOT NULL DEFAULT 0` (0/1). No native bool type in schema.
9. **Interface for repositories, not use cases** — `IAccountRepository` exists. `ICreateAccountUseCase` does not (YAGNI).
10. **Offline-first** — Every feature must work with zero network. Network is additive, never required.

## Reference Loading Guide

Load the relevant reference file when starting that sub-task:

| Task | Load |
|------|------|
| Designing layers, ViewModel, UiState, UseCase | `references/architecture.md` |
| Wiring Koin modules, scopes, DI structure | `references/di.md` |
| Writing SQLDelight schema, queries, mappers | `references/database.md` |
| Android/iOS platform files, expect/actual, drivers | `references/platform.md` |

> Reference files are in `.claude/skills/kmp/references/`. Read them with the Read tool when needed.
