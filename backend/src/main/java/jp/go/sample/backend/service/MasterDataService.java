package jp.go.sample.backend.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.go.sample.backend.domain.model.ApplicationStatus;
import jp.go.sample.backend.domain.repository.ApplicationCategoryMapper;
import jp.go.sample.backend.web.dto.ApplicationCategoryResponse;
import jp.go.sample.backend.web.dto.StatusOptionResponse;
import lombok.RequiredArgsConstructor;

/**
 * マスタ参照サービス（申請区分・ステータスの選択肢）。
 *
 * <p>画面のプルダウンに出す値をAPIから取る形にしておくと、
 * マスタを増やしたときにフロントの修正が不要になる。
 * 官公庁案件では年度替わりで区分が変わることがあり、この作りが効く。
 *
 * <p>こちらはインターフェースを切らずクラス単体にしている。
 * 「業務ロジックが薄い参照系はクラスのみ」という割り切りも実務ではよくある。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {

    private final ApplicationCategoryMapper categoryMapper;

    /** 申請区分の一覧（表示順） */
    public List<ApplicationCategoryResponse> findAllCategories() {
        return categoryMapper.findAll().stream().map(ApplicationCategoryResponse::from).toList();
    }

    /** ステータスの一覧（enum定義から生成するのでDBアクセス不要） */
    public List<StatusOptionResponse> findAllStatuses() {
        return Arrays.stream(ApplicationStatus.values()).map(StatusOptionResponse::from).toList();
    }
}
