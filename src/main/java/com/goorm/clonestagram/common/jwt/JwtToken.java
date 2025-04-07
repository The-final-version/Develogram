package com.goorm.clonestagram.common.jwt;

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
	private String accessToken;
	private String refreshToken;
}
