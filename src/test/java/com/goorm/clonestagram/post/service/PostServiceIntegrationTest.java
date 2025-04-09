package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.exception.UserNotFoundException;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;
    private Posts testPost;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성
        testUser = Users.builder()
                .username("testUser")
                .password("password")
                .email("test@example.com")
                .build();
        testUser = userRepository.save(testUser);

        // 테스트용 게시물 생성
        testPost = Posts.builder()
                .user(testUser)
                .content("테스트 게시물 내용")
                .mediaName("test.jpg")
                .build();
        testPost = postsRepository.save(testPost);
    }

    @Test
    @DisplayName("I01게시물저장성공: 게시물 저장 정상 수행")
    void savePost_Success() {
        // given
        Posts newPost = Posts.builder()
                .user(testUser)
                .content("새로운 게시물")
                .mediaName("new.jpg")
                .build();

        // when
        Posts savedPost = postService.save(newPost);

        // then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("새로운 게시물");
        assertThat(savedPost.getMediaName()).isEqualTo("new.jpg");
        assertThat(savedPost.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("I02게시물조회성공: 존재하는 게시물 조회 정상 수행")
    void findPost_Success() {
        // given
        Long postId = testPost.getId();

        // when
        Posts foundPost = postService.findByIdAndDeletedIsFalse(postId);

        // then
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isEqualTo(postId);
        assertThat(foundPost.getContent()).isEqualTo("테스트 게시물 내용");
        assertThat(foundPost.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("I03사용자게시물목록조회성공: 사용자의 게시물 목록 조회 정상 수행")
    void getUserPosts_Success() {
        // given
        Long userId = testUser.getId();
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        var result = postService.getMyPosts(userId, pageRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getFeed()).isNotNull();
        assertThat(result.getFeed().getContent()).hasSize(1);
        assertThat(result.getFeed().getContent().get(0).getId()).isEqualTo(testPost.getId());
    }

    @Test
    @DisplayName("I04게시물존재여부확인성공: 존재하는 게시물 확인 정상 수행")
    void checkPostExists_Success() {
        // given
        Long postId = testPost.getId();

        // when
        boolean exists = postService.existsByIdAndDeletedIsFalse(postId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("I05사용자모든게시물조회성공: 사용자의 모든 게시물 조회 정상 수행")
    void getAllUserPosts_Success() {
        // given
        Long userId = testUser.getId();

        // when
        var posts = postService.findAllByUserIdAndDeletedIsFalse(userId);

        // then
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getId()).isEqualTo(testPost.getId());
        assertThat(posts.get(0).getUser().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("I06게시물삭제성공: 게시물 삭제 정상 수행")
    void deletePost_Success() {
        // given
        Long postId = testPost.getId();
        Posts post = postService.findByIdAndDeletedIsFalse(postId);
        post.setDeleted(true);

        // when
        Posts deletedPost = postService.save(post);

        // then
        assertThat(deletedPost.getDeleted()).isTrue();
        assertThrows(IllegalArgumentException.class, () -> {
            postService.findByIdAndDeletedIsFalse(postId);
        });
    }

    @Test
    @DisplayName("I07존재하지않는게시물조회예외: 존재하지 않는 게시물 조회 시 예외 발생")
    void findPost_NotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 999L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            postService.findByIdAndDeletedIsFalse(nonExistentPostId);
        });
    }

    @Test
    @DisplayName("I08존재하지않는사용자게시물조회예외: 존재하지 않는 사용자의 게시물 조회 시 예외 발생")
    void getUserPosts_UserNotFound_ThrowsException() {
        // given
        Long nonExistentUserId = 999L;
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when & then
        assertThrows(UserNotFoundException.class, () -> {
            postService.getMyPosts(nonExistentUserId, pageRequest);
        });
    }

    @Test
    @DisplayName("I09삭제된게시물조회예외: 삭제된 게시물 조회 시 예외 발생")
    void findDeletedPost_ThrowsException() {
        // given
        Long postId = testPost.getId();
        Posts post = postService.findByIdAndDeletedIsFalse(postId);
        post.setDeleted(true);
        postService.save(post);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            postService.findByIdAndDeletedIsFalse(postId);
        });
    }
} 