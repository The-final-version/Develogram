package com.goorm.clonestagram.common.jwt;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

	private final Key key;

	public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * 토큰 생성
	 *
	 * @return JwtToken (accessToken, refreshToken 등)
	 */
	public JwtToken generateToken(String userEmail) {
		long now = (new Date()).getTime();

		// Access Token 유효시간 예시 (24시간)
		Date accessTokenExpiresIn = new Date(now + 86400000);

		// Access Token 생성
		String accessToken = Jwts.builder()
			.setSubject(userEmail)               // 사용자 email
			.claim("auth", "ROLE_USER")   // 권한
			.setExpiration(accessTokenExpiresIn) // 만료 시간
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// Refresh Token (같은 24시간 예시)
		String refreshToken = Jwts.builder()
			.setExpiration(new Date(now + 86400000))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// 결과 반환
		return JwtToken.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	/**
	 * 토큰으로부터 인증 정보를 가져오기
	 *
	 * @param accessToken JWT accessToken
	 * @return Authentication 객체
	 */
	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);

		if (claims.get("auth") == null) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		// 권한 정보 가져오기
		Collection<SimpleGrantedAuthority> authorities =
			Arrays.stream(claims.get("auth").toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		// UserDetails 객체를 만들어서 Authentication 리턴
		UserDetails principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	/**
	 * 토큰 검증
	 *
	 * @param token JWT Token
	 * @return boolean (true: 유효한 토큰, false: 무효한 토큰)
	 */
	public boolean validateToken(String token) {
		try {
			// Jwts.parserBuilder() 를 사용하여 JWT를 파싱, JWT 가 위변조되지 않았는지 secretKey(key)값을 넣어 확인
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.info("Invalid JWT Token", e);
		} catch (ExpiredJwtException e) {
			log.info("Expired JWT Token", e);
		} catch (UnsupportedJwtException e) {
			log.info("Unsupported JWT Token", e);
		} catch (IllegalArgumentException e) {
			log.info("JWT claims string is empty.", e);
		}
		return false;
	}

	/**
	 * 클레임(Claims) 파싱
	 *
	 * @param accessToken JWT accessToken
	 * @return Claims
	 */
	private Claims parseClaims(String accessToken) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(accessToken)
				.getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
