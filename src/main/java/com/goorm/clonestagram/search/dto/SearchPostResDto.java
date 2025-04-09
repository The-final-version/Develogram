package com.goorm.clonestagram.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.goorm.clonestagram.post.dto.PostInfoDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Page;

/**
 * 게시글 조회 응답 위한 DTO
 * - totalCount, postList를 반환
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchPostResDto {

	private Long totalCount;

	@JsonProperty("content")
	private Page<PostInfoDto> postList;

	public static SearchPostResDto of(Long totalCount, Page<PostInfoDto> postList) {
		return SearchPostResDto.builder()
			.postList(postList)
			.totalCount(totalCount)
			.build();
	}
}


