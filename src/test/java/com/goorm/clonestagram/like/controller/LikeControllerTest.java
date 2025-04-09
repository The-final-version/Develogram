package com.goorm.clonestagram.like.controller;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.like.service.LikeService;
import com.goorm.clonestagram.util.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
public class LikeControllerTest {

	@Mock
	LikeService likeService;

	@InjectMocks
	LikeController likeController;

	@Nested
	@DisplayName("특정 게시물의 좋아요 개수 조회 테스트")
	class GetLikeCountTest {
		@Test
		@DisplayName("포스트 ID로 좋아요 개수 조회에 성공하면 200 ok")
		void success() {
			// given : likeService.getLikeCount를 호출하면 특정 값이 나오도록 세팅
			Long postId = 999L;
			when(likeService.getLikeCount(postId)).thenReturn(123L);

			// when : 호출
			ResponseEntity<Long> result = likeController.getLikeCount(postId);

			// then : 해당 값과 함께 200 ok
			assertThat(result.getStatusCodeValue()).isEqualTo(200);
			assertThat(result.getBody()).isEqualTo(123L);
		}

		@Test
		@DisplayName("없는 포스트 ID로 조회할 경우 404 not found")
		void fail_post_not_found() {
			// given : likeService.getLikeCount를 호출하면 PostNotFoundException 발생하도록 세팅
			Long postId = 999L;
			when(likeService.getLikeCount(postId)).thenThrow(new PostNotFoundException(postId));

			// when
			// then : 호출 시 PostNotFoundException을 캐치해서 404 에러 발생
			PostNotFoundException exception = assertThrows(PostNotFoundException.class, () -> {
				likeController.getLikeCount(postId);
			});
			assertThat(exception.getMessage()).contains("존재하지 않는 게시글입니다. ID: " + postId);
		}
	}

	@Nested
	@DisplayName("좋아요 토글 테스트")
	class ToggleLikeCountTest {
		@Test
		@DisplayName("정상적인 요청일 경우 200 OK를 반환")
		void success_toggle_like() {
			// given
			Long postId = 1L;
			Long userId = 10L;
			CustomUserDetails userDetails = mock(CustomUserDetails.class);
			when(userDetails.getId()).thenReturn(userId);

			// when
			ResponseEntity<Void> response = likeController.toggleLike(postId, userDetails);

			// then
			verify(likeService).toggleLike(userId, postId);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isNull(); // void 응답이므로
		}

		@Test
		@DisplayName("없는 게시글 ID인 경우 PostNotFoundException 발생")
		void should_throw_PostNotFoundException_when_post_not_found() {
			// given
			Long postId = 999L;
			Long userId = 1L;
			CustomUserDetails userDetails = mock(CustomUserDetails.class);
			when(userDetails.getId()).thenReturn(userId);

			doThrow(new PostNotFoundException(postId))
				.when(likeService).toggleLike(userId, postId);

			// when + then
			assertThrows(PostNotFoundException.class, () -> {
				likeController.toggleLike(postId, userDetails);
			});
		}
	}

	@Nested
	@DisplayName("좋아요 여부 조회 테스트")
	class CheckIfLikedTest {
		@Test
		@DisplayName("좋아요가 눌려있으면 true를 반환")
		void liked_should_return_true() {
			// given
			Long postId = 1L;
			Long userId = 10L;
			CustomUserDetails userDetails = mock(CustomUserDetails.class);
			when(userDetails.getId()).thenReturn(userId);
			when(likeService.isPostLikedByLoginUser(postId, userId)).thenReturn(true);

			// when
			ResponseEntity<Boolean> response = likeController.checkIfLiked(postId, userDetails);

			// then
			verify(likeService).isPostLikedByLoginUser(postId, userId);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isTrue();
		}

		@Test
		@DisplayName("좋아요가 없으면 false를 반환")
		void notLiked_should_return_false() {
			// given
			Long postId = 1L;
			Long userId = 20L;
			CustomUserDetails userDetails = mock(CustomUserDetails.class);
			when(userDetails.getId()).thenReturn(userId);
			when(likeService.isPostLikedByLoginUser(postId, userId)).thenReturn(false);

			// when
			ResponseEntity<Boolean> response = likeController.checkIfLiked(postId, userDetails);

			// then
			verify(likeService).isPostLikedByLoginUser(postId, userId);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isFalse();
		}

		@Test
		@DisplayName("없는 게시글 ID인 경우 PostNotFoundException 발생")
		void should_throw_PostNotFoundException_when_post_not_found() {
			// given
			Long postId = 999L;
			Long userId = 1L;
			CustomUserDetails userDetails = mock(CustomUserDetails.class);
			when(userDetails.getId()).thenReturn(userId);

			when(likeService.isPostLikedByLoginUser(postId, userId))
				.thenThrow(new PostNotFoundException(postId));

			// when + then
			assertThrows(PostNotFoundException.class, () -> {
				likeController.checkIfLiked(postId, userDetails);
			});
		}
	}

}
