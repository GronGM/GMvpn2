#!/usr/bin/env bash
# Build the two native libraries the Android client needs and lay them
# out so Gradle can pick them up directly:
#
#   shared/target/android/jniLibs/<abi>/libgmvpn_ffi.so   (Rust + UniFFI)
#   shared/target/android/kotlin/uniffi/gmvpn_ffi/*.kt    (Kotlin bindings)
#   core/build/gmvpn.aar                                  (Go + Xray-core)
#
# The Android Gradle module copies these into:
#   clients/android/app/src/main/jniLibs/<abi>/libgmvpn_ffi.so
#   clients/android/app/src/main/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt
#   clients/android/app/libs/gmvpn.aar
#
# Prerequisites checked at the top so failures point at the missing
# tool, not the middle of a build.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHARED_DIR="$REPO_ROOT/shared"
CORE_DIR="$REPO_ROOT/core"

err() {
  printf 'build-android-libs: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || err "missing tool: $1 ($2)"
}

require_env() {
  [[ -n "${!1-}" ]] || err "missing env var: $1 ($2)"
}

require_cmd cargo "install Rust toolchain"
require_cmd cargo-ndk "cargo install cargo-ndk"
require_cmd go "install Go 1.26+"
require_cmd gomobile "go install golang.org/x/mobile/cmd/gomobile@latest && gomobile init"
require_env ANDROID_NDK_HOME "point at an installed Android NDK r26+"

# Sanity-check the four Rust targets are installed; cargo-ndk will fail
# without them but the message it prints is opaque.
for tgt in aarch64-linux-android armv7-linux-androideabi \
           x86_64-linux-android i686-linux-android; do
  rustup target list --installed | grep -q "^$tgt$" \
    || err "missing rust target: $tgt (rustup target add $tgt)"
done

echo ">> Building Rust shared library for all Android ABIs"
make -C "$SHARED_DIR" android-aar

echo ">> Building Go gmvpn.aar via gomobile bind"
make -C "$CORE_DIR" android

echo
echo "Artifacts ready:"
echo "  $SHARED_DIR/target/android/jniLibs/"
ls -1 "$SHARED_DIR/target/android/jniLibs/"
echo "  $SHARED_DIR/target/android/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt"
echo "  $CORE_DIR/build/gmvpn.aar"
echo
echo "Next: copy them into clients/android/app/ (see clients/android/README.md)."
