# Technology Stack & Architecture Specification (`tech-stack.md`)

This document defines the technical architecture, framework standards, dependencies, and project structure for **Kitchen Twenty2**. All code generated or modified by developers and AI tools (including Google Antigravity) must adhere to these technical choices.

---

## 1. Core Technology Stack

* **Platform:** Pure Native Android
* **Programming Language:** Kotlin (100% Kotlin-first, explicit non-null safety, Coroutines & Flow)
* **UI Framework:** Jetpack Compose (Declarative UI, no XML layouts)
* **Local Database:** Google Room ORM (SQLite-backed persistence layer)
* **Architecture Pattern:** MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
* **Dependency Injection:** Hilt (Dagger-backed for Android)
* **Asynchronous Execution:** Kotlin Coroutines + Reactive Flow (`StateFlow`, `SharedFlow`)

---

## 2. Recommended Project Directory Structure

```text
com.kitchentwenty2/
├── data/
│   ├── local/
│   │   ├── dao/             # Room Data Access Objects (OrderDao, CustomerDao, etc.)
│   │   ├── entity/          # Room Entity models matching database.md
│   │   ├── relation/        # Composite database relationship objects (OrderWithDetails)
│   │   └── KitchenDatabase.kt # Main RoomDatabase definition class
│   └── repository/          # Concrete implementation of repository layer
├── domain/
│   ├── model/               # Pure UI/Domain data models
│   └── repository/          # Repository interface definitions
├── ui/
│   ├── components/          # Reusable Compose UI elements (Buttons, Input fields, Cards)
│   ├── theme/               # Color, Typography, Shape, and Theme setups
│   └── screens/
│       ├── order/           # Order creation & detail screens (Screen + ViewModel)
│       ├── customer/        # Customer management screens
│       ├── menu/            # Menu items management screens
│       ├── expense/         # Expense tracking screens
│       └── logs/            # App error log viewer (for technical analysis)
├── di/                      # Hilt Dependency Injection Modules (DatabaseModule, RepositoryModule)
└── util/                    # Helper functions, extensions, and error handling abstractions
```

## 3. Core Dependencies Configuration (`build.gradle.kts` - App Level)
The following dependencies form the base runtime environment for the application:

```plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp) // Kotlin Symbol Processing for Room & Hilt
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.kitchentwenty2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kitchentwenty2"
        minSdk = 26 // Android 8.0 (Oreo) and above
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Aligned with Jetpack Compose version
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose UI Framework
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Material Design 3 Components
    implementation(libs.androidx.navigation.compose) // Compose Navigation

    // Room Database (SQLite ORM)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Coroutine & Flow integration for Room
    ksp(libs.androidx.room.compiler)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Asynchronous Flow / Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

## 4. Architectural Guidelines & Best Practices

### 1. Unidirectional Data Flow (UDF):
- ViewModels manage state using `StateFlow<UiState>`.
- UI (Compose screens) reads from `StateFlow` and emits user events to the ViewModel.
- UI components should remain state-less wherever possible.

### 2. Database Operations & Asynchrony:
- Direct database calls must never run on the main UI thread.
- All Room DAO operations must either be suspend functions or return `Flow<T>`.
- Repository classes are responsible for switching execution context using `Dispatchers.IO`.

### 3. Error Logging & Exception Handling:
- Global and local exceptions caught in ViewModels or DAOs should be dispatched to `AppErrorLogDao` via an error logger utility to populate `AppErrorLogEntity`.