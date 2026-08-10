# Dev Workflow

Current mobile app lives under `src/mobile/`; Gradle root includes only `:composeApp`: `src/mobile/settings.gradle.kts:1-34`.

## Pi workflow

- Use Pi modes in order: discuss to clarify, plan to produce Section 4 task queues, build to implement one confirmed task at a time.
- Engineering docs live under `docs/engineering/`; ADRs live under `docs/engineering/decisions/`.
- When exact text repeats across call sites, include enclosing function/component context in replacement anchors so only intended site changes.
- Design-token and component reference is `docs/engineering/design-system.html` plus Compose code under `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/`; never resolve `core/designsystem/...` from repo root. If an expected source path fails, inspect the project tree before retrying.

## Code search

- Prefer semantic search first: `ccc search "safe to spend calculation"` from repo root. Project skill docs own `ccc` indexing/search workflow: `.pi/skills/ccc/SKILL.md`.
- Run ccc searches serially. The shared daemon can race on its socket (`FileExistsError`); retry with a narrower query after refresh before falling back.
- Empty `--path` values are rejected. Use a valid project-relative path. No semantic results do not prove source absence: after narrower retries, use exact `find`/`rg`/`read` with concrete file paths. `read` does not expand wildcard paths.
- For raw-token or path scans, discover exact directories first; do not include guessed/nonexistent paths because one bad operand can hide useful output.
- After source changes, refresh ccc and record clean indexing even when known symbols still return no semantic matches.

## Compose

- Inside `Row` or `Column` scope, call `Modifier.weight(...)` without importing `androidx.compose.foundation.layout.weight`; that import resolves an internal parent-data accessor and fails compilation.
- Named arguments cannot be passed through a Kotlin function reference. Use an explicit lambda or branch when callbacks need named arguments; compile the affected source before interpreting later test failures.
- Compose test APIs vary by dependency version. Search available APIs before using collection helpers such as `onLast()` or `onAllNodes`; prefer stable semantic nodes and canonical rendered text.
- Update every production and test call site after changing a function signature, including required nullable/default fields, before interpreting compiler or runtime failures.

## Engineering-doc references

- Before adding source line references, search each target doc independently for the source filename. Do not infer a file's references from combined multi-file search output.

## Android / Gradle

Run Gradle from `src/mobile/` because wrapper and settings live there: `src/mobile/gradlew`, `src/mobile/settings.gradle.kts:1-34`. Running `./gradlew` from repo root fails because no wrapper exists there.

```bash
cd src/mobile

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Install on connected Android device/emulator
./gradlew :composeApp:installDebug

# Run all KMP target tests
./gradlew :composeApp:allTests

# Run Android unit tests only
./gradlew :composeApp:testDebugUnitTest

# Run connected Android instrumented tests (task does not support --tests)
./gradlew :composeApp:connectedDebugAndroidTest

# Full release gate from src/mobile/
./gradlew :composeApp:allTests :composeApp:testDebugUnitTest :composeApp:assembleDebug
./gradlew :composeApp:connectedDebugAndroidTest

# Run one test class or method
./gradlew :composeApp:testDebugUnitTest --tests "com.petergabriel.budgetcalendar.SomeTest"
```

These task names are present in Gradle task output for `:composeApp`; `CLAUDE.md:41-64` lists the same commands.

Home available-credit regression is in common tests and can run alone:

```bash
cd src/mobile
./gradlew :composeApp:testDebugUnitTest --tests "com.petergabriel.budgetcalendar.features.home.presentation.HomeViewModelTest"
```

`HomeViewModelTest` uses fake account, credit-card, and transaction repositories with `StandardTestDispatcher`; it covers configured and unconfigured limits, per-card over-limit clamping, and transaction-triggered updates: `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeViewModelTest.kt:1-117`.

## Preview QA

