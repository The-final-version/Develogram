package com.goorm.clonestagram.user.application.service.auth;

import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.DuplicateEmailException;
import com.goorm.clonestagram.exception.user.error.PasswordMismatchException;
import com.goorm.clonestagram.user.application.dto.auth.JoinDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserJoinService 단일 테스트
 * - Bean Validation (+ Service 로직)으로 발생하는 에러 메시지 동일 시 통과
 */
@ExtendWith(MockitoExtension.class)
class UserJoinServiceTest {

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@InjectMocks
	private UserJoinService userJoinService;

	@Test
	@DisplayName("정상 가입: Bean Validation 통과 & 이메일 미중복 → saveUser() 호출")
	void joinProcess_Success() {
		// given
		JoinDto dto = new JoinDto("valid@test.com", "mypassword11@", "mypassword11@", "홍길동");

		when(userInternalQueryService.existsByEmail("valid@test.com"))
			.thenReturn(false);

		// when
		userJoinService.joinProcess(dto);

		// then
		verify(userInternalQueryService, times(1)).saveUser(any(User.class));
	}

	@Nested
	@DisplayName("Bean Validation 위반 시나리오")
	class BeanValidationCases {

		@Test
		@DisplayName("이메일 중복 시 -> '이미 사용 중인 이메일입니다: ...' (IllegalStateException)")
		void duplicateEmail_Fail() {
			JoinDto dto = new JoinDto("duplicate@test.com", "mypassword1!", "mypassword1!", "홍길동");

			// Bean Validation 통과
			when(userInternalQueryService.existsByEmail("duplicate@test.com"))
				.thenReturn(true);

			DuplicateEmailException ex =
				assertThrows(DuplicateEmailException.class, () -> userJoinService.joinProcess(dto));

			assertThat(ex.getMessage()).contains(ErrorCode.USER_DUPLICATE_EMAIL.getMessage());
			verify(userInternalQueryService, never()).saveUser(any());
		}
	}

	@Test
	@DisplayName("비밀번호 불일치 시 -> '비밀번호가 일치하지 않습니다.' (PasswordMismatchException)")
	void passwordMismatch_Fail() {
		// given: 비밀번호와 비밀번호 확인 값이 다른 경우
		JoinDto dto = new JoinDto("valid@test.com", "password123@", "differentPassword", "홍길동");

		when(userInternalQueryService.existsByEmail("valid@test.com"))
			.thenReturn(false);

		// when & then
		PasswordMismatchException ex =
			assertThrows(PasswordMismatchException.class, () -> userJoinService.joinProcess(dto));

		assertThat(ex.getMessage()).contains("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
		verify(userInternalQueryService, never()).saveUser(any());
	}
}
