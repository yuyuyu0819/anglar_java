package jp.go.sample.backend.common.dto;

import java.util.List;

/**
 * ページング結果の共通レスポンス。
 *
 * <p>一覧APIは「このページの明細」と「全体の件数情報」を両方返す必要がある。
 * ここを共通化しておかないと、画面ごとにJSON構造がバラバラになって
 * フロント側の実装コストが膨らむ（大規模案件では致命的になりやすい）。
 *
 * @param <T> 明細の型
 * @param content       このページの明細
 * @param page          現在のページ番号（0始まり）
 * @param size          1ページあたりの件数
 * @param totalElements 条件に一致する総件数
 * @param totalPages    総ページ数
 * @param first         先頭ページかどうか
 * @param last          最終ページかどうか
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page <= 0,
                totalPages == 0 || page >= totalPages - 1);
    }
}
