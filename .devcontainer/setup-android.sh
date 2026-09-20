#!/bin/bash
set -e

echo "=== Setting up Android SDK ==="

# Install dependencies
apt-get update && apt-get install -y \
  wget \
  unzip \
  curl \
  git \
  gradle \
  && rm -rf /var/lib/apt/lists/*

# Install Android SDK command line tools
export ANDROID_HOME=/opt/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools

cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest

# Accept licenses and install SDK components
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "platforms;android-34" \
  "build-tools;34.0.0" \
  "platform-tools"

# Set up Gradle wrapper if not present
cd /workspaces/MeizuMyvuApp
if [ ! -f gradlew ]; then
  gradle wrapper --gradle-version 8.2
  chmod +x gradlew
fi

echo "=== Android SDK setup complete ==="
echo "ANDROID_HOME=$ANDROID_HOME"
