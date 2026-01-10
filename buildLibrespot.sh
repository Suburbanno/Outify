#!/usr/bin/env bash

set -e

declare -A ABI_TRIPLE_MAP=(
	["arm64-v8a"]="aarch64-linux-android"
	["armeabi-v7a"]="armv7-linux-androideabi"
	["x86"]="i686-linux-android"
	["x86_64"]="x86_64-linux-android"
)

PROJECT_ROOT=$(pwd)
LIBRESPOT_DIR="$PROJECT_ROOT/rust/librespot-ffi"
OUTPUT_DIR="$PROJECT_ROOT/app/src/main/jniLibs"
PLATFORM_VERSION=21

if [ -z "$ANDROID_SDK_ROOT" ]; then
	echo "ANDROID_SDK_ROOT is not set"
	exit 1
fi

if [ -z "$ANDROID_NDK" ]; then
	NDK_BASE="$ANDROID_SDK_ROOT/ndk"

	if [ ! -d "$NDK_BASE" ]; then
		echo "No NDK directory found at $NDK_BASE"
		exit 1
	fi

	ANDROID_NDK="$(ls -d "$NDK_BASE"/29.* 2>/dev/null | sort -V | tail -n 1)"

	if [ -z "$ANDROID_NDK" ]; then
		echo "No Android NDK 29.x found in $NDK_BASE"
		exit 1
	fi
fi

TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"

export PATH="$TOOLCHAIN:$PATH"

export TARGET_CC="$TOOLCHAIN/aarch64-linux-android21-clang"
export TARGET_AR="$TOOLCHAIN/llvm-ar"

export CC_aarch64_linux_android="$TARGET_CC"
export AR_aarch64_linux_android="$TARGET_AR"

echo "Using ANDROID_NDK=$ANDROID_NDK"
echo "Using TARGET_CC=$TARGET_CC"

for ABI in "${!ABI_TRIPLE_MAP[@]}"; do
	TRIPLE=${ABI_TRIPLE_MAP[$ABI]}
	echo "Building librespot for $ABI ($TRIPLE).."

	cd "$LIBRESPOT_DIR"

	cargo ndk \
		-t "$ABI" \
		--platform "$PLATFORM_VERSION" \
		build --release

	if [ "$?" -ne 0 ]; then
		echo "Failed to build librespot for $ABI!"
		exit 1
	fi

	cd "$PROJECT_ROOT"

	mkdir -p "$OUTPUT_DIR/$ABI"

	cp "$LIBRESPOT_DIR/target/$TRIPLE/release/liblibrespot_ffi.so" "$OUTPUT_DIR/$ABI/"

	#chmod +x "$OUTPUT_DIR/$ABI/liblibrespot"
done
