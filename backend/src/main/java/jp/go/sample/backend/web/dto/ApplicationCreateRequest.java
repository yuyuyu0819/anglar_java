package jp.go.sample.backend.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申請登録リクエスト。
 *
 * <p><b>なぜエンティティを直接受け取らないのか</b><br>
 * {@code Application} をそのまま受け取ると、画面から {@code id} や {@code status} を
 * 自由に送り込めてしまう（いわゆるマスアサインメント脆弱性）。
 * 「受け取ってよい項目だけ」を持つ専用クラスを用意するのが定石。
 *
 * <p>検証は Bean Validation のアノテーションで宣言し、
 * コントローラ側の {@code @Valid} で発火させる。
 * 違反時は {@code MethodArgumentNotValidException} が送出され、
 * {@code GlobalExceptionHandler} が400レスポンスに変換する。
 */
@Data
public class ApplicationCreateRequest {

    @NotBlank(message = "申請者氏名は必須です")
    @Size(max = 100, message = "申請者氏名は100文字以内で入力してください")
    private String applicantName;

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String applicantEmail;

    @NotBlank(message = "申請区分は必須です")
    @Size(max = 10)
    private String categoryCode;

    @NotNull(message = "申請日は必須です")
    @PastOrPresent(message = "申請日に未来日は指定できません")
    private LocalDate appliedOn;

    @Size(max = 1000, message = "備考は1000文字以内で入力してください")
    private String note;
}
