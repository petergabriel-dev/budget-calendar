# KMP Reference: Platform-Specific Patterns

## Android Entry Point

`MainActivity` is a thin host. Zero business logic lives here.

```kotlin
// androidMain/com/petergabriel/budgetcalendar/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()   // Shared Compose entry point from commonMain
        }
    }
}
```

If Koin is initialized in an `Application` class (preferred for SQLite driver context):

```kotlin
// androidMain — BudgetCalendarApp.kt
class BudgetCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BudgetCalendarApp)
            modules(androidModule + allModules)
        }
    }
}
```

Register in `AndroidManifest.xml`:
```xml
<application android:name=".BudgetCalendarApp" ...>
```

---

## iOS Entry Point

```kotlin
// iosMain/com/petergabriel/budgetcalendar/MainViewController.kt
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        App()   // Same shared Compose entry point
    }
```

Koin initialization for iOS is called from Swift before the first view controller:

```swift
// iOSApp.swift
@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.doInitKoin()  // calls initKoin() in iosMain
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

```kotlin
// iosMain — expose to Swift
fun initKoin() {
    startKoin {
        modules(iosModule + allModules)
    }
}
```

---

## SQLDelight Platform Drivers

The `SqlDriver` is platform-specific and injected by Koin. Never construct it in `commonMain`.

### Android Driver

```kotlin
// androidMain/core/di/AndroidModule.kt
val androidModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            context = get(),
            name = "budget_calendar.db"
        )
    }
}
```

### iOS Driver

```kotlin
// iosMain/core/di/IosModule.kt
val iosModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            name = "budget_calendar.db"
        )
    }
}
```

---

## expect/actual — Approved Uses

Only use `expect/actual` for stateless utility functions. Keep them in `core/utils/`.

### UUID Generation

```kotlin
// commonMain/core/utils/UuidUtils.kt
expect fun generateUuid(): String

// androidMain/core/utils/UuidUtils.android.kt
actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

// iosMain/core/utils/UuidUtils.ios.kt
actual fun generateUuid(): String = platform.Foundation.NSUUID().UUIDString()
```

### Current Time

```kotlin
// commonMain — use kotlinx-datetime (no expect/actual needed)
import kotlinx.datetime.Clock
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
```

Prefer `kotlinx-datetime` APIs in `commonMain` over `expect/actual` for time. `Clock.System.now()` works on all targets.

---

## Android Context Rule

`Context` must never appear in `commonMain`, domain models, or use cases.

```
// WRONG — Context in a use case
class GetAccountsUseCase(val context: Context, val repo: IAccountRepository)

// CORRECT — Context only in androidMain, injected where the driver is created
// The use case has no idea Android exists
class GetAccountsUseCase(val repo: IAccountRepository)
```

If a feature genuinely needs Android Context (e.g., accessing resources), create an interface in `commonMain` and provide the Android implementation via Koin in `androidModule`.

---

## Compose Multiplatform Notes

- All `@Composable` functions live in `commonMain` — this is shared UI
- `@Preview` annotation is Android-only — use it in `androidMain` preview files only
- `MaterialTheme` from `org.jetbrains.compose.material3` works on both platforms
- `painterResource()`, `stringResource()` from Compose Resources work in `commonMain`
- Never import `androidx.compose.*` directly in `commonMain` — use `org.jetbrains.compose.*`

### Theme Setup

```kotlin
// commonMain/com/petergabriel/budgetcalendar/App.kt
@Composable
fun App() {
    BudgetCalendarTheme {
        // Navigation graph
        NavigationGraph()
    }
}
```

The theme `BudgetCalendarTheme` is defined in `commonMain` using `org.jetbrains.compose.material3`.

---

## Gradle: composeApp Plugin Notes

This project uses the `org.jetbrains.kotlin.multiplatform` plugin with `composeApp` source sets — not the Android application plugin alone.

- Do not add `com.android.application` Gradle tasks that assume single-variant Android
- The iOS framework is embedded via the Xcode project — do not manually configure framework linking
- If iOS build fails to pick up shared code changes, clean derived data in Xcode (`Shift+Cmd+K`)
