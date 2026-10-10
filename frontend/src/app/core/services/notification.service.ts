import { Injectable, signal } from '@angular/core';

export type NotificationLevel = 'success' | 'error' | 'info';

export interface Notification {
  id: number;
  level: NotificationLevel;
  message: string;
  /** 補足情報（業務エラーコードなど） */
  detail?: string;
}

/**
 * 画面右下に出す通知（トースト）を管理するサービス。
 *
 * <h3>Signal（シグナル）とは</h3>
 * Angular 16 で導入された状態管理の仕組み。
 * `signal([])` で「変更を検知できる値」を作り、
 *   - 読み取り: `items()`  … 関数呼び出しの形になる
 *   - 更新:     `set()` / `update()`
 * テンプレートで `items()` を参照しておくと、
 * 値が変わった箇所だけを Angular が自動で再描画してくれる。
 *
 * 従来は `BehaviorSubject` + `| async` パイプで同じことをしていた。
 * Angular 20 では Signal が標準的な選択肢になっている。
 *
 * <h3>公開の仕方</h3>
 * 書き込み可能な signal は private に隠し、
 * 外部には `asReadonly()` で読み取り専用として渡す。
 * こうすると「状態を変えられるのはこのサービスだけ」を型で保証できる。
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly items = signal<Notification[]>([]);

  /** テンプレートから購読する読み取り専用シグナル */
  readonly notifications = this.items.asReadonly();

  private nextId = 1;

  /** 自動で閉じるまでの時間（ミリ秒） */
  private static readonly AUTO_DISMISS_MS = 6000;

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string, detail?: string): void {
    this.push('error', message, detail);
  }

  info(message: string): void {
    this.push('info', message);
  }

  /** 利用者が×ボタンを押したときに呼ぶ */
  dismiss(id: number): void {
    this.items.update((current) => current.filter((item) => item.id !== id));
  }

  private push(level: NotificationLevel, message: string, detail?: string): void {
    const id = this.nextId++;
    // update() には「現在の値を受け取って新しい値を返す関数」を渡す。
    // 配列を直接 push すると参照が変わらず変更を検知できないため、
    // 必ず新しい配列を作る（イミュータブルな更新）
    this.items.update((current) => [...current, { id, level, message, detail }]);

    setTimeout(() => this.dismiss(id), NotificationService.AUTO_DISMISS_MS);
  }
}
