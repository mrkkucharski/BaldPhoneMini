# Contacts: Frequently Used Section Design

## Goal

Surface the most-called contacts at the top of the contacts list so elderly users can reach familiar people without scrolling or typing.

The contacts screen currently shows all contacts sorted alphabetically. Most users call the same small set of people repeatedly, yet they must scroll or search every time. A "Frequently Used" section at the top eliminates that friction for the most common task.

## Current Behavior

The contacts list shows all contacts in a single alphabetical sequence. The favorites toggle filters to starred contacts only. There is no shortcut to frequently called contacts.

## New Behavior

When the favorites toggle is OFF and no search query is active, a "Frequently Used" section appears above the alphabetical list. It shows up to 8 contacts ordered by total call count (incoming + outgoing combined), drawn from the system call log.

| Condition | Frequently Used section |
|---|---|
| Toggle OFF, no search, ≥ 2 qualifying contacts | Shown at top |
| Toggle OFF, no search, < 2 qualifying contacts | Hidden |
| Toggle OFF, search active | Hidden |
| Toggle ON | Hidden |

Contacts in the section also appear in their normal alphabetical position — the section is a shortcut, not a filter.

## Requirements

**Display**
- R1. Section appears at the top when the favorites toggle is OFF and the search field is empty.
- R2. Up to 8 contacts, ordered by call count descending (incoming + outgoing).
- R3. Section is omitted entirely when fewer than 2 contacts qualify — no empty or near-empty group.
- R4. Section uses a distinct labeled header ("Frequently Used") visually consistent with the existing alphabetical letter headers.

**Interaction with existing controls**
- R5. Favorites toggle ON hides the section; existing filter behavior is unchanged.
- R6. Any non-empty search query hides the section.

**List integrity**
- R7. Contacts appear in both the frequent section and their alphabetical position — no de-duplication.

## Key Decisions

**Call log only for frequency.** Using only the call log (not SMS or app usage) keeps the implementation simple and avoids requesting additional permissions beyond `READ_CALL_LOG`.

**Up to 8 contacts.** Enough coverage for the most common calling patterns without dominating the screen.

**Median call count as the qualification threshold.** Rather than a hardcoded minimum, contacts qualify if their call count is strictly greater than the median count across all contacts with any call history. This adapts to each user's actual calling patterns — a user who calls only one person heavily still sees that contact; a user whose 100 contacts were each called once sees no section at all. The R3 guard (< 2 qualifiers → hide section) remains as the final check.

**All-time call history.** No rolling time window. Simpler query, no extra columns. A time window can be added in a follow-up without changing the public interface.

**Hidden during search and favorites-toggle-on.** Both states already transform the list; showing a pinned section in those states would be confusing. The section disappears immediately when the user starts typing, even before the debounced search result updates.

**No de-duplication between sections.** The frequent section is a shortcut highlight, not a filter. Removing contacts from the alphabetical list would make the list feel inconsistent and harder to scroll predictably.

**Graceful degradation when `READ_CALL_LOG` is denied.** The section is simply omitted — no error state, no toast. The permission is requested opportunistically after contacts permission is granted; the contacts screen does not block on it.

## Scope Boundaries

- Favorites toggle behavior is not changed.
- SMS and messaging data are not used for frequency — call log only.
- No manual pinning of contacts by the user.
- No user-configurable settings for the number of contacts shown or the time window.

## Success Criteria

- A user can reach their most-called contacts without scrolling or typing, visible immediately on opening the contacts screen.
- The favorites toggle and search continue to work exactly as before.
- Denying the call log permission leaves the contacts screen fully functional; the section is simply absent.

## Risks

| Risk | Mitigation |
|---|---|
| Phone number format mismatches (international numbers in call log vs. contacts) | Normalize both sides before comparing; rare misses are acceptable for v1 |
| Call log query performance on large call histories | Query fetches only number and type columns; aggregation is done in memory |
| `READ_CALL_LOG` prompt surprising users | Prompt appears after the contacts screen is visible and only after contacts permission is granted; denial has no negative consequence |

## Testing Plan

Manual checks:

1. Open contacts with call history — confirm "Frequently Used" section appears above the alphabetical list with the most-called contacts.
2. Tap the favorites toggle ON — confirm the section disappears and only starred contacts are shown.
3. Tap the favorites toggle OFF — confirm the section reappears.
4. Type in the search field — confirm the section disappears immediately.
5. Clear the search field — confirm the section reappears.
6. On a fresh install, deny `READ_CALL_LOG` — confirm the contacts screen functions normally and no error is shown.
7. On a device with no call history — confirm no "Frequently Used" section appears.
8. Confirm contacts in the frequent section also appear in their alphabetical position.
