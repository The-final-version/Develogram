package com.goorm.clonestagram.search.service;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HashTagSearchService {

	private final HashTagRepository hashTagRepository;
	private final PostHashTagRepository postHashTagRepository;

	@Transactional(readOnly = true)
	public SearchPostResDto searchHashTagByKeyword(@NotBlank String keyword, Pageable pageable) {
		Page<Posts> tagPosts = postHashTagRepository.findPostsByHashtagKeyword(keyword, pageable);

		Page<PostInfoDto> userProfiles = tagPosts.map(PostInfoDto::fromEntity);

		return SearchPostResDto.of(tagPosts.getTotalElements(), userProfiles);
	}

	@Transactional(readOnly = true)
	public List<HashtagSuggestionDto> getHashtagSuggestions(String keyword) {
		List<HashTags> tags = hashTagRepository.findByTagContentContaining(keyword);
		return tags.stream()
			.map(tag -> new HashtagSuggestionDto(tag.getTagContent(), postHashTagRepository.countByHashTags(tag)))
			.collect(Collectors.toList());
	}

}
