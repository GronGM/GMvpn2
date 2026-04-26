package tun2socks

// SOCKS5 UDP ASSOCIATE primitives (RFC 1928 §4 / §7).
//
// `golang.org/x/net/proxy` only does TCP CONNECT, so we implement the
// little extra plumbing UDP forwarding needs by hand: open a TCP
// control connection to the SOCKS5 server with CMD=0x03, parse the
// returned BND.ADDR/BND.PORT to get the UDP relay endpoint, and
// wrap/unwrap UDP datagrams with SOCKS5 headers as we relay them
// between the gVisor netstack and the proxy.

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"time"
)

const (
	socks5Version = 0x05
	cmdConnect    = 0x01
	cmdUDP        = 0x03
	atypIPv4      = 0x01
	atypDomain    = 0x03
	atypIPv6      = 0x04

	// Worst-case UDP header: RSV(2) + FRAG(1) + ATYP(1) + IPv6(16) + PORT(2)
	maxUDPHeader = 22

	socks5HandshakeTimeout = 5 * time.Second
)

// udpAssociation is the live result of a SOCKS5 UDP ASSOCIATE call.
// Closing the control connection signals the server to drop the
// association; the caller is responsible for closing both fields when
// the UDP session ends.
type udpAssociation struct {
	control net.Conn
	relay   *net.UDPAddr
}

// associateUDP performs the SOCKS5 handshake and UDP ASSOCIATE.
func associateUDP(socks5Addr string) (*udpAssociation, error) {
	d := net.Dialer{Timeout: socks5HandshakeTimeout}
	conn, err := d.Dial("tcp", socks5Addr)
	if err != nil {
		return nil, fmt.Errorf("socks5 udp: dial: %w", err)
	}
	if err := conn.SetDeadline(time.Now().Add(socks5HandshakeTimeout)); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: set deadline: %w", err)
	}

	// Method negotiation: NO AUTHENTICATION.
	if _, err := conn.Write([]byte{socks5Version, 0x01, 0x00}); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: greet: %w", err)
	}
	var greet [2]byte
	if _, err := io.ReadFull(conn, greet[:]); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: read greet: %w", err)
	}
	if greet[0] != socks5Version || greet[1] != 0x00 {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: bad greet ver=%d method=%d", greet[0], greet[1])
	}

	// UDP ASSOCIATE with placeholder DST = 0.0.0.0:0 (we don't bind).
	req := []byte{
		socks5Version, cmdUDP, 0x00, atypIPv4,
		0x00, 0x00, 0x00, 0x00, // 0.0.0.0
		0x00, 0x00, // port 0
	}
	if _, err := conn.Write(req); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: associate: %w", err)
	}

	// Reply: VER REP RSV ATYP BND.ADDR BND.PORT
	hdr := make([]byte, 4)
	if _, err := io.ReadFull(conn, hdr); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: read reply: %w", err)
	}
	if hdr[0] != socks5Version {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: bad reply ver=%d", hdr[0])
	}
	if hdr[1] != 0x00 {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: associate refused, rep=%d", hdr[1])
	}
	addr, err := readBoundAddress(conn, hdr[3])
	if err != nil {
		_ = conn.Close()
		return nil, err
	}

	// Reset deadlines: control conn lives for the session.
	if err := conn.SetDeadline(time.Time{}); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("socks5 udp: clear deadline: %w", err)
	}

	return &udpAssociation{control: conn, relay: addr}, nil
}

func (a *udpAssociation) Close() error {
	if a == nil || a.control == nil {
		return nil
	}
	return a.control.Close()
}

func readBoundAddress(r io.Reader, atyp byte) (*net.UDPAddr, error) {
	switch atyp {
	case atypIPv4:
		buf := make([]byte, 6)
		if _, err := io.ReadFull(r, buf); err != nil {
			return nil, fmt.Errorf("socks5 udp: read v4 bnd: %w", err)
		}
		ip := net.IPv4(buf[0], buf[1], buf[2], buf[3])
		port := binary.BigEndian.Uint16(buf[4:6])
		return &net.UDPAddr{IP: ip, Port: int(port)}, nil
	case atypIPv6:
		buf := make([]byte, 18)
		if _, err := io.ReadFull(r, buf); err != nil {
			return nil, fmt.Errorf("socks5 udp: read v6 bnd: %w", err)
		}
		ip := make(net.IP, 16)
		copy(ip, buf[:16])
		port := binary.BigEndian.Uint16(buf[16:18])
		return &net.UDPAddr{IP: ip, Port: int(port)}, nil
	case atypDomain:
		var l [1]byte
		if _, err := io.ReadFull(r, l[:]); err != nil {
			return nil, fmt.Errorf("socks5 udp: read domain len: %w", err)
		}
		buf := make([]byte, int(l[0])+2)
		if _, err := io.ReadFull(r, buf); err != nil {
			return nil, fmt.Errorf("socks5 udp: read domain: %w", err)
		}
		host := string(buf[:l[0]])
		port := binary.BigEndian.Uint16(buf[l[0] : l[0]+2])
		// Servers usually return an IP for BND.ADDR; if a domain comes
		// back, resolve it once here.
		ips, err := net.LookupIP(host)
		if err != nil || len(ips) == 0 {
			return nil, fmt.Errorf("socks5 udp: resolve bnd domain %q: %w", host, err)
		}
		return &net.UDPAddr{IP: ips[0], Port: int(port)}, nil
	default:
		return nil, fmt.Errorf("socks5 udp: unknown atyp %#x", atyp)
	}
}

