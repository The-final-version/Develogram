package com.goorm.clonestagram.common.jwt;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

import com.goorm.clonestagram.exception.user.error.AuthTokenMissingException;
import com.goorm.clonestagram.exception.user.error.DuplicateLoginException;
import com.goorm.clonestagram.exception.user.error.ExpiredJwtExceptionCustom;
import com.goorm.clonestagram.exception.user.error.InvalidJwtException;

@Slf4j
@Component
public class JwtTokenProvider {

	private final Key key;
	private final LoginDeviceRegistry registry;

	public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, LoginDeviceRegistry registry) {
		this.registry = registry;
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * 토큰 생성
	 *
	 * @param userEmail    사용자 이메일
	 * @param device       로그인한 기기 정보
	 * @return JwtToken (accessToken, refreshToken 등)
	 */
	/**
	 * 로그인 시 토큰 생성 + 중복 로그인 차단
	 */
	public JwtToken generateToken(String userEmail, String device) {

		// 1) 다른 기기에서 이미 로그인되어 있으면 거부
		if (registry.isDuplicated(userEmail, device)) {
			throw new DuplicateLoginException("중복 로그인: 이미 다른 기기에서 사용 중입니다.");
		}

		// 2) (email → device) 저장 / 갱신
		registry.save(userEmail, device);

		long now = System.currentTimeMillis();
		Date accessExp  = new Date(now + 60 * 60 * 1000L);  // 1h
		Date refreshExp = new Date(now + 7  * 24 * 60 * 60 * 1000L); // 7d

		// Access Token
		String accessToken = Jwts.builder()
			.setSubject(userEmail)
			.claim("auth", "ROLE_USER")
			.claim("device", device)
			.setExpiration(accessExp)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// Refresh Token
		String refreshToken = Jwts.builder()
			.setExpiration(refreshExp)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		return JwtToken.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.accessTokenExpiration(LocalDateTime.now().plusHours(1))
			.refreshTokenExpiration(LocalDateTime.now().plusDays(7))
			.loginTime(LocalDateTime.now())
			.device(device)
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
			throw new AuthTokenMissingException();
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
	public boolean validateToken(String token) throws ExpiredJwtExceptionCustom, InvalidJwtException {
		if (token == null || token.isBlank())
			throw new AuthTokenMissingException();
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			throw new ExpiredJwtExceptionCustom();
		} catch (SecurityException | MalformedJwtException |
				 UnsupportedJwtException | IllegalArgumentException e) {
			throw new InvalidJwtException();
		}
	}

	/**
	 * 클레임(Claims) 파싱
	 *
	 * @param accessToken JWT accessToken
	 * @return Claims
	 */
	Claims parseClaims(String accessToken) {
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
