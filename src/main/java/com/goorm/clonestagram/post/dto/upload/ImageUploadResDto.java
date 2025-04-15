package com.goorm.clonestagram.post.dto.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.goorm.clonestagram.post.ContentType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class ImageUploadResDto {

    private String content;
    private ContentType type;
    private LocalDateTime createdAt;
    private Long postId;
    private List<String> hashTagList = new ArrayList<>();
    private String mediaName;

    // @JsonCreator와 @JsonProperty를 사용하여 생성자 지정
    @JsonCreator
    public ImageUploadResDto(
        @JsonProperty("content") String content,
        @JsonProperty("type") ContentType type,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("postId") Long postId,
        @JsonProperty("hashTagList") List<String> hashTagList,
        @JsonProperty("mediaName") String mediaName
    ) {
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
        this.postId = postId;
        this.hashTagList = hashTagList != null ? hashTagList : new ArrayList<>();
        this.mediaName = mediaName;
    }
}
