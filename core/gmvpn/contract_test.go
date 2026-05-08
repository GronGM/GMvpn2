package gmvpn

import (
	"bytes"
	"os"
	"path/filepath"
	"testing"

	"github.com/xtls/xray-core/core"
)

// TestXrayAcceptsRustGeneratedConfigs is the cross-language contract
// test between the Rust-side gmvpn_core::xray::build_config and the
// Go-side Xray-core embedding API. Each fixture under
// testdata/configs/ is the deterministic dump of one realistic
// profile (one per supported protocol). If Xray-core ever stops
// accepting one of those shapes — or if the Rust generator drifts
// away from the schema — this test goes red.
//
// Regeneration of fixtures (run from the repo root):
//
//	cargo run -p gmvpn-core --example dump_configs -- \
//	    core/gmvpn/testdata/configs
//
// The Rust example uses fixed UUIDs and short, stable server
// addresses so the committed fixtures remain byte-stable across
// runs and only move on real surface changes.
func TestXrayAcceptsRustGeneratedConfigs(t *testing.T) {
	matches, err := filepath.Glob("testdata/configs/*.json")
	if err != nil {
		t.Fatalf("glob fixtures: %v", err)
	}
	if len(matches) == 0 {
		t.Fatal("no fixtures found under testdata/configs/")
	}

	for _, path := range matches {
		path := path
		t.Run(filepath.Base(path), func(t *testing.T) {
			body, err := os.ReadFile(path)
			if err != nil {
				t.Fatalf("read %s: %v", path, err)
			}
			cfg, err := core.LoadConfig("json", bytes.NewReader(body))
			if err != nil {
				t.Fatalf("xray-core rejected %s: %v", path, err)
			}
			if cfg == nil {
				t.Fatalf("xray-core returned nil config for %s", path)
			}
			// Build an Instance to make sure config wiring (not just
			// parsing) is happy. Don't Start it — we don't want any
			// listeners or outbound connections in CI; New() exercises
			// the same module init path that runtime takes.
			inst, err := core.New(cfg)
			if err != nil {
				t.Fatalf("xray-core New() failed for %s: %v", path, err)
			}
			if inst == nil {
				t.Fatalf("xray-core New() returned nil for %s", path)
			}
		})
	}
}
