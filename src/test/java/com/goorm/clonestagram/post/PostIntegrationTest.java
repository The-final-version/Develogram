package com.goorm.clonestagram.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.goorm.clonestagram.util.CustomUserDetails;
import java.util.Base64;
import java.util.List;
import java.util.HashSet;

import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import com.goorm.clonestagram.post.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.goorm.clonestagram.post.service.ImageService;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Disabled;
import java.util.Collections;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private JpaUserExternalWriteRepository userRepository;

    @Autowired
    private com.goorm.clonestagram.hashtag.repository.PostHashTagRepository postHashTagsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostService postService;

    @Autowired
    private ImageService imageService;

	private UserEntity testUser;
	private UserEntity otherUser;
	private Posts testPost;

    @BeforeEach
    void setUp() {
        postHashTagsRepository.deleteAllInBatch();
        postsRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

		testUser = new UserEntity(User.testMockUser("integrationUser"));
		testUser = userRepository.save(testUser);

		otherUser = new UserEntity(User.testMockUser("otherUser"));
		otherUser = userRepository.save(otherUser);

		testPost = Posts.builder()
                .user(testUser)
                .content("통합 테스트 게시물 내용")
                .mediaName("integration_test.jpg")
                .contentType(com.goorm.clonestagram.post.ContentType.IMAGE)
                .deleted(false)
                .build();
        testPost = postsRepository.save(testPost);
    }

    @Test
    @DisplayName("C: 이미지 게시물 생성 성공")
    @Transactional
    void createImagePost_Success() throws Exception {
        String url = "/posts/images/upload";
        String content = "새 이미지 게시물 내용";
        String imageUrl = "http://example.com/test-image.jpg";
        List<String> hashTags = List.of("테스트", "이미지");
        ImageUploadReqDto uploadDto = new ImageUploadReqDto();
        uploadDto.setContent(content);
        uploadDto.setFile(imageUrl);
        uploadDto.setHashTagList(new HashSet<>(hashTags));
        String requestBody = objectMapper.writeValueAsString(uploadDto);

        ResultActions resultActions = mockMvc.perform(post(url)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(requestBody)
                 .header("Idempotency-Key", UUID.randomUUID().toString())
                 .with(user(new CustomUserDetails(testUser)))
                 .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.mediaName").value(imageUrl))
                .andExpect(jsonPath("$.type").value(com.goorm.clonestagram.post.ContentType.IMAGE.toString()))
                .andExpect(jsonPath("$.hashTagList").isArray())
                .andExpect(jsonPath("$.hashTagList", hasSize(2)))
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.hashTagList[*]", containsInAnyOrder("테스트", "이미지")));
    }

    @Test
    @DisplayName("R: 게시물 단건 조회 성공")
    @Transactional
    void getPostById_Success() throws Exception {
        Long postId = testPost.getId();
        String url = "/posts/" + postId;

        ResultActions resultActions = mockMvc.perform(get(url)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value("통합 테스트 게시물 내용"))
                .andExpect(jsonPath("$.mediaName").value("integration_test.jpg"))
                .andExpect(jsonPath("$.name").value(testUser.getName()))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    @DisplayName("R: 존재하지 않는 게시물 단건 조회 실패 (404 - Not Found)")
    @Transactional
    void getPostById_NotFound() throws Exception {
        Long nonExistentPostId = 9999L;
        String url = "/posts/" + nonExistentPostId;

        ResultActions resultActions = mockMvc.perform(get(url)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("U: 게시물 내용 수정 성공")
    @Transactional
    void updatePostContent_Success() throws Exception {
        Long postId = testPost.getId();
        String url = "/posts/images/" + postId;
        String updatedContent = "수정된 게시물 내용";

        List<String> hashTags = List.of("#updated", "#test");
        ImageUpdateReqDto updateDto = new ImageUpdateReqDto();
        updateDto.setContent(updatedContent);
        updateDto.setHashTagList(hashTags);

        String requestBody = objectMapper.writeValueAsString(updateDto);

        ResultActions resultActions = mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Posts updatedPostDb = postsRepository.findById(postId).orElseThrow();
        assertThat(updatedPostDb.getContent()).isEqualTo(updatedContent);
    }

    @Test
    @DisplayName("게시물 수정 실패 - 동시성 문제 (400 - Bad Request)")
    @Transactional
    void updatePost_ConcurrencyFailure() throws Exception {
        Long postId = testPost.getId();
        jdbcTemplate.update("UPDATE posts SET version = version + 1, content = 'Concurrent update' WHERE id = ?", postId);

        String url = "/posts/images/" + postId;
        ImageUpdateReqDto updateDto = new ImageUpdateReqDto();
        updateDto.setContent("My Conflicting Update");
        updateDto.setHashTagList(Collections.emptyList());
        String requestBody = objectMapper.writeValueAsString(updateDto);

        ResultActions resultActions = mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D: 게시물 삭제 성공")
    @Transactional
    void deletePost_Success() throws Exception {
        Long postId = testPost.getId();
        String url = "/posts/images/" + postId;

        ResultActions resultActions = mockMvc.perform(delete(url)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print()); // 디버깅용

        resultActions
                .andExpect(status().isOk());

        Posts deletedPost = postsRepository.findById(postId).orElseThrow();
        assertThat(deletedPost.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("C: 비디오 게시물 생성 성공")
    @Transactional
    void createVideoPost_Success() throws Exception {
        String url = "/posts/videos/upload";
        String content = "새 비디오 게시물 내용";
        String videoUrl = "http://example.com/test-video.mp4";
        List<String> hashTags = List.of("테스트", "비디오");

        VideoUploadReqDto uploadDto = new VideoUploadReqDto();
        uploadDto.setContent(content);
        uploadDto.setFile(videoUrl);
        uploadDto.setHashTagList(new HashSet<>(hashTags));

        String requestBody = objectMapper.writeValueAsString(uploadDto);

        ResultActions resultActions = mockMvc.perform(post(url)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(requestBody)
                 .header("Idempotency-Key", UUID.randomUUID().toString())
                 .with(user(new CustomUserDetails(testUser)))
                 .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.type").value(com.goorm.clonestagram.post.ContentType.VIDEO.toString()))
                .andExpect(jsonPath("$.hashTagList").isArray())
                .andExpect(jsonPath("$.hashTagList", hasSize(2)))
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.hashTagList[*]", containsInAnyOrder("테스트", "비디오")));
    }

    @Test
    @DisplayName("R: 특정 사용자 게시물 목록 조회 성공")
    void getUserPosts_Success() throws Exception {
        Posts anotherPost = Posts.builder()
                .user(testUser)
                .content("다른 테스트 게시물")
                .mediaName("another_test.png")
                .contentType(com.goorm.clonestagram.post.ContentType.IMAGE)
                .deleted(false)
                .build();
        postsRepository.save(anotherPost);

        String url = "/feeds/user?userId=" + testUser.getId();

        ResultActions resultActions = mockMvc.perform(get(url)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.feed.content").isArray())
                .andExpect(jsonPath("$.feed.content", hasSize(2)))
                .andExpect(jsonPath("$.feed.content[?(@.content == '통합 테스트 게시물 내용')]").exists())
                .andExpect(jsonPath("$.feed.content[?(@.content == '다른 테스트 게시물')]").exists());
    }

    @Test
    @DisplayName("Auth: 다른 사용자의 게시물 수정 시도 실패 (400 Bad Request)")
    void updatePost_ForbiddenForOtherUser() throws Exception {
        Long postId = testPost.getId();
        String url = "/posts/images/" + postId;
        String updatedContent = "다른 사용자가 수정 시도";

        List<String> hashTags = List.of("#updated", "#test");
        ImageUpdateReqDto updateDto = new ImageUpdateReqDto();
        updateDto.setContent(updatedContent);
        updateDto.setHashTagList(hashTags);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        ResultActions resultActions = mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(new CustomUserDetails(otherUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest());

        Posts postDb = postsRepository.findById(postId).orElseThrow();
        assertThat(postDb.getContent()).isNotEqualTo(updatedContent);
        assertThat(postDb.getContent()).isEqualTo("통합 테스트 게시물 내용");
    }

    @Test
    @DisplayName("Auth: 다른 사용자의 게시물 삭제 시도 실패 (400 Bad Request)")
    void deletePost_ForbiddenForOtherUser() throws Exception {
        Long postId = testPost.getId();
        String url = "/posts/images/" + postId;

        ResultActions resultActions = mockMvc.perform(delete(url)
                .with(user(new CustomUserDetails(otherUser)))
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest());

        Posts postDb = postsRepository.findById(postId).orElseThrow();
        assertThat(postDb.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Validation: 게시물 생성 시 내용 누락 실패 (400 Bad Request - 실제 코드 수정 필요)")
    void createImagePost_FailMissingContent() throws Exception {
        String url = "/posts/images/upload";
        String imageUrl = "http://example.com/no-content.jpg";
        List<String> hashTags = List.of("유효성", "실패");
        ImageUploadReqDto uploadDto = new ImageUploadReqDto();
        uploadDto.setContent(null);
        uploadDto.setFile(imageUrl);
        uploadDto.setHashTagList(new HashSet<>(hashTags));
        String requestBody = objectMapper.writeValueAsString(uploadDto);

        ResultActions resultActions = mockMvc.perform(post(url)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(requestBody)
                 .header("Idempotency-Key", UUID.randomUUID().toString())
                 .with(user(new CustomUserDetails(testUser)))
                 .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: 게시물 생성 시 파일 URL 누락 실패 (400 Bad Request)")
    void createImagePost_FailMissingFileUrl() throws Exception {
        String url = "/posts/images/upload";
        String content = "파일 없는 게시물";
        List<String> hashTags = List.of("유효성", "실패");
        ImageUploadReqDto uploadDto = new ImageUploadReqDto();
        uploadDto.setContent(content);
        uploadDto.setHashTagList(new HashSet<>(hashTags));
        String requestBody = objectMapper.writeValueAsString(uploadDto);

        ResultActions resultActions = mockMvc.perform(post(url)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(requestBody)
                 .header("Idempotency-Key", UUID.randomUUID().toString())
                 .with(user(new CustomUserDetails(testUser)))
                 .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C: 이미지 게시물 생성 멱등성 보장")
    void createImagePost_Idempotency() throws Exception {
        String url = "/posts/images/upload";
        String content = "멱등성 테스트 이미지 게시물";
        String imageUrl = "http://example.com/idempotency-image.jpg";
        List<String> hashTags = List.of("#test", "#integration");
        String idempotencyKey = UUID.randomUUID().toString();

        ImageUploadReqDto uploadDto = new ImageUploadReqDto();
        uploadDto.setContent(content);
        uploadDto.setFile(imageUrl);
        uploadDto.setHashTagList(new HashSet<>(hashTags));
        String requestBody = objectMapper.writeValueAsString(uploadDto);

        ResultActions firstResultActions = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Idempotency-Key", idempotencyKey)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        String firstResponseContent = firstResultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.mediaName").value(imageUrl))
                .andExpect(jsonPath("$.postId").exists())
                .andReturn().getResponse().getContentAsString();

        long initialCount = postsRepository.count();

        ResultActions secondResultActions = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Idempotency-Key", idempotencyKey)
                .with(user(new CustomUserDetails(testUser)))
                .accept(MediaType.APPLICATION_JSON));

        secondResultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(content().json(firstResponseContent));

        long finalCount = postsRepository.count();
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("C: 비디오 게시물 생성 멱등성 보장")
    void createVideoPost_Idempotency() throws Exception {
        String url = "/posts/videos/upload";
        String content = "멱등성 테스트 비디오 게시물";
        String videoUrl = "http://example.com/idempotency-video.mp4";
        List<String> hashTags = List.of("멱등성", "비디오");
        String idempotencyKey = UUID.randomUUID().toString(); // 고유 멱등성 키 생성

        VideoUploadReqDto uploadDto = new VideoUploadReqDto();
        uploadDto.setContent(content);
        uploadDto.setFile(videoUrl);
        uploadDto.setHashTagList(new HashSet<>(hashTags));
        String requestBody = objectMapper.writeValueAsString(uploadDto);

        // when: 첫 번째 요청
        ResultActions firstResultActions = mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .header("Idempotency-Key", idempotencyKey) // 헤더 이름 수정
            .with(user(new CustomUserDetails(testUser)))
            .accept(MediaType.APPLICATION_JSON));

        // then: 첫 번째 응답 검증
        String firstResponseContent = firstResultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(content))
            .andExpect(jsonPath("$.type").value(com.goorm.clonestagram.post.ContentType.VIDEO.toString()))
            .andExpect(jsonPath("$.postId").exists())
            .andReturn().getResponse().getContentAsString();

        long initialCount = postsRepository.count();

        // when: 두 번째 요청 (동일한 내용, 동일한 키)
        ResultActions secondResultActions = mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .header("Idempotency-Key", idempotencyKey) // 헤더 이름 수정
            .with(user(new CustomUserDetails(testUser)))
            .accept(MediaType.APPLICATION_JSON));

        // then: 두 번째 응답 검증
        secondResultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").exists())
            .andExpect(content().json(firstResponseContent));

        // DB 카운트 확인
        long finalCount = postsRepository.count();
        assertThat(finalCount).isEqualTo(initialCount);
    }
} 
