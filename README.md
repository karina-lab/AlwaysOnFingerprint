# Always-On Fingerprint

An LSPosed module that restores screen-off fingerprint unlocking for Pixel devices with optical sensors.

## Description
Starting with the **Android 16 QPR3** release, Google completely removed the ability to use the optical fingerprint scanner while the screen is off. Even the previously known workaround via ADB: `adb shell settings put secure screen_off_udfps_enabled 1` has stopped working. 
**Always-On Fingerprint** bypasses this artificial limitation, bringing back the seamless "screen-off" unlock experience to your older Pixels!

## Installation
1. Install the APK.
2. Enable the **Always-On Fingerprint** module in **LSPosed Manager**.
3. Ensure **System UI** (`com.android.systemui`)  and **Settings** (`com.android.settings`) are selected in the module's scope.
4. Reboot your device.
5. Navigate to *Settings -> Security & Privacy -> Device Unlock -> Fingerprint* and enable **Screen-off Fingerprint Unlock**

## Compatibility
- Pixel 6, 6 Pro, 6a, 7, Pro, 7a, 8, 8 Pro, 8a
- Stock Android 16 QPR3.
