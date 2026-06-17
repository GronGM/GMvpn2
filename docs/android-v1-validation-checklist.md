# Android v1 validation checklist

The YAML block below is the machine-readable checklist for Android v1
device validation. Keep `status` as one of `pending`, `pass`, `fail`,
`pass_limited`, `blocked`, or `not_applicable`. Do not change
device-only items to `pass` without real device evidence.

```yaml
schema_version: 1
product: GMvpn2
platform: android
package_debug: com.gmvpn.client.debug
package_release: com.gmvpn.client
version_code: 1000005
version_name: 1.0.0-rc.5
rc_tag_candidate: android-v1.0.0-rc.5
overall_status: rc5_prerelease_for_apk_testing_profile_management_validation_limited_v100_pending_network_decision
rc_tag_approval_package:
  rc_candidate: android-v1.0.0-rc.1
  artifact_source_sha: "1775829107eac1066af911353fc17f8d11f24a18"
  docs_audit_head_after_artifact_verification: "a2fe00a5677665a44ab6b1396a50acf2e28f0d42"
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27632339860"
  workflow_run_id: 27632339860
  apk_aab_signed: true
  apk_signature_verified: true
  checksums_verified: true
  secrets_exposed: false
  tag_release_requires_explicit_approval: true
rc3_candidate:
  rc_candidate: android-v1.0.0-rc.3
  status: prerelease_for_apk_testing_physical_validation_pass_limited_not_production
  based_on_branch: codex/p1-play-compliance-and-device-validation
  artifact_source_sha: "dd10df9d3683fa41ccc628e5db0c186d029dd6ae"
  tag_object_sha: "65f3f0bd0d99a284291f178e4ac326300dc8d353"
  tag_target_sha: "dd10df9d3683fa41ccc628e5db0c186d029dd6ae"
  version_code: 1000003
  version_name: 1.0.0-rc.3
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27643689894"
  workflow_run_id: 27643689894
  release_blocker_cleanup:
    vpn_permission_cancel_state_fix: pass
    invalid_profile_persistent_error_ux: pass
  apk_aab_signed: true
  apk_signature_verified: true
  aab_signature_verified: true
  checksums_verified: true
  native_16kb_verified: true
  apk_zipalign_16kb_verified: true
  physical_validation_status: pass_limited
  physical_validation_date: "2026-06-16"
  physical_validation_device: "TECNO LG8n / Android 12 / API 31"
  physical_validation_summary:
    permission_cancel: pass
    invalid_profile_persistent_error: pass
    real_profile_used_redacted: true
    vpn_permission_allow: pass
    tunnel_connect: pass
    tunnel_disconnect: pass
    reconnect_cycles: pass
    app_restart_connected_disconnected: pass
    basic_browsing: pass
    ipv4_route: pass
    dns: pass_limited
    network_change: pass_limited
    udp_iperf: not_tested
    ipv6: not_tested
    crash_anr: pass
    log_privacy: pass
  rc3_tag_created: true
  github_release_created: true
  github_release_type: prerelease
  github_release_url: "https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3"
  github_release_latest_or_production: false
  google_play_published: false
  android_v100_tag_created: false
  release_assets:
    apk: GMvpn-android-v1.0.0-rc.3.apk
    sha256: GMvpn-android-v1.0.0-rc.3.apk.sha256
    aab_uploaded: false
  release_notes_state_limitations:
    dns: pass_limited
    udp_iperf: not_tested
    ipv6: not_tested
    google_play_publication: false
    final_production_release: false
rc4_candidate:
  rc_candidate: android-v1.0.0-rc.4
  status: prerelease_for_apk_testing_physical_validation_limited_not_production
  based_on_branch: codex/p1-play-compliance-and-device-validation
  artifact_source_sha: "1b99d5abc1a693584519eb201c49c466ca13a782"
  tag_object_sha: "86c4a5158ae9c784d5ad97bbee251e5e4b1444a5"
  tag_target_sha: "1b99d5abc1a693584519eb201c49c466ca13a782"
  privacy_fix_commit: "c6f635211a698c75df904152cbe0e3cb39f2e730"
  version_code: 1000004
  version_name: 1.0.0-rc.4
  purpose: "Privacy-sensitive tester-facing fix for saved profile labels."
  saved_profile_label_privacy:
    server_ip_hidden: pass_by_unit_tests
    host_domain_hidden: pass_by_unit_tests
    port_hidden: pass_by_unit_tests
    uuid_password_raw_uri_hidden: pass_by_unit_tests
    base64_query_like_secret_labels_hidden: pass_by_unit_tests
    secondary_label_endpoint_hidden: pass_by_unit_tests
    normal_ui_fallbacks:
      - "Профиль N"
      - "VLESS профиль"
      - "VMess профиль"
      - "Trojan профиль"
      - "Shadowsocks профиль"
    secondary_label_allowed_values:
      - "Профиль"
      - "VLESS"
      - "VMess"
      - "Trojan"
      - "Shadowsocks"
  local_validation_before_metadata_bump:
    date: "2026-06-17"
    git_diff_check: pass
    unit_tests: pass
    lint_debug: pass
    assemble_debug: pass
    assemble_release: pass
    debug_apk_install_launch_on_tecno_lg8n: pass
    manual_synthetic_ui_validation: limited_encrypted_profile_store_not_modified
  signed_workflow:
    workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27672658765"
    workflow_run_id: 27672658765
    artifact_source_sha: "1b99d5abc1a693584519eb201c49c466ca13a782"
    signed_apk_aab: true
    apk_signature_verified: true
    aab_verified: true
    checksums_verified: true
    native_16kb: true
    zipalign_16kb_verified: true
    apk_sha256: "bfd4c9232891ba93300bc6acebf9f1e191e3b57e1ba542d71c9c7129b0549b14"
    aab_sha256: "74c94c1f06dd7968b17036c10ab8a9bcdb3d14f238898ef81e4fbc475c895b63"
  physical_validation_signed_rc4:
    status: limited_install_launch_only
    install_launch: pass
    package_metadata: pass
    crash_anr_scan: pass
    log_privacy_scan: pass
    visual_profile_list_check: limited_manual
    connect_disconnect_reconnect: pending
    required_checks:
      - saved_profile_list_no_endpoint_data
      - secondary_line_no_endpoint_data
      - approved_profile_connect_disconnect_reconnect
  known_limitations:
    dns: pass_limited
    udp_iperf: not_tested
    ipv6: not_tested
  network_validation_bench:
    runbook: docs/android-network-validation-bench.md
    controlled_udp_iperf_required_for_full_pass: true
    dns_requires_two_independent_methods_for_full_pass: true
    ipv6_requires_real_external_ipv6_network: true
    random_public_iperf_servers_allowed_as_release_evidence: false
  rc4_tag_created: true
  github_release_created: true
  github_release_type: prerelease
  github_release_url: "https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4"
  github_release_latest_or_production: false
  release_assets:
    apk: GMvpn-android-v1.0.0-rc.4.apk
    sha256: GMvpn-android-v1.0.0-rc.4.apk.sha256
    aab_uploaded: false
  google_play_published: false
  android_v100_tag_created: false
  approval_phrase_used: "APPROVE RC TAG android-v1.0.0-rc.4 ON 1b99d5abc1a693584519eb201c49c466ca13a782"
rc5_candidate:
  rc_candidate: android-v1.0.0-rc.5
  status: prerelease_for_apk_testing_profile_management_validation_limited_not_production
  based_on_branch: codex/p1-play-compliance-and-device-validation
  artifact_source_sha: "15d0a7f5fd691f9bf517a05ac867fc661be8c233"
  tag_object_sha: "16503777e38328d890ee78e47b27f46778f72e13"
  tag_target_sha: "15d0a7f5fd691f9bf517a05ac867fc661be8c233"
  tag_created: true
  github_release_created: true
  github_release_type: prerelease
  github_release_url: "https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.5"
  github_release_latest_or_production: false
  google_play_published: false
  android_v100_tag_created: false
  version_code: 1000005
  version_name: 1.0.0-rc.5
  purpose: "Profile/import/diagnostics UX test build after RC4 privacy fix."
  changelog:
    - "Profile management UI: safe names, details, rename, delete confirmation, active-profile reset."
    - "Safe import preview avoids endpoint, raw URI, UUID, password, token, and query-like labels."
    - "Diagnostics report remains redacted and avoids raw profiles, raw logs, endpoints, and credentials."
    - "Network validation bench docs define controlled UDP, full DNS, and IPv6 evidence requirements."
    - "RC4 saved-profile privacy fix is kept."
  local_review_before_metadata_bump:
    date: "2026-06-17"
    profile_metadata_privacy: pass_by_code_review
    import_preview_privacy: pass_by_code_review
    diagnostics_privacy: pass_by_code_review
    delete_active_resets_active_index: pass_by_code_review
    no_fake_vpn_success: pass_by_code_review
  debug_physical_synthetic_validation:
    date: "2026-06-17"
    device: "TECNO LG8n / Android 12 / API 31"
    package: com.gmvpn.client.debug
    app_open: pass
    synthetic_profile_add: pass
    profile_list_safe_names: pass
    active_profile_selection: pass
    rename: pass
    delete_active_confirmation: pass
    delete_active_resets_remaining_profile_active: pass
    endpoint_or_secret_visible_in_checked_ui: false
    import_preview_subscription: blocked_no_safe_https_synthetic_endpoint
    diagnostics_report_content_readback: limited_shell_clipboard_unavailable
    real_profile_connect_disconnect_reconnect: not_tested_no_approved_profile_in_this_run
    screenshots_or_raw_ui_with_real_profiles_committed: false
  signed_workflow:
    workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27679203026"
    workflow_run_id: 27679203026
    artifact_source_sha: "15d0a7f5fd691f9bf517a05ac867fc661be8c233"
    signed_apk_aab: true
    apk_signature_verified: true
    aab_verified: true
    bundletool_validate: true
    checksums_verified: true
    native_16kb: true
    zipalign_16kb_verified: true
    metadata_verified: true
    apk_sha256: "ae2ed403818039d90d8f926d9bd8baaa1815e21e10676e9147fcdb509f2c01c8"
    aab_sha256: "b6484b8846fd0dd5288bfc9a977576d91d8d9dec5c0cd4801a0d4818025e7942"
  physical_validation_signed_rc5:
    status: limited_install_launch_no_ui_dump
    install_launch: pass
    package_metadata: pass
    crash_anr_scan: pass
    log_privacy_scan: pass
    profile_management_smoke: pass_debug_synthetic_and_unit_code_review
    diagnostics_privacy_smoke: pass_unit_code_review_and_log_privacy_scan
    connect_disconnect_reconnect: not_tested_no_approved_profile_in_signed_rc5_run
    release_app_ui_dump: not_performed_to_avoid_real_profile_exposure
  known_limitations:
    dns: pass_limited
    udp_iperf: pass_limited
    ipv6: not_tested
  network_validation_evidence_plan:
    runbook: docs/android-network-validation-bench.md
    plan_updated: "2026-06-17"
    approved_controlled_endpoint_required: true
    random_public_iperf_servers_allowed_as_release_evidence: false
    udp_template_added: true
    dns_two_method_template_added: true
    ipv6_real_network_template_added: true
    raw_evidence_git_policy: "Keep raw logs, endpoints, screenshots, profiles, and packet captures out of git."
  release_assets:
    apk: GMvpn-android-v1.0.0-rc.5.apk
    sha256: GMvpn-android-v1.0.0-rc.5.apk.sha256
    aab_uploaded: false
  approval_phrase_used: "APPROVE RC TAG android-v1.0.0-rc.5 ON 15d0a7f5fd691f9bf517a05ac867fc661be8c233"
  release_rules:
    github_release_pre_release_only: true
    github_release_latest_or_production: false
    aab_uploaded_for_testers: false
    do_not_publish_google_play: true
post_rc5_source_hardening:
  date: "2026-06-17"
  branch: codex/p1-play-compliance-and-device-validation
  status: local_tests_pass_not_released
  public_rc5_assets_changed: false
  github_release_created: false
  tags_changed: false
  changes:
    diagnostics_profile_uri_endpoint_redaction: pass
    diagnostics_http_url_ipv4_host_context_redaction: pass
  tests:
    unit_tests: pass
    lint_debug: pass
    assemble_debug: pass
  release_impact: >
    Source change after RC5; ship only via a later version bump, signed
    workflow, verification, and explicit RC approval.
  rc6_required_by_this_change_alone: false
post_rc5_network_stability_attempt:
  date: "2026-06-17"
  branch: codex/p1-play-compliance-and-device-validation
  status: device_endpoint_ready_android_side_udp_client_missing
  github_issues_checked: true
  new_issues: 0
  preflight_script: scripts/validation/preflight-windows.ps1
  network_runner: scripts/validation/run-network-validation-windows.ps1
  adb_found: pass
  authorized_device_found: pass
  authorized_device_previous_status: pass
  adb_source: standard_android_sdk_platform_tools
  device_serial_printed: false
  device_serial_masked: true
  iperf3_found: pass
  iperf3_source: winget_user_portable_install_or_path
  approved_endpoint_env_present: true
  controlled_endpoint_vps_configured: pass
  vps_os: ubuntu_24_04
  vps_ipv4_present: true
  vps_ipv6_present: true
  ssh_password_rotated: true
  iperf3_service_active: pass
  firewall_tcp_5201: pass
  firewall_udp_5201: pass
  endpoint_connectivity_tcp: pass
  endpoint_connectivity_udp: pass
  endpoint_connectivity_path: windows_pc_to_vps_only
  endpoint_connectivity_udp_duration_seconds: 30
  endpoint_connectivity_udp_target_bitrate: 5M
  endpoint_connectivity_udp_packet_loss_percent: 0
  endpoint_connectivity_udp_jitter_ms: 4.249
  endpoint_redacted: true
  app_version_name: 1.0.0-rc.5
  app_version_code: 1000005
  android_release: "12"
  android_api: 31
  vpn_connected_before_udp: false
  vpn_connected_after_udp: false
  android_iperf3_client: missing
  android_termux_client: not_installed
  android_nc_or_toybox: present_not_used
  controlled_udp_iperf: pass_limited
  udp_path: endpoint_connectivity_only_not_android_vpn_path
  controlled_udp_iperf_blocker: >
    The controlled VPS endpoint is configured and reachable over TCP and
    UDP from Windows, and ADB sees the physical RC5 device. However,
    GMvpn VPN was not connected, Termux is not installed, Android-side
    iperf3 is missing, and no safe Android VPN-path UDP client method was
    executed. Do not treat Windows-to-VPS iperf as release-grade Android
    VPN-path UDP evidence.
  next_android_udp_method: >
    Install Termux from F-Droid and install iperf3 there, or add a
    debug-only project-owned Android UDP helper outside production UI.
  full_dns_leak_audit: pass_limited
  full_dns_leak_audit_blocker: >
    No fresh two-method DNS evidence was captured because GMvpn VPN was
    not connected.
  ipv6: not_tested
  ipv6_blocker: >
    No real external IPv6 device/network baseline was established.
  stability_smoke: pass_limited
  stability_smoke_previous_status: pass_limited
  stability_smoke_limitation: >
    The latest runner captured Android release/API, app version, app
    process state, and logcat crash/ANR markers only. Manual app restart,
    reconnect, no-profile, diagnostics copy/export, and log privacy
    checks remain pending.
  raw_evidence_path: ".local/validation/<timestamp>/"
  redacted_summary_path: ".local/validation/<timestamp>/summary-redacted.md"
  raw_logs_committed: false
  profiles_or_credentials_committed: false
  apk_aab_committed: false
  release_changed: false
  tags_changed: false
  google_play_published: false
  rc6_required_by_this_attempt: false
post_rc5_android_udp_matrix:
  date: "2026-06-17"
  branch: codex/p1-play-compliance-and-device-validation
  status: android_side_udp_pass_limited
  device: TECNO_LG8n_android_12_api_31
  app_version_name: 1.0.0-rc.5
  app_version_code: 1000005
  android_side_udp_client: termux_iperf3
  termux_source: official_termux_app_github_prerelease_v0_119_0_beta_3
  termux_apk_sha256_verified: true
  iperf3_version: "3.21"
  approved_subscription_imported: true
  imported_profiles: 4
  skipped_profiles: 0
  vpn_permission_allow: pass
  vpn_connected_before_tests: true
  vpn_connected_after_tests: true
  post_udp_crash_anr_scan: pass
  post_udp_crash_anr_scan_scope: logcat_tail_4000_lines_case_sensitive_markers
  endpoint_redacted: true
  payload_bytes: 1200
  duration_seconds_per_run: 30
  raw_evidence_committed: false
  matrix:
    - bitrate: 1M
      runs: 3
      packet_loss_min_avg_max_percent: "0 / 0 / 0"
      jitter_min_avg_max_ms: "1.370 / 5.574 / 8.041"
      result: pass_limited
    - bitrate: 2M
      runs: 3
      packet_loss_min_avg_max_percent: "0 / 14.333 / 43"
      jitter_min_avg_max_ms: "0.941 / 9.110 / 22.166"
      result: pass_limited_outlier
    - bitrate: 3M
      runs: 3
      packet_loss_min_avg_max_percent: "0 / 0.004 / 0.011"
      jitter_min_avg_max_ms: "1.037 / 2.506 / 3.511"
      result: pass_limited
    - bitrate: 5M
      runs: 3
      packet_loss_min_avg_max_percent: "0 / 0.041 / 0.096"
      jitter_min_avg_max_ms: "0.906 / 1.854 / 2.477"
      result: best_stable_pass_limited
  best_stable_udp_result: "5M, payload 1200 bytes, 3 runs, max packet loss 0.096%, max jitter 2.477 ms, VPN stayed connected."
  controlled_udp_iperf: pass_limited
  controlled_udp_iperf_limitation: >
    Android-side evidence now exists through Termux iperf3 over active
    GMvpn, but keep the status pass_limited because no formal release
    loss threshold has been approved and the 2M row had one high-loss
    outlier.
  full_dns_leak_audit: pass_limited
  full_dns_leak_audit_limitation: >
    Two Android-side resolver-discovery methods were run while GMvpn
    stayed connected and no private/router DNS was observed, but
    provider/country attribution and browser DNS leak page evidence were
    not completed.
  ipv6: not_tested
  ipv6_limitation: >
    No real external IPv6 baseline was collected before VPN. Current
    probe did not observe a global IPv6 route, so IPv6 must not be marked
    pass.
  release_changed: false
  tags_changed: false
  google_play_published: false
  rc6_required_by_this_attempt: false
rc3_tag_approval_package:
  candidate: android-v1.0.0-rc.3
  tag_object_sha: "65f3f0bd0d99a284291f178e4ac326300dc8d353"
  tag_target_sha: "dd10df9d3683fa41ccc628e5db0c186d029dd6ae"
  artifact_source_sha: "dd10df9d3683fa41ccc628e5db0c186d029dd6ae"
  validation_docs_head: "b129c93ff65564da9543a7350779d0af70daf068"
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27643689894"
  workflow_run_id: 27643689894
  signed_apk_aab: true
  sdk35: true
  native_16kb: true
  apk_signature_verified: true
  aab_verified: true
  checksums_verified: true
  zipalign_16kb_verified: true
  physical_android_install_connect_disconnect_reconnect: true
  dns: pass_limited
  ipv4_route: pass
  udp_iperf: not_tested
  ipv6: not_tested
  log_privacy: pass
  github_release_authorized: "pre-release only for tester APK"
  rc3_tag_created: true
  final_v100_tag_authorized: false
  required_approval_phrase: "APPROVE RC TAG android-v1.0.0-rc.3 ON dd10df9d3683fa41ccc628e5db0c186d029dd6ae WITH UDP_IPV6_LIMITATIONS_ACCEPTED"
  tag_target_rule: "Workflow run 27643689894 artifacts are tied to android-v1.0.0-rc.3 at dd10df9d3683fa41ccc628e5db0c186d029dd6ae. Do not tag validation/docs commits unless a new signed workflow is rerun on that exact commit."
post_rc3_v100_network_validation:
  date: "2026-06-16"
  latest_attempt_date: "2026-06-17"
  adb_device_seen: true
  latest_attempt_device_seen: true
  latest_attempt_release_package_installed: true
  latest_attempt_version_code: 1000003
  latest_attempt_version_name: 1.0.0-rc.3
  latest_attempt_target_sdk: 35
  latest_attempt_active_vpn_internet_observed: false
  latest_attempt_ipv6_route_observed: false
  controlled_udp_iperf: blocked
  controlled_udp_iperf_blocker: "Local iperf3 tooling is available, but no approved controlled iperf3 endpoint was provided and no GMVPN_IPERF_* or IPERF3_* endpoint variable was present."
  dns_leak_audit: pass_limited
  dns_leak_audit_limitation: "Prior signed RC3 evidence was browser-level and found no local ISP/router markers, but follow-up checks did not run two fresh independent DNS methods while an active VPN Internet network was observed."
  ipv6: not_tested
  ipv6_blocker: "No real external IPv6 baseline was established for signed RC3 follow-up; latest sanitized route check did not observe an IPv6 route."
  evidence_plan_updated: "2026-06-17"
  evidence_plan_runbook: docs/android-network-validation-bench.md
  evidence_plan_status: "Redacted templates and endpoint/network gates are ready; validation evidence is still missing."
  raw_logs_committed: false
  profiles_or_credentials_committed: false
  apk_aab_committed: false
  raw_ip_or_connectivity_dump_committed: false
  github_release_created: false
  android_v100_tag_created: false
  release_decision: "Block unrestricted v1.0.0 until UDP/full-DNS/real-IPv6 evidence is complete, or release MVP only with explicit acceptance of remaining UDP/DNS/IPv6 limitations."
github_actions_node24_readiness:
  status: pass
  maintenance_commit: "9786fe3fa23080b8c9aff80f8e26e88bd38f87fc"
  node20_warning_source_workflow_run: 27643689894
  proof_workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27648312721"
  proof_workflow_run_id: 27648312721
  proof_head_sha: "5a7aca93e34dac3aa606711806669af75a99d067"
  proof_rc_tag_input: android-v1.0.0-rc.3-node24-proof
  proof_version_name_input: 1.0.0-rc.3
  proof_result: pass
  node20_warning_remains: false
  github_release_created: false
  android_v100_tag_created: false
  updated_refs:
    - actions/checkout@v6
    - actions/setup-java@v5
    - actions/setup-go@v6
    - actions/cache@v5
    - actions/upload-artifact@v7
    - actions/download-artifact@v8
    - android-actions/setup-android@v4
  unchanged_refs:
    - dtolnay/rust-toolchain@stable
    - nttld/setup-ndk@v1
    - taiki-e/install-action@cargo-llvm-cov
    - taiki-e/install-action@cargo-audit
v100_release_gate:
  unrestricted:
    approved: false
    ready: false
    required_approval_phrase: "APPROVE UNRESTRICTED V1.0.0 AFTER UDP_DNS_IPV6_PASS"
    requirements:
      node24_workflow_proof: pass
      controlled_udp_iperf: pass_limited
      full_dns_leak_audit: pass_limited
      ipv6: not_tested
      signed_final_workflow_from_release_commit: pending
      physical_validation: pass_limited
      play_vpnservice_declaration_draft: ready
    decision: blocked_until_udp_dns_ipv6_and_final_signed_workflow_pass
  mvp_limited:
    approved: false
    ready_for_approval_review: true
    required_approval_phrase: "APPROVE MVP V1.0.0 WITH UDP_DNS_IPV6_LIMITATIONS_ACCEPTED"
    limitations:
      udp_iperf: pass_limited
      dns: pass_limited
      ipv6: not_tested
    release_notes_required: "State MVP/internal/limited validation and list UDP/DNS/IPv6 limitations."
    rollout_required: "Start with Play internal testing, not broad production."
    final_signed_workflow_required_before_release: true
    release_notes_required_before_release: true
  rules:
    without_exact_phrase_do_not_create_android_v100_tag: true
    without_final_signed_workflow_do_not_create_github_release: true
    do_not_reuse_node24_proof_artifacts_as_final_release: true
final_v100_preparation_plan:
  version_bump_not_committed: true
  planned_version_code: greater_than_rc5_1000005
  planned_version_name: 1.0.0
  final_workflow:
    workflow: android-release.yml
    rc_tag_input: android-v1.0.0
    version_name_input: 1.0.0
    must_run_from_exact_final_release_source_sha: true
  required_artifact_checks:
    checksums: pending
    apk_signature: pending
    aab_verification: pending
    native_16kb_elf_alignment: pending
    zipalign_p_16: pending
    apk_metadata_version_code_greater_than_rc5_1000005: pending
    apk_metadata_version_name_1_0_0: pending
    apk_metadata_target_sdk_35: pending
  android_v100_tag_before_final_workflow_pass: forbidden
  github_release_before_final_workflow_pass: forbidden
rc2_candidate:
  rc_candidate: android-v1.0.0-rc.2
  status: physical_validation_failed_not_tagged_not_released
  based_on_branch: codex/p1-play-compliance-and-device-validation
  artifact_source_sha: "4d15f3054384cd6a1ee7ae954491ade0e7a98370"
  version_code: 1000002
  version_name: 1.0.0-rc.2
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27640095772"
  workflow_run_id: 27640095772
  apk_aab_signed: true
  apk_signature_verified: true
  aab_signature_verified: true
  checksums_verified: true
  native_16kb_verified: true
  apk_zipalign_16kb_verified: true
  physical_validation_status: failed
  physical_validation_date: "2026-06-16"
  physical_validation_device: "physical Android 12 / API 31"
  physical_validation_blockers:
    - "VPN permission cancel path leaves UI stuck in Preparing with Disconnect visible"
    - "Invalid-profile start fails safely in service logs, but the user-visible error was not persistently visible in the captured UI"
    - "No approved real VPN profile/server was used, so signed RC2 tunnel, DNS, IPv4 route, UDP, and IPv6 validation remain pending"
  rc1_tag_unchanged: true
  rc2_tag_created: false
  github_release_created: false
items:
  - id: apk-debug-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleDebug"
    evidence: "2026-06-15: Gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest passed; debug APK at clients/android/app/build/outputs/apk/debug/app-debug.apk. 2026-06-15 RC packaging step bumped Android metadata to versionName 1.0.0-rc.1 / versionCode 1000001; the post-packaging Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease passed. Physical-device evidence remains tied to the earlier audited debug build and no production/public release is implied."

  - id: native-artifacts-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "./scripts/build-android-libs.sh"
    evidence: "2026-06-15: Rust libgmvpn_ffi.so rebuilt for arm64-v8a/armeabi-v7a/x86/x86_64 via cargo-ndk; Kotlin UniFFI bindings regenerated; Go gmvpn.aar rebuilt via gomobile bind. Local Windows Git Bash did not have GNU make, so the Makefile-equivalent cargo/uniffi/gomobile commands were run directly after updating gomobile and moving Go temp/cache to D:. CI runs scripts/build-android-libs.sh on Ubuntu, where make is available. Artifacts remain ignored under core/build, shared/target, clients/android/app/libs, and clients/android/app/src/main/jniLibs."

  - id: apk-release-build
    priority: P0
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleRelease :app:bundleRelease --stacktrace"
    evidence: "2026-06-15: initial release build failed in R8 because JNA optional desktop/AWT helpers referenced java.awt classes unavailable on Android. Added narrow dontwarn rules for java.awt.Component, GraphicsEnvironment, HeadlessException, and Window in app/proguard-rules.pro. :app:assembleRelease and :app:bundleRelease then passed, producing app-release-unsigned.apk and app-release.aab. 2026-06-15 RC packaging step set versionName 1.0.0-rc.1 / versionCode 1000001 and prepared manual android-release.yml packaging/signing workflow; the post-packaging Gradle command passed and aapt2 confirmed release package com.gmvpn.client versionCode 1000001 versionName 1.0.0-rc.1. Local apksigner verification of app-release-unsigned.apk returned DOES NOT VERIFY as expected without RELEASE_KEYSTORE_* env vars. 2026-06-16 manual workflow run 27632339860 produced signed APK/AAB artifacts; public distribution still requires explicit tag/release approval."

  - id: release-signing-workflow
    priority: P0
    status: pass
    requires_physical_device: false
    command: "gh workflow run android-release.yml --repo GronGM/GMvpn2 -f rc_tag=android-v1.0.0-rc.1 -f version_name=1.0.0-rc.1"
    evidence: "2026-06-16: manual-only android-release.yml run 27632339860 succeeded from branch claude/relaxed-euler-1Vr2R at 1775829107eac1066af911353fc17f8d11f24a18. It did not create git tags or GitHub Releases. It uploaded unsigned audit artifact gmvpn-android-android-v1.0.0-rc.1-unsigned-audit and signed artifact gmvpn-android-android-v1.0.0-rc.1-signed as GitHub Actions artifacts. Downloaded signed APK verified locally with apksigner using APK Signature Scheme v2 and one signer; signed-rc.sha256 matched the signed APK/AAB, and unsigned-audit.sha256 matched all five unsigned audit files. Local copy: .local/release-artifacts/android-v1.0.0-rc.1/."

  - id: target-sdk-35-play-migration
    priority: P1
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace"
    evidence: "2026-06-16: installed local Android SDK platform android-35 and build-tools 35.0.0; bumped compileSdk and targetSdk from 34 to 35; kept minSdk 26; Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace passed. Limitation: connected instrumentation tests and signed SDK-35 workflow artifacts still need separate validation before any Play-bound artifact is approved."

  - id: play-vpnservice-declaration
    priority: P1
    status: pass_limited
    requires_physical_device: false
    manual_step: "Prepare Play Console VpnService declaration from docs/android-play-compliance-and-validation.md"
    evidence: "2026-06-16: prepared Play Console VpnService declaration draft in docs/android-play-compliance-and-validation.md. Current audit: VPN is core functionality; manifest service is private, protected by BIND_VPN_SERVICE, and declares android.net.VpnService; repo scan found no ad, analytics, crash-reporting, hidden telemetry, or traffic monetization SDK in the Android dependency/config surface. Limitation: Play listing copy, screenshots, Data safety answers, and final product/privacy review are still pending."

  - id: android-15-fgs-vpnservice-audit
    priority: P1
    status: pass_limited
    requires_physical_device: false
    command: "rg -n \"foregroundServiceType|FOREGROUND_SERVICE|dataSync|mediaProcessing|BOOT_COMPLETED|BroadcastReceiver|onTimeout|startForeground|VpnService\" clients/android/app/src/main"
    evidence: "2026-06-16: audited AndroidManifest, GmvpnVpnService, and TunnelController. GmvpnVpnService is the only foreground service, uses foregroundServiceType=systemExempted, declares BIND_VPN_SERVICE and android.net.VpnService intent filter, is exported=false, and is started through user/VpnService flows. No dataSync/mediaProcessing foreground service type, BOOT_COMPLETED receiver, or background boot auto-start path was found. Limitation: long-running signed-release physical tunnel validation still required."

  - id: native-16kb-page-size-readiness
    priority: P1
    status: pass
    requires_physical_device: false
    command: "scripts/check-android-16kb-elf-alignment.sh <release-apk-or-aab>"
    evidence: "2026-06-16: post-RC/P1 source pipeline updated for Android NDK r28c, gomobile CGO_LDFLAGS, cargo-ndk RUSTFLAGS, and JNA 5.17.0. Local Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace passed. scripts/check-android-16kb-elf-alignment.sh passed against local unsigned APK/AAB and against downloaded signed RC2 candidate APK/AAB from workflow run 27640095772: all 23 packaged .so entries in each artifact had minimum LOAD align 0x4000. zipalign -c -P 16 passed for unsigned and signed APKs. Limitation: existing RC1 signed artifacts are unchanged; RC2 tag/release is not approved."

  - id: release-signing-workflow-rc2-candidate
    priority: P1
    status: pass
    requires_physical_device: false
    command: "gh workflow run android-release.yml --repo GronGM/GMvpn2 --ref codex/p1-play-compliance-and-device-validation -f rc_tag=android-v1.0.0-rc.2 -f version_name=1.0.0-rc.2"
    evidence: "2026-06-16: manual-only android-release.yml run 27640095772 succeeded from branch codex/p1-play-compliance-and-device-validation at 4d15f3054384cd6a1ee7ae954491ade0e7a98370. It did not create git tags or GitHub Releases. It uploaded unsigned audit artifact gmvpn-android-android-v1.0.0-rc.2-unsigned-audit and signed artifact gmvpn-android-android-v1.0.0-rc.2-signed as GitHub Actions artifacts. CI verified unsigned native ELF 16 KB alignment, unsigned APK zipalign -P 16, signed native ELF 16 KB alignment, signed APK apksigner verification, and signed APK zipalign -P 16. Local download under .local/release-artifacts/android-v1.0.0-rc.2/ verified signed-rc.sha256 and unsigned-audit.sha256, APK v2 signature, AAB jarsigner verification with expected self-signed/untimestamped RC certificate warnings, signed APK/AAB 16 KB ELF alignment, signed APK zipalign -P 16, and aapt metadata versionCode 1000002 / versionName 1.0.0-rc.2 / minSdk 26 / targetSdk 35. Signed APK SHA-256: 4f8901d00af6f09792b39584168d758466b1e16174d86a35e83e6a27709334c5. Signed AAB SHA-256: 92da35514e603e1474edd42c665a9192c702bc49c9c2f941f939abb5282fc7e2. RC1 tag remains unchanged; RC2 tag and GitHub Release were not created."

  - id: release-signing-workflow-rc3-candidate
    priority: P0
    status: pass
    requires_physical_device: false
    command: "gh workflow run android-release.yml --repo GronGM/GMvpn2 --ref codex/p1-play-compliance-and-device-validation -f rc_tag=android-v1.0.0-rc.3 -f version_name=1.0.0-rc.3"
    evidence: "2026-06-16: manual-only android-release.yml run 27643689894 succeeded from branch codex/p1-play-compliance-and-device-validation at dd10df9d3683fa41ccc628e5db0c186d029dd6ae. It did not create git tags or GitHub Releases. It uploaded unsigned audit artifact gmvpn-android-android-v1.0.0-rc.3-unsigned-audit and signed artifact gmvpn-android-android-v1.0.0-rc.3-signed as GitHub Actions artifacts. CI verified unsigned native ELF 16 KB alignment, unsigned APK zipalign -P 16, signed native ELF 16 KB alignment, signed APK apksigner verification, signed APK zipalign -P 16, and signed checksum generation. Local download under .local/release-artifacts/android-v1.0.0-rc.3/ verified signed-rc.sha256 and unsigned-audit.sha256, APK v2 signature with one signer, AAB jarsigner verification with expected self-signed/untimestamped certificate warnings, signed APK/AAB 16 KB ELF alignment for all 23 packaged .so entries, signed APK zipalign -P 16, and aapt metadata versionCode 1000003 / versionName 1.0.0-rc.3 / minSdk 26 / targetSdk 35. Signed APK SHA-256: 1f5c819e1eca9bb77986878241a0821beb5ec87f6e088fb966c686c853a99acf. Signed AAB SHA-256: 770eb861c9f5d75c58074b264f7841c4896b53c53c610ce1dac3a2739d3776da. After explicit approval, annotated tag android-v1.0.0-rc.3 was created and pushed at tag object 65f3f0bd0d99a284291f178e4ac326300dc8d353 targeting dd10df9d3683fa41ccc628e5db0c186d029dd6ae. GitHub Release was not created."

  - id: signed-release-apk-physical-validation
    priority: P1
    status: pass_limited
    requires_physical_device: true
    command: "adb install -r .local/release-artifacts/android-v1.0.0-rc.3/gmvpn-android-android-v1.0.0-rc.3-signed/outputs/apk/release/app-release.apk"
    evidence: "2026-06-16: signed RC3 APK installed successfully on physical TECNO LG8n with adb state device, Android 12/API 31, and package metadata versionCode 1000003 / versionName 1.0.0-rc.3 / minSdk 26 / targetSdk 35. No emulator was used. Android Settings -> VPN -> GMvpn -> Forget VPN reset consent for permission checks. Permission cancel passed: Android VPN dialog appeared, tapping Cancel returned to Disconnected/Connect, no GmvpnVpnService start was logged, no fake Connected state appeared, and the UI did not remain stuck in Preparing. Invalid-profile UX passed with a non-secret dummy https profile: service failed safely with unsupported protocol, no fake Connected state, visible persistent error remained after Idle, and Dismiss removed it. A redacted approved subscription URL from ignored .local/test-profile.txt decoded to 4 profiles / 0 skipped; raw URL/profile contents were not printed or committed. VPN permission allow passed; valid profile connected, UI reached Connected/Disconnect, basic browsing to example.com worked, three disconnect/reconnect cycles passed, app relaunch while connected and disconnected preserved truthful UI state, Cloudflare browser trace changed from RU baseline to NL VPN exit, and a short Wi-Fi disable/restore network-change check stayed error-free. DNS evidence is pass_limited from browser-based DNS page parsing with no local ISP/router markers, but not a full lab DNS audit. UDP/iperf was not tested because no controlled endpoint was provided. IPv6 was not tested because the browser trace used IPv4 before and during VPN, so no real external IPv6 baseline was verified. Raw logcat/UI/connectivity dumps stayed under ignored .local/device-validation/rc3/ and were not committed. GMvpn-related privacy scan found no private keys, VPN URIs, UUIDs, password, token, Authorization, Cookie, X-Api-Key, pbk, sid, or spx patterns. Crash scan found no FATAL EXCEPTION, AndroidRuntime crash, or ANR for com.gmvpn.client. RC3 tag was later created after explicit approval; GitHub Release was not created."

  - id: full-dns-leak-audit-rc3
    priority: P1
    status: pass_limited
    requires_physical_device: true
    manual_step: "Run at least two independent DNS leak methods while signed RC3 VPN is connected"
    evidence: "2026-06-16 signed RC3 physical validation included browser-level DNS evidence with no local ISP/router markers. Post-RC3 follow-up did not run two fresh independent DNS methods while VPN state was known-good, so DNS remains pass_limited for v1.0.0 approval. Do not mark full pass until local ISP/router DNS is explicitly absent across at least two independent methods and only redacted provider/country-level evidence is recorded."

  - id: controlled-udp-iperf-validation
    priority: P1
    status: pass_limited
    requires_physical_device: true
    manual_step: "Run controlled iperf3 UDP validation through an approved test endpoint"
    evidence: "2026-06-16 signed RC3 physical validation did not run controlled UDP/iperf because no approved iperf3 endpoint was provided. 2026-06-17 follow-up configured an approved controlled endpoint and installed Termux iperf3 on the physical RC5 Android device. Android-side UDP matrix over active GMvpn used payload 1200 bytes, 30 seconds per run, 3 runs each at 1M/2M/3M/5M, endpoint redacted, and VPN connected before/after each run. Best stable result was 5M with max packet loss 0.096% and max jitter 2.477 ms. Keep status pass_limited because no formal release loss threshold has been approved and the 2M row had one high-loss outlier."

  - id: real-ipv6-network-validation
    priority: P1
    status: blocked
    requires_physical_device: true
    manual_step: "Run IPv6 leak validation on a network with a real public IPv6 baseline"
    evidence: "2026-06-16 signed RC3 browser trace used IPv4 before and during VPN, so no real external IPv6 baseline was verified. Post-RC3 follow-up did not establish a real external IPv6 network. Do not mark pass until a real IPv6 baseline is proven and active-VPN behavior either tunnels IPv6 or fails closed without raw IPv6 fallback. If v1.0.0 proceeds first, this limitation needs explicit release approval."

  - id: android-lint
    priority: P0
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:lintDebug --stacktrace"
    evidence: "2026-06-15: initial lintDebug failed on ForegroundServicePermission for android:foregroundServiceType=\"systemExempted\" even though GmvpnVpnService is a VpnService and already declares FOREGROUND_SERVICE_SYSTEM_EXEMPTED. Added a narrow tools:ignore=\"ForegroundServicePermission\" on the VPN service instead of adding unrelated exact-alarm permissions. :app:lintDebug then passed. Limitation: this is an explicit lint suppression for the VPN service exemption, not a new runtime permission."

  - id: emulator-smoke-tests
    priority: P1
    status: pass
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:connectedDebugAndroidTest"
    evidence: "2026-06-15: VpnTunnelSmokeTest passed on emulator gmvpn_api34 and physical TECNO LG8n Android 12/API 31; reports under clients/android/app/build/reports/androidTests/connected/debug/"

  - id: install-debug-apk
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb install -r clients/android/app/build/outputs/apk/debug/app-debug.apk"
    evidence: "2026-06-15: adb install succeeded on TECNO LG8n, Android 12/API 31; package com.gmvpn.client.debug versionName 0.0.1 targetSdk 34"

  - id: import-vless-reality-profile
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Import a redacted throwaway VLESS+Reality test profile"
    evidence: "2026-06-15: TECNO LG8n imported HTTPS subscription from .local/test-profile.txt through the normal UI confirmation flow; 4 VLESS+Reality profiles saved; redacted evidence in artifacts/android-diagnostics/20260615-194415/"

  - id: xray-version-visible
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Open About and verify Engine: Xray-core is not unbundled"
    evidence: "2026-06-15: About screen on TECNO LG8n showed Xray-core 26.3.27; redacted UI dump artifacts/android-diagnostics/20260615-194415/16-about-engine-version.xml"

  - id: vpn-permission-flow
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Tap Connect before permission is granted; approve Android VPN prompt"
    evidence: "2026-06-15: Android VPN permission prompt appeared from com.android.vpndialogs, user approved it physically, and later Connect attempts on TECNO LG8n reused the granted permission"

  - id: basic-connect-browse-disconnect
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Connect, browse two HTTPS sites, disconnect"
    evidence: "2026-06-15: TECNO LG8n reached Connected and remained stable for 60s after EngineBridge class lookup fix; browser loaded https://example.com and https://api.ipify.org through active VPN; disconnect returned UI to Disconnected and closed tun fd only through handleStop; redacted evidence in artifacts/android-diagnostics/tun-lifecycle-fixed-20260615-201047/"

  - id: ipv4-connectivity
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb shell ping -4 -c 4 <REDACTED_IP>"
    evidence: "2026-06-15: TECNO LG8n browser loaded https://api.ipify.org while VPN was Connected and displayed a public IPv4 address; exact IP redacted. adb shell curl/wget were not available on the device, so browser evidence was used instead."

  - id: ipv6-behavior
    priority: P0
    status: not_applicable
    requires_physical_device: true
    command: "adb shell ping -6 -c 4 2606:4700:4700::1111"
    evidence: "2026-06-15: TECNO LG8n browser test-ipv6.com showed no public IPv6 while VPN was Connected; baseline had no underlying IPv6 default route, so full IPv6 egress is not applicable for this device/network. Android VPN LinkProperties included ::/0 -> tun0 and no public IPv6 fall-through was observed. Redacted evidence: artifacts/android-diagnostics/dns-ipv6-audit-20260615-202413/ipv6-behavior-summary-redacted.txt"

  - id: dns-leak-audit
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb shell nslookup example.com"
    evidence: "2026-06-15: TECNO LG8n browser-based DNS leak audit ran dnsleaktest.com standard test and browserleaks.com/dns while VPN was Connected; BrowserLeaks reported public/VPN-path resolver providers (Cloudflare, Google LLC, Kraken Network ISP LTD) and no local mobile/Wi-Fi ISP resolver in the result set. Raw IPs redacted. Evidence: artifacts/android-diagnostics/dns-ipv6-audit-20260615-202413/dns-leak-summary-redacted.txt"

  - id: kill-switch-always-on
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Enable Always-on VPN and Block connections without VPN, then interrupt network"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 exposed Always-on VPN and Block connections without VPN for GMvpn. After GmvpnVpnService handled the Android system android.net.VpnService start action, enabling Always-on started the service via the system path and UI reached Connected. With lockdown=1, HTTPS loaded while VPN was active; after force-stop of com.gmvpn.client.debug, Chrome could not load a unique example.com URL and logcat returned BLOCKED NetworkInfo instead of direct network access. Restore set always_on_vpn_app=null and always_on_vpn_lockdown=0; onRevoke -> handleStop -> closeTun was observed. Evidence: artifacts/android-diagnostics/always-on-killswitch-20260615-204557/"

  - id: reconnect-network-change
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Switch Wi-Fi to cellular and back while connected"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 had active cellular data (mobile_data=1). With GMvpn Connected, adb svc wifi disable moved the active path to cellular+VPN; HTTPS example.com and IPv4 browser egress still worked. adb svc wifi enable moved back to Wi-Fi+VPN; HTTPS and IPv4 still worked. A second short Wi-Fi off/on cycle also ended in Wi-Fi+VPN with browser success. The tunnel state remained Connected across the observed handovers; no app crash or traffic leak was observed. Post-handover UI disconnect removed the VPN, reconnect reached VPN Connected for 60s, HTTPS worked, and final disconnect removed the VPN. DNS sanity after handover used VPN LinkProperties DNS <REDACTED_IP>/<REDACTED_IP> and browser domain resolution. Evidence: artifacts/android-diagnostics/network-handover-20260615-212318/"

  - id: udp-heavy-traffic
    priority: P0
    status: pass_limited
    requires_physical_device: true
    manual_step: "Run DNS workload and 5-minute video/QUIC-heavy browsing"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 had no controlled UDP/iperf target in ignored local config/env, so the documented browser/WebRTC/QUIC fallback was used. Chrome loaded a WebRTC/STUN leak-check page, then played a 10-minute YouTube browser video for a 5-minute observed window; playback progressed from 9:34 to 3:25 while VPN LinkProperties stayed Connected at each minute. After the UDP-heavy window, browser HTTPS example.com, browser IPv4 egress, DNS sanity via VPN LinkProperties plus browser domain resolution, UI disconnect, 60s reconnect, and final disconnect all passed. Logcat showed no GMvpn crash/panic, reconnect loop, TUN loss, or app onDestroy during the UDP-heavy window. Limitation: this validates browser/WebRTC/QUIC behavior only; it does not measure controlled iperf UDP throughput/loss. Evidence: artifacts/android-diagnostics/udp-heavy-20260615-215101/; adb diagnostics bundle: artifacts/android-diagnostics/20260615-191157Z/"

  - id: diagnostics-export-in-app
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "About -> Export diagnostics"
    evidence: "redacted diagnostics text; no profile URI, UUID, token, pbk, sid, spx"

  - id: diagnostics-adb-bundle
    priority: P1
    status: pass
    requires_physical_device: true
    command: "./scripts/collect-android-diagnostics.sh"
    evidence: "2026-06-15: Git Bash syntax check passed and scripts/collect-android-diagnostics.sh collected artifacts/android-diagnostics/20260615-171555Z, artifacts/android-diagnostics/20260615-174021Z, artifacts/android-diagnostics/20260615-181132Z, artifacts/android-diagnostics/20260615-184123Z, and artifacts/android-diagnostics/20260615-191157Z from TECNO LG8n; default Windows bash points to WSL and is blocked by missing distro"

  - id: release-not-ready-until-device-pass
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Confirm every P0 device item is pass"
    evidence: "2026-06-15: P0 physical validation evidence is complete on TECNO LG8n, including stable connect/basic browse, DNS leak audit, this TECNO/network's IPv6 behavior, Always-on/block-without-VPN, Wi-Fi/cellular handover reconnect, and UDP-heavy browser/WebRTC/QUIC fallback validation. Final release-readiness audit found and fixed two local blockers: R8 rules for JNA optional AWT references and a narrow lint suppression for VpnService systemExempted foreground-service type. After the fixes debug build/tests, physical connected tests, lintDebug, release APK build, and release bundle build passed. Classification: release_ready_candidate, not a production/public distribution claim."
```
