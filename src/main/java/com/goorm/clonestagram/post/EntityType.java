package com.goorm.clonestagram.post;

/**
 * Posts의 ContentType
 * - IMAGE, VIDEO
 */
public enum EntityType {
	POST("post"),
	USER("user");
	private final String value;

	EntityType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
