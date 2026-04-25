// Package tun2socks bridges an Android TUN file descriptor to Xray-core's
// SOCKS5 inbound. The TCP path is implemented against gVisor's userspace
// netstack and the standard library SOCKS5 dialer
// (golang.org/x/net/proxy). UDP is stubbed: golang.org/x/net/proxy does
// not support UDP ASSOCIATE, so DNS over UDP and QUIC do not flow until
// a UDP-capable SOCKS5 client lands. Practically, route DNS through the
// Xray inbound's DNS outbound (DoH/DoT) and treat QUIC as a future
// item — see ADR 0004 §3.
package tun2socks

import (
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"sync"

	"golang.org/x/net/proxy"
	"gvisor.dev/gvisor/pkg/tcpip"
	"gvisor.dev/gvisor/pkg/tcpip/adapters/gonet"
	"gvisor.dev/gvisor/pkg/tcpip/header"
	"gvisor.dev/gvisor/pkg/tcpip/link/fdbased"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv4"
	"gvisor.dev/gvisor/pkg/tcpip/network/ipv6"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
	"gvisor.dev/gvisor/pkg/tcpip/transport/icmp"
	"gvisor.dev/gvisor/pkg/tcpip/transport/tcp"
	"gvisor.dev/gvisor/pkg/tcpip/transport/udp"
	"gvisor.dev/gvisor/pkg/waiter"
)

// Bridge converts IP packets read from a TUN fd into TCP connections
// that talk to a SOCKS5 server on localhost. One Bridge owns at most
// one running netstack at a time.
type Bridge interface {
	// Start brings the bridge up. tunFD is the file descriptor returned
	// by VpnService.Builder#establish(); the caller keeps ownership but
	// must keep it open for the lifetime of the bridge. mtu must match
	// the value set on the TUN device. socks5Addr is "host:port" of the
	// SOCKS5 inbound Xray-core is listening on (loopback in production).
	Start(tunFD int32, mtu int32, socks5Addr string) error

	// Stop tears the netstack down. Safe to call when not running
	// (returns nil).
	Stop() error

	// IsRunning reports whether Start has succeeded and Stop has not yet
	// run.
	IsRunning() bool
}

// New returns the default Bridge implementation, backed by a gVisor
// userspace netstack and a SOCKS5 dialer from `golang.org/x/net/proxy`.
func New() Bridge {
	return &gvisorBridge{}
}

// Sentinel errors. See package docs for the UDP caveat.
var (
	ErrInvalidTunFD    = errors.New("tun2socks: tunFD must be >= 0")
	ErrInvalidMTU      = errors.New("tun2socks: mtu must be > 0 and <= 65535")
	ErrEmptySocks5Addr = errors.New("tun2socks: socks5Addr is empty")
	ErrAlreadyRunning  = errors.New("tun2socks: bridge already running")
)

const (
	nicID          tcpip.NICID = 1
	tcpRcvWnd                  = 0    // 0 → use stack default
	tcpMaxInFlight             = 1024 // pending SYNs per forwarder
	relayBufferSize            = 32 * 1024
)

type gvisorBridge struct {
	mu      sync.Mutex
	running bool
	stack   *stack.Stack
}

