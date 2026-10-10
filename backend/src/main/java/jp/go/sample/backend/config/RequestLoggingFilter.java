package jp.go.sample.backend.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * アクセスログ出力フィルタ。
 *
 * <p>官公庁案件では「いつ・誰が・どのAPIを叩いて・結果どうだったか」を
 * 追跡できることが監査要件として求められる。その最小形。
 *
 * <p><b>MDC（Mapped Diagnostic Context）</b><br>
 * リクエストごとに一意のIDを MDC に入れておくと、
 * そのリクエスト処理中に出力される全ログ行に同じIDが付く。
 * 障害調査時に「この1リクエストで何が起きたか」をログから抜き出せるようになる。
 * ログの出力書式は {@code application.yml} の {@code logging.pattern.level} を参照。
 *
 * <p>{@code OncePerRequestFilter} を継承すると、
 * フォワード等で同一リクエストが複数回通っても1回しか実行されない。
 *
 * <p><b>{@code @NonNull} を付けている理由</b><br>
 * Spring のパッケージには {@code @NonNullApi}（そのパッケージの引数・戻り値は
 * 既定で非null）が宣言されている。つまり {@code OncePerRequestFilter} の
 * {@code doFilterInternal} の引数は「nullが渡ってくることはない」という契約になっている。
 *
 * <p>オーバーライド側でその指定を省略すると、null解析を有効にしたIDE
 * （Eclipse / VS Code の Java拡張）が
 * <em>Missing non-null annotation: inherited method from OncePerRequestFilter
 * specifies this parameter as @NonNull</em> という警告を出す。
 * 「親の契約を引き継ぐのか、緩めるのかを明示せよ」という指摘で、
 * 付けておくと「このメソッド内で引数のnullチェックは不要」が型として保証される。
 *
 * <p>※ Spring Framework 7（Spring Boot 4）以降は独自アノテーションをやめ、
 * 業界標準の JSpecify（{@code org.jspecify.annotations.NonNull} と
 * パッケージ単位の {@code @NullMarked}）へ移行している。
 * 将来バージョンアップする際は import を差し替えることになる。
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID_KEY, requestId);

        long startedAt = System.currentTimeMillis();
        try {
            // ここで後続の処理（他のフィルタ → コントローラ）へ処理を渡す
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info(
                    "{} {}{} -> {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString() == null ? "" : "?" + request.getQueryString(),
                    response.getStatus(),
                    elapsed);
            // finally で必ず remove する。スレッドは再利用されるため、
            // 消し忘れると別リクエストのログに前のIDが混ざる
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    /** 静的リソースやヘルスチェックはログに出さない */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/h2-console");
    }
}
