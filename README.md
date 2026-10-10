# 申請管理システム（Angular + Java キャッチアップ用サンプル）

官公庁系の業務システム案件を想定した、**Angular 20 + Spring Boot 3** の最小構成サンプルです。
実案件で頻出する作り（3層アーキテクチャ・DTO分離・MyBatis・楽観ロック・共通例外処理・
HTTPインターセプター・Reactive Forms）を一通り入れてあります。

> **学習ガイドは [docs/CATCHUP_GUIDE.md](docs/CATCHUP_GUIDE.md) にあります。**
> 「どこから読むか」「何を押さえるべきか」はそちらにまとめています。

---

## 1. 動作確認済みの環境

| | バージョン | 備考 |
|---|---|---|
| Node.js | 22.21.1 | |
| npm | 10.9.4 | |
| JDK | 21 (Microsoft OpenJDK) | Spring Boot 3.5 の要件は 17 以上 |
| Angular | 20.3 | CLI は `npx` 経由で実行するためグローバルインストール不要 |
| Spring Boot | 3.5.16 | |
| Maven | 3.9.16 | **インストール不要**（`mvnw` が自動取得する） |
| DB | **PostgreSQL 18**（Docker） | `docker-compose.yml` で起動。データは永続化される |
| DB（代替） | H2 2.x（インメモリ） | `-H2` オプションで切替。自動テストもこちらを使用 |
| Docker | 29.1.2 / Compose v2.40 | PostgreSQL の起動に使用 |

---

## 2. 起動手順

ターミナルを **3つ** 開く構成です（DB は一度起動すれば放置でよいので実質2つ）。

### 2-1. データベース（ポート 5432）

```powershell
cd C:\develop\angular_java
.\start-database.ps1
```

Docker で PostgreSQL 18 が起動します。**初回起動時のみ** `db\init\*.sql` が自動実行され、
ロール `shinsei_app` とデータベース `shinsei_db` が作られます。

#### 接続情報（pgAdmin にはこれを登録してください）

| 項目 | 値 |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Maintenance database | `postgres` |
| Database | `shinsei_db` |
| Username | `shinsei_app` |
| Password | `shinsei_pass` |
| 管理用（スーパーユーザー） | `postgres` / `postgres` |
| JDBC URL | `jdbc:postgresql://localhost:5432/shinsei_db` |
| エンコーディング | `UTF8`（照合順序 `C`） |

> **Windows にインストール済みの PostgreSQL 18 サービスは使いません。**
> どちらもポート 5432 を使うため、同時に起動すると衝突します。
> Windows 側のサービスは停止したままにしてください。
> 併用したい場合は `$env:POSTGRES_PORT = '5433'` を設定してから起動し、
> バックエンド側も `$env:DB_PORT = '5433'` を設定します。

#### start-database.ps1 のオプション

```powershell
.\start-database.ps1            # 起動して healthy になるまで待つ
.\start-database.ps1 -Status    # 状態と接続情報を表示
.\start-database.ps1 -Logs      # ログを追う
.\start-database.ps1 -Psql      # コンテナ内の psql に入る
.\start-database.ps1 -Stop      # 停止（データは残る）
.\start-database.ps1 -Reset     # ボリュームごと作り直す（データは消える）
```

### 2-2. バックエンド（ポート 8080）

**初回だけ** `-Init` を付けてテーブルを作成します。

```powershell
cd C:\develop\angular_java

.\start-backend.ps1 -Init    # 初回: schema.sql / data.sql を実行してテーブル作成＋初期データ投入
.\start-backend.ps1          # 2回目以降: 既存のテーブルをそのまま使う
.\start-backend.ps1 -H2      # PostgreSQL を使わず H2 インメモリDBで起動
```

> **`-Init` は既存のテーブルとデータを削除します**（`schema.sql` が `DROP TABLE` から始まるため）。
> データをきれいな初期状態に戻したいときにも使えます。

起動確認:

```powershell
curl http://localhost:8080/actuator/health      # => {"status":"UP", ... "db":{"status":"UP"} ...}
curl http://localhost:8080/api/applications     # => 申請一覧のJSON
```

