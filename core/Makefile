.PHONY: help test vet lint tidy gomobile-install android ios clean

PKG       := github.com/GronGM/GMvpn2/core/gmvpn
OUT_DIR   := build
ANDROID   := $(OUT_DIR)/gmvpn.aar
APPLE     := $(OUT_DIR)/Gmvpn.xcframework

help:
	@echo "Targets:"
	@echo "  test              run Go unit tests"
	@echo "  vet               go vet"
	@echo "  tidy              go mod tidy"
	@echo "  gomobile-install  install gomobile toolchain"
	@echo "  android           build $(ANDROID)"
	@echo "  ios               build $(APPLE)"
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
	gomobile bind -target=android -androidapi=26 \
		-javapkg=com.gmvpn.core \
		-o $(ANDROID) $(PKG)

ios: $(OUT_DIR)
	gomobile bind -target=ios,iossimulator,macos \
		-o $(APPLE) $(PKG)

clean:
	rm -rf $(OUT_DIR)
