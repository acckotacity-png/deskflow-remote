# Mobile se Windows PC control (alag internet bhi)

The GitHub Pages browser broadcast is **view-only**. Browser-generated clicks cannot control Windows apps. Turning the page into a PWA does not change this. The Windows host in this folder provides the missing native mouse/keyboard input and serves its own mobile controller.

## Setup

1. On the PC install Python 3.11+ from https://www.python.org/downloads/windows/ (include the Python launcher).
2. For different internet connections, install Tailscale on the PC and phone: https://tailscale.com/download. Sign both devices into your own tailnet and keep Tailscale connected. See https://tailscale.com/docs/how-to/connect-to-devices.
3. Download this repository, extract it, and double-click `windows-host/Start-DeskFlow.bat` on the PC. First launch installs Pillow into a local `.venv` folder.
4. Keep the host window open. It displays an 8-digit PIN and available addresses. For remote internet access, use the PC's Tailscale IPv4 address (usually `100.x.y.z`): `http://100.x.y.z:8765` in the mobile browser. The existing website's **Open Windows PC controller** button also accepts this address.
5. Enter the PIN from the PC. Tap to single-click; choose **Double click** to open desktop icons. Use **Right click**, **Start**, navigation keys and scroll buttons as needed.
6. Disconnect in the mobile page, or press Ctrl+C in the PC host window to stop all sessions.

If Windows Firewall prompts, permit the host only for your intended private connection. If the VPN can reach the PC but the page times out, allow TCP 8765 only from your phone's Tailscale IP using Windows Firewall's inbound rule settings. Do not disable the firewall or open router ports.

## Current limits

- Primary Windows monitor only. PC must be awake, signed in and unlocked.
- Normal desktop apps; Windows secure desktop/UAC and elevated windows are not supported. Do not run this host as administrator as a workaround.
- Single/double/right click, selected keyboard keys and vertical scroll; no drag-and-drop, full text typing, audio, clipboard or file transfer yet.
- PIN/session authentication is separate from the old website's decorative ID/password. Sessions last one hour and are cleared when the host restarts.
- The controller uses HTTP inside your private VPN (or trusted LAN). Do not expose it directly to the public internet. Tailscale supplies encrypted transport between devices.
- This is a foreground helper, not an unattended Windows service. The host serves its own page to avoid HTTPS GitHub Pages trying to call an HTTP desktop endpoint.

## Tests

Run `py -3 -m unittest discover -s windows-host -p "test_*.py" -v` from the repository root. Tests use a fake desktop and never click your real desktop. A real two-device test is still needed after Tailscale setup.
