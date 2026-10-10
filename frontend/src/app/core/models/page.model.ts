/**
 * ページング結果の共通型。
 *
 * バックエンドの `jp.go.sample.backend.common.dto.PageResponse` と1対1で対応する。
 * サーバのJSON構造と型を揃えておくと、項目名の打ち間違いをコンパイル時に検出できる。
 *
 * `interface` は TypeScript の「型の定義だけ」の構文で、
 * JavaScript にコンパイルされると消える（実行時には存在しない）。
 */
export interface PageResponse<T> {
  /** このページの明細 */
  content: T[];
  /** 現在のページ番号（0始まり） */
  page: number;
  /** 1ページあたりの件数 */
  size: number;
  /** 条件に一致する総件数 */
  totalElements: number;
  /** 総ページ数 */
  totalPages: number;
  /** 先頭ページか */
  first: boolean;
  /** 最終ページか */
  last: boolean;
}

/** 空のページング結果（初期表示や検索失敗時のフォールバックに使う） */
export function emptyPage<T>(size = 10): PageResponse<T> {
  return {
    content: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };
}
