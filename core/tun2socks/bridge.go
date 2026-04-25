// Package tun2socks bridges an Android TUN file descriptor to Xray-core's
// SOCKS5 inbound. The interface and lifecycle are stable; the gVisor
// netstack engine is **not yet wired** — see the package README and
// `docs/adr/0004-xray-core-pin.md` §3.
//
// Why a stub: the obvious off-the-shelf implementation
// (xjasonlyu/tun2socks/v2) is pinned to a May-2025 gVisor that is
// incompatible with the Jan-2026 gVisor that the pinned Xray-core
// pulls. Forcing one version breaks the other. Resolving that is
// tracked as the next concrete task; in the meantime gmvpn integrates
// against the [Bridge] interface so the engine integration lands as a
// surgical change, not a refactor.
package tun2socks

import (
	"errors"
	"sync"
)

// Bridge converts IP packets read from a TUN fd into TCP/UDP connections
// that talk to a SOCKS5 server on localhost. One Bridge owns at most one
// running netstack at a time.
type Bridge interface {
	// Start brings the bridge up. tunFD is the file descriptor returned
	// by VpnService.Builder#establish(); ownership transfers for the
	// lifetime of the call. mtu must match the value set on the TUN
	// device (Android Builder.setMtu). socks5Addr is "host:port" of
	// the SOCKS5 inbound Xray-core is listening on (loopback in
	// production).
	Start(tunFD int32, mtu int32, socks5Addr string) error

	// Stop tears the netstack down and closes the TUN fd. Safe to call
	// when not running (no-op).
	Stop() error

	// IsRunning reports whether Start has succeeded and Stop has not yet
	// run.
	IsRunning() bool
}

// New returns the default Bridge implementation. Currently a validating
// stub; replace with a real netstack-backed bridge once the gVisor
// version conflict (ADR 0004 §3) is resolved.
func New() Bridge {
	return &stubBridge{}
}

// Errors returned for bad arguments and the not-yet-implemented engine.
var (
	ErrInvalidTunFD    = errors.New("tun2socks: tunFD must be >= 0")
	ErrInvalidMTU      = errors.New("tun2socks: mtu must be > 0 and <= 65535")
	ErrEmptySocks5Addr = errors.New("tun2socks: socks5Addr is empty")
	ErrAlreadyRunning  = errors.New("tun2socks: bridge already running")
	ErrNotImplemented  = errors.New("tun2socks: netstack engine not wired yet")
)

type stubBridge struct {
	mu      sync.Mutex
	running bool
}

func (b *stubBridge) Start(tunFD int32, mtu int32, socks5Addr string) error {
	if err := validate(tunFD, mtu, socks5Addr); err != nil {
		return err
	}

	b.mu.Lock()
	defer b.mu.Unlock()

	if b.running {
		return ErrAlreadyRunning
	}
	// TODO(bridge): start a gVisor-backed netstack reading from tunFD,
	// forwarding TCP/UDP through socks5Addr.
	return ErrNotImplemented
}

func (b *stubBridge) Stop() error {
	b.mu.Lock()
	defer b.mu.Unlock()
	if !b.running {
		return nil
	}
	b.running = false
	return nil
}

func (b *stubBridge) IsRunning() bool {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.running
}

func validate(tunFD int32, mtu int32, socks5Addr string) error {
	if tunFD < 0 {
		return ErrInvalidTunFD
	}
	if mtu <= 0 || mtu > 65535 {
		return ErrInvalidMTU
	}
	if socks5Addr == "" {
		return ErrEmptySocks5Addr
	}
	return nil
}
