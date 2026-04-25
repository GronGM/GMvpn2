package tun2socks

import (
	"errors"
	"testing"
)

func TestNewBridgeNotRunningInitially(t *testing.T) {
	b := New()
	if b.IsRunning() {
		t.Fatal("new bridge must not be running")
	}
}

func TestStartRejectsNegativeTunFD(t *testing.T) {
	b := New()
	err := b.Start(-1, 1500, "127.0.0.1:10808")
	if !errors.Is(err, ErrInvalidTunFD) {
		t.Fatalf("expected ErrInvalidTunFD, got %v", err)
	}
}

func TestStartRejectsBadMTU(t *testing.T) {
	b := New()
	for _, mtu := range []int32{0, -1, 70000} {
		err := b.Start(3, mtu, "127.0.0.1:10808")
		if !errors.Is(err, ErrInvalidMTU) {
			t.Fatalf("mtu=%d: expected ErrInvalidMTU, got %v", mtu, err)
		}
	}
}

func TestStartRejectsEmptySocks5Addr(t *testing.T) {
	b := New()
	err := b.Start(3, 1500, "")
	if !errors.Is(err, ErrEmptySocks5Addr) {
		t.Fatalf("expected ErrEmptySocks5Addr, got %v", err)
	}
}

func TestStartReturnsNotImplementedForNow(t *testing.T) {
	// Documents the current state: validation passes but the netstack
	// hasn't been wired in yet. This test must be flipped (and the
	// stub replaced) when the engine lands.
	b := New()
	err := b.Start(3, 1500, "127.0.0.1:10808")
	if !errors.Is(err, ErrNotImplemented) {
		t.Fatalf("expected ErrNotImplemented, got %v", err)
	}
	if b.IsRunning() {
		t.Fatal("bridge must not report running after stub Start")
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
