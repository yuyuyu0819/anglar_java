package jp.go.sample.backend.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jp.go.sample.backend.common.dto.PageResponse;
import jp.go.sample.backend.common.exception.ResourceNotFoundException;
import jp.go.sample.backend.service.ApplicationService;
import jp.go.sample.backend.web.dto.ApplicationResponse;

/**
 * コントローラ層の単体テスト。
 *
 * <p><b>{@code @WebMvcTest}</b><br>
 * DIコンテナのうち「Web層だけ」を起動する（DataSourceやサービス実装は読み込まない）。
 * {@code @SpringBootTest} より格段に速いので、HTTPの入出力だけを検証したいときに使う。
 *
 * <p><b>{@code @MockitoBean}</b><br>
 * サービスをモックに差し替える。Spring Boot 3.4 以降の書き方で、
 * それ以前の {@code @MockBean} は非推奨になった（古い案件のコードでは
 * {@code @MockBean} を見ることが多い）。
 *
 * <p><b>{@code MockMvc}</b><br>
 * 実際にサーバを起動せずHTTPリクエストを模擬実行する。
 * ステータスコード・JSONの構造・ヘッダまで検証できる。
 *
 * <p><b>{@code MediaType.APPLICATION_JSON_VALUE} を使っている理由</b><br>
 * {@code MediaType.APPLICATION_JSON}（MediaType型の定数）でも同じ動作だが、
 * null解析を有効にしたIDEでは
 * 「Null type safety: The expression of type 'MediaType' needs unchecked
 *   conversion to conform to '@NonNull MediaType'」という警告が出る。
 * Spring は {@code @NonNullFields} でフィールドの非nullを宣言しているが、
 * IDE側が既定でそれを解釈しないために起きる誤検知。
 * {@code APPLICATION_JSON_VALUE} は {@code String} のコンパイル時定数
 * （実体は "application/json"）なので、常に非nullと判定され警告が出ない。
 */
@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ApplicationService applicationService;

    private ApplicationResponse sampleResponse() {
        return new ApplicationResponse(
                1L,
                "APP-2026-00001",
                "山田 太郎",
                "yamada.taro@example.jp",
                "BLD",
                "建築確認申請",
                "RECEIVED",
                "受付済",
                LocalDate.of(2026, 9, 1),
                "木造2階建て住宅の新築",
                1,
                LocalDateTime.of(2026, 9, 1, 9, 15),
                LocalDateTime.of(2026, 9, 1, 9, 15));
    }

    @Test
    @DisplayName("一覧API_ページング構造のJSONが返る")
    void search_returnsPagedJson() throws Exception {
        given(applicationService.search(any()))
                .willReturn(PageResponse.of(List.of(sampleResponse()), 0, 10, 1));

        mockMvc.perform(get("/api/applications").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].applicationNo").value("APP-2026-00001"))
                .andExpect(jsonPath("$.content[0].statusLabel").value("受付済"))
                // 日付が数値配列ではなくISO文字列で出ること（Jacksonの設定確認）
                .andExpect(jsonPath("$.content[0].appliedOn").value("2026-09-01"));
    }

    @Test
    @DisplayName("一覧API_ページサイズが上限超過なら400とフィールドエラーが返る")
    void search_rejectsTooLargePageSize() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400-001"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    @DisplayName("詳細API_サービスが未検出例外を投げたら404になる")
    void findById_returns404() throws Exception {
        given(applicationService.findById(999L))
                .willThrow(ResourceNotFoundException.of("申請", 999L));

        mockMvc.perform(get("/api/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404-001"))
                .andExpect(jsonPath("$.path").value("/api/applications/999"));
    }

    @Test
    @DisplayName("詳細API_IDが数値でなければ400になる")
    void findById_returns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/applications/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    @DisplayName("登録API_成功すると201とLocationヘッダが返る")
    void create_returns201WithLocation() throws Exception {
        // リクエストボディはテキストブロックで直接書く。
        // ObjectMapper でDTOをJSON化する方法もあるが、
        //   - 「実際にクライアントが送るJSON」がそのまま見えて意図が伝わりやすい
        //   - DTOのフィールド名を変えたときにテストが失敗して気づける
        // ため、コントローラのテストでは生のJSONを書くほうが好まれることが多い。
        String body =
                """
                {
                  "applicantName": "山田 太郎",
                  "applicantEmail": "yamada.taro@example.jp",
                  "categoryCode": "BLD",
                  "appliedOn": "2026-09-01",
                  "note": "木造2階建て住宅の新築"
                }
                """;

        given(applicationService.create(any())).willReturn(sampleResponse());

        mockMvc.perform(
                        post("/api/applications").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/applications/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("登録API_必須項目が未入力なら400と項目別エラーが返る")
    void create_returns400WhenValidationFails() throws Exception {
        // applicantName / applicantEmail / categoryCode / appliedOn を全て未設定
        String body = "{}";

        mockMvc.perform(
                        post("/api/applications").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("E400-001"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    @DisplayName("登録API_メールアドレスの形式が不正なら400になる")
    void create_returns400WhenEmailIsInvalid() throws Exception {
        String body =
                """
                {
                  "applicantName": "山田 太郎",
                  "applicantEmail": "not-an-email",
                  "categoryCode": "BLD",
                  "appliedOn": "2026-09-01"
                }
                """;

        mockMvc.perform(
                        post("/api/applications").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("applicantEmail"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("メールアドレスの形式が正しくありません"));
    }

    @Test
    @DisplayName("登録API_JSONが壊れていれば400になる")
    void create_returns400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(
                        post("/api/applications")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content("{ broken json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400-002"));
    }

    @Test
    @DisplayName("削除API_成功すると204が返る")
    void delete_returns204() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/applications/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("削除API_サービスが未検出例外を投げたら404になる")
    void delete_returns404() throws Exception {
        willThrow(ResourceNotFoundException.of("申請", 999L))
                .given(applicationService)
                .delete(999L);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/applications/999"))
                .andExpect(status().isNotFound());
    }
}
