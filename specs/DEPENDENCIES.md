# Dependencies Specification

## External Dependencies

### Core Kotlin Multiplatform

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| kotlin | 1.9.22 | Kotlin compiler and stdlib | active |
| kotlin-stdlib | 1.9.22 | Kotlin standard library | active |
| kotlin-gradle-plugin | 1.9.22 | Gradle Kotlin DSL support | active |

### Database

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| sqldelight | 2.0.1 | Type-safe SQL database | active |
| sqldelight-gradle-plugin | 2.0.1 | SQLDelight Gradle integration | active |

### Dependency Injection

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| koin | 3.5.3 | Lightweight DI framework | active |
| koin-core | 3.5.3 | Core DI functionality | active |
| koin-android | 3.5.3 | Android-specific DI | active |
| koin-androidx-compose | 3.5.3 | Compose integration | active |

### Concurrency

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| kotlin-coroutines-core | 1.7.3 | Coroutines support | active |
| kotlinx-coroutines | 1.7.3 | Async/await primitives | active |

### Date/Time

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| kotlinx-datetime | 0.5.0 | Multiplatform date/time | active |

### Serialization (Future API)

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| kotlinx-serialization-json | 1.6.2 | JSON serialization | active |

### HTTP Client (Future API)

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| ktor-client-core | 2.3.7 | HTTP client | active |
| ktor-client-content-negotiation | 2.3.7 | Content negotiation | active |

### Font Assets

| Asset | Source | Usage | Status |
|-------|--------|-------|--------|
| Outfit | Google Fonts (OFL License) | Display text, headings, hero amounts, badges, section titles | required |
| Inter | Google Fonts (OFL License) | Body text, labels, descriptions, form inputs, metadata | required |

**Bundling strategy:**
- Android: Place `.ttf` files in `composeApp/src/commonMain/composeResources/font/`
- iOS: Same shared font files via Compose Multiplatform resource system
- Load via `Font(resource = Res.font.outfit_bold)` using `compose-multiplatform-resources`

---

### Android UI (Jetpack Compose)

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| compose-bom | 2024.02.00 | Compose library bundle | active |
| androidx-compose-ui | (via BOM) | UI toolkit | active |
| androidx-compose-ui-graphics | (via BOM) | Graphics primitives | active |
| androidx-compose-ui-tooling | (via BOM) | Debug tooling | active |
| androidx-compose-ui-tooling-preview | (via BOM) | Preview support | active |
| androidx-compose-material3 | (via BOM) | Material 3 components | active |
| androidx-compose-material-icons | (via BOM) | Material icons | active |
| androidx-activity-compose | 1.8.2 | Activity Compose integration | active |
| androidx.lifecycle-runtime-ktx | 2.7.0 | Lifecycle runtime | active |
| androidx.lifecycle-viewmodel-compose | 2.7.0 | ViewModel Compose | active |
| androidx.lifecycle-runtime-compose | 2.7.0 | Runtime Compose | active |
| androidx.navigation-compose | 2.7.7 | Compose navigation | active |

### iOS UI (SwiftUI)

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| swiftui | (built-in) | Apple UI framework | active |
| foundation | (built-in) | Foundation types | active |

### Testing

| Package | Version | Purpose | Status |
|---------|---------|---------|--------|
| junit | 4.13.2 | Unit testing | active |
| androidx-test-ext | 1.1.5 | Android testing extensions | active |
| androidx.compose.ui-test | (via BOM) | Compose UI testing | active |
| kotlin-test | 1.9.22 | Kotlin test library | active |
| kotlin-test-common | 1.9.22 | Common test utilities | active |

#### Core Dependencies

- **kotlin** (1.9.22) - Kotlin language runtime
- **sqldelight** (2.0.1) - Local SQLite database
- **koin** (3.5.3) - Dependency injection
- **kotlinx-coroutines** (1.7.3) - Asynchronous programming
- **kotlinx-datetime** (0.5.0) - Date/time handling
- **compose-bom** (2024.02.00) - Android UI toolkit

#### Dev Dependencies

