#!/bin/bash

echo "Cleaning project..."
./gradlew clean

echo "Removing Gradle caches..."
rm -rf ~/.gradle/caches/
rm -rf .gradle
rm -rf app/build

echo "Refreshing dependencies and building..."
./gradlew build --refresh-dependencies

echo "Done."