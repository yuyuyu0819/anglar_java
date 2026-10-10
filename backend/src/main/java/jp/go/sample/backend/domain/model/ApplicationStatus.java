package jp.go.sample.backend.domain.model;

import java.util.Arrays;
import java.util.Set;

/**
 * 申請ステータス。
 *
 * <p>業務ルール（どのステータスからどのステータスへ遷移できるか）を
 * enum 自身に持たせている。こうしておくとサービス層が薄く保てる。
 */
public enum ApplicationStatus {

    /** 受付済 */
    RECEIVED("受付済"),
    /** 審査中 */
    REVIEWING("審査中"),
    /** 承認 */
    APPROVED("承認"),
    /** 却下 */
    REJECTED("却下"),
    /** 取下げ */
    WITHDRAWN("取下げ");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    /** 画面表示用の日本語名 */
    public String getLabel() {
        return label;
    }

    /** 遷移先として許可されるステータス */
    public Set<ApplicationStatus> allowedNextStatuses() {
        return switch (this) {
            case RECEIVED -> Set.of(REVIEWING, WITHDRAWN);
            case REVIEWING -> Set.of(APPROVED, REJECTED, WITHDRAWN);
            // 承認・却下・取下げは終了状態。これ以上遷移させない
            case APPROVED, REJECTED, WITHDRAWN -> Set.of();
        };
    }

    /** {@code next} への遷移が業務ルール上許されるか */
    public boolean canTransitionTo(ApplicationStatus next) {
        return allowedNextStatuses().contains(next);
    }

    /** 終了状態（これ以上更新させない）か */
    public boolean isClosed() {
        return allowedNextStatuses().isEmpty();
    }

    /**
     * 文字列からの変換。該当なしの場合は {@code null} を返す。
     * （リクエストパラメータが空文字で飛んできても落ちないようにするため）
     */
    public static ApplicationStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
