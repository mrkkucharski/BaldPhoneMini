# BaldPhone Mini

## Project Overview
**BaldPhoneMini** is a fork of [BaldPhone Neo](https://github.com/DamianKuzmiak/BaldPhoneNeo) by Damian Kuzmiak, which in turn is a continuation of the original [BaldPhone](https://github.com/UriahShaulMandel/BaldPhone) by Uriah Shaul Mandel. 

## Fork Goals
The goal of this fork is to push usability simplification even further. The app should stay useful as a daily phone launcher while reducing unnecessary choices, making important actions easier to reach, and keeping flows predictable for elderly and accessibility-challenged users.

## Modifications in This Fork
- **Simplified messages:** Native SMS thread and conversation screens, incoming message handling, unread badge fixes, and default SMS app role syncing.


- **Speed dial:** Add contacts to one-tap calling tiles from contact details, with a speed dial row directly on the home screen.
- **Frequently used contacts:** The Contacts screen shows a "Frequently Used" section at the top, surfacing the contacts you call most.
- **Better pill reminders:** Six configurable daily pill time slots and improved pill alarm behavior.
- **More flexible second home screen:** Now you can reduce visible widgets to just a few.
- **Launcher setup fixes:** Easier default launcher setup and option to return to the default home launcher.
- **Page-turn arrows:** Optional large navigation arrows in basic accessibility mode for users who find swiping difficult.
- **Android 13+ language support:** Per-app language settings.

<img  width="256" src="https://github.com/user-attachments/assets/193f0ff2-b711-4188-ba91-62870cb039f6" />

<img width="256" src="https://github.com/user-attachments/assets/b361221c-64e0-4105-8da1-cfe1765e9871" />
<img width="256" src="https://github.com/user-attachments/assets/a9c02961-baec-4a0b-9f60-663e7e7c167e" />
<img width="256" src="https://github.com/user-attachments/assets/6dde363f-3a2b-4715-b658-cff408c8edbe" />
<img width="256"  src="https://github.com/user-attachments/assets/4da2d84f-6c93-4f69-9b41-45b769e42384" />




## Installation

Installation requires a PC and a USB cable. The steps below take about 5 minutes.

### 1. Enable Developer Options on the phone

Go to **Settings → About Phone** and tap **Build number** 7 times rapidly until you see "You are now a developer!"

> **Samsung devices:** the Build number is under **Settings → About Phone → Software information → Build number**

### 2. Enable USB Debugging

Go back to **Settings** — a new **Developer options** entry has appeared near the bottom. Open it and turn on **USB Debugging**.

### 3. Connect via USB and set connection mode to File Transfer

Plug the phone into the PC. When the phone asks *"How do you want to use this connection?"*, choose **File Transfer** (or **MTP**). If you missed the prompt, pull down the notification shade and tap the *"Charging via USB"* notification to change it.

> This step is required — ADB does not reach the device reliably in charging-only mode.

### 4. Install ADB on the PC

- **Windows:** Download and extract [platform-tools for Windows](https://dl.google.com/android/repository/platform-tools-latest-windows.zip), then open a terminal in that folder.
- **macOS:** `brew install android-platform-tools`
- **Linux (Ubuntu/Debian):** `sudo apt-get install android-sdk-platform-tools`

### 5. Verify the connection

```bash
adb devices
```

A dialog will appear on the phone — tap **Allow** (check *Always allow from this computer*). Run `adb devices` again; the device should show status `device`.

### 6. Install the APK

Download the latest APK from [Releases](../../releases) and run:

```bash
adb install BaldPhoneMini.apk
```

To update an existing installation:

```bash
adb install -r BaldPhoneMini.apk
```

On success the terminal prints `Success` and the app appears in the launcher.

## Community

BaldPhone Mini shares its roots with the broader BaldPhone community — see [BaldPhone Neo](https://github.com/DamianKuzmiak/BaldPhoneNeo) for community links.

## Contact

[mrkkucharski@gmail.com](mailto:mrkkucharski@gmail.com)

## Google Play
At this stage, **BaldPhone Neo is not available on Google Play**.

## License
BaldPhone Mini is an open-source project, released under the **Apache License, Version 2.0** -  
the same license as the original [BaldPhone](https://github.com/UriahShaulMandel/BaldPhone) and [BaldPhone Neo](https://github.com/DamianKuzmiak/BaldPhoneNeo) .

This means you are free to:
- Use, modify, and distribute the app
- As long as derivative works remain open-source under the same license

For the full license text, see the [LICENSE](LICENSE) file.
