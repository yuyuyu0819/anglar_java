import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Application,
  ApplicationCreatePayload,
  ApplicationSearchParams,
  ApplicationStatusUpdatePayload,
  ApplicationUpdatePayload,
} from '../models/application.model';
import { PageResponse } from '../models/page.model';

/**
 * 申請APIとの通信を担うサービス。
 *
 * <h3>Angular の DI（依存性の注入）</h3>
 * `@Injectable({ providedIn: 'root' })` を付けると、
 * アプリ全体で1つだけのインスタンス（シングルトン）が自動生成され、
 * 使う側は `inject(ApplicationService)` と書くだけで受け取れる。
 * Spring の `@Service` + コンストラクタインジェクションとほぼ同じ発想。
 *
 * <h3>設計上のルール</h3>
 * HTTP通信はこの層に閉じ込め、コンポーネントからは `HttpClient` を直接触らない。
 * そうしておくとAPIのURLやパラメータ名が変わっても修正が1ファイルで済む。
 *
 * <h3>Observable について</h3>
 * `HttpClient` のメソッドは `Observable` を返すが、
 * これは「購読（subscribe）した時点で初めてリクエストが飛ぶ」遅延実行の仕組み。
 * Promise と違い、呼んだだけでは通信は発生しない点に注意。
 */
@Injectable({ providedIn: 'root' })
export class ApplicationService {
  /**
   * `inject()` 関数による注入。
   * コンストラクタ引数で受け取る従来の書き方（`constructor(private http: HttpClient)`）でもよいが、
   * Angular 14 以降はこの形が推奨されている。継承時に親のコンストラクタ引数を
   * 書き写す必要がなくなるのが利点。
   */
  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiBaseUrl}/applications`;

  /**
   * 一覧検索。
   *
   * `HttpParams` はクエリ文字列を組み立てるためのクラス。
   * イミュータブル（`set` は新しいインスタンスを返す）なので、
   * 戻り値を再代入しないと反映されない点に注意。
   *
   * 値が空のパラメータは送らない。空文字を送ると
   * サーバ側で「空文字での絞り込み」と解釈されかねないため。
   */
  search(params: ApplicationSearchParams): Observable<PageResponse<Application>> {
    let httpParams = new HttpParams();

    if (params.keyword) {
      httpParams = httpParams.set('keyword', params.keyword);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.categoryCode) {
      httpParams = httpParams.set('categoryCode', params.categoryCode);
    }
    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 10);
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    if (params.direction) {
      httpParams = httpParams.set('direction', params.direction);
    }

    // ジェネリクスで戻り値の型を指定する。
    // ※ これは「サーバがこの形で返すと信じる」宣言であって実行時の検証はされない。
    //    サーバとフロントで型定義を合わせておくことが前提になる。
    return this.http.get<PageResponse<Application>>(this.baseUrl, { params: httpParams });
  }

  /** 1件取得 */
  findById(id: number): Observable<Application> {
    return this.http.get<Application>(`${this.baseUrl}/${id}`);
  }

  /** 新規登録 */
  create(payload: ApplicationCreatePayload): Observable<Application> {
    return this.http.post<Application>(this.baseUrl, payload);
  }

  /** 更新 */
  update(id: number, payload: ApplicationUpdatePayload): Observable<Application> {
    return this.http.put<Application>(`${this.baseUrl}/${id}`, payload);
  }

  /** ステータス変更 */
  updateStatus(id: number, payload: ApplicationStatusUpdatePayload): Observable<Application> {
    return this.http.patch<Application>(`${this.baseUrl}/${id}/status`, payload);
  }

  /** 削除。レスポンスボディが無い（204）ので `void` を指定する */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
