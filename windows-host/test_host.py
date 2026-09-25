import http.client
import json
import threading
import unittest
from unittest.mock import Mock
from server import Host, Desktop

class FakeDesktop:
    def __init__(self): self.events = []
    def frame(self): return b'fake-jpeg'
    def input(self, data): self.events.append(data)

class HostTests(unittest.TestCase):
    def setUp(self):
        self.desktop = FakeDesktop()
        self.host = Host(('127.0.0.1', 0), self.desktop, '12345678')
        self.thread = threading.Thread(target=self.host.serve_forever, daemon=True)
        self.thread.start()
        self.cookie = ''
    def tearDown(self):
        self.host.shutdown()
        self.host.server_close()
        self.thread.join()
    def request(self, path, data=None, headers=None):
        conn = http.client.HTTPConnection('127.0.0.1', self.host.server_port, timeout=3)
        h = {'Content-Type': 'application/json', 'X-DeskFlow': '1', 'Cookie': self.cookie}
        h.update(headers or {})
        conn.request('POST' if data is not None else 'GET', path, json.dumps(data) if data is not None else None, h)
        r = conn.getresponse()
        result = r.status, r.read(), r.getheader('Set-Cookie')
        conn.close()
        return result
    def login(self):
        status, _, cookie = self.request('/api/auth', {'pin': '12345678'})
        self.assertEqual(status, 200)
        self.assertIn('HttpOnly', cookie)
        self.cookie = cookie.split(';')[0]
    def test_authentication_required_for_screen_and_clicks(self):
        self.assertEqual(self.request('/api/frame')[0], 401)
        self.assertEqual(self.request('/api/input', {'type': 'click'})[0], 401)
        self.assertEqual(self.desktop.events, [])
    def test_login_frame_click_logout(self):
        self.login()
        self.assertEqual(self.request('/api/frame')[:2], (200, b'fake-jpeg'))
        click = {'type': 'click', 'x': 0.25, 'y': 0.75, 'button': 'double'}
        self.assertEqual(self.request('/api/input', click)[0], 200)
        self.assertEqual(self.desktop.events, [click])
        self.assertEqual(self.request('/api/logout', {})[0], 200)
        self.assertEqual(self.request('/api/frame')[0], 401)
    def test_cross_origin_and_missing_header_rejected(self):
        self.assertEqual(self.request('/api/auth', {'pin': '12345678'}, {'Origin': 'https://evil.example'})[0], 403)
        self.assertEqual(self.request('/api/auth', {'pin': '12345678'}, {'X-DeskFlow': ''})[0], 403)
    def test_pin_rate_limit_and_unicode(self):
        self.assertEqual(self.request('/api/auth', {'pin': '\u2603'})[0], 401)
        for _ in range(4): self.assertEqual(self.request('/api/auth', {'pin': 'wrong'})[0], 401)
        self.assertEqual(self.request('/api/auth', {'pin': '12345678'})[0], 429)
    def test_expired_session(self):
        self.login()
        for key in self.host.sessions: self.host.sessions[key] = 0
        self.assertEqual(self.request('/api/input', {'type': 'key', 'key': 'enter'})[0], 401)
    def test_no_path_traversal(self):
        self.assertEqual(self.request('/../server.py')[0], 404)
        self.assertEqual(self.request('/')[0], 200)

class ApprovalTests(HostTests):
    def test_correct_pin_cannot_access_before_owner_approval(self):
        requests = []
        self.host.request_approval = requests.append
        status, body, cookie = self.request('/api/auth', {'pin': '12345678'})
        self.assertEqual(status, 202)
        self.assertIsNone(cookie)
        request_id = json.loads(body)['requestId']
        self.assertEqual(requests, [request_id])
        self.assertEqual(self.request('/api/frame')[0], 401)
        self.assertEqual(self.request('/api/approval', {'requestId': request_id})[0], 202)
        self.host.resolve_approval(request_id, True)
        status, _, cookie = self.request('/api/approval', {'requestId': request_id})
        self.assertEqual(status, 200)
        self.cookie = cookie.split(';')[0]
        self.assertEqual(self.request('/api/frame')[0], 200)
        self.assertEqual(self.request('/api/approval', {'requestId': request_id})[0], 403)

    def test_decline_and_stop_revoke_access(self):
        self.host.request_approval = lambda rid: self.host.resolve_approval(rid, False)
        _, body, _ = self.request('/api/auth', {'pin': '12345678'})
        self.assertEqual(self.request('/api/approval', {'requestId': json.loads(body)['requestId']})[0], 403)
        self.host.request_approval = None
        self.login()
        self.host.revoke_all()
        self.assertEqual(self.request('/api/frame')[0], 401)
        self.assertEqual(self.request('/api/input', {'type': 'click'})[0], 503)
        self.assertEqual(self.desktop.events, [])

    def test_tunnel_origin_and_secure_cookie(self):
        self.host.public_origin = 'https://example.trycloudflare.com'
        self.assertEqual(self.request('/api/auth', {'pin': '12345678'}, {'Origin': 'https://other.example'})[0], 403)
        status, _, cookie = self.request('/api/auth', {'pin': '12345678'}, {'Origin': self.host.public_origin})
        self.assertEqual(status, 200)
        self.assertIn('; Secure', cookie)

class DesktopInputTests(unittest.TestCase):
    def setUp(self):
        self.desktop = Desktop.__new__(Desktop)
        self.desktop.user = Mock()
        self.desktop.user.GetSystemMetrics.side_effect = lambda n: 1920 if n == 0 else 1080
        self.desktop.user.SetCursorPos.return_value = 1
        self.desktop.lock = threading.Lock()
    def test_click_maps_to_primary_screen_and_one_press_release(self):
        self.desktop.input({'type': 'click', 'x': 1, 'y': 0.5})
        self.desktop.user.SetCursorPos.assert_called_once_with(1919, 540)
        self.assertEqual([c.args[0] for c in self.desktop.user.mouse_event.call_args_list], [2, 4])
    def test_double_and_right_click(self):
        self.desktop.input({'type': 'click', 'x': 0, 'y': 0, 'button': 'double'})
        self.assertEqual(self.desktop.user.mouse_event.call_count, 4)
        self.desktop.user.mouse_event.reset_mock()
        self.desktop.input({'type': 'click', 'x': 0, 'y': 0, 'button': 'right'})
        self.assertEqual([c.args[0] for c in self.desktop.user.mouse_event.call_args_list], [8, 16])
    def test_invalid_coordinates_never_inject(self):
        for x in (-1, 2, float('nan'), float('inf'), '1', True, None):
            with self.assertRaises(ValueError): self.desktop.input({'type': 'click', 'x': x, 'y': 0})
        self.desktop.user.SetCursorPos.assert_not_called()
    def test_rejected_pointer_does_not_click(self):
        self.desktop.user.SetCursorPos.return_value = 0
        with self.assertRaises(RuntimeError): self.desktop.input({'type': 'click', 'x': 0, 'y': 0})
        self.desktop.user.mouse_event.assert_not_called()

if __name__ == '__main__': unittest.main()
