package com.goorm.clonestagram.search.controller;


import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;
import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.dto.UserSuggestionDto;
import com.goorm.clonestagram.search.service.SearchService;
import com.goorm.clonestagram.util.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 검색 요청을 처리하는 컨트롤러
 * - 클라이언트로부터 검색 키워드를 받아 검증 및 서비스 호출을 수행
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * 유저 검색
     * - 클라이언트가 유저 이름을 검색하면 관련 데이터 반환
     *
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 유저 리스트, 검색된 데이터 수
     */
    @GetMapping("/users")
    public ResponseEntity<SearchUserResDto> searchUsers(@RequestParam @NotBlank String keyword,
                                                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable){
        return ResponseEntity.ok(searchService.searchUserByKeyword(keyword, pageable));
    }

    /**
     * 팔로우 유저 검색
     * - 클라이언트가 팔로우 중에서 유저를 찾고자 할때 사용
     * - 클라이언트가 유저 이름을 검색하면 팔로우 중에서 관련 데이터 반환
     *
     * @param userDetail
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 유저 리스트, 검색된 데이터 수
     */
    @GetMapping("/following")
    public ResponseEntity<SearchUserResDto> searchFollowing(@AuthenticationPrincipal CustomUserDetails userDetail,
                                                            @RequestParam @NotBlank String keyword,
                                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable){

        Long userId = userDetail.getId();

        return ResponseEntity.ok(searchService.searchFollowingByKeyword(userId,keyword,pageable));
    }

    /**
     * 팔로워 유저 검색
     * - 클라이언트가 팔로워 중에서 유저를 찾고자 할때 사용
     * - 클라이언트가 유저 이름을 검색하면 팔로워 중에서 관련 데이터 반환
     *
     * @param userDetail
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 유저 리스트, 검색된 데이터 수
     */
    @GetMapping("/follower")
    public ResponseEntity<SearchUserResDto> searchFollower(@AuthenticationPrincipal CustomUserDetails userDetail,
                                                           @RequestParam @NotBlank String keyword,
                                                           @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable){

        Long userId = userDetail.getId();

        return ResponseEntity.ok(searchService.searchFollowerByKeyword(userId,keyword,pageable));
    }

    /**
     *해시 태그 검색
     * - 클라이언트가 특정 해시태그가 붙은 피드 조회시 사용
     *
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 피드 리스트, 검색된 데이터 수
     */
    @GetMapping("/tag")
    public ResponseEntity<SearchPostResDto> searchHashTag(@RequestParam @NotBlank String keyword,
                                                          @PageableDefault(size = 20, sort = "posts.createdAt", direction = Sort.Direction.DESC)Pageable pageable) {
        return ResponseEntity.ok(searchService.searchHashTagByKeyword(keyword,pageable));
    }

    @GetMapping("/tag/suggestions")
    public ResponseEntity<List<HashtagSuggestionDto>> suggestHashtags(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(searchService.getHashtagSuggestions(keyword));
    }


    @GetMapping("/user/suggestions")
    public ResponseEntity<List<UserSuggestionDto>> suggestUsers(@RequestParam String keyword) {
        List<UserSuggestionDto> suggestions = searchService.findUsersByKeyword(keyword);
        return ResponseEntity.ok(suggestions);
    }

}
