package jp.go.sample.backend.common.exception;

/**
 * 業務ルール違反を表す例外。
 *
 * <p>RuntimeException を継承している（非検査例外）のがポイント。
 * 検査例外にすると全メソッドに {@code throws} が波及して保守しづらくなるため、
 * Springアプリでは非検査例外にして {@code @RestControllerAdvice} で一括処理するのが定石。
 *
 * <p>また、{@code RuntimeException} は Spring の {@code @Transactional} が
 * デフォルトでロールバック対象にするため、業務エラー時にDB更新が残らない。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
