package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.service.HashtagService;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.util.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class VideoServiceTest {

    @Mock private PostsRepository postsRepository;
    @Mock private UserRepository userRepository;
    @Mock private HashtagService hashtagService;
    @Mock private FeedService feedService;
    @Mock private SoftDeleteRepository softDeleteRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private HashTagRepository hashTagRepository;
    @Mock private PostHashTagRepository postHashTagRepository;

    @InjectMocks
    private VideoService videoService;

    private Users testUser;
    private CustomUserDetails testUserDetails;
    private Posts testPost;
    private VideoUploadReqDto uploadReqDto;
    private VideoUpdateReqDto updateReqDto;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .id(1L)
                .email("testuser@example.com")
                .password("password")
                .build();
        testUserDetails = new CustomUserDetails(testUser);

        testPost = Posts.builder()
                .id(1L)
                .user(testUser)
                .content("테스트 비디오 #테스트")
                .mediaName("test-video.mp4")
                .contentType(ContentType.VIDEO)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();

        uploadReqDto = new VideoUploadReqDto();
        uploadReqDto.setFile("test-video.mp4");
        uploadReqDto.setContent("테스트 비디오 #테스트");
        uploadReqDto.setHashTagList(new HashSet<>(Arrays.asList("테스트")));

        updateReqDto = new VideoUpdateReqDto();
        updateReqDto.setFile("updated-video.mp4");
        updateReqDto.setContent("수정된 비디오 #수정");
        updateReqDto.setHashTagList(Arrays.asList("수정"));

        idempotencyKey = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("비디오 업로드 성공 (멱등성 적용)")
    void 비디오업로드_성공() {
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> ((Supplier<VideoUploadResDto>) invocation.getArgument(1)).get());
        when(userRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testUser));
        when(postsRepository.save(any(Posts.class))).thenReturn(testPost);
        doNothing().when(hashtagService).saveHashtags(eq(testPost), eq(uploadReqDto.getHashTagList()));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        VideoUploadResDto result = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails.getId(), idempotencyKey);

        assertNotNull(result);
        assertEquals(uploadReqDto.getContent(), result.getContent());
        assertEquals(ContentType.VIDEO, result.getType());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getPostId());
        assertEquals(uploadReqDto.getHashTagList().stream().toList(), result.getHashTagList());
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postsRepository).save(any(Posts.class));
        verify(hashtagService).saveHashtags(eq(testPost), eq(uploadReqDto.getHashTagList()));
        verify(feedService).createFeedForFollowers(eq(testPost));
    }

    @Test
    @DisplayName("비디오 업로드 멱등성 보장 - 동일 키로 재요청 시 동일 결과 반환")
    void 비디오업로드_멱등성_보장() {
        VideoUploadResDto firstResultDto = VideoUploadResDto.builder()
                .postId(testPost.getId())
                .content(testPost.getContent())
                .type(ContentType.VIDEO)
                .createdAt(testPost.getCreatedAt())
                .hashTagList(new ArrayList<>(uploadReqDto.getHashTagList()))
                .build();

        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                Supplier<VideoUploadResDto> operation = invocation.getArgument(1);
                when(userRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testUser));
                when(postsRepository.save(any(Posts.class))).thenReturn(testPost);
                doNothing().when(hashtagService).saveHashtags(any(Posts.class), anySet());
                doNothing().when(feedService).createFeedForFollowers(any(Posts.class));
                return operation.get();
            }).thenReturn(firstResultDto);

        VideoUploadResDto firstResult = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails.getId(), idempotencyKey);
        assertNotNull(firstResult);
        assertEquals(testPost.getId(), firstResult.getPostId());
        assertEquals(ContentType.VIDEO, firstResult.getType());
        assertNotNull(firstResult.getCreatedAt());
        assertEquals(firstResultDto.getHashTagList().stream().toList(), firstResult.getHashTagList());
        verify(idempotencyService, times(1)).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userRepository, times(1)).findByIdAndDeletedIsFalse(eq(1L));
        verify(postsRepository, times(1)).save(any(Posts.class));
        verify(hashtagService, times(1)).saveHashtags(any(Posts.class), anySet());
        verify(feedService, times(1)).createFeedForFollowers(any(Posts.class));

        VideoUploadResDto secondResult = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails.getId(), idempotencyKey);
        assertNotNull(secondResult);
        assertEquals(testPost.getId(), secondResult.getPostId());
        assertEquals(ContentType.VIDEO, secondResult.getType());
        assertNotNull(secondResult.getCreatedAt());
        assertEquals(firstResultDto.getHashTagList().stream().toList(), secondResult.getHashTagList());
        verify(idempotencyService, times(2)).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userRepository, times(1)).findByIdAndDeletedIsFalse(anyLong());
        verify(postsRepository, times(1)).save(any(Posts.class));
        verify(hashtagService, times(1)).saveHashtags(any(Posts.class), anySet());
        verify(feedService, times(1)).createFeedForFollowers(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 업로드 실패 - 유저 없음 (멱등성 적용)")
    void 비디오업로드_실패_유저없음() {
        IllegalArgumentException expectedException = new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userRepository.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(expectedException);
                return ((Supplier<VideoUploadResDto>)invocation.getArgument(1)).get();
            });
        assertThrows(RuntimeException.class, () -> videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails.getId(), idempotencyKey), "해당 유저를 찾을 수 없습니다.");
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postsRepository, never()).save(any(Posts.class));
        verify(hashtagService, never()).saveHashtags(any(Posts.class), anySet());
    }

    @Test
    @DisplayName("비디오 업로드 실패 - 파일 URL 없음 (멱등성 적용)")
    void 비디오업로드_실패_파일없음() {
        uploadReqDto.setFile(null);
        IllegalArgumentException expectedException = new IllegalArgumentException("Cloudinary 비디오 URL이 필요합니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testUser));
                return ((Supplier<VideoUploadResDto>)invocation.getArgument(1)).get();
            });
        assertThrows(RuntimeException.class, () -> videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails.getId(), idempotencyKey), "Cloudinary 비디오 URL이 필요합니다.");
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userRepository, atLeastOnce()).findByIdAndDeletedIsFalse(eq(1L));
        verify(postsRepository, never()).save(any(Posts.class));
        verify(hashtagService, never()).saveHashtags(any(Posts.class), anySet());
    }

    @Test
    @DisplayName("비디오 수정 성공")
    void 비디오수정_성공() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        when(postsRepository.save(any(Posts.class))).thenAnswer(invocation -> {
            Posts postArg = invocation.getArgument(0);
            ReflectionTestUtils.setField(postArg, "updatedAt", LocalDateTime.now());
            return postArg;
        });
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(2L, "수정"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateResDto result = videoService.videoUpdate(1L, updateReqDto, 1L);

        assertThat(result).isNotNull();
        assertThat(testPost.getContent()).isEqualTo(updateReqDto.getContent());
        assertThat(testPost.getMediaName()).isEqualTo(updateReqDto.getFile());
        assertThat(result.getHashTagList()).isEqualTo(updateReqDto.getHashTagList());
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postsRepository).save(eq(testPost));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(hashTagRepository, times(updateReqDto.getHashTagList().size())).findByTagContent(anyString());
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        verify(postHashTagRepository, times(updateReqDto.getHashTagList().size())).save(any(PostHashTags.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 버전 충돌 (낙관적 락)")
    void 비디오수정_실패_버전충돌() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(2L, "수정"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postsRepository.save(any(Posts.class))).thenThrow(new OptimisticLockingFailureException("버전 충돌!"));

        assertThrows(OptimisticLockingFailureException.class,
                () -> videoService.videoUpdate(1L, updateReqDto, 1L));

        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(hashTagRepository, atLeastOnce()).findByTagContent(anyString());
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 게시물 없음")
    void 비디오수정_실패_게시물없음() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpdate(1L, updateReqDto, 1L));
        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(hashTagRepository, never()).findByTagContent(anyString());
        verify(postsRepository, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 권한 없음")
    void 비디오수정_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).email("other@example.com").password("pw").build();
        testPost.setUser(otherUser);
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoUpdate(1L, updateReqDto, 1L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(hashTagRepository, never()).findByTagContent(anyString());
        verify(postsRepository, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 내용만 수정")
    void 비디오수정_성공_내용만() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        when(postsRepository.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoUpdateReqDto contentOnlyDto = new VideoUpdateReqDto();
        contentOnlyDto.setContent("내용만 수정된 비디오");
        VideoUpdateResDto result = videoService.videoUpdate(1L, contentOnlyDto, 1L);
        assertThat(result.getContent()).isEqualTo("내용만 수정된 비디오");
        assertThat(testPost.getMediaName()).isEqualTo("test-video.mp4");
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 파일만 수정")
    void 비디오수정_성공_파일만() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        when(postsRepository.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoUpdateReqDto fileOnlyDto = new VideoUpdateReqDto();
        fileOnlyDto.setFile("only-file-updated.mp4");
        VideoUpdateResDto result = videoService.videoUpdate(1L, fileOnlyDto, 1L);
        assertThat(testPost.getMediaName()).isEqualTo("only-file-updated.mp4");
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그만 수정 (빈 리스트)")
    void 비디오수정_성공_해시태그만_빈리스트() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(postsRepository.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoUpdateReqDto hashtagOnlyEmptyDto = new VideoUpdateReqDto();
        hashtagOnlyEmptyDto.setHashTagList(Collections.emptyList());
        VideoUpdateResDto result = videoService.videoUpdate(1L, hashtagOnlyEmptyDto, 1L);
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(result.getHashTagList()).isEmpty();
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository).deleteAllByPostsId(1L);
        verify(hashTagRepository, never()).findByTagContent(anyString());
        verify(hashTagRepository, never()).save(any(HashTags.class));
        verify(postHashTagRepository, never()).save(any(PostHashTags.class));
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그만 수정 (null 리스트)")
    void 비디오수정_성공_해시태그만_null리스트() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        VideoUpdateReqDto hashtagOnlyNullDto = new VideoUpdateReqDto();
        hashtagOnlyNullDto.setHashTagList(null);
        VideoUpdateResDto result = videoService.videoUpdate(1L, hashtagOnlyNullDto, 1L);
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(result.getHashTagList()).isNull();
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(hashTagRepository, never()).findByTagContent(anyString());
        verify(hashTagRepository, never()).save(any(HashTags.class));
        verify(postHashTagRepository, never()).save(any(PostHashTags.class));
        verify(postsRepository, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 변경 없음")
    void 비디오수정_성공_변경없음() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        VideoUpdateReqDto noChangeDto = new VideoUpdateReqDto();
        VideoUpdateResDto result = videoService.videoUpdate(1L, noChangeDto, 1L);
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(testPost.getMediaName()).isEqualTo("test-video.mp4");
        assertThat(result.getHashTagList()).isNull();
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(postsRepository, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그 리스트에 null/공백 포함")
    void 비디오수정_성공_해시태그_null_공백포함() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(hashTagRepository.findByTagContent("유효태그")).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(3L, "유효태그"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postsRepository.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        VideoUpdateReqDto mixedTagsDto = new VideoUpdateReqDto();
        mixedTagsDto.setHashTagList(Arrays.asList("유효태그", null, "", "  "));
        VideoUpdateResDto result = videoService.videoUpdate(1L, mixedTagsDto, 1L);
        assertThat(result).isNotNull();
        assertThat(result.getHashTagList()).isEqualTo(Arrays.asList("유효태그", null, "", "  "));
        verify(postsRepository).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository).deleteAllByPostsId(1L);
        verify(hashTagRepository, times(1)).findByTagContent("유효태그");
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        verify(postHashTagRepository, times(1)).save(any(PostHashTags.class));
        verify(hashTagRepository, never()).findByTagContent(null);
        verify(hashTagRepository, never()).findByTagContent("");
        verify(hashTagRepository, never()).findByTagContent("  ");
        verify(postsRepository).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 삭제 성공")
    void 비디오삭제_성공() {
        testPost.setUser(testUser);
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        doNothing().when(feedService).deleteFeedsByPostId(anyLong());

        videoService.videoDelete(1L, 1L);

        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        assertThat(testPost.isDeleted()).isTrue();
        assertThat(testPost.getDeletedAt()).isNotNull();
        verify(softDeleteRepository).save(any(SoftDelete.class));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(feedService).deleteFeedsByPostId(eq(1L));
    }

    @Test
    @DisplayName("비디오 삭제 실패 - 게시물 없음")
    void 비디오삭제_실패_게시물없음() {
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> videoService.videoDelete(1L, 1L));
        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(feedService, never()).deleteFeedsByPostId(anyLong());
    }

    @Test
    @DisplayName("비디오 삭제 실패 - 권한 없음")
    void 비디오삭제_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).email("other@example.com").password("pw").build();
        testPost.setUser(otherUser);
        when(postsRepository.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(Optional.of(testPost));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoDelete(1L, 1L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postsRepository).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(feedService, never()).deleteFeedsByPostId(anyLong());
    }
} 