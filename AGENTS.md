# AGENTS.md

This file describes the agent skills available in `.claude/skills/`. Skills are loaded by Claude Code when relevant tasks are detected. Each skill contains a `SKILL.md` with conventions, rules, and pointers to reference files.

---

## Code Search: cocoindex

**Always prefer `cocoindex-code` (semantic search MCP tool) over `grep`, `rg`, or `Glob` when exploring this codebase.** It understands meaning, not just text — find implementations, trace data flows, and locate related code without knowing exact names.

Use it at the start of any implementation task to understand existing code before writing new code. Include `cocoindex-code` search steps in all `/plan`-generated prompts wherever existing code must be located or understood.

---

## Pencil Design Fidelity

**Any task that produces Compose UI must follow this confirmation loop — include it explicitly in every `/plan`-generated prompt that touches frontend:**

1. **Pre-implementation**: call `mcp__pencil__batch_get` with the component node ID(s) from `specs/DESIGN_SYSTEM.pen` to read the authoritative visual spec before writing code.
2. **Implementation**: build the composable using exact tokens, sizes, spacing, and layout from step 1.
3. **Post-implementation**: call `mcp__pencil__batch_get` again and diff the spec against your implementation. Fix any mismatches before marking the task done.

Every UI component task in a prompt file must contain a sub-step in this form:
```
- Read component `<nodeId>` from `specs/DESIGN_SYSTEM.pen` via `mcp__pencil__batch_get`, implement to match, then re-read and confirm visual fidelity before marking done.
```

`DESIGN_SYSTEM.md` is a secondary reference. `specs/DESIGN_SYSTEM.pen` is the source of truth.

---

## Available Skills

### `kmp` — Kotlin Multiplatform Development

**Path:** `.claude/skills/kmp/SKILL.md`

**Activate for:** any task involving shared business logic, Compose UI, SQLDelight schema, Koin DI, feature scaffolding, or cross-platform architecture. Do NOT activate for purely native XML layouts or standalone Swift-only tasks.

**Covers:**
- Source set rules (`commonMain` / `androidMain` / `iosMain`) and what goes where
- Package-by-feature structure under `com.petergabriel.budgetcalendar`
- Dependency remapping (SQLDelight not Room, Koin not Hilt, Ktor not Retrofit, etc.)
- Non-negotiable coding rules (money as `Long` cents, timestamps as Unix ms, no `!!`, UDF state, `Result<T>` wrapping, immutable data classes)

**Reference files** (load the relevant one per sub-task):

| File | Load when… |
|------|------------|
| `.claude/skills/kmp/references/architecture.md` | Designing layers, ViewModel, UiState, or UseCase |
| `.claude/skills/kmp/references/di.md` | Wiring Koin modules, scopes, or DI structure |
| `.claude/skills/kmp/references/database.md` | Writing SQLDelight schema, `.sq` queries, or mappers |
| `.claude/skills/kmp/references/platform.md` | Android/iOS platform files, `expect`/`actual`, or drivers |

---

### `agentic-engineering` — Shadow Specs & Planning Conventions

**Path:** `.claude/skills/agentic-engineering/SKILL.md`

**Activate for:** running `/spec` (writing shadow specs) or `/plan` (generating implementation task lists). Also relevant when interpreting or extending any file in `specs/shadow/`.

**Covers:**
- SudoLang syntax: interfaces, enums, constraints, data flow pipes, function signatures, UI component props
- Required sections for every shadow spec in `specs/shadow/[FeatureName].shadow.md`
- Naming conventions (PascalCase features, snake_case DB columns, `idx_[table]_[column]` indexes)
- Shadow spec status values: `Planned`, `In Progress`, `Done`
- How `/spec` maps feature requirements → shadow spec
- How `/plan` decomposes a shadow spec into ordered implementation tasks

**Relationship between spec files:**
```
specs/FEATURES.md       ← business rules, validation, use cases
specs/ARCHITECTURE.md   ← layers, package structure, data flows
specs/DB_DESIGN.md      ← schema, queries, indexes, migrations
specs/DESIGN_SYSTEM.md  ← components, colors, typography, spacing
specs/DEPENDENCIES.md   ← external libraries, versions, module graph

specs/shadow/[Name].shadow.md
  ← synthesizes all of the above into one implementable unit
  ← consumed by /plan to generate docs/prompts/[NN]_[feature].md
```
