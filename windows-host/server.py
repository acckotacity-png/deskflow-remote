"""User-started Windows desktop host. No unattended service or public relay."""
import argparse
import ctypes
from ctypes import wintypes
import hmac
import io
import json
import math
import secrets
import socket
import subprocess
import threading
import time
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

BASE = Path(__file__).resolve().parent

class Desktop:
    def __init__(self):
        from PIL import ImageGrab
        self.grab = ImageGrab.grab
        self.user = ctypes.WinDLL('user32', use_last_error=True)
        try:
            ctypes.WinDLL('shcore').SetProcessDpiAwareness(2)
        except (OSError, AttributeError):
            self.user.SetProcessDPIAware()
        self.lock = threading.Lock()

    def frame(self):
        with self.lock:
            image = self.grab(all_screens=False)
        image.thumbnail((1600, 1000))
        result = io.BytesIO()
        image.convert('RGB').save(result, 'JPEG', quality=65)
        return result.getvalue()

    def input(self, data):
        kind = data.get('type')
        if kind == 'click':
            x, y = data.get('x'), data.get('y')
            if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v) or not 0 <= v <= 1 for v in (x, y)):
                raise ValueError('Invalid click coordinates')
            button = data.get('button', 'left')
            if button not in ('left', 'right', 'double'):
                raise ValueError('Invalid mouse button')
            width, height = self.user.GetSystemMetrics(0), self.user.GetSystemMetrics(1)
            with self.lock:
                if not self.user.SetCursorPos(round(x * (width - 1)), round(y * (height - 1))):
                    raise RuntimeError('Windows blocked pointer input. Unlock the PC locally.')
                down, up = (0x0008, 0x0010) if button == 'right' else (0x0002, 0x0004)
                for _ in range(2 if button == 'double' else 1):
                    self.user.mouse_event(down, 0, 0, 0, 0)
                    self.user.mouse_event(up, 0, 0, 0, 0)
                    if button == 'double':
                        time.sleep(0.05)
        elif kind == 'key':
            keys = {'enter': 0x0D, 'escape': 0x1B, 'backspace': 0x08, 'tab': 0x09, 'windows': 0x5B, 'left': 0x25, 'up': 0x26, 'right': 0x27, 'down': 0x28}
            key = keys.get(data.get('key'))
            if key is None:
                raise ValueError('Unsupported key')
            with self.lock:
                self.user.keybd_event(key, 0, 0, 0)
                self.user.keybd_event(key, 0, 2, 0)
        elif kind == 'scroll':
            delta = data.get('delta')
            if delta not in (-120, 120):
                raise ValueError('Invalid scroll amount')
            with self.lock:
                self.user.mouse_event(0x0800, 0, 0, ctypes.c_uint32(delta).value, 0)
        else:
            raise ValueError('Unsupported input')

class Host(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, address, desktop, pin):
        self.desktop, self.pin = desktop, pin
        self.sessions = {}
        self.attempts = {}
        self.state_lock = threading.Lock()
        super().__init__(address, Handler)

