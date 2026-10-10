import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';

import { Application, ApplicationStatusCode } from '../../../core/models/application.model';
import { ApplicationCategory, StatusOption } from '../../../core/models/master.model';
import { PageResponse, emptyPage } from '../../../core/models/page.model';
import { ApplicationService } from '../../../core/services/application.service';
import { MasterService } from '../../../core/services/master.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';

/** 1ページあたりの表示件数 */
const PAGE_SIZE = 10;

/**
 * 申請一覧画面。
 *
 * <h3>検索条件をURLのクエリパラメータに持たせている理由</h3>
 * 「検索 → 詳細 → ブラウザの戻る」で検索条件が消えると実務では非常に使いづらい。
 * 条件をURLに載せておけば、
 *   - ブラウザバックで条件が復元される
 *   - URLを共有すれば同じ検索結果を再現できる
 *   - F5 を押しても条件が消えない
 * という利点がある。官公庁案件では操作性の要件として挙がることが多い。
 *
 * 実装としては「画面はURLを唯一の正としてデータを取得する」構成にしている。
 *   検索ボタン押下 → URLを書き換える（router.navigate）
 *                  → queryParamMap の購読が発火
 *                  → APIを呼んで一覧を更新
 * 画面の状態更新経路が1本にまとまるため、処理を追いやすい。
 */
