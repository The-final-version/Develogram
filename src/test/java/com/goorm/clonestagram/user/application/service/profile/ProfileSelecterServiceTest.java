package com.goorm.clonestagram.user.application.service.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

@ExtendWith(MockitoExtension.class)
class ProfileSelecterServiceTest {

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@InjectMocks
	private ProfileSelecterService profileSelecterService;

	private final Long userId = 1L;

	@Test
	@DisplayName("정상 케이스: 사용자 프로필 조회 성공")
	void testGetUserProfile_Success() {
		// given: 사용자 존재하는 경우
		User mockUser = mock(User.class);
		when(userInternalQueryService.findByIdAndDeletedIsFalse(eq(userId))).thenReturn(mockUser);

		// when
		User result = profileSelecterService.getUserProfile(userId);
		// then
		assertNotNull(result);
		assertEquals(mockUser, result);
	}

	@Test
	@DisplayName("실패: 사용자 미존재 시 null 또는 예외 발생 (기대 결과에 따라)")
	void testGetUserProfile_UserNotFound() {
		// given: 존재하지 않는 사용자, javadoc에 의하면 예외 발생을 기대하지만 구현상 null을 반환할 수 있음
		when(userInternalQueryService.findByIdAndDeletedIsFalse(eq(userId))).thenReturn(null);

		// when
		User result = profileSelecterService.getUserProfile(userId);

		// then: null 반환 여부 또는 별도 예외 처리를 확인 (여기서는 null 반환으로 검증)
		assertNull(result);
	}

	@Test
	@DisplayName("실패: 내부 서비스에서 예외 발생 시 예외 전파")
	void testGetUserProfile_ServiceException() {
		// given: 내부 조회 시 예외 발생
		when(userInternalQueryService.findByIdAndDeletedIsFalse(eq(userId)))
			.thenThrow(new IllegalArgumentException("User not found"));

		// when & then
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
			profileSelecterService.getUserProfile(userId));
		assertTrue(ex.getMessage().contains("User not found"));
	}
}
