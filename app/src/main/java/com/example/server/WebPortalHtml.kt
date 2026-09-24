package com.example.server

object WebPortalHtml {
    fun getHtml(remoteId: String, deviceName: String, requiresAuth: Boolean): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>DeskFlow Remote | $remoteId</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Outfit:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-base: #0a0e17;
            --bg-panel: rgba(19, 26, 43, 0.85);
            --bg-panel-hover: #1e283d;
            --border-subtle: rgba(255, 255, 255, 0.08);
            --border-cyan: rgba(0, 229, 255, 0.3);
            --cyan-primary: #00e5ff;
            --cyan-glow: rgba(0, 229, 255, 0.25);
            --orange-accent: #ff5722;
            --text-main: #f1f5f9;
            --text-dim: #94a3b8;
            --success: #10b981;
            --danger: #ef4444;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            user-select: none;
            -webkit-user-select: none;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background-color: var(--bg-base);
            color: var(--text-main);
            height: 100vh;
            width: 100vw;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }

        /* Top Navigation Bar */
        .app-bar {
            height: 54px;
            background: var(--bg-panel);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border-subtle);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 16px;
            z-index: 100;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .brand-icon {
            width: 32px;
            height: 32px;
            background: linear-gradient(135deg, var(--cyan-primary), #0284c7);
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 0 12px var(--cyan-glow);
        }

        .brand-icon svg {
            width: 20px;
            height: 20px;
            fill: #00222a;
        }

        .brand-text {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: -0.2px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .target-chip {
            background: rgba(0, 229, 255, 0.1);
            color: var(--cyan-primary);
            border: 1px solid var(--border-cyan);
            border-radius: 6px;
            padding: 2px 8px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 12px;
            font-weight: 600;
            letter-spacing: 0.5px;
        }

        .stats-bar {
            display: flex;
            align-items: center;
            gap: 16px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 12px;
            color: var(--text-dim);
        }

        .stat-item {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--success);
            box-shadow: 0 0 8px var(--success);
        }

        .app-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .btn {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-subtle);
            color: var(--text-main);
            border-radius: 8px;
            padding: 6px 12px;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.15s ease;
        }

        .btn:hover {
            background: var(--bg-panel-hover);
            border-color: var(--border-cyan);
            color: var(--cyan-primary);
        }

        .btn-danger:hover {
            background: rgba(239, 68, 68, 0.15);
            border-color: var(--danger);
            color: var(--danger);
        }

        /* Main Workspace Stage */
        .workspace {
            flex: 1;
            position: relative;
            background: radial-gradient(circle at center, #111a2e 0%, #060911 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }

        .display-container {
            position: relative;
            max-height: calc(100vh - 120px);
            max-width: 90vw;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8), 0 0 20px rgba(0, 229, 255, 0.1);
            border-radius: 18px;
            overflow: hidden;
            border: 2px solid #202b3d;
            background: #000;
        }

        #screen-view {
            display: block;
            max-height: calc(100vh - 120px);
            max-width: 90vw;
            object-fit: contain;
            cursor: crosshair;
        }

        /* Virtual Cursor Indicator */
        #virtual-pointer {
            position: absolute;
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: var(--orange-accent);
            border: 2px solid #fff;
            box-shadow: 0 0 10px var(--orange-accent);
            pointer-events: none;
            transform: translate(-50%, -50%);
            transition: opacity 0.2s, transform 0.05s linear;
            opacity: 0;
            z-index: 10;
        }

        /* Bottom Floating Remote Dock */
        .dock {
            position: absolute;
            bottom: 16px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--bg-panel);
            backdrop-filter: blur(16px);
            border: 1px solid var(--border-subtle);
            border-radius: 14px;
            padding: 8px 14px;
            display: flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
            z-index: 50;
        }

        .dock-divider {
            width: 1px;
            height: 24px;
            background: var(--border-subtle);
            margin: 0 4px;
        }

        .dock-btn {
            background: transparent;
            border: none;
            color: var(--text-dim);
            padding: 8px 12px;
            border-radius: 8px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            font-weight: 500;
            transition: all 0.15s;
        }

        .dock-btn:hover {
            background: rgba(255, 255, 255, 0.08);
            color: var(--text-main);
        }

        .dock-btn.active {
            color: var(--cyan-primary);
            background: rgba(0, 229, 255, 0.1);
        }

        /* Text Input Popover */
        .input-bar {
            display: flex;
            gap: 6px;
            align-items: center;
        }

        .text-input {
            background: rgba(0, 0, 0, 0.4);
            border: 1px solid var(--border-subtle);
            border-radius: 6px;
            color: #fff;
            padding: 6px 10px;
            font-size: 13px;
            width: 180px;
            outline: none;
        }

        .text-input:focus {
            border-color: var(--cyan-primary);
        }

        /* Auth Modal */
        .modal-overlay {
            position: fixed;
            inset: 0;
            background: rgba(5, 8, 15, 0.85);
            backdrop-filter: blur(10px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }

        .modal-card {
            background: #131a2b;
            border: 1px solid var(--border-cyan);
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.8), 0 0 30px var(--cyan-glow);
            border-radius: 16px;
            width: 360px;
            padding: 24px;
            text-align: center;
        }

        .modal-card h2 {
            font-size: 20px;
            margin-bottom: 8px;
            color: var(--text-main);
        }

        .modal-card p {
            font-size: 14px;
            color: var(--text-dim);
            margin-bottom: 20px;
        }

        .pin-input {
            width: 100%;
            height: 48px;
            background: rgba(0, 0, 0, 0.5);
            border: 1px solid var(--border-subtle);
            border-radius: 10px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 24px;
            text-align: center;
            letter-spacing: 8px;
            color: var(--cyan-primary);
            outline: none;
            margin-bottom: 16px;
        }

        .pin-input:focus {
            border-color: var(--cyan-primary);
            box-shadow: 0 0 15px var(--cyan-glow);
        }

        .btn-primary {
            width: 100%;
            height: 44px;
            background: var(--cyan-primary);
            color: #00222a;
            border: none;
            border-radius: 10px;
            font-weight: 700;
            font-size: 15px;
            cursor: pointer;
            transition: filter 0.15s;
        }

        .btn-primary:hover {
            filter: brightness(1.1);
        }

        .auth-error {
            color: var(--danger);
            font-size: 13px;
            margin-top: 10px;
            display: none;
        }

        /* Toast notification */
        .toast {
            position: fixed;
            top: 68px;
            right: 20px;
            background: rgba(19, 26, 43, 0.95);
            border: 1px solid var(--border-cyan);
            border-radius: 8px;
            padding: 10px 16px;
            font-size: 13px;
            color: #fff;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
            transform: translateY(-20px);
            opacity: 0;
            transition: all 0.2s ease;
            pointer-events: none;
            z-index: 200;
        }

        .toast.show {
            transform: translateY(0);
            opacity: 1;
        }
    </style>
