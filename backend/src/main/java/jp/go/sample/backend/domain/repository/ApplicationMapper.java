package jp.go.sample.backend.domain.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import jp.go.sample.backend.domain.model.Application;
import jp.go.sample.backend.web.dto.ApplicationSearchCriteria;

/**
 * 申請テーブルへのアクセスを担う MyBatis Mapper。
 *
 * <p><b>MyBatis の仕組み</b><br>
 * このインターフェースには実装クラスを書かない。
 * 同名のXML（{@code src/main/resources/mapper/ApplicationMapper.xml}）に書いたSQLと
 * メソッド名を {@code id} で突き合わせ、MyBatis が実行時に実装を生成する。
 *
 * <p>XMLとインターフェースの対応ルール:
 * <ul>
 *   <li>XMLの {@code <mapper namespace="...">} = このインターフェースのFQCN</li>
 *   <li>XMLの {@code <select id="findById">} = このインターフェースの {@code findById} メソッド</li>
 * </ul>
 */
public interface ApplicationMapper {

    /**
     * 検索条件に一致する申請を、ページングして取得する。
     *
     * @param criteria 検索条件（キーワード・ステータス・区分・ページ情報）
     */
    List<Application> search(@Param("criteria") ApplicationSearchCriteria criteria);

    /**
     * 検索条件に一致する申請の総件数を取得する。
     * ページング表示では「全何件中 何件目」を出すために件数取得が別途必要になる。
     */
    long countBySearchCriteria(@Param("criteria") ApplicationSearchCriteria criteria);

    /** IDで1件取得。存在しない場合は {@code Optional.empty()} */
    Optional<Application> findById(@Param("id") Long id);

    /** 申請番号で1件取得（重複チェック用） */
    Optional<Application> findByApplicationNo(@Param("applicationNo") String applicationNo);

    /**
     * 採番用。指定年度で使われている申請番号の最大連番を返す。
     * 該当がない場合は {@code null}。
     */
    Integer findMaxSequenceByYear(@Param("year") int year);

    /**
     * 登録。
     * XML側で {@code useGeneratedKeys="true" keyProperty="id"} を指定しているため、
     * INSERT後に引数オブジェクトの {@code id} に採番値が入る（戻り値ではなく引数が書き換わる）。
     *
     * @return 挿入件数
     */
    int insert(Application application);

    /**
     * 更新（楽観ロックあり）。
     * WHERE句に {@code version} を含めるため、他者が先に更新していると0件になる。
     *
     * @return 更新件数（0 = 競合発生）
     */
    int update(Application application);

    /**
     * 物理削除。
     * 実際の官公庁案件では監査証跡の都合で論理削除（削除フラグ）が多いが、
     * ここでは学習のため物理削除にしている。
     *
     * @return 削除件数
     */
    int deleteById(@Param("id") Long id);
}
