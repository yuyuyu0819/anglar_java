import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApplicationCategory, StatusOption } from '../models/master.model';

/**
 * マスタ（申請区分・ステータス）取得サービス。
 *
 * <h3>shareReplay によるキャッシュ</h3>
 * マスタは画面を開くたびに取り直す必要がない。
 * `shareReplay({ bufferSize: 1, refCount: false })` を挟むと、
 *   1回目の購読 … 実際にHTTPリクエストが飛ぶ
 *   2回目以降   … 保持済みの結果が即座に返る（通信なし）
 * となり、無駄な通信を抑えられる。
 *
 * マスタが頻繁に変わる場合は `refresh()` のように
 * キャッシュを破棄する手段を用意しておく。
 */
@Injectable({ providedIn: 'root' })
export class MasterService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/master`;

  private categories$?: Observable<ApplicationCategory[]>;
  private statuses$?: Observable<StatusOption[]>;

  /** 申請区分の一覧（キャッシュあり） */
  getCategories(): Observable<ApplicationCategory[]> {
    this.categories$ ??= this.http
      .get<ApplicationCategory[]>(`${this.baseUrl}/categories`)
      .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.categories$;
  }

  /** ステータスの一覧（キャッシュあり） */
  getStatuses(): Observable<StatusOption[]> {
    this.statuses$ ??= this.http
      .get<StatusOption[]>(`${this.baseUrl}/statuses`)
      .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.statuses$;
  }

  /** キャッシュを破棄して次回取得時に再通信させる */
  clearCache(): void {
    this.categories$ = undefined;
    this.statuses$ = undefined;
  }
}
