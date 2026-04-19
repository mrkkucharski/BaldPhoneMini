# Code Review: `support-android13-app-languages`

**Date:** 2026-04-19  
**Branch:** `support-android13-app-languages`  
**Files changed:** `AndroidManifest.xml`, `SettingsActivity.java`, `res/xml/locales_config.xml` (new)

---

## Overview

Adds Android 13 (API 33) per-app language preferences support:
- Declares supported locales via `res/xml/locales_config.xml`
- Wires `android:localeConfig` in the manifest
- Routes language settings to `Settings.ACTION_APP_LOCALE_SETTINGS` on API 33+, falling back to `Settings.ACTION_LOCALE_SETTINGS` on older versions

---

## Issues

### Bug: `e.getMessage()` can return null — `SettingsActivity.java:408`

`ActivityNotFoundException` may have a null message, causing a NullPointerException inside `Log.e()`.

```java
// Current (unsafe)
Log.e(TAG, e.getMessage());
e.printStackTrace();

// Fix
Log.e(TAG, "Failed to open app locale settings", e);
```

### Minor: Redundant `e.printStackTrace()` — `SettingsActivity.java:409`

`Log.e` with the exception argument already captures the stack trace in Logcat. Remove the extra `printStackTrace()`.

### locales_config.xml — Missing region-specific locales

Comparing declared locales against actual `res/values-*` directories:

| Missing entry | Existing resource dir |
|---|---|
| `nl-NL` | `values-nl-rNL/` |
| `fi-FI` | `values-fi-rFI/` |

Without these entries the per-app language picker won't offer Dutch (Netherlands) or Finnish (Finland) as distinct choices.

```xml
<!-- Add to locales_config.xml -->
<locale android:name="nl-NL" />
<locale android:name="fi-FI" />
```

---

## Observations

- `android:localeConfig` is silently ignored below API 33 — no version guard needed in the manifest. Correct.
- The `iw` → `he` mapping (old vs. new Hebrew BCP 47 tag) is handled transparently by Android's locale negotiation. The config's use of `he` is correct.
- The `ActivityNotFoundException` catch is the right defensive pattern for OEMs that may not implement `ACTION_APP_LOCALE_SETTINGS` even on API 33+.
- Logic and API usage in `openLanguageSettings()` match the Android documentation exactly.

---

## Risk

**Low.** The fallback path is safe and the manifest change is backwards compatible.
