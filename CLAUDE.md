# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ComposeDemoHub** is a multi-module Android application showcasing Jetpack Compose implementations. It contains three modules demonstrating different use cases with Material 3 design and modern Android architecture.

### Modules

1. **app** - Basic "Hello World" demo module
2. **fitdemo** - Garmin FIT file parser with GPS visualization using Mapbox
3. **rundemo** - User authentication system (login/register) with network integration

## Build and Development Commands

### Build Commands

```bash
# Build specific module
./gradlew :fitdemo:build
./gradlew :rundemo:build
./gradlew :app:build

# Build all modules
./gradlew build

# Install to device (debug)
./gradlew :fitdemo:installDebug
./gradlew :rundemo:installDebug

# Clean build
./gradlew clean
```

### Running Applications

```bash
# Launch fitdemo
adb shell am start -n com.oterman.fitdemo/.MainActivity

# Launch rundemo
adb shell am start -n com.oterman.rundemo/.MainActivity
```

### Testing

```bash
# Run unit tests for a module
./gradlew :rundemo:test

# Run instrumented tests
./gradlew :rundemo:connectedAndroidTest

# Run all tests
./gradlew test
```

## Architecture Patterns

### rundemo Module (Clean Architecture)

The rundemo module follows **Clean Architecture** with clear layer separation:

```
rundemo/
├── data/
│   ├── local/              # DataStore, local storage
│   ├── network/
│   │   ├── api/            # Retrofit API interfaces
│   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── request/    # API request models
│   │   │   └── response/   # API response models
│   │   └── interceptor/    # OkHttp interceptors
│   └── repository/         # Repository implementations
├── domain/
│   └── model/              # Domain models (business logic)
├── presentation/
│   ├── components/         # Reusable Compose components
│   ├── feature/            # Feature-based screens
│   │   ├── auth/login/     # LoginScreen + LoginViewModel
│   │   └── welcome/        # WelcomeScreen
│   └── navigation/         # NavGraph and Screen definitions
└── util/                   # Utilities
```

**Key Architecture Principles:**
- **Feature-based organization**: Each feature (login, register) is self-contained
- **MVVM pattern**: ViewModels manage UI state, Screens observe state via StateFlow
- **Single source of truth**: ViewModels hold UI state; Screens are stateless
- **Unidirectional data flow**: UI events → ViewModel → State updates → UI recomposition

### fitdemo Module (MVVM)

```
fitdemo/
├── data/
│   ├── model/              # Data models (FitSummaryData, UiState)
│   └── repository/         # FIT file parsing repository
├── ui/
│   ├── components/         # Reusable UI components (InfoCard, InfoRow)
│   ├── screen/             # Screens (FitFileScreen, MapScreen)
│   └── theme/              # Material 3 theme configuration
├── viewmodel/              # ViewModels
└── util/                   # Utilities (MapPreferences, FormatUtils)
```

## Technology Stack

### Core Dependencies

- **Compose BOM**: 2024.09.00 (Material 3)
- **Kotlin**: 2.0.21
- **AGP**: 8.13.2
- **Min SDK**: 33 (Android 13+)
- **Target SDK**: 36

### Key Libraries

**rundemo specific:**
- Retrofit 2.9.0 + OkHttp 4.12.0 (networking)
- Navigation Compose 2.7.6
- DataStore 1.0.0 (preferences)
- Material Icons Extended 1.6.0

**fitdemo specific:**
- Garmin FIT SDK 21.171.0 (FIT file parsing)
- Mapbox Maps SDK (GPS visualization)

**Common:**
- Kotlin Coroutines + Flow (async/state management)
- Lifecycle ViewModel Compose
- Material 3

## Compose Best Practices (Applied in This Project)

1. **State hoisting**: State is managed in ViewModels, passed down to composables
2. **Reusable components**: Common UI elements extracted (GradientButton, PasswordTextField, ShakeBox)
3. **LazyColumn for scrollable content**: Used with `nestedScroll` for collapsing toolbars
4. **Material 3 theming**: Consistent use of `MaterialTheme.colorScheme`
5. **Preview annotations**: `@Preview` for development
6. **Modifier chaining**: Proper order (size → padding → behavior)

