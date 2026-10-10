# ============================================================================
#  バックエンド（Spring Boot）を開発モードで起動する。
#
#    .\start-backend.ps1          PostgreSQL に接続して起動（通常）
#    .\start-backend.ps1 -Init    テーブル作成＋初期データ投入をしてから起動
#    .\start-backend.ps1 -H2      PostgreSQL を使わず H2 インメモリDBで起動
#
#  停止は Ctrl+C。初回はMaven本体とライブラリの取得で数分かかる。
# ============================================================================

[CmdletBinding()]
param(
    # DBを初期化する（schema.sql / data.sql を実行）。
    # 【警告】既存のテーブルとデータは削除されます。初回セットアップ時のみ使用してください。
    [switch]$Init,

    # PostgreSQL の代わりに H2 インメモリDBを使う（DBセットアップ不要で動作確認できる）
    [switch]$H2
)

# Windows のコンソールを UTF-8 にしてログの日本語化けを防ぐ。
# アプリは UTF-8 でログを書くが、Windows のコンソールは既定が Shift_JIS(CP932) のため
# 以下の3つを揃えておく必要がある。
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'

# --- 有効化するプロファイルを組み立てる -------------------------------------
$profiles = @()
if ($H2) { $profiles += 'h2' }
if ($Init) { $profiles += 'init' }

Push-Location "$PSScriptRoot\backend"
try {
    Write-Host '=== Spring Boot を起動します (http://localhost:8080) ===' -ForegroundColor Cyan

    if ($H2) {
        Write-Host '  DB : H2 インメモリ（停止するとデータは消えます）' -ForegroundColor Yellow
        Write-Host '  H2コンソール : http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:sampledb / User: sa / Password: 空欄)'
    }
    else {
        $dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { '5432' }
        Write-Host "  DB : PostgreSQL  jdbc:postgresql://localhost:$dbPort/shinsei_db  (User: shinsei_app)"

        # PostgreSQL が起動していないと接続エラーで落ちるため、先に確認する
        $listening = Get-NetTCPConnection -LocalPort ([int]$dbPort) -State Listen -ErrorAction SilentlyContinue
        if (-not $listening) {
            Write-Host ''
            Write-Host "ポート $dbPort で待ち受けているプロセスがありません。" -ForegroundColor Red
            Write-Host 'PostgreSQL コンテナを起動してください:'
            Write-Host '  .\start-database.ps1' -ForegroundColor Yellow
            Write-Host ''
            Write-Host 'PostgreSQL を使わずに動作確認したい場合:'
            Write-Host '  .\start-backend.ps1 -H2' -ForegroundColor Yellow
            exit 1
        }
    }

    if ($Init) {
        Write-Host ''
        Write-Host '  !! init プロファイル: schema.sql / data.sql を実行します。' -ForegroundColor Yellow
        Write-Host '     既存のテーブルとデータは削除されます。' -ForegroundColor Yellow
    }

    Write-Host "  ヘルスチェック : http://localhost:8080/actuator/health"
    Write-Host ''

    if ($profiles.Count -gt 0) {
        $joined = $profiles -join ','
        Write-Host "  有効プロファイル: $joined" -ForegroundColor DarkGray
        .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$joined"
    }
    else {
        .\mvnw.cmd spring-boot:run
    }
}
finally {
    Pop-Location
}
