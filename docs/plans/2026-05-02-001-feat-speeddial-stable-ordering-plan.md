---
title: "feat: Stable speed dial ordering via createdAt"
type: feat
status: active
date: 2026-05-02
origin: docs/brainstorms/speeddial-stable-ordering-requirements.md
---

# feat: Stable speed dial ordering via createdAt

## Overview

Add a `createdAt: Long` field to `SpeedDialEntry` so that the order of speed dial entries is determined by insertion time and never changes unless the user explicitly removes an entry. Sort by `createdAt` ascending in `SpeedDialRepository.getAll()`. Fix the home row in `HomePage1` to read through `SpeedDialActionsUseCase.getAll()` instead of directly from the repository, so both the home row and the swipe-page grid always consume the same sorted, pruned list.

---

## Problem Frame

The home row on the main screen shows the first three speed dial entries. Entry order is currently whatever order the JSON array happens to be in SharedPreferences — there is no stable ordering field. The home row also reads directly from `SpeedDialRepository`, bypassing the stale-contact pruning that `SpeedDialActionsUseCase.getAll()` applies. This creates two risks: order instability if anything rewrites the stored array, and a divergent view between the home row and the pager pages.

(see origin: `docs/brainstorms/speeddial-stable-ordering-requirements.md`)

---

## Requirements Trace

- R1. Each `SpeedDialEntry` carries a `createdAt: Long` (epoch ms) set once at add time, never modified.
- R2. `SpeedDialRepository.getAll()` returns entries sorted by `createdAt` ascending; no other sort is applied anywhere.
- R3. Removing an entry does not change the `createdAt` of surviving entries.
- R4. New entries always sort after all existing ones (largest `createdAt`).
- R5. Existing stored entries without a `createdAt` are migrated by assigning sequential values (`1, 2, 3, …`) by their current array index, preserving the current stored order.
- R6. `HomePage1.refreshSpeedDial()` reads through `SpeedDialActionsUseCase.getAll()`, not `SpeedDialRepository.getAll()` directly.
- R7. Existing users' displayed speed dial order is unchanged after the update.

---

## Scope Boundaries

- No drag-to-reorder UI.
- No numbered fixed slots or gap display (display is always compact).
- No changes to the pinned apps/contacts pager grid (alphabetical order is acceptable).
- No change to the two-option contact details menu ("Add to Home" / "Add to Speed Dial").
- No `Clock` or `TimeSource` abstraction — `System.currentTimeMillis()` is used directly, consistent with existing patterns in the codebase (e.g. `ConversationViewModel`, `SmsRepository`).

---

## Context & Research

### Relevant Code and Patterns

- `app/src/main/java/app/baldphone/neo/contacts/speeddial/SpeedDialEntry.kt` — pure `data class`, six fields; `createdAt` is appended as the seventh
- `app/src/main/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepository.kt` — SharedPreferences/JSON persistence; new fields are read defensively with `o.has(...)` (see `phoneLabel`, `photoUri` pattern)
- `app/src/main/java/app/baldphone/neo/contacts/speeddial/SpeedDialActionsUseCase.kt` — permission-aware facade; its `getAll()` prunes stale contacts before returning
- `app/src/main/java/com/bald/uriah/baldphone/views/home/HomePage1.java:274` — the call site that must switch from `new SpeedDialRepository(ctx).getAll()` to `new SpeedDialActionsUseCase(ctx).getAll()`
- `app/src/main/java/com/bald/uriah/baldphone/adapters/BaldPagerAdapter.java` — already uses `SpeedDialActionsUseCase.getAll()`, no change needed
- `app/src/main/java/app/baldphone/neo/contacts/ui/details/ContactDetailsActivity.kt:351,378` — constructs `SpeedDialEntry` for add; must supply `System.currentTimeMillis()`
- `app/src/test/java/com/bald/uriah/baldphone/testutil/InMemorySharedPreferences.kt` — in-memory test double; supports `getLong`/`putLong`; no changes needed
- `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepositoryTest.kt` — uses a positional `entry()` helper; needs `createdAt` parameter
- `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialActionsUseCaseTest.kt` — same `entry()` helper pattern
- `app/src/androidTest/java/...SpeedDialHomeRowTest.java` — Java instrumentation test; constructs `SpeedDialEntry` with positional args; needs updating

### Institutional Learnings

