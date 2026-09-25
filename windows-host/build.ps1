$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath '.venv/Scripts/python.exe')) {
    py -3 -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw 'Python 3.11 or later is required.' }
}
& ./.venv/Scripts/python.exe -m pip install -r requirements.txt 'pyinstaller==6.22.3'
if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
$releaseInfo = Get-Content -Raw -LiteralPath 'vendor/cloudflared-release.json' | ConvertFrom-Json
if (-not (Test-Path -LiteralPath 'vendor/cloudflared.exe')) {
    Invoke-WebRequest -Uri $releaseInfo.url -OutFile 'vendor/cloudflared.exe'
}
$actualHash = (Get-FileHash -LiteralPath 'vendor/cloudflared.exe' -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $releaseInfo.sha256) { throw 'Cloudflare binary checksum mismatch.' }
& ./.venv/Scripts/python.exe -m PyInstaller --noconfirm --windowed --onedir --name DeskFlowHost --distpath dist --workpath build --specpath . --add-data 'viewer.html;.' --add-binary 'vendor/cloudflared.exe;vendor' host_app.py
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
Copy-Item -LiteralPath 'README.md' -Destination 'dist/DeskFlowHost/README.md'
Copy-Item -LiteralPath 'vendor/CLOUDFLARED-LICENSE.txt' -Destination 'dist/DeskFlowHost/CLOUDFLARED-LICENSE.txt'
Write-Host 'Built dist/DeskFlowHost/DeskFlowHost.exe'
