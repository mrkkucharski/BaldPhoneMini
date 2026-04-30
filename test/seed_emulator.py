#!/usr/bin/env python3
"""
seed_emulator.py

1. Clears all call logs on the emulator
2. Clears all contacts on the emulator
3. Uploads sample contacts from sample_contacts.md
4. Uploads call events from sample_call_events.md

SAFETY: aborts immediately if the target device is not an emulator.
"""

import subprocess
import sys
import re
import argparse
import shlex
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR    = Path(__file__).parent
CONTACTS_FILE = SCRIPT_DIR / "sample_contacts.md"
CALLS_FILE    = SCRIPT_DIR / "sample_call_events.md"


# ── ADB helpers ───────────────────────────────────────────────────────────────

def _run(*cmd):
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout.strip(), result.stderr.strip()


def pick_emulator() -> str:
    out, _ = _run("adb", "devices")
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device" and parts[0].startswith("emulator-"):
            return parts[0]
    sys.exit("ERROR: No running emulator found. Start an Android emulator first.")


def verify_emulator(serial: str):
    """Hard-check system properties — refuses to run on physical devices."""
    qemu,  _ = _run("adb", "-s", serial, "shell", "getprop ro.kernel.qemu")
    chars, _ = _run("adb", "-s", serial, "shell", "getprop ro.build.characteristics")
    qemu  = qemu.strip()
    chars = chars.strip()
    if qemu != "1" and "emulator" not in chars:
        sys.exit(
            f"ERROR: Device '{serial}' does not appear to be an emulator.\n"
            f"  ro.kernel.qemu          = {qemu!r}\n"
            f"  ro.build.characteristics = {chars!r}\n"
            "Aborting — will not modify a physical device."
        )


def shell(serial: str, *args) -> str:
    # Join as a quoted string so values with spaces survive the remote shell
    cmd_str = " ".join(shlex.quote(a) for a in args)
    out, _ = _run("adb", "-s", serial, "shell", cmd_str)
    return out


# ── Contact insertion ─────────────────────────────────────────────────────────

def insert_contact(serial: str, first: str, last: str, phone: str, email: str):
    full = f"{first} {last}"

    shell(
        serial,
        "content", "insert",
        "--uri", "content://com.android.contacts/raw_contacts",
        "--bind", "account_type:s:",
        "--bind", "account_name:s:",
    )
    # Newer emulators don't print rowId — query all IDs and take the max
    out = shell(
        serial,
        "content", "query",
        "--uri", "content://com.android.contacts/raw_contacts",
        "--projection", "_id",
    )
    ids = [int(m) for m in re.findall(r"_id=(\d+)", out)]
    if not ids:
        print(f"  WARN: could not get rowId for {full!r}")
        return
    row_id = str(max(ids))

    shell(
        serial,
        "content", "insert",
        "--uri", "content://com.android.contacts/data",
        "--bind", f"raw_contact_id:i:{row_id}",
        "--bind", "mimetype:s:vnd.android.cursor.item/name",
        "--bind", f"data1:s:{full}",
        "--bind", f"data2:s:{first}",
        "--bind", f"data3:s:{last}",
    )
    shell(
        serial,
        "content", "insert",
        "--uri", "content://com.android.contacts/data",
        "--bind", f"raw_contact_id:i:{row_id}",
        "--bind", "mimetype:s:vnd.android.cursor.item/phone_v2",
        "--bind", f"data1:s:{phone}",
        "--bind", "data2:i:2",
    )
    shell(
        serial,
        "content", "insert",
        "--uri", "content://com.android.contacts/data",
        "--bind", f"raw_contact_id:i:{row_id}",
        "--bind", "mimetype:s:vnd.android.cursor.item/email_v2",
        "--bind", f"data1:s:{email}",
        "--bind", "data2:i:1",
    )
    print(f"  + {full}")


# ── Call-log insertion ────────────────────────────────────────────────────────

CALL_TYPE_INT = {"INCOMING": 1, "OUTGOING": 2, "MISSED": 3}


def to_epoch_ms(dt_str: str) -> int:
    """'2026-04-29 08:02'  →  epoch milliseconds (UTC)."""
    dt = datetime.strptime(dt_str, "%Y-%m-%d %H:%M").replace(tzinfo=timezone.utc)
    return int(dt.timestamp() * 1000)


def insert_call(serial: str, name: str, phone: str, call_type: str,
                duration: str, dt_str: str):
    type_int = CALL_TYPE_INT.get(call_type.upper(), 1)
    epoch_ms = to_epoch_ms(dt_str)

    shell(
        serial,
        "content", "insert",
        "--uri", "content://call_log/calls",
        "--bind", f"number:s:{phone}",
        "--bind", f"type:i:{type_int}",
        "--bind", f"date:l:{epoch_ms}",
        "--bind", f"duration:i:{duration}",
        "--bind", f"name:s:{name}",
        "--bind", "new:i:0",
    )
    print(f"  + [{call_type:8}] {name:<25} {dt_str}")


# ── MD table parser ───────────────────────────────────────────────────────────

def parse_table(path: Path) -> list[list[str]]:
    """Return data rows from a Markdown table (skips header & separator lines)."""
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.split("|")[1:-1]]
        if not cells or not cells[0].isdigit():
            continue
        rows.append(cells)
    return rows


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=None,
                        help="Max number of contacts (and calls) to upload")
    args = parser.parse_args()

    serial = pick_emulator()
    verify_emulator(serial)
    print(f"Target emulator : {serial}")
    print(f"Contacts file   : {CONTACTS_FILE.name}")
    print(f"Calls file      : {CALLS_FILE.name}")
    if args.limit:
        print(f"Limit           : {args.limit}")
    print()

    # ── Step 1: Clear call logs ───────────────────────────────────────────────
    print("Step 1 — Clearing call logs...")
    shell(serial, "content", "delete", "--uri", "content://call_log/calls")
    print("  Done.\n")

    # ── Step 2: Clear contacts ────────────────────────────────────────────────
    print("Step 2 — Clearing contacts...")
    shell(serial, "content", "delete",
          "--uri", "content://com.android.contacts/raw_contacts")
    print("  Done.\n")

    # ── Step 3: Upload contacts ───────────────────────────────────────────────
    print("Step 3 — Uploading contacts...")
    contacts = parse_table(CONTACTS_FILE)[:args.limit]
    for row in contacts:
        _, first, last, phone, email = row[:5]
        insert_contact(serial, first, last, phone, email)
    print(f"\n  {len(contacts)} contacts uploaded.\n")

    # ── Step 4: Upload call events ────────────────────────────────────────────
    print("Step 4 — Uploading call events...")
    calls = parse_table(CALLS_FILE)[:args.limit]
    for row in calls:
        _, name, phone, call_type, duration, dt_str = row[:6]
        insert_call(serial, name, phone, call_type, duration, dt_str)
    print(f"\n  {len(calls)} call events uploaded.\n")

    print("All done!")


if __name__ == "__main__":
    main()
