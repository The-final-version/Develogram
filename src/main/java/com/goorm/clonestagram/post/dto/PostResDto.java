package com.goorm.clonestagram.post.dto;

import lombok.Builder;
import lombok.Getter;

import org.springframework.data.domain.Page;

import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;

/**
 * 피드 요청 응답을 위한 DTO
 * - user정보와 페이징 처리된 feed 리스트를 반환
 */
@Getter
@Builder
public class PostResDto {
	private UserProfileDto user;
	private Page<PostInfoDto> feed;
}
