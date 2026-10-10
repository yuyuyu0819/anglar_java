package jp.go.sample.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis の Mapper スキャン設定。
 *
 * <p>{@code @MapperScan} を指定したパッケージ配下のインターフェースを、
 * MyBatis が実行時に実装生成してDIコンテナに登録する。
 *
 * <p><b>Mapper を認識させる2つの方式</b>
 * <ol>
 *   <li>各Mapperインターフェースに {@code @Mapper} を付ける</li>
 *   <li>本クラスのように {@code @MapperScan} でパッケージ単位に一括指定する</li>
 * </ol>
 * 大規模案件では付け忘れを防げる 2 の方式が選ばれやすい。
 *
 * <p><b>なぜ起動クラスではなく専用クラスに置くのか</b><br>
 * {@code @MapperScan} を起動クラス（{@code @SpringBootApplication}）に付けると、
 * {@code @WebMvcTest} のようなWeb層だけを起動するスライステストでも
 * Mapper の Bean 定義が登録されてしまい、DataSource が無いため起動に失敗する。
 * 専用の {@code @Configuration} に分けておけばスライステストの対象から外れるため、
 * コントローラの単体テストを軽量なまま保てる。
 */
@Configuration
@MapperScan("jp.go.sample.backend.domain.repository")
public class MyBatisConfig {}