</head>
<body>

    <!-- Top Bar -->
    <header class="app-bar">
        <div class="brand">
            <div class="brand-icon">
                <svg viewBox="0 0 24 24"><path d="M20 3H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h3l-1 2v1h12v-1l-1-2h3c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 13H4V5h16v11z"/></svg>
            </div>
            <div class="brand-text">
                <span>DeskFlow</span>
                <span class="target-chip">$remoteId</span>
            </div>
        </div>

        <div class="stats-bar">
            <div class="stat-item">
                <div class="dot"></div>
                <span id="conn-status">LIVE STREAM</span>
            </div>
            <div class="stat-item">
                <span>PING:</span>
                <span id="ping-val" style="color: var(--cyan-primary);">-- ms</span>
            </div>
            <div class="stat-item">
                <span>FPS:</span>
                <span id="fps-val" style="color: var(--cyan-primary);">--</span>
            </div>
            <div class="stat-item">
                <span id="device-label">$deviceName</span>
            </div>
        </div>

        <div class="app-actions">
            <button class="btn" onclick="toggleFullscreen()" title="Fullscreen">
                ⛶ Fullscreen
            </button>
            <button class="btn" onclick="downloadScreenshot()" title="Screenshot">
                📸 Capture
            </button>
            <button class="btn btn-danger" onclick="disconnectSession()">
                ✕ Disconnect
            </button>
        </div>
    </header>

    <!-- Center Screen Work Area -->
    <main class="workspace" id="workspace">
        <div class="display-container" id="display-box">
            <img id="screen-view" src="/stream.mjpeg" alt="Remote Screen" crossorigin="anonymous" />
            <div id="virtual-pointer"></div>
        </div>

        <!-- Floating Remote Control Dock -->
        <div class="dock">
            <!-- Android System Keys -->
            <button class="dock-btn" onclick="sendAction('back')" title="Back">
                ◀ Back
            </button>
            <button class="dock-btn" onclick="sendAction('home')" title="Home">
                ⚪ Home
            </button>
            <button class="dock-btn" onclick="sendAction('recents')" title="Recent Apps">
                ▢ Apps
            </button>
            <div class="dock-divider"></div>
            <!-- Volume & Power -->
            <button class="dock-btn" onclick="sendAction('volume_down')" title="Volume Down">🔉 Vol-</button>
            <button class="dock-btn" onclick="sendAction('volume_up')" title="Volume Up">🔊 Vol+</button>
            <button class="dock-btn" onclick="sendAction('power')" title="Lock / Power">⚡ Power</button>
            <div class="dock-divider"></div>
            <!-- Text Input directly to Phone -->
            <div class="input-bar">
                <input type="text" id="phone-text-input" class="text-input" placeholder="Type text to phone..." onkeydown="if(event.key==='Enter') sendTypedText()" />
                <button class="dock-btn active" onclick="sendTypedText()">Send</button>
            </div>
            <div class="dock-divider"></div>
            <!-- Clipboard sync -->
            <button class="dock-btn" onclick="syncClipboard()" title="Sync Clipboard">📋 Paste Clp</button>
        </div>
    </main>

    <!-- Toast -->
    <div id="toast" class="toast">Action dispatched</div>

    <!-- Security PIN / Access Code Dialog -->
    <div class="modal-overlay" id="auth-modal" style="${if (requiresAuth) "display:flex;" else "display:none;"}">
        <div class="modal-card">
            <h2>DeskFlow Security</h2>
            <p>Enter the 4-digit security PIN shown on the target phone screen to start remote access.</p>
            <input type="password" id="pin-input" class="pin-input" maxlength="8" placeholder="••••" autofocus onkeydown="if(event.key==='Enter') submitPin()" />
            <button class="btn-primary" onclick="submitPin()">Connect to System</button>
            <div id="auth-error" class="auth-error">Incorrect PIN. Please check device.</div>
        </div>
    </div>

    <script>
        let isAuthenticated = ${if (requiresAuth) "false" else "true"};
        const screenImg = document.getElementById('screen-view');
        const pointer = document.getElementById('virtual-pointer');
        let isMouseDown = false;

        // Telemetry loop
        let lastPingTime = Date.now();
        setInterval(async () => {
            if (!isAuthenticated) return;
            const start = performance.now();
            try {
                const res = await fetch('/api/status');
                const elapsed = Math.round(performance.now() - start);
                document.getElementById('ping-val').textContent = elapsed + ' ms';
                if (res.ok) {
                    const data = await res.json();
                    if (data.fps) document.getElementById('fps-val').textContent = data.fps;
                    if (data.deviceName) document.getElementById('device-label').textContent = data.deviceName;
                }
            } catch (e) {
                document.getElementById('conn-status').textContent = 'RECONNECTING...';
            }
        }, 2000);

        // Touch & Mouse Handling on Remote Display
        function getCoords(e) {
            const rect = screenImg.getBoundingClientRect();
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
            const y = Math.max(0, Math.min(1, (clientY - rect.top) / rect.height));
            return { x, y, px: clientX - rect.left, py: clientY - rect.top };
        }

        screenImg.addEventListener('mousedown', (e) => {
            e.preventDefault();
            isMouseDown = true;
            const c = getCoords(e);
            showPointer(c.px, c.py);
            dispatchTouch('down', c.x, c.y, e.button);
        });

        window.addEventListener('mousemove', (e) => {
            if (!isMouseDown) return;
            const c = getCoords(e);
            showPointer(c.px, c.py);
            dispatchTouch('move', c.x, c.y, 0);
        });

        window.addEventListener('mouseup', (e) => {
            if (!isMouseDown) return;
            isMouseDown = false;
            const c = getCoords(e);
            hidePointer();
            dispatchTouch('up', c.x, c.y, 0);
        });

        // Touch support for tablets / touch screens
        screenImg.addEventListener('touchstart', (e) => {
            e.preventDefault();
            const c = getCoords(e);
            showPointer(c.px, c.py);
            dispatchTouch('down', c.x, c.y, 0);
        }, { passive: false });

        screenImg.addEventListener('touchmove', (e) => {
            e.preventDefault();
            const c = getCoords(e);
            showPointer(c.px, c.py);
            dispatchTouch('move', c.x, c.y, 0);
        }, { passive: false });

        screenImg.addEventListener('touchend', (e) => {
            e.preventDefault();
            hidePointer();
            dispatchTouch('up', 0.5, 0.5, 0);
        }, { passive: false });

        function showPointer(x, y) {
            pointer.style.opacity = '1';
            pointer.style.left = x + 'px';
            pointer.style.top = y + 'px';
        }

        function hidePointer() {
            setTimeout(() => { pointer.style.opacity = '0'; }, 200);
        }

        async function dispatchTouch(action, xRatio, yRatio, button) {
            if (!isAuthenticated) return;
            try {
                await fetch('/api/touch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action, xRatio, yRatio, button })
                });
            } catch(e) {}
        }

        async function sendAction(actionName) {
            if (!isAuthenticated) return;
            try {
                await fetch('/api/key', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ keyType: 'action', actionName })
                });
                showToast('Key command: ' + actionName);
            } catch(e) {}
        }

        async function sendTypedText() {
            const input = document.getElementById('phone-text-input');
            const text = input.value;
            if (!text) return;
            try {
                await fetch('/api/key', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ keyType: 'text', text })
                });
                input.value = '';
                showToast('Text sent to phone');
            } catch(e) {}
        }

        async function syncClipboard() {
            try {
                const text = await navigator.clipboard.readText();
                if (text) {
                    await fetch('/api/clipboard', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ text })
                    });
                    showToast('Copied to device clipboard');
                }
            } catch(e) {
                showToast('Clipboard access denied');
            }
        }

        function toggleFullscreen() {
            if (!document.fullscreenElement) {
                document.documentElement.requestFullscreen();
            } else {
                document.exitFullscreen();
            }
        }

        function downloadScreenshot() {
            const a = document.createElement('a');
            a.href = '/api/screenshot?t=' + Date.now();
            a.download = 'deskflow_screen_' + Date.now() + '.jpg';
            a.click();
            showToast('Downloading screenshot...');
        }

        function disconnectSession() {
            if (confirm('Disconnect remote session?')) {
                window.location.reload();
            }
        }

        async function submitPin() {
            const pin = document.getElementById('pin-input').value.trim();
            const errDiv = document.getElementById('auth-error');
            try {
                const res = await fetch('/api/auth', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pin })
                });
                const data = await res.json();
                if (data.success) {
                    document.getElementById('auth-modal').style.display = 'none';
                    isAuthenticated = true;
                    showToast('Access Granted! Live Session Started.');
                    // Refresh stream to start video
                    screenImg.src = '/stream.mjpeg?t=' + Date.now();
                } else {
                    errDiv.style.display = 'block';
                    errDiv.textContent = data.message || 'Incorrect PIN or Access Code';
                }
            } catch(e) {
                errDiv.style.display = 'block';
                errDiv.textContent = 'Connection error. Please retry.';
            }
        }

        function showToast(msg) {
            const t = document.getElementById('toast');
            t.textContent = msg;
            t.classList.add('show');
            setTimeout(() => t.classList.remove('show'), 2000);
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
