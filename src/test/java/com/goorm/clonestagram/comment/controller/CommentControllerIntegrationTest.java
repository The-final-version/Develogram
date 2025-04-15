package com.goorm.clonestagram.comment.controller;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.comment.domain.Comments;
import com.goorm.clonestagram.comment.dto.CommentRequest;
import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.exception.PostNotFoundException;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@DisplayName("CommentController 통합 테스트")
public class CommentControllerIntegrationTest {
	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	JpaUserExternalWriteRepository userExternalWriteRepository;
	@Autowired
	PostsRepository postsRepository;
	@Autowired
	UserExternalQueryService userService;

	public static final String BASE = "/comments";
	public static final String BY_ID = BASE + "/{commentsId}";
	public static final String BY_POST_ID = BASE + "/post/{postId}";

	UserEntity userA, userB, userC, userD;
	Posts postX;
	@Autowired
	private CommentRepository commentRepository;

	@BeforeEach
	void setUp() {
		userExternalWriteRepository.deleteAll();
		userA = UserEntity.from(User.testMockUser("userA"));
		userB = UserEntity.from(User.testMockUser("userB"));
		userC = UserEntity.from(User.testMockUser("userC"));
		userD = UserEntity.from(User.testMockUser("userD"));
		postX = Posts.builder().user(userA).content("postX").mediaName("postX.jpg").build();
		System.out.println("userA = " + userA);
		System.out.println("userB = " + userB);
		System.out.println("userC = " + userC);
		System.out.println("userD = " + userD);
		userExternalWriteRepository.saveAll(List.of(userA, userB, userC, userD));
		postsRepository.save(postX);
	}

	@Nested
	@DisplayName("댓글 조회 테스트")
	class CommentReadTest {

		@Test
		@DisplayName("1. 댓글 작성 후 댓글 ID로 조회")
		void createAndGetCommentById() throws Exception {
			// 포스트에 댓글 작성 후 댓글 ID로 댓글 조회하기.
			// 	사용자 B가 포스트 X에 댓글을 작성한다.
			CommentRequest request = new CommentRequest
				(userB.getId(), postX.getId(), "comment written by userB");

			ResultActions createResult = mockMvc.perform(
				MockMvcRequestBuilders.post(BASE)
					.with(user(userB.getEmail()).roles("USER"))
					.header("Idempotency-Key", "test-key-" + UUID.randomUUID())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			);

			// 	201을 보내줌.
			createResult.andExpect(status().isCreated())
				.andDo(
					result -> {
						String location = result.getResponse().getHeader("Location");
						assertThat(location).isNotNull();
						assertThat(location).contains("/comments/");
					}
				).andDo(MockMvcResultHandlers.print());

			// ID 추출
			MvcResult resultOfPost = createResult.andReturn();
			String responseBody = resultOfPost.getResponse().getContentAsString();
			Long commentBId = objectMapper.readTree(responseBody).get("id").asLong();

			// 댓글 ID로 조회 시
			ResultActions getResult = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_ID, commentBId)
					.contentType(MediaType.APPLICATION_JSON)
			);

			// User ID 추출
			MvcResult resultOfGet = getResult.andReturn();
			String body = resultOfGet.getResponse().getContentAsString();
			JsonNode json = objectMapper.readTree(body);

			Long userId = json.get("userId").asLong();

