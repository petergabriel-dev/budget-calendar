# Dev Workflow

Current mobile app lives under `src/mobile/`; Gradle root includes only `:composeApp`: `src/mobile/settings.gradle.kts:1-34`.

## Pi workflow

- Use Pi modes in order: discuss to clarify, plan to produce Section 4 task queues, build to implement one confirmed task at a time.
- Engineering docs live under `docs/engineering/`; ADRs live under `docs/engineering/decisions/`.
- Design-token and component reference is `docs/engineering/design-system.html` plus Compose code under `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/`.

## Code search

- Prefer semantic search first: `ccc search "safe to spend calculation"` from repo root. Project skill docs own `ccc` indexing/search workflow: `.pi/skills/ccc/SKILL.md`.
- Use exact grep only after you know symbol/file names.

## Android / Gradle

Run Gradle from `src/mobile/` because wrapper and settings live there: `src/mobile/gradlew`, `src/mobile/settings.gradle.kts:1-34`.

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

# Run one test class or method
./gradlew :composeApp:testDebugUnitTest --tests "com.petergabriel.budgetcalendar.SomeTest"
```

These task names are present in Gradle task output for `:composeApp`; `CLAUDE.md:41-64` lists the same commands.

## iOS

- Open `src/mobile/iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` target. Xcode project exists at `src/mobile/iosApp/iosApp.xcodeproj/project.pbxproj`.
- iOS host is thin SwiftUI wrapper around Compose `MainViewController()`: `src/mobile/iosApp/iosApp/ContentView.swift:1-18`.
- Kotlin iOS entry initializes Koin if needed and calls shared `App()`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.

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

## Entry points

- Android: `MainActivity` initializes Koin with `androidContext`, then `setContent { App() }`: `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:13-28`.
- iOS: `MainViewController()` initializes Koin and hosts `App()` in `ComposeUIViewController`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.
- Shared UI root: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:66-193`.

## DI wiring

- All shared Koin wiring is in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt`.
- Installed modules: `platformModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:106-224`.
- Repositories are `single`, use cases are `factory`, ViewModels use `viewModel`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:120-211`.
