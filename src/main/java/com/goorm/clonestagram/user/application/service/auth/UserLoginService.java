package com.goorm.clonestagram.user.application.service.auth;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.goorm.clonestagram.common.jwt.JwtToken;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.common.jwt.LoginDeviceRegistry;
import com.goorm.clonestagram.exception.user.error.AuthAccountDisabledException;
import com.goorm.clonestagram.exception.user.error.AuthAccountLockedException;
import com.goorm.clonestagram.exception.user.error.AuthCredentialsExpiredException;
import com.goorm.clonestagram.exception.user.error.DuplicateLoginException;
import com.goorm.clonestagram.exception.user.error.InvalidCredentialsException;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	public LoginResponseDto loginAndBuildResponse(String email, String password,
		HttpServletRequest request, String device) throws
		AuthAccountLockedException, AuthAccountDisabledException, AuthCredentialsExpiredException {
		try {
			// 1) 이메일 / 비밀번호 (도메인) 검증
			User user = authenticateUser(email, password);

			// 2) 스프링 시큐리티 인증 + 컨텍스트 설정
			Authentication authentication = authenticateWithSecurity(email, password);
			setSecurityContextAndSession(authentication, request);

			// 3) JWT 토큰 생성 (access & refresh)
			JwtToken jwtToken = generateJwtTokens(email, request, device);

			// 4) 성공 응답
			return buildSuccessResponse(user, jwtToken);

		} catch (BadCredentialsException e) {              // email O, password X
			throw new InvalidCredentialsException();
		} catch (LockedException e) {                      // 잠긴 계정
			throw new AuthAccountLockedException();
		} catch (DisabledException e) {                    // 비활성 계정
			throw new AuthAccountDisabledException();
		} catch (CredentialsExpiredException e) {          // 비밀번호 만료
			throw new AuthCredentialsExpiredException();
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
		User user = userInternalQueryService.findByEmail(email);
		if (user == null) {
			log.error("사용자를 찾을 수 없습니다. 이메일: {}", email);
			throw new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다.");
		}

		if (!user.authenticate(password)) {
			log.error("비밀번호 인증 실패. 이메일: {}", email);
			throw new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다.");
		}

		return user;
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
	private JwtToken generateJwtTokens(String email, HttpServletRequest request, String device) {

		if (device == null) {
			device = "Unknown Device"; // 기기 정보가 없으면 기본값으로 설정
		}

		if(LoginDeviceRegistry.match(email, device)) {
			throw new DuplicateLoginException("이미 다른 기기에서 로그인한 계정입니다.");
		}
		log.debug("User-Agent: {}", device);  // 기기 정보 로깅

		JwtToken jwtToken = jwtProvider.generateToken(email, device);

		if (jwtToken == null) {
			throw new RuntimeException("JWT 토큰 생성 실패");
		}

		return jwtToken;
	}

	/**
	 * (Private) 로그인 성공 응답 DTO 생성
	 * @param user 사용자 엔티티
	 * @param jwtToken JWT 토큰 객체
	 * @return 로그인 응답 DTO
	 */
	private LoginResponseDto buildSuccessResponse(User user, JwtToken jwtToken) {
		String loginTimeStr = jwtToken.getLoginTime() != null ? jwtToken.getLoginTime().toString() : "";

		LocalDateTime loginTime = null;
		if (!loginTimeStr.isEmpty()) {
			try {
				loginTime = LocalDateTime.parse(loginTimeStr);
			} catch (DateTimeParseException e) {
				log.error("Login time parsing failed for user: {}", user.getEmail(), e);
				loginTime = LocalDateTime.now();  // Fallback to current time if parsing fails
			}
		} else {
			loginTime = LocalDateTime.now();  // Default to current time if loginTime is empty or null
		}

		return LoginResponseDto.builder()
			.message("로그인 성공")
			.userId(String.valueOf(user.getId()))
			.accessToken(jwtToken.getAccessToken())
			.refreshToken(jwtToken.getRefreshToken())
			.device(jwtToken.getDevice())  // Ensure device is not null
			.loginTime(loginTime)  // Set login time safely
			.accessTokenExpiration(jwtToken.getAccessTokenExpiration())
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
