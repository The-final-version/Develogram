package com.goorm.clonestagram.user.domain.vo;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonValue;
import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.UserValidationException;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserPassword {

	private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();
	private final String hashedPassword;

	protected UserPassword() {
		this.hashedPassword = null;
	}

	public UserPassword(String raw) {
		validate(raw);
		this.hashedPassword = ENC.encode(raw);
	}

	private void validate(String pw) {
		if (pw == null || pw.isBlank())
			throw new UserValidationException(ErrorCode.USER_PASSWORD_BLANK);
		if (pw.length() < 6)
			throw new UserValidationException(ErrorCode.USER_PASSWORD_TOO_SHORT);
		if (pw.length() > 20)
			throw new UserValidationException(ErrorCode.USER_PASSWORD_TOO_LONG);
		if (pw.contains(" "))
			throw new UserValidationException(ErrorCode.USER_PASSWORD_SPACE);
		if (!pw.matches(".*[0-9].*"))
			throw new UserValidationException(ErrorCode.USER_PASSWORD_NO_NUMBER);
		if (!pw.matches(".*[!@#$%^&*()\\-+].*"))
			throw new UserValidationException(ErrorCode.USER_PASSWORD_NO_SPECIAL);
	}

	public static UserPassword fromHashed(String hashed) {
		return new UserPassword(hashed, true);
	}

	private UserPassword(String h, boolean already) {
		this.hashedPassword = h;
	}

	public boolean matches(String raw) {
		return ENC.matches(raw, hashedPassword);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof UserPassword up && Objects.equals(hashedPassword, up.hashedPassword));
	}

	@Override
	public int hashCode() {
		return Objects.hash(hashedPassword);
	}

	@Override
	public String toString() {
		return "***********";
	}

	@JsonValue
	public String getHashedPassword() {
		return hashedPassword;
	}
}
