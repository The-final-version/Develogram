package com.goorm.clonestagram.follow.mapper;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;

public class FollowMapper {

    // 팔로잉 목록: 내가 팔로우한 사람들 → followed 정보 기준
    public static FollowDto toFollowingDto(Follows f) {
        return FollowDto.builder()
                .id(f.getId())
                .followedId(f.getFollowed().getId())
                .followedName(f.getFollowed().getUsername())
                .followedProfileImg(f.getFollowed().getProfileimg())
                .createdAt(f.getCreatedAt())
                .build();
    }

    // 팔로워 목록: 나를 팔로우한 사람들 → follower 정보 기준
    public static FollowDto toFollowerDto(Follows f) {
        return FollowDto.builder()
                .id(f.getId())
                .followerId(f.getFollower().getId())
                .followerName(f.getFollower().getUsername())
                .followerProfileimg(f.getFollower().getProfileimg())
                .createdAt(f.getCreatedAt())
                .build();
    }

    public static FollowDto toSimpleDto(Follows f) {
        return FollowDto.builder()
                .id(f.getId())
                .followerId(f.getFollower().getId())
                .followedId(f.getFollowed().getId())
                .createdAt(f.getCreatedAt())
                .build();
    }

}
