import { Component, inject } from '@angular/core';

import { NotificationService } from '../../../core/services/notification.service';

/**
 * 画面右下に通知を積み上げて表示するコンポーネント。
 *
 * <h3>@for（Angular 17 以降の繰り返し構文）</h3>
 * 以前の `*ngFor="let x of items"` に相当する。
 * `track` の指定が<b>必須</b>になった点が大きな違い。
 * track は「どの要素が同じものか」を Angular に教えるためのキーで、
 * 適切に指定すると再描画時にDOMを作り直さず差分更新できる。
 * 一意なIDがあればそれを、無ければ `$index` を使う。
 */
@Component({
  selector: 'app-toast-container',
  template: `
    <div class="toasts" aria-live="assertive">
      @for (toast of notificationService.notifications(); track toast.id) {
        <div class="toast" [class]="'toast--' + toast.level">
          <div class="toast__body">
            <p class="toast__message">{{ toast.message }}</p>
            @if (toast.detail) {
              <p class="toast__detail">{{ toast.detail }}</p>
            }
          </div>
          <button
            type="button"
            class="toast__close"
            aria-label="閉じる"
            (click)="notificationService.dismiss(toast.id)"
          >
            ×
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      z-index: 1100;
      max-width: min(26rem, calc(100vw - 2rem));
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      padding: 0.7rem 0.8rem;
      border-radius: 4px;
      border-left: 4px solid;
      background: #ffffff;
      box-shadow: 0 2px 10px rgb(0 0 0 / 18%);
      font-size: 0.875rem;
    }
    .toast--success {
      border-left-color: #1c6b3c;
    }
    .toast--error {
      border-left-color: #a52121;
    }
    .toast--info {
      border-left-color: #1a4f9c;
    }
    .toast__body {
      flex: 1;
    }
    .toast__message {
      margin: 0;
      line-height: 1.6;
      color: #212121;
    }
    .toast__detail {
      margin: 0.25rem 0 0;
      font-size: 0.75rem;
      color: #666666;
    }
    .toast__close {
      border: none;
      background: none;
      cursor: pointer;
      font-size: 1.1rem;
      line-height: 1;
      color: #777777;
      padding: 0 0.2rem;
    }
    .toast__close:hover {
      color: #222222;
    }
  `,
})
export class ToastContainer {
  protected readonly notificationService = inject(NotificationService);
}
