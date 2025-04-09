package com.goorm.clonestagram.user.application.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import lombok.Getter;

@Getter
public class JoinDto {
	private final UserEmail email;
	private final String password;
	private final String confirmPassword;
	private final UserName name;

	@JsonCreator
	public JoinDto(
		@JsonProperty("email")           String email,
		@JsonProperty("password")        String password,
		@JsonProperty("confirmPassword") String confirmPassword,
		@JsonProperty("name")            String name) {
		this.email = new UserEmail(email);
		this.password = password;
		this.confirmPassword = confirmPassword;
		this.name = new UserName(name);
	}

	@JsonPOJOBuilder(withPrefix = "")
	public static class JoinDtoBuilder {
	}

	public String getEmail() {
		return email.getEmail();
	}

	public String getName() {
		return name.getName();
	}
}
