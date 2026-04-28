# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

**ComposeDemoHub** is a multi-module Android application showcasing Jetpack Compose with Material 3 design.

| Module | Description | Package |
|--------|-------------|---------|
| **app** | Basic demo | `com.oterman.app` |
| **fitdemo** | Garmin FIT file parser with Mapbox GPS visualization | `com.oterman.fitdemo` |
| **rundemo** | User authentication system (login/register/forgot password) | `com.oterman.rundemo` |

## Build Commands

```bash
# Build & install specific module
./gradlew :rundemo:installDebug
./gradlew :fitdemo:installDebug

# Run tests
./gradlew :rundemo:test                    # Unit tests
./gradlew :rundemo:connectedAndroidTest    # Instrumented tests

# Launch via adb
adb shell am start -n com.oterman.rundemo/.MainActivity
adb shell am start -n com.oterman.fitdemo/.MainActivity
```

## Architecture

### rundemo Module (Clean Architecture + MVVM)

```
rundemo/
├── data/
│   ├── local/                  # DataStore (PreferencesManager)
│   ├── network/
│   │   ├── api/                # Retrofit interfaces (UserApi)
│   │   ├── dto/request/        # API request models
│   │   ├── dto/response/       # API response models
│   │   └── interceptor/        # OkHttp interceptors
│   └── repository/             # UserRepository
├── domain/model/               # Domain models (UserInfo)
├── presentation/
│   ├── components/             # Reusable UI (LoadingButton, PasswordTextField, ShakeAnimation)
│   ├── feature/
│   │   ├── auth/
│   │   │   ├── login/          # LoginScreen + LoginViewModel + LoginUiState
│   │   │   ├── register/       # Multi-step registration flow
│   │   │   │   └── steps/      # PhoneInputStep, VerificationStep, PasswordStep
│   │   │   └── forgotpassword/ # Password reset flow
│   │   │       └── steps/      # PhoneStep, VerificationStep, NewPasswordStep
│   │   ├── home/               # HomeScreen with bottom tabs
│   │   │   └── tabs/           # HomeTab, DataTab, ProfileTab
│   │   └── welcome/            # WelcomeScreen (entry point)
│   └── navigation/             # NavGraph, Screen sealed class
└── util/                       # ValidationUtils, SecurityUtils, Logger
```

**Pattern**: Each feature has `{Feature}Screen.kt`, `{Feature}ViewModel.kt`, `{Feature}UiState.kt`, and `{Feature}ViewModelFactory.kt`.

### fitdemo Module (MVVM)

```
fitdemo/
├── data/model/                 # FitSummaryData, UiState
├── data/repository/            # FIT file parsing
├── ui/screen/                  # FitFileScreen, MapScreen
├── viewmodel/                  # FitFileViewModel
└── util/                       # MapPreferences, FormatUtils
```

## Key Implementation Patterns

### Collapsing Toolbar with LazyColumn

```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

Scaffold(topBar = { LargeTopAppBar(scrollBehavior = scrollBehavior) }) {
    LazyColumn(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
        item { /* content */ }
    }
}
```

**Important**: Use `LazyColumn` (not `Column + verticalScroll`). `Spacer.weight()` doesn't work in LazyColumn—use fixed heights.

### Navigation

Routes defined in `presentation/navigation/Screen.kt` as sealed class. NavGraph in `NavGraph.kt`.

```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Welcome.route) { inclusive = true }
}
```

### Multi-Step Flows (Register, Forgot Password)

State machine pattern in ViewModel with step enum. Each step is a separate composable in `steps/` subdirectory. ViewModel exposes `currentStep` via StateFlow.

## Configuration

### Maven Repositories

Internal OPPO Maven configured in `settings.gradle.kts`. Credentials in `gradle.properties`:
- `sonatypeUsername` / `sonatypePassword`
- `prop_oppoMavenUrlRelease`

### Mapbox

Token in `settings.gradle.kts`. Override via `MAPBOX_DOWNLOADS_TOKEN` env variable.

## iOS Reference

`zrun/demo_ios/DemoNav/` contains SwiftUI reference implementations for feature parity.

## Module-Specific Notes

**fitdemo**: Uses Storage Access Framework (no permissions). FIT parsing on `Dispatchers.IO`. Mapbox requires `ACCESS_FINE_LOCATION`.

**rundemo**: Retrofit for networking. Login has failure tracking with attempt limits. DataStore for persistence.
