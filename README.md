# LiquidBounce (De-bloated & Offline Edition)

A privacy-focused, 100% offline overhaul of **LiquidBounce 0.37.0** targeting **Minecraft 1.21.11 (Fabric)**.

This fork strips all telemetry, proprietary cloud services, Axochat IRC tracking, remote cosmetics, auto-configs, and marketplace dependencies. It also bundles Chromium (JCEF/MCEF) directly inside the mod jar with native Linux/NSS fixes so in-game web views function completely offline without calling home.

---

## Key Changes & Enhancements

### 1. Complete Telemetry & Tracking Removal
- **Zero CCBlueX Connections:** Completely removed connections to `api.liquidbounce.net`, `auth.liquidbounce.net`, `cdn.liquidbounce.net`, and `liquidproxy.net`.
- **Axochat IRC Stripped:** Deleted the Axochat packet protocol, WebSocket handler, and background chat services.
- **Auto-Config Cloud Sync Removed:** Removed remote auto-configuration downloading; all client configurations remain 100% local.
- **Marketplace Stripped:** Removed all in-game marketplace models, revision checkers, item subscriptions, and interop API routes.
- **Cosmetics & Cape Services Removed:** Removed remote cape queries and CCBlueX cosmetic subscriber APIs.

### 2. Bundled Offline Chromium (JCEF / MCEF)
- **Offline Binaries:** Pre-packages JCEF natives (`windows_amd64` and `linux_amd64`) directly inside the mod jar (`resources/liquidbounce/cef/`).
- **No Remote Downloads:** Neutralized all MCEF download manager URLs to prevent any background network fetches at runtime.
- **NSS Library Shadowing Fix:** Resolves `UnsatisfiedLinkError` when running under Linux AppImage or custom launcher environments with `CefSystemLibraries`.
- **Wayland & Linux Stability:** Added `--disable-gpu` and `--disable-gpu-compositing` off-screen rendering switches to eliminate Wayland Ozone Skia Ganesh GL crashes, and disabled Google GCM background push services.

### 3. In-Game Offline Skin Changer Integration
- Enhances `ModuleSkinChanger` to seamlessly read local skin PNGs written by the launcher or `CustomSkinLoader`.
- Supports forced local texture overrides (`ForceOverride: true`) for offline and cracked gameplay.

---

## Network Architecture (On-Demand Only)

The client makes **zero startup connections**. The only network traffic permitted is strictly user-initiated:

| Destination | Purpose |
| :--- | :--- |
| `api.mojang.com` / `sessionserver.mojang.com` | User-initiated skin lookup or skin upload |
| `ipinfo.io` | User-initiated proxy / server location query |
| `translate.googleapis.com` | In-game chat translation module (opt-in) |
| `api.openai.com` | AI chat module (opt-in, requires personal API key) |
| `127.0.0.1` | Local loopback interop between Minecraft and the offline CEF GUI |

---

## Building from Source

### Prerequisites
- **Java Development Kit (JDK):** Version 21 (Adoptium Temurin / Azul Zulu recommended)
- **Node.js:** For building web themes in `src-theme`

### Build Command

```bash
# Clone the repository
git clone https://github.com/CrafterStudioIndia/LiquidBounce.git
cd LiquidBounce

# Compile the mod jar
./gradlew build
```

The compiled mod jar will be output to:
```
build/libs/liquidbounce-0.37.0.jar
```

---

## Recommended Launcher

For multi-instance management, offline account switching, and a Modrinth-style 3D interactive skin switcher, pair this mod with:
- [**LiquidLauncher**](https://github.com/CrafterStudioIndia/LiquidLauncher)

---

## License

This project is licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
