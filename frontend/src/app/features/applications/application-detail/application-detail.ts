import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { Application, ApplicationStatusCode } from '../../../core/models/application.model';
import { StatusOption } from '../../../core/models/master.model';
import { ApplicationService } from '../../../core/services/application.service';
import { MasterService } from '../../../core/services/master.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';

/**
 * 申請詳細画面。ステータス変更もここで行う。
 *
 * <h3>ルートパラメータの受け取り方</h3>
 * `app.config.ts` で `provideRouter(routes, withComponentInputBinding())` を指定すると、
 * ルート定義の `:id` が同名の input に自動でバインドされる。
 * ActivatedRoute を注入して `snapshot.paramMap.get('id')` と書く従来の方法より簡潔。
 * パスパラメータは常に文字列なので、数値として使う場合は変換が必要。
 *
 * 既存の大規模案件では ActivatedRoute 方式がほとんどなので、
 * 両方を読めるようにしておくとよい。
 */
@Component({
  selector: 'app-application-detail',
  imports: [RouterLink, DatePipe, StatusBadge],
  templateUrl: './application-detail.html',
  styleUrl: './application-detail.scss',
})
export class ApplicationDetail implements OnInit {
  private readonly applicationService = inject(ApplicationService);
  private readonly masterService = inject(MasterService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** ルートの :id が入る（文字列） */
  readonly id = input.required<string>();

  protected readonly application = signal<Application | null>(null);
  protected readonly statusOptions = signal<StatusOption[]>([]);
  /** 取得を試みたが見つからなかった場合に true */
  protected readonly notFound = signal(false);

  /**
   * 現在のステータスから遷移できる選択肢。
   *
   * 遷移ルール（受付済 → 審査中、など）はサーバのenumが持っており、
   * `/api/master/statuses` の `allowedNextStatuses` として受け取っている。
   * 画面側にルールを再実装しないので、ルール変更時の修正が1か所で済む。
   */
  protected readonly nextStatusOptions = computed<StatusOption[]>(() => {
    const current = this.application();
    const options = this.statusOptions();
    if (!current) {
      return [];
    }

    const currentOption = options.find((option) => option.code === current.status);
    if (!currentOption) {
      return [];
    }

    return currentOption.allowedNextStatuses
      .map((code) => options.find((option) => option.code === code))
      // 型ガードで undefined を除去する。filter(Boolean) だと型が絞り込まれない
      .filter((option): option is StatusOption => option !== undefined);
  });

  ngOnInit(): void {
    this.masterService
      .getStatuses()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.statusOptions.set(list), error: () => undefined });

    this.fetch();
  }

  /** ステータス変更ボタン押下 */
  protected onChangeStatus(next: StatusOption): void {
    const current = this.application();
    if (!current) {
      return;
    }

    const confirmed = window.confirm(
      `ステータスを「${current.statusLabel}」から「${next.label}」へ変更します。よろしいですか？`,
    );
    if (!confirmed) {
      return;
    }

    this.applicationService
      // version をそのまま送り返すことで、画面表示後に他ユーザーが
      // 更新していた場合は 409 が返る（楽観ロック）
      .updateStatus(current.id, { status: next.code, version: current.version })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.application.set(updated);
          this.notificationService.success(`ステータスを「${updated.statusLabel}」に変更しました。`);
        },
        error: () => {
          // 競合時はサーバの最新状態を読み直す。
          // 古い version を持ったまま再操作させると延々と失敗するため
          this.fetch();
        },
      });
  }

  /** 削除ボタン押下 */
  protected onDelete(): void {
    const current = this.application();
    if (!current) {
      return;
    }

    const confirmed = window.confirm(
      `申請番号 ${current.applicationNo}（${current.applicantName}）を削除します。よろしいですか？`,
    );
    if (!confirmed) {
      return;
    }

    this.applicationService
      .delete(current.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notificationService.success(`申請番号 ${current.applicationNo} を削除しました。`);
          void this.router.navigate(['/applications']);
        },
        error: () => undefined,
      });
  }

  /** ステータスが終了状態（これ以上変更できない）か */
  protected isClosed(status: ApplicationStatusCode): boolean {
    const option = this.statusOptions().find((candidate) => candidate.code === status);
    return option !== undefined && option.allowedNextStatuses.length === 0;
  }

  private fetch(): void {
    this.applicationService
      .findById(Number(this.id()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.application.set(result);
          this.notFound.set(false);
        },
        error: () => {
          this.application.set(null);
          this.notFound.set(true);
        },
      });
  }
}
