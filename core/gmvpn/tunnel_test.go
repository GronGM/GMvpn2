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

func TestStartReturnsNotImplementedUntilEngineIsWired(t *testing.T) {
	l := &capturingListener{}
	tun := New(l)

	err := tun.Start(`{"inbounds":[]}`, 3)
	if !errors.Is(err, ErrNotImplemented) {
		t.Fatalf("expected ErrNotImplemented, got %v", err)
	}

	events := l.snapshot()
	if len(events) != 2 {
		t.Fatalf("expected 2 events (starting+error), got %d: %+v", len(events), events)
	}
	if events[0].status != StatusStarting {
		t.Errorf("expected first event %q, got %q", StatusStarting, events[0].status)
	}
	if events[1].status != StatusError {
		t.Errorf("expected second event %q, got %q", StatusError, events[1].status)
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

func TestStopWhenNotRunningReturnsNotRunning(t *testing.T) {
	tun := New(nil)
	if err := tun.Stop(); !errors.Is(err, ErrNotRunning) {
		t.Fatalf("expected ErrNotRunning, got %v", err)
	}
}

func TestStatsWhenNotRunningReturnsNotRunning(t *testing.T) {
	tun := New(nil)
	if _, err := tun.Stats(); !errors.Is(err, ErrNotRunning) {
		t.Fatalf("expected ErrNotRunning, got %v", err)
	}
}

func TestVersionIsNonEmpty(t *testing.T) {
	if Version() == "" {
		t.Fatal("Version must not be empty")
	}
}

func TestNilListenerIsSafe(t *testing.T) {
	tun := New(nil)
	// Should not panic on status emission.
	_ = tun.Start(`{}`, 1)
}
