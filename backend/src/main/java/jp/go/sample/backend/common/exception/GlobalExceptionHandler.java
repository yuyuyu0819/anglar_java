package jp.go.sample.backend.common.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jp.go.sample.backend.common.dto.ApiErrorResponse;
import jp.go.sample.backend.common.dto.ApiErrorResponse.FieldErrorDetail;

/**
 * 全コントローラ共通の例外ハンドラ。
 *
 * <p>{@code @RestControllerAdvice} を付けたクラスに {@code @ExceptionHandler} を並べると、
 * どのコントローラで発生した例外もここに集約される。
 * 各コントローラで try-catch を書かなくて済むため、大規模案件では必須の仕組み。
 *
 * <p><b>設計上のポイント</b>
 * <ul>
 *   <li>業務エラー（4xx）は warn ログ＋利用者向けメッセージをそのまま返す</li>
 *   <li>想定外エラー（5xx）は error ログにスタックトレースを出し、
 *       レスポンスには内部情報を出さない（情報漏えい対策）</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * {@code @Valid @RequestBody} のバリデーション違反。
     * 項目ごとのエラーをまとめて返すことで、画面が各入力欄にメッセージを出せる。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(buildValidationError(ex.getBindingResult().getFieldErrors(), request));
    }

    /** {@code @Valid @ModelAttribute}（クエリパラメータ）のバリデーション違反。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(buildValidationError(ex.getBindingResult().getFieldErrors(), request));
    }

    /** JSONが壊れている・日付の形式が違う・enumに無い値が来た等。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("リクエストの解析に失敗しました: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                ErrorCode.MALFORMED_REQUEST,
                                ErrorCode.MALFORMED_REQUEST.getDefaultMessage(),
                                request.getRequestURI()));
    }

    /** パス変数・クエリパラメータの型が合わない（例: /api/applications/abc）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("パラメータの型が不正です: name={}, value={}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest()
                .body(
                        ApiErrorResponse.withFieldErrors(
                                HttpStatus.BAD_REQUEST.value(),
                                ErrorCode.VALIDATION_ERROR,
                                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                                request.getRequestURI(),
                                List.of(
                                        new FieldErrorDetail(
                                                ex.getName(), "値の形式が正しくありません", ex.getValue()))));
    }

    /** データが存在しない → 404 */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("データ未検出: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.NOT_FOUND.value(),
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    /**
     * 業務ルール違反・楽観ロック競合 → 409 Conflict。
     *
     * <p>{@link ResourceNotFoundException} は {@link BusinessException} のサブクラスだが、
     * Spring はより具体的な型のハンドラを優先するため、上の404ハンドラが先に選ばれる。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("業務エラー: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.CONFLICT.value(),
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    /** 存在しないURL → 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.NOT_FOUND.value(),
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "指定されたAPIは存在しません。",
                                request.getRequestURI()));
    }

    /**
     * 上記以外のすべての例外 → 500。
     *
     * <p>ここでスタックトレースを error ログに出すのが運用上きわめて重要。
     * 逆にレスポンスには例外メッセージを含めない（内部構造の露出を防ぐ）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("想定外のエラーが発生しました: path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                ErrorCode.INTERNAL_ERROR,
                                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                                request.getRequestURI()));
    }

    private ApiErrorResponse buildValidationError(
            List<FieldError> fieldErrors, HttpServletRequest request) {
        List<FieldErrorDetail> details =
                fieldErrors.stream()
                        .map(
                                fe ->
                                        new FieldErrorDetail(
                                                fe.getField(),
                                                fe.getDefaultMessage(),
                                                fe.getRejectedValue()))
                        .toList();
        log.warn("入力検証エラー: path={}, errors={}", request.getRequestURI(), details);
        return ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                request.getRequestURI(),
                details);
    }
}
