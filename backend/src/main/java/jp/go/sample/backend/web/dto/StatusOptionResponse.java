package jp.go.sample.backend.web.dto;

import java.util.List;

import jp.go.sample.backend.domain.model.ApplicationStatus;

/**
 * ステータスの選択肢レスポンス。
 *
 * <p>enum の定義（コード・表示名・遷移可能先）をそのまま画面に渡すことで、
 * 「フロントとバックで業務ルールが二重管理になる」のを避けられる。
 */
public record StatusOptionResponse(String code, String label, List<String> allowedNextStatuses) {

    public static StatusOptionResponse from(ApplicationStatus status) {
        return new StatusOptionResponse(
                status.name(),
                status.getLabel(),
                // メソッド参照 Enum::name ではなくラムダにしている。
                // メソッド参照はレシーバ（ここでは Enum 型）のnull許容性が
                // 未指定なため、null解析を有効にしたIDEが
                // 「needs unchecked conversion to conform to @NonNull」と警告する。
                // ラムダなら引数の型が関数型インターフェース側から確定するので警告が出ない。
                status.allowedNextStatuses().stream().map(next -> next.name()).sorted().toList());
    }
}
