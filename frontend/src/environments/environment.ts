/**
 * 本番ビルド（ng build）で使われる環境設定。
 *
 * angular.json の production 構成に fileReplacements が定義されており、
 * 開発時は environment.development.ts がこのファイルを置き換える。
 * 「環境ごとに変わる値はここに集約し、コンポーネントに直書きしない」のが原則。
 *
 * apiBaseUrl を相対パス '/api' にしているのは、
 * 本番では Angular の成果物と API を同一ドメインから配信する想定のため
 * （リバースプロキシや同一サーバ配信）。この場合CORSは発生しない。
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
};
