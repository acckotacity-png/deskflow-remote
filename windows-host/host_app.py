"""Visible, attended Windows host with a temporary Cloudflare internet link."""
import collections
import argparse
import json
import os
from pathlib import Path
import queue
import re
import secrets
import subprocess
import sys
import threading
import time
import tkinter as tk
from tkinter import messagebox, ttk
from server import Desktop, Host

BASE = Path(__file__).resolve().parent

class App:
    def __init__(self, root, status_path=None):
        self.root = root
        self.status_path = status_path
        self.host = None
        self.tunnel = None
        self.events = queue.Queue()
        self.generation = 0
        self.session_url = ''
        self.logs = collections.deque(maxlen=12)
        root.title('DeskFlow Remote - Windows Host')
        root.geometry('700x530')
        root.minsize(640, 500)
        root.protocol('WM_DELETE_WINDOW', self.close)
        frame = ttk.Frame(root, padding=24)
        frame.pack(fill='both', expand=True)
        ttk.Label(frame, text='DeskFlow Remote', font=('Segoe UI', 23, 'bold')).pack(anchor='w')
        ttk.Label(frame, text='Share this PC with a person you choose.', font=('Segoe UI', 11)).pack(anchor='w', pady=(4, 18))
        self.status = tk.StringVar(value='Stopped - nobody can connect')
        ttk.Label(frame, textvariable=self.status, wraplength=630).pack(anchor='w', pady=6)
        ttk.Label(frame, text='Session link').pack(anchor='w', pady=(14, 3))
        self.link = tk.StringVar(value='Press Start internet session')
        ttk.Entry(frame, textvariable=self.link, state='readonly').pack(fill='x')
        self.pin = tk.StringVar(value='--------')
        ttk.Label(frame, text='Session PIN (share separately)').pack(anchor='w', pady=(12, 0))
        ttk.Label(frame, textvariable=self.pin, font=('Consolas', 24, 'bold')).pack(anchor='w')
        row = ttk.Frame(frame); row.pack(fill='x', pady=12)
        self.start_button = ttk.Button(row, text='Start internet session', command=self.start)
        self.start_button.pack(side='left', padx=(0, 8))
        self.stop_button = ttk.Button(row, text='STOP sharing', command=self.stop, state='disabled')
        self.stop_button.pack(side='left')
        ttk.Button(row, text='Copy link', command=lambda: self.copy(self.session_url)).pack(side='right')
        ttk.Label(frame, text='1. Start a session and send the link + PIN to your partner.\n2. They open the link in a phone or PC browser and enter the PIN.\n3. You must press Allow here before they can view or control the desktop.\n4. STOP sharing immediately revokes access.', wraplength=630, justify='left').pack(anchor='w', pady=8)
        ttk.Label(frame, text='Beta: temporary link changes each session. Traffic passes through Cloudflare.\nKeep the PC awake/unlocked. Primary monitor only; UAC/elevated windows are unsupported.\nNo unattended startup. Close this window to stop access.', wraplength=630, foreground='#555555').pack(anchor='w', pady=10)
        root.after(100, self.process_events)

    def write_status(self, state):
        if self.status_path:
            try:
                self.status_path.write_text(json.dumps({'state': state, 'url': self.session_url, 'pid': os.getpid()}), encoding='utf-8')
            except OSError:
                pass

    def copy(self, value):
        if value:
            self.root.clipboard_clear(); self.root.clipboard_append(value)
            self.status.set('Link copied. Share the PIN separately; approve only your expected partner.')

    def start(self):
        if self.host:
            return
        binary = BASE / 'vendor' / 'cloudflared.exe'
        if not binary.exists():
            messagebox.showerror('Missing tunnel client', 'Extract the complete DeskFlow Windows ZIP, then run DeskFlowHost.exe.', parent=self.root)
            return
        self.generation += 1
        generation = self.generation
        pin = f'{secrets.randbelow(100000000):08d}'
        try:
            self.host = Host(('127.0.0.1', 0), Desktop(), pin,
                             request_approval=lambda rid: self.events.put((generation, 'approval', rid)))
            # Before the public URL is known, refuse browser origins rather than guessing.
            self.host.public_origin = 'https://session-not-ready.invalid'
            threading.Thread(target=self.host.serve_forever, daemon=True).start()
            self.pin.set(pin)
            self.start_button.config(state='disabled'); self.stop_button.config(state='normal')
            self.status.set('Creating temporary internet connection...')
            self.link.set('Connecting...')
            port = self.host.server_port
            self.tunnel = subprocess.Popen(
                [str(binary), 'tunnel', '--no-autoupdate', '--url', f'http://127.0.0.1:{port}', '--protocol', 'http2'],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace',
                creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
            threading.Thread(target=self.read_tunnel, args=(self.tunnel, generation), daemon=True).start()
            self.root.after(60000, lambda: self.check_startup(generation))
        except Exception as error:
            self.stop()
            messagebox.showerror('Could not start host', str(error), parent=self.root)

    def read_tunnel(self, process, generation):
        for line in process.stdout:
            match = re.search(r'https://[a-z0-9-]+\.trycloudflare\.com', line)
            if match:
                self.events.put((generation, 'url', match.group(0)))
            if 'Registered tunnel connection' in line:
                self.events.put((generation, 'ready', 'Internet session ready - waiting for your partner'))
            if 'ERR' in line:
                self.events.put((generation, 'log', line.strip()[-300:]))
        process.wait()
        self.events.put((generation, 'ended', 'Internet connection ended. Start a new session to reconnect.'))

    def check_startup(self, generation):
        if generation == self.generation and self.host and not self.session_url:
            details = '\n'.join(self.logs)
            self.stop()
            messagebox.showerror('Internet connection unavailable', 'The temporary relay did not connect. Check internet access and retry.\n\n' + details, parent=self.root)

    def process_events(self):
        try:
            while True:
                generation, kind, value = self.events.get_nowait()
                if generation != self.generation:
                    continue
                if kind == 'url' and self.host:
                    self.session_url = value
                    self.host.public_origin = value
                    self.link.set(value)
                    self.write_status("connecting")
                elif kind == 'ready':
                    self.status.set(value)
                    self.write_status('ready')
                elif kind == 'log':
                    self.logs.append(value)
                elif kind == 'ended':
                    self.stop(); self.status.set(value)
                elif kind == 'approval' and self.host:
                    host = self.host
                    with host.state_lock:
                        request = host.pending.get(value)
                        valid = request and request['expires'] > time.monotonic()
                    if not valid:
                        continue
                    self.root.deiconify(); self.root.lift()
                    approved = messagebox.askyesno('Allow remote desktop access?',
                        'Someone entered your session PIN.\n\nAllow them to SEE your primary screen and CONTROL your mouse and keyboard for up to one hour?\n\nChoose Yes only if this is the partner you are expecting. STOP sharing revokes all access.', parent=self.root)
                    if self.host is host:
                        host.resolve_approval(value, approved)
                        self.status.set('Partner approved - desktop control enabled. STOP sharing to revoke.' if approved else 'Request declined - waiting for your partner')
        except queue.Empty:
            pass
        self.root.after(100, self.process_events)

    def stop(self):
        self.generation += 1
        host, self.host = self.host, None
        process, self.tunnel = self.tunnel, None
        if host:
            host.revoke_all()
            host.shutdown(); host.server_close()
        if process and process.poll() is None:
            process.terminate()
            try: process.wait(timeout=3)
            except subprocess.TimeoutExpired: process.kill()
        self.session_url = ''
        self.pin.set('--------'); self.link.set('Session stopped')
        self.status.set('Stopped - nobody can connect')
        self.start_button.config(state='normal'); self.stop_button.config(state='disabled')
        self.logs.clear()
        self.write_status("stopped")

    def close(self):
        self.stop(); self.root.destroy()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--start', action='store_true', help='Start an attended session after opening the visible host window')
    parser.add_argument('--status-file', type=Path, help='Optional local status output; never includes the PIN')
    parser.add_argument('--smoke-test', type=Path, help='Check packaged GUI/resources without sharing the desktop')
    args = parser.parse_args()
    root = tk.Tk()
    if args.smoke_test:
        root.withdraw()
    app = App(root, args.status_file)
    if args.smoke_test:
        root.update_idletasks()
        result = {
            'gui': app.start_button.cget('text') == 'Start internet session',
            'viewer': (BASE / 'viewer.html').is_file(),
            'tunnel_binary': (BASE / 'vendor' / 'cloudflared.exe').is_file(),
            'starts_stopped': app.host is None,
        }
        args.smoke_test.write_text(json.dumps(result), encoding='utf-8')
        root.destroy()
        return
    if args.start:
        root.after(300, app.start)
    root.mainloop()

if __name__ == '__main__':
    main()
