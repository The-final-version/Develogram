package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.post.dto.PostResDto;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.user.application.adapter.UsersAdapter;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
                .username("testuser")
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
                .user(UsersAdapter.toUserProfileDto(testUser))
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
    void 사용자의_게시물_목록_조회_실패_유저없음() {
        // given
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(null);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> postController.userPosts(1L, pageRequest));
        assertEquals("해당 유저를 찾을 수 없습니다.", exception.getMessage());
        verify(userService).findByIdAndDeletedIsFalse(1L);
        verify(postService, never()).getMyPosts(anyLong(), any());
    }
} 
