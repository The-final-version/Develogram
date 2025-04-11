package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoServiceTest {

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private UserExternalQueryService userExternalQueryService;

    @Mock
    private HashTagRepository hashTagRepository;

    @Mock
    private PostHashTagRepository postHashTagRepository;

    @Mock
    private FeedService feedService;

    @Mock
    private SoftDeleteRepository softDeleteRepository;

    @InjectMocks
    private VideoService videoService;

    private UserEntity testUser;
    private Posts testPost;
    private VideoUploadReqDto uploadReqDto;
    private VideoUpdateReqDto updateReqDto;
    private HashTags testHashTag;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity(User.testMockUser(1L, "testUser"));

        testPost = new Posts();
        testPost.setId(1L);
        testPost.setContent("테스트 비디오 #테스트");
        testPost.setUser(testUser);
        testPost.setContentType(ContentType.VIDEO);
        testPost.setMediaName("test-video.mp4");

        uploadReqDto = new VideoUploadReqDto();
        uploadReqDto.setFile("test-video.mp4");
        uploadReqDto.setContent("테스트 비디오 #테스트");
        uploadReqDto.setHashTagList(Arrays.asList("테스트"));

        updateReqDto = new VideoUpdateReqDto();
        updateReqDto.setFile("updated-video.mp4");
        updateReqDto.setContent("수정된 비디오 #수정");
        updateReqDto.setHashTagList(Arrays.asList("수정"));

        testHashTag = new HashTags();
        testHashTag.setTagContent("테스트");
    }

    @Test
    void 비디오업로드_성공() {
        // given
        when(userExternalQueryService.findByIdAndDeletedIsFalse(anyLong())).thenReturn(testUser.toDomain());
        when(postsRepository.save(any(Posts.class))).thenReturn(testPost);
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);

        // when
        VideoUploadResDto result = videoService.videoUpload(uploadReqDto, 1L);

        // then
        assertNotNull(result);
        assertEquals(testPost.getContent(), result.getContent());
        assertEquals(testPost.getContentType(), result.getType());
        assertEquals(uploadReqDto.getHashTagList(), result.getHashTagList());
        verify(feedService).createFeedForFollowers(any(Posts.class));
    }

    @Test
    void 비디오업로드_실패_유저없음() {
        // given
        when(userExternalQueryService.findByIdAndDeletedIsFalse(anyLong()))
            .thenThrow(new IllegalArgumentException("유저가 존재하지 않습니다."));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpload(uploadReqDto, 1L));
    }

    @Test
    void 비디오업로드_실패_파일없음() {
        // given
        uploadReqDto.setFile(null);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpload(uploadReqDto, 1L));
    }

    @Test
    void 비디오수정_성공() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));
        when(postsRepository.save(any(Posts.class))).thenReturn(testPost);
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, updateReqDto, 1L);

        // then
        assertNotNull(result);
        assertEquals(updateReqDto.getContent(), result.getContent());
        assertEquals(updateReqDto.getHashTagList(), result.getHashTagList());
    }

    @Test
    void 비디오수정_실패_게시물없음() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpdate(1L, updateReqDto, 1L));
    }

    @Test
    void 비디오수정_실패_권한없음() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpdate(1L, updateReqDto, 2L));
    }

    @Test
    void 비디오삭제_성공() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        videoService.videoDelete(1L, 1L);

        // then
        assertTrue(testPost.getDeleted());
        assertNotNull(testPost.getDeletedAt());
        verify(feedService).deleteFeedsByPostId(1L);
        verify(softDeleteRepository).save(any(SoftDelete.class));
    }

    @Test
    void 비디오삭제_실패_게시물없음() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoDelete(1L, 1L));
    }

    @Test
    void 비디오삭제_실패_권한없음() {
        // given
        when(postsRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(testPost));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> videoService.videoDelete(1L, 2L));
    }
} 
