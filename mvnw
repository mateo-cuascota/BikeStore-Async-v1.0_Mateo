#!/usr/bin/env bash
# Mini "Maven wrapper": descarga Maven la primera vez (en ~/.m2/wrapper) y lo ejecuta.
# Uso:  ./mvnw compile exec:java
set -e

MVN_VERSION=3.9.11
WRAPPER_DIR="$HOME/.m2/wrapper"
BASE="$WRAPPER_DIR/apache-maven-$MVN_VERSION"
URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.zip"

if [ ! -f "$BASE/bin/mvn" ]; then
  echo "Descargando Maven $MVN_VERSION, solo la primera vez..."
  mkdir -p "$WRAPPER_DIR"
  ZIP="$WRAPPER_DIR/maven.zip"
  curl -fL -o "$ZIP" "$URL"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$WRAPPER_DIR"
  else
    # Git Bash sin unzip: usamos PowerShell
    powershell -NoProfile -Command "Expand-Archive -Force '$(cygpath -w "$ZIP")' '$(cygpath -w "$WRAPPER_DIR")'"
  fi
  rm -f "$ZIP"
  echo "Maven listo en $BASE"
fi

exec sh "$BASE/bin/mvn" "$@"
