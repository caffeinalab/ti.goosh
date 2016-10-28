#!/bin/sh

ICON_SOURCE="platform/android/res/drawable-xxxhdpi/notificationicon.png"
if [ -f "$ICON_SOURCE" ]; then
    mkdir -p "platform/android/res/drawable-xxhdpi"
    mkdir -p "platform/android/res/drawable-xhdpi"
    mkdir -p "platform/android/res/drawable-hdpi"
    mkdir -p "platform/android/res/drawable-mdpi"
    convert "$ICON_SOURCE" -resize 72x72 "platform/android/res/drawable-xxhdpi/notificationicon.png"
    convert "$ICON_SOURCE" -resize 48x48 "platform/android/res/drawable-xhdpi/notificationicon.png"
    convert "$ICON_SOURCE" -resize 36x36 "platform/android/res/drawable-hdpi/notificationicon.png"
    convert "$ICON_SOURCE" -resize 24x24 "platform/android/res/drawable-mdpi/notificationicon.png"
else
    echo "No notificationicon.png found"
fi
