package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;
import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.exception.ErrorCode;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class ProfileBio {

	private final String bio;

	protected ProfileBio() {
		this.bio = null;
	}

	public ProfileBio(String bio) {
		if (bio != null && bio.length() > 300)
			throw new BusinessException(ErrorCode.USER_BIO_LENGTH);
		this.bio = bio;
	}

	@JsonValue
	public String getBio() {
		return bio;
	}
}
