package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
public class LikeServiceTest {

	@Mock
	private LikeRepository likeRepository;

	@Mock
	private JpaUserExternalWriteRepository userRepository;
	@Mock
	private PostsRepository postRepository;

	@Mock
	private UserExternalQueryService userService;

	@Mock
	private PostService postService;

	@InjectMocks
	private LikeService likeService;

	private UserEntity user;
	private Posts post;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		// Test user
		user = new UserEntity(User.testMockUser(1L, "user1"));

		// Test posts
		post = new Posts();
		post.setId(100L);
		post.setContent("Test Post");
	}

	@Test
	public void testToggleLikeAddLike() {
		// Given: User and posts are available, and no existing like in the database.
		when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user.toDomain());
		when(postService.findByIdAndDeletedIsFalse(100L)).thenReturn(post);
		when(likeRepository.findByUser_IdAndPost_Id(1L, 100L)).thenReturn(Optional.empty());  // No like exists

		// When: User toggles like
		likeService.toggleLike(1L, 100L);

		// Then: The like should be saved
		verify(likeRepository, times(1)).save(any(Like.class));
		verify(likeRepository, never()).delete(any(Like.class));
	}

	@Test
	public void testToggleLikeRemoveLike() {
		// Given: User and posts are available, and existing like is found in the database.
		Like existingLike = new Like();
		existingLike.setUser(user);
		existingLike.setPost(post);

		when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user.toDomain());
		when(postService.findByIdAndDeletedIsFalse(100L)).thenReturn(post);
		when(likeRepository.findByUser_IdAndPost_Id(1L, 100L)).thenReturn(
			Optional.of(existingLike));  // Like already exists

		// When: User toggles like
		likeService.toggleLike(1L, 100L);

		// Then: The like should be deleted
		verify(likeRepository, never()).save(any(Like.class));
		verify(likeRepository, times(1)).delete(existingLike);
	}

	@Test
	void getLikeCount_should_call_repository_and_return_value() {
		// given
		Long postId = 1L;
		when(postService.existsByIdAndDeletedIsFalse(postId)).thenReturn(true);
		when(likeRepository.findLikeCount(postId)).thenReturn(Optional.of(3L));

		// when
		Long result = likeService.getLikeCount(postId);

		// then
		assertThat(result).isEqualTo(3L);
		verify(likeRepository, times(1)).findLikeCount(postId);
	}

	@Test
	void getLikeCount_post_not_found() {
		// given
		Long postId = 1L;
		when(postService.existsByIdAndDeletedIsFalse(postId)).thenReturn(false);

		// when
		// then
		assertThrows(PostNotFoundException.class, () -> {
			likeService.getLikeCount(postId);
		});
	}

	@Nested
	@DisplayName("특정 유저가 게시글에 좋아요 눌렀는지 여부 조회 테스트")
	class IsPostLikedByLoginUser {
		@Test
		@DisplayName("좋아요가 존재하면 true 반환")
		void should_return_true_when_like_exists() {
			// given
			when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user.toDomain());
			when(postService.findByIdAndDeletedIsFalse(100L)).thenReturn(post);
			when(likeRepository.existsByUser_IdAndPost_Id(1L, 100L)).thenReturn(true);

			// when
			boolean result = likeService.isPostLikedByLoginUser(100L, 1L);

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("좋아요가 존재하지 않으면 false 반환")
		void should_return_false_when_like_does_not_exist() {
			// given
			when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user.toDomain());
			when(postService.findByIdAndDeletedIsFalse(100L)).thenReturn(post);
			when(likeRepository.existsByUser_IdAndPost_Id(1L, 100L)).thenReturn(false);

			// when
			boolean result = likeService.isPostLikedByLoginUser(100L, 1L);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 유저인 경우 UserNotFoundException 발생")
		void should_throw_UserNotFoundException_when_user_not_found() {
			// given
			when(userService.findByIdAndDeletedIsFalse(999L))
				.thenThrow(new UserNotFoundException(999L));

			// when + then
			assertThrows(UserNotFoundException.class, () -> {
				likeService.isPostLikedByLoginUser(100L, 999L);
			});
		}

		@Test
		@DisplayName("존재하지 않는 게시글인 경우 PostNotFoundException 발생")
		void should_throw_PostNotFoundException_when_post_not_found() {
			// given
			when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user.toDomain());
			when(postService.findByIdAndDeletedIsFalse(999L))
				.thenThrow(new PostNotFoundException(999L));

			// when + then
			assertThrows(PostNotFoundException.class, () -> {
				likeService.isPostLikedByLoginUser(999L, 1L);
			});
		}
	}

}
