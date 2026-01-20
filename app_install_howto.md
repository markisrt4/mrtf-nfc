# Installing the .apk Android App

## Linux Host Setup
```
sudo apt update
sudo apt install adb
```

## Instructions to download
* [Wired] Connect phone via USB & Install
```
adb devices
adb install app.apk
```

* [Wireless] 
```
adb pair <device-ip>:<port>
adb connect <device-ip>:<port>
adb install ~/Downloads/app-debug.apk
```
Option B: Wireless (Wi-Fi)

Connect Android device and Chromebook to the same network.

On the device: go to Developer Options → Wireless Debugging → Enable.

On your Chromebook/Linux:

adb pair <device-ip>:<port>
# Follow the on-screen pairing code instructions from your phone


Connect:

adb connect <device-ip>:<port>
adb devices    # Confirm your device is listed


Install APK:

adb install ~/Downloads/app-debug.apk

* If reinstalling over an existing app:
```
adb install -r app.apk
```
