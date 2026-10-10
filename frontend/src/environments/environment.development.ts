/**
 * 開発時（ng serve / ng build --configuration development）で使われる環境設定。
 *
 * apiBaseUrl は '/api' のまま。実体への転送は proxy.conf.json が担当し、
 * localhost:4200/api/** → localhost:8080/api/** に中継される。
 *
 * この「プロキシ方式」の利点:
 *   - ブラウザから見ると同一オリジンになるのでCORSを気にしなくてよい
 *   - 環境ごとにURLを書き換えなくて済む（コードは常に '/api'）
 *
 * プロキシを使わず直接 http://localhost:8080/api を叩く構成にもできるが、
 * その場合はサーバ側のCORS設定（backend の WebMvcConfig）が必須になる。
 */
export const environment = {
  production: false,
  apiBaseUrl: '/api',
};
