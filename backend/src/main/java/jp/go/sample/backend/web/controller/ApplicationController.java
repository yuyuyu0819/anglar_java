package jp.go.sample.backend.web.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;
import jp.go.sample.backend.common.dto.PageResponse;
import jp.go.sample.backend.service.ApplicationService;
import jp.go.sample.backend.web.dto.ApplicationCreateRequest;
import jp.go.sample.backend.web.dto.ApplicationResponse;
import jp.go.sample.backend.web.dto.ApplicationSearchCriteria;
import jp.go.sample.backend.web.dto.ApplicationStatusUpdateRequest;
import jp.go.sample.backend.web.dto.ApplicationUpdateRequest;
import lombok.RequiredArgsConstructor;

/**
 * 申請のREST APIコントローラ。
 *
 * <p><b>コントローラは薄く保つ</b>のが基本方針。
 * ここでやるのは「HTTPとJavaオブジェクトの相互変換」と「サービス呼び出し」だけで、
 * if文による業務判断は書かない。
 *
 * <p>{@code @RestController} は {@code @Controller} + {@code @ResponseBody} 相当。
 * 戻り値のオブジェクトが Jackson によって自動でJSONに変換される。
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 申請一覧の検索。
     *
     * <p>{@code GET /api/applications?keyword=山田&status=REVIEWING&page=0&size=10}
     *
     * <p>{@code @ModelAttribute} はクエリパラメータをオブジェクトに束縛する。
     * （{@code @RequestBody} はリクエストボディのJSONを束縛する。GETにボディは付けないのでこちらを使う）
     */
    @GetMapping
    public PageResponse<ApplicationResponse> search(
            @Valid @ModelAttribute ApplicationSearchCriteria criteria) {
        return applicationService.search(criteria);
    }

    /**
     * 申請1件の取得。
     *
     * <p>{@code @PathVariable} はURLの {@code {id}} 部分を受け取る。
     * 存在しない場合はサービス層が例外を投げ、GlobalExceptionHandler が404に変換する。
     */
    @GetMapping("/{id}")
    public ApplicationResponse findById(@PathVariable Long id) {
        return applicationService.findById(id);
    }

    /**
     * 申請の新規登録。
     *
     * <p>RESTの作法として、作成成功時は <b>201 Created</b> と
     * {@code Location} ヘッダ（作られたリソースのURL）を返す。
     * 戻り値を {@code ResponseEntity} にするとステータスやヘッダを自分で制御できる。
     *
     * <p>URLの組み立てに {@code UriComponentsBuilder} を使っている。
     * {@code URI.create("/api/applications/" + id)} と文字列連結しても動くが、
     * <ul>
     *   <li>{@code {id}} のテンプレート展開時にURLエンコードが自動で行われる</li>
     *   <li>クエリパラメータの追加など拡張がしやすい</li>
     * </ul>
     * ため、Spring MVC ではこちらが定石。
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @Valid @RequestBody ApplicationCreateRequest request) {
        ApplicationResponse created = applicationService.create(request);

        URI location =
                UriComponentsBuilder.fromPath("/api/applications/{id}")
                        .buildAndExpand(created.id())
                        .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /** 申請内容の更新（全項目を置き換えるので PUT） */
    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable Long id, @Valid @RequestBody ApplicationUpdateRequest request) {
        return applicationService.update(id, request);
    }

    /**
     * ステータスのみの変更。
     *
     * <p>一部の項目だけを変える操作なので PATCH を使っている。
     * PUT（全置換）との使い分けは実案件でも議論になるポイント。
     */
    @PatchMapping("/{id}/status")
    public ApplicationResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return applicationService.updateStatus(id, request);
    }

    /** 申請の削除。返すものが無いので 204 No Content */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
