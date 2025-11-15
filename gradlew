#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

DIR="$( cd "$( dirname "$0" )" && pwd )"
"$DIR/gradle/wrapper/gradle-wrapper.jar" --gradle-user-home "$DIR/.gradle" -p "$DIR" "$@"
