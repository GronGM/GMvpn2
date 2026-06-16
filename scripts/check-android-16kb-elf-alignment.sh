#!/usr/bin/env bash
# Verify that packaged Android native libraries use 16 KB-compatible
# ELF LOAD segment alignment.

set -euo pipefail

usage() {
  cat >&2 <<'EOF'
Usage: scripts/check-android-16kb-elf-alignment.sh <apk|aab|aar|directory>

Set READELF or LLVM_READELF to override llvm-readelf detection.
EOF
}

if [[ $# -ne 1 ]]; then
  usage
  exit 2
fi

input="$1"
required_align=$((0x4000))

find_readelf() {
  if [[ -n "${READELF:-}" ]]; then
    printf '%s\n' "$READELF"
    return
  fi
  if [[ -n "${LLVM_READELF:-}" ]]; then
    printf '%s\n' "$LLVM_READELF"
    return
  fi
  if command -v llvm-readelf >/dev/null 2>&1; then
    command -v llvm-readelf
    return
  fi
  if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
    find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" \
      \( -name llvm-readelf -o -name llvm-readelf.exe \) \
      | sort \
      | head -n1
    return
  fi
  if command -v readelf >/dev/null 2>&1; then
    command -v readelf
    return
  fi
}

readelf_bin="$(find_readelf)"
[[ -n "$readelf_bin" && -x "$readelf_bin" ]] \
  || { echo "check-android-16kb: llvm-readelf/readelf not found" >&2; exit 2; }

work_dir=""
cleanup() {
  if [[ -n "$work_dir" ]]; then
    rm -rf "$work_dir"
  fi
  return 0
}
trap cleanup EXIT

if [[ -d "$input" ]]; then
  scan_root="$input"
elif [[ -f "$input" ]]; then
  case "$input" in
    *.apk|*.aab|*.aar|*.zip)
      command -v unzip >/dev/null 2>&1 \
        || { echo "check-android-16kb: unzip not found" >&2; exit 2; }
      work_dir="$(mktemp -d "${TMPDIR:-/tmp}/gmvpn-16kb-XXXXXX")"
      while IFS= read -r entry; do
        [[ -n "$entry" ]] || continue
        unzip -qq "$input" "$entry" -d "$work_dir"
      done < <(
        unzip -Z1 "$input" \
          | grep -E '(^|/)(lib|jni)/[^/]+/[^/]+\.so$' \
          || true
      )
      scan_root="$work_dir"
      ;;
    *)
      echo "check-android-16kb: unsupported input: $input" >&2
      exit 2
      ;;
  esac
else
  echo "check-android-16kb: input not found: $input" >&2
  exit 2
fi

mapfile -d '' libs < <(find "$scan_root" -type f -name '*.so' -print0 | sort -z)
if [[ ${#libs[@]} -eq 0 ]]; then
  echo "No native libraries found in $input"
  exit 0
fi

printf '%-16s %-36s %-10s %-10s %s\n' "ABI" "Library" "MinAlign" "MaxAlign" "Result"

failed=0
for so in "${libs[@]}"; do
  rel="${so#"$scan_root"/}"
  lib_name="$(basename "$so")"
  abi="unknown"
  IFS='/' read -r -a parts <<< "$rel"
  if [[ ${#parts[@]} -eq 2 ]]; then
    abi="${parts[0]}"
  fi
  for ((i = 0; i < ${#parts[@]} - 1; i++)); do
    if [[ "${parts[$i]}" == "lib" || "${parts[$i]}" == "jni" ]]; then
      abi="${parts[$((i + 1))]}"
      break
    fi
  done

  mapfile -t aligns < <("$readelf_bin" -l "$so" | awk '/^[[:space:]]*LOAD[[:space:]]/ {print $NF}')
  if [[ ${#aligns[@]} -eq 0 ]]; then
    printf '%-16s %-36s %-10s %-10s %s\n' "$abi" "$lib_name" "-" "-" "fail:no-load"
    failed=1
    continue
  fi

  min_align=
  max_align=0
  for align_hex in "${aligns[@]}"; do
    align=$((align_hex))
    if [[ -z "${min_align:-}" || $align -lt $min_align ]]; then
      min_align=$align
    fi
    if [[ $align -gt $max_align ]]; then
      max_align=$align
    fi
  done

  result="pass"
  if (( min_align < required_align )); then
    result="fail"
    failed=1
  fi

  printf '%-16s %-36s 0x%-8x 0x%-8x %s\n' \
    "$abi" "$lib_name" "$min_align" "$max_align" "$result"
done

exit "$failed"
