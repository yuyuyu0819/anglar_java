package jp.go.sample.backend.common.exception;

/**
 * 楽観ロックの競合（他ユーザーが先に更新していた）を表す例外。HTTP 409 に変換される。
 *
 * <p>複数の職員が同じ申請を同時に開いて更新するケースを想定したもので、
 * 官公庁案件では必ず要件に挙がる論点。
 */
public class OptimisticLockConflictException extends BusinessException {

    public OptimisticLockConflictException() {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    public OptimisticLockConflictException(String message) {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, message);
    }
}
