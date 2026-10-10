import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';

import { errorInterceptor } from './core/interceptors/error.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { routes } from './app.routes';

/**
 * アプリ全体の設定（DIプロバイダの登録）。
 *
 * standalone 構成では、かつて `AppModule` の `imports` / `providers` に
 * 書いていた内容をここに集約する。
 * `main.ts` の `bootstrapApplication(App, appConfig)` から渡される。
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // 未処理の例外やPromise rejectionをAngularのエラーハンドラに流す
    provideBrowserGlobalErrorListeners(),

    // 変更検知の設定。eventCoalescing は短時間に連続して起きたイベントを
    // まとめて1回の変更検知にすることで無駄な再描画を減らす
    provideZoneChangeDetection({ eventCoalescing: true }),

    provideRouter(
      routes,
      // ルートパラメータ（:id）をコンポーネントの input に自動バインドする。
      // これが無いと ApplicationDetail の `id = input.required()` に値が入らない
      withComponentInputBinding(),
      // 画面遷移時にスクロール位置を先頭へ戻す。
      // 指定しないと、一覧の下の方から詳細へ遷移したとき途中から表示されてしまう
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),

    /*
     * HttpClient の登録。
     *
     * withInterceptors() に渡した関数は <b>配列の順番どおり</b>に
     * リクエストを処理する（レスポンスは逆順に通る）。
     *   loading → error → 実際の通信 → error → loading
     * ローディング解除を最後に行いたいので loading を先に置いている。
     *
     * 認証トークンを付与するインターセプターを追加する場合もここに並べる。
     */
    provideHttpClient(withInterceptors([loadingInterceptor, errorInterceptor])),
  ],
};
