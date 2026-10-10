package jp.go.sample.backend.common.dto;

import java.time.LocalDateTime;
import java.util.List;

import jp.go.sample.backend.common.exception.ErrorCode;

/**
 * エラーレスポンスの共通フォーマット。
 *
 * <p>APIのエラー形式を1つに統一しておくと、
 * フロント側はHTTPインターセプター1箇所でエラー処理を書ける。
 * 逆にここがバラバラだと、画面ごとにエラー処理を書く羽目になる。
 *
 * @param timestamp   発生日時
 * @param status      HTTPステータスコード
 * @param code        業務エラーコード（{@link ErrorCode}）
 * @param message     画面に出してよいメッセージ
 * @param path        リクエストパス
 * @param fieldErrors 項目別のバリデーションエラー（無い場合は空リスト）
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    /** 項目単位のエラー明細。画面の各入力欄の下に表示する用途。 */
    public record FieldErrorDetail(String field, String message, Object rejectedValue) {}

    public static ApiErrorResponse of(
            int status, ErrorCode errorCode, String message, String path) {
        return new ApiErrorResponse(
                LocalDateTime.now(), status, errorCode.getCode(), message, path, List.of());
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            ErrorCode errorCode,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(
                LocalDateTime.now(), status, errorCode.getCode(), message, path, fieldErrors);
    }
}
