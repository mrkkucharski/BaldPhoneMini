# Speed Dial Home Row Design

## Problem

Speed dial contacts are currently available only after swiping away from the main
home screen. That extra gesture makes the most important calling path harder than
it needs to be for elderly users and users with reduced fine motor control.

The main home screen should surface the user's most important speed dial entries
without changing the experience for users who have not configured speed dial.

## Goal

Show the first 1-3 configured speed dial contacts directly on the main home
screen. The row appears automatically when at least one entry exists and
disappears when no speed dial entries are configured.

The home screen remains accessibility-first:

- Large touch targets.
- Clear contact names and photos.
- No hidden gestures.
- No invisible or empty touch targets.
- No setup or editing flow added to the home screen.
- A call is never placed without an explicit confirmation.

## User Experience

When the user has no speed dial entries, the main home screen should look and
behave exactly as it does today.

When the user has one or more speed dial entries, a new row appears above the
existing home action buttons. The row shows up to three contacts in the same
large tile style used by speed dial elsewhere in the launcher. Existing app
buttons keep their order and shift down as a group.

If the user has more than three speed dial entries, only the first three are
promoted to the main home screen. The remaining entries continue to appear on
the existing additional speed dial pages.

The row updates whenever the user returns to the home screen, so adding or
removing a speed dial contact is reflected without restarting the app.

## Call Confirmation

Tapping a speed dial tile opens a confirmation dialog before the call starts.

The dialog should make the action clear:

- The contact name is shown in the prompt.
- The selected phone number is visible.
- The primary action places the call.
- The cancel action dismisses the dialog and leaves the user on the same screen.

This confirmation applies to all speed dial tiles, not only the promoted home
row. Accidental calls are a risk anywhere speed dial appears, so the behavior
should be consistent across the app.

## Layout Behavior

The speed dial row is hidden by default. Hidden means fully collapsed, not merely
transparent. This avoids leaving an invisible touch region on the screen.

When the row is visible, the clock and notification area becomes more compact
and the button area expands enough to keep all four button rows comfortably
tappable. The time should remain prominent; the row should not make the home
screen feel cramped.

Unused slots are also fully collapsed. For example, if only one contact is
configured, only one tile is visible and the other two slots do not receive
touches.

No divider is required between the speed dial row and the normal app rows. The
normal row spacing is enough unless visual testing shows the rows blending
together.

## Data And Ordering

The home row uses the existing speed dial list and preserves its order.

The first three entries in that list are promoted to the home screen. The feature
does not introduce a separate ordering model, a separate favorites list, or a
new setting.

Each entry continues to represent a selected phone number for a contact. The
saved snapshot of the contact name and photo can be used for display, while the
stored phone number remains the number to call.

## Refresh Behavior

The home row refreshes on home screen resume.

This is intentionally not a live subscription. The row does not need to update
while the user is staring at the home screen; it only needs to be correct after
the user returns from Contacts or another part of the app.

If reading the speed dial list fails, the safe fallback is to hide the row.

## Scope

In scope:

- Promote the first 1-3 speed dial entries to the main home screen.
- Hide the row when no entries exist.
- Refresh the row when returning to the home screen.
- Require confirmation before speed dial calls.
- Keep additional speed dial pages intact.

Out of scope:

- Editing speed dial entries from the home screen.
- Reordering speed dial entries from the home screen.
- Showing more than three contacts on the main home screen.
- Adding a setting or toggle for the promoted row.
- Real-time updates while the home screen remains foregrounded.
- Changing the existing speed dial storage model.

## Edge Cases

| Scenario | Expected behavior |
|---|---|
| No speed dial entries | Home screen is visually unchanged |
| One speed dial entry | One tile appears; unused slots are collapsed |
| Two speed dial entries | Two tiles appear; the third slot is collapsed |
| Three speed dial entries | All three promoted slots are visible |
| Four or more entries | Only the first three appear on the main home screen |
| Last entry removed | Row disappears on the next home resume |
| Contact photo missing or unavailable | The existing fallback avatar is shown |
| Call permission missing | Existing call handling opens the appropriate fallback |
| User cancels confirmation | No call starts |

## Testing

Recommended coverage:

- The row is hidden with zero entries.
- The row appears with one, two, or three entries.
- A fourth entry is not promoted to the main home screen.
- Removing all entries restores the original home layout.
- Tapping a speed dial tile shows confirmation before calling.
- Existing speed dial pages also use the confirmation flow.

Manual verification should include a small-screen emulator to confirm the clock,
row spacing, and tile sizes remain comfortable.