// encodeUDPDatagram wraps a payload with a SOCKS5 UDP header destined
// for `target`. `out` is reused if it has enough capacity.
func encodeUDPDatagram(out []byte, target *net.UDPAddr, payload []byte) []byte {
	out = out[:0]
	// RSV(2) + FRAG(1) + ATYP(1)
	out = append(out, 0x00, 0x00, 0x00)

	if v4 := target.IP.To4(); v4 != nil {
		out = append(out, atypIPv4)
		out = append(out, v4...)
	} else {
		out = append(out, atypIPv6)
		out = append(out, target.IP.To16()...)
	}
	port := [2]byte{}
	binary.BigEndian.PutUint16(port[:], uint16(target.Port))
	out = append(out, port[:]...)
	out = append(out, payload...)
	return out
}

// decodeUDPDatagram parses a SOCKS5 UDP datagram returned by the relay.
// FRAG != 0 means the server is fragmenting; we drop those (RFC 1928
// allows clients to refuse fragmentation).
var (
	errFragmented        = errors.New("socks5 udp: fragmented datagrams unsupported")
	errShortDatagram     = errors.New("socks5 udp: datagram too short")
	errBadDatagramAtyp   = errors.New("socks5 udp: bad datagram atyp")
	errEmptyDatagramData = errors.New("socks5 udp: empty datagram data")
)

func decodeUDPDatagram(buf []byte) (*net.UDPAddr, []byte, error) {
	if len(buf) < 4 {
		return nil, nil, errShortDatagram
	}
	if buf[2] != 0x00 {
		return nil, nil, errFragmented
	}
	atyp := buf[3]
	switch atyp {
	case atypIPv4:
		if len(buf) < 4+4+2 {
			return nil, nil, errShortDatagram
		}
		ip := net.IPv4(buf[4], buf[5], buf[6], buf[7])
		port := binary.BigEndian.Uint16(buf[8:10])
		data := buf[10:]
		if len(data) == 0 {
			return nil, nil, errEmptyDatagramData
		}
		return &net.UDPAddr{IP: ip, Port: int(port)}, data, nil
	case atypIPv6:
		if len(buf) < 4+16+2 {
			return nil, nil, errShortDatagram
		}
		ip := make(net.IP, 16)
		copy(ip, buf[4:20])
		port := binary.BigEndian.Uint16(buf[20:22])
		data := buf[22:]
		if len(data) == 0 {
			return nil, nil, errEmptyDatagramData
		}
		return &net.UDPAddr{IP: ip, Port: int(port)}, data, nil
	case atypDomain:
		if len(buf) < 5 {
			return nil, nil, errShortDatagram
		}
		l := int(buf[4])
		if len(buf) < 5+l+2 {
			return nil, nil, errShortDatagram
		}
		host := string(buf[5 : 5+l])
		port := binary.BigEndian.Uint16(buf[5+l : 5+l+2])
		data := buf[5+l+2:]
		if len(data) == 0 {
			return nil, nil, errEmptyDatagramData
		}
		ips, err := net.LookupIP(host)
		if err != nil || len(ips) == 0 {
			return nil, nil, fmt.Errorf("socks5 udp: resolve datagram domain %q: %w", host, err)
		}
		return &net.UDPAddr{IP: ips[0], Port: int(port)}, data, nil
	default:
		return nil, nil, errBadDatagramAtyp
	}
}

// targetAddrFromID converts a gVisor TransportEndpointID local
// destination into a net.UDPAddr suitable for SOCKS5 wrapping.
func targetAddrFromID(localAddr string, localPort uint16) *net.UDPAddr {
	host := localAddr
	port := strconv.Itoa(int(localPort))
	udp, err := net.ResolveUDPAddr("udp", net.JoinHostPort(host, port))
	if err != nil {
		// IP literals from the netstack should always resolve; if they
		// don't, fall back to a parsed IP so we still return a usable
		// address rather than nil.
		ip := net.ParseIP(host)
		return &net.UDPAddr{IP: ip, Port: int(localPort)}
	}
	return udp
}
