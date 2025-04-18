package com.goorm.clonestagram.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.exception.FeedFetchFailedException;
import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.dto.SeenRequest;
import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;
import com.goorm.clonestagram.util.IntegrationTestHelper;
import com.goorm.clonestagram.util.MockEntityFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(GlobalExceptionHandler.class)
class FeedIntegrationTest {

    @LocalServerPort
    int port;

    @PersistenceContext
    private EntityManager em;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private IntegrationTestHelper helper;
    @Autowired private ObjectMapper objectMapper;

    private UserEntity userA;
    private UserEntity userB;
    private Posts postA;
    private Posts postB;
    private HttpHeaders headers;
    @Autowired private FeedService feedService;
    @Autowired private FeedRepository feedRepository;
    @Autowired private PostsRepository postRepository;
    @Autowired private JpaUserExternalWriteRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll(); // 외래키 충돌 방지용 선제 삭제
        feedRepository.deleteAll(); // 외래키 충돌 방지용 선제 삭제
        User testUserA = User.testMockUser("testUserA");
        User testUserB = User.testMockUser("testUserB");

        userA = new UserEntity(testUserA);
        userB = new UserEntity(testUserB);
        userRepository.saveAll(List.of(userA, userB));
        headers = loginAndGetSession(userA.getEmail(), "mock1234!@");
    }

    private HttpHeaders loginAndGetSession(String email, String rawPassword) { // plain text password
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"email\":\"" + email + "\", \"password\":\"" + rawPassword + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity("/login", new HttpEntity<>(loginBody, loginHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders sessionHeaders = new HttpHeaders();
        sessionHeaders.setContentType(MediaType.APPLICATION_JSON);
        sessionHeaders.set("Cookie", response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        return sessionHeaders;
    }

    @AfterEach
    void cleanup() {
        helper.deleteUserAndDependencies(userA);
        if (userB != null) {
            helper.deleteUserAndDependencies(userB);
        }
    }

    @Test
    @Order(1)
    void FI01_피드_조회_없음() {
        ResponseEntity<String> response = restTemplate.exchange("/feeds", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\":[]");
    }

    @Test
    @Order(2)
    void FI02_피드_조회_있음() {
        // 유저B 생성 및 게시물 업로드 → 유저A가 팔로우 → 피드 생성
        userB = helper.createUser("userB");
        helper.follow(userA, userB);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        postB = helper.createPost(userB);

        // 피드 조회
        ResponseEntity<String> response = restTemplate.exchange("/feeds", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("media.jpg");
    }

    @Test
    @Order(3)
    void FI03_전체_피드_조회() {
        ResponseEntity<String> response = restTemplate.exchange("/feeds/all", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
    }

    @Test
    @Order(4)
    void FI04_팔로우_피드_조회_없음() {
        ResponseEntity<String> response = restTemplate.exchange("/feeds/follow", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\":[]");
    }

    @Test
    @Order(5)
    void FI05_팔로우_피드_조회_있음() {
        userB = helper.createUser("userB");
        helper.follow(userA, userB);

        // ✅ 피드 포함 게시물 생성
        postB = helper.createPost(userB);
        System.out.println("🔥 post.mediaName = " + postB.getMediaName());

        List<Feeds> allFeeds = feedRepository.findAllByUserIdWithDetails(userA.getId());
        System.out.println("🔍 피드 개수: " + allFeeds.size());
        for (Feeds f : allFeeds) {
            System.out.println("🧪 Feed: feedId=" + f.getId() + ", post.media = " + f.getPost().getMediaName());
        }
        // 조회
        ResponseEntity<String> response = restTemplate.exchange("/feeds/follow", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        System.out.println("응답 본문: " + response.getBody()); // 📌 디버깅용

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("media.jpg");
    }



    @Test
    @Order(6)
    void FI06_removeSeenFeeds_정상() {
        userB = helper.createUser("userB");
        postB = helper.createPost(userB);
        helper.follow(userA, userB);

        SeenRequest request = new SeenRequest();
        request.setPostIds(List.of(postB.getId()));
        ResponseEntity<Void> response = restTemplate.exchange("/feeds/seen", HttpMethod.DELETE, new HttpEntity<>(request, headers), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @Order(7)
    void FI07_removeSeenFeeds_예외_빈목록() {
        SeenRequest request = new SeenRequest(); // postIds null
        ResponseEntity<String> response = restTemplate.exchange(
                "/feeds/seen", HttpMethod.DELETE, new HttpEntity<>(request, headers), String.class
        );

        // 기존에는 204 기대 → 컨트롤러가 @Valid + @NotEmpty라면 400 발생이 맞음
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    // @Test
    // @Order(8)
    // void FI08_removeSeenFeeds_예외_세션없음() {
    //     SeenRequest request = new SeenRequest();
    //     request.setPostIds(List.of(1L));
    //     HttpHeaders noSession = new HttpHeaders(); // 세션 없음
    //     ResponseEntity<String> response = restTemplate.exchange("/feeds/seen", HttpMethod.DELETE, new HttpEntity<>(request, noSession), String.class);
    //     assertThat(response.getStatusCode())
    //         .isEqualTo(HttpStatus.FORBIDDEN); // 403
    //
    // }

    @Test
    @Order(9)
    void FI09_전체_피드_삭제() {
        userB = helper.createUser("userB");
        helper.follow(userA, userB);
        helper.createPost(userB);

        ResponseEntity<Void> response = restTemplate.exchange("/feeds/all", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }



}
