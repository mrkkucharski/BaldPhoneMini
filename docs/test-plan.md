

## Scope of Verification

These tests use Espresso instrumentation running on a device or emulator. Each test drives real UI interactions (taps, swipes, text input) and asserts observable view state.

**What is verified:**
- Correct screen is launched when a button is tapped
- Views are visible, hidden, enabled, or disabled as expected
- Text and content descriptions reflect the right state
- Navigation flows complete without a crash
- Permission-denied paths produce a safe fallback rather than an exception

**What is not verified:**
- Visual appearance — icon correctness, color, contrast, font size, layout proportions, or overlap are not checked
- Animation and motion behavior
- Actual audio or haptic output
- Behavior on a physical device with hardware variation

Visual presentation must be reviewed manually by inspecting the app on an emulator or device. Screenshot captures from `BaseScreenshotTakerTest` can assist that review but do not assert correctness automatically.

---

# Functional Test Proposals - April 2026

High-level functional tests for user-facing features added in April 2026.

- Test per-app language selection on Android 13+.
- Test configurable Pills home button.
- Test second home screen personalization.
- Test configurable top bar controls.
- Test speed dial setup from contact details.
- Test speed dial calling from the home screen.
- Test speed dial UI in portrait and landscape.
- Test default home launcher setup and revert flow.
- Test pill reminders with six daily time slots.
- Test pill alarm ringing and automatic stop.
- Test simplified messages inbox.
- Test conversation view.
- Test receiving incoming SMS messages.
- Test sending SMS messages.
- Test unread SMS badge behavior.
- Test SMS behavior when BaldPhone is the default SMS app.
- Test SMS behavior when BaldPhone is not the default SMS app.
- Test hidden home widget option.
- Test page arrows in basic accessibility mode.
- Test frequent contacts section.
- Test contacts screen with and without call log permission.
- Test frequent contacts ordering after call history changes.

# Functional Test Proposals - May 2026

Emulator-targeted functional tests for all major user-facing flows described in the Simple User Manual.
Each entry maps to a screen or capability section in the manual.

## Home Screen — Core Navigation

- Test tapping each native home button (Contacts, Recent calls, Dialer, Camera, WhatsApp, Messages, SOS, Assistant) launches the expected screen.
- Test Lock Screen button on API 28+ locks the device; on older APIs the button opens Apps instead.
- Test replacing a native home button with a custom app via the home screen editor, then tapping it launches the custom app.
- Test restoring a replaced button returns it to the built-in behavior.
- Test hiding a supported optional button removes it from the home screen.
- Test showing a previously hidden button makes it appear on the home screen.
- Test that pinned contacts, pinned apps, and speed dial entries appear on additional home pages (up to 8 items per page).
- Test swiping left and right between home pages navigates correctly.
- Test that adding a ninth shortcut creates a new page.
- Test that removing a shortcut collapses pages correctly without an app restart.

## Top Bar Controls

- Test Battery indicator taps show a status toast/message that disappears automatically.
- Test Flashlight button toggles the flashlight on and off, and the icon reflects the current state.
- Test Flashlight fails gracefully (no crash) when camera/flashlight access is unavailable.
- Test Sound button cycles through normal, vibration, and silent modes.
- Test Notifications bell icon shows the correct state icon for zero, moderate, and many notifications.
- Test Notifications bell in many-notifications state shows the urgent icon.
- Test Notifications bell shows error icon when notification count cannot be read.
- Test tapping the Notifications bell opens the Notifications screen.
- Test Notifications screen lists active notifications.
- Test clearing a single notification removes it from the list.
- Test clearing all notifications empties the list.
- Test opening a notification (when possible) launches the correct app.
- Test simplified top bar mode shows only Battery and Notifications controls.
- Test full top bar mode shows Battery, Flashlight, Sound, and Notifications controls.

## Contacts

