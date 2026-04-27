# Home Screen Widget — Hide Option

## Goal

Let users remove unwanted buttons from the Page 1 home screen. A button the user does not need should not occupy space or invite accidental taps.

The change must remain accessibility-first: the hide action must be an explicit, deliberate choice — never a side effect of a swipe or a long press.

## Current Behavior

Each Page 1 button can be set to one of two states:

- **Default** — the button performs its built-in action (open Dialer, Contacts, etc.).
- **Custom app** — the button launches a user-chosen installed app instead.

There is no way to remove a button entirely.

## New Behavior

A third state — **Hidden** — is added to the customization dialog for each button. Selecting it collapses the button with `GONE` visibility, reclaiming its layout space so the remaining buttons reflow naturally.

The three states and their effects:

| State | How stored | Home screen result |
|---|---|---|
| Default | Preference key absent | Button visible, default action |
| Custom app | Preference key = app component name | Button visible, launches chosen app |
| Hidden | Preference key = `"HIDDEN"` | Button collapsed, no touch target |

## User Flow

1. The user opens **Settings → Edit home screen**.
2. The user taps any Page 1 button.
3. A dialog appears with three options: the button's default label, **Custom**, and **Hide**.
4. The user selects **Hide** and confirms with OK.
5. The button collapses immediately on the next home screen render.
6. To restore the button, the user repeats the flow and selects the default option.

The dialog pre-selects the current state each time it is opened, so the user always sees what is active before changing it.

## Why GONE, Not INVISIBLE

`INVISIBLE` hides the button visually but preserves its layout space and leaves an invisible touch target. For elderly users, an empty region that silently consumes taps is confusing and inaccessible. `GONE` removes the button from layout entirely, so no dead zone remains.

## Stale Preference Guard

The app currently treats any preference value that does not match a known installed app as a stale entry and deletes it automatically. Without a guard, `"HIDDEN"` would be silently deleted on the first home screen load, immediately un-hiding the button.

The guard checks for the sentinel before the stale-cleanup logic runs. A hidden button is a valid, intentional state — not a stale reference — and must be preserved across restarts.

## SMS Native Panel Interaction

The messages button has a side effect beyond launching the app: when it is set to its default state, the app registers itself as the system SMS handler and enables its native messaging components. When it is set to a custom app, those components are disabled.

A hidden messages button should behave the same as the default state for this purpose — the user is hiding the shortcut, not switching to a different SMS app. The native panel and its system components remain active when the messages button is hidden.

## Dialog Button Widths

The customization dialog renders all option buttons at equal width by default. In languages with long option labels (such as Polish, where "Custom" becomes "Niestandardowy"), equal-width buttons cause the longest label to wrap onto two lines while the shortest label (e.g. "SOS" or "Ukryj") has excess empty space.

Button widths are instead distributed proportionally to each label's character count, with a minimum weight applied so that very short labels still produce a comfortably tappable button.

## Edge Cases

- **All nine buttons hidden**: the first page renders empty. Settings remain on Page 2 and are not affected.
- **App uninstalled while button is hidden**: the hidden state is preserved independently of the app database, so uninstalling any app does not accidentally un-hide an unrelated hidden button.
- **Locale change**: the sentinel value is language-independent, so switching language does not affect hidden state.

## Scope

Out of scope for this feature:

- Hiding buttons on Page 2 (those already use a separate boolean visibility mechanism).
- Reordering or resizing the remaining visible buttons after one is hidden.