### 2-3. フロントエンド（ポート 4200）

```powershell
cd C:\develop\angular_java
.\start-frontend.ps1         # = cd frontend && npm start
```

ブラウザで **http://localhost:4200/** を開くと申請一覧が表示されます。

### 2-4. その他の便利なコマンド

```powershell
# --- DB ---
docker compose up -d                        # start-database.ps1 の中身はこれ
docker compose down                         # 停止
docker compose down -v                      # ボリュームごと削除
docker compose logs -f postgres             # ログ

# --- バックエンド（backend ディレクトリで実行）---
.\mvnw.cmd clean package                    # ビルド + テスト → target\backend-1.0.0-SNAPSHOT.jar
.\mvnw.cmd test                             # テストのみ（H2を使うのでDB起動不要）
.\mvnw.cmd spring-boot:run                  # 起動
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=init"   # 初期化して起動
java -jar target\backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=h2   # H2で起動

# --- フロントエンド（frontend ディレクトリで実行）---
npm run build                               # 本番ビルド → dist\frontend\browser
npm test -- --watch=false --browsers=ChromeHeadless   # テスト（Chrome必須）
```

> **ログの日本語が文字化けする場合**
> Windows のコンソールは既定が Shift_JIS のためです。`chcp 65001` を実行して
> UTF-8 に切り替えてください（`start-*.ps1` は自動で設定しています）。

---

## 3. 全体構成

```
C:\develop\angular_java\
├── README.md                … このファイル
├── docs\
│   └── CATCHUP_GUIDE.md     … 学習ガイド（最初に読む）
├── docker-compose.yml       … 開発用 PostgreSQL の定義
├── db\
│   └── init\                … コンテナ初回起動時に自動実行されるSQL
│       ├── 01_create_role_and_database.sql   … ロールとDBの作成
│       └── 02_grant_schema.sql               … スキーマ権限の設定
├── start-database.ps1       … DB（Docker）起動スクリプト
├── start-backend.ps1        … バックエンド起動スクリプト
├── start-frontend.ps1       … フロントエンド起動スクリプト
├── backend\                 … Spring Boot アプリ（ポート8080）
└── frontend\                … Angular アプリ（ポート4200）
```

### 通信の流れ（開発時）

```
ブラウザ
  │  http://localhost:4200/applications
  ▼
Angular 開発サーバ (4200)
  │  /api/** だけを proxy.conf.json に従って転送
  ▼
Spring Boot (8080)
  │  jdbc:postgresql://localhost:5432/shinsei_db
  ▼
PostgreSQL 18 (Docker コンテナ shinsei-postgres)
  ▲
  └── pgAdmin から直接接続して中身を確認できる
```

`/api` 配下だけがバックエンドへ転送され、それ以外はAngularが処理します。
ブラウザから見ると常に同一オリジン（4200）なので **CORSの問題が起きません**。
この「開発プロキシ方式」は実務でも標準的な構成です。

---

## 4. バックエンドのディレクトリ構成

```
backend\
├── mvnw.cmd / .mvn\         … Maven Wrapper（Maven本体を自動取得する）
├── pom.xml                  … 依存ライブラリとビルド設定
└── src\
    ├── main\
    │   ├── java\jp\go\sample\backend\
    │   │   ├── BackendApplication.java      … 起動クラス
    │   │   ├── config\                      … 設定クラス
    │   │   │   ├── MyBatisConfig.java       …   Mapperスキャン
    │   │   │   ├── WebMvcConfig.java        …   CORS設定
    │   │   │   └── RequestLoggingFilter.java…   アクセスログ（監査証跡の最小形）
    │   │   ├── common\                      … 全機能で使う共通部品
    │   │   │   ├── dto\                     …   PageResponse / ApiErrorResponse
    │   │   │   └── exception\               …   例外クラス + GlobalExceptionHandler
    │   │   ├── domain\
    │   │   │   ├── model\                   … エンティティ・enum（業務ルールを持つ）
    │   │   │   └── repository\              … MyBatis Mapper インターフェース
    │   │   ├── service\                     … 業務ロジック・トランザクション境界
    │   │   └── web\
    │   │       ├── controller\              … REST エンドポイント
    │   │       └── dto\                     … リクエスト/レスポンスDTO
    │   └── resources\
    │       ├── application.yml              … 共通設定（既定はPostgreSQL接続）
    │       ├── application-init.yml         … init プロファイル: DDL/初期データを実行
    │       ├── application-h2.yml           … h2 プロファイル: H2インメモリDBに差し替え
    │       ├── schema.sql / data.sql        … DDL と初期データ
    │       └── mapper\ApplicationMapper.xml … SQL定義（動的SQL）
    └── test\java\...                        … テスト（26件）
```

