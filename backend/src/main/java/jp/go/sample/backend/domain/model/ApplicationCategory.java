package jp.go.sample.backend.domain.model;

import lombok.Data;

/**
 * 申請区分マスタ（application_category テーブルに対応）。
 *
 * <p>プルダウンの選択肢など、画面から参照されるコードマスタ。
 * 官公庁案件ではこうした「コード定義」がテーブル化されているのが一般的。
 */
@Data
public class ApplicationCategory {

    /** 区分コード */
    private String code;

    /** 区分名 */
    private String name;

    /** 表示順 */
    private Integer displayOrder;
}
