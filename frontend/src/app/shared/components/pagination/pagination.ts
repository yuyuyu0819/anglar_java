import { Component, computed, input, output } from '@angular/core';

/**
 * ページ送りコンポーネント。
 *
 * <h3>親子間のデータ受け渡し</h3>
 * <ul>
 *   <li><b>input</b> … 親 → 子。`input()` で受け取る（旧 `@Input()`）</li>
 *   <li><b>output</b> … 子 → 親。`output()` で定義し `emit()` で通知する（旧 `@Output() EventEmitter`）</li>
 * </ul>
 * 「子は自分で状態を変えず、親に『変えてほしい』と伝えるだけ」にしておくと、
 * データの流れが一方向になり追いやすくなる。
 *
 * <h3>computed</h3>
 * 表示用のページ番号リストは input から導出できる値なので `computed()` にする。
 * メソッドにして `{{ buildPages() }}` と呼ぶ書き方でも動くが、
 * その場合は変更検知のたびに毎回再計算される。
 * computed は依存する値が変わったときだけ再計算されるため効率がよい。
 */
@Component({
  selector: 'app-pagination',
  template: `
    <nav class="pager" aria-label="ページ送り">
      <p class="pager__summary">
        全 <strong>{{ totalElements() }}</strong> 件
        @if (totalElements() > 0) {
          （{{ rangeFrom() }} - {{ rangeTo() }} 件目を表示）
        }
      </p>

      @if (totalPages() > 1) {
        <ul class="pager__list">
          <li>
            <button
              type="button"
              class="pager__btn"
              [disabled]="page() === 0"
              (click)="pageChange.emit(page() - 1)"
            >
              前へ
            </button>
          </li>

          @for (p of visiblePages(); track p) {
            <li>
              <button
                type="button"
                class="pager__btn"
                [class.pager__btn--current]="p === page()"
                [attr.aria-current]="p === page() ? 'page' : null"
                (click)="pageChange.emit(p)"
              >
                {{ p + 1 }}
              </button>
            </li>
          }

          <li>
            <button
              type="button"
              class="pager__btn"
              [disabled]="page() >= totalPages() - 1"
              (click)="pageChange.emit(page() + 1)"
            >
              次へ
            </button>
          </li>
        </ul>
      }
    </nav>
  `,
  styles: `
    .pager {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      margin-top: 1rem;
    }
    .pager__summary {
      margin: 0;
      font-size: 0.875rem;
      color: #444444;
    }
    .pager__list {
      display: flex;
      gap: 0.25rem;
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .pager__btn {
      min-width: 2.25rem;
      padding: 0.35rem 0.6rem;
      border: 1px solid #c3cedb;
      background: #ffffff;
      color: #1a4f9c;
      border-radius: 3px;
      cursor: pointer;
      font-size: 0.875rem;
    }
    .pager__btn:hover:not(:disabled) {
      background: #eef3fa;
    }
    .pager__btn:disabled {
      color: #aaaaaa;
      cursor: not-allowed;
      background: #f5f5f5;
    }
    .pager__btn--current {
      background: #1a4f9c;
      border-color: #1a4f9c;
      color: #ffffff;
      font-weight: 700;
    }
  `,
})
export class Pagination {
  /** 現在のページ（0始まり） */
  readonly page = input.required<number>();
  /** 総ページ数 */
  readonly totalPages = input.required<number>();
  /** 総件数 */
  readonly totalElements = input.required<number>();
  /** 1ページあたりの件数 */
  readonly size = input.required<number>();

  /**
   * ページ変更の通知。親は `(pageChange)="onPageChange($event)"` で受け取る。
   * 旧構文の `@Output() pageChange = new EventEmitter<number>();` と等価。
   */
  readonly pageChange = output<number>();

  /** 「◯ - ◯件目」の開始番号 */
  protected readonly rangeFrom = computed(() => this.page() * this.size() + 1);

  /** 「◯ - ◯件目」の終了番号。最終ページでは総件数で止める */
  protected readonly rangeTo = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.totalElements()),
  );

  /**
   * 表示するページ番号のリスト。
   * 総ページ数が多い場合は現在ページの前後2ページ分だけを出す
   * （全ページ分のボタンを並べると画面が崩れるため）。
   */
  protected readonly visiblePages = computed<number[]>(() => {
    const total = this.totalPages();
    const current = this.page();
    const window = 2;

    const start = Math.max(0, Math.min(current - window, total - (window * 2 + 1)));
    const end = Math.min(total - 1, Math.max(current + window, window * 2));

    const pages: number[] = [];
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  });
}
