package com.goorm.clonestagram.search.service;

import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowSearchService {


    private final FollowRepository followRepository;

    /**
     * 팔로우 검색
     * - 검색어를 활용해 검색어와 부분 혹은 일치하는 팔로우 반환
     *
     * @param userId   검색을 한 클라이언트의 식별자
     * @param keyword  클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 팔로우 리스트, 검색된 데이터 수
     */
    @Transactional(readOnly = true)
    public SearchUserResDto searchFollowingByKeyword(Long userId, @NotBlank String keyword,
                                                     Pageable pageable) {
        Page<Users> follows = followRepository.findFollowingByKeyword(userId, keyword, pageable);
        Page<UserProfileDto> userProfiles = follows.map(UserProfileDto::fromEntity);

        return SearchUserResDto.of(follows.getTotalElements(), userProfiles);
    }

    /**
     * 팔로워 검색
     * - 검색어를 활용해 검색어와 부분 혹은 전체 일치하는 팔로워 반환
     *
     * @param userId   검색을 한 클라이언트의 식별자
     * @param keyword  클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 팔로워 리스트, 검색된 데이터 수
     */
    @Transactional(readOnly = true)
    public SearchUserResDto searchFollowerByKeyword(Long userId, @NotBlank String keyword,
                                                    Pageable pageable) {
        Page<Users> follows = followRepository.findFollowerByKeyword(userId, keyword, pageable);

        Page<UserProfileDto> userProfiles = follows.map(UserProfileDto::fromEntity);

        return SearchUserResDto.of(follows.getTotalElements(), userProfiles);
    }

}
