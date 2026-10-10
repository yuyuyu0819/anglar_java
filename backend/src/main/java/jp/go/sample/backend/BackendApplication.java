package jp.go.sample.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションの起動クラス。
 *
 * <p>{@code @SpringBootApplication} は以下3つをまとめたアノテーション。
 * <ul>
 *   <li>{@code @Configuration} … 設定クラスであることの宣言</li>
 *   <li>{@code @EnableAutoConfiguration} … クラスパスの内容から自動設定を行う
 *       （spring-boot-starter-web があれば組込みTomcatとSpring MVCを立ち上げる、など）</li>
 *   <li>{@code @ComponentScan} … このクラスのパッケージ配下を走査し、
 *       {@code @Component / @Service / @RestController} などをDIコンテナに登録する</li>
 * </ul>
 *
 * <p>そのため、<b>このクラスは必ず最上位のパッケージに置く</b>必要がある。
 * 配置を間違えるとコンポーネントが見つからず「Beanが無い」エラーになる。
 *
 * <p>MyBatis の Mapper スキャンは {@code config.MyBatisConfig} に分離している
 * （理由はそのクラスのコメントを参照）。
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
