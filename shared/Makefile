.PHONY: help check test clippy fmt fmt-check build-debug build-release kotlin swift python android-libs android-aar clean

BIND_OUT  := bindings
LIB_DEBUG := target/debug/libgmvpn_ffi.so

# Android cross-compile via cargo-ndk. We ship four ABIs to cover ~all
# devices in circulation. minSdk matches clients/android (API 26).
ANDROID_ABIS    := arm64-v8a armeabi-v7a x86_64 x86
ANDROID_API     := 26
ANDROID_OUT     := target/android
ANDROID_AAR_DIR := $(ANDROID_OUT)/jniLibs
ANDROID_RUSTFLAGS ?= -C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384

help:
	@echo "Targets:"
	@echo "  check          cargo check (workspace)"
	@echo "  test           cargo test (workspace)"
	@echo "  clippy         cargo clippy -D warnings"
	@echo "  fmt            rustfmt"
	@echo "  fmt-check      rustfmt --check"
	@echo "  build-debug    cargo build -p gmvpn-ffi"
	@echo "  build-release  cargo build -p gmvpn-ffi --release"
	@echo "  kotlin         generate Kotlin bindings into $(BIND_OUT)/kotlin"
	@echo "  swift          generate Swift bindings into $(BIND_OUT)/swift"
	@echo "  python         generate Python bindings into $(BIND_OUT)/python"
	@echo "  android-libs   build libgmvpn_ffi.so for $(ANDROID_ABIS) via cargo-ndk"
	@echo "  android-aar    android-libs + Kotlin bindings, layout for jniLibs/"
	@echo "  clean          remove $(BIND_OUT)/ and cargo target/"

check:
	cargo check --workspace --all-targets

test:
	cargo test --workspace --all-targets

clippy:
	cargo clippy --workspace --all-targets -- -D warnings

fmt:
	cargo fmt --all

fmt-check:
	cargo fmt --all --check

build-debug:
	cargo build -p gmvpn-ffi

build-release:
	cargo build -p gmvpn-ffi --release

$(LIB_DEBUG): build-debug

kotlin: $(LIB_DEBUG)
	@mkdir -p $(BIND_OUT)/kotlin
	cargo run --bin uniffi-bindgen -- generate \
		--library $(LIB_DEBUG) \
		--language kotlin \
		--out-dir $(BIND_OUT)/kotlin \
		--no-format

swift: $(LIB_DEBUG)
	@mkdir -p $(BIND_OUT)/swift
	cargo run --bin uniffi-bindgen -- generate \
		--library $(LIB_DEBUG) \
		--language swift \
		--out-dir $(BIND_OUT)/swift \
		--no-format

python: $(LIB_DEBUG)
	@mkdir -p $(BIND_OUT)/python
	cargo run --bin uniffi-bindgen -- generate \
		--library $(LIB_DEBUG) \
		--language python \
		--out-dir $(BIND_OUT)/python \
		--no-format

# Cross-compile libgmvpn_ffi.so for every ABI listed in $(ANDROID_ABIS).
# Requires:
#   * Android NDK with $ANDROID_NDK_HOME set
#   * cargo install cargo-ndk
#   * rustup target add aarch64-linux-android armv7-linux-androideabi \
#       x86_64-linux-android i686-linux-android
android-libs:
	@if [ -z "$$ANDROID_NDK_HOME" ]; then \
		echo "ANDROID_NDK_HOME is not set"; exit 1; \
	fi
	@command -v cargo-ndk >/dev/null || { \
		echo "cargo-ndk not installed: cargo install cargo-ndk"; exit 1; }
	RUSTFLAGS="$${RUSTFLAGS:+$$RUSTFLAGS }$(ANDROID_RUSTFLAGS)" cargo ndk \
		$(foreach abi,$(ANDROID_ABIS),-t $(abi)) \
		-o $(ANDROID_AAR_DIR) \
		--platform $(ANDROID_API) \
		build -p gmvpn-ffi --release

# Generate Kotlin bindings + lay out the jniLibs/ tree so the Android
# Gradle module can pick them up directly. Output:
#   $(ANDROID_OUT)/jniLibs/<abi>/libgmvpn_ffi.so
#   $(ANDROID_OUT)/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt
android-aar: android-libs $(LIB_DEBUG)
	@mkdir -p $(ANDROID_OUT)/kotlin
	# Generate bindings from the host library. Android .so metadata is not
	# reliable enough for uniffi-bindgen in CI, while the API surface is the same.
	cargo run --bin uniffi-bindgen -- generate \
		--library $(LIB_DEBUG) \
		--language kotlin \
		--out-dir $(ANDROID_OUT)/kotlin \
		--no-format
	@echo "Android artifacts ready under $(ANDROID_OUT)/"
	@echo "  jniLibs/<abi>/libgmvpn_ffi.so  -> clients/android/app/src/main/jniLibs/"
	@echo "  kotlin/uniffi/gmvpn_ffi/*.kt   -> clients/android/app/src/main/kotlin/"

clean:
	rm -rf $(BIND_OUT) target
