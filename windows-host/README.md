# DeskFlow Windows Host — attended remote support beta

## For the person whose PC will be controlled

1. Download **DeskFlow-Windows-x64.zip** from the repository's latest release.
2. Extract the entire ZIP. Keep `DeskFlowHost.exe` and `_internal` together.
3. Open `DeskFlowHost.exe` and click **Start internet session**.
4. Send the HTTPS session link and the 8-digit PIN to your expected partner. Share the PIN separately from the link.
5. When the approval dialog appears on this PC, choose **Yes** only for the person you expect. No screen frames or input are available before approval.
6. Press **STOP sharing**, or close the host window, to revoke access and shut down the tunnel.

## For the person connecting (phone or PC)

Open the owner's session link in a browser, enter the PIN, and wait for the owner to approve. No viewer installation, Python, Tailscale or router configuration is required. Select **Double click** to open Windows desktop icons. The controller also supports right click, Start, selected navigation keys, scroll and zoom.

Hindi: Jiska PC chalana hai woh app khole, Start kare, link/PIN bheje aur Allow kare. Aap mobile ya doosre PC ke browser mein us link ko kholkar connect karein.

## Scope and limits

- Windows 10/11 x64; primary monitor only. The PC must stay awake, signed in and unlocked.
- This is an attended beta with temporary session links, not an UltraViewer clone with permanent IDs or an unattended service.
- Internet sessions use Cloudflare Quick Tunnels. Links change on restart; availability is not guaranteed and Cloudflare describes Quick Tunnels as a testing service. Screen/control traffic passes through Cloudflare, which terminates HTTPS. Use only for sessions whose participants accept this relay.
- The host listens on loopback only in the packaged app. Authentication uses an 8-digit random PIN, local owner approval, expiring HttpOnly/Secure/SameSite cookies and origin checks. Five wrong PIN attempts per minute are permitted. Sessions expire after one hour; stopping clears all sessions and pending approvals.
- No automatic startup, hidden background service, UAC/elevated window control, drag-and-drop, full text keyboard entry, audio, clipboard or file transfer.
- The executable is unsigned. Review the source and the published SHA-256 before running. Do not disable antivirus/SmartScreen to run it; an organization may require a signed build.
- The original `server.py` CLI remains available for trusted LAN/Tailscale setups, with PIN authentication. The approval dialog belongs to `host_app.py`; use the packaged app for public internet sessions.

## Build from source

Install Python 3.11+ and use `build.ps1`. It creates a virtual environment, installs Pillow/PyInstaller, downloads the pinned official Cloudflare binary, verifies its SHA-256, and builds `dist/DeskFlowHost`. No administrator privileges are needed. Cloudflare's license is included under `vendor` and in the downloadable package.

## Validation

- `py -3 -m unittest discover -s windows-host -p "test_*.py" -v`: fake-desktop HTTP/authentication/approval/input regression suite.
- `node windows-host/test_viewer.cjs`: mobile controller coordinate/input regression checks.
- `.venv/Scripts/python.exe verify_native.py`: real capture and input test. It clicks only a visible target belonging to its own test window, restores the pointer, and refuses to click an obscured target.
- `.venv/Scripts/python.exe verify_tunnel.py`: temporary public HTTPS round-trip against a generated test image and fake input handler; never shares the user's actual desktop.

Cloudflare documentation: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/

### Validation on the build PC

The native screen capture and click test passed against its own test window. The public HTTPS fake-desktop test passed using public DNS resolution with certificate validation enabled. This PC's default DNS returned NXDOMAIN for temporary tunnel hostnames during testing; a viewer on a network with the same DNS issue may need another working network or their network administrator's help. No Windows DNS/security settings were changed.