- Test Contacts screen opens and shows contact list when contact permission is granted.
- Test Contacts screen requests permission when contact permission is missing.
- Test contact search filters the list to matching results.
- Test clearing the search query restores the full list.
- Test tapping a contact in the list opens Contact Details.
- Test Contact Details shows the selected contact's name and numbers.
- Test calling a number from Contact Details places the call (or opens system dialer if direct-call permission is missing).
- Test Contact Details handles a contact with no numbers without crashing.
- Test adding a contact to the home screen from Contact Details pins a tile.
- Test removing a contact from the home screen from Contact Details removes the tile.
- Test Add Contact button from the Contacts screen opens the add-contact flow.

## Dialer and Calls

- Test entering digits on the keypad updates the number display.
- Test the delete button removes the last digit.
- Test the call button with direct-call permission places the call.
- Test the call button without direct-call permission falls back to the system dialer.
- Test dialing an emergency number uses the safe call path.
- Test the Add Contact button for the entered number opens the add-contact flow.

## Recent Calls

- Test Recent Calls screen opens and shows call history when call log permission is granted.
- Test Recent Calls requests permission when call log permission is missing.
- Test tapping a recent call entry initiates a call or opens a contact action.

## Photos

- Test Photos screen opens and shows image thumbnails in a grid.
- Test tapping a thumbnail opens the single-image viewer.
- Test sharing an image from the viewer invokes the system share sheet.
- Test deleting an image removes it from the grid (when device allows deletion).

## Videos

- Test Videos screen opens and shows video thumbnails in a grid.
- Test tapping a thumbnail opens the simplified video player.
- Test play and pause controls work in the video player.
- Test sharing a video from the player invokes the system share sheet.
- Test deleting a video removes it from the grid (when device allows deletion).

## Internet and Maps

- Test Internet button with one browser app launches that browser directly.
- Test Internet button with multiple browser apps shows the in-app picker.
- Test Internet button with no browser app shows an error message without crashing.
- Test Maps button with one maps app launches that maps app directly.
- Test Maps button with multiple maps apps shows the in-app picker.
- Test Maps button with no maps app shows an error message without crashing.

## Alarms

- Test Alarms screen opens and shows existing alarms.
- Test adding an alarm creates a new entry in the list.
- Test editing an alarm updates the displayed entry.
- Test deleting an alarm removes the entry from the list.
- Test an alarm rings at the configured time and the user can dismiss or snooze it.

## Applications and Home Customization

- Test Apps screen lists installed applications.
- Test tapping an app in the Apps screen launches it.
- Test the home screen editor lists native buttons and allows selecting a replacement app.
- Test hiding an optional button in the editor removes it from the home screen immediately after returning.

## Settings

- Test Settings screen opens and displays all category tiles.
- Test Connectivity category opens device Wi-Fi or system settings without crashing.
- Test Accessibility category opens and shows options (keyboard, accessibility level, page arrows, font size).
- Test Display and Appearance category opens and shows theme and brightness options.
- Test Personalization category opens and shows home-screen and content options.
- Test Set Home Screen category guides the user to the Android default-launcher flow.
- Test Advanced Options opens standard Android system settings without crashing.
- Test Feedback and Support opens the feedback email flow or shows a safe fallback when no email app is available.
- Test About screen shows version number and credits.

## Permissions and Resilience

- Test every major screen that requires a permission shows a graceful fallback (no crash) when that permission is permanently denied.
- Test the app does not crash when started without any runtime permissions granted.
- Test all screens that launch external apps handle the case where the target app is not installed (no crash, optional error message).

---

# What Is Not Easily Testable on a Single Emulator

The items below cannot be covered by standard Espresso instrumentation on one emulator without either dedicated test infrastructure or accepting high brittleness. They require manual review or a purpose-built test environment.

## Hardware and sensor-dependent behavior

- **Flashlight toggle and icon state** — `R.id.flash` can be tapped in a test, but whether the physical flashlight actually turns on/off cannot be asserted. The camera service may also refuse hardware access in an emulator, so the icon state after the tap is indeterminate.
- **Haptic/vibration feedback** — no API surface to assert that a vibration fired.
- **Sound mode cycling** — `AudioManager` state can be read back after tapping `R.id.sound`, but the emulator's audio routing differs from a device and the cycle order depends on the current DnD and Bluetooth state. The assertion is fragile.

