package com.goorm.clonestagram.comment.service;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.mapper.CommentMapper;
import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.exception.UnauthorizedCommentAccessException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.goorm.clonestagram.user.service.UserService;
import com.goorm.clonestagram.util.CustomTestLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CustomTestLogger.class, OutputCaptureExtension.class})
class CommentServiceTest {


	@Mock
	private CommentRepository commentRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PostsRepository postRepository;

	@Mock
	private UserService userService;

	@Mock
	private PostService postService;

	private List<Comments> testComments;

	@InjectMocks
	private CommentService commentService;

	private Users mockUsers;
	private Comments testComment;
	private Posts mockPost;
	private Posts mockPost2;

	@BeforeEach
	void setUp() {
		mockUsers = Users.builder()
			.id(11L)
			.username("mockuser")
			.password("mockpassword")
			.email("mock@domain.com")
			.build();

		mockPost = Posts.builder()
			.id(111L)
			.user(mockUsers)
			.content("Test Post 1")
			.mediaName("test.jpg")
			.contentType(ContentType.IMAGE)
			.build();

		mockPost2 = Posts.builder()
			.id(222L)
			.user(mockUsers)
			.content("Test Post 2")
			.mediaName("test.jpg")
			.contentType(ContentType.IMAGE)
			.build();

		testComment = Comments.builder()
			.id(1111L)
			.users(mockUsers)
			.posts(mockPost)
			.content("Test Comment")
			.build();

		testComments = Arrays.asList(
			Comments.builder()
				.id(2222L)
				.posts(mockPost)
				.users(mockUsers)
				.content("첫 번째 댓글")
				.build(),
			Comments.builder()
				.id(3333L)
				.posts(mockPost2)
				.users(mockUsers)
				.content("두 번째 댓글")
				.build()
		);
	}

	@Nested
	@DisplayName("댓글 생성 테스트")
	class CreateCommentTest {

		@Test
		void createComment_ShouldSaveComment_WhenValidInputGiven() {
			// Given
			CommentRequest request = new CommentRequest(11L, 111L, "Test Comment");

			when(userService.findByIdAndDeletedIsFalse(11L)).thenReturn(mockUsers);
			when(postService.findByIdAndDeletedIsFalse(111L)).thenReturn(mockPost);

			ArgumentCaptor<Comments> captor = ArgumentCaptor.forClass(Comments.class);
			Comments dummySavedComment = testComment;
			when(commentRepository.save(any(Comments.class))).thenReturn(dummySavedComment);

			// When
			Comments result = commentService.createComment(request);

			// Then
			verify(commentRepository, times(1)).save(captor.capture()); // Repository 호출 확인
			Comments captured = captor.getValue();

			assertEquals("Test Comment", captured.getContent()); // 매핑 검증
			assertEquals(mockUsers, captured.getUsers());
			assertEquals(mockPost, captured.getPosts());

			assertEquals(dummySavedComment.getId(), result.getId()); // 반환값 확인
		}

		@Test
		@DisplayName("존재하지 않는 userId로 댓글 작성 테스트")
		void createComment_ShouldThrowException_WhenUserDoesNotExist() {
			// Given: 모든 userId에 대해 false 반환
			CommentRequest request = new CommentRequest(11L, 111L, "Test Comment");
			when(userService.findByIdAndDeletedIsFalse(anyLong())).thenThrow(new UserNotFoundException(11L));

			// When & Then: 예외 발생 여부 확인
			Exception exception = assertThrows(UserNotFoundException.class,
				() -> commentService.createComment(request));

			// 예외 메시지 검증
			assertTrue(exception.getMessage().contains("존재하지 않는 사용자입니다. ID: 11"));

			// Verify: userRepository.existsById()가 1번 호출되었는지 확인
			verify(userService, times(1)).findByIdAndDeletedIsFalse(anyLong());
			// Verify: commentRepository.save()가 호출되지 않았는지 확인
			verify(commentRepository, never()).save(any(Comments.class));
		}

