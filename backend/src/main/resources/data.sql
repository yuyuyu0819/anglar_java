-- ============================================================================
--  初期データ投入（学習用のテストデータ）
--
--  schema.sql の後に実行される。
--  実案件でも「コードマスタの初期値」はこの種のSQLやマイグレーションで投入する。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 申請区分マスタ
-- ----------------------------------------------------------------------------
INSERT INTO application_category (code, name, display_order) VALUES
    ('BLD', '建築確認申請',     10),
    ('RSD', '住民票交付申請',   20),
    ('TAX', '税証明交付申請',   30),
    ('SUB', '補助金交付申請',   40),
    ('EVT', '施設利用許可申請', 50);

-- ----------------------------------------------------------------------------
-- 申請データ
--   ステータスを散らしてあるので、一覧の絞り込みや状態遷移の確認に使える。
--   id は IDENTITY 列なので明示せず、DBに採番させる。
-- ----------------------------------------------------------------------------
INSERT INTO application
    (application_no, applicant_name, applicant_email, category_code, status, applied_on, note, version, created_at, updated_at)
VALUES
    ('APP-2026-00001', '山田 太郎',   'yamada.taro@example.jp',   'BLD', 'RECEIVED',  DATE '2026-09-01', '木造2階建て住宅の新築',                1, TIMESTAMP '2026-09-01 09:15:00', TIMESTAMP '2026-09-01 09:15:00'),
    ('APP-2026-00002', '鈴木 花子',   'suzuki.hanako@example.jp', 'RSD', 'APPROVED',  DATE '2026-09-03', '本人分1通',                            2, TIMESTAMP '2026-09-03 10:00:00', TIMESTAMP '2026-09-04 14:20:00'),
    ('APP-2026-00003', '佐藤 次郎',   'sato.jiro@example.jp',     'TAX', 'REVIEWING', DATE '2026-09-05', '所得課税証明書（令和8年度分）',        2, TIMESTAMP '2026-09-05 11:30:00', TIMESTAMP '2026-09-06 09:05:00'),
    ('APP-2026-00004', '高橋 三郎',   'takahashi@example.jp',     'SUB', 'REJECTED',  DATE '2026-09-08', '添付書類の不足により却下',              3, TIMESTAMP '2026-09-08 13:45:00', TIMESTAMP '2026-09-12 16:30:00'),
    ('APP-2026-00005', '田中 美咲',   'tanaka.misaki@example.jp', 'EVT', 'RECEIVED',  DATE '2026-09-10', '市民会館 大ホール（10月15日）',        1, TIMESTAMP '2026-09-10 08:50:00', TIMESTAMP '2026-09-10 08:50:00'),
    ('APP-2026-00006', '伊藤 健一',   'ito.kenichi@example.jp',   'BLD', 'REVIEWING', DATE '2026-09-12', '店舗併用住宅の増築',                    2, TIMESTAMP '2026-09-12 09:20:00', TIMESTAMP '2026-09-15 10:10:00'),
    ('APP-2026-00007', '渡辺 由美',   'watanabe@example.jp',      'RSD', 'WITHDRAWN', DATE '2026-09-14', '申請者より取下げの申し出',              2, TIMESTAMP '2026-09-14 15:00:00', TIMESTAMP '2026-09-16 11:00:00'),
    ('APP-2026-00008', '中村 大輔',   'nakamura@example.jp',      'TAX', 'APPROVED',  DATE '2026-09-17', '納税証明書（その3）',                   2, TIMESTAMP '2026-09-17 10:40:00', TIMESTAMP '2026-09-18 09:30:00'),
    ('APP-2026-00009', '小林 さくら', 'kobayashi@example.jp',     'SUB', 'RECEIVED',  DATE '2026-09-20', '住宅省エネ改修補助金',                  1, TIMESTAMP '2026-09-20 14:10:00', TIMESTAMP '2026-09-20 14:10:00'),
    ('APP-2026-00010', '加藤 誠',     'kato.makoto@example.jp',   'EVT', 'REVIEWING', DATE '2026-09-22', '体育館（11月3日・地域大会）',          2, TIMESTAMP '2026-09-22 09:00:00', TIMESTAMP '2026-09-25 13:25:00'),
    ('APP-2026-00011', '吉田 洋子',   'yoshida.yoko@example.jp',  'BLD', 'APPROVED',  DATE '2026-09-24', '車庫の新築',                            2, TIMESTAMP '2026-09-24 11:15:00', TIMESTAMP '2026-09-28 15:40:00'),
    ('APP-2026-00012', '山口 智也',   'yamaguchi@example.jp',     'RSD', 'RECEIVED',  DATE '2026-09-28', '世帯全員分',                            1, TIMESTAMP '2026-09-28 16:20:00', TIMESTAMP '2026-09-28 16:20:00'),
    ('APP-2026-00013', '松本 久美',   'matsumoto@example.jp',     'TAX', 'RECEIVED',  DATE '2026-10-01', '事業用（金融機関提出）',                1, TIMESTAMP '2026-10-01 09:35:00', TIMESTAMP '2026-10-01 09:35:00'),
    ('APP-2026-00014', '井上 隆',     'inoue.takashi@example.jp', 'SUB', 'REVIEWING', DATE '2026-10-02', '中小企業デジタル化支援補助金',          2, TIMESTAMP '2026-10-02 10:05:00', TIMESTAMP '2026-10-03 14:00:00'),
    ('APP-2026-00015', '木村 彩',     'kimura.aya@example.jp',    'EVT', 'RECEIVED',  DATE '2026-10-05', '会議室A（10月20日 午後）',             1, TIMESTAMP '2026-10-05 13:50:00', TIMESTAMP '2026-10-05 13:50:00');