## Important Implementation Details

### Collapsing Toolbar Pattern (LoginScreen)

The LoginScreen implements Material 3's collapsing toolbar pattern:

```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
    rememberTopAppBarState()
)

Scaffold(
    topBar = {
        LargeTopAppBar(
            title = { Text("账号登录") },
            scrollBehavior = scrollBehavior
        )
    }
) { paddingValues ->
    LazyColumn(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        // Content in items...
    }
}
```

**Key points:**
- Use `LargeTopAppBar` or `MediumTopAppBar` for collapsing effect
- Connect scroll behavior with `.nestedScroll()`
- Use `LazyColumn` instead of `Column + verticalScroll` for proper integration
- `Spacer.weight()` doesn't work in LazyColumn; use fixed height instead

### Navigation Setup

Navigation uses Jetpack Navigation Compose with sealed class routes:

```kotlin
// Define routes
sealed class Screen(val route: String)

// Setup NavGraph
NavHost(navController, startDestination = Screen.Welcome.route) {
    composable(Screen.Login.route) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
        )
    }
}
```

### Custom Reusable Components

Located in `presentation/components/`:
- **GradientButton**: Button with gradient background and loading state
- **PasswordTextField**: TextField with show/hide password toggle
- **ShakeBox**: Wrapper that applies shake animation (for validation errors)
- **SimpleTermsCheckbox**: Checkbox with clickable terms text

## iOS Reference Code

The `zrun/` directory contains iOS reference implementations (SwiftUI) for comparison. When implementing Android features that have iOS counterparts:

1. Review iOS implementation in `zrun/demo_ios/DemoNav/`
2. Adapt UI/UX patterns to Android/Compose conventions
3. Maintain feature parity while following Android best practices

**Example**: The PRD mentions `PhoneRegisterView` (iOS) as reference for implementing Android registration.

## Project Configuration

### Maven Repositories

The project uses custom internal Maven repositories (configured in `settings.gradle.kts`):
- OPPO internal Maven (requires credentials in `gradle.properties`)
- Internal snapshots/releases at `maven.scm.adc.com`

### Mapbox Token

Mapbox access token is configured in `settings.gradle.kts`:
- Can be overridden via environment variable: `MAPBOX_DOWNLOADS_TOKEN`
- Default token is embedded (for development only)

### Gradle Properties

Key properties in `gradle.properties`:
- `sonatypeUsername` / `sonatypePassword`: Maven credentials
- `prop_oppoMavenUrlRelease`: Internal Maven URL

## Development Workflow

### Adding a New Feature (rundemo)

1. Create feature package: `presentation/feature/{feature_name}/`
2. Add Screen composable: `{Feature}Screen.kt`
3. Add ViewModel: `{Feature}ViewModel.kt`
4. Add route to `Screen.kt` sealed class
5. Register in `NavGraph.kt`
6. Add network DTOs if needed: `data/network/dto/`
7. Add API endpoints: `data/network/api/`
8. Create repository: `data/repository/`

### Adding a Reusable Component

1. Create in `presentation/components/{Component}.kt`
2. Make composable stateless (hoist state to caller)
3. Add `@Preview` for development
4. Document parameters with KDoc

### Working with Collapsing Toolbars

When converting a screen to use collapsing toolbar:
1. Replace `TopAppBar` → `LargeTopAppBar`
2. Add `scrollBehavior` parameter
3. Change `Column + verticalScroll` → `LazyColumn`
4. Wrap each child in `item {}`
5. Add `.nestedScroll(scrollBehavior.nestedScrollConnection)` to LazyColumn
6. Replace `Spacer(Modifier.weight())` with fixed height

## Module-Specific Notes

### fitdemo

- Uses **Storage Access Framework** (no storage permissions needed)
- FIT file parsing runs on `Dispatchers.IO`
- Mapbox maps require ACCESS_FINE_LOCATION permission
- Map styles stored in SharedPreferences (MapPreferences.kt)

### rundemo

- Uses **Retrofit** for API calls
- Authentication state managed in ViewModel
- Login failure tracking with attempt limits
- DataStore for token persistence (planned)
