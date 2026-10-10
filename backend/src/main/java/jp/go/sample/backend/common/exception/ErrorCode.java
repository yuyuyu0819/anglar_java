package jp.go.sample.backend.common.exception;

/**
 * エラーコード定義。
 *
 * <p>官公庁案件では「エラーコード一覧」が設計書として存在し、
 * 画面表示・ログ・問い合わせ対応がこのコードを軸に紐づくことが多い。
 * HTTPステータスだけでは粒度が足りないので、業務エラーコードを別に持つ。
 */
public enum ErrorCode {

    /** 入力値が不正 */
    VALIDATION_ERROR("E400-001", "入力内容に誤りがあります。"),
    /** リクエストの形式自体が不正（JSONが壊れている等） */
    MALFORMED_REQUEST("E400-002", "リクエストの形式が正しくありません。"),
    /** 対象データが存在しない */
    RESOURCE_NOT_FOUND("E404-001", "対象のデータが見つかりません。"),
    /** 業務ルール違反（ステータス遷移不可など） */
    BUSINESS_RULE_VIOLATION("E409-001", "業務ルールに反する操作です。"),
    /** 他ユーザーによる更新と競合した */
    OPTIMISTIC_LOCK_CONFLICT("E409-002", "他のユーザーが更新したため処理できませんでした。画面を再表示してやり直してください。"),
    /** 想定外のエラー */
    INTERNAL_ERROR("E500-001", "システムエラーが発生しました。時間をおいて再度お試しください。");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
