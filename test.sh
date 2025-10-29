#!/bin/bash
set -e

# Install the plugin
./gradlew install

# Run the example workflow
cd example
nextflow run .
