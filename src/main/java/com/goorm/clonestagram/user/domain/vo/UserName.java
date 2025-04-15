package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;
import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserName {

	private final String name;

	protected UserName() {
		this.name = null;
	}

	public UserName(String name) {
		if (name == null || name.trim().isEmpty())
			throw new BusinessException(ErrorCode.USER_NAME_BLANK);
		if (name.length() < 2)
			throw new BusinessException(ErrorCode.USER_NAME_TOO_SHORT);
		if (name.length() > 20)
			throw new BusinessException(ErrorCode.USER_NAME_TOO_LONG);
		if (name.matches(".*[0-9].*"))
			throw new BusinessException(ErrorCode.USER_NAME_NUMBER);
		if (name.matches(".*[!@#$%^&*()\\-+].*"))
			throw new BusinessException(ErrorCode.USER_NAME_SPECIAL);
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return name;
	}
}
