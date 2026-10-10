import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import {
  Application,
  ApplicationCreatePayload,
  ApplicationUpdatePayload,
} from '../../../core/models/application.model';
import { ApplicationCategory } from '../../../core/models/master.model';
import { ApplicationService } from '../../../core/services/application.service';
import { MasterService } from '../../../core/services/master.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FieldError } from '../../../shared/components/field-error/field-error';
import { toFieldErrorMap } from '../../../shared/utils/server-validation';
import { notFutureDate } from '../../../shared/validators/not-future-date.validator';

/**
 * 申請の登録・編集フォーム。
 *
 * <h3>1コンポーネントで新規と編集の両方を担う</h3>
 * 入力項目が同じなら1つにまとめたほうが保守しやすい。
 * ルート定義で `:id` があるかどうかで動作を切り替えている。
 *   /applications/new        … 新規（id なし）
 *   /applications/123/edit   … 編集（id あり）
 *
 * <h3>バリデーションの二段構え</h3>
 * <ol>
 *   <li>クライアント側（Validators）… 入力中に即座にフィードバックする</li>
 *   <li>サーバ側（Bean Validation）… 最終的な正しさを保証する。
 *       400 のレスポンスに含まれる項目別メッセージを
 *       `serverFieldErrors` に取り込み、該当の入力欄の下に表示する</li>
 * </ol>
 * 画面側の検証は開発ツールで回避できるため、サーバ側の検証は必須。
 */
@Component({
  selector: 'app-application-form',
  imports: [ReactiveFormsModule, RouterLink, FieldError],
  templateUrl: './application-form.html',
  styleUrl: './application-form.scss',
})
export class ApplicationForm implements OnInit {
  private readonly applicationService = inject(ApplicationService);
  private readonly masterService = inject(MasterService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  /**
   * ルートの :id。新規登録のルートには :id が無いため未設定（undefined）になる。
   * `input.required` ではなく `input` を使うのがポイント。
   */
  readonly id = input<string>();

  /** 編集中の申請（新規の場合は null） */
  protected readonly current = signal<Application | null>(null);

  protected readonly categories = signal<ApplicationCategory[]>([]);

  /** 送信ボタンが押されたか。未入力エラーの表示タイミング判定に使う */
  protected readonly submitted = signal(false);

  /** 送信処理中か（二重送信の防止） */
  protected readonly saving = signal(false);

  /** サーバから返ってきた項目別エラー（項目名 → メッセージ） */
  protected readonly serverFieldErrors = signal<Record<string, string>>({});

  /** 編集モードかどうか */
  protected readonly isEditMode = computed(() => this.id() !== undefined);

  protected readonly pageTitle = computed(() => (this.isEditMode() ? '申請編集' : '申請登録'));

  /**
   * 入力フォームの定義。
   *
   * 第1引数が初期値、第2引数がバリデータの配列。
   * ここで宣言した制約は、サーバ側 DTO のアノテーションと必ず対応させる。
   * （applicantName: @NotBlank @Size(max=100) ↔ required + maxLength(100)）
   */
  protected readonly form = this.formBuilder.group({
    applicantName: ['', [Validators.required, Validators.maxLength(100)]],
    applicantEmail: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    categoryCode: ['', [Validators.required]],
    appliedOn: ['', [Validators.required, notFutureDate()]],
    note: ['', [Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    this.masterService
      .getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.categories.set(list), error: () => undefined });

    const id = this.id();
    if (id !== undefined) {
      this.loadForEdit(Number(id));
    } else {
      // 新規登録時は申請日に当日を初期表示する（入力の手間を減らす配慮）
      this.form.patchValue({ appliedOn: this.toDateInputValue(new Date()) });
    }
  }

  /** 保存ボタン押下 */
  protected onSubmit(): void {
    this.submitted.set(true);
    this.serverFieldErrors.set({});

    if (this.form.invalid) {
      // markAllAsTouched() を呼ぶと全項目が「触られた」状態になり、
      // 未入力のままでもエラーメッセージが表示される
      this.form.markAllAsTouched();
      this.notificationService.error('入力内容に誤りがあります。各項目のメッセージを確認してください。');
      return;
    }

    const values = this.form.getRawValue();
    const payload: ApplicationCreatePayload = {
      applicantName: values.applicantName.trim(),
      applicantEmail: values.applicantEmail.trim(),
      categoryCode: values.categoryCode,
      appliedOn: values.appliedOn,
      // 空文字ではなく null を送る。DBのNULLと空文字を混在させないため
      note: values.note.trim() === '' ? null : values.note.trim(),
    };

    this.saving.set(true);

    const existing = this.current();
    if (existing) {
      const updatePayload: ApplicationUpdatePayload = { ...payload, version: existing.version };
      this.save(this.applicationService.update(existing.id, updatePayload), '更新');
    } else {
      this.save(this.applicationService.create(payload), '登録');
    }
  }

  /**
   * 保存処理の共通部分。
   * 登録と更新で後処理が同じなので1つにまとめている。
   */
  private save(request: Observable<Application>, actionLabel: '登録' | '更新'): void {
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notificationService.success(
          `申請番号 ${saved.applicationNo} を${actionLabel}しました。`,
        );
        // 保存後は詳細画面へ遷移する
        void this.router.navigate(['/applications', saved.id]);
      },
      error: (error: unknown) => {
        this.saving.set(false);

        // 400 の場合は項目別エラーを取り込んで各入力欄の下に表示する。
        // （共通トーストは errorInterceptor 側で意図的に出していない）
        const fieldErrors = toFieldErrorMap(error);
        if (Object.keys(fieldErrors).length > 0) {
          this.serverFieldErrors.set(fieldErrors);
          this.notificationService.error('入力内容にエラーがあります。各項目のメッセージを確認してください。');
          return;
        }

        // 409（楽観ロック競合・業務エラー）等は errorInterceptor がトーストを出すため、
        // ここでは最新データの読み直しだけを行う
        const existing = this.current();
        if (existing) {
          this.loadForEdit(existing.id);
        }
      },
    });
  }

  /** この項目にサーバ側エラーがあればメッセージを返す */
  protected serverErrorOf(field: string): string | null {
    return this.serverFieldErrors()[field] ?? null;
  }

  private loadForEdit(id: number): void {
    this.applicationService
      .findById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (application) => {
          this.current.set(application);
          // サーバから取得した値をフォームに流し込む。
          // setValue は全項目を必須で要求するが、patchValue は一部だけでよい
          this.form.patchValue({
            applicantName: application.applicantName,
            applicantEmail: application.applicantEmail,
            categoryCode: application.categoryCode,
            appliedOn: application.appliedOn,
            note: application.note ?? '',
          });
        },
        error: () => {
          this.current.set(null);
          void this.router.navigate(['/applications']);
        },
      });
  }

  /** <input type="date"> が要求する 'YYYY-MM-DD' 形式に変換する */
  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
