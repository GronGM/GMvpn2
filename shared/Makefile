.PHONY: help check test clippy fmt fmt-check build-debug build-release kotlin swift python clean

BIND_OUT  := bindings
LIB_DEBUG := target/debug/libgmvpn_ffi.so

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

clean:
	rm -rf $(BIND_OUT) target
