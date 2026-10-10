/**
 * 申請に関する型定義。
 *
 * バックエンドの DTO（ApplicationResponse / ApplicationCreateRequest など）と
 * 項目名・型を揃えてある。
 */

/**
 * ステータスのコード。
 *
 * ユニオン型（`'A' | 'B'`）で定義すると、`'RECEIVE'` のような打ち間違いを
 * コンパイル時に検出できる。TypeScript には列挙のための `enum` もあるが、
 * Angular では文字列ユニオン型を使うのが一般的
 * （実行時のコードが増えず、JSONとそのまま対応するため）。
 */
export type ApplicationStatusCode =
  | 'RECEIVED'
  | 'REVIEWING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN';

/** 申請1件（サーバからの受信用） */
export interface Application {
  id: number;
  applicationNo: string;
  applicantName: string;
  applicantEmail: string;
  categoryCode: string;
  /** JOINで取得した申請区分名 */
  categoryName: string;
  status: ApplicationStatusCode;
  /** ステータスの日本語名（サーバ側で付与） */
  statusLabel: string;
  /** 'YYYY-MM-DD' 形式。JSONに日付型は無いので文字列で受け取る */
  appliedOn: string;
  note: string | null;
  /** 楽観ロック用。更新時にそのまま送り返す */
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 一覧検索のリクエストパラメータ */
export interface ApplicationSearchParams {
  keyword?: string;
  status?: ApplicationStatusCode | '';
  categoryCode?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/** 登録リクエストのボディ */
export interface ApplicationCreatePayload {
  applicantName: string;
  applicantEmail: string;
  categoryCode: string;
  appliedOn: string;
  note: string | null;
}

/**
 * 更新リクエストのボディ。
 *
 * `ApplicationCreatePayload & { version: number }` と交差型で書くことで
 * 「登録項目 + バージョン」であることを型で表現している。
 */
export type ApplicationUpdatePayload = ApplicationCreatePayload & {
  version: number;
};

/** ステータス変更リクエストのボディ */
export interface ApplicationStatusUpdatePayload {
  status: ApplicationStatusCode;
  version: number;
}
