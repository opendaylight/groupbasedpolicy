#!/usr/bin/env bash

if ! java -version 2>/dev/null; then
  sudo apt-get update
  sudo apt-get install openjdk-7-jre-headless -y
fi
