package jp.go.sample.backend.web.dto;

import jakarta.validation.constraints.NotNull;
import jp.go.sample.backend.domain.model.ApplicationStatus;
import lombok.Data;

/**
 * ステータス変更リクエスト（審査中にする／承認する／却下する 等）。
 *
 * <p>CRUDの「更新」とは業務的な意味が違うため、エンドポイントを分けている
 * （{@code PATCH /api/applications/{id}/status}）。
 * 大規模案件でも「状態遷移だけは専用API」にする設計はよく見る。
 */
@Data
public class ApplicationStatusUpdateRequest {

    @NotNull(message = "変更後ステータスは必須です")
    private ApplicationStatus status;

    @NotNull(message = "バージョンは必須です")
    private Integer version;
}
