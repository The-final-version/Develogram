package com.goorm.clonestagram.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FollowSearchServiceTest {

    @InjectMocks
    FollowSearchService followSearchService;

    @Mock
    FollowRepository followRepository;

    UserEntity user;

    @BeforeEach
    void init() {
        user = UserEntity.builder()
            .name("example")
            .email("example@test.com")
            .build();
    }


    @Test
    @DisplayName("searchFollowingByKeyword - 성공")
    void searchFollowingByKeywordTest() {
        // given
        Long userId = 1L;
        String keyword = "example";
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<UserEntity> mockPage = new PageImpl<>(List.of(user), pageRequest, 1);
        when(followRepository.findFollowingByKeyword(userId, keyword, pageRequest)).thenReturn(mockPage);

        SearchUserResDto result = followSearchService.searchFollowingByKeyword(userId, keyword, pageRequest);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getUserList().getContent().size());
        assertEquals("example", result.getUserList().getContent().getFirst().getName());

        verify(followRepository, times(1)).findFollowingByKeyword(userId, keyword, pageRequest);
    }


    @Test
    @DisplayName("searchFollowerByKeyword - 성공")
    void searchFollowerByKeywordTest() {
        Long userId = 1L;
        String keyword = "example";
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<UserEntity> mockPage = new PageImpl<>(List.of(user), pageRequest, 1);
        when(followRepository.findFollowerByKeyword(userId, keyword, pageRequest)).thenReturn(mockPage);

        SearchUserResDto result = followSearchService.searchFollowerByKeyword(userId, keyword, pageRequest);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getUserList().getContent().size());
        assertEquals("example", result.getUserList().getContent().getFirst().getName());

        verify(followRepository, times(1)).findFollowerByKeyword(userId, keyword, pageRequest);
    }

}
