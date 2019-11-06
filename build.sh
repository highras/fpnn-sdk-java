#!/usr/bin/env bash

# go to project root. chmod 777
echo [LiveData] Android Exporting... {$(pwd)}

# build gradle
./gradlew build

echo [LiveData] Export Done!

