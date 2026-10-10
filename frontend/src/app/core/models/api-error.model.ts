/**
 * サーバが返すエラーレスポンスの型。
 * バックエンドの `ApiErrorResponse` と対応する。
 */
export interface FieldErrorDetail {
  /** エラーになった項目名（例: applicantName） */
  field: string;
  /** 画面に表示するメッセージ */
  message: string;
  /** 入力された値 */
  rejectedValue: unknown;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  /** 業務エラーコード（例: E409-002） */
  code: string;
  message: string;
  path: string;
  /** 項目別エラー。バリデーション以外では空配列 */
  fieldErrors: FieldErrorDetail[];
}

/**
 * HttpErrorResponse の `error` が ApiErrorResponse 形式かを判定する型ガード。
 *
 * `unknown` を受け取って `value is ApiErrorResponse` を返す関数を書くと、
 * TypeScript はこの関数が true を返した分岐の中で型を絞り込んでくれる。
 * サーバが落ちていてHTMLが返るようなケースを安全に弾けるので、実務では重要な小技。
 */
export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiErrorResponse>;
  return typeof candidate.code === 'string' && typeof candidate.message === 'string';
}
