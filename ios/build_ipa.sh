#!/bin/bash
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
usage() {
    printf 'Usage: %s --unsigned OUTPUT.ipa | --signed-export EXPORT_OPTIONS.plist OUTPUT.ipa\n' "$0" >&2
    exit 2
}

mode="${1:-}"
case "$mode" in
    --unsigned) [[ $# = 2 && "$2" = *.ipa ]] || usage; output="$2" ;;
    --signed-export) [[ $# = 3 && -f "$2" && "$3" = *.ipa ]] || usage; output="$3" ;;
    *) usage ;;
esac

if ! command -v xcodebuild >/dev/null; then
    printf 'Xcode 14.3.1 on macOS Ventura is required for the iOS 11 build.\n' >&2
    exit 1
fi
if ! xcodebuild -version | grep -q '^Xcode 14\.'; then
    printf 'Select Xcode 14 with DEVELOPER_DIR to build and preserve the iOS 11 deployment target.\n' >&2
    exit 1
fi

mkdir -p "$(dirname "$output")"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"
build="$(mktemp -d)"
trap 'rm -rf "$build"' EXIT

project=(-project "$here/TransitTR.xcodeproj" -scheme TransitTR -configuration Release
         -destination 'generic/platform=iOS' -derivedDataPath "$build/derived")
if [[ "$mode" = "--unsigned" ]]; then
    xcodebuild "${project[@]}" CODE_SIGNING_ALLOWED=NO build
    app="$build/derived/Build/Products/Release-iphoneos/TransitTR.app"
    test -s "$app/TransitTR"
    test -s "$app/TransitCatalog.sqlite"
    test -s "$app/Frameworks/libswiftCore.dylib"
    mkdir -p "$build/Payload"
    cp -R "$app" "$build/Payload/"
    (cd "$build" && /usr/bin/zip -qry "$build/TransitTR.ipa" Payload)
    mv -f "$build/TransitTR.ipa" "$output"
else
    xcodebuild "${project[@]}" -archivePath "$build/TransitTR.xcarchive" archive
    /usr/bin/codesign --verify --deep --strict "$build/TransitTR.xcarchive/Products/Applications/TransitTR.app"
    xcodebuild -exportArchive -archivePath "$build/TransitTR.xcarchive" \
        -exportOptionsPlist "$2" -exportPath "$build/export"
    shopt -s nullglob
    packages=("$build"/export/*.ipa)
    [[ ${#packages[@]} = 1 ]] || { printf 'Expected one IPA from Xcode.\n' >&2; exit 1; }
    mv -f "${packages[0]}" "$output"
fi
printf '%s\n' "$output"
