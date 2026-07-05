---
id: ADR-0003
title: Single Compose UI for Android and iOS
status: Active
date: 2026-07-06
---

# ADR-0003: Single Compose UI for Android and iOS

## Decision

- Product UI is Kotlin Compose Multiplatform shared across Android and iOS.
- iOS native layer is a thin host that embeds `MainViewController()`; no separate SwiftUI feature implementation.
- Shared entry point is `App()` in package `com.petergabriel.budgetcalendar`.

## Why

- iOS Kotlin entry wraps shared `App()` in `ComposeUIViewController`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:1-12`.
- Swift `ContentView` only hosts the Compose UIViewController: `src/mobile/iosApp/iosApp/ContentView.swift:1-18`.
- Shared `App()` owns tab navigation and shared screens: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:66-193`.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/dev-workflow.md`
- `docs/engineering/traps.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:66-193`
- `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:1-12`
- `src/mobile/iosApp/iosApp/ContentView.swift:1-18`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:213-224`

## Consequences

- Good: one UI implementation and design system for both platforms.
- Good: iOS bootstraps shared DI and UI through Kotlin entrypoint.
- Cost: platform-specific UI polish must be expressed through Compose or thin platform hosts.
- Risk: adding feature logic in SwiftUI creates duplicate behavior and test drift.

## Read when

- adding screens, navigation, or feature UI
- touching iOS app entry points
- deciding whether UI belongs in SwiftUI or common Compose

## Supersedes

- Native SwiftUI feature-screen architecture. SwiftUI remains only as host shell for Compose.
