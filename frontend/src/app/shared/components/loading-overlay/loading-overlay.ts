import { Component, inject } from '@angular/core';

import { LoadingService } from '../../../core/services/loading.service';

/**
 * 通信中に画面全体を薄く覆うインジケータ。
 *
 * `LoadingService` のシグナルを直接参照しているため、
 * 親コンポーネントから状態を渡す必要がない。
 * loadingInterceptor がカウンタを増減させ、この表示が自動で切り替わる。
 *
 * <h3>@if（Angular 17 以降の制御フロー構文）</h3>
 * 以前は `<div *ngIf="cond">` と書いていたが、
 * 現在は `@if (cond) { ... }` というテンプレート構文が推奨されている。
 * `CommonModule` の import が不要になり、型チェックも効きやすい。
 * 旧構文も引き続き動作するため、既存案件では `*ngIf` が残っていることが多い。
 */
@Component({
  selector: 'app-loading-overlay',
  template: `
    @if (loadingService.isLoading()) {
      <div class="overlay" role="status" aria-live="polite">
        <div class="overlay__box">
          <span class="overlay__spinner" aria-hidden="true"></span>
          <span>通信中...</span>
        </div>
      </div>
    }
  `,
  styles: `
    .overlay {
      position: fixed;
      inset: 0;
      background: rgb(255 255 255 / 45%);
      display: flex;
      align-items: flex-start;
      justify-content: center;
      z-index: 1000;
      pointer-events: none;
    }
    .overlay__box {
      margin-top: 5rem;
      display: flex;
      align-items: center;
      gap: 0.6rem;
      padding: 0.6rem 1.1rem;
      background: #ffffff;
      border: 1px solid #c8d4e3;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
      font-size: 0.875rem;
      color: #1b3b66;
    }
    .overlay__spinner {
      width: 1rem;
      height: 1rem;
      border: 2px solid #c8d4e3;
      border-top-color: #1a4f9c;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `,
})
export class LoadingOverlay {
  // テンプレートから参照するため protected / public にする（private だと参照できない）
  protected readonly loadingService = inject(LoadingService);
}