- Existing fields added after initial design (`phoneLabel`, `photoUri`) are read with `if (o.has(F_FIELD)) ... else null`. Follow this same defensive-read pattern for `createdAt`, using a fallback of `0L` to identify legacy entries.
- Design doc (`docs/designs/speed-dial-design.md`) confirms SharedPreferences + JSON is the deliberate storage choice; no Room migration.
- Home row design doc (`docs/designs/speed-dial-home-row-design.md`) confirms the row is a downstream consumer of the repository's sorted list and must apply no independent ordering.

### External References

None — codebase patterns are sufficient.

---

## Key Technical Decisions

- **`createdAt: Long = 0L` default on `SpeedDialEntry`**: The `0L` sentinel identifies legacy (pre-migration) entries. Note that Kotlin `data class` default parameters are **not** visible to Java callers as overloads unless `@JvmOverloads` is applied. The Java instrumentation test (`SpeedDialHomeRowTest.java`) must explicitly pass `0L` as the `createdAt` argument — it cannot rely on a default.
- **Migration inside `SpeedDialRepository.getAll()`**: After deserializing, if any entry has `createdAt == 0L`, assign `(index + 1).toLong()` to those entries and immediately re-persist. Values `1, 2, 3, …` always sort before any real epoch-ms timestamp (~1.7 trillion), so migrated legacy entries always sort before newly added ones — correct by construction. On the first app launch after upgrade, if stale contacts exist, `SpeedDialActionsUseCase.getAll()` triggers `keepOnly()` which calls `getAll()` again. The second `getAll()` call may re-detect `0L` entries if the first `.apply()` write has not flushed yet; both writes queue to the SharedPreferences background thread in order and produce identical values — the result is correct and idempotent.
- **`0L` is a reserved sentinel for "unset"**: Never construct a `SpeedDialEntry` with `createdAt = 0L` in production code. The sentinel exists only for legacy stored data and test fixtures where ordering is explicitly not under test.
- **Sort only in `SpeedDialRepository.getAll()`**: No call site applies a secondary sort. The repository is the single source of sorted truth.
- **`add()` always sets `createdAt = System.currentTimeMillis()`**: Construction of `SpeedDialEntry` in `ContactDetailsActivity` must pass the timestamp at the time of add. The repository does not override an already-set `createdAt` (it uses whatever the entry carries).
- **UI-thread ContentProvider queries in `refreshSpeedDial()`**: After U3, `SpeedDialActionsUseCase.getAll()` calls `contactExists()` (a ContentProvider query) for each entry on the main thread. This is identical to the pattern `BaldPagerAdapter` already uses, so it is an accepted existing tradeoff. With up to 8 entries the impact is negligible, consistent with the project's current approach.

---

## Open Questions

### Resolved During Planning

- **Should `createdAt` be nullable or a sentinel?** Resolved: use `0L` sentinel with a non-nullable field. Keeps comparisons simple and avoids nullable unwrapping.
- **Where should migration run?** Resolved: in `SpeedDialRepository.getAll()`. Single responsibility — the repository owns persistence and ordering. The use case and UI layers never see unmigrated data.
- **Does `BaldPagerAdapter` need changes?** Resolved: no. It already uses `SpeedDialActionsUseCase.getAll()`.

### Deferred to Implementation

- **Exact timestamp collision behaviour**: Two entries added within the same millisecond will have equal `createdAt`. Kotlin's `sortedBy` is stable, so insertion order is preserved for equal keys. This edge case does not need explicit handling but may be worth a comment.

---

## Implementation Units

