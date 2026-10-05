# Mind-Ur-Set

Mind-Ur-Set is an offline-first Android app for the moment a thought starts repeating
itself. It offers a fifteen-second emotion check-in, a short guided reframe, and one small
thing to do next.

This repository holds the native Android client, meaning the Kotlin and Jetpack Compose
application, its local Room database, its unit tests, and its build configuration. The app
works with no network connection and keeps everything on the device.

## What is in this repository

| Area   | Contents                                                                                                                      |
| ------ | ----------------------------------------------------------------------------------------------------------------------------- |
| UI     | Jetpack Compose with Material 3, Navigation Compose, a floating pill navigation bar and a modal drawer                        |
| State  | One ViewModel per screen exposing a `UiState`, with shared loading, empty and error components                                |
| Data   | Room database (`user_account`, `user_profile`, `check_in_log`, `journal_entry`) and DataStore for the session and preferences |
| Domain | Repository interfaces and implementations, credential validation, PBKDF2 password hashing                                     |
| DI     | Manual dependency container in `di/AppContainer.kt`, provided through a `CompositionLocal`                                    |
| Tests  | JVM unit tests for password hashing and credential validation                                                                 |

## Module and package structure

The single Gradle module is `:app`, with `applicationId com.mindurset.app`.

```text
app/
  build.gradle.kts                       the android app module, namespace com.mindurset.app
  src/main/AndroidManifest.xml
  src/main/java/com/mindurset/app/
    MindUrSetApp.kt                      Application class, owns the AppContainer
    MainActivity.kt                      single activity, resolves the start destination from the session
    di/AppContainer.kt                   manual dependency graph and the LocalAppContainer CompositionLocal
    data/
      local/                             Room database, entities and daos
      prefs/DataStorePreferences.kt      session and preference storage
      repository/                        Auth, Journal, CheckIn and Profile repository implementations
    domain/
      model/                             Emotion, CheckIn, JournalEntry, UserAccount, UserProfile
      auth/                              PasswordHasher (PBKDF2-HMAC-SHA256) and ValidateCredentials
      repository/Repositories.kt         repository interfaces and the session store
    ui/
      auth/ dashboard/ home/checkin/ reflection/ profile/ companion/ therapy/ info/
                                         one screen and one ViewModel per feature
      menu/AppDrawer.kt                  drawer with every destination
      navigation/                        Routes.kt and MindUrSetNavigation.kt, the NavHost and the bottom bar
      theme/                             Color.kt design tokens and Theme.kt Material 3 colour scheme
      common/UiCommon.kt                 LoadingState, EmptyState and ErrorState
  src/test/java/com/mindurset/app/       PasswordHasherTest and ValidateCredentialsTest
.github/workflows/android.yml            continuous integration
```

## Prerequisites

- **JDK 21 or newer.** The build pins Android Studio's bundled JBR in `gradle.properties`
  through `org.gradle.java.home`. On a machine without Android Studio, add
  `-Dorg.gradle.java.home=/path/to/jdk21` to a `./gradlew` call.
- **Android SDK platform 37** (`compileSdk = 37`), plus build-tools and platform-tools.
  Point the build at it with `local.properties` (`sdk.dir=...`) or `ANDROID_HOME`.
- **A device or emulator on Android 8.0 (API 26) or newer** to install and run the app.
- The Gradle wrapper is committed, so no separate Gradle install is needed.

Toolchain for reference: AGP 9.4.1 with built-in Kotlin (there is no
`org.jetbrains.kotlin.android` plugin applied, so do not add one), Kotlin and Compose
plugin 2.4.20, KSP 2.3.12, Compose BOM 2026.09.00, Room 2.8.5, DataStore 1.2.1.

## Build, test, install

Run everything from the repository root.

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Run the JVM unit tests
./gradlew :app:testDebugUnitTest

# Build and test in one go, which is what continuous integration runs
./gradlew :app:assembleDebug :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug

# Install onto a connected device or emulator
./gradlew :app:installDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To sideload it
directly:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## License

The source code is released under the Apache License 2.0. The full text is in `LICENSE`.
