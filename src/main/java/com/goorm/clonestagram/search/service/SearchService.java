package com.goorm.clonestagram.search.service;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.PostInfoDto;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;
import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.dto.UserSuggestionDto;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final HashTagRepository hashTagsRepository;
    private final PostHashTagRepository postHashTagRepository;
    /**
     * 유저 검색
     * - 검색어를 활용해 검색어와 부분 혹은 전체 일치하는 유저 반환
     *
     * @param rawKeyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 유저 리스트, 검색된 데이터 수
     */
    public SearchUserResDto searchUserByKeyword(String rawKeyword, Pageable pageable) {
        String keyword = Arrays.stream(rawKeyword.split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> "+" + s + "*")
                .collect(Collectors.joining(" "));
        //1. 유저의 이름으로 관련된 데이터 모두 반환, Like 사용
        Page<Users> users = userRepository.searchUserByFullText(keyword, pageable);

        //2. 반환을 위해 users를 UserProfileDto형태로 변환
        Page<UserProfileDto> userProfileDtos = users.map(user -> UserProfileDto.builder()
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .bio(user.getBio())
                        .profileimg(user.getProfileimg())
                        .build()
                );

        //3. UserProfileDto의 리스트와 users가 담고있는 전체 데이터 수를 SearchResDto에 넣고 반환
        return SearchUserResDto.builder()
                .userList(userProfileDtos)
                .totalCount(users.getTotalElements())
                .build();
    }

    /**
     *
     * 팔로우 검색
     * - 검색어를 활용해 검색어와 부분 혹은 일치하는 팔로우 반환
     *
     * @param userId 검색을 한 클라이언트의 식별자
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 팔로우 리스트, 검색된 데이터 수
     */
    public SearchUserResDto searchFollowingByKeyword(Long userId, @NotBlank String keyword, Pageable pageable) {
        //1. 유저의 팔로우 중에 keyword와 관련된 이름을 가지고 있는 데이터 모두 반환, Like 사용
        Page<Users> follows = followRepository.findFollowingByKeyword(userId, keyword, pageable);

        //2. 반환을 위해 users를 UserProfileDto형태로 변환
        Page<UserProfileDto> userProfileDtos = follows.map(user -> UserProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileimg(user.getProfileimg())
                .build()
        );

        //3. UserProfileDto의 리스트와 users가 담고있는 전체 데이터 수를 SearchResDto에 넣고 반환
        return SearchUserResDto.builder()
                .userList(userProfileDtos)
                .totalCount(follows.getTotalElements())
                .build();
    }

    /**
     *
     * 팔로워 검색
     * - 검색어를 활용해 검색어와 부분 혹은 전체 일치하는 팔로워 반환
     *
     * @param userId 검색을 한 클라이언트의 식별자
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 팔로워 리스트, 검색된 데이터 수
     */
    public SearchUserResDto searchFollowerByKeyword(Long userId, @NotBlank String keyword, Pageable pageable) {
        //1. 유저의 팔로워 중에 keyword와 관련된 이름을 가지고 있는 데이터 모두 반환, Like 사용
        Page<Users> follows = followRepository.findFollowerByKeyword(userId, keyword, pageable);

        //2. 반환을 위해 users를 UserProfileDto형태로 변환
        Page<UserProfileDto> userProfileDtos = follows.map(user -> UserProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileimg(user.getProfileimg())
                .build()
        );

        //3. UserProfileDto의 리스트와 users가 담고있는 전체 데이터 수를 SearchResDto에 넣고 반환
        return SearchUserResDto.builder()
                .userList(userProfileDtos)
                .totalCount(follows.getTotalElements())
                .build();
    }

    /**
     *
     * 피드 검색
     * - 검색어를 활용해 검색어와 부분 혹은 전체 일치하는 태그를 가진 피드 반환
     *
     * @param keyword 클라이언트의 검색 키워드
     * @param pageable 페이징 기능
     * @return 피드 리스트, 검색된 데이터 수
     */
    public SearchPostResDto searchHashTagByKeyword(@NotBlank String keyword, Pageable pageable) {
        //1. 게시글의 해시테그중에 keyword와 관련된 태그를 가지고 있는 데이터 모두 반환, Like 사용
        Page<Posts> tagPosts = postHashTagRepository.findPostsByHashtagKeyword(keyword, pageable);

//        2. 반환을 위해 users를 UserProfileDto형태로 변환
        Page<PostInfoDto> userProfileDtos = tagPosts.map(posts -> PostInfoDto.builder()
                .id(posts.getId())
                .userId(posts.getUser().getId())
                .content(posts.getContent())
                .mediaName(posts.getMediaName())
                .contentType(posts.getContentType())
                .createdAt(posts.getCreatedAt())
                .build()
        );

//        3. UserProfileDto의 리스트와 users가 담고있는 전체 데이터 수를 SearchResDto에 넣고 반환
        return SearchPostResDto.builder()
                .postList(userProfileDtos)
                .totalCount(tagPosts.getTotalElements())
                .build();
    }

    public List<HashtagSuggestionDto> getHashtagSuggestions(String keyword) {
        List<HashTags> tags = hashTagsRepository.findByTagContentContaining(keyword);
        return tags.stream()
                .map(tag -> new HashtagSuggestionDto(tag.getTagContent(), postHashTagRepository.countByHashTags(tag)))
                .collect(Collectors.toList());
    }

    public List<UserSuggestionDto> findUsersByKeyword(String keyword) {
        List<Users> users = userRepository.findByUsernameContainingIgnoreCase(keyword);
        return users.stream()
                .map(user -> new UserSuggestionDto(user.getId(), user.getUsername(), user.getProfileimg()))
                .collect(Collectors.toList());
    }

}
