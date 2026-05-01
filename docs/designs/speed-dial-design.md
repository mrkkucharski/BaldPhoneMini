# Speed Dial Design

## Goal

Add a speed dial capability that lets the user choose a small set of contacts and call one of them with a single tap.

The feature should remain accessibility-first:

- Large touch targets.
- Minimal setup steps.
- Clear labels and photos.
- No hidden gestures required for primary or secondary behavior.
- One visible control should have one predictable action.
- Safe behavior when phone permissions are missing or the selected contact changes.

The maximum number of speed dial entries may be limited, but it should be no fewer than 3. The recommended first limit is 8, because the existing home pinned-item page already renders 8 large tiles per page.

## Existing Building Blocks

The project already has most of the required infrastructure:

- `ContactDetailsActivity` has a contact action menu with favorite and home pin actions.
- `ContactActionsUseCase` already toggles contact home pins.
- `HomeScreenPinHelper` stores pinned contacts and apps.
- `BaldPagerAdapter` loads pinned items into additional home pages.
- `HomeViewFactory` renders 8 large pinned tiles per page.
- `HomeScreenAppView` binds a pinned item to a home tile.
- `CallManager` centralizes call behavior, including dual-SIM selection, direct-call permission handling, emergency-number fallback, and dialer fallback.

The main behavioral gap is that pinned contacts currently open contact details. Speed dial tiles should place a call.

## Recommended UX

Use a dedicated speed dial concept and display it on the existing home pinned-item pages.

1. The user opens a contact details screen.
2. The user opens the contact menu.
3. The user taps `Add to speed dial`.
4. If the contact has one phone number, it is added immediately.
5. If the contact has multiple phone numbers, the app shows a simple large-button chooser for mobile, home, work, and custom labels.
6. The selected contact appears as a large tile on a home speed dial page.
7. A single tap on the tile calls the saved number through `CallManager`.
8. Removing the entry is available from the contact details menu.

Do not reuse Android contact favorites for this behavior. Favorites are already used by the contacts list as a filtering/list-priority concept. Speed dial means "call this number immediately", so it should have separate state.

Avoid using long press or double tap to distinguish speed dial from opening contact details. Elderly users may have inconsistent tap length, reduced fine motor control, or app settings that already require long tap. Double tap is also timing-dependent and can conflict conceptually with accessibility-service interaction patterns. Use explicit tile roles instead.

## Placement

The recommended first implementation is to reuse the existing pinned home pages instead of creating a completely separate screen.

Reasons:

- Speed dial is most useful from the launcher home screen.
- The existing home pinned pages already provide large, simple tiles.
- The first page is crowded and should not lose one of its core launcher buttons.
- The existing pinned-item grid already supports 8 items per page, which is a reasonable accessible limit for the MVP.

The contact details menu exposes two distinct actions for contacts:

- `Add to home` — pins the contact as a regular tile that opens contact details on tap (existing behavior, unchanged).
- `Add to speed dial` — adds the contact as a speed dial tile that places a call on tap (new behavior).

Both tile types appear on the same pinned home pages. A contact may appear as both a regular tile and a speed dial tile simultaneously; this redundancy is acceptable and requires no deduplication logic.

App pinning keeps its current behavior and wording.

## Data Model

The current contact pin storage only stores a contact lookup key. That is not enough for speed dial because one contact can have multiple numbers and the user may choose a specific one.

Add a small speed dial model under modern Kotlin code, for example:

```kotlin
data class SpeedDialEntry(
    val lookupKey: String,
    val phoneNumber: String,
    val phoneType: Int,
    val phoneLabel: String?,
    val displayNameSnapshot: String,
    val photoUriSnapshot: String?
)
```

The entry should keep `lookupKey` so the app can refresh display name and photo when possible. It should also keep the chosen `phoneNumber` so calling remains stable even if the contact has multiple numbers or the contact's numbers later change.

Use a constant for the first implementation:

```kotlin
const val MAX_SPEED_DIAL_ENTRIES = 8
```

## Storage

Use a small modern Kotlin repository/helper, for example:

- `app/baldphone/neo/contacts/speeddial/SpeedDialEntry.kt`
- `app/baldphone/neo/contacts/speeddial/SpeedDialRepository.kt`
- `app/baldphone/neo/contacts/speeddial/SpeedDialActionsUseCase.kt`

For the MVP, `SharedPreferences` is sufficient because:

- The list is small.
- The feature does not require queries across large datasets.
- The current contact pin implementation already uses preferences.
- Avoiding a Room migration keeps the change smaller.

Store entries as a JSON string or another structured encoded string. Prefer a structured serializer if one is already available in the project; otherwise, keep the encoding simple and isolated inside `SpeedDialRepository`.

Do not store speed dial entries in Android contact favorites.

## Home Tile Behavior

