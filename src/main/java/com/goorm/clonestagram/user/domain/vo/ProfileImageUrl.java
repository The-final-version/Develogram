package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class ProfileImageUrl {

	private final String url;

	// JPA용 protected 생성자
	protected ProfileImageUrl() {
		this.url = null;
	}

	public ProfileImageUrl(String url) {
		/*if (!url.startsWith("http")) {
			throw new IllegalArgumentException("잘못된 URL 형식입니다. -> " + url);
		}*/
		this.url = url;
	}

	@JsonValue
	public String getUrl() {
		return url;
	}
}
