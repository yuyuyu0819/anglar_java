# ============================================================================
#  開発用 PostgreSQL（Docker）を操作するスクリプト
#
#    .\start-database.ps1           起動して healthy になるまで待つ
#    .\start-database.ps1 -Status   状態と接続情報を表示する
#    .\start-database.ps1 -Logs     ログを追う（Ctrl+C で抜ける）
#    .\start-database.ps1 -Psql     コンテナ内の psql に入る
#    .\start-database.ps1 -Stop     停止する（データは残る）
#    .\start-database.ps1 -Reset    ボリュームごと削除して作り直す（データは消える）
#
#  中身は docker compose を呼んでいるだけなので、
#  docker compose コマンドを直接叩いても同じです。
# ============================================================================

[CmdletBinding(DefaultParameterSetName = 'Up')]
param(
    [Parameter(ParameterSetName = 'Status')][switch]$Status,
    [Parameter(ParameterSetName = 'Logs')][switch]$Logs,
    [Parameter(ParameterSetName = 'Psql')][switch]$Psql,
    [Parameter(ParameterSetName = 'Stop')][switch]$Stop,
    [Parameter(ParameterSetName = 'Reset')][switch]$Reset
)

chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$service = 'postgres'
$container = 'shinsei-postgres'
$port = if ($env:POSTGRES_PORT) { $env:POSTGRES_PORT } else { '5432' }

function Show-ConnectionInfo {
    Write-Host ''
    Write-Host '--- pgAdmin / アプリからの接続情報 ---' -ForegroundColor Cyan
    Write-Host "  Host              : localhost"
    Write-Host "  Port              : $port"
    Write-Host "  Database          : shinsei_db"
    Write-Host "  Username          : shinsei_app      (アプリ用。権限は最小限)"
    Write-Host "  Password          : shinsei_pass"
    Write-Host "  Maintenance DB    : postgres         (pgAdmin の接続設定で指定する項目)"
    Write-Host ''
    Write-Host "  管理用(スーパーユーザー) : postgres / postgres"
    Write-Host "  JDBC URL                 : jdbc:postgresql://localhost:$port/shinsei_db"
}

Push-Location $PSScriptRoot
try {
    # --- Docker デーモンの確認 ---------------------------------------------
    docker info --format '{{.ServerVersion}}' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Docker デーモンに接続できません。Docker Desktop を起動してください。' -ForegroundColor Red
        exit 1
    }

    if ($Status) {
        Write-Host '=== コンテナの状態 ===' -ForegroundColor Cyan
        docker compose ps
        Show-ConnectionInfo
        exit 0
    }

    if ($Logs) {
        docker compose logs -f $service
        exit 0
    }

    if ($Psql) {
        Write-Host '=== コンテナ内の psql に接続します（\q で終了）===' -ForegroundColor Cyan
        Write-Host '  例) \dt  … テーブル一覧 /  SELECT * FROM application LIMIT 5;'
        Write-Host ''
        docker compose exec -e PGPASSWORD=shinsei_pass $service psql -U shinsei_app -d shinsei_db
        exit 0
    }

    if ($Stop) {
        Write-Host '=== 停止します（データは残ります）===' -ForegroundColor Cyan
        docker compose down
        exit $LASTEXITCODE
    }

    if ($Reset) {
        Write-Host '=== 警告: データベースのデータをすべて削除して作り直します ===' -ForegroundColor Yellow
        $answer = Read-Host '続行しますか？ (y/N)'
        if ($answer -ne 'y') { Write-Host '中止しました。'; exit 0 }

        docker compose down -v
        Write-Host ''
        Write-Host 'ボリュームを削除しました。再作成します...' -ForegroundColor Cyan
        # 以降は通常の起動処理に進む
    }

    # --- 起動 ---------------------------------------------------------------
    Write-Host '=== PostgreSQL コンテナを起動します ===' -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    # --- healthy になるまで待つ ----------------------------------------------
    Write-Host ''
    Write-Host '起動完了（healthy）を待っています...' -NoNewline
    $healthy = $false
    for ($i = 0; $i -lt 60; $i++) {
        $state = docker inspect --format '{{.State.Health.Status}}' $container 2>$null
        if ($state -eq 'healthy') { $healthy = $true; break }
        Start-Sleep -Seconds 2
        Write-Host '.' -NoNewline
    }
    Write-Host ''

    if (-not $healthy) {
        Write-Host '起動を確認できませんでした。ログを確認してください:' -ForegroundColor Red
        Write-Host '  .\start-database.ps1 -Logs' -ForegroundColor Yellow
        exit 1
    }
    Write-Host 'PostgreSQL は起動しています。' -ForegroundColor Green

    # --- 初期化結果の確認 ----------------------------------------------------
    Write-Host ''
    Write-Host '=== データベースの状態 ===' -ForegroundColor Cyan
    docker compose exec -T -e PGPASSWORD=postgres $service psql -U postgres -d postgres -c `
        "SELECT d.datname AS database, pg_encoding_to_char(d.encoding) AS encoding, d.datcollate AS collate, r.rolname AS owner FROM pg_database d JOIN pg_roles r ON r.oid = d.datdba WHERE d.datname = 'shinsei_db';"

    Write-Host '=== テーブルの状態 ===' -ForegroundColor Cyan
    $tables = docker compose exec -T -e PGPASSWORD=shinsei_pass $service psql -U shinsei_app -d shinsei_db -t -A -c `
        "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"
    $tableCount = ($tables | Out-String).Trim()

    if ($tableCount -eq '0') {
        Write-Host '  テーブルは未作成です。次のコマンドで作成してください:' -ForegroundColor Yellow
        Write-Host '    .\start-backend.ps1 -Init' -ForegroundColor Yellow
    }
    else {
        docker compose exec -T -e PGPASSWORD=shinsei_pass $service psql -U shinsei_app -d shinsei_db -c `
            "SELECT table_name AS table, (SELECT count(*) FROM application) AS application_rows FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
    }

    Show-ConnectionInfo
}
finally {
    Pop-Location
}
