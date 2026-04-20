# Pill Alert Slots Design

## Problem

The current pill reminder flow supports only three shared alert times:

- Morning
- Afternoon
- Evening

That is not enough for medication plans where pills are taken several times a day. One Parkinson's schedule might be every 3 hours, for example:

- 07:00
- 10:00
- 13:00
- 16:00
- 19:00
- 22:00

The revised target is 6 total pill alert slots, not 10. This keeps the feature aligned with the existing large-button UI while covering common multi-dose schedules. The exact clock times must remain user-configurable.

## Current Implementation

Relevant files:

- `app/src/main/java/com/bald/uriah/baldphone/databases/reminders/Reminder.java`
- `app/src/main/java/com/bald/uriah/baldphone/databases/reminders/ReminderScheduler.java`
- `app/src/main/java/com/bald/uriah/baldphone/activities/pills/AddPillActivity.java`
- `app/src/main/java/com/bald/uriah/baldphone/activities/pills/PillTimeSetterActivity.java`
- `app/src/main/java/com/bald/uriah/baldphone/activities/pills/PillsActivity.java`
- `app/src/main/java/com/bald/uriah/baldphone/utils/BPrefs.java`

The current model is:

- Each `Reminder` has a `startingTime` integer.
- `startingTime` is currently limited to `TIME_MORNING`, `TIME_AFTERNOON`, and `TIME_EVENING`.
- The actual hour and minute for each slot is stored globally in `BPrefs`.
- `ReminderScheduler` reads `BPrefs.getHour(reminder.getStartingTime(), context)` and `BPrefs.getMinute(...)`.
- The `Reminder` entity also has `hour` and `minute` columns, but they are not currently used by pill scheduling.
- The pill list orders reminders by `starting_time`, not by actual hour/minute.

This shared-slot model is still useful. A caregiver can change the clock time for a slot once, and all reminders assigned to that slot follow it.

## Recommended Solution

Keep the shared slot model and expand it from 3 slots to 6 slots.

Existing slot values stay compatible:

- `0` remains Morning
- `1` remains Afternoon
- `2` remains Evening

Add three more slots:

- `3` = Extra 1
- `4` = Extra 2
- `5` = Extra 3

This is safer and simpler than moving to 10 slots because:

- It covers the Parkinson's example exactly.
- It avoids a Room migration.
- It can reuse the existing `BaldMultipleSelection` style and large-button UX.
- It does not introduce a long scrolling slot list.
- It keeps the mental model small for caregivers and elderly users.

## Configurable Times

The app must not hardcode 07:00 through 22:00 as fixed alarm times. All scheduling and display code should resolve the actual hour and minute from `BPrefs`, just like the current three-slot implementation does.

The current first three fallback values must remain unchanged for backwards compatibility:

- Morning: `07:30`
- Afternoon: `12:30`
- Evening: `19:30`

Do not silently change these defaults during the 6-slot expansion. Existing users who never customized pill times still rely on the current fallback behavior, and fresh installs should continue to see the same first three defaults unless a separate product decision deliberately changes them.

The following values are recommended defaults after the expansion:

| Slot | Display | Default/fallback value |
| --- | --- | --- |
| 0 | Morning | 07:30 |
| 1 | Afternoon | 12:30 |
| 2 | Evening | 19:30 |
| 3 | Extra 1 | 10:00 |
| 4 | Extra 2 | 16:00 |
| 5 | Extra 3 | 22:00 |

The existing names can remain for the first three slots to reduce translation and compatibility work. In the UI, however, the resolved time from preferences should always be visible, because a label such as "Evening" can become misleading after customization. The optional `Every 3 hours` shortcut may intentionally rewrite all six slot times after explicit user action; that is separate from default/fallback behavior.

## UX Proposal

### Add/Edit Pill

Keep the current large selection-button pattern, but show 6 options instead of 3.

The existing one-row horizontal selector should not simply add three more buttons in the same row. Six buttons in one row will likely become too small. Instead, use the same style in a 2-row layout:

```text
Morning      Afternoon     Evening
07:30        12:30         19:30

Extra 1      Extra 2       Extra 3
10:00        16:00         22:00
```

The times in this example are placeholders. The actual text on each button should be generated from the saved slot time.

Introduce one reusable dynamic slot selector for Add/Edit Pill and Pill Time Setter. `BaldMultipleSelection` already accepts runtime `CharSequence` labels, so dynamic text is possible, but the current call sites only pass string resources and the current horizontal layout does not satisfy the 2-row requirement. The implementation can either extend `BaldMultipleSelection` with clear/update helpers and wrapping support, or introduce a small pill-specific `BaldTimeSlotSelection` view. In either case, the selector must support:

