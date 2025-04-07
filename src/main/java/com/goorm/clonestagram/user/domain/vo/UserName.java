package com.goorm.clonestagram.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserName {

	private final String name;

	// JPA용 protected 생성자
	protected UserName() {
		this.name = null;
	}

	public UserName(String name) {
		/*if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("이름을 입력해주세요.");
		}*/
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return name;
	}

}
