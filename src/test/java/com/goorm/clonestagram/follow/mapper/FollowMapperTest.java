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
        Users follower = Users.builder().id(1L).build();
        Users followed = Users.builder()
                .id(2L)
                .username("followedUser")
                .profileimg("followed.jpg")
                .build();

        Follows follows = new Follows(10L, follower, followed, LocalDateTime.of(2024, 1, 1, 12, 0));

        // when
        FollowDto dto = FollowMapper.toFollowingDto(follows);

        // then
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getFollowedId()).isEqualTo(2L);
        assertThat(dto.getFollowedName()).isEqualTo("followedUser");
        assertThat(dto.getFollowedProfileImg()).isEqualTo("followed.jpg");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
    }


    @Test
    @DisplayName("M02_팔로워_DTO_변환_성공")
    void toFollowerDtoTest() {
        // given
        Users follower = Users.builder()
                .id(3L)
                .username("followerUser")
                .profileimg("follower.jpg")
                .build();
        Users followed = Users.builder().id(1L).build();

        Follows follows = new Follows(20L, follower, followed, LocalDateTime.of(2024, 2, 1, 9, 0));

        // when
        FollowDto dto = FollowMapper.toFollowerDto(follows);

        // then
        assertThat(dto.getId()).isEqualTo(20L);
        assertThat(dto.getFollowerId()).isEqualTo(3L);
        assertThat(dto.getFollowerName()).isEqualTo("followerUser");
        assertThat(dto.getFollowerProfileimg()).isEqualTo("follower.jpg");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 1, 9, 0));
    }

}
