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

	"github.com/GronGM/GMvpn2/core/tun2socks"
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
//
// The data path is two-stage: Xray-core embeds a SOCKS5 inbound on
// `127.0.0.1:socksPort` (built into configJSON by gmvpn-core::xray on
// the Rust side); a tun2socks bridge translates IP packets read from
// tunFD into TCP/UDP sockets that talk to that inbound.
type Tunnel interface {
	// Start brings the tunnel up. configJSON must be the output of
	// gmvpn-core's `build_xray_config` for the active profile. tunFD
	// is the descriptor returned by VpnService#establish (Android) or
	// equivalent. mtu must match the device's MTU. socksPort is the
	// port the configJSON's SOCKS inbound listens on (10808 by default).
	Start(configJSON string, tunFD int32, mtu int32, socksPort int32) error

	// Stop tears the tunnel down. Safe to call when not running
	// (returns ErrNotRunning).
	Stop() error

	// Stats returns a snapshot, or nil + error if no tunnel is up.
	Stats() (*TrafficStats, error)
}

// New returns a new Tunnel that drives Xray-core plus a tun2socks
// bridge. Passing nil as the listener is allowed — status events will
// be dropped.
func New(listener StatusListener) Tunnel {
	return newWithBridge(listener, tun2socks.New())
}

// newWithBridge is the test seam: substitute a fake Bridge to exercise
// engineTunnel lifecycle without touching the real netstack.
func newWithBridge(listener StatusListener, bridge tun2socks.Bridge) Tunnel {
	return &engineTunnel{listener: listener, bridge: bridge}
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
const version = "0.0.3"

type engineTunnel struct {
	listener StatusListener
	bridge   tun2socks.Bridge

	mu       sync.Mutex
	running  atomic.Bool
	instance *core.Instance
	stats    atomic.Pointer[TrafficStats]
}

func (t *engineTunnel) Start(configJSON string, tunFD int32, mtu int32, socksPort int32) error {
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
	if socksPort <= 0 || socksPort > 65535 {
		return fmt.Errorf("gmvpn: invalid socks port: %d", socksPort)
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
		_ = inst.Close()
		t.emit(StatusError, err.Error())
		return fmt.Errorf("gmvpn: start instance: %w", err)
	}

	socksAddr := fmt.Sprintf("127.0.0.1:%d", socksPort)
	if err := t.bridge.Start(tunFD, mtu, socksAddr); err != nil {
		// Bridge failed to come up — tear Xray down so we never leave a
		// half-running tunnel.
		_ = inst.Close()
		t.emit(StatusError, err.Error())
		return fmt.Errorf("gmvpn: start bridge: %w", err)
	}

	t.instance = inst
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

	// Stop the bridge first so the netstack stops feeding traffic into
	// Xray-core; only then close the engine.
	bridgeErr := t.bridge.Stop()

	var instErr error
	if t.instance != nil {
		instErr = t.instance.Close()
		t.instance = nil
	}

	t.running.Store(false)
	t.stats.Store(nil)

	if bridgeErr != nil {
		t.emit(StatusError, bridgeErr.Error())
		return fmt.Errorf("gmvpn: stop bridge: %w", bridgeErr)
	}
	if instErr != nil {
		t.emit(StatusError, instErr.Error())
		return fmt.Errorf("gmvpn: close instance: %w", instErr)
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
