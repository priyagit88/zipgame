#!/bin/bash
mkdir -p classes

# Detect OS for classpath separator
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$(uname -s)" == *"MINGW"* ]]; then
    # Windows (Git Bash / MINGW)
    SEP=";"
else
    # Linux / Mac
    SEP=":"
fi

echo "Compiling..."
javac -d classes -cp "lib/*" src/main/java/com/zipgame/*.java

echo "Starting Server..."
# Pass all arguments ($@) to the java command
java -cp "classes${SEP}lib/*" com.zipgame.SimpleServer "$@"
