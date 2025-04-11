package com.goorm.clonestagram.user.domain.vo;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;
import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserEmail {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
	private final String email;

	protected UserEmail() {
		this.email = null;
	}

	public UserEmail(String email) {
		if (email == null || email.isBlank())
			throw new BusinessException(ErrorCode.USER_EMAIL_BLANK);
		if (!EMAIL_PATTERN.matcher(email).matches())
			throw new BusinessException(ErrorCode.USER_EMAIL_FORMAT);
		if (email.length() > 50)
			throw new BusinessException(ErrorCode.USER_EMAIL_LENGTH);
		this.email = email;
	}

	@JsonValue
	public String getEmail() {
		return email;
	}
}
