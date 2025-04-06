package com.goorm.clonestagram.user.service;

import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.dto.UserProfileUpdateDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.service.PostService;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private PostService postService;

    @Mock
    private SoftDeleteRepository softDeleteRepository;

    @InjectMocks
    private ProfileService profileService;

    private Users testUser;
    private UserProfileUpdateDto updateDto;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = Users.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .profileimg("http://example.com/profile.jpg")
                .bio("안녕하세요. 테스트 사용자입니다.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        updateDto = new UserProfileUpdateDto();
        updateDto.setBio("수정된 자기소개");
        updateDto.setProfileImage("http://example.com/new-profile.jpg");
    }

    @Test
    public void getUserProfile_Success() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testUser));
        when(followRepository.getFollowerCountByFollowedId(anyLong())).thenReturn(10);
        when(followRepository.getFollowingCountByFollowerId(anyLong())).thenReturn(5);
        when(postsRepository.findAllByUserIdAndDeletedIsFalse(anyLong(), any(Pageable.class))).thenReturn(Page.empty());

        // when
        UserProfileDto profile = profileService.getUserProfile(1L);

        // then
        assertNotNull(profile);
        assertEquals(testUser.getId(), profile.getId());
        assertEquals(testUser.getUsername(), profile.getUsername());
        assertEquals(testUser.getEmail(), profile.getEmail());
        assertEquals(testUser.getProfileimg(), profile.getProfileimg());
        assertEquals(testUser.getBio(), profile.getBio());
        assertEquals(10, profile.getFollowerCount());
        assertEquals(5, profile.getFollowingCount());
    }

    @Test
    public void updateUserProfile_Success() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(Users.class))).thenReturn(testUser);

        // when
        UserProfileDto updatedProfile = profileService.updateUserProfile(1L, updateDto);

        // then
        assertNotNull(updatedProfile);
        assertEquals(testUser.getUsername(), updatedProfile.getUsername());
        assertEquals(updateDto.getBio(), updatedProfile.getBio());
        assertEquals(updateDto.getProfileImage(), updatedProfile.getProfileimg());
    }

    @Test
    public void deleteUserProfile_Success() {
        // given
        when(userRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(Users.class))).thenReturn(testUser);

        // when
        profileService.deleteUserProfile(1L);

        // then
        verify(userRepository).save(any(Users.class));
        assertTrue(testUser.getDeleted());
        assertNotNull(testUser.getDeletedAt());
    }
} 