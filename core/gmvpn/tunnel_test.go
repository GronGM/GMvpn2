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

// minimalConfig is the smallest JSON Xray-core accepts: one freedom
// outbound, no listener sockets, no routing. Good for lifecycle tests.
const minimalConfig = `{
  "outbounds": [{"protocol": "freedom", "settings": {}}]
}`

func TestStartLifecycleEmitsStartingThenConnected(t *testing.T) {
	l := &capturingListener{}
	tun := New(l)

	if err := tun.Start(minimalConfig, 3); err != nil {
		t.Fatalf("Start returned error: %v", err)
	}
	t.Cleanup(func() { _ = tun.Stop() })

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
	tun := New(nil)
	err := tun.Start(`{"this is": "not xray"`, 3)
	if err == nil {
		t.Fatal("expected error for malformed config JSON")
	}
}

func TestStartRejectsInvalidInputs(t *testing.T) {
	tun := New(nil)

	if err := tun.Start("", 3); err == nil {
		t.Error("expected error for empty config JSON")
	}
	if err := tun.Start(`{}`, -1); err == nil {
		t.Error("expected error for negative tun fd")
	}
}

func TestDoubleStartReturnsAlreadyRunning(t *testing.T) {
	tun := New(nil)
	if err := tun.Start(minimalConfig, 3); err != nil {
		t.Fatalf("first Start failed: %v", err)
	}
	t.Cleanup(func() { _ = tun.Stop() })

	if err := tun.Start(minimalConfig, 3); !errors.Is(err, ErrAlreadyRunning) {
		t.Fatalf("expected ErrAlreadyRunning, got %v", err)
	}
}

func TestStopAfterStartReturnsToIdle(t *testing.T) {
	l := &capturingListener{}
	tun := New(l)
	if err := tun.Start(minimalConfig, 3); err != nil {
		t.Fatalf("Start: %v", err)
	}
	if err := tun.Stop(); err != nil {
		t.Fatalf("Stop: %v", err)
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
	tun := New(nil)
	if err := tun.Stop(); !errors.Is(err, ErrNotRunning) {
		t.Fatalf("expected ErrNotRunning, got %v", err)
	}
}

func TestStatsWhenRunningReturnsZeroByDefault(t *testing.T) {
	tun := New(nil)
	if err := tun.Start(minimalConfig, 3); err != nil {
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
	tun := New(nil)
	if err := tun.Start(minimalConfig, 3); err != nil {
		t.Fatalf("Start: %v", err)
	}
	if err := tun.Stop(); err != nil {
		t.Fatalf("Stop: %v", err)
	}
}
