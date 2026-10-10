package jp.go.sample.backend.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jp.go.sample.backend.domain.model.Application;

/**
 * 申請のレスポンスDTO。
 *
 * <p>Java 16 以降の {@code record} を使っている。
 * イミュータブルで getter / equals / hashCode / toString が自動生成されるため、
 * 「外に出すだけ」のDTOには最適。Jackson もそのままJSON化できる。
 *
 * <p>エンティティをそのまま返さないのは、
 * DBの内部構造（カラム追加など）が外部APIの契約に直結してしまうのを避けるため。
 *
 * @param statusLabel ステータスの日本語名。画面側で変換テーブルを持たなくて済むよう
 *                    サーバ側で付与している（案件の方針次第でフロント側に持たせることもある）
 */
public record ApplicationResponse(
        Long id,
        String applicationNo,
        String applicantName,
        String applicantEmail,
        String categoryCode,
        String categoryName,
        String status,
        String statusLabel,
        LocalDate appliedOn,
        String note,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** エンティティ → レスポンスDTO への変換 */
    public static ApplicationResponse from(Application entity) {
        return new ApplicationResponse(
                entity.getId(),
                entity.getApplicationNo(),
                entity.getApplicantName(),
                entity.getApplicantEmail(),
                entity.getCategoryCode(),
                entity.getCategoryName(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getStatus() == null ? null : entity.getStatus().getLabel(),
                entity.getAppliedOn(),
                entity.getNote(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
