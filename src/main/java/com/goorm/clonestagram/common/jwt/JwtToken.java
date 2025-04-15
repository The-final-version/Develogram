package com.goorm.clonestagram.common.jwt;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 발급된 JWT 토큰(Access/Refresh) 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {
	private String grantType;
	private String device;              // 접속 기기
	private LocalDateTime loginTime;    // 로그인 시간
	private String userId;             // 사용자 ID
	private String message;            // 응답 메시지
	private String accessToken;
	private String refreshToken;
	private LocalDateTime accessTokenExpiration;  // accessToken 만료 시간
	private LocalDateTime refreshTokenExpiration; // refreshToken 만료 시간

	public JwtToken(String bearer, String dummyAccessToken, String dummyRefreshToken, LocalDateTime now, LocalDateTime now1, String s) {
		this.accessToken = bearer + dummyAccessToken;
		this.refreshToken = bearer + dummyRefreshToken;
		this.accessTokenExpiration = now;
		this.refreshTokenExpiration = now1;
		this.userId = s;

	}
}
