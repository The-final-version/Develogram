package com.goorm.clonestagram.feed.dto;
import com.goorm.clonestagram.feed.domain.Feeds;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    public static FeedResponseDto from(Feeds feed) {
        return FeedResponseDto.builder()
                .feedId(feed.getId())
                .postId(feed.getPost().getId())
                .userId(feed.getUser().getId())
                .username(feed.getPost().getUser().getName()) // 게시글 작성자
                .content(feed.getPost().getContent())
                .mediaUrl(feed.getPost().getMediaName())
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
