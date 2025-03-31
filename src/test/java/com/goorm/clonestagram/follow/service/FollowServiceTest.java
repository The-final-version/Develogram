package com.goorm.clonestagram.follow.service;

import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
public class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    private User user1;
    private User user2;
    private Follows follow;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Test users
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setProfileimg("user1_profile.jpg");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setProfileimg("user2_profile.jpg");

        // Test follow relationship
        follow = new Follows(1L, user1, user2, LocalDateTime.now());
    }

    @Test
    @DisplayName("팔로우 목록 조회: 사용자가 팔로우한 유저 목록이 정상 조회된다")
    public void testGetFollowingList() {
        // Mock user repository
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user1));

        // Mock followRepository to return a list of follows
        when(followRepository.findByFromUserAndDeletedIsFalse(user1)).thenReturn(Collections.singletonList(follow));

        List<FollowDto> followingList = followService.getFollowingList(1L);

        assertNotNull(followingList);
        assertEquals(1, followingList.size());
        assertEquals("user1", followingList.get(0).getFromUsername());
        assertEquals("user2", followingList.get(0).getToUsername());
        assertEquals("user1_profile.jpg", followingList.get(0).getFromProfileimg());
        assertEquals("user2_profile.jpg", followingList.get(0).getToProfileImg());
    }

    @Test
    @DisplayName("팔로우 목록 조회: 사용자가 팔로우한 유저가 없는 경우 빈 리스트 반환")
    public void testGetFollowingListWithNoFollowings() {
        // Mock user repository
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user1));

        // Mock followRepository to return empty list
        when(followRepository.findByFromUserAndDeletedIsFalse(user1)).thenReturn(Collections.emptyList());

        List<FollowDto> followingList = followService.getFollowingList(1L);

        assertNotNull(followingList);
        assertTrue(followingList.isEmpty());
    }

    @Test
    @DisplayName("팔로워 목록 조회: 특정 사용자의 팔로워 목록이 정상 조회된다")
    public void testGetFollowerList() {
        // Mock user repository
        when(userRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(user2));

        // Mock followRepository to return a list of follows
        when(followRepository.findByToUserAndDeletedIsFalse(user2)).thenReturn(Collections.singletonList(follow));

        List<FollowDto> followerList = followService.getFollowerList(2L);

        assertNotNull(followerList);
        assertEquals(1, followerList.size());
        assertEquals("user1", followerList.get(0).getFromUsername());
        assertEquals("user2", followerList.get(0).getToUsername());
        assertEquals("user1_profile.jpg", followerList.get(0).getFromProfileimg());
        assertEquals("user2_profile.jpg", followerList.get(0).getToProfileImg());
    }

    @Test
    @DisplayName("팔로워 목록 조회: 팔로워가 없는 경우 빈 리스트 반환")
    public void testGetFollowerListWithNoFollowers() {
        // Mock user repository
        when(userRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(user2));

        // Mock followRepository to return empty list
        when(followRepository.findByToUserAndDeletedIsFalse(user2)).thenReturn(Collections.emptyList());

        List<FollowDto> followerList = followService.getFollowerList(2L);

        assertNotNull(followerList);
        assertTrue(followerList.isEmpty());
    }


    @Test
    @DisplayName("팔로우 토글: 팔로우 상태가 없을 경우 팔로우가 추가된다")
    public void testToggleFollow() {
        // Mock user repository
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(user2));

        // Mock followRepository to return empty result
        when(followRepository.findByFromUserAndToUser(user1, user2)).thenReturn(Optional.empty());

        // Perform the toggleFollow action (follow user2 from user1)
        followService.toggleFollow(1L, 2L);

        // Verify that followRepository save is called
        verify(followRepository, times(1)).save(any(Follows.class));

        // Now, mock the repository to simulate the user already follows user2
        when(followRepository.findByFromUserAndToUser(user1, user2)).thenReturn(Optional.of(follow));

        // Perform the toggleFollow action again (this should delete the follow)
        followService.toggleFollow(1L, 2L);

        // Verify that followRepository delete is called
        verify(followRepository, times(1)).delete(follow);
    }
    @Test
    @DisplayName("팔로우 토글 예외: 사용자가 자기 자신을 팔로우하려 할 경우 예외 발생")
    void toggleFollow_ShouldThrowException_WhenFollowSelf() {
        // given
        Long userId = 1L;

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                followService.toggleFollow(userId, userId)
        );

        assertEquals("자기 자신을 팔로우할 수 없습니다.", ex.getMessage());
    }
    @Test
    @DisplayName("팔로우 토글 예외: 팔로우하는 사용자가 존재하지 않을 경우 예외 발생")
    void toggleFollow_ShouldThrowException_WhenFromUserNotFound() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                followService.toggleFollow(1L, 2L)
        );

        assertEquals("팔로우하는 사용자를 찾을 수 없습니다.", ex.getMessage());
    }

    @Test
    @DisplayName("팔로우 토글 예외: 팔로우 받을 사용자가 존재하지 않을 경우 예외 발생")
    void toggleFollow_ShouldThrowException_WhenToUserNotFound() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                followService.toggleFollow(1L, 2L)
        );

        assertEquals("팔로우 받을 사용자를 찾을 수 없습니다.", ex.getMessage());
    }
    @Test
    @DisplayName("팔로우 목록 조회 예외: 사용자가 존재하지 않을 경우 예외 발생")
    void getFollowingList_ShouldThrowException_WhenUserNotFound() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                followService.getFollowingList(1L)
        );

        assertEquals("사용자를 찾을 수 없습니다.", ex.getMessage());
    }

    @Test
    @DisplayName("팔로워 목록 조회 예외: 사용자가 존재하지 않을 경우 예외 발생")
    void getFollowerList_ShouldThrowException_WhenUserNotFound() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                followService.getFollowerList(2L)
        );

        assertEquals("사용자를 찾을 수 없습니다.", ex.getMessage());
    }

}
