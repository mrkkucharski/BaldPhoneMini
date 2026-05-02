# Speed Dial Stable Ordering

## Problem

Speed dial entries on the home screen must stay in a predictable order. The app
shows the first three speed dial contacts in a dedicated row on the main home
screen, and users expect the same person to always appear in the same position.

The original design stored entries as a plain list with no ordering field. If
anything rewrote that list in a different order, the home screen would silently
accept the new order as truth. The home row also read from a different code path
than the swipe pages, so the two views could diverge after the app pruned a stale
contact.

## Goal

The position a speed dial contact occupies is determined by when it was added and
never changes unless the user explicitly removes that contact. Removing one entry
does not shift the positions of the remaining ones.

Both the main home screen row and the additional speed dial pages must always show
the same list in the same order.

## Ordering Model

Each speed dial entry records the time it was added. Entries are always displayed
in that order, oldest first. A new entry is always placed after all existing ones.

This means:

- The first contact added always occupies the leftmost slot.
- Removing a contact does not move anyone else.
- A contact added after a removal goes to the next available position after all
  remaining contacts.

The visible display is always compact. There are no empty placeholder slots. If
two of three contacts are removed, the remaining one appears in the first slot.

## Migration

Contacts stored before this change have no ordering timestamp. When the app first
reads an existing list after the update, it assigns ordering values that reproduce
the original stored order exactly. The user sees no change — the contacts appear
in the same positions they were in before the update.

## User Experience

The contact details menu is unchanged. Users still tap `Add to speed dial` to add
a contact and `Remove from speed dial` to remove one. The order in which contacts
appear on the home screen and the swipe pages reflects the order in which the user
added them.

## Edge Cases

| Scenario | Expected behavior |
|---|---|
| First launch after update with existing speed dial contacts | Contacts appear in the same order as before the update |
| User removes a middle contact and adds a new one | Remaining contacts keep their positions; the new contact appears last |
| All three home row slots occupied; user removes one | The remaining two stay in their original slots; the row shrinks by one tile |
| Two contacts added in quick succession | The one added first always appears first |
| App restarts | Order is preserved exactly as stored |

## Testing

Recommended coverage:

- Adding contacts in a specific order and verifying that order is preserved across
  app restarts.
- Removing a contact and confirming the others do not shift.
- Adding a contact after a removal and confirming it appears after the existing ones.
- Updating the app over an existing installation and confirming no visible
  reordering occurs.
- Confirming that the main home screen row and the swipe pages always show the
  same contacts in the same order.
