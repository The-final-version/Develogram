package com.goorm.clonestagram.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.dto.UserSuggestionDto;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
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
class UserSearchServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserSearchService userSearchService;

    Users user;

    @BeforeEach
    void init() {
        user = Users.builder()
            .username("johndoe")
            .email("example@test.com")
            .build();
    }

    @Test
    @DisplayName("searchUserByKeyword - 성공")
    void searchUserByKeywordTest() {
        // given
        String keyword = "john doe";
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Users> mockPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.searchUserByFullText("+john* +doe*", pageable)).thenReturn(mockPage);

        // when
        SearchUserResDto result = userSearchService.searchUserByKeyword(keyword, pageable);

        // then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getUserList().getContent().size());
        assertEquals("johndoe", result.getUserList().getContent().getFirst().getUsername());
    }

    @Test
    @DisplayName("findUsersByKeyword - 성공")
    void findUsersByKeywordTest() {
        // given
        String keyword = "johnd";

        when(userRepository.findByUsernameContainingIgnoreCase(keyword)).thenReturn(List.of(user));

        // when
        List<UserSuggestionDto> result = userSearchService.findUsersByKeyword(keyword);

        // then
        assertEquals(1, result.size());
        assertEquals("johndoe", result.getFirst().getUsername());

        verify(userRepository, times(1)).findByUsernameContainingIgnoreCase(keyword);
    }

}


