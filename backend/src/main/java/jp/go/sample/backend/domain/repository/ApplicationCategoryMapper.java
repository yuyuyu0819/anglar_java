package jp.go.sample.backend.domain.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import jp.go.sample.backend.domain.model.ApplicationCategory;

/**
 * 申請区分マスタの Mapper。
 *
 * <p>こちらは <b>アノテーションにSQLを直接書く方式</b>。
 * {@link ApplicationMapper} のXML方式と見比べて使い分けを掴むとよい。
 *
 * <ul>
 *   <li>アノテーション方式 … 単純なSQLで手軽。Javaから離れずに読める</li>
 *   <li>XML方式 … 条件分岐（動的SQL）が必要、SQLが長い、DBA/設計書とSQLを突き合わせたい場合に有利。
 *       大規模案件ではこちらが標準になりやすい</li>
 * </ul>
 */
public interface ApplicationCategoryMapper {

    @Select("""
            SELECT code, name, display_order
              FROM application_category
             ORDER BY display_order
            """)
    List<ApplicationCategory> findAll();

    @Select("""
            SELECT code, name, display_order
              FROM application_category
             WHERE code = #{code}
            """)
    Optional<ApplicationCategory> findByCode(@Param("code") String code);
}
