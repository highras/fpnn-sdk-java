#!/usr/bin/env bash

# go to project root. chmod 777
echo [LiveData] Android fpnn Exporting... {$(pwd)}

# build gradle
./gradlew build --info

echo [LiveData] Export Done!

