package com.goorm.clonestagram.search.dto;

import com.goorm.clonestagram.user.dto.UserProfileDto;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * 유저 조회 응답 위한 DTO
 * - totalCount, userList를 반환
 */
@Getter
@Builder
public class SearchUserResDto {

    private Long totalCount;
    private Page<UserProfileDto> userList;

    public static SearchUserResDto of(long totalElements, Page<UserProfileDto> userProfiles) {
        return SearchUserResDto.builder()
            .totalCount(totalElements)
            .userList(userProfiles)
            .build();
    }
}
