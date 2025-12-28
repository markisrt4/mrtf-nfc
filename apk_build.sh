#!/bin/bash

docker run --rm \
  -v "$(pwd)":/workspace \
  -v "$HOME/.gradle":/root/.gradle \
  -w /workspace \
  mrtf-android-buildenv:latest \
  ./gradlew :app:assembleDebug

