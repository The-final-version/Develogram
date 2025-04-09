package com.goorm.clonestagram.search.controller;

import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;
import com.goorm.clonestagram.search.service.HashTagSearchService;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tags")
@RequiredArgsConstructor
public class HashTagSearchController {

	private final HashTagSearchService hashTagSearchService;

	@GetMapping("")
	public ResponseEntity<SearchPostResDto> searchHashTag(@RequestParam @NotBlank String keyword,
		@PageableDefault(size = 20, sort = "posts.createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(hashTagSearchService.searchHashTagByKeyword(keyword, pageable));
	}

	@GetMapping("suggestions")
	public ResponseEntity<List<HashtagSuggestionDto>> suggestHashtags(
		@RequestParam("keyword") String keyword) {
		return ResponseEntity.ok(hashTagSearchService.getHashtagSuggestions(keyword));
	}

}
