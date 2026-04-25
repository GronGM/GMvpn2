package tun2socks

import (
	"errors"
	"testing"
)

// These tests cover the pure validation surface and the idle-state
// behavior. Starting the real netstack requires a live TUN fd and an
// already-listening SOCKS5 server, neither of which CI provides.
// Lifecycle integration is exercised at the gmvpn layer via a fake
// bridge; the gVisor wiring itself is verified by `go vet` and at
// run-time on a device.

func TestValidateRejectsNegativeTunFD(t *testing.T) {
	if err := validate(-1, 1500, "127.0.0.1:10808"); !errors.Is(err, ErrInvalidTunFD) {
		t.Fatalf("expected ErrInvalidTunFD, got %v", err)
	}
}

func TestValidateRejectsBadMTU(t *testing.T) {
	for _, mtu := range []int32{0, -1, 70000} {
		if err := validate(3, mtu, "127.0.0.1:10808"); !errors.Is(err, ErrInvalidMTU) {
			t.Fatalf("mtu=%d: expected ErrInvalidMTU, got %v", mtu, err)
		}
	}
}

func TestValidateRejectsEmptySocks5Addr(t *testing.T) {
	if err := validate(3, 1500, ""); !errors.Is(err, ErrEmptySocks5Addr) {
		t.Fatalf("expected ErrEmptySocks5Addr, got %v", err)
	}
}

func TestValidateAcceptsGoodInputs(t *testing.T) {
	if err := validate(3, 1500, "127.0.0.1:10808"); err != nil {
		t.Fatalf("validate returned %v on good inputs", err)
	}
}

func TestNewBridgeNotRunningInitially(t *testing.T) {
	b := New()
	if b.IsRunning() {
		t.Fatal("new bridge must not be running")
	}
}

func TestStopWhenNotRunningIsNoop(t *testing.T) {
	b := New()
	if err := b.Stop(); err != nil {
		t.Fatalf("Stop on idle bridge returned %v", err)
	}
	if b.IsRunning() {
		t.Fatal("bridge must remain idle after Stop")
	}
}

func TestStartCallsValidationBeforeNetstack(t *testing.T) {
	// Validation runs before any gVisor / fdbased call, so bad inputs
	// must surface as the typed error without touching the netstack.
	b := New()
	if err := b.Start(-1, 1500, "127.0.0.1:10808"); !errors.Is(err, ErrInvalidTunFD) {
		t.Fatalf("expected ErrInvalidTunFD, got %v", err)
	}
	if err := b.Start(3, 0, "127.0.0.1:10808"); !errors.Is(err, ErrInvalidMTU) {
		t.Fatalf("expected ErrInvalidMTU, got %v", err)
	}
	if err := b.Start(3, 1500, ""); !errors.Is(err, ErrEmptySocks5Addr) {
		t.Fatalf("expected ErrEmptySocks5Addr, got %v", err)
	}
	if b.IsRunning() {
		t.Fatal("bridge must not be running after failed validation")
	}
}
