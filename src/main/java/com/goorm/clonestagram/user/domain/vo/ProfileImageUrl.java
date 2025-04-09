package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;
import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.UserValidationException;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class ProfileImageUrl {

	private final String url;

	protected ProfileImageUrl() {
		this.url = null;
	}

	public ProfileImageUrl(String url) {
		// if (url == null || url.isBlank())
		// 	throw new UserValidationException(ErrorCode.USER_IMAGE_URL_BLANK);
		if (url.length() > 255)
			throw new UserValidationException(ErrorCode.USER_IMAGE_URL_LENGTH);
		if (url.contains(" "))
			throw new UserValidationException(ErrorCode.USER_IMAGE_URL_SPACE);
		this.url = url;
	}

	@JsonValue
	public String getUrl() {
		return url;
	}
}