			// 해당 댓글이 반환되며 200을 보내줌
			getResult
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(commentBId))
				.andExpect(jsonPath("$.content").value("comment written by userB"));
			// 작성자가 일치하는지 검증
			User foundUser = userService.findByIdAndDeletedIsFalse(userId);
			assertThat(foundUser.getName()).isEqualTo("userB");
			assertThat(foundUser.getEmail()).isEqualTo("userB@example.com");

		}

		@Test
		@DisplayName("2. 존재하지 않는 댓글 ID 조회 시 404")
		void getNonExistentCommentById() throws Exception {
			// 사용자 B가 포스트 X에 댓글을 작성한다.

			// 잘못된 댓글 ID로 조회 시
			ResultActions getResult = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_ID, 998877L)
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 적절한 오류 메세지와 404를 보내줌
			getResult
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("댓글을 찾을 수 없습니다"))
				.andExpect(jsonPath("$.detail").value(containsString("존재하지 않는 댓글입니다. ID: " + Long.toString(998877L))));
		}

		@Test
		@DisplayName("3. 포스트 ID로 댓글 목록 조회")
		void getCommentsByPostId() throws Exception {
			// 사용자 B, C, D가 포스트 X에 댓글을 작성한다.
			Comments commentB = Comments.builder()
				.users(userB)
				.posts(postX)
				.content("comment written by userB")
				.build();
			Comments commentC = Comments.builder()
				.users(userC)
				.posts(postX)
				.content("comment written by userC")
				.build();
			Comments commentD = Comments.builder()
				.users(userD)
				.posts(postX)
				.content("comment written by userD")
				.build();

			commentRepository.saveAll(List.of(commentB, commentC, commentD));

			// 포스트 ID로 조회 시
			ResultActions getResult = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);
			//댓글의 리스트가 반환되며 200을 보내줌.
			getResult
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].content", containsInAnyOrder(
					"comment written by userB",
					"comment written by userC",
					"comment written by userD"
				)));
		}

		@Test
		@DisplayName("3-2. 포스트 ID로 댓글 목록 조회 (댓글이 없는 경우)")
		void getCommentsByPostIdWithoutComments() throws Exception {
			// 포스트 ID로 조회 시
			ResultActions getResult = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);
			//빈 리스트가 반환되며 200을 보내줌.
			getResult
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
		}

		@Test
		@DisplayName("4. 존재하지 않는 포스트 ID로 댓글 목록 조회 시 404")
		void getCommentsByInvalidPostId() throws Exception {
			// 잘못된 포스트 ID로 조회 시
			ResultActions getResult = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_POST_ID, 789456L)
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 적절한 오류 메세지와 404를 보내줌
			getResult
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("게시글을 찾을 수 없습니다"))
				.andExpect(jsonPath("$.detail").value(containsString("존재하지 않는 게시글입니다. ID: " + Long.toString(789456L))));
		}
	}

	@Nested
	@DisplayName("댓글 생성 테스트")
	class CommentCreateTest {

		@Test
		@DisplayName("10. 멱등성 키를 사용하여 댓글 생성 성공")
		void createCommentWithIdempotencyKey() throws Exception {
			String idempotencyKey = UUID.randomUUID().toString();
			CommentRequest request = new CommentRequest(userC.getId(), postX.getId(), "멱등성 테스트 댓글");

			// 첫 번째 요청
			ResultActions firstResult = mockMvc.perform(
					MockMvcRequestBuilders.post(BASE)
							.with(user(userC.getEmail()).roles("USER"))
							.header("Idempotency-Key", idempotencyKey)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request))
			);

			// 첫 번째 요청은 201 Created 반환
			firstResult.andExpect(status().isCreated())
					.andExpect(jsonPath("$.content").value("멱등성 테스트 댓글"));

			String firstResponseBody = firstResult.andReturn().getResponse().getContentAsString();
			long firstCommentId = objectMapper.readTree(firstResponseBody).get("id").asLong();

			// 동일한 멱등성 키로 두 번째 요청
			ResultActions secondResult = mockMvc.perform(
					MockMvcRequestBuilders.post(BASE)
							.with(user(userC.getEmail()).roles("USER"))
							.header("Idempotency-Key", idempotencyKey)
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request))
			);

			// 두 번째 요청도 컨트롤러 로직상 201 Created를 반환하지만, 내용은 첫 번째 응답과 동일해야 함.
			// IdempotencyService가 캐시된 결과를 반환해도 Controller는 항상 .created() 로 응답하기 때문.
			secondResult.andExpect(status().isCreated())
					.andExpect(content().json(firstResponseBody));

			// DB에 댓글이 하나만 생성되었는지 확인
			List<Comments> comments = commentRepository.findByPosts_Id(postX.getId());
			long count = comments.stream().filter(c -> c.getContent().equals("멱등성 테스트 댓글")).count();
			assertThat(count).isEqualTo(1);
			assertThat(comments.get(comments.size()-1).getId()).isEqualTo(firstCommentId);
		}

		@Test
		@DisplayName("11. 멱등성 키 없이 댓글 생성 성공")
		void createCommentWithoutIdempotencyKey() throws Exception {
			CommentRequest request = new CommentRequest(userD.getId(), postX.getId(), "멱등성 키 없는 댓글");

			mockMvc.perform(
							MockMvcRequestBuilders.post(BASE)
									.with(user(userD.getEmail()).roles("USER"))
									.contentType(MediaType.APPLICATION_JSON)
									.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.content").value("멱등성 키 없는 댓글"));
		}
	}

	@Nested
	@DisplayName("댓글 삭제 테스트")
	class CommentDeleteTest {

		@Test
		@DisplayName("5. 댓글 작성자가 댓글 삭제")
		void deleteOwnComment() throws Exception {
			// 사용자 B, C, D가 포스트 X에 댓글을 작성한다.
			// 사용자 B, C, D가 포스트 X에 댓글을 작성한다.
			Comments commentB = Comments.builder()
				.users(userB)
				.posts(postX)
				.content("comment written by userB")
				.build();
			Comments commentC = Comments.builder()
				.users(userC)
				.posts(postX)
				.content("comment written by userC")
				.build();
			Comments commentD = Comments.builder()
				.users(userD)
				.posts(postX)
				.content("comment written by userD")
				.build();

			commentRepository.saveAll(List.of(commentB, commentC, commentD));

			// 사용자 B가 자신이 작성한 댓글을 삭제한다.

			ResultActions deleteResult = mockMvc.perform(
				MockMvcRequestBuilders.delete(BY_ID, commentB.getId())
					.param("requesterId", userB.getId().toString())
					.with(user(userB.getEmail()).roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
			);
			// 204를 보내줌.
			deleteResult
				.andExpect(status().isNoContent());

			// 댓글 ID로 조회하면
			ResultActions getResultOfFindById = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_ID, commentB.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 적절한 오류 메세지와 404를 보내줌
			getResultOfFindById
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("댓글을 찾을 수 없습니다"))
				.andExpect(jsonPath("$.detail").value(containsString("존재하지 않는 댓글입니다. ID: " + commentB.getId())));

			// 	포스트 ID로 조회하면
			ResultActions getResultOfFindByPostId = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 	조회되는 리스트에 B의 댓글이 포함되어 있지 않다.
			getResultOfFindByPostId
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].content", not(contains("comment written by userB"))));
		}

		@Test
		@DisplayName("6. 포스트 작성자가 다른 사람의 댓글 삭제")
		void deleteCommentAsPostAuthor() throws Exception {
			// 사용자 C, D가 포스트 X에 댓글을 작성한다.
			// 사용자 C, D가 포스트 X에 댓글을 작성한다.
			Comments commentC = Comments.builder()
				.users(userC)
				.posts(postX)
				.content("comment written by userC")
				.build();
			Comments commentD = Comments.builder()
				.users(userD)
				.posts(postX)
				.content("comment written by userD")
				.build();

			commentRepository.saveAll(List.of(commentC, commentD));

			// 사용자 A가 포스트 X에 작성된 사용자 C의 댓글을 삭제한다.

			ResultActions deleteResult = mockMvc.perform(
				MockMvcRequestBuilders.delete(BY_ID, commentC.getId())
					.param("requesterId", userA.getId().toString())
					.contentType(MediaType.APPLICATION_JSON)
					.with(user(userA.getEmail()).roles("USER"))
			);
			// 204를 보내줌.
			deleteResult
				.andExpect(status().isNoContent());

			// 댓글 ID로 조회하면
			ResultActions getResultOfFindById = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_ID, commentC.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 적절한 오류 메세지와 404를 보내줌
			getResultOfFindById
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("댓글을 찾을 수 없습니다"))
				.andExpect(jsonPath("$.detail").value(containsString("존재하지 않는 댓글입니다. ID: " + commentC.getId())));

			// 	포스트 ID로 조회하면
			ResultActions getResultOfFindByPostId = mockMvc.perform(
				MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
					.contentType(MediaType.APPLICATION_JSON)
			);

			// 	조회되는 리스트에 C의 댓글이 포함되어 있지 않다.
			getResultOfFindByPostId
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].content", not(contains("comment written by userC"))));
		}

		@Test
		@DisplayName("8. 권한 없는 사용자가 댓글 삭제 시 403")
		void deleteCommentWithoutPermission() throws Exception {
			// 사용자 D가 포스트 X에 댓글을 작성한다.
			Comments commentD = Comments.builder()
				.users(userD)
				.posts(postX)
				.content("comment written by userD")
				.build();

			commentRepository.save(commentD);

			// 사용자 C가 사용자 D가 작성한 댓글을 삭제하려고 한다.
			ResultActions deleteResult = mockMvc.perform(
				MockMvcRequestBuilders.delete(BY_ID, commentD.getId())
					.param("requesterId", userC.getId().toString())
					.contentType(MediaType.APPLICATION_JSON)
					.with(user(userC.getEmail()).roles("USER"))
			);
			//     403을 보내줌
			deleteResult.andExpect(status().isForbidden());

			//     댓글 ID를 조회하면 D가 작성한 댓글이 나온다.
			mockMvc.perform(
					MockMvcRequestBuilders.get(BY_ID, commentD.getId())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content")
					.value("comment written by userD"));

			//     포스트 ID로 조회하면 조회되는 리스트에 D의 댓글이 포함되어 있다.
			mockMvc.perform(
					MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].content", contains("comment written by userD")));
		}

		@Test
		@DisplayName("9. 인증되지 않은 사용자가 댓글 삭제 시 401")
		void deleteCommentUnauthenticated() throws Exception {
			// 로그인 되지 않은 사용자가 D의 댓글을 삭제하려고 한다.
			Comments commentD = Comments.builder()
				.users(userD)
				.posts(postX)
				.content("comment written by userD")
				.build();

			commentRepository.save(commentD);

			ResultActions deleteResult = mockMvc.perform(
				MockMvcRequestBuilders.delete(BY_ID, commentD.getId())
					.param("requesterId", userD.getId().toString()) // 요청자 ID는 있어도 인증이 없으면 의미 없음
					.contentType(MediaType.APPLICATION_JSON)
			);
			// 401을 보내줌
			deleteResult.andExpect(status().isUnauthorized());

			// 댓글 ID를 조회하면 D가 작성한 댓글이 나온다.
			mockMvc.perform(
					MockMvcRequestBuilders.get(BY_ID, commentD.getId())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("comment written by userD"));

			// 포스트 ID로 조회하면 조회되는 리스트에 D의 댓글이 포함되어 있다.
			mockMvc.perform(
					MockMvcRequestBuilders.get(BY_POST_ID, postX.getId())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].content", contains("comment written by userD")));
		}
	}

	// @Nested
	// @DisplayName("댓글 작성 실패 테스트")
	// class CommentWriteFailTest {
	//
	// 	@Test
	// 	@DisplayName("7. 빈 댓글 작성 시 400")
	// 	void createCommentWithInvalidContent() {
	// 		// TODO : 댓글 유효성 검증 필요, 유효성 검증 도입 이후 테스트 작성 예정
	// 	}
	// }
}

