#!/bin/bash

adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
