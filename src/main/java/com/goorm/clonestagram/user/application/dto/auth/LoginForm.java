package com.goorm.clonestagram.user.application.dto.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LoginForm {

	@Getter
	@NotBlank(message = "이메일을 입력해주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	@Getter
	@NotBlank(message = "비밀번호를 입력해주세요.")
	private String password;

	public LoginForm(String email, String password) {
		this.email = email;
		this.password = password;
	}

}

