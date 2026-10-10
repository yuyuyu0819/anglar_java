-- ============================================================================
--  申請管理システム  DB初期セットアップ 1/2
--
--  【実行タイミング】
--  docker-compose.yml が ./db/init を /docker-entrypoint-initdb.d にマウントして
--  いるため、PostgreSQL コンテナの <b>初回起動時にのみ</b> 自動実行される
--  （正確には、データボリュームが空の状態で起動したとき）。
--  スーパーユーザー postgres として postgres データベースに接続した状態で動く。
--
--  やり直したい場合はボリュームごと作り直す:
--      docker compose down -v
--      docker compose up -d
--  （.\start-database.ps1 -Reset でも同じことができる）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. アプリケーション用ロール（ユーザー）の作成
--
--    実案件ではスーパーユーザー(postgres)でアプリを動かすことはなく、
--    必要最小限の権限を持つ専用ロールを作る。
--      LOGIN        … このロールでDBに接続できる
--      NOSUPERUSER  … スーパーユーザー権限は与えない
--      NOCREATEDB   … 他のDBを作る権限は与えない
--      NOCREATEROLE … 他のロールを作る権限は与えない
--
--    パスワードは学習用。実案件では必ず外部（Secrets Manager / Vault /
--    Kubernetes Secret など）から注入し、SQLやソースに書かない。
-- ----------------------------------------------------------------------------
CREATE ROLE shinsei_app
    WITH LOGIN
         NOSUPERUSER
         NOCREATEDB
         NOCREATEROLE
         NOINHERIT
         CONNECTION LIMIT -1
         PASSWORD 'shinsei_pass';

COMMENT ON ROLE shinsei_app IS '申請管理システム アプリケーション接続用ロール';

-- ----------------------------------------------------------------------------
-- 2. データベースの作成
--
--    【TEMPLATE template0 を指定する理由】
--    CREATE DATABASE は既定で template1 を複製し、そのロケール設定を引き継ぐ。
--    クラスタのロケールと ENCODING が合わないと
--        ERROR: encoding "UTF8" does not match locale "..."
--    になる。template0（ロケール中立のテンプレート）を使えば
--    ロケールとエンコーディングを自由に指定できる。
--
--    本サンプルの docker-compose.yml は initdb に --encoding=UTF8 --locale=C を
--    渡しているため実は template1 のままでも通るが、
--    Windows に直接インストールした PostgreSQL（既定ロケールが
--    Japanese_Japan.932 = Shift_JIS になりがち）でもそのまま動くよう
--    明示的に template0 を指定している。
--
--    【LC_COLLATE='C' の意味】
--    ORDER BY の並び順がバイト順（UTF-8ではほぼUnicodeコードポイント順）になる。
--    漢字は読み順にはならないが、これはどのDBでも同じ。
--    'C' は決定的で速く、インデックスも効きやすいため実案件でもよく選ばれる。
-- ----------------------------------------------------------------------------
CREATE DATABASE shinsei_db
    WITH OWNER      = shinsei_app
         ENCODING   = 'UTF8'
         TEMPLATE   = template0
         LC_COLLATE = 'C'
         LC_CTYPE   = 'C'
         CONNECTION LIMIT = -1;

COMMENT ON DATABASE shinsei_db IS '申請管理システム（キャッチアップ用サンプル）';

-- ----------------------------------------------------------------------------
-- 3. 確認
--    期待値: encoding=UTF8 / collate=C / ctype=C / owner=shinsei_app
-- ----------------------------------------------------------------------------
SELECT d.datname                        AS database_name,
       pg_encoding_to_char(d.encoding)  AS encoding,
       d.datcollate                     AS collate,
       d.datctype                       AS ctype,
       r.rolname                        AS owner
  FROM pg_database d
  JOIN pg_roles r ON r.oid = d.datdba
 WHERE d.datname = 'shinsei_db';
