# Starting the Android build container

`docker run -it --rm -v "$PWD":/workspace -w /workspace mrtf-android:latest bash`

# building the image

`./gradlew assembleDebug`