- 6 slots.
- Runtime labels such as `Morning\n07:30`.
- Refreshing button text after a slot time changes.
- A single selected slot across both rows.
- Stable large touch targets.

The Add Pill and Edit Pill flow must also reflect configured slot times. When `AddPillActivity` opens, it should build the six slot buttons from `BPrefs`, not from hardcoded labels/times. If the user edits a pill after changing the pill times, the selected slot should still be selected and the button should show the current configured time for that slot.

Requirements:

- Buttons stay large.
- The selected slot is visually obvious.
- Each button shows both label and actual time.
- No long press is required.
- Existing add/edit flow remains one screen.

### Pills List

The list should show actual alert time, not only "Morning", "Afternoon", or "Evening".

Recommended item content:

- Pill name
- Pill icon/color
- Time, for example `07:00`
- Optional slot label, for example `Morning` or `Extra 1`
- Repeat days, same as today
- Edit and delete buttons, same as today

For a Parkinson's schedule, the caregiver should be able to scan the list top to bottom by time.

The Pills screen must reflect time-slot changes immediately when the user returns from `PillTimeSetterActivity`. `PillsActivity.onStart()` should reload reminders and re-resolve each reminder's slot time from `BPrefs` before binding the list. It should not cache old slot labels or times in memory beyond a single bind pass.

### Time Setup Screen

`PillTimeSetterActivity` can keep the same basic structure:

- Slot selector
- Hour chooser
- Minute chooser

But the selector should become the same 2-row, 6-slot large-button selector used by Add/Edit Pill.

This is less disruptive than redesigning the screen as a full list. It also keeps the user's current task clear: pick one slot, adjust its time.

When a slot time is changed, the selector itself must update its displayed time from the newly saved value. For example, if Extra 1 is changed from 16:00 to 15:30, the Extra 1 button on this screen should immediately show 15:30 after saving or changing the chooser value.

On leaving `PillTimeSetterActivity`, existing reminder alarms should be rescheduled using the updated slot times. The current `onStop()` restart behavior can still be used, but it should be verified after the 6-slot change.

### Fast Setup For Every 3 Hours

An optional shortcut is still useful, but it can be very small:

- Button text: `Every 3 hours`
- Start time chooser
- Save button

With 6 total slots, no count chooser is needed for the first version. The shortcut can always fill all 6 slots:

```text
Start: 07:00
Result: 07:00, 10:00, 13:00, 16:00, 19:00, 22:00
```

This shortcut should only configure slot times. It should not create pill reminders automatically. It should write the generated values into `BPrefs`; it should not introduce a separate hardcoded schedule path.

## Data Model Plan

### No Room Migration

Expand the existing slot IDs.

Changes:

- In `Reminder.java`, keep the current constants:
  - `TIME_MORNING = 0`
  - `TIME_AFTERNOON = 1`
  - `TIME_EVENING = 2`
- Add three new constants:
  - `TIME_EXTRA_1 = 3`
  - `TIME_EXTRA_2 = 4`
  - `TIME_EXTRA_3 = 5`
- Add `PILL_TIME_SLOT_COUNT = 6`.
- Update the `@IntDef` to allow all six constants.
- Replace or extend `PILLS_TIME_NAMES` so all six slots have display labels, and update its capacity hint from `3` to `6`.
- Do not keep relying on `getTimeAsStringRes()` for pill UI. `SparseIntArray.get()` returns `0` for unknown keys, which can silently pass an invalid resource ID if a slot is missing from the mapping. Add a context-aware helper instead, such as `getTimeLabel(Context)` or a separate utility that returns a `String`/`CharSequence` containing the slot label and resolved time.
- In `BPrefs`, expand `PILLS_HOUR_DEFAULTS` and `PILLS_MINUTE_DEFAULTS` to 6 values while preserving the current first three fallback values: `07:30`, `12:30`, and `19:30`.
- Keep using `HOUR_KEY_ + slot` and `MINUTE_KEY_ + slot`.
- In `ReminderScheduler`, continue resolving `startingTime` through `BPrefs`.
- In `AddPillActivity`, resolve slot labels and times from `BPrefs` when creating or editing a pill.
- In `AddPillActivity`, remove the existing double `ReminderScheduler.scheduleReminder(...)` call on the edit path while touching this flow.
- In `AddPillActivity`, leave a short code comment that `INDEX_CUSTOM = 5` belongs to the color picker and is unrelated to `TIME_EXTRA_3 = 5`.
- In `PillsActivity`, resolve current slot labels and times from `BPrefs` each time the list is loaded or rebound.
- In `PillsActivity`, sort display by actual slot time rather than raw `starting_time`.
- In `PillsActivity`, keep alarm cancellation independent of display sorting. `cancelAllAlarms()` only needs all reminders so it can continue using an unsorted DAO result.
- Do not hardcode the example times in `AddPillActivity`, `PillTimeSetterActivity`, `PillsActivity`, or `ReminderScheduler`.

