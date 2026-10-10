# frontend（Angular 20.3）

申請管理システムのフロントエンドです。

**セットアップ・起動手順・学習ガイドはリポジトリ直下のドキュメントを参照してください。**

- [../README.md](../README.md) … 起動手順・ディレクトリ構成・API一覧
- [../docs/CATCHUP_GUIDE.md](../docs/CATCHUP_GUIDE.md) … 学習ガイド

## このディレクトリ単体でのコマンド

```powershell
npm start                                              # 開発サーバ起動 (http://localhost:4200)
npm run build                                          # 本番ビルド → dist\frontend\browser
npm test -- --watch=false --browsers=ChromeHeadless    # ユニットテスト（Chrome必須）
npx ng generate component features/xxx/yyy             # コンポーネントの雛形生成
```

`npm start` は `/api/**` へのリクエストを `proxy.conf.json` に従って
`http://localhost:8080`（バックエンド）へ転送します。
**バックエンドを先に起動しておいてください。**