## Time-triggered behavior

- **Alarm rings at configured time** — a test would have to set an alarm one minute in the future and then sleep, holding the emulator for at least 60 seconds. This makes the suite impractically slow and the timing is unreliable in CI.
- **Pill reminder alarm ringing and automatic stop** — same issue; six daily slots compound it further.

## Media file dependencies

- **Photos: tap thumbnail → single-image viewer** — the emulator's media store is empty on a clean install. The test must push a JPEG to `MediaStore` via `ContentValues` before launching, which is fiddly and slow.
- **Videos: tap thumbnail → player, play/pause controls** — same. Additionally, asserting that a video is *playing* (not just that the player opened) requires polling a `MediaPlayer` state that is not exposed through the view hierarchy.
- **Share from Photos/Videos viewer** — tapping Share launches a system chooser. Espresso cannot interact reliably with the system chooser UI, and the test can block indefinitely waiting for it to dismiss.
- **Delete image/video** — on API 29+ deletion requires a `MediaStore` write request confirmed by a system dialog. That dialog is outside the app's process and cannot be driven by Espresso without UiAutomator.

## External-app state

- **Internet/Maps with one app** — the emulator has Chrome and Maps pre-installed, so the "one app" path is impossible to reproduce cleanly without uninstalling those, which is destructive and leaves the emulator in an abnormal state for subsequent tests.
- **Internet/Maps with no app** — same problem; uninstalling the only browser/maps app is irreversible within a test run.
- **Internet/Maps with multiple apps showing in-app picker** — requires exactly two or more qualifying apps. Controlling installed-app inventory during a test run is not feasible.
- **WhatsApp and Signal buttons on home screen** — presence depends on whether those packages are installed. The emulator normally has neither, so the buttons are hidden by the app itself; there is nothing to assert.

## SMS infrastructure

- **Receiving an incoming SMS** — requires injecting an SMS PDU via `adb shell`. Doable in a one-off manual test but fragile in CI because the emulator's telephony stack must be running.
- **Sending an SMS** — actually sends a real message if `SEND_SMS` is granted; on an emulator this goes nowhere, but the API call is real. Testing that the sent item appears in the thread requires round-tripping through the telephony stack.
- **Unread SMS badge** — the badge count is driven by a `ContentObserver` on the SMS provider. Seeding the provider with an unread message requires a shell `content insert` command, which works but is brittle across API levels.
- **SMS behavior as default vs. non-default app** — switching and verifying the role mid-test risks the SMS-role revocation crash documented in `AGENTS.md`. Each role switch takes 1–2 seconds and leaves the app in a changed state.

## Home screen editor and customization flows

- **Replace a native button with a custom app** — the flow ends with `AppsActivity` returning a result via `onActivityResult`, which uses a deprecated API path and requires the chosen app to be installed. The replacement only takes effect after returning to the editor and then to the home screen.
- **9th shortcut creates a new page / removing shortcut collapses pages** — both require seeding the BaldHomeWatcher shortcut database, triggering an `updateViewPager()`, and then asserting page counts. The `BaldPagerAdapter` page count is not directly observable from Espresso; it would require accessing the adapter via `runOnMainSync`.
- **Default home launcher setup and revert** — triggers a system dialog (`RoleManager.createRequestRoleIntent`) that is outside the app's process.

## Per-app language selection

- **Android 13+ per-app locale** — requires `LocaleManager.setApplicationLocales()`, which takes effect asynchronously and may restart the activity. Asserting the resulting locale in the UI requires string-resource knowledge of both the original and target language.

## Accessibility edge cases

