# SMS Default App Sync Design

## Problem

BaldPhoneMini can be set as the default SMS app, which is required for:
- Writing incoming SMS to the system provider (`Telephony.Sms.Inbox`)
- Marking messages as read
- Showing the unread badge on the home screen

However, the home screen is customizable. The user can replace the native Messages
("Wiadomości") button with any other app. When they do, BaldPhoneMini should
**relinquish** the default SMS role. When the native button is restored, it should
**re-acquire** the role.

## Signal: how to detect native vs custom panel

`BPrefs.CUSTOM_MESSAGES_KEY` in SharedPreferences:
- `null` → native Messages panel active → **should be default SMS app**
- non-null ComponentName string → custom app → **should NOT be default SMS app**

## Trigger point

`HomeScreenActivity.onResume()` already runs after every customization change.
Insert a single call to `SmsDefaultAppSyncer.sync(activity)` there.

## Component: `SmsDefaultAppSyncer`

Location: `app/baldphone/neo/sms/SmsDefaultAppSyncer.kt`

```
fun sync(activity: Activity):
    isNativePanel = BPrefs.CUSTOM_MESSAGES_KEY pref is null/missing
    isCurrentDefault = Telephony.Sms.getDefaultSmsPackage() == packageName

    if isNativePanel && !isCurrentDefault:
        re-enable SMS components (in case they were disabled)
        launch RoleManager.createRequestRoleIntent(ROLE_SMS)   ← system dialog, user approves

    if !isNativePanel && isCurrentDefault:
        disable SmsReceiver, WapPushReceiver, RespondViaMessageService via PackageManager
        (Android revokes the SMS role automatically when required components are disabled)
```

## SMS components to enable/disable

| Component | Why required by SMS role |
|---|---|
| `SmsReceiver` | Must receive `SMS_DELIVER` |
| `WapPushReceiver` | Must receive `WAP_PUSH_DELIVER` |
| `RespondViaMessageService` | Must handle `RESPOND_VIA_MESSAGE` |

Disabled via `PackageManager.setComponentEnabledSetting(COMPONENT_ENABLED_STATE_DISABLED)`.
Re-enabled with `COMPONENT_ENABLED_STATE_ENABLED` before requesting the role.

## Files to touch

| File | Change |
|---|---|
| `app/baldphone/neo/sms/SmsDefaultAppSyncer.kt` | New — all sync logic |
| `com/bald/uriah/baldphone/activities/HomeScreenActivity.java` | Add `SmsDefaultAppSyncer.sync(this)` in `onResume()` |

## Edge cases

| Scenario | Outcome |
|---|---|
| App first installed, Messages panel visible | `onResume()` requests role |
| User switches back to native Messages | Components re-enabled, role dialog shown |
| User denies the role dialog | Re-prompted on next `onResume()` while panel is native |
| App uninstalled | Android clears role automatically |
| API < 29 (no RoleManager) | Fall back to `Telephony.Sms.getDefaultSmsPackage()` check only; role request not possible silently |
