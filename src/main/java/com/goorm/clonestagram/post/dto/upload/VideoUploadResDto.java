package com.goorm.clonestagram.post.dto.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.goorm.clonestagram.post.ContentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 업로드 응답을 위한 DTO
 * - content, type, createdAt을 반환
 */
@Getter
@Builder
public class VideoUploadResDto {
    private String content;
    private ContentType type;
    private LocalDateTime createdAt;
    private Long postId;
    private List<String> hashTagList;

    // 기본 생성자 추가 (필요한 경우)
    public VideoUploadResDto() {}

    // @JsonCreator와 @JsonProperty를 사용하여 생성자 정의
    @JsonCreator
    public VideoUploadResDto(
        @JsonProperty("content") String content,
        @JsonProperty("type") ContentType type,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("postId") Long postId,
        @JsonProperty("hashTagList") List<String> hashTagList
    ) {
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
        this.postId = postId;
        this.hashTagList = hashTagList;
    }
}