- **Page arrows in basic accessibility mode** — `BASIC_ACCESSIBILITY_PAGE_ARROWS_KEY=true` changes the ViewPager to non-swipeable and injects arrow buttons. A dedicated test can enable this pref and verify the arrows appear, but the swipe-gesture path (which this mode replaces) must be disabled in the same test, making it impossible to share `BaseActivityTest`'s default pref setup.

---

# Current Test Coverage Report (May 2026)

Coverage is assessed against the 93 test items in the Functional Test Proposals sections above. Each item is rated:
- **Covered** — an automated Espresso test exists that directly verifies the stated behaviour.
- **Partial** — a test exists that opens the relevant screen or exercises a related path, but does not fully assert the item.
- **Not covered** — no automated test exists; manual verification required.

## April 2026 Proposals (21 items)

| Item | Status | Test class |
|---|---|---|
| Per-app language selection on Android 13+ | Not covered | — |
| Configurable Pills home button | Not covered | — |
| Second home screen personalization | Not covered | — |
| Configurable top bar controls | Covered | `TopBarFunctionalTest` |
| Speed dial setup from contact details | Not covered | — |
| Speed dial calling from the home screen | Not covered | — |
| Speed dial UI in portrait and landscape | Partial | `SpeedDialHomeRowTest` (portrait only, no calling) |
| Default home launcher setup and revert flow | Not covered | — |
| Pill reminders with six daily time slots | Partial | `PillsActivityFunctionalTest` (screen opens, time-changer button visible) |
| Pill alarm ringing and automatic stop | Not covered | — |
| Simplified messages inbox | Covered | `MessagesActivityFunctionalTest` |
| Conversation view | Covered | `ConversationActivityFunctionalTest` |
| Receiving incoming SMS messages | Not covered | — |
| Sending SMS messages | Not covered | — |
| Unread SMS badge behavior | Not covered | — |
| SMS behavior when BaldPhone is the default SMS app | Not covered | — |
| SMS behavior when BaldPhone is not the default SMS app | Not covered | — |
| Hidden home widget option | Not covered | — |
| Page arrows in basic accessibility mode | Not covered | — |
| Frequent contacts section | Not covered | — |
| Contacts screen with and without call log permission | Partial | `PermissionsResilienceFunctionalTest` (no-crash only) |
| Frequent contacts ordering after call history changes | Not covered | — |

**April 2026: 3 covered, 4 partial, 14 not covered.**

## May 2026 — Home Screen — Core Navigation (10 items)

| Item | Status | Test class |
|---|---|---|
| Each native home button launches expected screen | Partial | `HomeScreenFunctionalTest` (Dialer, Contacts, Recent, SOS only) |
| Lock Screen button API 28+ vs older | Not covered | — |
| Replace native button with custom app | Not covered | — |
| Restoring replaced button returns to built-in | Not covered | — |
| Hiding optional button removes it from home screen | Covered | `HomeScreenDeepFunctionalTest` |
| Showing previously hidden button makes it appear | Covered | `HomeScreenDeepFunctionalTest` |
| Pinned contacts/apps/speed dial appear on extra pages | Not covered | — |
| Swiping between home pages navigates correctly | Covered | `HomeScreenDeepFunctionalTest` |
| Adding ninth shortcut creates new page | Not covered | — |
| Removing shortcut collapses pages | Not covered | — |

**Home Screen: 3 covered, 1 partial, 6 not covered.**

## May 2026 — Top Bar Controls (14 items)

| Item | Status | Test class |
|---|---|---|
| Battery indicator tap shows status toast | Not covered | — |
| Flashlight button toggles on/off, icon reflects state | Not covered | — |
| Flashlight fails gracefully when unavailable | Not covered | — |
| Sound button cycles through modes | Not covered | — |
| Notifications bell shows correct icon for zero/moderate/many | Not covered | — |
| Notifications bell shows urgent icon for many notifications | Not covered | — |
| Notifications bell shows error icon on count failure | Not covered | — |
| Tapping notifications bell opens Notifications screen | Covered | `TopBarFunctionalTest` |
| Notifications screen lists active notifications | Covered | `NotificationsActivityFunctionalTest` |
| Clearing a single notification removes it | Not covered | — |
| Clearing all notifications empties the list | Partial | `NotificationsActivityFunctionalTest` (button visible, not tapped) |
| Opening a notification launches correct app | Not covered | — |
| Simplified top bar shows only Battery and Notifications | Covered | `TopBarFunctionalTest` |
| Full top bar shows all four controls | Covered | `TopBarFunctionalTest` |

