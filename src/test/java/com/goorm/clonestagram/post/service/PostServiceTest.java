package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.goorm.clonestagram.exception.PostNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

	@Mock
	private PostsRepository postsRepository;

	@Mock
	private JpaUserExternalWriteRepository userRepository;

	@Mock
	private UserExternalQueryService userService;

	@InjectMocks
	private PostService postService;

	private UserEntity testUser;
	private Posts testPost;

	@BeforeEach
	void setUp() {
		testUser = new UserEntity(User.testMockUser(1L, "testUser"));

		testPost = Posts.builder()
			.id(1L)
			.content("테스트 게시물")
			.user(testUser)
			.contentType(ContentType.IMAGE)
			.mediaName("test-image.jpg")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
	}

	@Test
	void 게시물_저장_성공() {
		// given
		when(postsRepository.save(any(Posts.class))).thenReturn(testPost);

		// when
		Posts result = postService.save(testPost);

		// then
		assertNotNull(result);
		assertEquals(testPost.getContent(), result.getContent());
		assertEquals(testPost.getContentType(), result.getContentType());
		assertEquals(testPost.getMediaName(), result.getMediaName());
		verify(postsRepository).save(any(Posts.class));
	}

	@Test
	void 게시물_조회_성공() {
		// given
		when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));

		// when
		Posts result = postService.findByIdAndDeletedIsFalse(1L);

		// then
		assertNotNull(result);
		assertEquals(testPost.getContent(), result.getContent());
		assertEquals(testPost.getContentType(), result.getContentType());
		assertEquals(testPost.getMediaName(), result.getMediaName());
	}

	@Test
	void 게시물_조회_실패_게시물없음() {
		// given
		when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.empty());

		// when & then
		assertThrows(PostNotFoundException.class, () -> postService.findByIdAndDeletedIsFalse(1L));
	}

	@Test
	void 게시물_조회_성공_from포함() {
		// given
		String from = "testSource";
		when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));

		// when
		Posts result = postService.findByIdAndDeletedIsFalse(1L, from);

		// then
		assertNotNull(result);
		assertEquals(testPost.getContent(), result.getContent());
		verify(postsRepository).findByIdAndDeletedIsFalse(1L);
	}

	@Test
	void 게시물_조회_실패_from포함_게시물없음() {
		// given
		String from = "testSource";
		when(postsRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.empty()); // 수정
		//when(postsRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.empty()); // 수정

		// when & then
		PostNotFoundException exception = assertThrows(PostNotFoundException.class,
			() -> postService.findByIdAndDeletedIsFalse(1L, from));
		assertTrue(exception.getMessage().contains("댓글이 속한 게시글이 존재하지 않습니다. postId: 1, from: " + from));
		verify(postsRepository).findByIdAndDeletedIsFalse(1L);
		//verify(postsRepository).findByIdWithPessimisticLock(1L);
	}

	@Test
	void 게시물_존재여부_확인_성공() {
		// given
		when(postsRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(true);

		// when
		boolean result = postService.existsByIdAndDeletedIsFalse(1L);

		// then
		assertTrue(result);
		verify(postsRepository).existsByIdAndDeletedIsFalse(1L);
	}

	@Test
	void 게시물_존재여부_확인_실패() {
		// given
		when(postsRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(false);

		// when
		boolean result = postService.existsByIdAndDeletedIsFalse(1L);

		// then
		assertFalse(result);
		verify(postsRepository).existsByIdAndDeletedIsFalse(1L);
	}

	@Test
	void 사용자의_게시물_목록_조회_성공() {
		// given
		List<Posts> posts = Arrays.asList(testPost);
		when(postsRepository.findAllByUserIdAndDeletedIsFalse(anyLong())).thenReturn(posts);

		// when
		List<Posts> result = postService.findAllByUserIdAndDeletedIsFalse(1L);

		// then
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.size());
		assertEquals(testPost.getContent(), result.get(0).getContent());
		verify(postsRepository).findAllByUserIdAndDeletedIsFalse(1L);
	}

	@Test
	void 사용자의_게시물_목록_조회_실패_게시물없음() {
		// given
		when(postsRepository.findAllByUserIdAndDeletedIsFalse(anyLong())).thenReturn(Arrays.asList());

		// when
		List<Posts> result = postService.findAllByUserIdAndDeletedIsFalse(1L);

		// then
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(postsRepository).findAllByUserIdAndDeletedIsFalse(1L);
	}

	@Test
	void 내_게시물_목록_조회_성공() {
		// given
		Long userId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		List<Posts> postList = Arrays.asList(testPost);
		Page<Posts> postPage = new PageImpl<>(postList, pageable, postList.size());

		when(userService.findByIdAndDeletedIsFalse(userId)).thenReturn(testUser.toDomain());
		when(postsRepository.findAllByUserIdAndDeletedIsFalse(userId, pageable)).thenReturn(postPage);

		// when
		com.goorm.clonestagram.post.dto.PostResDto result = postService.getMyPosts(userId, pageable);

		// then
		assertNotNull(result);
		assertNotNull(result.getUser());
		assertEquals(testUser.getName(), result.getUser().getName());
		assertNotNull(result.getFeed());
		assertEquals(1, result.getFeed().getTotalElements());
		assertEquals(testPost.getContent(), result.getFeed().getContent().get(0).getContent());

		verify(userService).findByIdAndDeletedIsFalse(userId);
		verify(postsRepository).findAllByUserIdAndDeletedIsFalse(userId, pageable);
	}

	@Test
	void 내_게시물_목록_조회_실패_사용자없음() {
		// given
		Long userId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		when(userService.findByIdAndDeletedIsFalse(userId)).thenThrow(
			new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

		// when & then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> postService.getMyPosts(userId, pageable));

		assertEquals("해당 유저를 찾을 수 없습니다.", exception.getMessage());
		verify(userService).findByIdAndDeletedIsFalse(userId);
		verify(postsRepository, never()).findAllByUserIdAndDeletedIsFalse(anyLong(), any(Pageable.class));
	}

	@Test
	void saveAndFlush_성공() {
		// given
		when(postsRepository.saveAndFlush(any(Posts.class))).thenReturn(testPost);

		// when
		Posts result = postService.saveAndFlush(testPost);

		// then
		assertNotNull(result);
		assertEquals(testPost.getId(), result.getId());
		verify(postsRepository).saveAndFlush(testPost);
	}
}
