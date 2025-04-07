package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import org.apache.juli.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private UserExternalQueryService userService;

    @InjectMocks
    private PostController postController;

    private UserEntity testUser;
    private Posts testPost;
    private PageRequest pageRequest;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(1L)
                .name("testuser")
                .build();

        testPost = new Posts();
        testPost.setId(1L);
        testPost.setContent("테스트 게시물");
        testPost.setUser(testUser);
        testPost.setContentType(ContentType.IMAGE);
        testPost.setMediaName("test-image.jpg");
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setUpdatedAt(LocalDateTime.now());

        pageRequest = PageRequest.of(0, 10);
    }

    @Test
    void 사용자의_게시물_목록_조회_성공() {
        // given
        List<Posts> posts = Arrays.asList(testPost);
        Page<Posts> postsPage = new PageImpl<>(posts, pageRequest, posts.size());
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser.toDomain());
        when(postService.getMyPosts(eq(1L), any())).thenReturn(PostResDto.builder()
                .user(UserAdapter.toUserProfileDto(testUser))
                .feed(postsPage.map(PostInfoDto::fromEntity))
                .build());

        // when
        ResponseEntity<PostResDto> response = postController.userPosts(1L, pageRequest);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getFeed().getContent().size());
        assertEquals(testPost.getContent(), response.getBody().getFeed().getContent().get(0).getContent());
        verify(userService).findByIdAndDeletedIsFalse(1L);
        verify(postService).getMyPosts(1L, pageRequest);
    }

    @Test
    @DisplayName("사용자의 게시물 목록 조회 실패 - 사용자 없음")
    void 사용자의_게시물_목록_조회_실패_유저없음() {
        // given
        when(userService.findByIdAndDeletedIsFalse(eq(1L)))
            .thenThrow(new UsernameNotFoundException("해당 사용자가 존재하지 않습니다."));

        // when & then: userPosts() 호출 시 UsernameNotFoundException 발생하는지 검증
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
            () -> postController.userPosts(1L, pageRequest)
        );
        assertEquals("해당 사용자가 존재하지 않습니다.", exception.getMessage());

        verify(userService).findByIdAndDeletedIsFalse(1L);
        verify(postService, never()).getMyPosts(any(Long.class), any());
    }
} 