func (b *gvisorBridge) Start(tunFD int32, mtu int32, socks5Addr string) error {
	if err := validate(tunFD, mtu, socks5Addr); err != nil {
		return err
	}

	b.mu.Lock()
	defer b.mu.Unlock()
	if b.running {
		return ErrAlreadyRunning
	}

	dialer, err := proxy.SOCKS5("tcp", socks5Addr, nil, proxy.Direct)
	if err != nil {
		return fmt.Errorf("tun2socks: socks5 dialer: %w", err)
	}

	ep, err := fdbased.New(&fdbased.Options{
		FDs: []int{int(tunFD)},
		MTU: uint32(mtu),
	})
	if err != nil {
		return fmt.Errorf("tun2socks: fdbased endpoint: %w", err)
	}

	s := stack.New(stack.Options{
		NetworkProtocols: []stack.NetworkProtocolFactory{
			ipv4.NewProtocol,
			ipv6.NewProtocol,
		},
		TransportProtocols: []stack.TransportProtocolFactory{
			tcp.NewProtocol,
			udp.NewProtocol,
			icmp.NewProtocol4,
			icmp.NewProtocol6,
		},
	})

	if tcpipErr := s.CreateNIC(nicID, ep); tcpipErr != nil {
		return fmt.Errorf("tun2socks: create NIC: %s", tcpipErr)
	}
	if tcpipErr := s.SetSpoofing(nicID, true); tcpipErr != nil {
		return fmt.Errorf("tun2socks: set spoofing: %s", tcpipErr)
	}
	if tcpipErr := s.SetPromiscuousMode(nicID, true); tcpipErr != nil {
		return fmt.Errorf("tun2socks: set promiscuous: %s", tcpipErr)
	}
	s.SetRouteTable([]tcpip.Route{
		{Destination: header.IPv4EmptySubnet, NIC: nicID},
		{Destination: header.IPv6EmptySubnet, NIC: nicID},
	})

	tcpFwd := tcp.NewForwarder(s, tcpRcvWnd, tcpMaxInFlight, tcpHandler(dialer))
	s.SetTransportProtocolHandler(tcp.ProtocolNumber, tcpFwd.HandlePacket)

	udpFwd := udp.NewForwarder(s, udpHandler())
	s.SetTransportProtocolHandler(udp.ProtocolNumber, udpFwd.HandlePacket)

	b.stack = s
	b.running = true
	return nil
}

func (b *gvisorBridge) Stop() error {
	b.mu.Lock()
	defer b.mu.Unlock()
	if !b.running {
		return nil
	}
	if b.stack != nil {
		b.stack.Close()
		b.stack.Wait()
		b.stack = nil
	}
	b.running = false
	return nil
}

func (b *gvisorBridge) IsRunning() bool {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.running
}

// tcpHandler returns a gVisor TCP forwarder handler that dials the
// destination via SOCKS5 and splices the two connections together.
// The dial is attempted before CreateEndpoint so we never half-open a
// netstack endpoint we cannot serve.
func tcpHandler(dialer proxy.Dialer) func(*tcp.ForwarderRequest) {
	return func(r *tcp.ForwarderRequest) {
		id := r.ID()
		target := net.JoinHostPort(
			id.LocalAddress.String(),
			strconv.Itoa(int(id.LocalPort)),
		)

		upstream, err := dialer.Dial("tcp", target)
		if err != nil {
			r.Complete(true) // sendReset = true
			return
		}

		var wq waiter.Queue
		ep, tcpipErr := r.CreateEndpoint(&wq)
		if tcpipErr != nil {
			_ = upstream.Close()
			r.Complete(true)
			return
		}
		r.Complete(false)

		local := gonet.NewTCPConn(&wq, ep)
		go relay(local, upstream)
		go relay(upstream, local)
	}
}

// udpHandler is a deliberate stub. golang.org/x/net/proxy does not
// implement SOCKS5 UDP ASSOCIATE; a future commit can swap this for a
// UDP-capable dialer (e.g. txthinking/socks5). Returning true marks the
// session as handled so the stack does not emit an ICMP port unreachable
// — that would be noisier than silently dropping.
func udpHandler() udp.ForwarderHandler {
	return func(*udp.ForwarderRequest) bool {
		return true
	}
}

// relay copies bytes one direction. Both endpoints are closed when
// either side returns EOF or errors, which propagates the close to the
// other relay goroutine.
func relay(dst, src io.ReadWriteCloser) {
	defer dst.Close()
	defer src.Close()
	buf := make([]byte, relayBufferSize)
	_, _ = io.CopyBuffer(dst, src, buf)
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
