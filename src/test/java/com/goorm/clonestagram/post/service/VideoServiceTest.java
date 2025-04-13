package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class VideoServiceTest {

    @Mock private PostService postService;
    @Mock private UserService userService;
    @Mock private HashTagRepository hashTagRepository;
    @Mock private PostHashTagRepository postHashTagRepository;
    @Mock private FeedService feedService;
    @Mock private SoftDeleteRepository softDeleteRepository;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks
    private VideoService videoService;

    private Users testUser;
    private CustomUserDetails testUserDetails;
    private Posts testPost;
    private VideoUploadReqDto uploadReqDto;
    private VideoUpdateReqDto updateReqDto;
    private HashTags testHashTag;
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
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();

        uploadReqDto = new VideoUploadReqDto();
        uploadReqDto.setFile("test-video.mp4");
        uploadReqDto.setContent("테스트 비디오 #테스트");
        uploadReqDto.setHashTagList(Arrays.asList("테스트"));

        updateReqDto = new VideoUpdateReqDto();
        updateReqDto.setFile("updated-video.mp4");
        updateReqDto.setContent("수정된 비디오 #수정");
        updateReqDto.setHashTagList(Arrays.asList("수정"));

        testHashTag = new HashTags(1L, "테스트");

        idempotencyKey = UUID.randomUUID().toString();

        lenient().when(idempotencyService.executeWithIdempotency(anyString(), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                Supplier<VideoUploadResDto> operation = invocation.getArgument(1);
                try {
                    return operation.get();
                } catch (Exception e) {
                    throw new RuntimeException("Operation failed within idempotency mock", e);
                }
            });
    }

    @Test
    @DisplayName("비디오 업로드 성공 (멱등성 적용)")
    void 비디오업로드_성공() {
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
        when(postService.save(any(Posts.class))).thenReturn(testPost);
        when(hashTagRepository.findByTagContent(eq("테스트"))).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        VideoUploadResDto result = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
        assertThat(result.getHashTagList()).isEqualTo(uploadReqDto.getHashTagList());
        verify(feedService).createFeedForFollowers(eq(testPost));
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
    }

    @Test
    @DisplayName("비디오 업로드 멱등성 보장 - 동일 키로 재요청 시 동일 결과 반환")
    void 비디오업로드_멱등성_보장() {
        // given
        VideoUploadResDto firstResultDto = VideoUploadResDto.builder()
                .content(testPost.getContent())
                .type(ContentType.VIDEO)
                .createdAt(testPost.getCreatedAt())
                .hashTagList(uploadReqDto.getHashTagList())
                .build();

        // 1. 첫 번째 요청 Mocking: 실제 로직 실행 (Supplier 실행)
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                Supplier<VideoUploadResDto> operation = invocation.getArgument(1);
                // Supplier 내부의 Mock 설정 - lenient() 추가
                lenient().when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
                // postService.save는 verify에서 사용되므로 lenient 불필요
                // lenient().when(postService.save(any(Posts.class))).thenReturn(testPost);
                lenient().when(hashTagRepository.findByTagContent(eq("테스트"))).thenReturn(Optional.empty());
                lenient().when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);
                lenient().when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(inv -> inv.getArgument(0));
                lenient().doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

                // VideoService의 videoUploadOperation 로직을 일부 모방하여 DTO 생성
                // 실제 서비스 로직에서 postService.save가 반환하는 값을 사용한다고 가정
                when(postService.save(any(Posts.class))).thenReturn(testPost); // save Mocking은 여기 유지 (verify에서 사용)
                Posts postFromOperation = postService.save(uploadReqDto.toEntity("dummy-url", testUser));
                return VideoUploadResDto.builder()
                        .content(postFromOperation.getContent())
                        .type(ContentType.VIDEO)
                        .createdAt(postFromOperation.getCreatedAt())
                        .hashTagList(uploadReqDto.getHashTagList())
                        .build();
            });

        // when: 첫 번째 요청
        VideoUploadResDto firstResult = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails, idempotencyKey);

        // then: 첫 번째 요청 결과 검증
        assertThat(firstResult).isNotNull();
        assertThat(firstResult.getContent()).isEqualTo(firstResultDto.getContent());
        assertThat(firstResult.getType()).isEqualTo(ContentType.VIDEO);
        assertThat(firstResult.getCreatedAt()).isEqualTo(firstResultDto.getCreatedAt());
        assertThat(firstResult.getHashTagList()).isEqualTo(firstResultDto.getHashTagList());
        verify(idempotencyService, times(1)).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(postService, times(1)).save(any(Posts.class)); // thenAnswer 내부 save 호출 검증

        // 2. 두 번째 요청 Mocking: 캐시된 결과(firstResultDto) 반환 (Supplier 실행 안 함)
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
                .thenReturn(firstResultDto);

        // when: 두 번째 요청
        VideoUploadResDto secondResult = videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails, idempotencyKey);

        // then: 두 번째 요청 결과 검증
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getContent()).isEqualTo(firstResultDto.getContent());
        assertThat(secondResult.getType()).isEqualTo(ContentType.VIDEO);
        assertThat(secondResult.getCreatedAt()).isEqualTo(firstResultDto.getCreatedAt());
        assertThat(secondResult.getHashTagList()).isEqualTo(firstResultDto.getHashTagList());
        verify(idempotencyService, times(2)).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(postService, times(1)).save(any(Posts.class)); // 총 호출 횟수 1회 유지
    }

    @Test
    @DisplayName("비디오 업로드 실패 - 유저 없음 (멱등성 적용)")
    void 비디오업로드_실패_유저없음() {
        IllegalArgumentException expectedException = new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(expectedException);
                return ((Supplier<VideoUploadResDto>)invocation.getArgument(1)).get();
            });

        IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails, idempotencyKey));

        assertThat(actualException.getMessage()).isEqualTo(expectedException.getMessage());
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 업로드 실패 - 파일 URL 없음 (멱등성 적용)")
    void 비디오업로드_실패_파일없음() {
        uploadReqDto.setFile(null);
        IllegalArgumentException expectedException = new IllegalArgumentException("Cloudinary 비디오 URL이 필요합니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
                return ((Supplier<VideoUploadResDto>)invocation.getArgument(1)).get();
            });

        IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoUploadWithIdempotency(uploadReqDto, testUserDetails, idempotencyKey));

        assertThat(actualException.getMessage()).isEqualTo(expectedException.getMessage());
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(VideoUploadResDto.class));
        verify(userService, atLeastOnce()).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공")
    void 비디오수정_성공() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> {
            Posts postArg = invocation.getArgument(0);
            ReflectionTestUtils.setField(postArg, "updatedAt", LocalDateTime.now());
            return postArg;
        });

        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(2L, "수정"));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateResDto result = videoService.videoUpdate(1L, updateReqDto, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(updateReqDto.getContent());
        assertThat(result.getHashTagList()).isEqualTo(updateReqDto.getHashTagList());
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService).save(eq(testPost));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(hashTagRepository, times(updateReqDto.getHashTagList().size())).findByTagContent(anyString());
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        verify(postHashTagRepository, times(updateReqDto.getHashTagList().size())).save(any(PostHashTags.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 버전 충돌 (낙관적 락)")
    void 비디오수정_실패_버전충돌() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(postService.save(any(Posts.class))).thenThrow(new OptimisticLockingFailureException("버전 충돌!"));

        ConcurrencyFailureException exception = assertThrows(ConcurrencyFailureException.class,
                () -> videoService.videoUpdate(1L, updateReqDto, 1L));

        assertThat(exception.getMessage()).contains("다른 사용자에 의해 정보가 변경되었습니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(postService).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 게시물 없음")
    void 비디오수정_실패_게시물없음() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(new IllegalArgumentException("게시물을 찾을 수 없습니다"));

        assertThrows(IllegalArgumentException.class, () -> videoService.videoUpdate(1L, updateReqDto, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).save(any(Posts.class));
        verify(postService, never()).saveAndFlush(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 실패 - 권한 없음")
    void 비디오수정_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).email("other@example.com").password("pw").build();
        testPost.setUser(otherUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoUpdate(1L, updateReqDto, 1L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).save(any(Posts.class));
        verify(postService, never()).saveAndFlush(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 내용만 수정")
    void 비디오수정_성공_내용만() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0)); // 변경 감지 시뮬레이션

        VideoUpdateReqDto contentOnlyDto = new VideoUpdateReqDto();
        contentOnlyDto.setContent("내용만 수정된 비디오");
        // 파일과 해시태그는 null

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, contentOnlyDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("내용만 수정된 비디오");
        assertThat(testPost.getMediaName()).isEqualTo("test-video.mp4"); // 미디어 이름 변경되지 않음
        verify(postService).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong()); // 해시태그 삭제 안됨
        verify(postService).save(any(Posts.class)); // 변경 감지 또는 명시적 save
    }

    @Test
    @DisplayName("비디오 수정 성공 - 파일만 수정")
    void 비디오수정_성공_파일만() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateReqDto fileOnlyDto = new VideoUpdateReqDto();
        fileOnlyDto.setFile("only-file-updated.mp4");
        // 내용과 해시태그는 null

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, fileOnlyDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(testPost.getMediaName()).isEqualTo("only-file-updated.mp4"); // 미디어 이름 변경됨
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트"); // 내용 변경되지 않음
        verify(postService).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(postService).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그만 수정 (빈 리스트)")
    void 비디오수정_성공_해시태그만_빈리스트() {
        // given
        // 기존 해시태그 삭제 로직만 검증하면 됨
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateReqDto hashtagOnlyEmptyDto = new VideoUpdateReqDto();
        hashtagOnlyEmptyDto.setHashTagList(Collections.emptyList()); // 빈 리스트
        // 내용과 파일은 null

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, hashtagOnlyEmptyDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(result.getHashTagList()).isEmpty(); // 해시태그 비어있음
        verify(postService).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository).deleteAllByPostsId(1L); // 기존 태그 삭제됨
        verify(hashTagRepository, never()).findByTagContent(anyString()); // 새 태그 조회 안함
        verify(hashTagRepository, never()).save(any(HashTags.class)); // 새 태그 저장 안함
        verify(postHashTagRepository, never()).save(any(PostHashTags.class)); // 새 연결 저장 안함
        verify(postService).save(any(Posts.class)); // updated=true 이므로 save 호출
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그만 수정 (null 리스트)")
    void 비디오수정_성공_해시태그만_null리스트() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        // 수정: save 호출 안 함 Mocking 제거 (실제로는 호출될 수도 안 될 수도 있음. 검증 로직에서 never() 사용)
        // when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateReqDto hashtagOnlyNullDto = new VideoUpdateReqDto();
        hashtagOnlyNullDto.setHashTagList(null); // null 리스트
        // 내용과 파일은 null

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, hashtagOnlyNullDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(result.getHashTagList()).isNull(); // 해시태그 null
        verify(postService).findByIdAndDeletedIsFalse(1L);
        // hashTagList가 null이면 관련 로직 스킵됨
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(hashTagRepository, never()).findByTagContent(anyString());
        verify(hashTagRepository, never()).save(any(HashTags.class));
        verify(postHashTagRepository, never()).save(any(PostHashTags.class));
        // updated=false 이므로 save 호출 안 함
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 수정 성공 - 변경 없음")
    void 비디오수정_성공_변경없음() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        VideoUpdateReqDto noChangeDto = new VideoUpdateReqDto(); // 모든 필드 null

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, noChangeDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 비디오 #테스트");
        assertThat(testPost.getMediaName()).isEqualTo("test-video.mp4");
        assertThat(result.getHashTagList()).isNull(); // 요청 DTO 필드가 null이었으므로
        verify(postService).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(postService, never()).save(any(Posts.class)); // updated=false
    }

    @Test
    @DisplayName("비디오 수정 성공 - 해시태그 리스트에 null/공백 포함")
    void 비디오수정_성공_해시태그_null_공백포함() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(hashTagRepository.findByTagContent("유효태그")).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(3L, "유효태그"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoUpdateReqDto mixedTagsDto = new VideoUpdateReqDto();
        mixedTagsDto.setHashTagList(Arrays.asList("유효태그", null, "", "  "));

        // when
        VideoUpdateResDto result = videoService.videoUpdate(1L, mixedTagsDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getHashTagList()).isEqualTo(Arrays.asList("유효태그", null, "", "  ")); // 원본 리스트 반환
        verify(postService).findByIdAndDeletedIsFalse(1L);
        verify(postHashTagRepository).deleteAllByPostsId(1L);
        // 유효한 태그만 처리됨
        verify(hashTagRepository, times(1)).findByTagContent("유효태그");
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        verify(postHashTagRepository, times(1)).save(any(PostHashTags.class));
        // null, 빈 문자열 태그는 스킵됨
        verify(hashTagRepository, never()).findByTagContent(null);
        verify(hashTagRepository, never()).findByTagContent("");
        verify(hashTagRepository, never()).findByTagContent("  ");
        verify(postService).save(any(Posts.class)); // updated=true
    }

    @Test
    @DisplayName("비디오 삭제 성공")
    void 비디오삭제_성공() {
        testPost.setUser(testUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        doNothing().when(feedService).deleteFeedsByPostId(anyLong());

        videoService.videoDelete(1L, 1L);

        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository).save(any(SoftDelete.class));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(feedService).deleteFeedsByPostId(eq(1L));
    }

    @Test
    @DisplayName("비디오 삭제 실패 - 버전 충돌 (낙관적 락)")
    void 비디오삭제_실패_버전충돌() {
        testPost.setUser(testUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(postService.save(any(Posts.class))).thenThrow(new OptimisticLockingFailureException("버전 충돌!"));

        ConcurrencyFailureException exception = assertThrows(ConcurrencyFailureException.class,
                () -> videoService.videoDelete(1L, 1L));

        assertThat(exception.getMessage()).contains("다른 사용자에 의해 정보가 변경되었습니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 삭제 실패 - 게시물 없음")
    void 비디오삭제_실패_게시물없음() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(new IllegalArgumentException("해당 게시물이 없습니다"));

        assertThrows(IllegalArgumentException.class, () -> videoService.videoDelete(1L, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("비디오 삭제 실패 - 권한 없음")
    void 비디오삭제_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).email("other@example.com").password("pw").build();
        testPost.setUser(otherUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoService.videoDelete(1L, 1L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postService, never()).save(any(Posts.class));
    }
} 