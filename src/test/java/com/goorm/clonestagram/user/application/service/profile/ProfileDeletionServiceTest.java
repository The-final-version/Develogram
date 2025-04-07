package com.goorm.clonestagram.user.application.service.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.post.service.SoftDeleteService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileDeletionServiceTest {

	@Mock
	private UserInternalQueryService userInternalQueryService;

	@Mock
	private PostService postService;

	@Mock
	private SoftDeleteService softDeleteService;

	@InjectMocks
	private ProfileDeletionService profileDeletionService;

	private final Long userId = 1L;
	private User mockUser;

	@BeforeEach
	void setUp() {
		mockUser = mock(User.class);
		when(mockUser.getId()).thenReturn(userId);
	}

	@Test
	@DisplayName("정상 케이스: 사용자 프로필 삭제 성공")
	void testDeleteUserProfile_Success() {
		// given: 사용자 조회 정상
		when(userInternalQueryService.findUserById(eq(userId))).thenReturn(mockUser);

		// when: 삭제 실행
		assertDoesNotThrow(() -> profileDeletionService.deleteUserProfile(userId));

		// then: 각 의존성이 정상 호출되었는지 검증
		verify(postService, times(1)).deleteAllUserPosts(eq(userId));
		verify(softDeleteService, times(1)).saveUserSoftDeleteRecord(eq(mockUser));
		verify(userInternalQueryService, times(1)).saveUser(eq(mockUser));
		// 도메인 메서드 user.delete()의 호출 여부 검증
		verify(mockUser, times(1)).delete();
	}

	@Test
	@DisplayName("실패: 게시글 삭제 중 예외 발생 시 예외 전파")
	void testDeleteUserProfile_PostServiceException() {
		// given
		when(userInternalQueryService.findUserById(eq(userId))).thenReturn(mockUser);
		doThrow(new RuntimeException("post deletion failed")).when(postService).deleteAllUserPosts(eq(userId));

		// when & then
		RuntimeException ex = assertThrows(RuntimeException.class, () -> profileDeletionService.deleteUserProfile(userId));
		assertTrue(ex.getMessage().contains("post deletion failed"));
	}

	@Test
	@DisplayName("실패: 사용자 도메인 삭제 처리 중 예외 발생 시 예외 전파")
	void testDeleteUserProfile_UserDeleteException() {
		// given: user.delete()에서 예외 발생
		when(userInternalQueryService.findUserById(eq(userId))).thenReturn(mockUser);
		doThrow(new RuntimeException("user delete failed")).when(mockUser).delete();

		// when & then
		RuntimeException ex = assertThrows(RuntimeException.class, () -> profileDeletionService.deleteUserProfile(userId));
		assertTrue(ex.getMessage().contains("user delete failed"));
	}

	@Test
	@DisplayName("실패: 소프트 삭제 기록 저장 중 예외 발생 시 예외 전파")
	void testDeleteUserProfile_SoftDeleteException() {
		// given
		when(userInternalQueryService.findUserById(eq(userId))).thenReturn(mockUser);
		doThrow(new RuntimeException("soft delete failed")).when(softDeleteService).saveUserSoftDeleteRecord(eq(mockUser));

		// when & then
		RuntimeException ex = assertThrows(RuntimeException.class, () -> profileDeletionService.deleteUserProfile(userId));
		assertTrue(ex.getMessage().contains("soft delete failed"));
	}

	@Test
	@DisplayName("실패: 사용자 저장 중 예외 발생 시 예외 전파")
	void testDeleteUserProfile_SaveUserException() {
		// given
		when(userInternalQueryService.findUserById(eq(userId))).thenReturn(mockUser);
		// 정상 호출: postService, softDeleteService
		doNothing().when(postService).deleteAllUserPosts(eq(userId));
		doNothing().when(softDeleteService).saveUserSoftDeleteRecord(eq(mockUser));
		// 예외 발생: saveUser
		doThrow(new RuntimeException("save user failed")).when(userInternalQueryService).saveUser(eq(mockUser));

		// when & then
		RuntimeException ex = assertThrows(RuntimeException.class, () -> profileDeletionService.deleteUserProfile(userId));
		assertTrue(ex.getMessage().contains("save user failed"));
	}
}
