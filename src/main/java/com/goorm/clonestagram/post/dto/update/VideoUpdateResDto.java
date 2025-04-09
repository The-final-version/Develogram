package com.goorm.clonestagram.post.dto.update;

import com.goorm.clonestagram.post.ContentType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 비디오 수정 응답을 위한 DTO
 * - content, type, updatedAt을 반환
 */
@Getter
@Builder
public class VideoUpdateResDto {
	private String content;
	private ContentType type;
	private LocalDateTime updatedAt;
	private List<String> hashTagList;

}

