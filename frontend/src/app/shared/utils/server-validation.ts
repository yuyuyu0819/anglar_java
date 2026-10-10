import { HttpErrorResponse } from '@angular/common/http';

import { isApiErrorResponse } from '../../core/models/api-error.model';

/**
 * サーバが返した項目別バリデーションエラーを
 * 「項目名 → メッセージ」のマップに変換する。
 *
 * サーバの ApiErrorResponse.fieldErrors は
 *   [{ field: 'applicantName', message: '申請者氏名は必須です' }, ...]
 * という配列なので、画面から項目名で引けるようオブジェクトに詰め替える。
 *
 * @returns 400 以外、または fieldErrors が無い場合は空オブジェクト
 */
export function toFieldErrorMap(error: unknown): Record<string, string> {
  if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
    return {};
  }
  if (!isApiErrorResponse(error.error)) {
    return {};
  }

  // reduce で配列 → オブジェクトに畳み込む。
  // `Object.fromEntries(...)` を使って書くこともできる
  return (error.error.fieldErrors ?? []).reduce<Record<string, string>>((acc, fieldError) => {
    acc[fieldError.field] = fieldError.message;
    return acc;
  }, {});
}