### プロファイル構成

Spring Boot の「プロファイル」は環境ごとに設定を切り替える仕組みです。
`application.yml` が土台になり、`application-{プロファイル名}.yml` がそれを上書きします。

| プロファイル | 用途 | DB | schema.sql/data.sql |
|---|---|---|---|
| （指定なし） | 通常の開発 | PostgreSQL | 実行しない |
| `init` | 初回セットアップ・データリセット | PostgreSQL | **実行する（既存データは消える）** |
| `h2` | DB無しで動作確認／自動テスト | H2 インメモリ | 実行する |

実案件では `dev` / `stg` / `prod` のようにプロファイルを分け、
接続先やログレベルを切り替えるのが一般的です。

### 接続情報の外部化

`application.yml` の接続情報は環境変数で上書きできます（`${変数:既定値}` の記法）。

```yaml
url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:shinsei_db}
username: ${DB_USER:shinsei_app}
password: ${DB_PASSWORD:shinsei_pass}
```

```powershell
# 例) 別ポートのDBに接続する
$env:DB_PORT = '5433'
.\start-backend.ps1
```

**本番のパスワードを設定ファイルに書いてはいけません。** 設定ファイルは Git に入るため、
認証情報は必ず外部（環境変数・Secrets Manager・Vault・Kubernetes Secret 等）から注入します。
ここに書かれている `shinsei_pass` はローカル開発用の値です。

### レイヤーの責務

| 層 | やること | やらないこと |
|---|---|---|
| Controller | HTTP ⇄ Javaオブジェクトの変換、サービス呼び出し | 業務判断（if文）、SQL |
| Service | 業務ロジック、トランザクション管理、例外送出 | HTTPステータスの決定 |
| Mapper | SQL発行のみ | 業務判断 |
| GlobalExceptionHandler | 例外 → HTTPステータス・エラーJSONへの変換 | 業務ロジック |

**「業務ロジックはServiceに集める」** のが最重要の原則です。
Controllerに書くとテストしづらく、Mapperに書くと他機能から再利用できません。

---

## 5. フロントエンドのディレクトリ構成

```
frontend\
├── angular.json             … ビルド/起動設定（proxyConfig もここ）
├── proxy.conf.json          … /api を localhost:8080 へ転送
├── package.json
└── src\
    ├── index.html           … SPAの唯一のHTML
    ├── styles.scss          … グローバルCSS（デザイントークン・共通部品クラス）
    ├── main.ts              … 起動エントリポイント
    ├── environments\        … 環境別設定（dev / prod）
    └── app\
        ├── app.ts / .html / .scss   … アプリの外枠（ヘッダー・ナビ・router-outlet）
        ├── app.config.ts            … DIプロバイダ登録（HttpClient・Router等）
        ├── app.routes.ts            … URLと画面の対応表
        ├── core\                    … アプリ全体の基盤
        │   ├── models\              …   サーバDTOに対応する型定義
        │   ├── services\            …   API通信・通知・ローディング
        │   └── interceptors\        …   全HTTP通信に共通の処理
        ├── shared\                  … 画面をまたいで使う部品
        │   ├── components\          …   status-badge / pagination / field-error ...
        │   ├── validators\          …   カスタムバリデータ
        │   └── utils\
        └── features\                … 機能（画面）単位
            ├── applications\
            │   ├── application-list\    … 一覧（検索・ページング・削除）
            │   ├── application-detail\  … 詳細（ステータス変更）
            │   └── application-form\    … 登録・編集
            └── not-found\
```

