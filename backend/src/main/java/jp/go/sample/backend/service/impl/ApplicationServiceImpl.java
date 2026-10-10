package jp.go.sample.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.go.sample.backend.common.dto.PageResponse;
import jp.go.sample.backend.common.exception.BusinessException;
import jp.go.sample.backend.common.exception.ErrorCode;
import jp.go.sample.backend.common.exception.OptimisticLockConflictException;
import jp.go.sample.backend.common.exception.ResourceNotFoundException;
import jp.go.sample.backend.domain.model.Application;
import jp.go.sample.backend.domain.model.ApplicationStatus;
import jp.go.sample.backend.domain.repository.ApplicationCategoryMapper;
import jp.go.sample.backend.domain.repository.ApplicationMapper;
import jp.go.sample.backend.service.ApplicationService;
import jp.go.sample.backend.web.dto.ApplicationCreateRequest;
import jp.go.sample.backend.web.dto.ApplicationResponse;
import jp.go.sample.backend.web.dto.ApplicationSearchCriteria;
import jp.go.sample.backend.web.dto.ApplicationStatusUpdateRequest;
import jp.go.sample.backend.web.dto.ApplicationUpdateRequest;
import lombok.RequiredArgsConstructor;

/**
 * 申請業務のサービス実装。<b>業務ロジックはすべてこの層に置く</b>のが原則。
 *
 * <p>役割分担の目安:
 * <ul>
 *   <li>Controller … HTTPの入出力変換のみ（薄く保つ）</li>
 *   <li><b>Service … 業務ロジック・トランザクション境界</b></li>
 *   <li>Mapper … SQL発行のみ</li>
 * </ul>
 *
 * <p>{@code @RequiredArgsConstructor}（Lombok）は final フィールドを引数に取る
 * コンストラクタを生成する。Spring はコンストラクタが1つだけなら
 * {@code @Autowired} 無しでもDIしてくれるため、これがコンストラクタインジェクションの
 * 定型になっている（フィールドインジェクションより推奨される）。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // クラス全体の既定を「参照のみ」にし、更新系だけ個別に上書きする
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final ApplicationMapper applicationMapper;
    private final ApplicationCategoryMapper categoryMapper;

    @Override
    public PageResponse<ApplicationResponse> search(ApplicationSearchCriteria criteria) {
        // 件数0なら明細取得のSQLを投げない（無駄なクエリを減らす定番の最適化）
        long total = applicationMapper.countBySearchCriteria(criteria);
        if (total == 0) {
            return PageResponse.of(List.of(), criteria.getPage(), criteria.getSize(), 0);
        }

        List<ApplicationResponse> content =
                applicationMapper.search(criteria).stream().map(ApplicationResponse::from).toList();

        return PageResponse.of(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public ApplicationResponse findById(Long id) {
        return ApplicationResponse.from(findEntityOrThrow(id));
    }

    @Override
    @Transactional // ここから更新系。既定の readOnly=true を上書きしている
    public ApplicationResponse create(ApplicationCreateRequest request) {
        validateCategoryExists(request.getCategoryCode());

        LocalDateTime now = LocalDateTime.now();

        Application entity = new Application();
        entity.setApplicationNo(generateApplicationNo(now.getYear()));
        entity.setApplicantName(request.getApplicantName());
        entity.setApplicantEmail(request.getApplicantEmail());
        entity.setCategoryCode(request.getCategoryCode());
        entity.setAppliedOn(request.getAppliedOn());
        entity.setNote(request.getNote());
        // 新規は必ず「受付済」から開始する。画面から任意のステータスを指定させない
        entity.setStatus(ApplicationStatus.RECEIVED);
        entity.setVersion(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        applicationMapper.insert(entity);
        log.info("申請を登録しました: id={}, applicationNo={}", entity.getId(), entity.getApplicationNo());

        // INSERT時に自動採番されたIDで読み直す（JOINした区分名も一緒に取得するため）
        return ApplicationResponse.from(findEntityOrThrow(entity.getId()));
    }

    @Override
    @Transactional
    public ApplicationResponse update(Long id, ApplicationUpdateRequest request) {
        Application current = findEntityOrThrow(id);

        if (current.getStatus().isClosed()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "ステータスが「%s」の申請は編集できません。".formatted(current.getStatus().getLabel()));
        }
        validateCategoryExists(request.getCategoryCode());

        current.setApplicantName(request.getApplicantName());
        current.setApplicantEmail(request.getApplicantEmail());
        current.setCategoryCode(request.getCategoryCode());
        current.setAppliedOn(request.getAppliedOn());
        current.setNote(request.getNote());
        current.setUpdatedAt(LocalDateTime.now());
        // 画面が見ていた時点のバージョンをWHERE条件に使う
        current.setVersion(request.getVersion());

        executeUpdateWithOptimisticLock(current);
        log.info("申請を更新しました: id={}", id);

        return ApplicationResponse.from(findEntityOrThrow(id));
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(Long id, ApplicationStatusUpdateRequest request) {
        Application current = findEntityOrThrow(id);
        ApplicationStatus from = current.getStatus();
        ApplicationStatus to = request.getStatus();

        // 状態遷移の妥当性チェック。業務ルールの中核なのでサービス層で必ず検証する
        if (!from.canTransitionTo(to)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "「%s」から「%s」へのステータス変更はできません。"
                            .formatted(from.getLabel(), to.getLabel()));
        }

        current.setStatus(to);
        current.setUpdatedAt(LocalDateTime.now());
        current.setVersion(request.getVersion());

        executeUpdateWithOptimisticLock(current);
        log.info("ステータスを変更しました: id={}, {} -> {}", id, from, to);

        return ApplicationResponse.from(findEntityOrThrow(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 存在チェックを先に行うことで、0件削除を「成功」と誤認させない
        findEntityOrThrow(id);
        applicationMapper.deleteById(id);
        log.info("申請を削除しました: id={}", id);
    }

    // ------------------------------------------------------------------
    // 以下、private なヘルパー
    // ------------------------------------------------------------------

    /**
     * ID検索して、無ければ404相当の例外を投げる。
     *
     * <p>{@code Optional.orElseThrow} に例外のサプライヤを渡す形は、
     * 「取得できなければ例外」を1行で書けるため多用される。
     */
    private Application findEntityOrThrow(Long id) {
        return applicationMapper
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("申請", id));
    }

    /** 区分コードがマスタに存在するか検証する（外部キー違反を業務エラーに翻訳する） */
    private void validateCategoryExists(String categoryCode) {
        if (categoryMapper.findByCode(categoryCode).isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "申請区分コード「%s」は存在しません。".formatted(categoryCode));
        }
    }

    /**
     * UPDATE を実行し、更新件数が0なら楽観ロック競合として例外にする。
     *
     * <p>MyBatis の update は「更新された行数」を返す。
     * WHERE句に {@code version} を入れているため、
     * 他ユーザーが先に更新してバージョンが進んでいると0件になる。
     */
    private void executeUpdateWithOptimisticLock(Application entity) {
        int updated = applicationMapper.update(entity);
        if (updated == 0) {
            log.warn("楽観ロック競合が発生しました: id={}, version={}", entity.getId(), entity.getVersion());
            throw new OptimisticLockConflictException();
        }
    }

    /**
     * 申請番号を採番する。形式は {@code APP-<年>-<5桁連番>}。
     *
     * <p>学習用の簡易実装。実案件では「採番テーブル＋行ロック」や
     * 「DBシーケンス」を使い、同時実行でも重複しないようにする必要がある
     * （この実装は同時INSERTで重複する余地が残っている点に注意）。
     */
    private String generateApplicationNo(int year) {
        Integer maxSequence = applicationMapper.findMaxSequenceByYear(year);
        int next = (maxSequence == null ? 0 : maxSequence) + 1;
        return "APP-%d-%05d".formatted(year, next);
    }
}
