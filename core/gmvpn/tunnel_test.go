package gmvpn

import (
	"errors"
	"sync"
	"testing"
)

type capturingListener struct {
	mu     sync.Mutex
	events []event
}

type event struct {
	status string
	detail string
}

func (c *capturingListener) OnStatusChanged(status, detail string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.events = append(c.events, event{status, detail})
}

func (c *capturingListener) snapshot() []event {
	c.mu.Lock()
	defer c.mu.Unlock()
	out := make([]event, len(c.events))
	copy(out, c.events)
	return out
}

// fakeBridge stands in for tun2socks.Bridge in lifecycle tests so we
// don't need a real TUN device or CAP_NET_ADMIN. It records calls and
// can be configured to fail Start.
type fakeBridge struct {
	mu         sync.Mutex
	startErr   error
	stopErr    error
	running    bool
	startCalls int
	stopCalls  int
}

func (f *fakeBridge) Start(tunFD int32, mtu int32, socks5Addr string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.startCalls++
	if f.startErr != nil {
		return f.startErr
	}
	f.running = true
	return nil
}

func (f *fakeBridge) Stop() error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.stopCalls++
	f.running = false
	return f.stopErr
}

func (f *fakeBridge) IsRunning() bool {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.running
}

// minimalConfig is the smallest JSON Xray-core accepts: one freedom
// outbound, no listener sockets, no routing. Good for lifecycle tests.
const minimalConfig = `{
  "outbounds": [{"protocol": "freedom", "settings": {}}]
}`

func newTestTunnel(listener StatusListener, bridge *fakeBridge) Tunnel {
	if bridge == nil {
		bridge = &fakeBridge{}
	}
	return newWithBridge(listener, bridge)
}

func TestStartLifecycleEmitsStartingThenConnected(t *testing.T) {
	l := &capturingListener{}
	bridge := &fakeBridge{}
	tun := newTestTunnel(l, bridge)

	if err := tun.Start(minimalConfig, 3, 1500, 10808); err != nil {
		t.Fatalf("Start returned error: %v", err)
	}
	t.Cleanup(func() { _ = tun.Stop() })

	if bridge.startCalls != 1 {
		t.Errorf("bridge.Start should have been called once, got %d", bridge.startCalls)
	}

	events := l.snapshot()
	if len(events) < 2 {
		t.Fatalf("expected at least 2 events, got %d: %+v", len(events), events)
	}
	if events[0].status != StatusStarting {
		t.Errorf("expected first event %q, got %q", StatusStarting, events[0].status)
	}
	if events[len(events)-1].status != StatusConnected {
		t.Errorf("expected last event %q, got %q", StatusConnected, events[len(events)-1].status)
	}
}

func TestStartRejectsMalformedConfig(t *testing.T) {
	tun := newTestTunnel(nil, nil)
	err := tun.Start(`{"this is": "not xray"`, 3, 1500, 10808)
	if err == nil {
		t.Fatal("expected error for malformed config JSON")
	}
}

func TestStartRejectsInvalidInputs(t *testing.T) {
	tun := newTestTunnel(nil, nil)

	if err := tun.Start("", 3, 1500, 10808); err == nil {
		t.Error("expected error for empty config JSON")
	}
	if err := tun.Start(`{}`, -1, 1500, 10808); err == nil {
		t.Error("expected error for negative tun fd")
	}
	if err := tun.Start(`{}`, 3, 1500, 0); err == nil {
		t.Error("expected error for zero socks port")
	}
	if err := tun.Start(`{}`, 3, 1500, 70000); err == nil {
		t.Error("expected error for out-of-range socks port")
	}
}

func TestBridgeFailureTearsDownXray(t *testing.T) {
	l := &capturingListener{}
	bridge := &fakeBridge{startErr: errors.New("boom")}
	tun := newTestTunnel(l, bridge)

	err := tun.Start(minimalConfig, 3, 1500, 10808)
	if err == nil {
		t.Fatal("expected error from Start when bridge fails")
	}
	// The tunnel must not report itself as running after a failed bridge.
	if _, statsErr := tun.Stats(); !errors.Is(statsErr, ErrNotRunning) {
		t.Errorf("Stats after failed Start should be ErrNotRunning, got %v", statsErr)
	}

	events := l.snapshot()
	if events[len(events)-1].status != StatusError {
		t.Errorf("expected last event %q, got %q", StatusError, events[len(events)-1].status)
	}
}

func TestDoubleStartReturnsAlreadyRunning(t *testing.T) {
	tun := newTestTunnel(nil, nil)
	if err := tun.Start(minimalConfig, 3, 1500, 10808); err != nil {
		t.Fatalf("first Start failed: %v", err)
	}
	t.Cleanup(func() { _ = tun.Stop() })

	if err := tun.Start(minimalConfig, 3, 1500, 10808); !errors.Is(err, ErrAlreadyRunning) {
		t.Fatalf("expected ErrAlreadyRunning, got %v", err)
	}
}

func TestStopAfterStartReturnsToIdle(t *testing.T) {
	l := &capturingListener{}
	bridge := &fakeBridge{}
	tun := newTestTunnel(l, bridge)
	if err := tun.Start(minimalConfig, 3, 1500, 10808); err != nil {
		t.Fatalf("Start: %v", err)
	}
	if err := tun.Stop(); err != nil {
		t.Fatalf("Stop: %v", err)
	}

	if bridge.stopCalls != 1 {
		t.Errorf("bridge.Stop should have been called once, got %d", bridge.stopCalls)
	}

	events := l.snapshot()
	if events[len(events)-1].status != StatusIdle {
		t.Errorf("expected last event %q, got %q", StatusIdle, events[len(events)-1].status)
	}

	if _, err := tun.Stats(); !errors.Is(err, ErrNotRunning) {
		t.Errorf("Stats after Stop should return ErrNotRunning, got %v", err)
	}
}

func TestStopWhenNotRunningReturnsNotRunning(t *testing.T) {
	tun := newTestTunnel(nil, nil)
	if err := tun.Stop(); !errors.Is(err, ErrNotRunning) {
		t.Fatalf("expected ErrNotRunning, got %v", err)
	}
}

func TestStatsWhenRunningReturnsZeroByDefault(t *testing.T) {
	tun := newTestTunnel(nil, nil)
	if err := tun.Start(minimalConfig, 3, 1500, 10808); err != nil {
		t.Fatalf("Start: %v", err)
	}
	t.Cleanup(func() { _ = tun.Stop() })

	s, err := tun.Stats()
	if err != nil {
		t.Fatalf("Stats: %v", err)
	}
	if s.UplinkBytes != 0 || s.DownlinkBytes != 0 {
		t.Errorf("expected zero stats, got %+v", s)
	}
}

func TestVersionIsNonEmpty(t *testing.T) {
	if Version() == "" {
		t.Fatal("Version must not be empty")
	}
	if XrayVersion() == "" {
		t.Fatal("XrayVersion must not be empty")
	}
}

func TestNilListenerIsSafe(t *testing.T) {
	tun := newTestTunnel(nil, nil)
	if err := tun.Start(minimalConfig, 3, 1500, 10808); err != nil {
		t.Fatalf("Start: %v", err)
	}
	if err := tun.Stop(); err != nil {
		t.Fatalf("Stop: %v", err)
	}
}
