#!/usr/bin/env bash
#
# Install OpenELIS-Global classes JAR into local Maven repo for building plugins.
#
# Tries three sources in order (fastest first):
#   1. GitHub release asset  (gh release download — ~5s, pegged to published version)
#   2. Docker image extraction (extract WAR from published image — ~6s, no Maven)
#   3. Local Maven build       (build OE from source — minutes, last resort)
#
# Usage:
#   From OpenELIS-Global-2 root:  plugins/scripts/install-oe-jar.sh [options]
#   From plugins root:            ./scripts/install-oe-jar.sh [options]
#   Standalone (no submodule):    ./install-oe-jar.sh [options]
#
# Options:
#   --build     Force a local Maven build (skip tiers 1-2)
#   --docker    Force Docker extraction (skip tier 1)
#   --help      Show this help
#
set -e

OE_VERSION="3.2.1.2"
OE_JAR="openelisglobal-${OE_VERSION}.jar"
OE_CLASSES_JAR="openelisglobal-${OE_VERSION}-classes.jar"
OE_REPO="DIGI-UW/OpenELIS-Global-2"
DOCKER_IMAGE="itechuw/openelis-global-2"

# Parse options
FORCE_BUILD=false
FORCE_DOCKER=false
for arg in "$@"; do
  case "$arg" in
    --build)  FORCE_BUILD=true ;;
    --docker) FORCE_DOCKER=true ;;
    --help)   head -20 "$0" | tail -17; exit 0 ;;
  esac
done

# Resolve directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGINS_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ ! -f "$PLUGINS_ROOT/pom.xml" ]; then
  echo "Error: plugins root not found (expected pom.xml at $PLUGINS_ROOT)" >&2
  exit 1
fi

# Check if we're inside the OE2 submodule tree
OE_ROOT="$(cd "$PLUGINS_ROOT/.." && pwd)"
OE_POM="$OE_ROOT/pom.xml"
IN_SUBMODULE=false
if [ -f "$OE_POM" ] && grep -q '<artifactId>openelisglobal</artifactId>' "$OE_POM" 2>/dev/null; then
  IN_SUBMODULE=true
fi

# Maven install helper
install_jar() {
  local jar_file="$1"
  echo "Installing into local Maven repo..."
  mvn install:install-file \
    -Dfile="$jar_file" \
    -DgroupId=org.openelisglobal \
    -DartifactId=openelisglobal \
    -Dversion="$OE_VERSION" \
    -Dpackaging=jar \
    -q
  echo "Done. You can now run 'mvn clean install' in the plugins directory."
}

# Extract classes JAR from a WAR file
extract_classes_from_war() {
  local war_file="$1"
  local tmp="$2"
  mkdir -p "$tmp/war-contents"
  unzip -q "$war_file" WEB-INF/classes/* -d "$tmp/war-contents"
  (cd "$tmp/war-contents/WEB-INF/classes" && jar cf "$tmp/$OE_JAR" .)
  echo "$tmp/$OE_JAR"
}

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# ─── Tier 1: GitHub release asset ────────────────────────────────────────────
if [ "$FORCE_BUILD" != true ] && [ "$FORCE_DOCKER" != true ] && command -v gh &>/dev/null; then
  echo "[1/3] Trying GitHub release download ($OE_REPO tag $OE_VERSION)..."
  if gh release download "$OE_VERSION" \
      --repo "$OE_REPO" \
      --pattern "$OE_CLASSES_JAR" \
      --dir "$TMP" 2>/dev/null; then
    echo "  ✓ Downloaded $OE_CLASSES_JAR from release"
    install_jar "$TMP/$OE_CLASSES_JAR"
    exit 0
  else
    echo "  Release asset not found (not yet published?). Trying next source..."
  fi
else
  echo "[1/3] Skipping GitHub release download"
fi

# ─── Tier 2: Docker image extraction ─────────────────────────────────────────
if [ "$FORCE_BUILD" != true ] && command -v docker &>/dev/null; then
  # Try version-tagged image first, then :latest
  for tag in "$OE_VERSION" "latest"; do
    IMAGE="${DOCKER_IMAGE}:${tag}"
    echo "[2/3] Trying Docker image extraction ($IMAGE)..."
    if docker image inspect "$IMAGE" &>/dev/null || docker pull "$IMAGE" 2>/dev/null; then
      CONTAINER="oe-jar-extract-$$"
      docker create --name "$CONTAINER" "$IMAGE" true >/dev/null 2>&1
      if docker cp "$CONTAINER:/usr/local/tomcat/webapps/OpenELIS-Global.war" "$TMP/OpenELIS-Global.war" 2>/dev/null; then
        docker rm "$CONTAINER" >/dev/null 2>&1
        echo "  ✓ Extracted WAR from $IMAGE"
        JAR_PATH=$(extract_classes_from_war "$TMP/OpenELIS-Global.war" "$TMP")
        install_jar "$JAR_PATH"
        exit 0
      fi
      docker rm "$CONTAINER" >/dev/null 2>&1 || true
      echo "  WAR not found in image. Trying next..."
    fi
  done
  echo "  No usable Docker image found. Trying next source..."
else
  echo "[2/3] Skipping Docker extraction"
fi

# ─── Tier 3: Local Maven build (requires submodule context) ──────────────────
echo "[3/3] Building OpenELIS-Global-2 from source..."
if [ "$IN_SUBMODULE" != true ]; then
  echo "Error: Not inside OpenELIS-Global-2 submodule tree and no pre-built artifact available." >&2
  echo "" >&2
  echo "Options:" >&2
  echo "  1. Clone OpenELIS-Global-2 with --recurse-submodules and run from there" >&2
  echo "  2. Pull Docker image:  docker pull $DOCKER_IMAGE:$OE_VERSION" >&2
  echo "  3. Ask a maintainer to publish the classes JAR as a GitHub release asset" >&2
  exit 1
fi

WAR_PATH="$OE_ROOT/target/OpenELIS-Global.war"
if [ ! -f "$WAR_PATH" ] || [ "$FORCE_BUILD" = true ]; then
  echo "  Building dataexport + main project (this may take a few minutes)..."
  if [ -d "$OE_ROOT/dataexport" ]; then
    (cd "$OE_ROOT/dataexport" && mvn clean install -DskipTests -Dmaven.test.skip=true -q)
  fi
  (cd "$OE_ROOT" && mvn clean install -DskipTests -Dmaven.test.skip=true -Dspotless.check.skip=true -q)
fi

if [ ! -f "$WAR_PATH" ]; then
  echo "Error: WAR not found at $WAR_PATH" >&2
  exit 1
fi

echo "  ✓ Extracting classes from WAR..."
JAR_PATH=$(extract_classes_from_war "$WAR_PATH" "$TMP")
install_jar "$JAR_PATH"