**Top Bar: 4 covered, 1 partial, 9 not covered.**

## May 2026 — Contacts (11 items)

| Item | Status | Test class |
|---|---|---|
| Contacts screen opens with permission granted | Covered | `ContactsActivityFunctionalTest` |
| Contacts screen requests permission when missing | Partial | `PermissionsResilienceFunctionalTest` (no-crash only) |
| Contact search filters the list | Covered | `ContactsActivityFunctionalTest` |
| Clearing search restores full list | Covered | `ContactsDeepFunctionalTest` |
| Tapping a contact opens Contact Details | Not covered | — |
| Contact Details shows name and numbers | Not covered | — |
| Calling from Contact Details places call | Not covered | — |
| Contact Details handles no-numbers contact | Covered | `ContactDetailsActivityFunctionalTest` (invalid-key graceful finish) |
| Adding contact to home screen pins a tile | Not covered | — |
| Removing contact from home screen removes tile | Not covered | — |
| Add Contact button opens add-contact flow | Covered | `ContactsActivityFunctionalTest` |

**Contacts: 5 covered, 2 partial, 4 not covered.**

## May 2026 — Dialer and Calls (6 items)

| Item | Status | Test class |
|---|---|---|
| Entering digits updates number display | Covered | `DialerActivityFunctionalTest` |
| Delete button removes last digit | Covered | `DialerActivityFunctionalTest` |
| Call button with permission places call | Partial | `DialerDeepFunctionalTest` (button enabled, call not placed) |
| Call button without permission falls back to system dialer | Not covered | — |
| Dialing emergency number uses safe call path | Partial | `DialerDeepFunctionalTest` (button enabled, path not verified) |
| Add Contact button opens add-contact flow | Not covered | — |

**Dialer: 2 covered, 3 partial, 1 not covered.**

## May 2026 — Recent Calls (3 items)

| Item | Status | Test class |
|---|---|---|
| Screen opens with call log permission | Covered | `RecentCallsFunctionalTest` |
| Screen requests permission when missing | Partial | `PermissionsResilienceFunctionalTest` (no-crash only) |
| Tapping entry initiates call or opens contact action | Not covered | — |

**Recent Calls: 1 covered, 2 partial, 0 not covered.**

## May 2026 — Photos (4 items)

| Item | Status | Test class |
|---|---|---|
| Screen opens and shows image grid | Covered | `PhotosActivityFunctionalTest` |
| Tapping thumbnail opens single-image viewer | Not covered | — |
| Sharing image invokes system share sheet | Not covered | — |
| Deleting image removes it from grid | Not covered | — |

**Photos: 1 covered, 0 partial, 3 not covered.**

## May 2026 — Videos (5 items)

| Item | Status | Test class |
|---|---|---|
| Screen opens and shows video grid | Covered | `VideosActivityFunctionalTest` |
| Tapping thumbnail opens video player | Not covered | — |
| Play and pause controls work | Not covered | — |
| Sharing video invokes share sheet | Not covered | — |
| Deleting video removes it from grid | Not covered | — |

**Videos: 1 covered, 0 partial, 4 not covered.**

## May 2026 — Internet and Maps (6 items)

| Item | Status | Test class |
|---|---|---|
| Internet with one browser launches it directly | Not covered | — |
| Internet with multiple browsers shows picker | Not covered | — |
| Internet with no browser shows error, no crash | Not covered | — |
| Maps with one maps app launches it directly | Not covered | — |
| Maps with multiple maps apps shows picker | Not covered | — |
| Maps with no maps app shows error, no crash | Not covered | — |

