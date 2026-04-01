# Agentic Engineering Skill — Budget Calendar

## Purpose

This skill defines the SudoLang conventions and shadow spec structure used in this project. Shadow specs are pseudocode-level design documents that live in `specs/shadow/` and bridge the gap between feature requirements (`specs/FEATURES.md`) and implementation. They are consumed by `/plan` to generate concrete implementation tasks.

---

## SudoLang Conventions

SudoLang is a structured pseudocode language used in this project for expressing design intent. It is not executable — it is read by humans and AI agents to understand data shapes, business rules, and data flows.

### Syntax Rules

#### Interfaces (Data Models)
Use TypeScript-style interfaces with explicit types. Always include `id`, `createdAt`, and `updatedAt` on persistent entities.

```sudolang
interface EntityName {
  id: Long;
  fieldName: Type;
  optionalField: Type?;    // ? denotes optional/nullable
  listField: List<Type>;
  createdAt: Long;         // Unix timestamp ms
  updatedAt: Long;
}
```

**Supported types:** `Long`, `String`, `Boolean`, `Int`, `Double`, `List<T>`, `Map<K,V>`, `Flow<T>`, `Result<T>`, enum names.

#### Enums
```sudolang
enum EnumName {
  VALUE_ONE = "value_one"    // snake_case string matches DB storage
  VALUE_TWO = "value_two"
}
```

#### Constraints
State invariants and business rules as flat assertions. These map directly to validation logic and domain guards.

```sudolang
Constraint: [Subject] must/cannot/is [rule]
Constraint: [Field] must be [range or set]
Constraint: [State machine rule — valid transitions only]
```

#### Data Flow (Pipes)
Express data transformation as a pipeline using `|>`. Each step is a function, use case, or branch.

```sudolang
Input |> validateInput |> UseCase.execute()
  |> If (condition) |> BranchStep
  |> Repository.save()
  |> Return Result
  |> Update UiState
```

- Use `If (condition) |>` for conditional branches
- Use `ForEach |>` for iteration
- Use `Return` as the terminal step before UI update

#### Function Signatures
```sudolang
fn FunctionName(param: Type, param2: Type): ReturnType
```

Use `Result<T>` for single-value operations that can fail. Use `Flow<T>` for reactive streams. Use `void` for side-effect-only operations.

#### UI Component Props
```sudolang
interface ComponentNameProps {
  propName: Type;
  onAction: () -> Unit;
  onActionWithArg: (Type) -> Unit;
  optionalHandler: () -> Unit?;
}
```

---

## Shadow Spec Sections

Every shadow spec in `specs/shadow/[FeatureName].shadow.md` must include these sections in order:

| Section | Required | Purpose |
|---------|----------|---------|
| Header | Yes | Feature name, description, status, author |
| Context Imports | Yes | Links to spec files for context |
| Architecture | Yes | Package structure for this feature |
| Data Model | Yes | Domain interfaces, enums, request/response models, UiState |
| Constraints | Yes | Business rules and invariants |
| Data Flow | Yes | Pipe-based flow for each major operation |
| Function Signatures | Yes | Handler, use case, and repository signatures |
| UI Components | Yes | Prop interfaces and design system component references |
| Database Schema | If new tables | SQLDelight table definition |
| SQLDelight Queries | If new queries | Named SQL queries |
| Dependencies | Yes | External and internal dependencies used |
| Edge Cases | Yes | Table of edge cases with conditions and handling |

---

## Naming Conventions

| Context | Convention | Example |
|---------|------------|---------|
| Feature names | PascalCase | `AccountManagement`, `SafeToSpend` |
| Shadow spec files | `[FeatureName].shadow.md` | `Account.shadow.md` |
| Interfaces | PascalCase | `CreateTransactionRequest` |
| Enum values | UPPER_SNAKE_CASE | `CREDIT_CARD` |
| DB column values | snake_case string | `"credit_card"` |
| Function names | PascalCase verbs | `CreateAccount`, `GetTransactionById` |
| DB table names | snake_case plural | `recurring_transactions` |
| DB index names | `idx_[table]_[column]` | `idx_accounts_spending_pool` |

---

## Status Values

Shadow specs use these status values in their header:

| Status | Meaning |
|--------|---------|
| `Planned` | Spec written, not yet implemented |
| `In Progress` | Implementation underway |
| `Done` | Fully implemented and tested |

---

## Key Project Conventions

These are non-negotiable patterns that all shadow specs and generated implementation must follow:

1. **Money as cents** — All monetary values are `Long` in cents (e.g. `150000` = ₱1,500.00). Never use `Double` or `Float` for money.
2. **Unix timestamps in ms** — All datetime fields are `Long` milliseconds since epoch.
3. **SQLite booleans** — Stored as `Integer` (0/1), not native bool.
4. **Offline-first** — No feature may require a network call for its core functionality.
5. **UDF state management** — UI state is immutable. ViewModels emit `Flow<UiState>`. UI never mutates state directly.
6. **Package-by-feature** — All code for a feature lives under `features/[feature-name]/` with `data/`, `domain/`, and `presentation/` subdirectories.
7. **Repository pattern** — Domain layer only sees repository interfaces (`IXxxRepository`). Implementations live in `data/repository/`.
8. **Result wrapping** — All use case return types are `Result<T>` or `Flow<T>`. No raw throws across layer boundaries.
9. **Soft deletes** — Entities are not physically deleted when referenced; they are marked inactive or the reference is checked first.
10. **UUID for API, Long for DB** — Internal SQLite uses `INTEGER PRIMARY KEY AUTOINCREMENT`. Future API uses UUID strings.

---

## Relationship to Other Spec Files

```
specs/FEATURES.md      ← business rules, validation, use cases
specs/ARCHITECTURE.md  ← layers, package structure, data flows
specs/DB_DESIGN.md     ← schema, queries, indexes, migrations
specs/DESIGN_SYSTEM.md ← components, colors, typography, spacing
specs/DEPENDENCIES.md  ← external libraries, versions, module graph

specs/shadow/[Name].shadow.md
  ← synthesizes all of the above into a single implementable unit
  ← consumed by /plan to generate ordered implementation tasks
```

---

## Usage in `/spec` Workflow

When running `/spec`:

1. Read all five spec files for context
2. Identify the feature and check if a shadow spec already exists
3. Use `specs/shadow/Template.shadow.md` as the structural base
4. Fill each section using the SudoLang conventions defined in this file
5. Cross-reference `specs/FEATURES.md` for business rules and edge cases
6. Cross-reference `specs/DB_DESIGN.md` for schema and queries
7. Cross-reference `specs/DESIGN_SYSTEM.md` for component IDs and props
8. Update `specs/FEATURES.md` to add/update the feature entry with `Planned` status and shadow spec path

## Usage in `/plan` Workflow

When running `/plan`:

1. Read the target shadow spec
2. Use this SKILL.md to interpret SudoLang syntax
3. Decompose each section into implementation tasks, in dependency order:
   - DB schema → SQLDelight queries → Domain models → Repository interface → Repository impl → Use cases → ViewModel → UI
4. Each task should reference the relevant shadow spec section
5. **Include `cocoindex-code` search steps** at the start of any task that requires locating or understanding existing code before writing new code. Use natural language queries (e.g. `"Koin module definitions"`, `"SQLDelight database setup"`, `"existing ViewModel pattern"`). Do not use `grep` or `Glob` for exploratory searches — reserve those for exact-match lookups only.
