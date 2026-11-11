# MyVPN for Android

Personal Android VPN client project.

## Current milestone: 0.1 (prototype only)

- Android UI shell.
- Basic local VLESS URL input and validation.
- GitHub Actions builds an unsigned-for-distribution **debug APK**, signed with Android's generated debug key.
- **Does not connect to VPN yet.**

## Next milestones

1. Integrate actively maintained VPN engine and Android VpnService.
2. Implement and test direct VLESS Reality on an Android device.
3. Test WARP as upstream transport and build WARP → VLESS chaining.
4. Add secure credential storage, diagnostic logs, and connection controls.

**Security:** Never commit VLESS URLs, credentials, WARP private keys, or configuration exports into this repository. Prototype stores entered URL in app-private SharedPreferences, which is not encrypted. Do not enter sensitive production credentials yet.

## APK

Go to **Actions → Build Android APK → latest successful run → Artifacts → MyVPN-Android-prototype**.

An APK build succeeding does **not** prove VPN connectivity. Device testing will be necessary once transport is implemented.
