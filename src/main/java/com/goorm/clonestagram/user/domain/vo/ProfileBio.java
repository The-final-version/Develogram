package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class ProfileBio {

	private final String bio;

	// JPA용 protected 생성자
	protected ProfileBio() {
		this.bio = null;
	}

	public ProfileBio(String bio) {
		if (bio.length() > 300) {
			throw new IllegalArgumentException("자기소개는 300자 이내로 작성해야 합니다.");
		}
		this.bio = bio;
	}

	@JsonValue
	public String getBio() {
		return bio;
	}
}
