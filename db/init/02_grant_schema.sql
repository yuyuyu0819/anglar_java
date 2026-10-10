-- ============================================================================
--  申請管理システム  DB初期セットアップ 2/2
--
--  【実行タイミング】01_create_role_and_database.sql の直後に自動実行される。
--
--  【\connect について】
--  この行は psql のメタコマンドで、接続先データベースを切り替える。
--  docker-entrypoint-initdb.d のスクリプトは psql 経由で実行されるため使える。
--
--  pgAdmin の Query Tool では \connect は使えません。
--  pgAdmin から手動で流す場合は、この行を削除し、
--  左ツリーで shinsei_db を選択してから残りを実行してください。
-- ============================================================================

\connect shinsei_db

-- ----------------------------------------------------------------------------
-- public スキーマの権限設定
--
--  【PostgreSQL 15 からの変更点（実務で引っかかる箇所）】
--  14以前は public スキーマに対して全ユーザー(PUBLIC)が CREATE 権限を持っていたが、
--  15以降はセキュリティ強化のため削除された。
--  そのため「ロールは作ったのにテーブルが作れない」という事象が起きる。
--  （エラー: permission denied for schema public）
--
--  本手順ではDBのオーナーを shinsei_app にしているため実は権限は足りているが
--  （public スキーマの所有者が pg_database_owner で、DBオーナーがそれを兼ねるため）、
--  意図を明示するために所有権と権限を明示的に付与しておく。
-- ----------------------------------------------------------------------------
ALTER SCHEMA public OWNER TO shinsei_app;

GRANT USAGE, CREATE ON SCHEMA public TO shinsei_app;

-- ----------------------------------------------------------------------------
-- 既定権限（デフォルト権限）
--
--  今後 shinsei_app が public スキーマに作るテーブル／シーケンスに対して
--  自動で権限を付ける設定。
--
--  複数ロール（アプリ用／参照専用／バッチ用）を使い分ける案件では、
--  この ALTER DEFAULT PRIVILEGES を設定しておかないと
--  「テーブルを追加するたびに GRANT を流す」運用になってしまう。
--  本サンプルはロールが1つなので効果は限定的だが、書き方を押さえておくとよい。
-- ----------------------------------------------------------------------------
ALTER DEFAULT PRIVILEGES FOR ROLE shinsei_app IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO shinsei_app;

ALTER DEFAULT PRIVILEGES FOR ROLE shinsei_app IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO shinsei_app;

-- ----------------------------------------------------------------------------
-- 確認
-- ----------------------------------------------------------------------------
SELECT current_database() AS connected_database,
       n.nspname         AS schema_name,
       r.rolname         AS schema_owner
  FROM pg_namespace n
  JOIN pg_roles r ON r.oid = n.nspowner
 WHERE n.nspname = 'public';

-- ============================================================================
--  ここまででDBとロールの準備は完了。
--  テーブルの作成と初期データの投入はアプリ側で行います。
--
--      cd C:\develop\angular_java
--      .\start-backend.ps1 -Init
--
--  （init プロファイルで起動すると backend の
--    src/main/resources/schema.sql → data.sql が順に実行されます）
-- ============================================================================