- [x] U1. **Add `createdAt` to `SpeedDialEntry` and update all construction sites**

  **Goal:** Extend the `SpeedDialEntry` data class with `createdAt: Long = 0L` and update every site that constructs the class so it compiles and carries the intended timestamp.

  **Requirements:** R1, R4

  **Dependencies:** None

  **Files:**
  - Modify: `app/src/main/java/app/baldphone/neo/contacts/speeddial/SpeedDialEntry.kt`
  - Modify: `app/src/main/java/app/baldphone/neo/contacts/ui/details/ContactDetailsActivity.kt`
  - Modify: `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepositoryTest.kt` (entry helper)
  - Modify: `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialActionsUseCaseTest.kt` (entry helper)
  - Modify: `app/src/androidTest/java/.../SpeedDialHomeRowTest.java`

  **Approach:**
  - Append `val createdAt: Long = 0L` as the last field of `SpeedDialEntry`. Placing it last avoids breaking Java positional constructors immediately and matches the pattern of optional fields (`photoUri` is last today).
  - `ContactDetailsActivity` passes `System.currentTimeMillis()` as `createdAt` when constructing the entry to add.
  - The `entry()` helper in both test files gains an optional `createdAt: Long = 0L` parameter so existing test cases compile unchanged; new ordering tests supply explicit values.
  - `SpeedDialHomeRowTest.java` must explicitly pass `0L` as the `createdAt` argument. Kotlin default parameters are not visible to Java callers as overloads without `@JvmOverloads`; there is no fallback — the call site must be updated.
  - Also update the `addPersistsAllEntryFieldsInOrder` test in `SpeedDialRepositoryTest` to pass distinct non-zero `createdAt` values (e.g. `1L` and `2L`), so the assertion `assertEquals(listOf(anna, bob), repository.getAll())` does not break after migration is added in U2 (migration would assign `createdAt = 1` and `2` to `0L` entries, causing the local variables to no longer match the returned entries).

  **Patterns to follow:**
  - `photoUriSnapshot` is the current last optional field — add `createdAt` after it.

  **Test scenarios:**
  - Test expectation: none — this unit only introduces a field with a default; behaviour is verified in U2 and U4.

  **Verification:**
  - `./gradlew assembleDebug` and `./gradlew test` compile without error after this unit.

---

- [x] U2. **Update `SpeedDialRepository`: serialization, migration, and sort**

  **Goal:** Persist and restore `createdAt`, migrate legacy entries, and return entries sorted by `createdAt` ascending from `getAll()`.

  **Requirements:** R1, R2, R3, R4, R5, R7

  **Dependencies:** U1

  **Files:**
  - Modify: `app/src/main/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepository.kt`
  - Test: `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepositoryTest.kt`

  **Approach:**
  - `toJson`: write `createdAt` as a Long JSON field (`F_CREATED_AT = "createdAt"`).
  - `fromJson`: read `createdAt` defensively — `if (o.has(F_CREATED_AT)) o.getLong(F_CREATED_AT) else 0L`. Follow the existing `phoneLabel`/`photoUri` pattern exactly.
  - `add(entry)`: store the entry as-is; `createdAt` is already set by the caller. The repository does not override it.
  - `getAll()` migration block: after deserializing, if any entry has `createdAt == 0L`, assign `(index + 1).toLong()` to each such entry by its position in the raw deserialized list, then call `save()` once to persist the migrated values. Migration runs only when legacy entries are detected.
  - `getAll()` sort: return `entries.sortedBy { it.createdAt }` after the migration block.

  **Execution note:** Implement the migration and sort logic test-first — write the failing migration test and ordering test before changing `getAll()`.

  **Patterns to follow:**
  - `if (o.has(F_PHONE_LABEL)) o.getString(F_PHONE_LABEL) else null` — exact pattern for defensive field reads.

  **Test scenarios:**
  - Happy path: add entries A (createdAt=1), B (createdAt=2), C (createdAt=3) → `getAll()` returns [A, B, C].
  - Happy path: add entries in reverse timestamp order (C=3, A=1, B=2) → `getAll()` returns [A, B, C] sorted by `createdAt`.
  - Edge case: remove B, then call `getAll()` → returns [A, C]; A's and C's `createdAt` values are unchanged.
  - Edge case: two entries with equal `createdAt` — `getAll()` returns them in insertion order (stable sort).
  - Migration: load a stored JSON array with three entries that have no `createdAt` field → `getAll()` returns them in original array order with `createdAt` = 1, 2, 3; re-reading immediately after returns the same order (migration has been persisted).
  - Migration: load a stored JSON array where some entries have `createdAt` and some do not → entries with `createdAt` retain their value; entries without get sequential fallback values starting from 1 based on their raw index. Note: `1, 2, 3` are always less than any real epoch-ms timestamp (~1.7 trillion), so legacy entries sort before newly added ones — this is the correct and expected behaviour.
  - Round-trip: add entry with explicit `createdAt`, call `getAll()`, verify `createdAt` survives the JSON round-trip unchanged.
  - Edge case: `getAll()` on empty storage returns empty list without error.

  **Verification:**
  - `./gradlew test` passes all `SpeedDialRepositoryTest` cases including new ones.
  - Calling `getAll()` twice on a repository containing only legacy entries produces identical results (migration is idempotent).

