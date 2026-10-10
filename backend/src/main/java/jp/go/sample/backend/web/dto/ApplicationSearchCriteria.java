package jp.go.sample.backend.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jp.go.sample.backend.domain.model.ApplicationStatus;
import lombok.Data;

/**
 * 申請一覧の検索条件。
 *
 * <p>コントローラで {@code @ModelAttribute} として受けると、
 * クエリ文字列 {@code ?keyword=山田&status=REVIEWING&page=0&size=10} が
 * そのままこのオブジェクトに束縛（バインド）される。
 *
 * <p>MyBatis のXMLからは {@code #{criteria.keyword}} のように参照する。
 */
@Data
public class ApplicationSearchCriteria {

    /** 申請番号・申請者氏名の部分一致キーワード */
    @Size(max = 100, message = "キーワードは100文字以内で入力してください")
    private String keyword;

    /** ステータス絞り込み（未指定時は全件） */
    private ApplicationStatus status;

    /** 申請区分コード絞り込み（未指定時は全件） */
    @Size(max = 10)
    private String categoryCode;

    /** ページ番号（0始まり） */
    @Min(value = 0, message = "ページ番号は0以上で指定してください")
    private int page = 0;

    /** 1ページあたりの件数 */
    @Min(value = 1, message = "ページサイズは1以上で指定してください")
    @Max(value = 100, message = "ページサイズは100以下で指定してください")
    private int size = 10;

    /** ソート対象のカラム論理名（許可リストでホワイトリスト化する） */
    private String sort = "appliedOn";

    /** 昇順/降順 */
    private String direction = "desc";

    /**
     * SQLの OFFSET に渡す値。
     * Lombok の {@code @Data} が生成する getter に加えて手書きした派生プロパティで、
     * MyBatis からは {@code #{criteria.offset}} で参照できる。
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * ORDER BY に埋め込むカラム名。
     *
     * <p><b>重要</b>：ORDER BY のカラム名は MyBatis のプレースホルダ {@code #{}} では
     * 渡せず、文字列結合 {@code ${}} が必要になる。そのままだと SQLインジェクションの
     * 温床になるため、必ずこのような許可リスト方式で値を固定する。
     */
    public String getSortColumn() {
        return switch (sort == null ? "" : sort) {
            case "applicationNo" -> "a.application_no";
            case "applicantName" -> "a.applicant_name";
            case "status" -> "a.status";
            case "categoryCode" -> "a.category_code";
            case "updatedAt" -> "a.updated_at";
            default -> "a.applied_on";
        };
    }

    /** ORDER BY の並び順。許可リストで ASC / DESC のみに固定する。 */
    public String getSortDirection() {
        return "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
    }
}
