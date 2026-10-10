import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { LoadingOverlay } from './shared/components/loading-overlay/loading-overlay';
import { ToastContainer } from './shared/components/toast-container/toast-container';

/**
 * アプリの外枠（シェル）コンポーネント。
 *
 * ヘッダー・ナビゲーション・フッターのように全画面で共通の要素を置き、
 * 画面ごとに切り替わる部分を `<router-outlet />` に差し込む。
 *
 * `index.html` の `<app-root></app-root>` がこのコンポーネントに置き換わる
 * （`selector: 'app-root'` が対応している）。
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LoadingOverlay, ToastContainer],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  /** システム名。ヘッダーに表示する */
  protected readonly systemName = '申請管理システム（キャッチアップ用サンプル）';
}
