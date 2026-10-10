package jp.go.sample.backend.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

        import lombok.Data;

/**
 * 申請エンティティ（DBの application テーブルに対応）。
 *
 * <p>MyBatis がこのクラスにマッピングする。MyBatis は setter 経由で値を詰めるため
 * イミュータブルにはせず、Lombok の {@code @Data}（getter/setter/equals/hashCode/toString）を使う。
 *
 * <p><b>命名の対応</b><br>
 * DBのカラムはスネークケース（{@code application_no}）、
 * Javaのフィールドはキャメルケース（{@code applicationNo}）。
 * {@code application.yml} の {@code map-underscore-to-camel-case: true} で自動変換される。
 */
@Data
public class Application {

    /** 主キー（採番はDBのAUTO_INCREMENT） */
    private Long id;

    /** 申請番号（業務上の識別子。例: APP-2026-00001） */
    private String applicationNo;

    /** 申請者氏名 */
    private String applicantName;

    /** 申請者メールアドレス */
    private String applicantEmail;

    /** 申請区分コード（application_category.code への外部キー） */
    private String categoryCode;

    /**
     * 申請区分名。
     * テーブルには存在せず、SQLのJOINで取得する「表示用」の項目。
     * 実案件でも一覧表示のために結合して持たせるケースが多い。
     */
    private String categoryName;

    /** ステータス */
    private ApplicationStatus status;

    /** 申請日 */
    private LocalDate appliedOn;

    /** 備考 */
    private String note;

    /**
     * 楽観ロック用のバージョン番号。
     * UPDATE時に WHERE version = ? を付け、更新件数0件なら他者更新と判定する。
     * 官公庁案件では同時更新対策として頻出。
     */
    private Integer version;

    /** 登録日時 */
    private LocalDateTime createdAt;

    /** 更新日時 */
    private LocalDateTime updatedAt;
}
