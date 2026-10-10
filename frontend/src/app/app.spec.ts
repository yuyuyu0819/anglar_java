import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { routes } from './app.routes';

/**
 * アプリシェルのテスト。
 *
 * <h3>TestBed</h3>
 * テスト用のDIコンテナを組み立てる仕組み。
 * コンポーネントが依存しているもの（ルーター、HttpClient など）を
 * providers に登録しないと生成時にエラーになる。
 *
 * <h3>provideHttpClientTesting</h3>
 * 実際の通信を行わないモック実装に差し替える。
 * テストが外部サーバの起動状況に左右されなくなる。
 */
describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('コンポーネントが生成できる', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('ヘッダーにシステム名が表示される', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.site-header__title')?.textContent).toContain('申請管理システム');
  });

  it('メインメニューに一覧・登録のリンクがある', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const links = Array.from(element.querySelectorAll('.site-nav__link')).map(
      (link) => link.textContent?.trim(),
    );

    expect(links).toContain('申請一覧');
    expect(links).toContain('申請登録');
  });
});