@Component({
  selector: 'app-application-list',
  // standalone コンポーネントでは、テンプレートで使う部品をここに列挙する。
  // NgModule 方式（古い案件）では、モジュールの declarations/imports に書いていた
  // DatePipe はテンプレートで {{ value | date }} を使うために必要。
  // 旧案件では CommonModule をまとめて import していることが多いが、
  // 必要なものだけを列挙するほうがビルド後のサイズが小さくなる
  imports: [ReactiveFormsModule, RouterLink, DatePipe, Pagination, StatusBadge],
  templateUrl: './application-list.html',
  styleUrl: './application-list.scss',
})
export class ApplicationList implements OnInit {
  private readonly applicationService = inject(ApplicationService);
  private readonly masterService = inject(MasterService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  /**
   * 検索条件フォーム。
   *
   * <h3>Reactive Forms</h3>
   * フォームの定義をTypeScript側に持つ方式。
   * 型が効き、バリデーションやテストが書きやすいため、
   * 業務システムではほぼこちらが選ばれる
   * （テンプレート側に `[(ngModel)]` を書く Template-driven Forms もある）。
   *
   * `NonNullableFormBuilder` を使うと、各コントロールの型が
   * `string | null` ではなく `string` になり、null チェックが不要になる。
   */
  protected readonly searchForm = this.formBuilder.group({
    keyword: '',
    status: '' as ApplicationStatusCode | '',
    categoryCode: '',
  });

  /** 一覧データ（ページング情報込み） */
  protected readonly page = signal<PageResponse<Application>>(emptyPage(PAGE_SIZE));

  /** プルダウンの選択肢 */
  protected readonly categories = signal<ApplicationCategory[]>([]);
  protected readonly statuses = signal<StatusOption[]>([]);

  /** 1回でも検索が完了したか（初期表示と「0件」の区別に使う） */
  protected readonly loaded = signal(false);

  ngOnInit(): void {
    this.loadMasters();

    /*
     * クエリパラメータの変化を購読する。
     *
     * takeUntilDestroyed() は「コンポーネントが破棄されたら購読を解除する」演算子。
     * Observable の購読を放置するとコンポーネントが破棄された後も動き続け、
     * メモリリークや「破棄済み画面の更新」エラーの原因になる。
     * 以前は ngOnDestroy で手動 unsubscribe していたが、現在はこの書き方が推奨。
     */
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const keyword = params.get('keyword') ?? '';
      const status = (params.get('status') ?? '') as ApplicationStatusCode | '';
      const categoryCode = params.get('categoryCode') ?? '';
      const pageNumber = Number(params.get('page') ?? 0);

      // 入力欄にURLの条件を反映する。
      // emitEvent: false を付けないと valueChanges が発火し、無限ループになりかねない
      this.searchForm.patchValue({ keyword, status, categoryCode }, { emitEvent: false });

      this.fetch({ keyword, status, categoryCode, page: pageNumber });
    });
  }

  /** 検索ボタン押下。1ページ目に戻してURLを更新する */
  protected onSearch(): void {
    void this.applyQueryParams({ ...this.searchForm.getRawValue(), page: 0 });
  }

  /** 条件クリア */
  protected onClear(): void {
    this.searchForm.reset({ keyword: '', status: '', categoryCode: '' });
    void this.applyQueryParams({ keyword: '', status: '', categoryCode: '', page: 0 });
  }

  /** ページ送り（子コンポーネントからの通知を受けて呼ばれる） */
  protected onPageChange(pageNumber: number): void {
    void this.applyQueryParams({ ...this.searchForm.getRawValue(), page: pageNumber });
  }

  /** 削除ボタン押下 */
  protected onDelete(application: Application): void {
    // 学習用に window.confirm を使っている。
    // 実案件では自作のモーダルや Angular Material のダイアログを使うのが一般的
    const confirmed = window.confirm(
      `申請番号 ${application.applicationNo}（${application.applicantName}）を削除します。よろしいですか？`,
    );
    if (!confirmed) {
      return;
    }

    this.applicationService
      .delete(application.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success(`申請番号 ${application.applicationNo} を削除しました。`);
          // 削除後は一覧を取り直す（最後の1件を削除した場合のページずれも考慮）
          this.reload();
        },
        // エラー時のトーストは errorInterceptor が共通で出すため、ここでは何もしない。
        // ただし error コールバックを省略すると未処理エラーとして扱われるため空実装を置く
        error: () => undefined,
      });
  }

  /** 一覧を現在の条件で取り直す */
  private reload(): void {
    const { keyword, status, categoryCode } = this.searchForm.getRawValue();
    const current = this.page();
    // 表示中ページが空になるなら1つ前のページへ
    const pageNumber =
      current.content.length === 1 && current.page > 0 ? current.page - 1 : current.page;
    this.fetch({ keyword, status, categoryCode, page: pageNumber });
  }

  private fetch(params: {
    keyword: string;
    status: ApplicationStatusCode | '';
    categoryCode: string;
    page: number;
  }): void {
    this.applicationService
      .search({ ...params, size: PAGE_SIZE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.loaded.set(true);
        },
        error: () => {
          // 取得に失敗したら空表示に戻す（古い結果を残すと誤解を招く）
          this.page.set(emptyPage(PAGE_SIZE));
          this.loaded.set(true);
        },
      });
  }

  private loadMasters(): void {
    this.masterService
      .getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.categories.set(list), error: () => undefined });

    this.masterService
      .getStatuses()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.statuses.set(list), error: () => undefined });
  }

  /**
   * URLのクエリパラメータを書き換える。
   * 空の条件はURLに載せない（`?keyword=&status=` のような無意味な文字列を避ける）。
   *
   * <h3>URLが変わらなかった場合の考慮</h3>
   * 同じ条件で検索ボタンを押すとURLに変化が無く、
   * ルーターはナビゲーションをスキップする（= queryParamMap が発火しない）。
   * その場合は再取得されないので、ここで明示的にAPIを呼ぶ。
   * `router.navigate()` は成功時に true、スキップ時に falsy な値を返すので判定できる。
   */
  private async applyQueryParams(params: {
    keyword: string;
    status: ApplicationStatusCode | '';
    categoryCode: string;
    page: number;
  }): Promise<void> {
    const queryParams: Params = {};
    if (params.keyword) {
      queryParams['keyword'] = params.keyword;
    }
    if (params.status) {
      queryParams['status'] = params.status;
    }
    if (params.categoryCode) {
      queryParams['categoryCode'] = params.categoryCode;
    }
    if (params.page > 0) {
      queryParams['page'] = params.page;
    }

    const navigated = await this.router.navigate([], { relativeTo: this.route, queryParams });

    if (!navigated) {
      this.fetch(params);
    }
  }
}
