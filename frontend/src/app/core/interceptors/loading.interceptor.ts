import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { LoadingService } from '../services/loading.service';

/**
 * 通信中インジケータを自動で出すインターセプター。
 *
 * <h3>HTTPインターセプターとは</h3>
 * すべてのHTTPリクエスト・レスポンスが必ず通る共通処理。
 * 認証トークンの付与、ログ出力、エラー処理、ローディング表示といった
 * 「全APIに共通してやりたいこと」を1か所にまとめられる。
 * Spring の `Filter` / `HandlerInterceptor` と同じ役割。
 *
 * <h3>関数型インターセプター</h3>
 * Angular 15 以降は `HttpInterceptorFn` 型の関数として書く。
 * 古い案件では `HttpInterceptor` インターフェースを実装したクラス +
 * `HTTP_INTERCEPTORS` プロバイダ、という書き方になっているはず
 * （どちらも動作は同じ）。
 *
 * `next(req)` を呼ぶと後続のインターセプター、最終的に実際の通信へ処理が渡る。
 *
 * <h3>finalize</h3>
 * 成功・失敗・購読解除のいずれでも必ず実行される。
 * `stop()` の呼び忘れでインジケータが出たままになるのを防げる。
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  loadingService.start();

  return next(req).pipe(finalize(() => loadingService.stop()));
};
