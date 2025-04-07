package com.goorm.clonestagram.user.domain.vo;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class UserPassword {

	// BCryptPasswordEncoder는 스태틱 인스턴스로 관리하여 재사용합니다.
	private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final String hashedPassword;

	// JPA용 protected 생성자
	protected UserPassword() {
		this.hashedPassword = null;
	}

	@JsonValue
	public String getHashedPassword() {
		return hashedPassword;
	}

	/**
	 * 생성자: rawPassword를 받아서 BCrypt 해시를 적용합니다.
	 * @param rawPassword 원본 비밀번호
	 */
	public UserPassword(String rawPassword) {
		if (rawPassword == null) {
			throw new IllegalArgumentException("비밀번호를 입력해주세요.");
		}

		if (rawPassword.length() < 8) {
			throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
		}
		this.hashedPassword = encoder.encode(rawPassword);
	}

	/**
	 * DB에서 읽어온 암호화된 비밀번호를 감싸기 위한 정적 팩토리 메서드.
	 */
	public static UserPassword fromHashed(String hashedPassword) {
		return new UserPassword(hashedPassword, true);
	}

	/**
	 * DB에서 읽어온 해시값을 그대로 감싸기 위한 private 생성자
	 */
	private UserPassword(String hashedPassword, boolean alreadyEncoded) {
		this.hashedPassword = hashedPassword;
	}

	/**
	 * 입력된 원본 비밀번호와 저장된 해시값을 비교하여 일치 여부를 반환합니다.
	 * @param rawPassword 입력된 원본 비밀번호
	 * @return true if 일치, false otherwise
	 */
	public boolean matches(String rawPassword) {
		return encoder.matches(rawPassword, this.hashedPassword);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UserPassword))
			return false;
		UserPassword that = (UserPassword)o;
		return Objects.equals(hashedPassword, that.hashedPassword);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hashedPassword);
	}

	@Override
	public String toString() {
		return "***********"; // 보안을 위해 원본 출력은 감춤
	}
}
