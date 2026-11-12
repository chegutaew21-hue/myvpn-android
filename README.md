# MyVPN for Android

## Milestone 0.2 — VLESS Reality configuration and network diagnostics

This project **is not yet a VPN client**. It builds an Android APK that:
- Imports VLESS Reality TCP links **in memory** (no credentials saved in 0.2).
- Validates UUID, server/port, Reality SNI and public key.
- Generates a sing-box JSON configuration internally for future libbox integration.
- Tests a TCP connection to the server on a background thread and displays the outcome.

**TCP success is NOT proof that VLESS/Reality authentication works.** No packets are tunneled in 0.2.

## Automated builds

GitHub Actions → Build Android APK → successful run → Artifacts → MyVPN-Android-prototype.

## Next engineering milestone

- Integrate a supported Android libbox AAR + Android VpnService with lifecycle management.
- Handle socket protection to avoid VPN routing loops; ensure DNS and network changes behave correctly.
- Confirm VLESS Reality on a physical Android device before adding WARP -> VLESS chaining.
- Implement encrypted storage, QR import, auto reconnect and sanitized logs.

Never commit private keys or raw VLESS links. For safety, version 0.2 never persists submitted credentials.
