package com.goorm.clonestagram.search;

import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;
import com.goorm.clonestagram.search.dto.SearchUserResDto;
import com.goorm.clonestagram.search.service.SearchService;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    @InjectMocks
    private SearchService searchService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private HashTagRepository hashTagRepository;

    @Mock
    private PostHashTagRepository postHashTagRepository;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pageable = PageRequest.of(0, 10, Sort.by("id").descending());
    }

    /**
     * 1. searchUserByKeyword() 정상 검색
     */
    @Test
    void 유저_검색_정상동작() {
        // given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .bio("테스트 소개")
                .profileimg("image.jpg")
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.searchUserByFullText(anyString(), eq(pageable))).thenReturn(userPage);

        // when
        SearchUserResDto result = searchService.searchUserByKeyword("test", pageable);

        // then
        assertEquals(1, result.getUserList().getTotalElements());
        assertEquals("testuser", result.getUserList().getContent().get(0).getUsername());
        verify(userRepository).searchUserByFullText(anyString(), eq(pageable));
    }

    /**
     * 3. searchFollowingByKeyword() 정상 동작
     */
    @Test
    void 팔로잉_검색_정상동작() {
        // given
        User user = User.builder().id(1L).username("followed").build();
        Page<User> page = new PageImpl<>(List.of(user));
        when(followRepository.findFollowingByKeyword(eq(1L), eq("test"), eq(pageable))).thenReturn(page);

        // when
        SearchUserResDto result = searchService.searchFollowingByKeyword(1L, "test", pageable);

        // then
        assertEquals(1, result.getUserList().getTotalElements());
        assertEquals("followed", result.getUserList().getContent().get(0).getUsername());
        verify(followRepository).findFollowingByKeyword(eq(1L), eq("test"), eq(pageable));
    }

    /**
     * 5. searchFollowerByKeyword() 정상 동작
     */
    @Test
    void 팔로워_검색_정상동작() {
        // given
        User user = User.builder().id(2L).username("follower").build();
        Page<User> page = new PageImpl<>(List.of(user));
        when(followRepository.findFollowerByKeyword(eq(1L), eq("test"), eq(pageable))).thenReturn(page);

        // when
        SearchUserResDto result = searchService.searchFollowerByKeyword(1L, "test", pageable);

        // then
        assertEquals("follower", result.getUserList().getContent().get(0).getUsername());
        verify(followRepository).findFollowerByKeyword(eq(1L), eq("test"), eq(pageable));
    }

    /**
     * 7. searchHashTagByKeyword() 정상 동작
     */
    @Test
    void 해시태그로_피드검색_정상동작() {
        // given
        Posts post = Posts.builder().id(1L).content("내용").mediaName("image.jpg").build();
        Page<Posts> postsPage = new PageImpl<>(List.of(post));
        when(postHashTagRepository.findPostsByHashtagKeyword(eq("test"), eq(pageable))).thenReturn(postsPage);

        // when
        SearchPostResDto result = searchService.searchHashTagByKeyword("test", pageable);

        // then
        assertEquals(1, result.getPostList().getTotalElements());
        assertEquals("내용", result.getPostList().getContent().get(0).getContent());
        verify(postHashTagRepository).findPostsByHashtagKeyword(eq("test"), eq(pageable));
    }

    /**
     * 9. getHashtagSuggestions() 정상 동작
     */
    @Test
    void 해시태그_추천_정상동작() {
        // given
        HashTags tag = new HashTags();
        tag.setId(1L);
        tag.setTagContent("테스트태그");
        when(hashTagRepository.findByTagContentContaining("tag")).thenReturn(List.of(tag));
        when(postHashTagRepository.countByHashTags(tag)).thenReturn(5L);

        // when
        List<HashtagSuggestionDto> result = searchService.getHashtagSuggestions("tag");

        // then
        assertEquals(1, result.size());
        assertEquals("테스트태그", result.get(0).getTagName());
        assertEquals(5L, result.get(0).getPostCount());
        verify(hashTagRepository).findByTagContentContaining("tag");
        verify(postHashTagRepository).countByHashTags(tag);
    }

    /**
     * 11. 유저 검색 - 빈 키워드 필터링 정상 동작
     */
    @Test
    void 유저검색시_빈_키워드_필터링() {
        // given
        String keyword = " test   ";
        when(userRepository.searchUserByFullText(anyString(), eq(pageable)))
                .thenReturn(Page.empty());

        // when
        searchService.searchUserByKeyword(keyword, pageable);

        // then
        verify(userRepository).searchUserByFullText(eq("+test*"), eq(pageable));
    }

    /**
     * 12. 팔로잉 검색 - 없는 결과 반환
     */
    @Test
    void 팔로잉검색_결과없음() {
        // given
        when(followRepository.findFollowingByKeyword(eq(1L), eq("none"), eq(pageable)))
                .thenReturn(Page.empty());

        // when
        SearchUserResDto result = searchService.searchFollowingByKeyword(1L, "none", pageable);

        // then
        assertEquals(0, result.getUserList().getTotalElements());
        verify(followRepository).findFollowingByKeyword(eq(1L), eq("none"), eq(pageable));
    }
}
