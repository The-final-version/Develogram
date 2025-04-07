package com.goorm.clonestagram.search.service;

import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.dto.UserSuggestionDto;
import com.goorm.clonestagram.user.application.adapter.UserAdapter;
import com.goorm.clonestagram.user.application.dto.profile.UserProfileDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSearchService {

	private final UserExternalQueryService userExternalQueryService;

	/**
	 * 유저 검색
	 * - 검색어를 활용해 검색어와 부분 혹은 전체 일치하는 유저 반환
	 *
	 * @param rawKeyword 클라이언트의 검색 키워드
	 * @param pageable   페이징 기능
	 * @return 유저 리스트, 검색된 데이터 수
	 */
	@Transactional(readOnly = true)
	public SearchUserResDto searchUserByKeyword(String rawKeyword, Pageable pageable) {

		//1. 유저의 이름으로 관련된 데이터 모두 반환, Like 사용
		String keyword = Arrays.stream(rawKeyword.split("\\s+"))
			.filter(s -> !s.isBlank())
			.map(s -> "+" + s + "*")
			.collect(Collectors.joining(" "));

		Page<User> users = userExternalQueryService.searchUserByKeyword(keyword, pageable);

		Page<UserProfileDto> userProfiles = UserAdapter.toUserProfileDtoPage(users);

		return SearchUserResDto.of(users.getTotalElements(), userProfiles);
	}

	@Transactional(readOnly = true)
	public List<UserSuggestionDto> findUsersByKeyword(String keyword) {
		List<User> users = userExternalQueryService.findByName_NameContainingIgnoreCase(keyword);
		return users.stream()
			.map(user -> new UserSuggestionDto(user.getId(), user.getName(), user.getProfile().getImgUrl()))
			.collect(Collectors.toList());
	}

}
