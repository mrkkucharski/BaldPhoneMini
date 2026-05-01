# Android Instrumentation Tests

Lessons learned from adding and running emulator tests:

- The app may prompt to become the default SMS app after reinstall. This blocks HomeScreen-based tests because `HomeScreenActivity.onResume()` calls `SmsDefaultAppSyncer.sync(this)`.
- Before launching activities in instrumentation tests, configure the emulator role directly:
  - Enable required SMS role components first: `SmsReceiver`, `WapPushReceiver`, and `RespondViaMessageService`.
  - On Android Q+ use shell commands through `InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(...)`:
    - `cmd role set-bypassing-role-qualification true`
    - `cmd role add-role-holder android.app.role.SMS app.baldphone.neo 0`
    - Verify with `cmd role get-role-holders android.app.role.SMS`.
  - The trailing `0` flag on `add-role-holder` mattered on the API 34 emulator; without it the command failed.
- Shared setup should live in a small androidTest helper and be called from both:
  - `com.bald.uriah.baldphone.activities.BaseActivityTest`
  - `com.bald.uriah.baldphone.screenshots.BaseScreenshotTakerTest`
- Use `TestDeviceSetup.ensureReady()` for fresh-install emulator access setup. Do not add scattered `GrantPermissionRule` fields unless a test is explicitly verifying permission-denied behavior.
- `TestDeviceSetup.ensureReady()` is responsible for:
  - granting dangerous runtime permissions used by the app/tests (`CALL_PHONE`, contacts, call log, phone state, camera, SMS, storage);
  - allowing special app-ops where shell access is needed (`SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS`, `SCHEDULE_EXACT_ALARM`);
  - enabling notification listener access by appending `com.bald.uriah.baldphone.services.NotificationListenerService` to `settings get secure enabled_notification_listeners`;
  - making BaldPhoneMini the default SMS app.
- A fresh install / wiped app data defaults to medium accessibility. In this mode Bald button views require a longer press; a normal Espresso `click()` only shows the "press longer" warning and does not run the button action.
- Functional tests that use normal Espresso clicks must seed prefs before launch:
  - `BPrefs.TEST_KEY=true`
  - `BPrefs.LONG_PRESSES_KEY=false`
  - `BPrefs.LONG_PRESSES_SHORTER_KEY=false`
  - `BPrefs.TOUCH_NOT_HARD_KEY=true`
  - `BPrefs.BASIC_ACCESSIBILITY_PAGE_ARROWS_KEY=false`
- If a test intentionally covers medium accessibility behavior, use `longClick()` and keep it separate from normal navigation tests.
- Do not set `BPrefs.CUSTOM_MESSAGES_KEY` to a custom/non-null value in generic HomeScreen tests. `SmsDefaultAppSyncer` treats that as "not using the native messages panel", disables the SMS role components on `HomeScreenActivity.onResume()`, Android revokes the SMS role, and the resulting package/component churn can crash the instrumentation process.

Execution notes:

- Avoid debugging by running the entire `connectedDebugAndroidTest` suite first. A single failed UI test can leave the app/runner in a bad state, causing many later tests to fail with 45-second `MonitoringInstrumentation.startActivitySync` timeouts.
- Prefer running one class at a time while developing:
  - `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bald.uriah.baldphone.activities.DialerActivityFunctionalTest`
- If a run gets stuck or starts cascading timeouts, force-stop the target app before retrying:
  - `/Users/marekkucharski/Library/Android/sdk/platform-tools/adb shell am force-stop app.baldphone.neo`
- `./gradlew assembleDebugAndroidTest` is fast and useful for catching compile/import mistakes before emulator runs.

Stable test patterns found so far:

- `DialerActivityFunctionalTest` passed individually on the API 34 emulator after asserting digits independent of locale/as-you-type formatting.
- `ContactsActivityFunctionalTest` passed individually after waiting for the async contacts refresh before asserting empty-state visibility.
- `AboutActivityFunctionalTest` passed individually when asserting in-process UI and dialog behavior.

Brittle patterns to avoid:

- Do not assert exact dialer text for partial numbers like `123`; libphonenumber may format as the user types depending on region. Compare normalized digits instead.
- Do not assume Contacts empty state appears immediately after typing. The repository refresh/search flow is async; wait or poll boundedly.
- Avoid instrumentation tests that launch external chooser/system flows unless they are explicitly intercepted and verified. A feedback email chooser monitor caused activity-launch idle timeouts on the emulator.
- ActivityMonitor-based assertions from `HomeScreenActivity` were unreliable in early attempts. The click occurred, but the monitor did not consistently observe the launched activity; prefer direct activity tests or less chained navigation unless the monitor behavior is proven stable.
- When HomeScreen button clicks appear to do nothing, check accessibility prefs first. The most common cause is `LONG_PRESSES_KEY=true`, not a broken click target.
- When an instrumentation run crashes immediately after a HomeScreen test starts, check logcat for `Removing package that no longer qualifies for the role, package: app.baldphone.neo, role: android.app.role.SMS`. In this repo that usually means the test changed messages prefs or SMS components during the run.