---

- [x] U3. **Fix `HomePage1.refreshSpeedDial()` to use `SpeedDialActionsUseCase`**

  **Goal:** Make the home row read through `SpeedDialActionsUseCase.getAll()` so it applies the same stale-contact pruning and `createdAt` sort as the pager pages.

  **Requirements:** R6

  **Dependencies:** U2

  **Files:**
  - Modify: `app/src/main/java/com/bald/uriah/baldphone/views/home/HomePage1.java`

  **Approach:**
  - Replace `new SpeedDialRepository(ctx).getAll()` at line 274 with `new SpeedDialActionsUseCase(ctx).getAll()`.
  - The existing `ctx` local variable is already present and suitable — no additional context plumbing needed.
  - `SpeedDialActionsUseCase.getAll()` is not `suspend` — it is a regular synchronous call, same as the current `SpeedDialRepository.getAll()`. No threading changes required.
  - If the new call returns an empty list (permissions denied, all entries stale), the existing `visibleCount == 0` branch already hides the row safely — no new error handling needed.

  **Patterns to follow:**
  - `BaldPagerAdapter.java` lines 67-70 — already constructs `new SpeedDialActionsUseCase(homeScreen)` and calls `.getAll()` on it; mirror this pattern exactly.

  **Test scenarios:**
  - Integration: `SpeedDialHomeRowTest.java` `assertThreeEntriesState` — currently seeds three entries with synthetic lookup keys (`"lookup-1"`, `"lookup-2"`, `"lookup-3"`) that do not exist in the device ContentProvider. After U3, `SpeedDialActionsUseCase.getAll()` calls `contactExists()` for each entry; since the keys are synthetic, all entries are pruned and the row goes empty. **Fix required:** revoke the `READ_CONTACTS` permission before calling `refreshSpeedDial()` in this test (using `UiAutomation.revokeRuntimePermission` or an `InstrumentationRegistry`-based approach), so `SpeedDialActionsUseCase.getAll()` takes the no-permission path and returns raw entries without pruning.
  - Integration: if contacts permission is denied, `SpeedDialActionsUseCase.getAll()` returns raw entries (no prune); the home row must render those entries without crashing.

  **Verification:**
  - `./gradlew connectedAndroidTest` runs `SpeedDialHomeRowTest` without regression.
  - On a device, adding three speed dial contacts and restarting the app shows them in insertion order on the home row.

---

- [x] U4. **Extend unit tests for ordering and migration**

  **Goal:** Add explicit test coverage for `createdAt`-based ordering, migration of legacy data, and removal stability.

  **Requirements:** R2, R3, R5, R7

  **Dependencies:** U2

  **Files:**
  - Modify: `app/src/test/java/app/baldphone/neo/contacts/speeddial/SpeedDialRepositoryTest.kt`

  **Approach:**
  - The test scenarios in U2 already enumerate what is needed. This unit focuses on adding them to `SpeedDialRepositoryTest` using explicit `createdAt` timestamps passed through the `entry()` helper added in U1.
  - Tests that need to verify migration pre-seed the SharedPreferences with a JSON string that lacks `createdAt` fields, then call `repository.getAll()` and assert order and field values.
  - No clock mocking needed — use explicit `Long` literals (e.g. `1L`, `2L`, `3L`) as `createdAt` values in all ordering tests.

  **Patterns to follow:**
  - `invalidStoredJsonIsTreatedAsEmpty` test in `SpeedDialRepositoryTest` — uses `prefs.edit().putString(...)` to pre-seed raw JSON; follow the same pattern for migration tests.

  **Test scenarios:**
  - `getAll_returnsSortedByCreatedAt` — three entries added with out-of-order timestamps are returned in ascending `createdAt` order.
  - `removeDoesNotAffectSurvivingCreatedAt` — add A(1), B(2), C(3), remove B, call `getAll()`, assert result is [A, C] with `createdAt` values 1 and 3 unchanged.
  - `legacyEntriesWithoutCreatedAtAreMigratedToArrayOrder` — pre-seed three entries without `createdAt`, assert `getAll()` returns them in original array order.
  - `migrationIsPersisted` — call `getAll()` once on legacy data, then read the stored JSON directly and assert all entries now have `createdAt` set.
  - `newEntryAfterRemovalSortsLast` — add A(1), B(2), remove A, add C(`System.currentTimeMillis()`), assert `getAll()` returns [B, C].
  - `migrationViaAdd` — pre-seed two legacy entries (no `createdAt`) via raw JSON, then call `add()` with a new entry carrying a real timestamp; assert `getAll()` returns the two legacy entries first (in original order) followed by the new entry.

  **Verification:**
  - `./gradlew test` passes all cases including new ones with zero failures.

