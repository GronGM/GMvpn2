package tun2socks

import (
	"errors"
	"net"
	"testing"
)

func TestEncodeDecodeIPv4Roundtrip(t *testing.T) {
	target := &net.UDPAddr{IP: net.IPv4(1, 2, 3, 4), Port: 53}
	payload := []byte("hello dns")

	encoded := encodeUDPDatagram(nil, target, payload)
	if encoded[0] != 0x00 || encoded[1] != 0x00 || encoded[2] != 0x00 {
		t.Fatalf("encoded RSV/FRAG must be zero: %x", encoded[:3])
	}
	if encoded[3] != atypIPv4 {
		t.Fatalf("expected IPv4 ATYP, got %x", encoded[3])
	}

	addr, data, err := decodeUDPDatagram(encoded)
	if err != nil {
		t.Fatalf("decode: %v", err)
	}
	if !addr.IP.Equal(target.IP) || addr.Port != target.Port {
		t.Errorf("addr roundtrip: want %v, got %v", target, addr)
	}
	if string(data) != string(payload) {
		t.Errorf("payload roundtrip: want %q, got %q", payload, data)
	}
}

func TestEncodeDecodeIPv6Roundtrip(t *testing.T) {
	target := &net.UDPAddr{IP: net.ParseIP("2001:db8::1"), Port: 443}
	payload := []byte{0xde, 0xad, 0xbe, 0xef}

	encoded := encodeUDPDatagram(nil, target, payload)
	if encoded[3] != atypIPv6 {
		t.Fatalf("expected IPv6 ATYP, got %x", encoded[3])
	}

	addr, data, err := decodeUDPDatagram(encoded)
	if err != nil {
		t.Fatalf("decode: %v", err)
	}
	if !addr.IP.Equal(target.IP) || addr.Port != target.Port {
		t.Errorf("addr roundtrip: want %v, got %v", target, addr)
	}
	if string(data) != string(payload) {
		t.Errorf("payload roundtrip mismatch")
	}
}

func TestDecodeRejectsFragmented(t *testing.T) {
	// RSV(2) FRAG=1 ATYP=IPv4 + IP + port + 1 byte
	buf := []byte{0x00, 0x00, 0x01, atypIPv4, 1, 2, 3, 4, 0, 53, 0xaa}
	_, _, err := decodeUDPDatagram(buf)
	if !errors.Is(err, errFragmented) {
		t.Fatalf("expected errFragmented, got %v", err)
	}
}

func TestDecodeRejectsTooShort(t *testing.T) {
	for _, buf := range [][]byte{
		{},
		{0, 0, 0, atypIPv4, 1, 2, 3},
		{0, 0, 0, atypIPv6, 1, 2},
	} {
		if _, _, err := decodeUDPDatagram(buf); err == nil {
			t.Fatalf("expected error for short datagram %v", buf)
		}
	}
}

func TestDecodeRejectsBadAtyp(t *testing.T) {
	buf := []byte{0, 0, 0, 0xff, 0, 0, 0, 0, 0, 0, 0xaa}
	_, _, err := decodeUDPDatagram(buf)
	if !errors.Is(err, errBadDatagramAtyp) {
		t.Fatalf("expected errBadDatagramAtyp, got %v", err)
	}
}

func TestDecodeRejectsEmptyData(t *testing.T) {
	target := &net.UDPAddr{IP: net.IPv4(1, 1, 1, 1), Port: 1}
	encoded := encodeUDPDatagram(nil, target, []byte{}) // empty data
	_, _, err := decodeUDPDatagram(encoded)
	if !errors.Is(err, errEmptyDatagramData) {
		t.Fatalf("expected errEmptyDatagramData, got %v", err)
	}
}

func TestEncodeReusesProvidedBuffer(t *testing.T) {
	// Caller may pass a pre-allocated buffer (with capacity) to avoid
	// per-packet allocation; encode must respect it.
	pre := make([]byte, 0, 64)
	target := &net.UDPAddr{IP: net.IPv4(8, 8, 8, 8), Port: 53}
	out := encodeUDPDatagram(pre, target, []byte("dns"))
	if cap(out) < cap(pre) {
		t.Errorf("encode shrank capacity: was %d, got %d", cap(pre), cap(out))
	}
}

func TestTargetAddrFromIDIPv4(t *testing.T) {
	addr := targetAddrFromID("10.0.0.1", 8080)
	if addr == nil {
		t.Fatal("targetAddrFromID returned nil")
	}
	if !addr.IP.Equal(net.IPv4(10, 0, 0, 1)) {
		t.Errorf("ip want 10.0.0.1, got %v", addr.IP)
	}
	if addr.Port != 8080 {
		t.Errorf("port want 8080, got %d", addr.Port)
	}
}
