package com.goorm.clonestagram.post.dto.update;

import com.goorm.clonestagram.post.ContentType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 업로드 응답을 위한 DTO
 * - content, type, updatedAt을 반환
 */
@Getter
@Builder
public class ImageUpdateResDto {
	private String content;
	private ContentType type;
	private LocalDateTime updatedAt;
	private List<String> hashTagList;

}
