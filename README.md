# MyVPN Android 0.3

## WARP / WireGuard experimental milestone

New: WireGuard tunnel implementation using official `com.wireguard.android:tunnel:1.0.20260102`.

- Paste a valid **WireGuard-format WARP profile** (`[Interface]`, `[Peer]`).
- Tap **Включить WARP**, approve Android VPN permission.
- The app attempts to bring up a real WireGuard VPN tunnel and displays its state.
- Tap **Выключить WARP** to stop.
- VLESS Reality TCP diagnostics remain included.

**This does not yet chain WARP → VLESS Reality.** Direct VLESS transport also remains unimplemented. Do not confuse WireGuard tunnel start with successful Cloudflare handshake: verify connectivity with https://www.cloudflare.com/cdn-cgi/trace (`warp=on`).

**Config handling:** configuration is held in RAM only by the app, not saved to preferences or GitHub. Do not share your WARP private key or real VLESS URL in issues or screenshots. A WARP account's token is not a WireGuard config; obtain a complete authorized WireGuard profile separately. Legacy Cloudflare WARP WireGuard compatibility and reachability depend on account and network.

**Known limits:** VPN process may be terminated in background; no foreground control service, no persistent tunnel reconnection, no health check, no WARP registration in app, no chain with Reality. Next step: replace dual-engine concept with integrated VPN core that can route the VLESS dialer through a WireGuard endpoint. No guarantee that a blocked WARP UDP path works.

GitHub Actions builds APK debug artifact via `Build Android APK`.

## 0.4 experimental automatic WARP profile creation

The **Создать WARP автоматически** button generates WireGuard keys on-device and attempts to register a consumer WARP device using an **unofficial / undocumented API**. The user must explicitly consent and accept the Cloudflare terms before any network request. The request sends the **public** WireGuard key to Cloudflare; the private key stays on the phone. The returned profile is displayed in memory; it is not automatically uploaded anywhere.

This can fail if Cloudflare changes or blocks third-party registration. It is not guaranteed to work on current accounts, and even a successful registration will not necessarily connect from a filtered network. This release still has WARP-only mode: **WARP → VLESS is not implemented**.

To verify WARP connectivity after starting the tunnel, open https://www.cloudflare.com/cdn-cgi/trace and check `warp=on`.
