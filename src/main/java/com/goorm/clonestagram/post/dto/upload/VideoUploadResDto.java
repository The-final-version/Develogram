package com.goorm.clonestagram.post.dto.upload;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import com.goorm.clonestagram.post.ContentType;

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
    private List<String> hashTagList;

}