### `core` / `shared` / `features` の使い分け

これは Angular 案件でほぼ必ず採用される分け方です。

- **core** … アプリに1つだけ存在する基盤。サービス、インターセプター、型定義
- **shared** … 複数画面で使い回す「部品」。状態を持たず、入力に対して表示を返すだけ
- **features** … 業務機能ごとのまとまり。画面コンポーネントを置く

`features` 配下のディレクトリは互いに参照しないのが原則です（機能間の依存を断つ）。
共通化したいものが出てきたら `shared` / `core` に引き上げます。

---

## 6. API 一覧

ベースURL: `http://localhost:8080`

| メソッド | パス | 説明 |
|---|---|---|
| GET | `/api/applications` | 一覧検索（ページング）<br>`?keyword=&status=&categoryCode=&page=0&size=10&sort=appliedOn&direction=desc` |
| GET | `/api/applications/{id}` | 1件取得 |
| POST | `/api/applications` | 登録（201 + Location ヘッダ） |
| PUT | `/api/applications/{id}` | 更新（楽観ロック） |
| PATCH | `/api/applications/{id}/status` | ステータス変更（状態遷移チェック） |
| DELETE | `/api/applications/{id}` | 削除（204） |
| GET | `/api/master/categories` | 申請区分マスタ |
| GET | `/api/master/statuses` | ステータス一覧（遷移可能先を含む） |
| GET | `/actuator/health` | 死活監視 |

### エラーレスポンスの形式

全APIで共通です。

```json
{
  "timestamp": "2026-10-07T11:55:15.408",
  "status": 400,
  "code": "E400-001",
  "message": "入力内容に誤りがあります。",
  "path": "/api/applications",
  "fieldErrors": [
    { "field": "applicantName", "message": "申請者氏名は必須です", "rejectedValue": null }
  ]
}
```

| HTTPステータス | エラーコード | 発生条件 |
|---|---|---|
| 400 | E400-001 | 入力検証エラー（`fieldErrors` に項目別メッセージ） |
| 400 | E400-002 | JSONの形式不正 |
| 404 | E404-001 | 対象データなし |
| 409 | E409-001 | 業務ルール違反（不正な状態遷移、存在しない区分コード等） |
| 409 | E409-002 | 楽観ロック競合（他ユーザーが先に更新） |
| 500 | E500-001 | 想定外エラー（詳細はサーバログのみに出力） |

---

## 7. 業務仕様（サンプルの中身）

### 申請ステータスの遷移ルール

```
          ┌──────────────┐
          │   受付済     │ RECEIVED（登録時は必ずここから）
          └──┬────────┬──┘
             │        │
       ┌─────▼──┐  ┌──▼────────┐
       │ 審査中 │  │  取下げ   │ WITHDRAWN（終了）
       └──┬──┬──┘  └───────────┘
          │  │
   ┌──────▼┐ └──▼──────┐
   │ 承認  │   │ 却下   │  APPROVED / REJECTED（終了）
   └───────┘   └────────┘
```

- 終了状態（承認・却下・取下げ）になった申請は**編集もステータス変更もできません**
- このルールは `ApplicationStatus` enum（Java側）が唯一の定義元です。
  フロントは `/api/master/statuses` の `allowedNextStatuses` を見てボタンを出し分けるため、
  **業務ルールの二重管理が発生しません**

### 楽観ロック

1. 画面がデータを取得すると `version` が一緒に返る
2. 更新時はその `version` をそのまま送り返す
3. サーバは `UPDATE ... WHERE id = ? AND version = ?` を実行
4. 更新件数が 0 件 → 他ユーザーが先に更新済み → **409 (E409-002)**

複数の職員が同じ申請を同時に開く運用を前提とした、官公庁案件では定番の仕組みです。

