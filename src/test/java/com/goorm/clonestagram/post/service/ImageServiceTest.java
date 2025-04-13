package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.ImageUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;
import com.goorm.clonestagram.util.CustomUserDetails;
import jakarta.persistence.OptimisticLockException;
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

//Todo 로그인 구현 완료 후 유저를 포함하여 테스트 필요
@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    // --- Mock 객체 선언 ---
    @Mock private PostService postService;
    @Mock private UserService userService;
    @Mock private HashTagRepository hashTagRepository;
    @Mock private PostHashTagRepository postHashTagRepository;
    @Mock private FeedService feedService;
    @Mock private SoftDeleteRepository softDeleteRepository;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks
    private ImageService imageService;

    // --- 테스트 데이터 ---
    private Users testUser;
    private CustomUserDetails testUserDetails;
    private Posts testPost;
    private ImageUploadReqDto imageUploadReqDto;
    private ImageUpdateReqDto imageUpdateReqDto;
    private HashTags testHashTag;
    private String idempotencyKey;

    @BeforeEach
    void setUp(){
        testUser = Users.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .password("1234")
                .build();
        testUserDetails = new CustomUserDetails(testUser);

        testPost = Posts.builder()
                .id(1L)
                .content("테스트 내용 #해시태그")
                .mediaName("image.jpg")
                // .contentType(ContentType.IMAGE) // 필요시
                .user(testUser)
                .version(0L) // 초기 버전
                .createdAt(LocalDateTime.now())
                .build();

        imageUploadReqDto = new ImageUploadReqDto();
        imageUploadReqDto.setFile("http://example.com/image.jpg"); // URL 방식으로 변경 가정
        imageUploadReqDto.setContent("테스트 내용 #해시태그");
        imageUploadReqDto.setHashTagList(Arrays.asList("해시태그"));

        imageUpdateReqDto = new ImageUpdateReqDto();
        imageUpdateReqDto.setFile("http://example.com/new-image.jpg");
        imageUpdateReqDto.setContent("수정된 내용 #수정태그");
        imageUpdateReqDto.setHashTagList(Arrays.asList("수정태그"));

        testHashTag = new HashTags(1L, "해시태그");
        idempotencyKey = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("이미지 업로드 성공 (멱등성 적용)")
    void 파일업로드() {
        // given
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class)))
            .thenAnswer(invocation -> ((Supplier<ImageUploadResDto>)invocation.getArgument(1)).get());

        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
        when(postService.save(any(Posts.class))).thenReturn(testPost);
        when(hashTagRepository.findByTagContent(eq("해시태그"))).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        // when
        ImageUploadResDto result = imageService.imageUploadWithIdempotency(imageUploadReqDto, testUserDetails, idempotencyKey);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
        assertThat(result.getMediaName()).isEqualTo(testPost.getMediaName()); // mediaName 검증 추가
        assertThat(result.getHashTagList()).isEqualTo(imageUploadReqDto.getHashTagList());

        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class));
    }

    @Test
    @DisplayName("이미지 업로드 성공 (멱등성 미적용)")
    void 이미지_업로드_성공_멱등성_미적용() throws Exception {
        // given
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
        when(postService.save(any(Posts.class))).thenReturn(testPost);
        when(hashTagRepository.findByTagContent(eq("해시태그"))).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(testHashTag);
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        // when
        ImageUploadResDto result = imageService.imageUpload(imageUploadReqDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
        assertThat(result.getMediaName()).isEqualTo(testPost.getMediaName());
        assertThat(result.getHashTagList()).isEqualTo(imageUploadReqDto.getHashTagList());

        verify(userService).findByIdAndDeletedIsFalse(1L);
        verify(postService).save(any(Posts.class));
        verify(hashTagRepository).findByTagContent("해시태그");
        verify(hashTagRepository).save(any(HashTags.class));
        verify(postHashTagRepository).save(any(PostHashTags.class));
        verify(feedService).createFeedForFollowers(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 업로드 실패 - 유저 없음 (멱등성 적용)")
    void 해당_유저를_찾을_수_없습니다() {
        // given
        IllegalArgumentException expectedException = new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(expectedException);
                return ((Supplier<ImageUploadResDto>)invocation.getArgument(1)).get();
            });

        // when & then
        IllegalArgumentException actualException = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageUploadWithIdempotency(imageUploadReqDto, testUserDetails, idempotencyKey));

        assertThat(actualException.getMessage()).isEqualTo(expectedException.getMessage());
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class));
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 정보 수정 성공")
    void 파일업데이트() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        // 해시태그 관련 Mocking
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenAnswer(invocation -> {
            HashTags tagArg = invocation.getArgument(0);
            // 수정: Builder 대신 생성자 사용 및 ID 할당 시뮬레이션
            // 실제 ID는 DB에서 자동 생성되지만, 테스트에서는 명시적으로 설정 필요
            return new HashTags(2L, tagArg.getTagContent()); // 예시 ID 2L 할당
        });
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));

        // saveAndFlush Mocking 시 updatedAt 업데이트 시뮬레이션
        doAnswer(invocation -> {
            Posts postArg = invocation.getArgument(0);
            ReflectionTestUtils.setField(postArg, "updatedAt", LocalDateTime.now());
            return postArg;
        }).when(postService).saveAndFlush(any(Posts.class));

        // when
        // 수정: 요청 DTO 필드명 수정 (tags -> hashTagList)
        ImageUpdateReqDto reqDto = new ImageUpdateReqDto();
        reqDto.setFile("http://example.com/new-image.jpg");
        reqDto.setContent("수정된 내용 #수정태그");
        reqDto.setHashTagList(List.of("수정태그")); // hashTagList 사용

        ImageUpdateResDto result = imageService.imageUpdate(1L, reqDto, 1L);

        // then
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        // 수정: 요청 DTO 필드명 수정 (tags -> hashTagList)
        verify(hashTagRepository, times(reqDto.getHashTagList().size())).findByTagContent(anyString());
        // save 호출 횟수 검증 (새 태그가 1개이므로 1번 호출)
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        // 수정: 요청 DTO 필드명 수정 (tags -> hashTagList)
        verify(postHashTagRepository, times(reqDto.getHashTagList().size())).save(any(PostHashTags.class));
        verify(postService).saveAndFlush(any(Posts.class));

        // 수정: 응답 DTO 필드명 수정 (imageUrl -> 없음, tags -> hashTagList)
        // assertThat(result.getImageUrl()).isEqualTo(reqDto.getFile()); // ImageUrl 필드 없음
        assertThat(result.getContent()).isEqualTo(reqDto.getContent());
        assertThat(result.getHashTagList()).isEqualTo(reqDto.getHashTagList()); // hashTagList 사용
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미지 수정 실패 - 게시물 없음")
    void 게시물을_찾을_수_없습니다_수정시() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(new IllegalArgumentException("게시물을 찾을 수 없습니다"));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
    }

    @Test
    @DisplayName("이미지 수정 실패 - 권한 없음")
    void 이미지수정_실패_권한없음() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageUpdate(1L, imageUpdateReqDto, 2L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
    }

    @Test
    @DisplayName("이미지 수정 실패 - 버전 충돌 (낙관적 락)")
    void 이미지수정_실패_버전충돌() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        // 추가: NPE 방지를 위한 HashTagRepository Mocking
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Service 내부에서 saveAndFlush 호출 시 예외 발생 Mocking
        doThrow(new OptimisticLockingFailureException("버전 충돌!"))
            .when(postService).saveAndFlush(any(Posts.class));

        // when & then
        ConcurrencyFailureException exception = assertThrows(ConcurrencyFailureException.class,
                () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L));

        assertThat(exception.getMessage()).contains("다른 사용자에 의해 정보가 변경되었습니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        // 추가: 해시태그 관련 Mock 검증 (필요시 주석 해제)
        // verify(hashTagRepository, times(imageUpdateReqDto.getTags().size())).findByTagContent(anyString());
        // verify(hashTagRepository, times(imageUpdateReqDto.getTags().size())).save(any(HashTags.class));
        // verify(postHashTagRepository, times(imageUpdateReqDto.getTags().size())).save(any(PostHashTags.class));
        verify(postService).saveAndFlush(any(Posts.class)); // saveAndFlush 호출 검증 추가
    }

    @Test
    @DisplayName("이미지 삭제 성공")
    void 파일삭제() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        doNothing().when(feedService).deleteFeedsByPostId(eq(1L));
        when(postService.save(any(Posts.class))).thenReturn(testPost);

        // when
        imageService.imageDelete(1L, 1L);

        // then
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(softDeleteRepository).save(any(SoftDelete.class));
        verify(feedService).deleteFeedsByPostId(eq(1L));
        verify(postService).save(eq(testPost));
    }

    @Test
    @DisplayName("이미지 삭제 실패 - 게시물 없음")
    void 게시물을_찾을_수_없습니다_삭제시() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenThrow(new IllegalArgumentException("해당 게시물이 없습니다"));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> imageService.imageDelete(1L, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
    }

    @Test
    @DisplayName("이미지 삭제 실패 - 권한 없음")
    void 이미지삭제_실패_권한없음() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageDelete(1L, 2L));

        assertThat(exception.getMessage()).isEqualTo("권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
    }

    @Test
    @DisplayName("이미지 삭제 실패 - 버전 충돌 (낙관적 락)")
    void 이미지삭제_실패_버전충돌() {
        // given
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        // 수정: imageDelete에서 호출하는 save 메서드에서 예외 발생 Mocking
         doThrow(new OptimisticLockingFailureException("버전 충돌!"))
             .when(postService).save(any(Posts.class));

        // when & then
        ConcurrencyFailureException exception = assertThrows(ConcurrencyFailureException.class,
                () -> imageService.imageDelete(1L, 1L));

        assertThat(exception.getMessage()).contains("다른 사용자에 의해 정보가 변경되었습니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        // 수정: save 호출 검증
        verify(postService).save(any(Posts.class));
    }
}

