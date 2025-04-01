package com.goorm.clonestagram.post.dto;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 정보 응답을 위한 DTO
 * - id, content, mediaName, contentType, createdAt 반환
 */

@Getter
@Builder
public class PostInfoDto {
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private String mediaName;
    private ContentType contentType;
    private LocalDateTime createdAt;

    /**
     *
     * Entity를 Dto로 변환
     *
     * @param post 변환할 엔티티
     * @return 변환된 Dto
     */
    public static PostInfoDto fromEntity(Posts post) {
        return PostInfoDto.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .username(post.getUser().getUsername())
                .content(post.getContent())
                .mediaName(post.getMediaName())
                .contentType(post.getContentType())
                .createdAt(post.getCreatedAt())
                .build();
    }
}

