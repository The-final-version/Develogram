package com.goorm.clonestagram.user.application.service.auth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;

import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.*;

import com.goorm.clonestagram.common.jwt.JwtToken;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.common.jwt.LoginDeviceRegistry;
import com.goorm.clonestagram.exception.user.error.DuplicateLoginException;
import com.goorm.clonestagram.user.application.dto.auth.LoginResponseDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // LENIENT 설정 추가
public class DuplicateDeviceLoginExceptionTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenProvider jwtProvider;

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@InjectMocks
	private UserLoginService userLoginService;

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpSession mockSession;

	private User mockUser;
	private JwtToken mockJwtToken;

	private static final String EMAIL = "test@example.com";
	private static final String PASSWORD = "validPassword";

	@BeforeEach
	void setUp() {
		mockUser = mock(User.class);
		mockJwtToken = mock(JwtToken.class);

		// 설정: 유저 정보와 JWT 토큰 설정
		when(userInternalQueryService.findByEmail(EMAIL)).thenReturn(mockUser);
		when(mockUser.authenticate(PASSWORD)).thenReturn(true);
		when(jwtProvider.generateToken(EMAIL, "device-0")).thenReturn(mockJwtToken);
		when(mockJwtToken.getAccessToken()).thenReturn("ACCESS_TOKEN");
		when(mockJwtToken.getRefreshToken()).thenReturn("REFRESH_TOKEN");
		when(mockJwtToken.getDevice()).thenReturn("device-0");
	}

	@Test
	@DisplayName("첫 번째 기기에서 로그인 후 10개 기기까지 중복 로그인 예외 발생")
	void testDuplicateLogin() throws Exception {
		// Mocking LoginDeviceRegistry match() method
		try (MockedStatic<LoginDeviceRegistry> mockedRegistry = mockStatic(LoginDeviceRegistry.class)) {
			// 첫 번째 로그인: 정상적으로 로그인
			mockedRegistry.when(() -> LoginDeviceRegistry.match(EMAIL, "device-0")).thenReturn(false); // No duplication
			when(mockRequest.getSession(true)).thenReturn(mockSession); // Ensure session exists
			when(mockRequest.getHeader("User-Agent")).thenReturn("device-0");

			log.info("첫 번째 기기에서 로그인 시도: device-0");

			// 첫 번째 로그인 시도
			LoginResponseDto response = userLoginService.loginAndBuildResponse(EMAIL, PASSWORD, mockRequest, "device-0");
			log.info("첫 번째 로그인 응답: {}", response.getMessage());
			assertThat(response.getMessage()).isEqualTo("로그인 성공");

			// 두 번째부터 열 번째 기기에서 로그인 시도 (중복 로그인 예외 발생해야 함)
			for (int i = 1; i <= 10; i++) {
				final String device = "device-" + i;
				mockedRegistry.when(() -> LoginDeviceRegistry.match(EMAIL, device)).thenReturn(true); // Duplicate login for each subsequent device
				when(mockRequest.getHeader("User-Agent")).thenReturn(device);

				log.info("기기 {}에서 로그인 시도", i);

				// 중복 로그인 예외가 발생해야 한다
				assertThatThrownBy(() -> userLoginService.loginAndBuildResponse(EMAIL, PASSWORD, mockRequest, device))
					.isInstanceOf(DuplicateLoginException.class)
					.hasMessageContaining("이미 다른 기기에서 로그인한 계정입니다.");

				log.info("기기 {}에서 중복 로그인 예외 발생: 이미 다른 기기에서 로그인한 계정입니다.", i);
			}
		}
	}
}

