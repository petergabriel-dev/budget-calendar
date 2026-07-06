# AGENTS.md

This file describes the agent skills available in `.claude/skills/`. Skills are loaded by Claude Code when relevant tasks are detected. Each skill contains a `SKILL.md` with conventions, rules, and pointers to reference files.

---

## Code Search: cocoindex

**Always prefer `cocoindex-code` (semantic search MCP tool) over `grep`, `rg`, or `Glob` when exploring this codebase.** It understands meaning, not just text — find implementations, trace data flows, and locate related code without knowing exact names.

Use it at the start of any implementation task to understand existing code before writing new code. Include `cocoindex-code` search steps in all `/plan`-generated prompts wherever existing code must be located or understood.

---

## Pi Workflow & Engineering Docs

Use Pi discuss/plan/build workflow: clarify requirements, create ordered task queues, then implement one confirmed task at a time.

Source of truth:
- Current source code
- `docs/engineering/` for architecture, workflow, conventions, invariants, traps, and ADRs
- `docs/engineering/design-system.html` plus `core/designsystem/` for design tokens and reusable Compose components

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

