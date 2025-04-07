package com.goorm.clonestagram.user.application.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginForm {
	@Email(message = "이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일을 입력해주세요.")
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하로 입력해주세요.")
	private String password;
}

