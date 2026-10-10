package jp.go.sample.backend.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申請更新リクエスト。
 *
 * <p>登録用との違いは {@code version} を必須で受け取る点。
 * 画面が「どの時点のデータを見て編集を始めたか」を一緒に送ってもらい、
 * その間に他者が更新していたら409を返す（楽観ロック）。
 */
@Data
public class ApplicationUpdateRequest {

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

    @NotNull(message = "バージョンは必須です")
    private Integer version;
}
