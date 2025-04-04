package com.goorm.clonestagram.follow.mapper;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.user.domain.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FollowMapperTest {

    @Test
    @DisplayName("M01_팔로잉_DTO_변환_성공")
    void toFollowingDtoTest() {
        // given
        Users follower = Users.builder()
                .id(1L)
                .username("팔로우하는유저")
                .profileimg("follower.jpg")
                .build();

        Users followed = Users.builder()
                .id(2L)
                .username("팔로우받는유저")
                .profileimg("followed.jpg")
                .build();

        Follows follows = new Follows(100L, follower, followed, LocalDateTime.of(2024, 1, 1, 12, 0));

        // when
        FollowDto dto = FollowMapper.toFollowingDto(follows);

        // then
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getFollowerId()).isEqualTo(1L);
        assertThat(dto.getFollowedId()).isEqualTo(2L);
        assertThat(dto.getFollowerName()).isEqualTo("팔로우하는유저");
        assertThat(dto.getFollowedName()).isEqualTo("팔로우받는유저");
        assertThat(dto.getFollowerProfileimg()).isEqualTo("follower.jpg");
        assertThat(dto.getFollowedProfileImg()).isEqualTo("followed.jpg");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
    }

    @Test
    @DisplayName("M02_팔로워_DTO_변환_성공")
    void toFollowerDtoTest() {
        // given
        Users follower = Users.builder()
                .id(2L)
                .username("팔로우하는유저")
                .profileimg("follower.jpg")
                .build();

        Users followed = Users.builder()
                .id(1L)
                .username("나")
                .profileimg("me.jpg")
                .build();

        Follows follows = new Follows(200L, follower, followed, LocalDateTime.of(2024, 2, 1, 10, 30));

        // when
        FollowDto dto = FollowMapper.toFollowerDto(follows);

        // then
        assertThat(dto.getId()).isEqualTo(200L);
        assertThat(dto.getFollowerId()).isEqualTo(2L);
        assertThat(dto.getFollowedId()).isEqualTo(1L);
        assertThat(dto.getFollowerName()).isEqualTo("팔로우하는유저");
        assertThat(dto.getFollowedName()).isEqualTo("나");
        assertThat(dto.getFollowerProfileimg()).isEqualTo("follower.jpg");
        assertThat(dto.getFollowedProfileImg()).isEqualTo("me.jpg");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 1, 10, 30));
    }
}
