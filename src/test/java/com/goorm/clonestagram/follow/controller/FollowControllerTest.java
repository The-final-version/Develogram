package com.goorm.clonestagram.follow.controller;

import com.goorm.clonestagram.common.exception.GlobalExceptionHandler;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class FollowControllerTest {

    private MockMvc mockMvc;
    private FollowService followService;

    @BeforeEach
    void setup() {
        followService = mock(FollowService.class);
        FollowController controller = new FollowController(followService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("F01_팔로우_토글_성공")
    void F01_팔로우_토글_성공() throws Exception {
        mockMvc.perform(post("/follow/1/profile/2")
                        .accept(MediaType.TEXT_PLAIN) // ✅ 중요
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string("팔로우 상태가 변경되었습니다."));
    }

    @Test
    @DisplayName("F02_팔로우_토글_실패_자기자신")
    void F02_팔로우_토글_실패_자기자신() throws Exception {
        doThrow(new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다."))
                .when(followService).toggleFollow(1L, 1L);

        mockMvc.perform(post("/follow/1/profile/1")
                        .accept(MediaType.APPLICATION_JSON) // 예외는 JSON 응답 받도록
                        .characterEncoding("UTF-8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("자기 자신을 팔로우할 수 없습니다."));
    }

    @Test
    @DisplayName("F03_팔로워_목록_조회_성공")
    void F03_팔로워_목록_조회_성공() throws Exception {
        FollowDto dto = FollowDto.builder()
                .id(1L).followerId(2L).followedId(1L).followerName("user2").followedName("user1")
                .build();

        given(followService.getFollowerList(1L)).willReturn(List.of(dto));

        mockMvc.perform(get("/follow/1/profile/followers")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerId").value(2L));
    }

    @Test
    @DisplayName("F04_팔로잉_목록_조회_성공")
    void F04_팔로잉_목록_조회_성공() throws Exception {
        FollowDto dto = FollowDto.builder()
                .id(1L).followerId(1L).followedId(3L).followerName("user1").followedName("user3")
                .build();

        given(followService.getFollowingList(1L)).willReturn(List.of(dto));

        mockMvc.perform(get("/follow/1/profile/following")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followedId").value(3L));
    }

    @Test
    @DisplayName("F05_팔로워_목록_조회_실패")
    void F05_팔로워_목록_조회_실패() throws Exception {
        given(followService.getFollowerList(1L))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/follow/1/profile/followers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("DB 오류"));
    }

    @Test
    @DisplayName("F06_팔로잉_목록_조회_실패")
    void F06_팔로잉_목록_조회_실패() throws Exception {
        given(followService.getFollowingList(1L)).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/follow/1/profile/following")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("DB 오류"));
    }
}
