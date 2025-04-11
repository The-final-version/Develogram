package com.goorm.clonestagram.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.search.dto.HashtagSuggestionDto;
import com.goorm.clonestagram.search.dto.SearchPostResDto;
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
class HashTagSearchServiceTest {


    @Mock
    HashTagRepository hashTagRepository;

    @Mock
    PostHashTagRepository postHashTagRepository;

    @InjectMocks
    HashTagSearchService hashTagSearchService;

    Posts post;

    HashTags hashtag;

    @BeforeEach
    void init() {
        post = Posts.builder()
            .content("Sample post content")
            .user(new UserEntity("111","111","111"))
            .build();

        hashtag = new HashTags(1L, "example");
    }


    @Test
    @DisplayName("searchHashTagByKeyword - 성공")
    void searchHashTagByKeywordTest() {
        String keyword = "travel";
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Posts> mockPage = new PageImpl<>(List.of(post), pageable, 1);
        when(postHashTagRepository.findPostsByHashtagKeyword(keyword, pageable)).thenReturn(mockPage);

        SearchPostResDto result = hashTagSearchService.searchHashTagByKeyword(keyword, pageable);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getPostList().getContent().size());
        assertEquals(post.getId(), result.getPostList().getContent().getFirst().getId());

        verify(postHashTagRepository, times(1)).findPostsByHashtagKeyword(keyword, pageable);
    }

    @Test
    @DisplayName("getHashtagSuggestions - 성공")
    void getHashtagSuggestionsTest() {
        String keyword = "example..";

        when(hashTagRepository.findByTagContentContaining(keyword)).thenReturn(List.of(hashtag));
        when(postHashTagRepository.countByHashTags(hashtag)).thenReturn(1L);

        List<HashtagSuggestionDto> result = hashTagSearchService.getHashtagSuggestions(keyword);

        assertEquals(1, result.size());
        HashtagSuggestionDto dto = result.getFirst();
        assertEquals("example", dto.getTagName());
        assertEquals(1L, dto.getPostCount());

        verify(hashTagRepository, times(1)).findByTagContentContaining(keyword);
        verify(postHashTagRepository, times(1)).countByHashTags(hashtag);
    }

}
