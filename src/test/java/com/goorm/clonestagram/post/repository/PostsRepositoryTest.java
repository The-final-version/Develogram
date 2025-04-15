package com.goorm.clonestagram.post.repository;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostsRepositoryTest {

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private JpaUserExternalWriteRepository userRepository;

    private UserEntity testUser;
    private Posts testPost;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
            .name("testuser")
            .email("test111@example.com")
            .password("testpassword")
            .profileBio("test bio")
            .profileImgUrl("test_url").build();
        userRepository.save(testUser);

        Posts deletedPost = Posts.builder()
                .user(testUser)
                .content("Deleted Content")
                .mediaName("deleted.jpg")
                .contentType(ContentType.IMAGE)
                .deleted(true)
                .build();
        postsRepository.save(deletedPost);

        Optional<Posts> foundDeleted = postsRepository.findById(deletedPost.getId());
        assertThat(foundDeleted).isPresent();
        assertThat(foundDeleted.get().isDeleted()).isTrue();

        testPost = Posts.builder()
                .content("테스트 게시물")
                .user(testUser)
                .contentType(ContentType.IMAGE)
                .mediaName("test-image.jpg")
                .build();
        testPost = postsRepository.save(testPost);
    }

    @Test
    void 게시물_저장_성공() {
        // given
        Posts post = Posts.builder()
                .content("새로운 게시물")
                .user(testUser)
                .contentType(ContentType.IMAGE)
                .mediaName("new-image.jpg")
                .build();

        // when
        Posts savedPost = postsRepository.save(post);

        // then
        assertNotNull(savedPost.getId());
        assertThat(savedPost.getUser()).isEqualTo(testUser);
        assertThat(savedPost.getContent()).isEqualTo("새로운 게시물");
        assertThat(savedPost.getMediaName()).isEqualTo("new-image.jpg");
        assertThat(savedPost.getContentType()).isEqualTo(ContentType.IMAGE);
        assertFalse(savedPost.isDeleted());
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getVersion()).isNotNull();
    }

    @Test
    void ID로_게시물_조회_성공() {
        // when
        Optional<Posts> found = postsRepository.findByIdAndDeletedIsFalse(testPost.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals(testPost.getContent(), found.get().getContent());
        assertFalse(found.get().isDeleted());
    }

    @Test
    void 존재하지_않는_ID로_게시물_조회() {
        // when
        Optional<Posts> found = postsRepository.findByIdAndDeletedIsFalse(999L);

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    void 유저의_게시물_목록_조회_성공() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Posts> postsPage = postsRepository.findAllByUserIdAndDeletedIsFalse(testUser.getId(), pageRequest);

        // then
        assertNotNull(postsPage);
        assertFalse(postsPage.getContent().isEmpty());
        assertEquals(testPost.getContent(), postsPage.getContent().get(0).getContent());
        assertFalse(postsPage.getContent().get(0).isDeleted());
    }

    @Test
    void 유저의_모든_게시물_목록_조회_성공() {
        // when
        List<Posts> posts = postsRepository.findAllByUserIdAndDeletedIsFalse(testUser.getId());

        // then
        assertNotNull(posts);
        assertFalse(posts.isEmpty());
        assertEquals(testPost.getContent(), posts.get(0).getContent());
        assertFalse(posts.get(0).isDeleted());
    }

    @Test
    void 게시물_존재_여부_확인() {
        // when
        boolean exists = postsRepository.existsByIdAndDeletedIsFalse(testPost.getId());
        boolean notExists = postsRepository.existsByIdAndDeletedIsFalse(999L);

        // then
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void 전체_게시물_목록_조회_성공() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Posts> postsPage = postsRepository.findAllByDeletedIsFalse(pageRequest);

        // then
        assertNotNull(postsPage);
        assertFalse(postsPage.getContent().isEmpty());
        assertEquals(testPost.getContent(), postsPage.getContent().get(0).getContent());
        assertFalse(postsPage.getContent().get(0).isDeleted());
    }
} 
