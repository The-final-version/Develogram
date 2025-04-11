package com.goorm.clonestagram.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwt;                // 토큰 유틸
	private final LoginDeviceRegistry registry; 	   // 로그인‑기기 저장소
	private final AntPathMatcher matcher = new AntPathMatcher();

	/** 인증을 건너뛸 공개 URL 목록 */
	private static final List<String> PUBLIC = List.of(
		"/login/**", "/join/**",
		"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
		"/search/tag/**", "/me"
	);

	/** 공개 URL 이면 필터를 아예 건너뛴다 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return PUBLIC.stream().anyMatch(p -> matcher.match(p, uri));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req,
		HttpServletResponse res,
		FilterChain chain)
		throws ServletException, IOException {

		/* 1) 토큰 추출 */
		String token = resolveToken(req);

		/* 2) 토큰 검증 + 동일 기기 확인 */
		if (StringUtils.hasText(token)) {
			try {
				if (jwt.validateToken(token)) {
					// var claims = jwt.parseClaims(token);
					// String email  = claims.getSubject();
					// String device = claims.get("device", String.class);
					// // 저장된 기기와 일치하지 않으면 401
					// if (!registry.match(email, device)) {
					// 	res.sendError(HttpServletResponse.SC_UNAUTHORIZED,
					// 		"다른 기기에서 로그인했거나 세션이 만료되었습니다.");
					// 	return;
					// }

					Authentication auth = jwt.getAuthentication(token);
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			} catch (Exception ex) {   // 만료·위조 등
				res.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
				return;
			}
		}

		/* 4) 다음 필터로 진행 */
		chain.doFilter(req, res);
	}

	/** Authorization 헤더에서 Bearer 토큰만 추출 */
	private String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer "))
			? bearer.substring(7) : null;
	}
}
