package jp.go.sample.backend.common.exception;

/** 対象データが存在しない場合の例外（HTTP 404 に変換される）。 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 「{リソース名} が見つかりません（ID: xxx）」という定型メッセージを組み立てる。
     *
     * @param resourceName 画面やログに出すリソース名（例: 申請）
     * @param key          識別子
     */
    public static ResourceNotFoundException of(String resourceName, Object key) {
        return new ResourceNotFoundException(
                "%s が見つかりません。(キー: %s)".formatted(resourceName, key));
    }
}
