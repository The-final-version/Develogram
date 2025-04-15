package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.service.HashtagService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
    @Mock private HashtagService hashtagService;

    @InjectMocks
    private ImageService imageService;

    // --- 테스트 데이터 ---
    private Users testUser;
    private CustomUserDetails testUserDetails;
    private Posts testPost;
    private ImageUploadReqDto imageUploadReqDto;
    private ImageUpdateReqDto imageUpdateReqDto;
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
                .user(testUser)
                .version(0L) // 초기 버전
                .createdAt(LocalDateTime.now())
                .build();

        imageUploadReqDto = new ImageUploadReqDto();
        imageUploadReqDto.setFile("http://example.com/image.jpg"); // URL 방식으로 변경 가정
        imageUploadReqDto.setContent("테스트 내용 #해시태그");
        imageUploadReqDto.setHashTagList(new HashSet<>(Arrays.asList("해시태그")));

        imageUpdateReqDto = new ImageUpdateReqDto();
        imageUpdateReqDto.setFile("http://example.com/new-image.jpg");
        imageUpdateReqDto.setContent("수정된 내용 #수정태그");
        imageUpdateReqDto.setHashTagList(Arrays.asList("수정태그"));

        idempotencyKey = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("이미지 업로드 성공 (멱등성 적용)")
    void 파일업로드() {
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class)))
            .thenAnswer(invocation -> ((Supplier<ImageUploadResDto>)invocation.getArgument(1)).get());
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
        when(postService.save(any(Posts.class))).thenReturn(testPost);
        doNothing().when(hashtagService).saveHashtags(eq(testPost), eq(imageUploadReqDto.getHashTagList()));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        ImageUploadResDto result = imageService.imageUploadWithIdempotency(imageUploadReqDto, testUserDetails.getId(), idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
        assertThat(result.getMediaName()).isEqualTo(testPost.getMediaName());
        assertThat(result.getPostId()).isNotNull();
        assertThat(result.getHashTagList()).containsExactlyInAnyOrderElementsOf(imageUploadReqDto.getHashTagList());
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class));
        verify(userService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService).save(any(Posts.class));
        verify(hashtagService).saveHashtags(eq(testPost), eq(imageUploadReqDto.getHashTagList()));
        verify(feedService).createFeedForFollowers(eq(testPost));
    }

    @Test
    @DisplayName("이미지 업로드 성공 (멱등성 미적용)")
    void 이미지_업로드_성공_멱등성_미적용() throws Exception {
        when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testUser);
        when(postService.save(any(Posts.class))).thenReturn(testPost);
        doNothing().when(hashtagService).saveHashtags(eq(testPost), eq(imageUploadReqDto.getHashTagList()));
        doNothing().when(feedService).createFeedForFollowers(any(Posts.class));

        ImageUploadResDto result = imageService.imageUpload(imageUploadReqDto, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(testPost.getContent());
        assertThat(result.getMediaName()).isEqualTo(testPost.getMediaName());
        assertThat(result.getPostId()).isNotNull();
        assertThat(result.getHashTagList()).isEqualTo(new ArrayList<>(imageUploadReqDto.getHashTagList()));

        verify(userService).findByIdAndDeletedIsFalse(1L);
        verify(postService).save(any(Posts.class));
        verify(hashtagService).saveHashtags(eq(testPost), eq(imageUploadReqDto.getHashTagList()));
        verify(feedService).createFeedForFollowers(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 업로드 실패 - 유저 없음 (멱등성 적용)")
    void 해당_유저를_찾을_수_없습니다() {
        IllegalArgumentException expectedException = new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        when(idempotencyService.executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class)))
            .thenAnswer(invocation -> {
                when(userService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(null);
                return ((Supplier<ImageUploadResDto>)invocation.getArgument(1)).get();
            });
        assertThrows(RuntimeException.class, () -> imageService.imageUploadWithIdempotency(imageUploadReqDto, testUserDetails.getId(), idempotencyKey), "해당 유저를 찾을 수 없습니다.");
        verify(idempotencyService).executeWithIdempotency(eq(idempotencyKey), any(Supplier.class), eq(ImageUploadResDto.class));
        verify(userService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).save(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 정보 수정 성공")
    void 파일업데이트() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(2L, "수정태그"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Posts postArg = invocation.getArgument(0);
            ReflectionTestUtils.setField(postArg, "updatedAt", LocalDateTime.now());
            return postArg;
        }).when(postService).saveAndFlush(any(Posts.class));

        ImageUpdateResDto result = imageService.imageUpdate(1L, imageUpdateReqDto, 1L);

        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(hashTagRepository, times(imageUpdateReqDto.getHashTagList().size())).findByTagContent(anyString());
        verify(hashTagRepository, times(1)).save(any(HashTags.class));
        verify(postHashTagRepository, times(imageUpdateReqDto.getHashTagList().size())).save(any(PostHashTags.class));
        verify(postService).saveAndFlush(any(Posts.class));

        assertThat(result.getContent()).isEqualTo(imageUpdateReqDto.getContent());
        assertThat(result.getHashTagList()).isEqualTo(imageUpdateReqDto.getHashTagList());
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미지 수정 실패 - 게시물 없음")
    void 게시물을_찾을_수_없습니다_수정시() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
    }

    @Test
    @DisplayName("이미지 수정 실패 - 권한 없음")
    void 이미지수정_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).build();
        testPost.setUser(otherUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        assertThrows(IllegalArgumentException.class, () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L), "권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postService, never()).saveAndFlush(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 수정 실패 - 버전 충돌 (낙관적 락)")
    void 이미지수정_실패_버전충돌() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));
        when(hashTagRepository.findByTagContent(anyString())).thenReturn(Optional.empty());
        when(hashTagRepository.save(any(HashTags.class))).thenReturn(new HashTags(2L, "수정태그"));
        when(postHashTagRepository.save(any(PostHashTags.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new OptimisticLockingFailureException("버전 충돌")).when(postService).saveAndFlush(any(Posts.class));

        assertThrows(OptimisticLockingFailureException.class,
                () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L));

        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(hashTagRepository, atLeastOnce()).findByTagContent(anyString());
        verify(postService).saveAndFlush(any(Posts.class));
    }

    @Test
    @DisplayName("이미지 삭제 성공")
    void 파일삭제() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(anyLong());
        doNothing().when(feedService).deleteFeedsByPostId(anyLong());

        imageService.imageDelete(1L, 1L);

        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        assertThat(testPost.isDeleted()).isTrue();
        assertThat(testPost.getDeletedAt()).isNotNull();
        verify(softDeleteRepository).save(any(SoftDelete.class));
        verify(postHashTagRepository).deleteAllByPostsId(eq(1L));
        verify(feedService).deleteFeedsByPostId(eq(1L));
    }

    @Test
    @DisplayName("이미지 삭제 실패 - 게시물 없음")
    void 게시물을_찾을_수_없습니다_삭제시() {
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> imageService.imageDelete(1L, 1L));
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(feedService, never()).deleteFeedsByPostId(anyLong());
    }

    @Test
    @DisplayName("이미지 삭제 실패 - 권한 없음")
    void 이미지삭제_실패_권한없음() {
        Users otherUser = Users.builder().id(2L).build();
        testPost.setUser(otherUser);
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(testPost);

        assertThrows(IllegalArgumentException.class, () -> imageService.imageDelete(1L, 1L), "권한이 없는 유저입니다");
        verify(postService).findByIdAndDeletedIsFalse(eq(1L));
        verify(softDeleteRepository, never()).save(any(SoftDelete.class));
        verify(postHashTagRepository, never()).deleteAllByPostsId(anyLong());
        verify(feedService, never()).deleteFeedsByPostId(anyLong());
    }
}

