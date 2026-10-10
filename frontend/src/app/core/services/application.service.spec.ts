import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Application } from '../models/application.model';
import { PageResponse } from '../models/page.model';
import { ApplicationService } from './application.service';

/**
 * ApplicationService のテスト。
 *
 * <h3>HttpTestingController</h3>
 * 実際の通信を行わず、「どんなリクエストが飛んだか」を検証し、
 * 「任意のレスポンスを返す」ことができるテスト用ツール。
 *
 * 検証したいのは主に次の2点。
 *   1. 組み立てたURL・クエリパラメータ・HTTPメソッドが正しいか
 *   2. レスポンスを受け取った後の処理が正しいか
 *
 * `afterEach` の `verify()` は「待機中の未処理リクエストが残っていないか」を
 * 確認する。余計なリクエストを飛ばしていないことの保証になる。
 */
describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('検索_入力された条件だけがクエリパラメータに載る', () => {
    service.search({ keyword: '山田', page: 1, size: 20 }).subscribe();

    // URLだけで1件に絞り込めるようマッチャ関数で検証する
    const request = httpMock.expectOne(
      (req) => req.method === 'GET' && req.url === '/api/applications',
    );

    expect(request.request.params.get('keyword')).toBe('山田');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    // 未指定の条件は送らない（空文字での絞り込みと解釈されるのを避けるため）
    expect(request.request.params.has('status')).toBeFalse();
    expect(request.request.params.has('categoryCode')).toBeFalse();

    request.flush(emptyPageResponse());
  });

  it('検索_空文字のキーワードはクエリパラメータに載らない', () => {
    service.search({ keyword: '', status: '' }).subscribe();

    const request = httpMock.expectOne(
      (req) => req.method === 'GET' && req.url === '/api/applications',
    );

    expect(request.request.params.has('keyword')).toBeFalse();
    expect(request.request.params.has('status')).toBeFalse();

    request.flush(emptyPageResponse());
  });

  it('1件取得_IDがURLに含まれる', () => {
    let received: Application | undefined;
    service.findById(42).subscribe((result) => (received = result));

    const request = httpMock.expectOne('/api/applications/42');
    expect(request.request.method).toBe('GET');

    const stub = sampleApplication();
    request.flush(stub);

    expect(received).toEqual(stub);
  });

  it('登録_POSTでボディが送られる', () => {
    const payload = {
      applicantName: '山田 太郎',
      applicantEmail: 'yamada.taro@example.jp',
      categoryCode: 'BLD',
      appliedOn: '2026-09-01',
      note: null,
    };

    service.create(payload).subscribe();

    const request = httpMock.expectOne('/api/applications');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(sampleApplication());
  });

  it('ステータス変更_PATCHでstatusとversionが送られる', () => {
    service.updateStatus(1, { status: 'REVIEWING', version: 3 }).subscribe();

    const request = httpMock.expectOne('/api/applications/1/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'REVIEWING', version: 3 });

    request.flush(sampleApplication());
  });

  it('削除_DELETEが発行される', () => {
    service.delete(7).subscribe();

    const request = httpMock.expectOne('/api/applications/7');
    expect(request.request.method).toBe('DELETE');

    request.flush(null);
  });

  function sampleApplication(): Application {
    return {
      id: 1,
      applicationNo: 'APP-2026-00001',
      applicantName: '山田 太郎',
      applicantEmail: 'yamada.taro@example.jp',
      categoryCode: 'BLD',
      categoryName: '建築確認申請',
      status: 'RECEIVED',
      statusLabel: '受付済',
      appliedOn: '2026-09-01',
      note: null,
      version: 1,
      createdAt: '2026-09-01T09:15:00',
      updatedAt: '2026-09-01T09:15:00',
    };
  }

  function emptyPageResponse(): PageResponse<Application> {
    return {
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
  }
});