		@Test
		@DisplayName("존재하지 않는 postId로 댓글 작성 테스트")
		void createComment_ShouldThrowException_WhenPostDoesNotExist() {
			// Given: 특정 postId (mockComment의 postId)에 대해 false 반환 (게시글이 존재하지 않도록 설정)
			CommentRequest request = new CommentRequest(11L, 111L, "Test Comment");
			when(userService.findByIdAndDeletedIsFalse(anyLong())).thenReturn(mockUsers);
			when(postService.findByIdAndDeletedIsFalse(testComment.getPosts().getId()))
				.thenThrow(new IllegalArgumentException("게시물이 없습니다."));

			// When & Then: 예외 발생 여부 확인
			Exception exception = assertThrows(IllegalArgumentException.class,
				() -> commentService.createComment(request));

			// 예외 메시지 검증
			assertTrue(exception.getMessage().contains("게시물이 없습니다."));

			// Verify: postRepository.existsById()가 1번 호출되었는지 확인
			verify(postService, times(1)).findByIdAndDeletedIsFalse(testComment.getPosts().getId());
			// Verify: commentRepository.save()가 호출되지 않았는지 확인
			verify(commentRepository, never()).save(any(Comments.class));
		}
	}

	@Nested
	@DisplayName("댓글 ID로 조회 테스트")
	class GetCommentByIdTest {

		@Test
		@DisplayName("댓글 ID로 조회에 성공하면 댓글 객체를 반환한다.")
		void getCommentById_ShouldReturnComment_WhenCommentExists() {
			// Given: 특정 ID로 조회할 때 mockComment 반환
			when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));

			// When: 댓글을 ID로 조회
			Comments foundComment = commentService.getCommentById(1L);

			// Then: 댓글이 정상적으로 조회되는지 확인
			assertThat(foundComment).isNotNull();
			assertThat(foundComment.getId()).isEqualTo(1111L);
			assertThat(foundComment.getContent()).isEqualTo("Test Comment");

