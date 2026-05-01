# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease        # requires app/release-key.jks + app/keystore.properties

# Install to connected device
./gradlew installDebug

# Test
./gradlew connectedAndroidTest   # instrumentation tests (requires device/emulator)
./gradlew test                   # unit tests

# Lint
./gradlew lint

# Clean
./gradlew clean
```

**Environment:** Java 17, Gradle 8.9.2, Kotlin 2.1.20, AGP 8.9.2. Min SDK 21, compile SDK 34.

## Architecture Overview

BaldPhoneMini is an accessibility-focused Android launcher/phone app for elderly users. The codebase is in **active migration** from legacy Java to modern Kotlin MVVM.

### Dual Package Structure

**Modern code** (`app/baldphone/neo/`) — Kotlin, MVVM, Coroutines, StateFlow:
- `activities/` — Kotlin activities (Dialer, Contacts, Feedback, About)
- `viewmodels/` — ViewModels with StateFlow state management
- `contacts/` — Contact domain: `Contact`, `ContactRepository`, `ContactRepositoryImpl`, `ContactActionsUseCase`, `ContactSearcher`
- `calls/` — `CallManager`, `CallsRepository`
- `permissions/` — `PermissionManager` (Fragment-based), `RuntimePermission`, `AppPermission`
- `utils/` — `PhoneNumberUtils`, `ContextUtils`, `AppDialog`, messaging helpers

**Legacy code** (`com.bald.uriah.baldphone/`) — Java, Activity-based patterns:
- `BaldPhone.java` — Application class
- `databases/` — Room DB for Alarms, Apps, Reminders, Calls (DAOs + Entities)
- `activities/` — Base activities (`BaldActivity`, `HomeScreenActivity`, `SOSActivity`, etc.)
- `keyboard/` — `BaldInputMethodService` (custom simplified IME)
- `broadcast_receivers/`, `services/`, `adapters/`

### Key Patterns

- **No DI framework** — manual injection; `ContactRepositoryImpl` uses `companion object getInstance()` singleton
- **State:** modern code uses `StateFlow` + `viewModelScope`; legacy uses LiveData or direct UI updates
- **UI:** XML layouts + ViewBinding throughout (no Jetpack Compose)
- **Threading:** `withContext(Dispatchers.IO)` for data ops, `Mutex` for thread safety in repositories
- **Permissions:** custom `PermissionManager` wraps Fragment result API; always check permissions before accessing contacts/calls/SMS

### Data Layer

- **Room DB** (legacy): Alarms, Reminders, Calls, Apps tables
- **ContentProvider** (modern): system Contacts queried via `ContactRepositoryImpl`
- **Prefs** (`app/baldphone/neo/data/`): SharedPreferences wrapper with `PrefKeys`

### Key Libraries

- Kotlin Coroutines, Room 2.7.2 (KSP), Glide 5.0.5, Coil 3.0.4
- libphonenumber 9.0.28, Joda-Time (android.joda wrapper), Material Design 3

## Development Notes

- **When adding new features**, follow the modern Kotlin MVVM pattern under `app/baldphone/neo/`, not the legacy Java pattern.
- **UI must be accessibility-first**: large touch targets, high contrast, minimal complexity — the target user is elderly or accessibility-challenged.
- **Screenshot/instrumentation tests** live in `app/src/androidTest/`; no significant unit test suite exists.
- The `app/src/androidTest/` tests use Espresso + `BaseScreenshotTakerTest` for UI regression testing.

## Error-handling Philosophy

**Priority: keep the app running.** Prefer silently skipping an operation over throwing an uncaught exception. The target user is elderly and may not understand or recover from a crash, so resilience beats fail-fast.

- **Do not use `requireContext()` / `requireActivity()` / `!!`** to assert non-null in places where lifecycle, system services, or platform APIs may legitimately return null. Capture the value in a local, null-check it, and early-return / no-op instead.
- **Wrap risky platform calls** (e.g. `startActivity` for optional intents, content-resolver queries, system-service lookups) in try/catch where exceptions would otherwise bubble to the UI thread. Log and continue.
- **A skipped UI side effect is acceptable**; an uncaught `NullPointerException` / `IllegalStateException` is not.
- This applies to both legacy Java and modern Kotlin code.