---

- [x] U5. **Update `docs/Simple User Manual.md`**

  **Goal:** Ensure the user manual accurately reflects speed dial behaviour post-change. The visible UX is unchanged, but the manual's ordering statement should be made precise.

  **Requirements:** R2, R4 (documentation accuracy)

  **Dependencies:** U2

  **Files:**
  - Modify: `docs/Simple User Manual.md`

  **Approach:**
  - The current manual says "The first three speed dial entries also appear in a dedicated row on the main home screen." This is still accurate.
  - Add a clarifying sentence that speed dials appear in the order they were added, and that order does not change when other speed dials are removed.
  - No structural changes to the manual are needed — this is a one-sentence precision improvement.

  **Test scenarios:**
  - Test expectation: none — documentation only.

  **Verification:**
  - The updated sentence accurately describes the `createdAt`-sorted, compact display behaviour agreed in the brainstorm.

---

## System-Wide Impact

- **Interaction graph:** `SpeedDialActionsUseCase.getAll()` calls `repository.keepOnly()` as a side effect when stale contacts are pruned. `keepOnly()` calls `getAll()` internally (one extra deserialise + sort), then calls `save()`. The sort in `getAll()` runs twice on this path — once during pruning and once on the return value. This is a pre-existing double-read; adding a sort does not change the semantics, only the constant overhead.
- **Error propagation:** The home row's existing `visibleCount == 0` fallback (hide the row) handles an empty or failed `getAll()` safely. `SpeedDialActionsUseCase.getAll()` already returns raw entries when contacts permission is denied — the home row will display them in `createdAt` order without crashing.
- **State lifecycle risks:** Migration runs inside `getAll()` and calls `save()` once. If the app is killed mid-migration, the next `getAll()` call re-detects `createdAt == 0L` entries and re-runs migration — idempotent.
- **API surface parity:** `BaldPagerAdapter` already uses `SpeedDialActionsUseCase.getAll()` — no change needed. No other consumer of `SpeedDialRepository.getAll()` exists in the production source set.
- **Integration coverage:** `SpeedDialHomeRowTest.java` seeds entries and checks display order end-to-end; it must continue to pass after U3.
- **Unchanged invariants:** The `HomeScreenPinHelper` (pinned contacts/apps pager grid) is entirely separate from `SpeedDialRepository` and is not touched by this plan.

---

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| `SpeedDialEntry` positional constructor breaks Java instrumentation test | Default `= 0L` on `createdAt` keeps existing call sites valid; `SpeedDialHomeRowTest.java` is updated in U1 |
| Migration runs every `getAll()` call until persisted | Migration re-saves immediately in `getAll()`; subsequent calls find no `0L` entries and skip the migration block |
| `SpeedDialActionsUseCase.getAll()` prunes stale contacts on first home-row call, modifying stored data | This is the intended behaviour — it was already happening in the pager, now it also happens on the home row. Net effect: stale entries are pruned sooner |
| `addPersistsAllEntryFieldsInOrder` test may become fragile if both entries have `createdAt = 0L` | Updated `entry()` helper in U1 accepts explicit `createdAt`; the test is updated to pass distinct timestamps making the ordering intention explicit |

---

## Documentation / Operational Notes

- Manual update is covered in U5. The visible UX does not change — no release notes entry required beyond noting internal ordering stability.
- No SharedPreferences key or schema version bump is needed — the JSON format is extended with a new optional field, fully backward-compatible.

---

## Sources & References

- **Origin document:** [`docs/brainstorms/speeddial-stable-ordering-requirements.md`](../brainstorms/speeddial-stable-ordering-requirements.md)
- Design context: `docs/designs/speed-dial-design.md`, `docs/designs/speed-dial-home-row-design.md`
- Related code: `app/src/main/java/app/baldphone/neo/contacts/speeddial/`, `app/src/main/java/com/bald/uriah/baldphone/views/home/HomePage1.java:274`
