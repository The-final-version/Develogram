package com.goorm.clonestagram.follow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.clonestagram.common.exception.ErrorResponseDto;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.util.IntegrationTestHelper;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FollowIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IntegrationTestHelper testHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private Users userA;
    private Users userB;
    private HttpHeaders headers;

    private List<Users> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userA = testHelper.createUser("userA");
        userB = testHelper.createUser("userB");
        createdUsers.add(userA);
        createdUsers.add(userB);

        headers = loginAndGetSession(userA.getEmail(), "password");

    }

    @AfterEach
    void tearDown() {
        for (Users user : createdUsers) {
            testHelper.deleteUserAndDependencies(user);
        }
    }

    private HttpHeaders loginAndGetSession(String email, String password) {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        String loginBody = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";


        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, loginHeaders);
        ResponseEntity<String> response = restTemplate.postForEntity("/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders sessionHeaders = new HttpHeaders();
        sessionHeaders.setContentType(MediaType.APPLICATION_JSON);
        sessionHeaders.set("Cookie", response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

        return sessionHeaders;
    }

    @Test
    @Order(1)
    void I01_팔로우_요청_성공() {
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/follow/" + userA.getId() + "/profile/" + userB.getId(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    void I02_언팔로우_요청_성공() {
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        // 선행 팔로우
        restTemplate.exchange("/follow/" + userA.getId() + "/profile/" + userB.getId(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        // 언팔로우
        ResponseEntity<Void> response = restTemplate.exchange(
                "/follow/" + userA.getId() + "/profile/" + userB.getId(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(3)
    void I03_자기자신_팔로우_예외() {
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                "/follow/" + userA.getId() + "/profile/" + userA.getId(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ErrorResponseDto.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorMessage()).isEqualTo("자기 자신을 팔로우할 수 없습니다.");
    }

    @Test
    @Order(4)
    void I04_팔로우_후_팔로잉_목록_확인() throws Exception {
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        restTemplate.exchange("/follow/" + userA.getId() + "/profile/" + userB.getId(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/" + userA.getId() + "/profile/following",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FollowDto> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(list).anyMatch(dto -> dto.getFollowedId().equals(userB.getId()));
    }

    @Test
    @Order(5)
    void I05_언팔로우_후_팔로잉_목록_확인() throws Exception {
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        restTemplate.exchange("/follow/" + userA.getId() + "/profile/" + userB.getId(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        restTemplate.exchange("/follow/" + userA.getId() + "/profile/" + userB.getId(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/" + userA.getId() + "/profile/following",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        List<FollowDto> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(list).noneMatch(dto -> dto.getFollowedId().equals(userB.getId()));
    }

    @Test
    @Order(6)
    void I06_팔로우_후_팔로워_목록_확인() throws Exception {
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        restTemplate.exchange("/follow/" + userA.getId() + "/profile/" + userB.getId(), HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/" + userB.getId() + "/profile/followers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        List<FollowDto> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertThat(list).anyMatch(dto -> dto.getFollowerId().equals(userA.getId()));
    }

    @Test
    @Order(7)
    void I07_존재하지_않는_사용자에게_팔로우_요청_예외() {
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Long invalidId = -1L;

        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/" + invalidId,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(8)
    void I08_팔로워_목록_조회_시_존재하지_않는_사용자_예외() {
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/followers/-1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(9)
    void I09_팔로잉_목록_조회_시_존재하지_않는_사용자_예외() {
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange(
                "/follow/followings/-1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
