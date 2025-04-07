package com.goorm.clonestagram.user.application.service.auth;

import com.goorm.clonestagram.common.jwt.JwtToken;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.user.domain.vo.UserEmail;
import com.goorm.clonestagram.user.domain.vo.UserName;
import com.goorm.clonestagram.user.domain.vo.UserPassword;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserLoginService 단위 테스트
 * - 최대한 많은 시나리오(정상 / 실패 / 예외)를 커버
 * - "UnnecessaryStubbingException" / "MissingMethodInvocationException" 방지
 */
@ExtendWith(MockitoExtension.class)
class UserLoginServiceTest {

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
	private static final String VALID_EMAIL    = "test@example.com";
	private static final String VALID_PASSWORD = "validPassword";

	private User spyUser;
	private JwtToken mockJwtToken;

	@BeforeEach
	void setUp() {
		User realUser = User.builder()
			.id(123L)
			.email(new UserEmail(VALID_EMAIL))
			.password(new UserPassword("dummyPassword12"))
			.name(new UserName("홍길동"))
			.build();
		spyUser = spy(realUser);

		mockJwtToken = JwtToken.builder()
			.accessToken("ACCESS_TOKEN")
			.refreshToken("REFRESH_TOKEN")
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

		// JWT
		when(jwtProvider.generateToken(VALID_EMAIL)).thenReturn(mockJwtToken);

		// session
		when(mockRequest.getSession(true)).thenReturn(mockSession);

		// when
		LoginResponseDto result = userLoginService.loginAndBuildResponse(
			VALID_EMAIL, VALID_PASSWORD, mockRequest);

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
		@DisplayName("User 찾기 자체가 실패 -> BadCredentialsException & 실패 응답")
		void domainAuthFail_UserNotFound() {
			// given
			when(userInternalQueryService.findByEmail(VALID_EMAIL))
				.thenThrow(new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다."));

			// when
			LoginResponseDto result = userLoginService.loginAndBuildResponse(
				VALID_EMAIL, VALID_PASSWORD, mockRequest);

			// then
			assertThat(result.getMessage()).contains("로그인 실패");
			assertThat(result.getAccessToken()).isNull();
			verify(authenticationManager, never()).authenticate(any());
		}

		@Test
		@DisplayName("비밀번호 불일치 -> BadCredentialsException & 실패 응답")
		void domainAuthFail_WrongPassword() {
			// given
			when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
			when(spyUser.authenticate(VALID_PASSWORD)).thenReturn(false);

			// when
			LoginResponseDto result = userLoginService.loginAndBuildResponse(
				VALID_EMAIL, VALID_PASSWORD, mockRequest);

			// then
			assertThat(result.getMessage()).contains("로그인 실패");
			assertThat(result.getAccessToken()).isNull();
			// Security auth never called
			verify(authenticationManager, never()).authenticate(any());
		}
	}

	/* ====================================================================
	   스프링 시큐리티 인증 (authenticateWithSecurity) 실패
	   ==================================================================== */
	@Nested
	@DisplayName("시큐리티 인증 실패 시나리오")
	class SecurityAuthFailCases {

		@Test
		@DisplayName("domain 인증은 성공, but security auth fails -> BadCredentialsException")
		void securityAuthFail() {
			// given
			when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
			when(spyUser.authenticate(VALID_PASSWORD)).thenReturn(true);

			// security fails
			when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("security fail"));

			// when
			LoginResponseDto result = userLoginService.loginAndBuildResponse(
				VALID_EMAIL, VALID_PASSWORD, mockRequest);

			// then
			assertThat(result.getMessage()).contains("로그인 실패");
			verify(mockRequest, never()).getSession(true);
		}
	}

	/* ====================================================================
	   JWT 토큰 생성(generateJwtTokens) 실패
	   ==================================================================== */
	@Nested
	@DisplayName("JWT 토큰 생성 실패 시나리오")
	class JwtTokenFailCases {

		@Test
		@DisplayName("domain & security 성공, but generateToken() throws -> RuntimeException")
		void jwtTokenGenerateFail() {
			// given
			when(userInternalQueryService.findByEmail(VALID_EMAIL)).thenReturn(spyUser);
			when(spyUser.authenticate(VALID_PASSWORD)).thenReturn(true);

			Authentication mockAuth = mock(Authentication.class);
			when(authenticationManager.authenticate(any()))
				.thenReturn(mockAuth);

			// session
			when(mockRequest.getSession(true)).thenReturn(mockSession);

			// throw
			when(jwtProvider.generateToken(VALID_EMAIL))
				.thenThrow(new RuntimeException("JWT Error"));

			// when & then
			assertThatThrownBy(() ->
				userLoginService.loginAndBuildResponse(
					VALID_EMAIL, VALID_PASSWORD, mockRequest)
			).isInstanceOf(RuntimeException.class)
				.hasMessage("JWT Error");
		}
	}
}