### 初期データ

申請15件（全ステータスを含む）＋ 申請区分マスタ5件が `data.sql` で投入されます。

- **PostgreSQL（既定）** … データは Docker ボリューム `shinsei_pgdata` に永続化されます。
  アプリやコンテナを再起動してもデータは残ります。
  初期状態に戻したいときは `.\start-backend.ps1 -Init`
- **H2（`-H2`）** … インメモリなのでアプリを停止するとデータは消え、起動ごとに初期状態に戻ります

---

## 8. 現在の検証状況

| 項目 | 結果 |
|---|---|
| `mvnw clean package` | BUILD SUCCESS |
| バックエンドのテスト | 26件すべてパス（サービス結合16件 / コントローラ10件・H2プロファイル） |
| `ng build` | 成功（画面ごとに遅延読み込みチャンクが分割されることを確認） |
| フロントエンドのテスト | 9件すべてパス |
| **PostgreSQL 起動** | `docker compose up -d` → healthy、`db\init\*.sql` が自動実行されることを確認 |
| **DB 構成** | `shinsei_db`（UTF8 / 照合順序C / オーナー `shinsei_app`）、`shinsei_app` が非スーパーユーザーであることを確認 |
| **テーブル作成** | `-Init` で2テーブル・PK・FK・UNIQUE・インデックス3本・列コメント・IDENTITY列を作成確認 |
| **API（PostgreSQL）** | 一覧・検索（日本語キーワード）・詳細・登録・更新・ステータス変更・削除・マスタ参照 すべて確認 |
| **API 異常系（PostgreSQL）** | 400(E400-001) / 404(E404-001) / 409(E409-001 業務ルール違反) / 409(E409-002 楽観ロック競合) すべて確認 |
| **採番の連番** | `APP-2026-00016` → `APP-2026-00017` と進むことを確認 |
| API（H2） | 同上すべて確認 |
| プロキシ疎通 | `localhost:4200/api/**` → `localhost:8080` を確認 |
| 画面描画 | 一覧・詳細・登録・編集・404 の全画面をヘッドレスブラウザで描画確認 |

---

## 9. 実案件との差分（意図的に簡略化した点）

このサンプルは学習目的のため、以下は入れていません。
**実案件では必ず登場する**ので、頭の片隅に置いておいてください。

| 項目 | 本サンプル | 実案件 |
|---|---|---|
| 認証・認可 | なし | Spring Security + セッション/JWT、ロール別権限制御 |
| DB | PostgreSQL 18（Docker） | Oracle / PostgreSQL / SQL Server（専用サーバ） |
| DDL管理 | `schema.sql` を init プロファイルで実行 | Flyway / Liquibase によるマイグレーション |
| DBロール | アプリ用1つ（`shinsei_app`） | 用途別に分割（アプリ用／参照専用／バッチ用／DDL用） |
| 接続情報 | 設定ファイルの既定値 | 環境変数・Secrets Manager・Vault 等から注入 |
| 削除 | 物理削除 | 論理削除（削除フラグ）＋ 監査ログ |
| 採番 | SQLのMAX+1（同時実行で重複の余地あり） | 採番テーブル＋行ロック、またはDBシーケンス |
| 画面遷移制御 | なし | ルートガード（未認証リダイレクト、権限チェック） |
| 帳票 | なし | PDF / Excel 出力（JasperReports、Apache POI など） |
| 一括処理 | なし | Spring Batch による夜間バッチ |
| ファイル添付 | なし | 添付書類のアップロード・ウイルスチェック |
| UIライブラリ | 素のCSS | Angular Material / PrimeNG など |
| 排他 | 楽観ロックのみ | 悲観ロック（`SELECT ... FOR UPDATE`）との併用 |

また、実案件が **MyBatis ではなく Spring Data JPA**、
**Maven ではなく Gradle**、
**Standalone ではなく NgModule ベース** である可能性もあります。
その場合の読み替え方は [docs/CATCHUP_GUIDE.md](docs/CATCHUP_GUIDE.md) の最後にまとめています。