			// Verify: commentRepository.findById()가 1번 호출되었는지 확인
			verify(commentRepository, times(1)).findById(1L);
		}

		@Test
		@DisplayName("존재하지 않는 댓글을 조회할 때 예외 발생")
		void getCommentById_ShouldThrowException_WhenCommentNotFound() {
			// Given: 해당 ID의 댓글이 존재하지 않도록 설정
			when(commentRepository.findById(1111L)).thenReturn(Optional.empty());

			// When & Then: 예외 메시지도 함께 검증
			Exception exception = assertThrows(CommentNotFoundException.class,
				() -> commentService.getCommentById(1111L));

			// 예외 메시지 검증
			assertThat(exception.getMessage()).isEqualTo("존재하지 않는 댓글입니다. ID: 1111");

			// Verify: findById()가 1번 호출되었는지 확인
			verify(commentRepository, times(1)).findById(1111L);
		}

	}

	@Nested
	@DisplayName("포스트 ID로 조회 테스트")
	class GetCommentByPostIdTest {

		@Test
		@DisplayName("postId로 모든 댓글을 조회에 성공하면 댓글 목록을 반환한다.")
		void getCommentsByPostId_ShouldReturnListOfComments() {
			// Given: postId=111에 대한 댓글 목록을 반환하도록 설정
			when(postService.existsByIdAndDeletedIsFalse(111L)).thenReturn(true);
			when(commentRepository.findByPosts_Id(111L)).thenReturn(testComments);

			// When: postId=111으로 댓글 목록 조회
			List<Comments> comments = commentService.getCommentsByPostId(111L);

			// Then: 반환된 리스트가 예상대로 존재하는지 확인
			assertThat(comments).isNotNull();
			assertThat(comments.size()).isEqualTo(2);
			assertThat(comments.get(0).getContent()).isEqualTo("첫 번째 댓글");
			assertThat(comments.get(1).getContent()).isEqualTo("두 번째 댓글");

			// Verify: commentRepository.findByPostId()가 1번 호출되었는지 확인
			verify(commentRepository, times(1)).findByPosts_Id(111L);
		}

		@Test
		@DisplayName("postId로 조회했는데 댓글이 없는 경우 메세지 로그를 띄우며 빈 목록을 반환한다.")
		void getCommentsByPostId_ShouldThrowException_WhenNoCommentsExist(CapturedOutput output) {
			// Given: postId=111은 존재하지만, 해당 게시글에 댓글이 없음
			when(postService.existsByIdAndDeletedIsFalse(111L)).thenReturn(true);
			when(commentRepository.findByPosts_Id(111L)).thenReturn(Collections.emptyList());

			// When
			commentService.getCommentsByPostId(111L);

			// Then
			assertThat(output).contains("해당 포스트에 댓글이 없습니다. postId: 111");

			// Verify: commentRepository.findByPostId()가 1번 호출되었는지 확인
			verify(commentRepository, times(1)).findByPosts_Id(111L);
		}

		@Test
		@DisplayName("없는 postId로 조회했을 때 예외가 발생한다.")
		void getCommentsByPostId_ShouldThrowException_WhenPostDoesNotExist() {
			// Given: postId=999L는 존재하지 않음
			when(postService.existsByIdAndDeletedIsFalse(999L)).thenReturn(false);

			// When & Then: 예외 발생 여부 확인
			Exception exception = assertThrows(PostNotFoundException.class,
				() -> commentService.getCommentsByPostId(999L));

			// 예외 메시지 검증
			assertThat(exception.getMessage()).isEqualTo("존재하지 않는 게시글입니다. ID: " + 999L);

			// Verify: postRepository.existsById()가 1번 호출되었는지 확인
			verify(postService, times(1)).existsByIdAndDeletedIsFalse(999L);
			// Verify: commentRepository.findByPostId()가 호출되지 않았는지 확인
			verify(commentRepository, never()).findByPosts_Id(anyLong());
		}
	}

	@Nested
	@DisplayName("댓글 삭제 테스트")
	class RemoveCommentTest {

		@Test
		@DisplayName("댓글 작성자가 삭제하는 경우 (정상 삭제)")
		void removeComment_ShouldDeleteComment_WhenRequesterIsCommentOwner() {
			// 댓글 ID와 요청자 ID를 인수로 받음.
			// 댓글 ID를 DB에서 검색하여 댓글 작성자 ID를 찾아오며, 요청자 ID과 비교하여 삭제하는 메서드.
			// 테스트를 위해서 댓글 ID로 검색했을 때 요청자의 ID가 나오도록 설정
			// 그리고 그 전에 commentRepository.findById에서 빈 결과가 나오면 안됨.

			when(commentRepository.findById(1111L)).thenReturn(Optional.of(testComment));
			when(postService.findByIdAndDeletedIsFalse(111L, "Comment")).thenReturn(mockPost);

			// When: 댓글 삭제 요청
			commentService.removeComment(1111L, 11L);

			// Then: 댓글 삭제가 정상적으로 수행됨
			verify(commentRepository, times(1)).deleteById(1111L);
		}

		@Test
		@DisplayName("게시글 작성자가 댓글을 삭제하는 경우 (정상 삭제)")
		void removeComment_ShouldDeleteComment_WhenRequesterIsPostOwner() {
			// Given: 댓글과 게시글이 존재하고, 요청자가 게시글 작성자임
			Users postWriter = Users.builder().id(5L).build();
			Users commentWriter = Users.builder().id(11L).build();
			Posts testPost = Posts.builder().id(111L).user(postWriter).build();
			Comments testComment = Comments.builder().id(1111L).users(commentWriter).posts(testPost).build();

			when(commentRepository.findById(1111L)).thenReturn(Optional.of(testComment));
			when(postService.findByIdAndDeletedIsFalse(111L, "Comment")).thenReturn(testPost);

			// When: 게시글 작성자가 댓글 삭제 요청
			commentService.removeComment(1111L, 5L);

			// Then: 댓글 삭제가 정상적으로 수행됨
			verify(commentRepository, times(1)).deleteById(1111L);
		}

		@Test
		@DisplayName("권한이 없는 사용자가 삭제하려고 하면 예외 발생")
		void removeComment_ShouldThrowException_WhenRequesterHasNoPermission(CapturedOutput output) {
			// Given: 댓글과 게시글이 존재하지만 요청자가 댓글/게시글 작성자가 아님
			Users postWriter = Users.builder().id(5L).build();
			Users commentWriter = Users.builder().id(11L).build();
			Posts testPost = Posts.builder().id(111L).user(postWriter).build();
			Comments testComment = Comments.builder().id(1111L).users(commentWriter).posts(testPost).build();

			when(commentRepository.findById(1111L)).thenReturn(Optional.of(testComment));
			when(postService.findByIdAndDeletedIsFalse(111L, "Comment")).thenReturn(testPost);

			// When & Then: 예외 발생 확인 (잘못된 요청자 ID: 999L)
			Exception exception = assertThrows(UnauthorizedCommentAccessException.class, () -> {
				commentService.removeComment(1111L, 22L);
			});

			// 예외 메시지 검증
			assertTrue(exception.getMessage().contains("댓글을 삭제할 권한이 없습니다."));

			// 댓글 삭제가 호출되지 않아야 함
			verify(commentRepository, never()).deleteById(1L);
		}

		@Test
		@DisplayName("존재하지 않는 댓글을 삭제하려고 하면 예외 발생")
		void removeComment_ShouldThrowException_WhenCommentDoesNotExist() {
			// Given: 댓글이 존재하지 않음
			when(commentRepository.findById(999L)).thenReturn(Optional.empty());

			// When & Then: 예외 발생 확인
			Exception exception = assertThrows(CommentNotFoundException.class, () -> {
				commentService.removeComment(999L, 5L);
			});

			// 예외 메시지 검증
			assertTrue(exception.getMessage().contains("존재하지 않는 댓글입니다. ID: " + 999));

			// 댓글 삭제가 호출되지 않아야 함
			verify(commentRepository, never()).deleteById(999L);
		}
	}

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostsRepository postRepository;

    private List<CommentEntity> mockComments;

    @InjectMocks
    private CommentService commentService;

    private CommentEntity mockComment;

    private Posts mockPost;
    private Posts mockPost2;


    @BeforeEach
    void setUp() {
        Users mockUsers = Users.builder()
                .id(5L)
                .username("mockuser")
                .password("mockpassword")
                .email("mock@domain.com")
                .build();

        mockPost = Posts.builder()
                .id(200L)
                .user(mockUsers)
                .content("Test Post")
                .mediaName("test.jpg")
                .contentType(ContentType.IMAGE)
                .build();

        mockPost2 = Posts.builder()
                .id(100L)
                .user(mockUsers)
                .content("Test Post")
                .mediaName("test.jpg")
                .contentType(ContentType.IMAGE)
                .build();

        mockComment = CommentEntity.builder()
                .id(1L)
                .users(mockUsers)
                .posts(mockPost)
                .content("Test Comment")
                .build();

        mockComments = Arrays.asList(
                CommentEntity.builder()
                        .id(1L)
                        .posts(mockPost2)
                        .users(mockUsers)
                        .content("첫 번째 댓글")
                        .build(),
                CommentEntity.builder()
                        .id(2L)
                        .posts(mockPost2)
                        .users(mockUsers)
                        .content("두 번째 댓글")
                        .build()
        );



    }

    /**
     * ✅ 댓글을 성공적으로 저장하는 테스트
     */
    @Test
    void createComment_ShouldSaveComment() {
        // Given: commentRepository.save()가 실행될 때 mockComment를 반환하도록 설정
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(mockComment);
        when(userRepository.findByIdAndDeletedIsFalse(5L)).thenReturn(Optional.of(mockComment.getUsers()));
        when(postRepository.findByIdAndDeletedIsFalse(100L)).thenReturn(Optional.of(mockComment.getPosts()));

        CommentRequest commentRequest = new CommentRequest(5L, 100L, "Test Comment");

        // When: 새로운 댓글을 생성
        CommentEntity savedComment = commentService.createCommentWithRollback(commentRequest);

        // Then: 저장된 댓글이 예상대로 반환되는지 확인
        assertNotNull(savedComment);
        assertEquals(mockComment.getId(), savedComment.getId());
        assertEquals(mockComment.getContent(), savedComment.getContent());

        // Verify: commentRepository.save()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).save(any(CommentEntity.class));
    }

    /**
     * ✅ 존재하지 않는 userId로 댓글 작성 테스트
     */
    @Test
    void createComment_ShouldThrowException_WhenUserDoesNotExist() {
        // Given: 모든 userId에 대해 false 반환
        when(userRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(false);

        // When & Then: 예외 발생 여부 확인
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.createComment(mockComment));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("존재하지 않는 사용자 ID입니다"));

        // Verify: userRepository.existsById()가 1번 호출되었는지 확인
        verify(userRepository, times(1)).existsByIdAndDeletedIsFalse(anyLong());
        // Verify: commentRepository.save()가 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(CommentEntity.class));
    }

    /**
     * ✅ 존재하지 않는 postId로 댓글 작성 테스트
     */
    @Test
    void createComment_ShouldThrowException_WhenPostDoesNotExist() {
        // Given: 특정 postId (mockComment의 postId)에 대해 false 반환 (게시글이 존재하지 않도록 설정)
        when(userRepository.existsByIdAndDeletedIsFalse(anyLong())).thenReturn(true);
        when(postRepository.existsByIdAndDeletedIsFalse(mockComment.getPosts().getId())).thenReturn(false);

        // When & Then: 예외 발생 여부 확인
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.createComment(mockComment));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("존재하지 않는 게시글 ID입니다"));

        // Verify: postRepository.existsById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).existsByIdAndDeletedIsFalse(mockComment.getPosts().getId());
        // Verify: commentRepository.save()가 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(CommentEntity.class));
    }


    /**
     * ✅ 존재하는 댓글을 조회하는 테스트
     */
    @Test
    void getCommentById_ShouldReturnComment_WhenCommentExists() {
        // Given: 특정 ID로 조회할 때 mockComment 반환
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));

        // When: 댓글을 ID로 조회
        CommentEntity foundComment = commentService.getCommentById(1L);

        // Then: 댓글이 정상적으로 조회되는지 확인
        assertNotNull(foundComment);
        assertEquals(1L, foundComment.getId());
        assertEquals("Test Comment", foundComment.getContent());

        // Verify: commentRepository.findById()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findById(1L);
    }


    /**
     * ✅ 존재하는 댓글을 조회하는 테스트
     */
    @Test
    void getCommentByPostId_ShouldReturnComment_WhenCommentExists() {
        // Given: 특정 ID로 조회할 때 mockComment 반환
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));

        // When: 댓글을 ID로 조회
        CommentEntity foundComment = commentService.getCommentById(1L);

        // Then: 댓글이 정상적으로 조회되는지 확인
        assertNotNull(foundComment);
        assertEquals(1L, foundComment.getId());
        assertEquals("Test Comment", foundComment.getContent());

        // Verify: commentRepository.findById()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findById(1L);
    }


    /**
     * ✅ 존재하지 않는 댓글을 조회할 때 예외 발생하는지 테스트
     */
    @Test
    void getCommentById_ShouldThrowException_WhenCommentNotFound() {
        // Given: 해당 ID의 댓글이 존재하지 않도록 설정
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then: 예외 메시지도 함께 검증
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.getCommentById(999L));

        // 예외 메시지 검증
        String expectedMessage = "해당 댓글이 존재하지 않습니다: 999";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

        // Verify: findById()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findById(999L);
    }

    /**
     * ✅ postId로 모든 댓글을 조회할 때 리스트가 반환되는지 테스트
     */
    @Test
    void getCommentsByPostId_ShouldReturnListOfComments() {
        when(postRepository.existsByIdAndDeletedIsFalse(100L)).thenReturn(true);

        // Given: postId=100에 대한 댓글 목록을 반환하도록 설정
        when(commentRepository.findByPostsId(100L)).thenReturn(mockComments);

        // When: postId=100으로 댓글 목록 조회
        List<CommentEntity> comments = commentService.getCommentsByPostId(100L);

        // Then: 반환된 리스트가 예상대로 존재하는지 확인
        assertNotNull(comments);
        assertEquals(2, comments.size()); // 댓글 2개여야 함
        assertEquals("첫 번째 댓글", comments.get(0).getContent());
        assertEquals("두 번째 댓글", comments.get(1).getContent());

        // Verify: commentRepository.findByPostId()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findByPostsId(100L);
    }

    @Test
    void getCommentsByPostId_ShouldThrowException_WhenPostDoesNotExist() {
        // Given: postId=999L는 존재하지 않음
        when(postRepository.existsByIdAndDeletedIsFalse(999L)).thenReturn(false);

        // When & Then: 예외 발생 여부 확인
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.getCommentsByPostId(999L));

        System.out.println("🚨 발생한 예외 메시지: " + exception.getMessage());

        // 예외 메시지 검증
        assertEquals("존재하지 않는 게시글 ID입니다: 999", exception.getMessage());

        // Verify: postRepository.existsById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).existsByIdAndDeletedIsFalse(999L);
        // Verify: commentRepository.findByPostId()가 호출되지 않았는지 확인
        verify(commentRepository, never()).findByPostsId(anyLong());
    }

    @Test
    void getCommentsByPostId_ShouldThrowException_WhenNoCommentsExist() {
        // Given: postId=100은 존재하지만, 해당 게시글에 댓글이 없음
        when(postRepository.existsByIdAndDeletedIsFalse(100L)).thenReturn(true);
        when(commentRepository.findByPostsId(100L)).thenReturn(Collections.emptyList());

        // When & Then: 예외 발생 여부 확인
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> commentService.getCommentsByPostId(100L));

        System.out.println("🚨 발생한 예외 메시지: " + exception.getMessage());

        // 예외 메시지 검증
        assertEquals("해당 게시글(100)에는 댓글이 없습니다.", exception.getMessage());

        // Verify: postRepository.existsById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).existsByIdAndDeletedIsFalse(100L);
        // Verify: commentRepository.findByPostId()가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findByPostsId(100L);
    }


    /**
     * ✅ 댓글 작성자가 삭제하는 경우 (정상 삭제)
     */
    @Test
    void removeComment_ShouldDeleteComment_WhenRequesterIsCommentOwner() {
        // ✅ doReturn().when() 사용하여 더 유연한 stubbing 적용
        doReturn(Optional.of(mockComment)).when(commentRepository).findById(1L);
        doReturn(Optional.of(mockPost)).when(postRepository).findByIdAndDeletedIsFalse(200L);

        // When: 댓글 삭제 요청
        commentService.removeComment(1L, 5L);

        // Then: 댓글 삭제가 정상적으로 수행됨
        verify(commentRepository, times(1)).deleteById(1L);
    }


    /**
     * ✅ 게시글 작성자가 댓글을 삭제하는 경우 (정상 삭제)
     */
    @Test
    void removeComment_ShouldDeleteComment_WhenRequesterIsPostOwner() {
        // Given: 댓글과 게시글이 존재하고, 요청자가 게시글 작성자임
        doReturn(Optional.of(mockComment)).when(commentRepository).findById(1L);
        doReturn(Optional.of(mockPost)).when(postRepository).findByIdAndDeletedIsFalse(200L);


        // When: 게시글 작성자가 댓글 삭제 요청
        commentService.removeComment(1L, 5L);

        // Then: 댓글 삭제가 정상적으로 수행됨
        verify(commentRepository, times(1)).deleteById(1L);
    }

    /**
     * ✅ 권한이 없는 사용자가 삭제하려고 하면 예외 발생
     */
    @Test
    void removeComment_ShouldThrowException_WhenRequesterHasNoPermission() {
        // Given: 댓글과 게시글이 존재하지만 요청자가 댓글/게시글 작성자가 아님
        doReturn(Optional.of(mockComment)).when(commentRepository).findById(1L);
        doReturn(Optional.of(mockPost)).when(postRepository).findByIdAndDeletedIsFalse(200L);

        // When & Then: 예외 발생 확인 (잘못된 요청자 ID: 999L)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            commentService.removeComment(1L, 999L);
        });
        System.out.println("예외 메시지: " + exception.getMessage());


        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("댓글을 삭제할 권한이 없습니다."));

        // 댓글 삭제가 호출되지 않아야 함
        verify(commentRepository, never()).deleteById(1L);
    }

    /**
     * ✅ 존재하지 않는 댓글을 삭제하려고 하면 예외 발생
     */
    @Test
    void removeComment_ShouldThrowException_WhenCommentDoesNotExist() {
        // Given: 댓글이 존재하지 않음
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then: 예외 발생 확인
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            commentService.removeComment(999L, 5L);
        });

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("존재하지 않는 댓글 ID입니다"));

        // 댓글 삭제가 호출되지 않아야 함
        verify(commentRepository, never()).deleteById(999L);
    }
}
