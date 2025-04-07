package com.goorm.clonestagram.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

	private final JwtTokenProvider jwtTokenProvider;
	// 공개 엔드포인트 목록 (요청 URI가 이 목록으로 시작하면 JWT 인증 로직을 건너뛰도록 함)
	private static final List<String> PUBLIC_ENDPOINTS = List.of(
		"/login",
		"/join",
		"/v3/api-docs",
		"/swagger-ui",
		"/swagger-ui.html",
		"/swagger.html",
		"/search/tag/suggestions",
		"/search/tag",
		"/me"
	);
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestURI = httpRequest.getRequestURI();

		// 공개 엔드포인트인 경우 JWT 인증 로직을 건너뛰고 다음 필터로 바로 진행
		if (PUBLIC_ENDPOINTS.stream().anyMatch(requestURI::startsWith)) {
			chain.doFilter(request, response);
			return;
		}

		// 1. Request Header에서 JWT 토큰 추출
		String token = resolveToken((HttpServletRequest) request);

		// 2. 유효한 토큰인지 확인
		if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
			// 토큰이 유효하면 토큰에서 Authentication 객체를 가져와서 SecurityContext에 저장
			Authentication authentication = jwtTokenProvider.getAuthentication(token);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		// 다음 필터 진행
		chain.doFilter(request, response);
	}

	// Request Header에서 토큰 정보 추출
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization"); // "Authorization: Bearer xxxxxxxxxx"
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
