import { Component, input } from '@angular/core';

import { ApplicationStatusCode } from '../../../core/models/application.model';

/**
 * ステータスを色付きのラベルで表示する共通コンポーネント。
 *
 * <h3>signal input（Angular 17.1 以降）</h3>
 * `input.required<T>()` / `input<T>(defaultValue)` で親から値を受け取る。
 * 受け取った値はシグナルなので、テンプレートでは `status()` と関数呼び出しで読む。
 *
 * 従来の書き方は `@Input() status!: ApplicationStatusCode;` で、
 * 既存の大規模コードベースではこちらが大半。
 * signal input の利点は「未設定を型で防げる（required）」
 * 「computed で派生値を自然に書ける」こと。
 *
 * <h3>inline template</h3>
 * テンプレートが数行の小さな部品は `template:` に直接書くことが多い。
 * 画面単位の大きなコンポーネントは `templateUrl:` でHTMLを分けるほうが読みやすい。
 */
@Component({
  selector: 'app-status-badge',
  template: `
    <span class="badge" [class]="'badge--' + status().toLowerCase()">{{ label() }}</span>
  `,
  styles: `
    .badge {
      display: inline-block;
      padding: 0.15rem 0.6rem;
      border-radius: 999px;
      font-size: 0.8125rem;
      font-weight: 600;
      line-height: 1.5;
      white-space: nowrap;
      border: 1px solid transparent;
    }
    /* 受付済 */
    .badge--received {
      background: #e8f0fb;
      color: #1a4f9c;
      border-color: #b8cfee;
    }
    /* 審査中 */
    .badge--reviewing {
      background: #fff4e0;
      color: #8a5200;
      border-color: #f0d4a3;
    }
    /* 承認 */
    .badge--approved {
      background: #e4f3ea;
      color: #1c6b3c;
      border-color: #b4ddc4;
    }
    /* 却下 */
    .badge--rejected {
      background: #fbe9e9;
      color: #a52121;
      border-color: #eec0c0;
    }
    /* 取下げ */
    .badge--withdrawn {
      background: #eeeeee;
      color: #555555;
      border-color: #d5d5d5;
    }
  `,
})
export class StatusBadge {
  /** ステータスコード。色の出し分けに使う */
  readonly status = input.required<ApplicationStatusCode>();

  /** 表示する日本語名。サーバから受け取った statusLabel を渡す */
  readonly label = input.required<string>();
}