class Handler(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(10)

    def log_message(self, *_):
        pass  # Never log PINs, cookies or screen contents.

    def reply(self, status, content, mime='application/json', cookie=None):
        if isinstance(content, dict):
            content = json.dumps(content).encode()
        self.send_response(status)
        self.send_header('Content-Type', mime)
        self.send_header('Content-Length', str(len(content)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('X-Frame-Options', 'DENY')
        if cookie:
            self.send_header('Set-Cookie', cookie)
        self.end_headers()
        self.wfile.write(content)

    def authenticated(self):
        cookies = SimpleCookie()
        try:
            cookies.load(self.headers.get('Cookie', ''))
            token = cookies['deskflow_session'].value
        except (KeyError, ValueError):
            return False
        with self.server.state_lock:
            expiry = self.server.sessions.get(token, 0)
            if expiry <= time.monotonic():
                self.server.sessions.pop(token, None)
                return False
            return True

    def do_GET(self):
        path = self.path.split('?', 1)[0]
        if path == '/':
            self.reply(200, (BASE / 'viewer.html').read_bytes(), 'text/html; charset=utf-8')
        elif path == '/api/frame':
            if not self.authenticated():
                self.reply(401, {'error': 'Enter the PIN shown on the PC.'})
                return
            try:
                self.reply(200, self.server.desktop.frame(), 'image/jpeg')
            except Exception:
                self.reply(503, {'error': 'Screen unavailable. Keep the PC unlocked.'})
        else:
            self.reply(404, {'error': 'Not found'})

    def do_POST(self):
        # Custom header plus JSON forces cross-origin requests to preflight.
        # This host deliberately provides no CORS permission.
        if self.headers.get('X-DeskFlow') != '1' or self.headers.get_content_type() != 'application/json':
            self.reply(403, {'error': 'Use the controller served by this host.'})
            return
        origin = self.headers.get('Origin')
        if origin and origin != 'http://' + self.headers.get('Host', ''):
            self.reply(403, {'error': 'Cross-origin input is disabled.'})
            return
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 < length <= 4096:
                raise ValueError()
            data = json.loads(self.rfile.read(length))
            if not isinstance(data, dict):
                raise ValueError()
        except (ValueError, OSError):
            self.reply(400, {'error': 'Invalid request'})
            return
        if self.path == '/api/auth':
            address, now = self.client_address[0], time.monotonic()
            with self.server.state_lock:
                failures = [t for t in self.server.attempts.get(address, []) if now - t < 60]
                self.server.attempts[address] = failures
                if len(failures) >= 5:
                    self.reply(429, {'error': 'Too many PIN attempts. Wait one minute.'})
                    return
                submitted = data.get('pin', '')
                if not isinstance(submitted, str) or not submitted.isascii() or not hmac.compare_digest(submitted, self.server.pin):
                    failures.append(now)
                    self.reply(401, {'error': 'Incorrect PIN'})
                    return
                token = secrets.token_urlsafe(32)
                self.server.sessions = {k: v for k, v in self.server.sessions.items() if v > now}
                self.server.sessions[token] = now + 3600
                self.server.attempts.pop(address, None)
            self.reply(200, {'ok': True}, cookie='deskflow_session=' + token + '; HttpOnly; SameSite=Strict; Path=/; Max-Age=3600')
        elif self.path == '/api/input':
            if not self.authenticated():
                self.reply(401, {'error': 'Session expired. Reconnect using the PC PIN.'})
                return
            try:
                self.server.desktop.input(data)
                self.reply(200, {'ok': True})
            except ValueError as error:
                self.reply(400, {'error': str(error)})
            except Exception:
                self.reply(503, {'error': 'Windows input unavailable. Unlock the PC locally.'})
        elif self.path == '/api/logout':
            cookies = SimpleCookie(self.headers.get('Cookie', ''))
            if 'deskflow_session' in cookies:
                with self.server.state_lock:
                    self.server.sessions.pop(cookies['deskflow_session'].value, None)
            self.reply(200, {'ok': True}, cookie='deskflow_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0')
        else:
            self.reply(404, {'error': 'Not found'})

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bind', default='0.0.0.0')
    parser.add_argument('--port', type=int, default=8765)
    args = parser.parse_args()
    pin = f'{secrets.randbelow(100000000):08d}'
    host = Host((args.bind, args.port), Desktop(), pin)
    addresses = sorted({v[4][0] for v in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET)})
    print('\nDeskFlow Windows Host - primary monitor', flush=True)
    tailscale = Path('C:/Program Files/Tailscale/tailscale.exe')
    if tailscale.exists():
        try:
            result = subprocess.run([str(tailscale), 'ip', '-4'], capture_output=True, text=True, timeout=5, check=True)
            vpn_ip = result.stdout.strip()
            socket.inet_aton(vpn_ip)
            print(f'Remote internet via Tailscale: http://{vpn_ip}:{args.port}', flush=True)
        except (OSError, subprocess.SubprocessError):
            print('Tailscale is not connected. Connect it on the PC and phone for remote access.', flush=True)
    else:
        print('Different internet: install/connect Tailscale on PC and phone, then use the PC Tailscale IP.', flush=True)
    print('Use only on trusted Wi-Fi or a private VPN. Do not port-forward this HTTP server.', flush=True)
    for address in addresses:
        print(f'Open on your phone: http://{address}:{args.port}', flush=True)
    print(f'PIN: {pin}\nKeep this window open. Ctrl+C stops all remote access.\n', flush=True)
    try:
        host.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        host.server_close()

if __name__ == '__main__':
    main()