- **kotlin-gradle-plugin** (1.9.22) - Kotlin Gradle DSL
- **sqldelight-gradle-plugin** (2.0.1) - Database code generation
- **androidx-compose-ui-tooling** (via BOM) - Debug tools
- **junit** (4.13.2) - Unit testing framework
- **kotlin-test** (1.9.22) - Kotlin test support

---

## Internal Dependencies

### Feature Modules

| Module | Depends On | Purpose |
|--------|------------|---------|
| `features/accounts` | `core/database`, `core/di` | Account management feature |
| `features/transactions` | `core/database`, `core/di`, `features/accounts` | Transaction management |
| `features/calendar` | `core/database`, `core/di`, `features/accounts`, `features/transactions` | Calendar view with budget data |
| `features/sandbox` | `core/di` | Experimental features |
| `features/creditcard` | `core/database`, `core/di`, `features/accounts`, `features/transactions`, `features/budget` | Credit card liability tracking and payment management |

### Core Modules

| Module | Depends On | Purpose |
|--------|------------|---------|
| `core/database` | `sqldelight` | Database abstraction layer |
| `core/di` | `koin` | Dependency injection setup |

### Dependency Graph

```
                    ┌─────────────────────┐
                    │   features/calendar │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│features/accounts│  │features/transac │  │    core/di      │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  core/database  │  │  core/database  │  │      koin       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Module Responsibilities

- **core/database**: SQLDelight database setup, migrations, queries
- **core/di**: Koin modules, dependency providers
- **features/accounts**: Account CRUD, account types, balances
- **features/transactions**: Transaction CRUD, categorization, recurring
- **features/calendar**: Calendar view, budget visualization, date navigation
- **features/sandbox**: Experimental features, rapid prototyping

---

## Version Constraints

- **Kotlin**: 1.9.22
- **Compose BOM**: 2024.02.00
- **SQLDelight**: 2.0.1
- **Koin**: 3.5.3
- **Coroutines**: 1.7.3
- **kotlinx-datetime**: 0.5.0
- **kotlinx-serialization**: 1.6.2
- **Ktor**: 2.3.7

### Android Configuration

- **minSdk**: 26
- **targetSdk**: 34
- **compileSdk**: 34

---

## Security

| Package | Vulnerability Status | Notes |
|---------|---------------------|-------|
| kotlin-stdlib | No known CVEs | Regularly updated |
| sqldelight | No known CVEs | Active maintenance |
| koin | No known CVEs | Lightweight, audited |
| kotlinx-coroutines | No known CVEs | JetBrains maintained |
| kotlinx-datetime | No known CVEs | Stable release |
| kotlinx-serialization | No known CVEs | JetBrains maintained |
| ktor | No known CVEs | JetBrains maintained |

---

## Package Manager

### Gradle (Kotlin Multiplatform)

- **Plugin**: `org.jetbrains.kotlin.multiplatform` (1.9.22)
- **Android**: Gradle with Android Gradle Plugin 8.2.2
- **iOS**: Gradle with CocoaPods for Swift dependencies

### Gradle Dependencies Format

```kotlin
// Root build.gradle.kts
plugins {
    kotlin("multiplatform") version "1.9.22"
    id("com.squareup.sqldelight") version "2.0.1"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

// Common dependencies
commonMainApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
commonMainApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
commonMainApi("io.insert-koin:koin-core:3.5.3")
commonMainApi("io.ktor:ktor-client-core:2.3.7")

// Android dependencies
androidMainApi("androidx.compose.ui:ui")
androidMainApi("io.insert-koin:koin-android:3.5.3")
androidMainApi("io.insert-koin:koin-androidx-compose:3.5.3")

// iOS dependencies (native)
// Koin for iOS via CocoaPods
```

---

## Notes

1. **Offline-First**: No network dependencies required initially; data persists locally via SQLDelight
2. **Multiplatform**: Core logic in `commonMain`; platform-specific UI in `androidMain`/`iosMain`
3. **DI Strategy**: Koin chosen for simplicity and Kotlin-first API; scoped to Android/iOS separately
4. **Database**: SQLDelight generates type-safe Kotlin from SQL queries; no runtime SQL construction
5. **SwiftUI**: Native iOS UI; no Compose sharing (by design for best iOS UX)
6. **Future API**: kotlinx-serialization and Ktor included for future cloud sync backend
