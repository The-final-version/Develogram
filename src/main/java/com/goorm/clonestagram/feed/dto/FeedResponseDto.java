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
    private final String name;
    private final String content;
    private final String mediaUrl;
    private final LocalDateTime createdAt;

    @Builder
    public FeedResponseDto(Long feedId, Long postId, Long userId, String name,
                           String content, String mediaUrl, LocalDateTime createdAt) {
        this.feedId = feedId;
        this.postId = postId;
        this.userId = userId;
        this.name = name;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.createdAt = createdAt;
    }


    private static boolean isTestProfile() {
        return Arrays.asList(Optional.ofNullable(System.getProperty("spring.profiles.active"))
                .orElse("").split(",")).contains("test");
    }

    public static FeedResponseDto from(Feeds feed) {

        return FeedResponseDto.builder()
                .feedId(feed.getId())
                .postId(feed.getPost().getId())
                .userId(feed.getUser().getId())
                .name(feed.getPost().getUser().getName())   // 게시글 작성자 + 유저 도메인 수정
                .content(feed.getPost().getContent())
                .mediaUrl(feed.getPost().getMediaName())
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
