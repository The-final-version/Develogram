package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @InjectMocks
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserExternalQueryService userService;

    @Test
    void F01_팔로우_추가_또는_삭제_성공() {
        // given
        Long followerId = 1L;
        Long followedId = 2L;
        User follower = User.testMockUser(followerId, "팔로워");
        User followed = User.testMockUser(followedId, "팔로우");

        given(userService.findByIdAndDeletedIsFalse(followerId)).willReturn(follower);
        given(userService.findByIdAndDeletedIsFalse(followedId)).willReturn(followed);
        given(followRepository.findByFollowerAndFollowedWithLock(new UserEntity(follower), new UserEntity(followed))).willReturn(Optional.empty());

        // when
        followService.toggleFollow(followerId, followedId);

        // then
        verify(followRepository).save(any(Follows.class));
    }


    @Test
    void F02_자기자신_팔로우시_예외발생() {
        Long sameId = 1L;

        assertThrows(IllegalArgumentException.class, () -> followService.toggleFollow(sameId, sameId));
    }


    @Test
    void F03_팔로잉_목록_조회_성공() {
        Long userId = 1L;
        User user = User.testMockUser(userId, "유저A");
        User other = User.testMockUser(2L, "상대유저");

        Follows follow = new Follows(user, other);

        given(userService.findByIdAndDeletedIsFalse(userId)).willReturn(user);
        given(followRepository.findFollowedAllByFollower(UserEntity.from(user))).willReturn(List.of(follow));

        List<FollowDto> result = followService.getFollowingList(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFollowedId()).isEqualTo(2L);
    }


    @Test
    void F04_팔로워_목록_조회_성공() {
        Long userId = 1L;
        User user = User.testMockUser(userId, "팔로우대상");
        User follower = User.testMockUser(2L, "팔로워");

        Follows follow = new Follows(follower, user);

        given(userService.findByIdAndDeletedIsFalse(userId)).willReturn(user);
        given(followRepository.findFollowerAllByFollowed(UserEntity.from(user))).willReturn(List.of(follow));

        List<FollowDto> result = followService.getFollowerList(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFollowerId()).isEqualTo(2L);
    }


    @Test
    void F05_팔로잉_ID_목록_조회_성공() {
        Long userId = 1L;
        List<Long> followingIds = List.of(2L, 3L);

        given(followRepository.findFollowedIdsByFollowerId(userId)).willReturn(followingIds);

        List<Long> result = followService.findFollowingUserIdsByFollowerId(userId);

        assertThat(result).isEqualTo(followingIds);
    }


    @Test
    void F06_팔로워_ID_목록_조회_성공() {
        Long userId = 1L;
        List<Long> followerIds = List.of(4L, 5L);

        given(followRepository.findFollowerIdsByFollowedId(userId)).willReturn(followerIds);

        List<Long> result = followService.findFollowerIdsByFollowedId(userId);

        assertThat(result).isEqualTo(followerIds);
    }


    @Test
    void F07_팔로잉_목록_조회_유저없음_예외발생() {
        // given
        Long userId = 99L;
        given(userService.findByIdAndDeletedIsFalse(userId))
                .willThrow(new IllegalArgumentException("존재하지 않는 유저입니다."));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> followService.getFollowingList(userId));
    }


    @Test
    void F08_팔로워_목록_조회_유저없음_예외발생() {
        // given
        Long userId = 99L;
        given(userService.findByIdAndDeletedIsFalse(userId))
                .willThrow(new IllegalArgumentException("존재하지 않는 유저입니다."));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> followService.getFollowerList(userId));
    }


    @Test
    void F09_팔로잉_ID_목록_조회_빈리스트_반환() {
        Long userId = 1L;
        given(followRepository.findFollowedIdsByFollowerId(userId)).willReturn(List.of());

        List<Long> result = followService.findFollowingUserIdsByFollowerId(userId);

        assertThat(result).isEmpty();
    }


    @Test
    void F10_팔로잉_목록_조회_빈리스트_반환() {
        // given
        Long userId = 1L;
        User user = User.testMockUser(userId, "테스터");

        given(userService.findByIdAndDeletedIsFalse(userId)).willReturn(user);
        given(followRepository.findFollowedAllByFollower(UserEntity.from(user))).willReturn(List.of());

        // when
        List<FollowDto> result = followService.getFollowingList(userId);

        // then
        assertThat(result).isEmpty();
    }


    @Test
    void F11_팔로워_목록_조회_빈리스트_반환() {
        // given
        Long userId = 1L;
        User user = User.testMockUser(userId, "팔로우테스트");

        given(userService.findByIdAndDeletedIsFalse(userId)).willReturn(user);
        given(followRepository.findFollowerAllByFollowed(UserEntity.from(user))).willReturn(List.of());

        // when
        List<FollowDto> result = followService.getFollowerList(userId);

        // then
        assertThat(result).isEmpty();
    }
}
