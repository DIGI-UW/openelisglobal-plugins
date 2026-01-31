#!/usr/bin/env bash
#
# Install OpenELIS-Global JAR into local Maven repo for building plugins.
# Intended for use when plugins is a git submodule of OpenELIS-Global-2.
#
# Usage:
#   From OpenELIS-Global-2 root:  plugins/scripts/install-oe-jar.sh [--build]
#   From plugins root:            ./scripts/install-oe-jar.sh [--build]
#
# Options:
#   --build   Build OE (dataexport + main) even if WAR already exists.
#
set -e

OE_VERSION="3.2.1.2"
OE_JAR="openelisglobal-${OE_VERSION}.jar"
WAR_NAME="OpenELIS-Global.war"

# Resolve plugins root: directory containing plugins/pom.xml (openelisglobal-plugins)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ ! -f "$PLUGINS_ROOT/pom.xml" ]; then
  echo "Error: plugins root not found (expected pom.xml at $PLUGINS_ROOT)" >&2
  exit 1
fi

# OE root = parent of plugins directory
OE_ROOT="$(cd "$PLUGINS_ROOT/.." && pwd)"
OE_POM="$OE_ROOT/pom.xml"
if [ ! -f "$OE_POM" ]; then
  echo "Error: OpenELIS-Global-2 root not found (expected pom.xml at $OE_ROOT)." >&2
  echo "This script is intended for use when plugins is a git submodule of OpenELIS-Global-2." >&2
  echo "Run from OpenELIS-Global-2 root as: plugins/scripts/install-oe-jar.sh" >&2
  echo "Or from plugins root as: ./scripts/install-oe-jar.sh" >&2
  exit 1
fi
if ! grep -q '<artifactId>openelisglobal</artifactId>' "$OE_POM" 2>/dev/null; then
  echo "Error: $OE_POM does not look like OpenELIS-Global-2 (missing openelisglobal artifactId)." >&2
  echo "This script is intended for use when plugins is a submodule of OpenELIS-Global-2." >&2
  exit 1
fi

WAR_PATH="$OE_ROOT/target/$WAR_NAME"
DO_BUILD=false
for arg in "$@"; do
  if [ "$arg" = "--build" ]; then
    DO_BUILD=true
    break
  fi
done

if [ ! -f "$WAR_PATH" ] || [ "$DO_BUILD" = true ]; then
  echo "Building OpenELIS-Global-2..."
  if [ -d "$OE_ROOT/dataexport" ]; then
    (cd "$OE_ROOT/dataexport" && mvn clean install -DskipTests -Dmaven.test.skip=true -q)
  fi
  (cd "$OE_ROOT" && mvn clean install -DskipTests -Dmaven.test.skip=true -Dspotless.check.skip=true -q)
fi

if [ ! -f "$WAR_PATH" ]; then
  echo "Error: WAR not found at $WAR_PATH" >&2
  exit 1
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
unzip -q "$WAR_PATH" -d "$TMP"
(cd "$TMP/WEB-INF/classes" && jar cf "$TMP/$OE_JAR" .)

echo "Installing $OE_JAR into local Maven repo..."
(cd "$PLUGINS_ROOT" && mvn install:install-file \
  -Dfile="$TMP/$OE_JAR" \
  -DgroupId=org.openelisglobal \
  -DartifactId=openelisglobal \
  -Dversion="$OE_VERSION" \
  -Dpackaging=jar \
  -q)

echo "Done. You can now run 'mvn clean install' in the plugins directory."
