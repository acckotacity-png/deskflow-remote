# DeskFlow Remote

## Mobile to Windows PC control

Download the portable Windows app from [Releases](https://github.com/acckotacity-png/deskflow-remote/releases/latest). The owner starts an internet session, shares its temporary HTTPS link and PIN, and approves the viewer locally. A phone or another PC can connect using a browser, across different internet connections, without Python or VPN setup. See [Windows setup and beta limitations](windows-host/README.md).

The website now leads to the real native host. Browser-only screen sharing cannot click Windows desktop apps. Temporary internet links use Cloudflare Quick Tunnels; this is an attended beta, not a permanent-ID or unattended remote-access product.

The Android implementation below is separate from the Windows host.

> **AnyDesk & UltraViewer style Remote Desktop and Live Display Streaming System for Android & Web**

DeskFlow allows you to share and access live displays between mobile devices, PCs, Macs, and Linux systems over the local network or Wi-Fi. It includes an embedded HTTP/MJPEG streaming server that serves a full-featured web client to any web browser without needing any extra software installed on the client computer.

---

## 🌟 Key Features

### 1. Host Mode (Screen Mirroring & Web Access)
- **9-Digit Desk ID**: Instant device identification like AnyDesk / TeamViewer.
- **One-Time Access PIN**: 4-digit regeneratable code ensuring only authorized viewers can connect.
- **Embedded Web Client Portal**: Built-in HTTP and MJPEG stream server on port `8080`.
- **Zero-Install Client**: Open `http://<phone-ip>:8080` in Chrome, Safari, Edge, or Firefox on any PC, laptop, or tablet.
- **Live Telemetry**: Real-time FPS monitoring, latency ping, and active client list.

### 2. Remote Control & Input Emulation
- **Direct Touch & Trackpad Modes**: Tap directly on the remote display or glide a virtual mouse pointer.
- **Remote Navigation Bar**: Hardware back, home, recents, power/lock, and volume buttons.
- **Virtual Keyboard / Text Dispatch**: Send typed text from PC directly into the phone's active input fields.
- **Clipboard Sync**: Synchronize clipboard text seamlessly between PC and phone.
- **Accessibility Service (`DeskFlowAccessibilityService`)**: Dispatches real Android touch gestures and global navigation actions.

### 3. Remote Viewer Client
- Connect to any DeskFlow host or remote streaming endpoint.
- Built-in instant test demo to preview remote control and streaming with zero setup.

### 4. Security & Audit Logging
- **Unattended Access**: Support for permanent master passwords.
- **Local Persistence (Room DB)**: Saved devices, favorites, and complete session history with connection durations and data transfer stats.

---

## 🛠 Tech Stack & Architecture

- **Platform**: Android (Kotlin, Jetpack Compose, Material 3)
- **Architecture**: MVVM with Coroutines & StateFlow
- **Display Capture**: Android `MediaProjection` API & `VirtualDisplay`
- **Network Engine**: Lightweight Multi-Threaded HTTP & MJPEG Stream Server
- **Database**: Android Jetpack Room
- **Accessibility**: Android `AccessibilityService` for remote gesture dispatch

---

## 🚀 How to Run

1. Open the app on your Android device.
2. Tap **"START SCREEN BROADCAST"** on the Host screen.
3. Note the local web link (e.g. `http://192.168.1.55:8080`) and the 4-digit PIN.
4. On any PC, laptop, or phone connected to the same network, open the link in any modern web browser.
5. Enter the PIN and enjoy low-latency live screen viewing and remote control!

---

## 📄 License
Apache-2.0
