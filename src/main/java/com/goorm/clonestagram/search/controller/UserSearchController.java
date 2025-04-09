package com.goorm.clonestagram.search.controller;

import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.dto.UserSuggestionDto;
import com.goorm.clonestagram.search.service.UserSearchService;

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
@RequestMapping("users")
@RequiredArgsConstructor
public class UserSearchController {

	private final UserSearchService userSearchService;

	@GetMapping
	public ResponseEntity<SearchUserResDto> searchUsers(@RequestParam @NotBlank String keyword,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(userSearchService.searchUserByKeyword(keyword, pageable));
	}

	@GetMapping("/suggestions")
	public ResponseEntity<List<UserSuggestionDto>> suggestUsers(@RequestParam String keyword) {
		List<UserSuggestionDto> suggestions = userSearchService.findUsersByKeyword(keyword);
		return ResponseEntity.ok(suggestions);
	}
}