The home pinned pages render three distinct tile types. Each tile has one fixed role determined at creation time.

**Regular contact tile** (existing, unchanged):

- Displays contact photo and name.
- Tapping opens `ContactDetailsActivity`.

**Speed dial tile** (new):

- Displays contact photo when available, with the existing fallback avatar when not.
- Displays the contact name.
- Includes a visible phone receiver icon badge over the photo/avatar area.
- Has a distinct border/background accent that does not rely on color alone.
- Does not include a separate `Call` text row, so the photo/avatar remains the same size as a regular contact tile.
- Tapping calls the stored phone number through `CallManager.call(context, number)`.

**App tile** (existing, unchanged):

- Tapping launches the pinned app.

Do not use tap length as a mode switch. Each tile type has exactly one action. A speed dial tile calls; a regular contact tile opens details; an app tile launches the app.

Suggested speed dial tile content:

```text
[phone icon badge] [contact photo]

Anna
```

For accessibility services, the content description should include the full action, for example `Call Anna, mobile` or `Call Anna`.

**Integration:** `BaldPagerAdapter` currently pulls all pinned items from `HomeScreenPinHelper.getAll()`. Extend it to also pull speed dial entries from `SpeedDialRepository` and merge the two lists. `HomeScreenPinHelper` and its storage remain unchanged. App pin storage remains unchanged.

## Contact Details Integration

Update the contact details menu to include both home pin and speed dial actions:

- `Add to home` / `Remove from home` — existing toggle, behavior unchanged.
- `Add to speed dial` — shown when the contact is not already in speed dial. If the contact has multiple phone numbers, tapping opens a number chooser. If speed dial is full, show a simple error dialog or toast explaining the maximum.
- `Remove from speed dial` — shown when the contact already has a speed dial entry.

Removal from `ContactDetailsActivity` is part of the MVP. The user opens the contact through the Contacts screen, opens the contact details menu, and taps `Remove from speed dial`. The home page should stop showing the speed dial tile after the entry is removed.

The contact details state should expose whether the contact has any speed dial entry. If finer control is needed later, it can expose the specific selected numbers.

## Number Selection

If a contact has multiple numbers, the chooser should list:

- Formatted phone number.
- Contact phone type label, such as mobile, home, work, or custom.

The chooser should use existing app dialog patterns and large rows/buttons.

For the first implementation, selecting one number per contact is enough. Supporting multiple speed dial entries for the same contact can be added later if needed.

## Permissions And Calling

All calls should go through `CallManager`.

Do not start `ACTION_CALL` directly from the speed dial code. `CallManager` already handles:

- `CALL_PHONE` permission.
- Fallback to `ACTION_DIAL`.
- Emergency numbers.
- Dual-SIM selection.
- Missing phone app fallback.

Speed dial setup requires contact read access. Use the existing permission flow before querying contacts.

## Edge Cases

Handle these cases explicitly:

- Contact deleted: remove the stale speed dial entry or show an error and clean it up.
- Contact lookup key changed: resolve the latest lookup key when possible.
- Phone number changed: keep calling the stored selected number, but refresh name/photo if the contact can still be resolved.
- Duplicate entry: prevent adding the same `lookupKey + phoneNumber`.
- Max entries reached: block adding and show a clear message.
- Missing `CALL_PHONE`: allow `CallManager` to open the dialer.
- Emergency number selected: allow `CallManager` to use dialer fallback.
- Missing phone app: show the existing call error behavior.

## Testing

Recommended test coverage:

- Unit tests for `SpeedDialRepository` serialization, duplicate prevention, max limit, add, remove, and stale-entry cleanup.
- ViewModel tests for contact details speed dial state if a ViewModel layer is added.
- Instrumentation or screenshot tests for:
  - Contact details menu with add/remove speed dial action.
  - Number chooser for a contact with multiple numbers.
  - Home speed dial tile layout.

Manual device verification:

- Add a contact with one number.
- Add a contact with multiple numbers and verify the selected number is called.
- Remove an entry.
- Fill the list to the max and verify the limit message.
- Test with and without `CALL_PHONE` permission.
- Test dual-SIM behavior if available.

## MVP Scope

The recommended MVP is:

- Max 8 speed dial entries.
- Add/remove from `ContactDetailsActivity`.
- Number chooser for contacts with multiple numbers.
- Render speed dial entries on the existing home pinned pages.
- Single tap calls through `CallManager`.
- Remove speed dial entries from `ContactDetailsActivity`.
- Keep Android Favorites unchanged.
- Keep app pins unchanged.

Future enhancements:

- Dedicated speed dial management screen.
- Reordering entries.
- Multiple speed dial numbers for one contact.
- Edit/remove action directly from the home tile.
- Import existing pinned contacts into speed dial with a one-time migration.
