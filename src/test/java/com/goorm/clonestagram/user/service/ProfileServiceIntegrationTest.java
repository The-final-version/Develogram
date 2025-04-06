package com.goorm.clonestagram.user.service;

import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.dto.UserProfileUpdateDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // application-test.properties를 사용하기 위해 설정
public class ProfileServiceIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private ImageService imageService;

    private Users testUser;

    @BeforeEach
    public void setUp() {
        // 테스트용 유저 생성
        testUser = Users.builder()
                .username("testUser")
                .email("test@example.com")
                .password("testPassword")
                .profileimg("default-profile-img.jpg")
                .bio("This is a test bio")
                .build();
        userRepository.save(testUser);
    }

    @Test
    public void testGetUserProfile() {
        // 테스트: 사용자 프로필 조회
        UserProfileDto userProfile = profileService.getUserProfile(testUser.getId());

        assertNotNull(userProfile);
        assertEquals(testUser.getId(), userProfile.getId());
        assertEquals(testUser.getUsername(), userProfile.getUsername());
        assertEquals(testUser.getProfileimg(), userProfile.getProfileimg());
    }

    @Test
    public void testUpdateUserProfile() {
        // 테스트: 사용자 프로필 수정
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setBio("Updated bio");

        // 이미지 업로드는 Mock 처리해야 할 수 있음, 테스트 목적상 필드값만 업데이트
        UserProfileDto updatedUser = profileService.updateUserProfile(testUser.getId(), updateDto);

        assertNotNull(updatedUser);
        assertEquals("updatedUser", updatedUser.getUsername());
        assertEquals("Updated bio", updatedUser.getBio());
    }

    @Test
    public void testUpdateUserProfileWithImage() {
        // 이미지 업로드가 포함된 프로필 업데이트 테스트
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setBio("Updated bio with image");

        // 실제 이미지 업로드는 Mock 처리할 수 있지만, 여기에선 이미지만 업데이트하는 테스트
        UserProfileDto updatedUser = profileService.updateUserProfile(testUser.getId(), updateDto);

        assertNotNull(updatedUser);
        assertEquals("userWithNewProfileImage", updatedUser.getUsername());
        assertEquals("Updated bio with image", updatedUser.getBio());
        // 실제 이미지 URL 확인 (업로드된 파일이 저장된 경로 등)
        assertTrue(updatedUser.getProfileimg().contains("default-profile-img.jpg"));
    }
}
