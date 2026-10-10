import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { isApiErrorResponse } from '../models/api-error.model';
import { NotificationService } from '../services/notification.service';

/**
 * APIエラーを共通処理するインターセプター。
 *
 * <h3>方針</h3>
 * <ul>
 *   <li><b>400（バリデーションエラー）はここで通知を出さない</b><br>
 *       項目別のメッセージを各入力欄の下に表示したいので、
 *       画面側（フォームコンポーネント）に処理を委ねる</li>
 *   <li>それ以外（404 / 409 / 500 / 通信不能）は共通のトーストを出す</li>
 *   <li>いずれの場合も `throwError` で例外を再送出し、
 *       呼び出し側が必要なら追加処理できるようにする<br>
 *       ※ ここで握りつぶすと、画面側が成功したと誤認するので危険</li>
 * </ul>
 *
 * この「共通で出すもの / 画面ごとに出すもの」の線引きを最初に決めておかないと、
 * 同じエラーが二重に表示される実装が各所に生まれる。
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // status が 0 → サーバに到達できていない（バックエンド未起動、ネットワーク断など）
      if (error.status === 0) {
        notificationService.error(
          'サーバに接続できませんでした。バックエンドが起動しているか確認してください。',
        );
        return throwError(() => error);
      }

      // バリデーションエラーは画面側で項目別に表示するため、ここでは何もしない
      if (error.status === 400) {
        return throwError(() => error);
      }

      // サーバが共通フォーマットのエラーを返していれば、そのメッセージを表示する。
      // 型ガードを通すことで、HTMLなど想定外のレスポンスでも落ちない
      if (isApiErrorResponse(error.error)) {
        notificationService.error(error.error.message, `エラーコード: ${error.error.code}`);
      } else {
        notificationService.error(
          `通信エラーが発生しました。(HTTP ${error.status})`,
          error.message,
        );
      }

      return throwError(() => error);
    }),
  );
};
