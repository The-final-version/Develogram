package com.goorm.clonestagram.like.service;

import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.like.domain.Like;
import com.goorm.clonestagram.like.repository.LikeRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
public class LikeServiceTest {

	@Mock
	private LikeRepository likeRepository;

	@Mock
	private UserRepository userRepository;
	@Mock
	private PostsRepository postRepository;

	@Mock
	private UserService userService;

	@Mock
	private PostService postService;

	@InjectMocks
	private LikeService likeService;

	private Users user;
	private Posts post;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		// Test user
		user = new Users();
		user.setId(1L);
		user.setUsername("user1");

		// Test posts
		post = new Posts();
		post.setId(1L);
		post.setContent("Test Post");
	}

	@Test
	public void testToggleLikeAddLike() {
		// Given: User and posts are available, and no existing like in the database.
		when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user);
		when(postService.findByIdAndDeletedIsFalse(1L)).thenReturn(post);
		when(likeRepository.findByUser_IdAndPost_Id(1L, 1L)).thenReturn(Optional.empty());  // No like exists

		// When: User toggles like
		likeService.toggleLike(1L, 1L);

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

		when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(user);
		when(postService.findByIdAndDeletedIsFalse(1L)).thenReturn(post);
		when(likeRepository.findByUser_IdAndPost_Id(1L, 1L)).thenReturn(
			Optional.of(existingLike));  // Like already exists

		// When: User toggles like
		likeService.toggleLike(1L, 1L);

		// Then: The like should be deleted
		verify(likeRepository, never()).save(any(Like.class));
		verify(likeRepository, times(1)).delete(existingLike);
	}

	@Test
	void getLikeCount_should_call_repository_and_return_value() {
		// given
		Long postId = 1L;
		when(likeRepository.countByPost_Id(postId)).thenReturn(3L);

		// when
		Long result = likeService.getLikeCount(postId);

		// then
		assertThat(result).isEqualTo(3L);
		verify(likeRepository, times(1)).countByPost_Id(postId);
	}
}
