import { Component, computed, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/**
 * 入力欄の下に表示するエラーメッセージ。
 *
 * <h3>クライアント側とサーバ側、2種類のエラーを扱う</h3>
 * <ul>
 *   <li><b>control の errors</b> … Angular の Validators による検証結果。
 *       即座に反応させたいのでこちらを優先して表示する</li>
 *   <li><b>serverMessage</b> … サーバの Bean Validation が返した項目別メッセージ。
 *       クライアント側の検証を通り抜けた項目（業務的な整合性など）を表示する</li>
 * </ul>
 *
 * <h3>touched を見る理由</h3>
 * 画面を開いた直後から「必須です」が全項目に出ていると使いづらい。
 * 一度触った（touched）か、送信ボタンを押した（submitted）ときだけ出すのが定石。
 */
@Component({
  selector: 'app-field-error',
  template: `
    @if (message(); as text) {
      <p class="field-error" role="alert">{{ text }}</p>
    }
  `,
  styles: `
    .field-error {
      margin: 0.25rem 0 0;
      color: #a52121;
      font-size: 0.8125rem;
      line-height: 1.5;
    }
  `,
})
export class FieldError {
  /** 対象のフォームコントロール */
  readonly control = input.required<AbstractControl | null>();

  /** 項目の表示名（「◯◯は必須です」のメッセージ生成に使う） */
  readonly label = input<string>('この項目');

  /** 送信ボタンが押されたか。未入力エラーを出すタイミングの判定に使う */
  readonly submitted = input<boolean>(false);

  /** サーバから返ってきた、この項目に対するエラーメッセージ */
  readonly serverMessage = input<string | null>(null);

  protected readonly message = computed<string | null>(() => {
    const control = this.control();

    if (control && (control.touched || this.submitted()) && control.errors) {
      return this.toMessage(control.errors);
    }

    // クライアント側のエラーが無ければサーバ側のメッセージを出す
    return this.serverMessage();
  });

  /**
   * Validators が返すエラーオブジェクトを日本語メッセージに変換する。
   *
   * errors の中身は検証の種類ごとに決まっている。例:
   *   required   → { required: true }
   *   maxlength  → { maxlength: { requiredLength: 100, actualLength: 120 } }
   *
   * 実案件ではこの変換テーブルを共通化し、
   * メッセージの表現をシステム全体で統一するのが定石。
   */
  private toMessage(errors: Record<string, unknown>): string {
    const label = this.label();

    if (errors['required']) {
      return `${label}は必須です`;
    }
    if (errors['email']) {
      return 'メールアドレスの形式が正しくありません';
    }
    if (errors['maxlength']) {
      const detail = errors['maxlength'] as { requiredLength: number };
      return `${label}は${detail.requiredLength}文字以内で入力してください`;
    }
    if (errors['futureDate']) {
      return `${label}に未来日は指定できません`;
    }
    return `${label}の入力内容が正しくありません`;
  }
}
