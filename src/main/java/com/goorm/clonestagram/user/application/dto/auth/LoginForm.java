package com.goorm.clonestagram.user.application.dto.auth;


import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;
import com.goorm.clonestagram.user.domain.vo.UserEmail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
public class LoginForm {
	private UserEmail email;
	private String password;

	public LoginForm(String email, String password) {
		this.email = new UserEmail(email);
		if (password == null || password.isBlank())
			throw new BusinessException(ErrorCode.USER_PASSWORD_BLANK);
		this.password = password;
	}

	public String getEmail() {
		return email.getEmail();
	}
}

