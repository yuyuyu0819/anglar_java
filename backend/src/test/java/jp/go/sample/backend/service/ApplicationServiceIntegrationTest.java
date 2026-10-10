package jp.go.sample.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jp.go.sample.backend.common.dto.PageResponse;
import jp.go.sample.backend.common.exception.BusinessException;
import jp.go.sample.backend.common.exception.OptimisticLockConflictException;
import jp.go.sample.backend.common.exception.ResourceNotFoundException;
import jp.go.sample.backend.domain.model.ApplicationStatus;
import jp.go.sample.backend.web.dto.ApplicationCreateRequest;
import jp.go.sample.backend.web.dto.ApplicationResponse;
import jp.go.sample.backend.web.dto.ApplicationSearchCriteria;
import jp.go.sample.backend.web.dto.ApplicationStatusUpdateRequest;
import jp.go.sample.backend.web.dto.ApplicationUpdateRequest;

/**
 * サービス層の結合テスト。
 *
 * <p><b>{@code @SpringBootTest}</b><br>
 * アプリケーション全体（DIコンテナ＋DataSource＋MyBatis）を起動してテストする。
 * schema.sql / data.sql がそのまま使われるため、
 * 「SQLが本当に意図どおり動くか」まで確認できる。Mapperのテストはこの形が確実。
 *
 * <p><b>{@code @ActiveProfiles("h2")}</b><br>
 * 接続先を H2 インメモリDBに差し替える。
 * 本番構成（PostgreSQL）のままテストすると、
 * <ul>
 *   <li>DBサーバが起動していないとテストが落ちる（CIが回らない）</li>
 *   <li>テストが開発用DBのデータを壊す</li>
 * </ul>
 * という問題が起きるため、テストは必ず専用のDB設定で動かす。
 *
 * <p>なお本サンプルの schema.sql / data.sql は H2 と PostgreSQL の
 * 両方で動く書き方に揃えてあるが、SQLの方言差が大きい案件では
 * 「テストも本番と同じDB製品を Testcontainers で立てる」という方式が選ばれる。
 *
 * <p><b>{@code @Transactional}</b><br>
 * テストメソッドごとにトランザクションを開始し、終了時に必ずロールバックする。
 * そのためテストがデータを汚さず、実行順に依存しない。
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ApplicationServiceIntegrationTest {

    @Autowired private ApplicationService applicationService;

    // ------------------------------------------------------------------
    // 検索
    // ------------------------------------------------------------------

    @Test
    @DisplayName("検索_初期データ15件に対し1ページ目は10件・総件数15件が返る")
    void search_returnsFirstPage() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.content()).hasSize(10);
        assertThat(page.totalElements()).isEqualTo(15);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    @Test
    @DisplayName("検索_2ページ目は残り5件が返る")
    void search_returnsSecondPage() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setPage(1);

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.content()).hasSize(5);
        assertThat(page.last()).isTrue();
    }

    @Test
    @DisplayName("検索_ステータスで絞り込める")
    void search_filtersByStatus() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setStatus(ApplicationStatus.REVIEWING);

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.totalElements()).isEqualTo(4);
        // extracting(ApplicationResponse::status) でも書けるが、
        // メソッド参照のレシーバのnull許容性が未指定のため
        // null解析を有効にしたIDEが警告を出す。
        // allSatisfy なら各要素に対する検証をそのまま書けて意図も明確。
        assertThat(page.content())
                .isNotEmpty()
                .allSatisfy(response -> assertThat(response.status()).isEqualTo("REVIEWING"));
    }

    @Test
    @DisplayName("検索_キーワードが申請者氏名に部分一致する")
    void search_filtersByKeyword() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setKeyword("山田");

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).applicantName()).isEqualTo("山田 太郎");
    }

    @Test
    @DisplayName("検索_該当なしの場合は空リストと総件数0が返る")
    void search_returnsEmptyWhenNoMatch() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setKeyword("該当しないキーワード");

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DisplayName("検索_JOINした申請区分名が設定される")
    void search_includesCategoryName() {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setCategoryCode("BLD");

        PageResponse<ApplicationResponse> page = applicationService.search(criteria);

        assertThat(page.content())
                .isNotEmpty()
                .allSatisfy(r -> assertThat(r.categoryName()).isEqualTo("建築確認申請"));
    }

    // ------------------------------------------------------------------
    // 取得
    // ------------------------------------------------------------------

    @Test
    @DisplayName("取得_存在しないIDはResourceNotFoundExceptionになる")
    void findById_throwsWhenNotFound() {
        assertThatThrownBy(() -> applicationService.findById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("申請 が見つかりません");
    }

    // ------------------------------------------------------------------
    // 登録
    // ------------------------------------------------------------------

    @Test
    @DisplayName("登録_申請番号が採番されステータスは受付済になる")
    void create_assignsApplicationNoAndInitialStatus() {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setApplicantName("テスト 太郎");
        request.setApplicantEmail("test.taro@example.jp");
        request.setCategoryCode("BLD");
        request.setAppliedOn(LocalDate.now());
        request.setNote("結合テストからの登録");

        ApplicationResponse created = applicationService.create(request);

        assertThat(created.id()).isNotNull();
        // 採番形式 APP-<年4桁>-<連番5桁>
        assertThat(created.applicationNo()).matches("APP-\\d{4}-\\d{5}");
        // 画面からステータスを指定させず、必ず RECEIVED から始まる
        assertThat(created.status()).isEqualTo(ApplicationStatus.RECEIVED.name());
        assertThat(created.statusLabel()).isEqualTo("受付済");
        assertThat(created.version()).isEqualTo(1);
        // JOINして取り直しているので区分名も入っている
        assertThat(created.categoryName()).isEqualTo("建築確認申請");
    }

    @Test
    @DisplayName("登録_存在しない申請区分コードは業務エラーになる")
    void create_rejectsUnknownCategory() {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setApplicantName("テスト 太郎");
        request.setApplicantEmail("test.taro@example.jp");
        request.setCategoryCode("XXX");
        request.setAppliedOn(LocalDate.now());

        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("申請区分コード「XXX」は存在しません");
    }

    // ------------------------------------------------------------------
    // 更新（楽観ロック）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("更新_成功するとバージョンが1つ進む")
    void update_incrementsVersion() {
        ApplicationResponse before = findFirstWithStatus(ApplicationStatus.RECEIVED);

        ApplicationUpdateRequest request = new ApplicationUpdateRequest();
        request.setApplicantName("更新後 氏名");
        request.setApplicantEmail("updated@example.jp");
        request.setCategoryCode("RSD");
        request.setAppliedOn(before.appliedOn());
        request.setNote("更新テスト");
        request.setVersion(before.version());

        ApplicationResponse after = applicationService.update(before.id(), request);

        assertThat(after.applicantName()).isEqualTo("更新後 氏名");
        assertThat(after.categoryName()).isEqualTo("住民票交付申請");
        assertThat(after.version()).isEqualTo(before.version() + 1);
    }

    @Test
    @DisplayName("更新_古いバージョンを送ると楽観ロック競合になる")
    void update_detectsOptimisticLockConflict() {
        ApplicationResponse target = findFirstWithStatus(ApplicationStatus.RECEIVED);

        ApplicationUpdateRequest request = new ApplicationUpdateRequest();
        request.setApplicantName("競合テスト");
        request.setApplicantEmail("conflict@example.jp");
        request.setCategoryCode(target.categoryCode());
        request.setAppliedOn(target.appliedOn());
        // 実際のバージョンより古い値を渡す = 他ユーザーが先に更新した状況の再現
        request.setVersion(target.version() - 1);

        assertThatThrownBy(() -> applicationService.update(target.id(), request))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    @DisplayName("更新_終了状態（承認済）の申請は編集できない")
    void update_rejectsClosedApplication() {
        ApplicationResponse approved = findFirstWithStatus(ApplicationStatus.APPROVED);

        ApplicationUpdateRequest request = new ApplicationUpdateRequest();
        request.setApplicantName("編集できないはず");
        request.setApplicantEmail("closed@example.jp");
        request.setCategoryCode(approved.categoryCode());
        request.setAppliedOn(approved.appliedOn());
        request.setVersion(approved.version());

        assertThatThrownBy(() -> applicationService.update(approved.id(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("編集できません");
    }

    // ------------------------------------------------------------------
    // ステータス変更（状態遷移ルール）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ステータス変更_受付済から審査中へは遷移できる")
    void updateStatus_allowsValidTransition() {
        ApplicationResponse target = findFirstWithStatus(ApplicationStatus.RECEIVED);

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(ApplicationStatus.REVIEWING);
        request.setVersion(target.version());

        ApplicationResponse after = applicationService.updateStatus(target.id(), request);

        assertThat(after.status()).isEqualTo(ApplicationStatus.REVIEWING.name());
        assertThat(after.statusLabel()).isEqualTo("審査中");
        assertThat(after.version()).isEqualTo(target.version() + 1);
    }

    @Test
    @DisplayName("ステータス変更_受付済から承認へは直接遷移できない")
    void updateStatus_rejectsInvalidTransition() {
        ApplicationResponse target = findFirstWithStatus(ApplicationStatus.RECEIVED);

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(ApplicationStatus.APPROVED);
        request.setVersion(target.version());

        assertThatThrownBy(() -> applicationService.updateStatus(target.id(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("「受付済」から「承認」へのステータス変更はできません");
    }

    // ------------------------------------------------------------------
    // 削除
    // ------------------------------------------------------------------

    @Test
    @DisplayName("削除_削除後にIDで取得すると404相当の例外になる")
    void delete_removesRecord() {
        ApplicationResponse target = findFirstWithStatus(ApplicationStatus.RECEIVED);

        applicationService.delete(target.id());

        assertThatThrownBy(() -> applicationService.findById(target.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("削除_存在しないIDはResourceNotFoundExceptionになる")
    void delete_throwsWhenNotFound() {
        assertThatThrownBy(() -> applicationService.delete(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** 指定ステータスの申請を1件取り出すテスト用ヘルパー */
    private ApplicationResponse findFirstWithStatus(ApplicationStatus status) {
        ApplicationSearchCriteria criteria = new ApplicationSearchCriteria();
        criteria.setStatus(status);
        PageResponse<ApplicationResponse> page = applicationService.search(criteria);
        assertThat(page.content()).as("テストデータに %s の申請が必要", status).isNotEmpty();
        return page.content().get(0);
    }
}