Benefits:

- Existing reminders survive unchanged.
- Existing preferences for morning/afternoon/evening survive unchanged.
- Existing fallback defaults for morning/afternoon/evening survive unchanged.
- No database migration is required.
- Scheduling logic remains almost the same.
- The UI remains close to the current large-button design.

Risks:

- The first three labels are semantic day periods, while the last three labels are extra slots.
- `startingTime` still means "slot", not a direct time.
- The unused `Reminder.hour` and `Reminder.minute` columns remain confusing.

Mitigation:

- Always show the actual time next to the slot label.
- Add a code comment around `startingTime` explaining that it is a pill time slot.
- Do not use the unused `hour` and `minute` columns in this change.

## Scheduling Behavior

Each reminder still schedules one alarm at its selected slot and repeat days.

For one medication taken 6 times daily, the caregiver creates 6 reminders:

- Sinemet, Morning, every day
- Sinemet, Afternoon, every day
- Sinemet, Evening, every day
- Sinemet, Extra 1, every day
- Sinemet, Extra 2, every day
- Sinemet, Extra 3, every day

This matches the current architecture and keeps each alert independently editable and deletable.

## Sorting

Do not sort by `starting_time` once users can customize slot times.

Example problem:

- Morning is changed to 10:00
- Afternoon is changed to 07:00

Sorting by slot ID would show 10:00 before 07:00.

Recommended implementation:

- Load reminders from the database.
- Sort in memory by resolved hour and minute from `BPrefs`.
- Then sort by name as a tie breaker.

This avoids a Room query migration and respects customized slot times.

Note that the existing DAO method `getAllRemindersOrderedByTime()` currently orders by `starting_time`, which is a slot ID, not the resolved clock time. Either rename it to make that behavior explicit, add a new unsorted/all-reminders method for call sites that do not need ordering, or replace its display usage in `PillsActivity` with a method whose name matches the new in-memory sort behavior.

## Strings

Add new default-locale strings:

- `extra_1`
- `extra_2`
- `extra_3`
- `every_3_hours`
- `pill_time_slot`, if needed for accessibility labels

Existing translated strings for `morning`, `afternoon`, and `evening` remain useful.

## Accessibility Requirements

- Keep all six slot buttons large.
- Prefer a 2-row by 3-column selector over a 1-row by 6-column selector.
- The selected slot must be obvious through color and text state.
- The actual time must be visible wherever a slot is selected.
- Avoid long press as the only way to configure anything.
- Keep setup understandable for caregivers.

## Proposed Implementation Steps

1. Add three new pill time-slot constants in `Reminder`.
2. Update the `@IntDef` and slot label mapping for 6 slots.
3. Add 6 configurable slot defaults in `BPrefs`, preserving `07:30`, `12:30`, and `19:30` for slots 0-2.
4. Add helper methods for:
   - slot count
   - slot label
   - slot resolved display time
   - context-aware slot label plus time text, replacing pill UI use of `getTimeAsStringRes()`
5. Update Add/Edit Pill to show a 2-row, 6-slot large-button selector populated from current `BPrefs` slot times, and remove the existing double-schedule call when editing.
6. Update Pill Time Setter to use the same 6-slot selector and refresh changed slot times immediately.
7. Update Pills list to show actual times from `BPrefs` after returning from time setup.
8. Sort the Pills display list by resolved hour/minute, without changing alarm cancellation behavior.
9. Optionally add the `Every 3 hours` shortcut.
10. Add focused screenshot/instrumentation coverage for:
    - add pill with Extra 3
    - verify Add/Edit Pill shows the current configured time for Extra 3
    - edit Extra 3 to a custom time
    - verify Pill Time Setter shows the changed Extra 3 time
    - verify Pills screen shows the changed Extra 3 time after returning
    - list sorting by actual time
    - existing morning/afternoon/evening reminders still display correctly after upgrade

## Recommendation

Implement the 6-slot version first.

It covers the Parkinson's every-3-hours schedule, preserves the existing large-button UX, avoids database migration, and keeps the feature easier to understand than a 10-slot design.
