package gmvpn

import (
	"bytes"
	"errors"
	"fmt"
	"sync"
	"sync/atomic"

	"github.com/xtls/xray-core/core"
	// Register all inbound / outbound / transport / router modules. Without
	// this import core.LoadConfig succeeds only for a trivial subset.
	_ "github.com/xtls/xray-core/main/distro/all"
)

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
	//
	// NOTE: the TUN fd is accepted but not yet consumed. A tun2socks
	// bridge must run between the TUN device and Xray's SOCKS5 inbound
	// for traffic to actually flow — see `docs/adr/0004-xray-core-pin.md`.
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

// XrayVersion returns the pinned Xray-core version, exposed to native
// clients for diagnostics screens and crash reports.
func XrayVersion() string {
	return core.Version()
}

// version is the current wrapper semver. Bumped on release.
const version = "0.0.2"

type engineTunnel struct {
	listener StatusListener

	mu       sync.Mutex
	running  atomic.Bool
	instance *core.Instance
	tunFD    int32
	stats    atomic.Pointer[TrafficStats]
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

	cfg, err := core.LoadConfig("json", bytes.NewReader([]byte(configJSON)))
	if err != nil {
		t.emit(StatusError, err.Error())
		return fmt.Errorf("gmvpn: load config: %w", err)
	}

	inst, err := core.New(cfg)
	if err != nil {
		t.emit(StatusError, err.Error())
		return fmt.Errorf("gmvpn: new instance: %w", err)
	}

	if err := inst.Start(); err != nil {
		// Close in case Start partially initialised features.
		_ = inst.Close()
		t.emit(StatusError, err.Error())
		return fmt.Errorf("gmvpn: start instance: %w", err)
	}

	t.instance = inst
	t.tunFD = tunFD
	t.stats.Store(&TrafficStats{})
	t.running.Store(true)
	t.emit(StatusConnected, "")
	return nil
}

func (t *engineTunnel) Stop() error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if !t.running.Load() {
		return ErrNotRunning
	}
	t.emit(StatusStopping, "")

	var stopErr error
	if t.instance != nil {
		stopErr = t.instance.Close()
		t.instance = nil
	}
	t.tunFD = -1
	t.running.Store(false)
	t.stats.Store(nil)

	if stopErr != nil {
		t.emit(StatusError, stopErr.Error())
		return fmt.Errorf("gmvpn: close instance: %w", stopErr)
	}
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
