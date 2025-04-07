package com.goorm.clonestagram.user.domain.vo;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserEmail {

	private static final Pattern EMAIL_PATTERN =
		Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

	private final String email;

	// JPA용 protected 생성자
	protected UserEmail() {
		this.email = null;
	}

	public UserEmail(String email) {
		/*if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("이메일 형식을 지켜주세요. \n" + email + "은 이메일 형식이 아닙니다.");
		}*/
		this.email = email;
	}

	@JsonValue
	public String getEmail() {
		return email;
	}
}
