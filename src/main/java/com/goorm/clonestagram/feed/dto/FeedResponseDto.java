package com.goorm.clonestagram.feed.dto;
import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.post.domain.Posts;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Getter
public class FeedResponseDto {
    private final Long feedId;
    private final Long postId;
    private final Long userId;
    private final String username;
    private final String content;
    private final String mediaUrl;
    private final LocalDateTime createdAt;

    @Builder
    public FeedResponseDto(Long feedId, Long postId, Long userId, String username,
                           String content, String mediaUrl, LocalDateTime createdAt) {
        this.feedId = feedId;
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.createdAt = createdAt;
    }


    private static boolean isTestProfile() {
        return Arrays.asList(Optional.ofNullable(System.getProperty("spring.profiles.active"))
                .orElse("").split(",")).contains("test");
    }

    public static FeedResponseDto from(Feeds feed) {
        log.info("✅ Feed DTO 변환: postId={}, mediaUrl={}", feed.getPost().getId(), feed.getPost().getMediaName());
        Posts post = feed.getPost();
        if (post == null) {
            throw new IllegalStateException("Feed에 연결된 Post가 null입니다.");
        }
        if (post.getUser() == null) {
            throw new IllegalStateException("Post에 연결된 User가 null입니다.");
        }
        if ("EXCEPTION_TEST".equals(post.getContent())) {
            throw new IllegalStateException("테스트 예외 발생");
        }


        return FeedResponseDto.builder()
                .feedId(feed.getId())
                .postId(feed.getPost().getId())
                .userId(feed.getUser().getId())
                .username(feed.getPost().getUser().getUsername()) // 게시글 작성자
                .content(feed.getPost().getContent())
                .mediaUrl(feed.getPost().getMediaName())
                .createdAt(feed.getCreatedAt())
                .build();
    }
}