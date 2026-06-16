.PHONY: help test vet lint tidy gomobile-install android ios contract-fixtures clean

PKG       := github.com/GronGM/GMvpn2/core/gmvpn
OUT_DIR   := build
ANDROID   := $(OUT_DIR)/gmvpn.aar
APPLE     := $(OUT_DIR)/Gmvpn.xcframework
ANDROID_API ?= 26
ANDROID_CGO_LDFLAGS ?= -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

help:
	@echo "Targets:"
	@echo "  test              run Go unit tests"
	@echo "  vet               go vet"
	@echo "  tidy              go mod tidy"
	@echo "  gomobile-install  install gomobile toolchain"
	@echo "  android           build $(ANDROID)"
	@echo "  ios               build $(APPLE)"
	@echo "  contract-fixtures regenerate testdata/configs from gmvpn-core"
	@echo "  clean             remove $(OUT_DIR)"

test:
	go test ./...

vet:
	go vet ./...

tidy:
	go mod tidy

gomobile-install:
	go install golang.org/x/mobile/cmd/gomobile@latest
	gomobile init

$(OUT_DIR):
	mkdir -p $(OUT_DIR)

android: $(OUT_DIR)
	CGO_ENABLED=1 CGO_LDFLAGS="$(ANDROID_CGO_LDFLAGS)" \
	gomobile bind -target=android -androidapi=$(ANDROID_API) \
		-javapkg=com.gmvpn.core \
		-o $(ANDROID) $(PKG)

ios: $(OUT_DIR)
	gomobile bind -target=ios,iossimulator,macos \
		-o $(APPLE) $(PKG)

contract-fixtures:
	cd ../shared && cargo run -p gmvpn-core --example dump_configs -- \
		../core/gmvpn/testdata/configs

clean:
	rm -rf $(OUT_DIR)
