import { ApplicationStatusCode } from './application.model';

/** 申請区分マスタ（プルダウン用） */
export interface ApplicationCategory {
  code: string;
  name: string;
  displayOrder: number;
}

/**
 * ステータスの選択肢。
 *
 * `allowedNextStatuses` にはサーバ側 enum が持つ「遷移可能な次ステータス」が入る。
 * これを使うことで、画面側に状態遷移ルールを書かずに
 * 操作できるボタンだけを出す、という作りにできる
 * （業務ルールの二重管理を避けられる）。
 */
export interface StatusOption {
  code: ApplicationStatusCode;
  label: string;
  allowedNextStatuses: ApplicationStatusCode[];
}
