package jp.go.sample.backend.service;

import jp.go.sample.backend.common.dto.PageResponse;
import jp.go.sample.backend.web.dto.ApplicationCreateRequest;
import jp.go.sample.backend.web.dto.ApplicationResponse;
import jp.go.sample.backend.web.dto.ApplicationSearchCriteria;
import jp.go.sample.backend.web.dto.ApplicationStatusUpdateRequest;
import jp.go.sample.backend.web.dto.ApplicationUpdateRequest;

/**
 * 申請業務のサービス（インターフェース）。
 *
 * <p><b>なぜインターフェースを切るのか</b><br>
 * Spring Boot 単体なら実装クラスだけでも動く。それでも大規模案件で
 * インターフェースを置くのは、主に以下の理由。
 * <ul>
 *   <li>コントローラのテストで実装をモックに差し替えやすい</li>
 *   <li>「外部に公開するメソッド一覧」が1ファイルで俯瞰でき、設計書と対応づけやすい</li>
 *   <li>複数ベンダー体制で、インターフェースを先に固めて並行開発できる</li>
 * </ul>
 * 逆に小規模なら実装クラスのみで十分、という判断もある（案件の規約に従う）。
 */
public interface ApplicationService {

    /** 条件検索（ページング付き） */
    PageResponse<ApplicationResponse> search(ApplicationSearchCriteria criteria);

    /** 1件取得。存在しなければ {@code ResourceNotFoundException} */
    ApplicationResponse findById(Long id);

    /** 新規登録。申請番号はサーバ側で採番する */
    ApplicationResponse create(ApplicationCreateRequest request);

    /** 内容更新（楽観ロックあり）。終了状態の申請は更新できない */
    ApplicationResponse update(Long id, ApplicationUpdateRequest request);

    /** ステータス変更（状態遷移ルールを検証する） */
    ApplicationResponse updateStatus(Long id, ApplicationStatusUpdateRequest request);

    /** 削除 */
    void delete(Long id);
}
