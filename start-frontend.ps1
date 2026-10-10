# フロントエンド（Angular）の開発サーバを起動する。
#
#   PowerShell から:  .\start-frontend.ps1
#
# 停止は Ctrl+C。
# ソースを保存すると自動で再ビルドされ、ブラウザが更新される（ホットリロード）。

Push-Location "$PSScriptRoot\frontend"
try {
    if (-not (Test-Path 'node_modules')) {
        Write-Host '=== node_modules が無いため npm install を実行します ===' -ForegroundColor Yellow
        npm install
    }

    Write-Host '=== Angular 開発サーバを起動します (http://localhost:4200) ===' -ForegroundColor Cyan
    Write-Host '  /api/** へのリクエストは proxy.conf.json により localhost:8080 へ転送されます。'
    Write-Host '  バックエンドを先に起動しておいてください。'
    Write-Host ''
    npm start
}
finally {
    Pop-Location
}
