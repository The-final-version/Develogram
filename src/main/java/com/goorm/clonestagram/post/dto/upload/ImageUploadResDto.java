package com.goorm.clonestagram.post.dto.upload;

import com.goorm.clonestagram.post.ContentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 업로드 응답을 위한 DTO
 * - content, type, createdAt을 반환
 */
@Getter
@Builder
public class ImageUploadResDto {
    private String content;
    private ContentType type;
    private LocalDateTime createdAt;
    private Long postId;
    private List<String> hashTagList = new ArrayList<>();;
    private String mediaName;
}