**Internet and Maps: 0 covered, 0 partial, 6 not covered.**

## May 2026 — Alarms (5 items)

| Item | Status | Test class |
|---|---|---|
| Screen opens and shows existing alarms | Covered | `AlarmsActivityFunctionalTest` |
| Adding an alarm creates new entry | Partial | `AlarmsDeepFunctionalTest` (form fields verified, list mutation not asserted) |
| Editing an alarm updates the displayed entry | Not covered | — |
| Deleting an alarm removes the entry | Not covered | — |
| Alarm rings at configured time, dismiss/snooze | Not covered | — |

**Alarms: 1 covered, 1 partial, 3 not covered.**

## May 2026 — Applications and Home Customization (4 items)

| Item | Status | Test class |
|---|---|---|
| Apps screen lists installed applications | Covered | `AppsActivityFunctionalTest` |
| Tapping an app in Apps screen launches it | Not covered | — |
| Home screen editor lists buttons, allows replacement | Not covered | — |
| Hiding button in editor removes it from home screen | Not covered | — |

**Applications: 1 covered, 0 partial, 3 not covered.**

## May 2026 — Settings (9 items)

| Item | Status | Test class |
|---|---|---|
| Settings screen opens and displays category tiles | Covered | `SettingsActivityFunctionalTest` |
| Connectivity category opens Wi-Fi/system settings | Not covered | — |
| Accessibility category opens and shows options | Not covered | — |
| Display and Appearance category opens | Not covered | — |
| Personalization category opens | Not covered | — |
| Set Home Screen guides to Android launcher flow | Not covered | — |
| Advanced Options opens system settings | Not covered | — |
| Feedback and Support opens email flow or fallback | Not covered | — |
| About screen shows version and credits | Covered | `AboutActivityFunctionalTest` |

**Settings: 2 covered, 0 partial, 7 not covered.**

## May 2026 — Permissions and Resilience (3 items)

| Item | Status | Test class |
|---|---|---|
| Major screens show graceful fallback with permission denied | Partial | `PermissionsResilienceFunctionalTest` (Contacts + Recent Calls only) |
| App does not crash started without any runtime permissions | Covered | `PermissionsResilienceFunctionalTest` |
| Screens launching external apps handle missing app | Not covered | — |

**Permissions and Resilience: 1 covered, 1 partial, 1 not covered.**

---

## Overall Summary

| Category | Items | Covered | Partial | Not covered |
|---|---|---|---|---|
| April 2026 | 21 | 3 | 4 | 14 |
| Home Screen | 10 | 3 | 1 | 6 |
| Top Bar | 14 | 4 | 1 | 9 |
| Contacts | 11 | 5 | 2 | 4 |
| Dialer | 6 | 2 | 3 | 1 |
| Recent Calls | 3 | 1 | 2 | 0 |
| Photos | 4 | 1 | 0 | 3 |
| Videos | 5 | 1 | 0 | 4 |
| Internet and Maps | 6 | 0 | 0 | 6 |
| Alarms | 5 | 1 | 1 | 3 |
| Applications | 4 | 1 | 0 | 3 |
| Settings | 9 | 2 | 0 | 7 |
| Permissions and Resilience | 3 | 1 | 1 | 1 |
| **Total** | **101** | **25** | **15** | **61** |

**Automated coverage: 25/101 fully covered (25%), 15/101 partial (15%), 61/101 not covered (60%).**

The 61 uncovered items fall into three buckets: hardware/timing/media gaps (flashlight, alarm ring, video playback, share sheets — 19 items), external-app state gaps (Internet/Maps app inventory, WhatsApp, SMS infrastructure — 16 items), and in-app flows that require DB seeding, multi-activity chains, or system dialogs that block Espresso (home screen editor, alarm CRUD, contact-to-home-screen pinning — 26 items). The partial items represent screens that open correctly but whose deeper state mutations (delete, edit, list count changes) are not yet asserted.
