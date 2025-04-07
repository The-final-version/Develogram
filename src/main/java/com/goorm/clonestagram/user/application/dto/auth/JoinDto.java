package com.goorm.clonestagram.user.application.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class JoinDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "유효한 이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
    @Size(max = 20, message = "비밀번호는 20자 이하이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;

    @NotBlank(message = "이름을 입력해주세요.")
    private String username;

    // 비밀번호와 비밀번호 확인이 일치하는지 체크하는 커스텀 유효성 검사
    // TODO: 이건 왜 있는거지?
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
