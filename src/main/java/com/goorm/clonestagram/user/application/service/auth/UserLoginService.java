package com.goorm.clonestagram.user.application.service.auth;

import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.common.jwt.JwtToken;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLoginService {

	private final UserInternalQueryService userInternalQueryService;
	private final JwtTokenProvider jwtProvider;
	private final AuthenticationManager authenticationManager;

	/**
	 * (public) 이메일/비밀번호 검증 후 로그인 응답 DTO 생성 로직.
	 * @param email 	이메일
	 * @param password	비밀번호
	 * @param request	HTTP 요청 객체
	 * @return			로그인 응답 DTO
	 */
	public LoginResponseDto loginAndBuildResponse(String email, String password, HttpServletRequest request) {
		try {
			// 1) 이메일 / 비밀번호 (도메인) 검증
			User user = authenticateUser(email, password);

			// 2) 스프링 시큐리티 인증 + 컨텍스트 설정
			Authentication authentication = authenticateWithSecurity(email, password);
			setSecurityContextAndSession(authentication, request);

			// 3) JWT 토큰 생성 (access & refresh)
			JwtToken jwtToken = generateJwtTokens(email);

			// 4) 성공 응답
			return buildSuccessResponse(user, jwtToken);

		} catch (BadCredentialsException e) {
			return buildFailureResponse();
		}
	}

	/* =============================
       ( Private Methods )
   	============================= */

	/**
	 * (Private) 도메인 관점의 이메일, 비밀번호 검증 로직.
	 * @param email 	이메일
	 * @param password	비밀번호
	 * @return			로그인 성공한 사용자 엔티티
	 */
	private User authenticateUser(String email, String password) {
		return Optional.of(userInternalQueryService.findByEmail(email))
			.filter(user -> user.authenticate(password))
			.orElseThrow(() -> new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다."));
	}

	/**
	 * (Private) 스프링 시큐리티 기반의 인증 로직.
	 * - UsernamePasswordAuthenticationToken을 생성하고 AuthenticationManager로 검증
	 * @param email 	이메일
	 * @param password	비밀번호
	 * @return			인증된 Authentication 객체
	 */
	private Authentication authenticateWithSecurity(String email, String password) {
		UsernamePasswordAuthenticationToken authToken =
			new UsernamePasswordAuthenticationToken(email, password);
		return authenticationManager.authenticate(authToken);
	}

	/**
	 * (Private) 스프링 시큐리티 컨텍스트 및 세션 설정
	 * @param authentication 인증된 Authentication 객체
	 * @param request HTTP 요청 객체
	 */
	private void setSecurityContextAndSession(Authentication authentication, HttpServletRequest request) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
		HttpSession session = request.getSession(true);
		session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
	}

	/**
	 * (Private) 이메일 기반으로 AccessToken & RefreshToken 생성
	 * @param email 이메일
	 * @return JWT 토큰 객체
	 */
	private JwtToken generateJwtTokens(String email) {
		return jwtProvider.generateToken(email);
	}

	/**
	 * (Private) 로그인 성공 응답 DTO 생성
	 * @param user 사용자 엔티티
	 * @param jwtToken JWT 토큰 객체
	 * @return 로그인 응답 DTO
	 */
	private LoginResponseDto buildSuccessResponse(User user, JwtToken jwtToken) {
		return LoginResponseDto.builder()
			.message("로그인 성공")
			.userId(String.valueOf(user.getId()))
			.accessToken(jwtToken.getAccessToken())
			.refreshToken(jwtToken.getRefreshToken())
			.build();
	}

	/**
	 * (Private) 로그인 실패 응답 DTO 생성
	 * @return 로그인 실패 응답 DTO
	 */
	private LoginResponseDto buildFailureResponse() {
		return LoginResponseDto.builder()
			.message("로그인 실패 - 잘못된 이메일 또는 비밀번호입니다.")
			.userId(null)
			.accessToken(null)
			.refreshToken(null)
			.build();
	}
}
