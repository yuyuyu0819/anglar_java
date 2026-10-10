import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * 「未来日を許可しない」カスタムバリデータ。
 *
 * <h3>バリデータの作り方</h3>
 * `ValidatorFn` は「コントロールを受け取り、
 * 問題なければ `null`、エラーなら `{ キー: 詳細 }` を返す関数」。
 * そのキー（ここでは `futureDate`）を見てメッセージを出し分ける。
 *
 * Angular 標準の Validators（required / maxLength / email / pattern など）で
 * 足りない業務ルールは、この形で自作する。
 *
 * <h3>注意：サーバ側の検証を省略してはいけない</h3>
 * 画面のバリデーションは「利用者の入力を助ける」ためのもので、
 * ブラウザの開発ツールやAPIの直叩きで簡単に回避できる。
 * 同じルールを必ずサーバ側にも実装する
 * （本サンプルでは `@PastOrPresent` が対応する）。
 */
export function notFutureDate(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    // 未入力は required の責務なので、ここでは何も言わない。
    // 「1つのバリデータは1つのことだけを検証する」のが原則
    if (!value) {
      return null;
    }

    const inputDate = new Date(value);
    if (Number.isNaN(inputDate.getTime())) {
      return null; // 日付として解釈できない場合は型エラー側に任せる
    }

    const today = new Date();
    today.setHours(23, 59, 59, 999);

    return inputDate > today ? { futureDate: true } : null;
  };
}
