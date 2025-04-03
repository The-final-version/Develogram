package com.goorm.clonestagram.feed.controller;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.util.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class FeedControllerTest {

    @Mock
    private FeedService feedService;

    @InjectMocks
    private FeedController feedController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//
//    public FeedControllerUnitTest() {
//        MockitoAnnotations.openMocks(this); // 🔄 @BeforeEach 안 써도 됨
//    }

    @Test
    void 피드_조회_성공() {
        // given
        Users user = new Users();
        user.setId(1L);
        user.setUsername("test_user");
        user.setPassword("password");
        user.setEmail("test@naver.com");
        CustomUserDetails mockUser = new CustomUserDetails(user); // 가짜 사용자

        List<FeedResponseDto> feedList = List.of(
                FeedResponseDto.builder()
                        .feedId(1L)
                        .postId(100L)
                        .userId(2L)
                        .username("other_user")
                        .content("Hello world")
                        .mediaUrl("https://cdn.img")
                        .content("IMAGE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Page<FeedResponseDto> mockPage = new PageImpl<>(feedList);

        Pageable pageable = PageRequest.of(0, 10);
        when(feedService.getUserFeed(eq(1L), eq(pageable)))
                .thenReturn(mockPage);
        // when
        ResponseEntity<Page<FeedResponseDto>> response = feedController.getMyFeed(mockUser, pageable);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getPostId()).isEqualTo(100L);
    }

    @Test
    void 피드_삭제_성공() {
        // given
        Long userId = 1L;
        List<Long> postIds = List.of(101L, 102L);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(userId);

        FeedController.SeenRequest request = new FeedController.SeenRequest();
        request.setPostIds(postIds);

        // when
        ResponseEntity<Void> response = feedController.removeSeenFeeds(userDetails, request);

        // then
        verify(feedService).removeSeenFeeds(userId, postIds);
        assertThat(response.getStatusCodeValue()).isEqualTo(204); // NO_CONTENT
    }


}