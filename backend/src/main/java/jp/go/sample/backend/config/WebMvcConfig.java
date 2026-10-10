package jp.go.sample.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC の追加設定。ここでは CORS を設定している。
 *
 * <p><b>CORS とは</b><br>
 * ブラウザは「表示しているページのオリジン（スキーム+ホスト+ポート）と違う所へのリクエスト」を
 * 既定で遮断する。開発中は Angular が {@code http://localhost:4200}、
 * Spring Boot が {@code http://localhost:8080} で動くため別オリジンになり、
 * サーバ側で許可を返さないとブラウザがレスポンスを破棄してしまう。
 *
 * <p><b>開発時の代替手段</b><br>
 * Angular CLI のプロキシ（{@code frontend/proxy.conf.json}）を使うと、
 * {@code localhost:4200/api/*} を裏で {@code localhost:8080} に転送するため
 * ブラウザから見れば同一オリジンになり、CORS設定は不要になる。
 * 本サンプルはプロキシ方式を既定にしつつ、CORSも学習用に設定してある。
 *
 * <p><b>本番環境では</b><br>
 * Angular のビルド成果物を同じサーバ（または同じドメイン配下のリバースプロキシ）から
 * 配信する構成が多く、その場合CORSは不要になる。
 * 許可オリジンは必ず設定ファイルで環境ごとに切り替え、{@code *} の指定は避ける。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 許可するオリジン。application.yml の app.cors.allowed-origins で環境ごとに変える。
     *
     * <p>{@code @NonNull} を付けているのは、この値を Spring の
     * {@code allowedOrigins(String...)}（非nullを要求する）にそのまま渡すため。
     * 付けないと null解析が「nullかもしれない値を非null引数に渡している」と警告する。
     * 設定値が未定義ならそもそも起動時に失敗するので、実質的に非nullが保証される。
     */
    @NonNull private final String[] allowedOrigins;

    /**
     * 引数にも {@code @NonNull} が必要。
     * 付けないと「null許容性が不明な値を非nullフィールドに代入している」と警告される。
     * {@code app.cors.allowed-origins} が未定義なら Spring の起動時に解決に失敗するため、
     * ここに null が渡ることはない。
     */
    public WebMvcConfig(
            @Value("${app.cors.allowed-origins}") @NonNull String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * {@code @NonNull} は親インターフェース {@code WebMvcConfigurer} の契約に合わせたもの。
     * 省略するとIDEの null解析が
     * 「Missing non-null annotation: inherited method from WebMvcConfigurer
     *   specifies this parameter as @NonNull」
     * と警告する。詳細は {@link RequestLoggingFilter} のコメントを参照。
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
