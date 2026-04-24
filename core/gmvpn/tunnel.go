package gmvpn

import (
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
)

// ErrNotImplemented is returned by engine-backed methods until the
// Xray-core integration lands. Callers must treat it as "engine missing",
// not as a transient error.
var ErrNotImplemented = errors.New("gmvpn: engine not wired yet")

// ErrAlreadyRunning is returned if Start is called while a tunnel is up.
var ErrAlreadyRunning = errors.New("gmvpn: tunnel already running")

// ErrNotRunning is returned if Stop or Stats is called while no tunnel
// is up.
var ErrNotRunning = errors.New("gmvpn: tunnel not running")

// Status values emitted through StatusListener. String constants are
// stable across versions — native clients match on these.
const (
	StatusIdle         = "idle"
	StatusStarting     = "starting"
	StatusConnected    = "connected"
	StatusReconnecting = "reconnecting"
	StatusStopping     = "stopping"
	StatusError        = "error"
)

// TrafficStats is a plain-data snapshot of cumulative per-direction
// traffic since the current tunnel started. Bytes are monotonic for the
// lifetime of a single Start/Stop cycle; they reset on Start.
//
// Fields are exported and value-typed to keep gomobile happy.
type TrafficStats struct {
	UplinkBytes   int64
	DownlinkBytes int64
}

// StatusListener receives lifecycle events. Implementations are called
// from an engine goroutine; treat them as off-thread on the platform
// side.
type StatusListener interface {
	OnStatusChanged(status string, detail string)
}

// Tunnel is the full gomobile-exposed surface. A single instance owns
// at most one live tunnel at a time.
type Tunnel interface {
	// Start brings the tunnel up using the given Xray-core config JSON
	// and a pre-established TUN file descriptor. Ownership of tunFD
	// transfers to the tunnel for the lifetime of the session.
	Start(configJSON string, tunFD int32) error

	// Stop tears the tunnel down and releases tunFD. Safe to call when
	// not running (returns ErrNotRunning).
	Stop() error

	// Stats returns a snapshot, or nil + error if no tunnel is up.
	Stats() (*TrafficStats, error)
}

// New returns a new Tunnel. Passing nil as the listener is allowed —
// status events will be dropped.
func New(listener StatusListener) Tunnel {
	return &engineTunnel{listener: listener}
}

// Version returns the wrapper's semver, exposed to native clients for
// diagnostics screens.
func Version() string {
	return version
}

// version is the current wrapper semver. Bumped on release.
const version = "0.0.1"

type engineTunnel struct {
	listener StatusListener

	mu      sync.Mutex
	running atomic.Bool
	stats   atomic.Pointer[TrafficStats]
}

func (t *engineTunnel) Start(configJSON string, tunFD int32) error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if t.running.Load() {
		return ErrAlreadyRunning
	}
	if configJSON == "" {
		return fmt.Errorf("gmvpn: empty config JSON")
	}
	if tunFD < 0 {
		return fmt.Errorf("gmvpn: invalid tun fd: %d", tunFD)
	}

	t.emit(StatusStarting, "")

	// TODO(core): hand configJSON + tunFD to Xray-core here.
	// Until the engine is wired in we deliberately fail loudly so no
	// caller can mistake this wrapper for a working tunnel.
	t.emit(StatusError, ErrNotImplemented.Error())
	return ErrNotImplemented
}

func (t *engineTunnel) Stop() error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if !t.running.Load() {
		return ErrNotRunning
	}
	t.emit(StatusStopping, "")
	// TODO(core): stop Xray-core, release tunFD.
	t.running.Store(false)
	t.stats.Store(nil)
	t.emit(StatusIdle, "")
	return nil
}

func (t *engineTunnel) Stats() (*TrafficStats, error) {
	if !t.running.Load() {
		return nil, ErrNotRunning
	}
	s := t.stats.Load()
	if s == nil {
		return &TrafficStats{}, nil
	}
	// Return a copy — callers on the JVM side may mutate.
	snapshot := *s
	return &snapshot, nil
}

func (t *engineTunnel) emit(status, detail string) {
	if t.listener == nil {
		return
	}
	t.listener.OnStatusChanged(status, detail)
}
