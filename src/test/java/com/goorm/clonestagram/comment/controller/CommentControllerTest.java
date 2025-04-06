package com.goorm.clonestagram.comment.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.dto.CommentResponse;
import com.goorm.clonestagram.comment.service.CommentService;
import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.InvalidCommentException;
import com.goorm.clonestagram.exception.PermissionDeniedException;
import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.domain.Users;

@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {
	@Mock
	private CommentService commentService;

	@InjectMocks
	CommentController commentController;

	// 댓글 ID로 조회: 성공, 실패 - 없는ID
	@Nested
	@DisplayName("댓글 ID로 조회 테스트")
	class GetCommentByIdTest {

		@Test
		@DisplayName("댓글 조회에 성공하면 정상적으로 객체가 반환된다.")
		void success() {
			// given
			Users user = new Users();
			user.setId(100L);

			Posts post = new Posts();
			post.setId(200L);

			Comments comment = new Comments();
			comment.setId(1L);
			comment.setUsers(user);
			comment.setPosts(post);
			comment.setContent("댓글 내용");
			comment.setCreatedAt(LocalDateTime.now());

			when(commentService.getCommentById(1L)).thenReturn(comment);

			// when
			CommentResponse response = commentController.getCommentById(1L);

			// then
			assertEquals(1L, response.getId());
			assertEquals("댓글 내용", response.getContent());
		}

		@Test
		@DisplayName("없는 ID로 조회하면 404 발생")
		void fail_notFound() {
			// given
			Long id = 999L;
			when(commentService.getCommentById(id)).thenThrow(new CommentNotFoundException(id));

			// when + then
			assertThrows(CommentNotFoundException.class, () -> {
				commentController.getCommentById(id);
			});
		}

	}

	@Nested
	@DisplayName("포스트 ID로 조회 테스트")
	class GetCommentByPostIdTest {
		@Test
		@DisplayName("댓글 조회에 성공하면 해당 포스트에 달린 댓글의 리스트가 반환된다.")
		void success() {
			// given
			Long postId = 10L;

			Users user = new Users();
			user.setId(100L);

			Posts post = new Posts();
			post.setId(postId);

			Comments comment = new Comments();
			comment.setId(1L);
			comment.setUsers(user);
			comment.setPosts(post);
			comment.setContent("내용");
			comment.setCreatedAt(LocalDateTime.now());

			when(commentService.getCommentsByPostId(postId)).thenReturn(List.of(comment));

			// when
			List<CommentResponse> responses = commentController.getCommentsByPostId(postId);

			// then
			assertEquals(1, responses.size());
			assertEquals("내용", responses.get(0).getContent());
			assertEquals(100L, responses.get(0).getUserId());
		}

		@Test
		@DisplayName("해당 포스트에 댓글이 없는 경우 빈 리스트가 반환된다.")
		void success_empty_list() {
			// given
			when(commentService.getCommentsByPostId(anyLong())).thenReturn(Collections.emptyList());

			// when
			List<CommentResponse> result = commentController.getCommentsByPostId(100L);

			// then
			assertTrue(result.isEmpty());
		}

		@Test
		@DisplayName("없는 포스트 ID로 조회하면 404 발생")
		void fail_not_found() {
			// given
			Long postId = 999L;
			when(commentService.getCommentsByPostId(postId)).thenThrow(new PostNotFoundException(postId));

			// when + then
			assertThrows(PostNotFoundException.class, () -> {
				commentController.getCommentsByPostId(postId);
			});
		}
	}

	@Nested
	@DisplayName("댓글 저장 테스트")
	class CreateTest {
		@Test
		@DisplayName("댓글 저장에 성공하면 정상적으로 객체가 반환된다.")
		void success() throws Exception {
			// given
			CommentRequest request = new CommentRequest(1L, 2L, "댓글");

			Users user = new Users();
			user.setId(1L);

			Posts post = new Posts();
			post.setId(2L);

			Comments saved = new Comments();
			saved.setId(10L);
			saved.setUsers(user);
			saved.setPosts(post);
			saved.setContent("댓글");
			saved.setCreatedAt(LocalDateTime.now());

			when(commentService.createComment(request)).thenReturn(saved);

			// when
			CommentResponse response = commentController.create(request);

			// then
			assertEquals(10L, response.getId());
			assertEquals("댓글", response.getContent());
			assertEquals(1L, response.getUserId());
			assertEquals(2L, response.getPostId());
		}

		@Test
		@DisplayName("없는 사용자인 경우 예외 발생")
		void fail_user_not_found() throws Exception {
			CommentRequest request = new CommentRequest(999L, 2L, "내용");
			when(commentService.createComment(request)).thenThrow(new UserNotFoundException(999L));

			assertThrows(UserNotFoundException.class, () -> {
				commentController.create(request);
			});
		}

		@Test
		@DisplayName("없는 포스트인 경우 예외 발생")
		void fail_post_not_found() {
			CommentRequest request = new CommentRequest(1L, 999L, "내용");
			when(commentService.createComment(request)).thenThrow(new PostNotFoundException(999L));

			assertThrows(PostNotFoundException.class, () -> {
				commentController.create(request);
			});
		}

		@Test
		@DisplayName("내용이 없는 댓글인 경우 예외 발생")
		void fail_no_contents() {
			CommentRequest request = new CommentRequest(1L, 2L, ""); // or null

			when(commentService.createComment(request)).thenThrow(new InvalidCommentException("내용이 없습니다"));

			assertThrows(InvalidCommentException.class, () -> {
				commentController.create(request);
			});
		}
	}

	@Nested
	@DisplayName("댓글 삭제 테스트")
	class DeleteCommentTest {
		@Test
		@DisplayName("댓글 작성자가 댓글 삭제를 시도하는 경우 정상적으로 실행된다.")
		void success_comment_writer() {
			Long commentId = 1L;
			Long userId = 100L;

			doNothing().when(commentService).removeComment(commentId, userId);

			String result = commentController.deleteComment(commentId, userId);

			assertEquals("댓글이 삭제되었습니다. ID: " + commentId, result);
		}

		@Test
		@DisplayName("포스트 작성자가 댓글 삭제를 시도하는 경우 정상적으로 실행된다.")
		void success_post_writer() {
			Long commentId = 1L;
			Long postOwnerId = 200L;

			doNothing().when(commentService).removeComment(commentId, postOwnerId);

			String result = commentController.deleteComment(commentId, postOwnerId);

			assertEquals("댓글이 삭제되었습니다. ID: " + commentId, result);
		}

		@Test
		@DisplayName("권한이 없는 사용자가 삭제를 시도하는 경우 예외발생, 403발생")
		void fail_no_permission() {
			Long commentId = 1L;
			Long invalidUserId = 999L;

			doThrow(new PermissionDeniedException()).when(commentService).removeComment(commentId, invalidUserId);

			assertThrows(PermissionDeniedException.class, () -> {
				commentController.deleteComment(commentId, invalidUserId);
			});
		}

		@Test
		@DisplayName("없는 댓글을 삭제하려 하는 경우 예외 발생")
		void fail_not_found() {
			Long commentId = 999L;
			Long requesterId = 1L;

			doThrow(new CommentNotFoundException(commentId)).when(commentService).removeComment(commentId, requesterId);

			assertThrows(CommentNotFoundException.class, () -> {
				commentController.deleteComment(commentId, requesterId);
			});
		}
	}
}
