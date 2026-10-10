package jp.go.sample.backend.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.go.sample.backend.service.MasterDataService;
import jp.go.sample.backend.web.dto.ApplicationCategoryResponse;
import jp.go.sample.backend.web.dto.StatusOptionResponse;
import lombok.RequiredArgsConstructor;

/**
 * マスタ参照API（画面のプルダウン用）。
 *
 * <p>画面を開いたときに「申請区分」「ステータス」の選択肢を取りに来る。
 * フロント側でハードコードしないことで、マスタ変更時の改修範囲を小さくできる。
 */
@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    /** {@code GET /api/master/categories} */
    @GetMapping("/categories")
    public List<ApplicationCategoryResponse> categories() {
        return masterDataService.findAllCategories();
    }

    /** {@code GET /api/master/statuses} */
    @GetMapping("/statuses")
    public List<StatusOptionResponse> statuses() {
        return masterDataService.findAllStatuses();
    }
}
