#!/bin/bash

here=$(dirname "${0}")
version=$(cat "${here}/VERSION")
jar="${here}/build/libs/transmart-batch-${version}.jar"

# Check if the jar present
if [ ! -e "${jar}" ]; then
    echo "Cannot find jar: ${jar}"
    echo "To build it, run:"
    echo "  ./gradlew shadowJar"
    exit 1
fi

# Choose the location of the java binary
prefix=
if [ ! "x${JAVA_HOME}" == "x" ]; then
    prefix="${JAVA_HOME}/bin/"
fi

# Run the jar
${prefix}java -jar "${jar}" $@

