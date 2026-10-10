package jp.go.sample.backend.web.dto;

import jp.go.sample.backend.domain.model.ApplicationCategory;

/** 申請区分マスタのレスポンスDTO（画面のプルダウン用）。 */
public record ApplicationCategoryResponse(String code, String name, Integer displayOrder) {

    public static ApplicationCategoryResponse from(ApplicationCategory entity) {
        return new ApplicationCategoryResponse(
                entity.getCode(), entity.getName(), entity.getDisplayOrder());
    }
}
