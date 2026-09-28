# AdvDisplay Operator Guide

## First-Time Setup

1. Install the APK supplied by your administrator and connect the Android screen to the internet.
2. Open **AdvDisplay**, enter your assigned identifier (for example `AYNDP`), and tap **Identify**.
3. Wait for the assigned content. The first image/video download may take time.

Codes use letters, numbers, hyphens, or underscores, up to 64 characters. Leading/trailing
spaces are removed and letters are converted to uppercase. Your administrator must configure
the code and content on the API before first-time setup can succeed.

AdvDisplay selects the display automatically: a web page, a single-image/three-panel slideshow,
or looping videos. Reopening the app restores the last configured screen. The device stays
awake while the app is visible; playback and downloads stop when the player is backgrounded.

## Content Updates and Offline Use

- Images and videos are downloaded to local storage. Once a complete playlist has downloaded,
  it can play without internet, including after restarting AdvDisplay.
- Updates run while the player is visible, normally every five minutes. The current playlist
  stays available while a replacement downloads. Failed or incomplete updates do not clear it.
- Slideshow page timing is configured by the content administrator.
- Web display needs connectivity; it does not have an offline-display guarantee.
- Without any cached content, an error or download screen is shown until setup can complete.
  Slideshow and web errors have **Retry**; video recovery retries automatically.

Changing the assigned mode (for example, images to videos) requires returning to setup and
identifying again. Same-mode playlist changes are picked up by background synchronization.

## Change the Identifier

1. Press Android **Back** while content is playing.
2. Enter the administrator PIN and tap **Continue**.
3. Enter the new identifier on the setup screen.

An incorrect PIN keeps the dialog open and leaves the screen configured. **Cancel** returns
to playback. Clearing the session does not delete downloaded content or change the device ID.
Ask your administrator for the PIN; `1234` is the default only for development/debug builds.

The PIN protects AdvDisplay setup. It does not lock Android Home, system settings, or app removal.

## Invite Another Screen

1. On the setup screen, enter the identifier to share.
2. Tap **Invite Another Screen** and choose a destination in Android's share sheet.
3. On the target Android device, install AdvDisplay first, then open the shared link.
4. AdvDisplay applies the code and starts identification. A screen that is already configured asks
   for its own administrator PIN before replacing its setup.

The link format is `advdisplay://join?c=YOUR_CODE`. If your messaging app does not make the link
clickable, open AdvDisplay and enter the identifier manually. If no sharing app is installed, AdvDisplay
copies the invite to the clipboard. These links do not install the app automatically.

## Troubleshooting

| Symptom | What to do |
| --- | --- |
| Identification fails | Check connectivity and code; ask the API administrator to confirm the code is assigned. Tap **OK**, then **Identify** again. |
| Old content keeps playing | AdvDisplay preserves cached content when the API or a download fails. Check the server and allow the next sync. The administrator must change the media URL when replacing a file. |
| Empty slideshow / download error | Check that every assigned image is available and that three-panel pages include all three slots. Ensure the device has free storage. |
| Videos cannot be played | Ask for videos encoded in a format supported by this Android device. AdvDisplay skips failed clips and retries after all clips fail. |
| Web error | Check the server and connection, then tap **Retry**. AdvDisplay also retries automatically. Certificate errors require the server administrator to fix HTTPS. |
| Back does not return to setup | Enter the correct administrator PIN; it is required intentionally. |
| Invite cannot open | Install AdvDisplay on the receiving device, or enter the shared code manually. |

Uninstalling AdvDisplay or using Android **Clear storage** removes the saved session and local content.
Do not clear storage to troubleshoot offline playback: you would lose its cached media.

## Before Leaving a Screen Unattended

Confirm the correct content plays, test reopening AdvDisplay, disconnect/reconnect the network to
check cached image/video playback, and keep the administrator PIN secure. Test on the actual
TV/player hardware; codecs and remote-control behavior vary between devices.
