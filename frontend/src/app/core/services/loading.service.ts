import { Injectable, computed, signal } from '@angular/core';

/**
 * 通信中インジケータの表示状態を管理するサービス。
 *
 * <h3>なぜ boolean ではなくカウンタなのか</h3>
 * 同時に複数のリクエストが走ることがある（一覧取得とマスタ取得など）。
 * boolean で管理すると、先に終わった1本が `false` にしてしまい、
 * まだ通信中なのにインジケータが消えてしまう。
 * 「実行中の本数」を数え、0 になったときだけ非表示にするのが定石。
 *
 * <h3>computed</h3>
 * `computed()` は他のシグナルから導出される値。
 * 元の値（ここでは activeRequests）が変わったときだけ再計算される。
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly activeRequests = signal(0);

  /** 通信中かどうか（テンプレートからはこれを参照する） */
  readonly isLoading = computed(() => this.activeRequests() > 0);

  start(): void {
    this.activeRequests.update((count) => count + 1);
  }

  stop(): void {
    // 念のため 0 未満にならないようガードする
    this.activeRequests.update((count) => Math.max(0, count - 1));
  }
}
