"""Internet round trip against a fake desktop; never exposes the user's screen."""
import http.cookiejar
import io
import socket
import tempfile
import sys
import json
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.request
import urllib.error
from PIL import Image
from server import Host

class FakeDesktop:
    def __init__(self): self.events = []
    def frame(self):
        buffer = io.BytesIO()
        Image.new('RGB', (32, 24), '#123456').save(buffer, 'JPEG')
        return buffer.getvalue()
    def input(self, data): self.events.append(data)

def main():
    desktop = FakeDesktop()
    host = Host(('127.0.0.1', 0), desktop, '87654321')
    host.request_approval = lambda rid: host.resolve_approval(rid, True)
    host.public_origin = 'https://session-not-ready.invalid'
    threading.Thread(target=host.serve_forever, daemon=True).start()
    process = subprocess.Popen([str(Path(__file__).parent/'vendor/cloudflared.exe'), 'tunnel', '--no-autoupdate', '--url', f'http://127.0.0.1:{host.server_port}', '--protocol', 'http2'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace', creationflags=subprocess.CREATE_NO_WINDOW)
    found = threading.Event()
    def read():
        for line in process.stdout:
            match = re.search(r'https://[a-z0-9-]+\.trycloudflare\.com', line)
            if match:
                host.public_origin = match.group(0)
                print('Test relay: '+host.public_origin, flush=True)
                found.set()
            if 'Registered tunnel connection' in line or 'ERR' in line:
                print(line.strip(), flush=True)
    threading.Thread(target=read, daemon=True).start()
    try:
        if not found.wait(45): raise RuntimeError('Relay did not provide a URL')
        resolve_ip = None
        if '--public-dns' in sys.argv:
            hostname = host.public_origin.split('://')[1]
            for attempt in range(12):
                result = subprocess.run(['powershell.exe', '-NoProfile', '-Command', '(Resolve-DnsName -Type A -Server 1.1.1.1 -ErrorAction Stop -Name '+hostname+' | Where-Object IPAddress | Select-Object -First 1 -ExpandProperty IPAddress)'],capture_output=True,text=True)
                resolve_ip = result.stdout.strip()
                try:
                    socket.inet_aton(resolve_ip)
                    break
                except OSError:
                    if attempt == 11: raise RuntimeError('Public DNS did not return an address: '+result.stderr)
                    time.sleep(2)
            print('Testing with public DNS result; Windows DNS settings remain unchanged.',flush=True)
        cookie_dir = tempfile.TemporaryDirectory()
        cookie_path = str(Path(cookie_dir.name)/'test-cookies.txt')
        opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        def request(path, data=None):
            if resolve_ip:
                args=['curl.exe','--silent','--show-error','--connect-timeout','10','--max-time','15','--resolve',hostname+':443:'+resolve_ip,'--cookie',cookie_path,'--cookie-jar',cookie_path,'--header','Origin: '+host.public_origin,'--header','X-DeskFlow: 1','--header','Content-Type: application/json','--write-out','\n%{http_code}',host.public_origin+path]
                if data is not None: args += ['--data-binary',json.dumps(data)]
                result=subprocess.run(args,capture_output=True,check=True)
                body,code=result.stdout.rsplit(b'\n',1)
                status=int(code)
                if status >= 400: raise urllib.error.HTTPError(host.public_origin+path,status,'HTTP error',{},None)
                return status,body
            req = urllib.request.Request(host.public_origin+path, data=json.dumps(data).encode() if data is not None else None, headers={'Content-Type':'application/json','X-DeskFlow':'1','Origin':host.public_origin,'User-Agent':'DeskFlow-Integration-Test'})
            with opener.open(req,timeout=12) as response: return response.status,response.read()
        for i in range(12):
            try:
                status, page = request('/')
                if status == 200: break
            except (urllib.error.URLError, TimeoutError) as error:
                print('Probe '+str(i+1)+': '+str(error), flush=True)
                if i == 11: raise
                time.sleep(3)
        try: request('/api/frame'); raise AssertionError('Unauthenticated screen was served')
        except urllib.error.HTTPError as error: assert error.code == 401
        status, body = request('/api/auth', {'pin':'87654321'})
        assert status == 202
        status, _ = request('/api/approval', {'requestId':json.loads(body)['requestId']})
        assert status == 200
        status, image = request('/api/frame')
        assert Image.open(io.BytesIO(image)).size == (32,24)
        click = {'type':'click','x':0.5,'y':0.5,'button':'left'}
        assert request('/api/input',click)[0] == 200
        assert desktop.events == [click]
        assert request('/api/logout',{})[0] == 200
        try: request('/api/frame'); raise AssertionError('Logout failed')
        except urllib.error.HTTPError as error: assert error.code == 401
        cookie_dir.cleanup()
        print('Public HTTPS tunnel: auth, owner approval, secure cookie, JPEG frame, input and logout PASS',flush=True)
    finally:
        host.revoke_all();host.shutdown();host.server_close()
        process.terminate()
        try:process.wait(timeout=3)
        except subprocess.TimeoutExpired:process.kill()

if __name__ == '__main__': main()