- Validate each preview HTML file under `docs/design/previews/` with balanced CSS braces, declared-vs-used CSS variables, script syntax, and required theme-toggle markers. If a here-document cannot create its temporary file, retry with `python3 -c`; simplify over-escaped regexes before treating a `PatternError` as a design failure. Contrast checks can emit xcrun cache-permission warnings; inspect exit status and reported contrast values separately.
- If a screenshot shows browser defaults, inspect serving/reload and stylesheet loading before changing design values. Keep preview metadata outside the phone frame; frame the actual app surfaces. The preview stage follows `bg-primary`, rows use explicit tokenized full borders with `appearance: none`, sections use tokenized vertical gaps, and every referenced primitive (including `--p-space-5` and `--p-blue-100`) must be declared with correct light/dark mappings.

## iOS

- Open `src/mobile/iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` target. Xcode project exists at `src/mobile/iosApp/iosApp.xcodeproj/project.pbxproj`.
- CLI simulator smoke: `xcodebuild -project src/mobile/iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=<device>' build CODE_SIGNING_ALLOWED=NO`; launch once in light and dark system appearance, then shut down temporary simulator after verification.
- iOS host is thin SwiftUI wrapper around Compose `MainViewController()`: `src/mobile/iosApp/iosApp/ContentView.swift:1-18`.
- Kotlin iOS entry initializes Koin if needed and calls shared `App()`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.

## Plan checkpoints and staging

- If `workflow_plan_save` reports `Pi getAgentDir() unavailable`, preserve the authored plan and retry the workflow tool on a later explicit turn; never write the plan store directly. Task progress uses the active tracker ID only.
- Keep one confirmed task per checkpoint. Verify the task gate, stage exact paths, inspect `git diff --cached --name-status`, commit, then tick the tracker. If a worker is aborted or the task requires no subagents, the parent remains owner; do not assume an unreturned worker result.
- A failed edit may have applied nothing. Re-read the current file and retry with unique anchors. Include both `oldText` and `newText` for every replacement.
- Before committing, run `git diff --cached --check`. Intentional Markdown hard-break spaces can trigger it; inspect the staged diff and normalize changed lines or document an unchanged baseline rather than blanket-skipping whitespace checks.

## Project/module shape

- Shared module: `:composeApp`, included in Gradle settings: `src/mobile/settings.gradle.kts:34`.
- Package/namespace/application ID: `com.petergabriel.budgetcalendar`: `src/mobile/composeApp/build.gradle.kts:74-84`.
- KMP targets: Android, `iosArm64`, `iosSimulatorArm64`: `src/mobile/composeApp/build.gradle.kts:12-27`.
- SQLDelight DB package: `com.petergabriel.budgetcalendar.core.database`: `src/mobile/composeApp/build.gradle.kts:107-113`.

## Source sets

- `commonMain`: shared Compose UI, business logic, SQLDelight runtime/coroutines, Koin core/Compose: `src/mobile/composeApp/build.gradle.kts:36-53`.
- `androidMain`: Android entry/deps, Android SQLDelight driver, Koin Android: `src/mobile/composeApp/build.gradle.kts:29-35`.
- `iosMain`: native SQLDelight driver: `src/mobile/composeApp/build.gradle.kts:54-56`.
- Tests: `commonTest`, `androidUnitTest`, `androidInstrumentedTest`: `src/mobile/composeApp/build.gradle.kts:57-70`.
- Test discovery must search all three test source sets; Compose UI tests can exist only in `androidInstrumentedTest`.

## Entry points

- Android: `MainActivity` initializes Koin with `androidContext`, then `setContent { App() }`: `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:13-28`.
- iOS: `MainViewController()` initializes Koin and hosts `App()` in `ComposeUIViewController`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.
- Shared UI root: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:66-193`.

## DI wiring

- All shared Koin wiring is in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt`.
- Installed modules: `platformModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:106-220`.
- Repositories are `single`, use cases are `factory`, ViewModels use `viewModel`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:120-211`.
