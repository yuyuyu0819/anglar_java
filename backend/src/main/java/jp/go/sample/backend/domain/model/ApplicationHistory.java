package jp.go.sample.backend.domain.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ApplicationHistory {
    /** 主キー（採番はDBのAUTO_INCREMENT） */
    private Long id;

    /** 申請ID（application.id への外部キー） */
    private Long applicationId;

    /** 変更前ステータス */
    private ApplicationStatus fromStatus;

    /** 変更後ステータス */
    private ApplicationStatus toStatus;

    /** 変更日時 */
    private LocalDateTime changedAt;
}
