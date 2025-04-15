package com.goorm.clonestagram.feed.controller;
import com.goorm.clonestagram.feed.dto.FeedResponseDto;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.util.TestSecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedService feedService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public FeedService feedService() {
            return mock(FeedService.class);
        }
    }

    @Test
    void F01C_마이피드_조회_성공() throws Exception {
        Page<FeedResponseDto> mockPage = new PageImpl<>(List.of(
                FeedResponseDto.builder()
                        .postId(1L)
                        .content("내용")
                        .userId(2L)
                        .build()
        ));

        when(feedService.getUserFeed(eq(1L), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/feeds")
                        .with(authentication(TestSecurityUtil.mockUserDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(1L))
                .andExpect(jsonPath("$.content[0].content").value("내용"));
    }


    @Test
    void F02C_전체피드_조회_성공() throws Exception {
        // given
        FeedResponseDto dto = new FeedResponseDto(1L, 10L, 1L, "name", "작성자", null, null);
        Page<FeedResponseDto> page = new PageImpl<>(List.of(dto));
        given(feedService.getAllFeed(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/feeds/all")
                        .with(authentication(TestSecurityUtil.mockUserDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(10L))
                .andExpect(jsonPath("$.content[0].content").value("작성자"));
    }


    @Test
    void F03C_팔로우피드_조회_성공() throws Exception {
        when(feedService.getFollowFeed(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/feeds/follow")
                        .with(authentication(TestSecurityUtil.mockUserDetails(1L))))
                .andExpect(status().isOk());
    }


    @Test
    void F04C_피드_삭제_성공() throws Exception {
        mockMvc.perform(delete("/feeds/seen")
                        .with(authentication(TestSecurityUtil.mockUserDetails(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "postIds": [1, 2]
                            }
                        """))
                .andExpect(status().isNoContent());

        verify(feedService).removeSeenFeeds(eq(1L), eq(List.of(1L, 2L)));
    }


    @Test
    void F05C_사용자_전체_피드_삭제_성공() throws Exception {
        mockMvc.perform(delete("/feeds/all")
                        .with(authentication(TestSecurityUtil.mockUserDetails(1L))))
                .andExpect(status().isNoContent());

        verify(feedService, times(1)).deleteAllByUser(1L);
    }
}
