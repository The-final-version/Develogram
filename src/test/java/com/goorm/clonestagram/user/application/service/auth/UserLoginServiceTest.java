package com.goorm.clonestagram.user.application.service.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.goorm.clonestagram.common.exception.BusinessException;
import com.goorm.clonestagram.common.jwt.JwtToken;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * UserLoginService 단위 테스트
 * - 최대한 많은 시나리오(정상 / 실패 / 예외)를 커버
 * - "UnnecessaryStubbingException" / "MissingMethodInvocationException" 방지
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserLoginServiceTest {

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@Mock
	private JwtTokenProvider jwtProvider;

	@Mock
	private AuthenticationManager authenticationManager;

	@InjectMocks
	private UserLoginService userLoginService;

	// HTTP request & session
	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpSession mockSession;

	// Test constants
	private static final String VALID_EMAIL = "test@example.com";
	private static final String VALID_PASSWORD = "validPassword";

	private User spyUser;
	private JwtToken mockJwtToken;

	@BeforeEach
	void setUp() {
		User realUser = User.secureBuilder()
			.id(123L)
			.email(VALID_EMAIL)
			.password("dummyPassword12!")
			.name("홍길동")
			.isHashed(false)
			.build();
		spyUser = spy(realUser);

		mockJwtToken = JwtToken.builder()
			.accessToken("ACCESS_TOKEN")
			.refreshToken("REFRESH_TOKEN")
			.device("device1")
			.loginTime(LocalDateTime.now())
			.accessTokenExpiration(LocalDateTime.now().plusHours(1))
			.refreshTokenExpiration(LocalDateTime.now().plusDays(7))
			.build();
	}

	/* ====================================================================
	   정상 케이스(도메인 로직 & 시큐리티 인증 모두 성공, 토큰 생성 성공)
	   ==================================================================== */
	@Test
	@DisplayName("정상 로그인 - 모든 인증 로직 통과 & 토큰 생성")
	void loginAndBuildResponse_Success() {
		// given
		// Domain 인증
		when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
		when(spyUser.authenticate(VALID_PASSWORD)).thenReturn(true);

		// Security 인증
		Authentication mockAuth = mock(Authentication.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(mockAuth);

		String device = "device1";
		when(mockRequest.getHeader("User-Agent")).thenReturn(device);
		// JWT
		when(jwtProvider.generateToken(VALID_EMAIL, device)).thenReturn(mockJwtToken);

		// session
		when(mockRequest.getSession(true)).thenReturn(mockSession);

		// when
		LoginResponseDto result = userLoginService.loginAndBuildResponse(
			VALID_EMAIL, VALID_PASSWORD, mockRequest, "device1");

		// then
		assertThat(result.getMessage()).isEqualTo("로그인 성공");
		assertThat(result.getUserId()).isEqualTo("123");
		assertThat(result.getAccessToken()).isEqualTo("ACCESS_TOKEN");
		assertThat(result.getRefreshToken()).isEqualTo("REFRESH_TOKEN");
		verify(mockSession).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any());
	}

	/* ====================================================================
	   도메인 인증 (authenticateUser) 실패 시나리오
	   ==================================================================== */

	@Nested
	@DisplayName("도메인 인증 실패 시나리오")
	class DomainAuthFailCases {

		@Test
		@DisplayName("사용자 미발견 시 → BusinessException")
		void domainAuthFail_UserNotFound(){
			// 이메일을 찾지 못하면 BadCredentialsException을 던짐
			when(userInternalQueryService.findByEmail(VALID_EMAIL))
				.thenThrow(new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다."));

			// loginAndBuildResponse()는 이를 InvalidCredentialsException으로 재포장함.
			assertThatThrownBy(() ->
				userLoginService.loginAndBuildResponse(VALID_EMAIL, VALID_PASSWORD, mockRequest, "device1")
			).isInstanceOf(BadCredentialsException.class);

			// authenticationManager는 호출되지 않아야 함.
			verify(authenticationManager, never()).authenticate(any());
		}

		@Test
		@DisplayName("비밀번호 불일치 시 → BusinessException")
		void domainAuthFail_WrongPassword() throws Exception {
			when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
			doReturn(false).when(spyUser).authenticate(VALID_PASSWORD);

			assertThatThrownBy(() ->
				userLoginService.loginAndBuildResponse(VALID_EMAIL, VALID_PASSWORD, mockRequest, "device1")
			).isInstanceOf(BusinessException.class);

			verify(authenticationManager, never()).authenticate(any());
		}
	}

	@Nested
	@DisplayName("시큐리티 인증 실패 시나리오")
	class SecurityAuthFailCases {

		@Test
		@DisplayName("도메인 인증 성공하였으나 시큐리티 인증 실패 시 → BadCredentialsException")
		void securityAuthFail() throws Exception {
			when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
			doReturn(true).when(spyUser).authenticate(VALID_PASSWORD);

			// authenticationManager가 BadCredentialsException을 던짐
			when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("security auth fail"));

			assertThatThrownBy(() ->
				userLoginService.loginAndBuildResponse(VALID_EMAIL, VALID_PASSWORD, mockRequest,"device1")
			).isInstanceOf(BadCredentialsException.class);

			verify(mockRequest, never()).getSession(true);
		}
	}
}
