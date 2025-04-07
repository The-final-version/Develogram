package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.feed.service.FeedService;
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
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

//Todo 로그인 구현 완료 후 유저를 포함하여 테스트 필요
@ActiveProfiles("test")  // <- 이게 있어야 test 환경으로 바뀜
class ImageServiceTest {


    private ImageUploadReqDto imageUploadReqDto;
    private ImageUpdateReqDto imageUpdateReqDto;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private UserExternalWriteRepository userRepository;

    @Mock
    private FeedService feedService;

    @Mock
    private SoftDeleteRepository softDeleteRepository;

    @Mock
    private PostHashTagRepository postHashTagRepository;

    @Mock
    private PostService postService;

    @Mock
    private UserExternalQueryService userService;

    @InjectMocks
    private ImageService imageService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        imageUploadReqDto = new ImageUploadReqDto();
        imageUpdateReqDto = new ImageUpdateReqDto();
    }

    /**
     * imageUpload()가 동작하는지 테스트
     */
    @Test
    public void 파일업로드() throws Exception{
        // given: Mock 파일과 유저 설정 및 Stubbing
        /**
         * - 가상 파일 생성
         * - Dto에 관련 데이터 셋팅
         * - 가상 User 생성
         * - findById : 가상 유저 ID로 실행시 가상 유저 객체 반환
         * - save : Posts 객체 저장 시 Posts 객체 반환
         */
        String mockMultipartFile = ".jpg";

        imageUploadReqDto.setFile(mockMultipartFile);
        imageUploadReqDto.setContent("테스트 내용");

        UserEntity testUser = UserEntity.builder()
                .id(1L)
                .name("testuser")
                .email("testuser@example.com")
                .password("1234")
                .build();

        Posts savedPost = Posts.builder()
                .id(100L)
                .content("테스트 내용")
                .contentType(ContentType.IMAGE)
                .user(testUser)
                .build();

        when(userService.findByIdAndDeletedIsFalse(1L)).thenReturn(testUser.toDomain());
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when: 이미지 업로드 서비스 실행
        ImageUploadResDto imageUploadResDto = imageService.imageUpload(imageUploadReqDto, testUser.getId());

        // then: 응답 및 호출 검증
        /**
         * - Dto가 null이 아닌지 확인
         * - userRepository.findById()가 실행되었는지 확인
         * - postsRepository.save()가 실행되었는지 확인
         */
        assertNotNull(imageUploadResDto);
        verify(userService).findByIdAndDeletedIsFalse(testUser.getId());
        verify(postService).save(any(Posts.class));
        verify(feedService).createFeedForFollowers(any(Posts.class)); // ✅ feedService 호출 확인
    }

    /**
     * imageUpdate()가 동작하는지 테스트
     */
    @Test
    public void 파일업데이트(){
        //given : 가상 데이터 생성, stubbing
        /**
         * - 가상 파일 생성(old, new)
         * - 가상 유저 생성
         * - 가상 게시글 생성
         * - findById : ID가 1L와 같으면 가상 게시글 반환
         * - save : Posts객체를 저장하면 Posts객체 반환
         */
        MockMultipartFile oldFile = new MockMultipartFile(
                "file", "old-image.jpg","image/jpeg","dummy image content".getBytes()
        );

        UserEntity testUser = UserEntity.builder()
                .id(1L)
                .name("testuser")
                .email("testuser@example.com")
                .password("1234")
                .build();

        Posts tempPost = Posts.builder()
                .id(1L)
                .content("수정전 내용")
                .mediaName(oldFile.getOriginalFilename())
                .contentType(ContentType.IMAGE)
                .user(testUser)
                .build();

        String newFile = "new-image.jpg";
        ImageUpdateReqDto reqDto = new ImageUpdateReqDto();
        reqDto.setFile(newFile);
        reqDto.setContent("수정된 내용");

        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(tempPost);
        when(postService.save(any(Posts.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //when : imageUpdate() 실행
        ImageUpdateResDto imageUpdateResDto = imageService.imageUpdate(tempPost.getId(), reqDto, testUser.getId());

        //then : 응답 데이터 검증
        /**
         * - Dto가 null이 아닌지 확인
         * - content가 수정되었는지 확인
         * - findById가 실행되었는지 확인
         * - save가 실행되었는지 확인
         */
        assertNotNull(imageUpdateResDto);
        assertEquals("수정된 내용", imageUpdateResDto.getContent());
        verify(postService).findByIdAndDeletedIsFalse(tempPost.getId());
        verify(postService).save(any(Posts.class));
    }

    /**
     * imageDelete()가 동작하는지 테스트
     */
    @Test
    public void 파일삭제(){
        //given : 가상 데이터 생성, stubbing
        /**
         * - 가상 파일 생성
         * - 가상 유저 생성
         * - 가상 게시글 생성
         * - findById : ID가 1L와 같으면 가상 게시글 반환
         */
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file", "image.jpg","image/jpeg","dummy image content".getBytes()
        );

        UserEntity testUser = UserEntity.builder()
                .id(1L)
                .name("testuser")
                .email("testuser@example.com")
                .password("1234")
                .build();

        Posts tempPost = Posts.builder()
                .id(1L)
                .content("수정전 내용")
                .mediaName(mockMultipartFile.getOriginalFilename())
                .contentType(ContentType.IMAGE)
                .user(testUser)
                .build();

        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(tempPost);
        when(softDeleteRepository.save(any(SoftDelete.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(postHashTagRepository).deleteAllByPostsId(eq(1L));

        //when : imageDelete 실행
        imageService.imageDelete(tempPost.getId(), testUser.getId());

        //then : 응답 데이터 검증
        /**
         * - findById가 실행되었는지 확인
         * - delete가 실행되었는지 확인
         */
        verify(postService).findByIdAndDeletedIsFalse(tempPost.getId());
        verify(postHashTagRepository).deleteAllByPostsId(tempPost.getId());
        verify(softDeleteRepository).save(any(SoftDelete.class));
        verify(feedService).deleteFeedsByPostId(tempPost.getId()); // ✅ feedService 호출 확인
    }

    /**
     * 유저가 존재하지 않을시 발생하는 에러 테스트
     */
    @Test
    public void 해당_유저를_찾을_수_없습니다(){
        //given : 가상 데이터 생성, stubbing
        /**
         * 가상 파일 생성
         * 가상 데이터 셋팅
         * findById : 유저의 Id가 1L과 같으면 빈 객체 반환
         */
        String mockMultipartFile = "image.jpg";

        imageUploadReqDto.setFile(mockMultipartFile);
        imageUploadReqDto.setContent("파일생성");

        when(userService.findByIdAndDeletedIsFalse((1L))).thenReturn(null);

        //when : imageUpload() 실행
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageUpload(imageUploadReqDto, 1L));

        //then : 응답 데이터 검증
        /**
         * - "해당 유저를 찾을 수 없습니다." 에러 메세지 반환 확인
         * - findById 동작 확인
         */
        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        verify(userService).findByIdAndDeletedIsFalse(1L);
    }

    /**
     * 게시글이 존재하지 않을시 발생하는 에러 테스트
     */
    @Test
    public void 게시물을_찾을_수_없습니다(){
        //given : stubbing
        when(postService.findByIdAndDeletedIsFalse(eq(1L))).thenReturn(null);

        //when : imageUpdate(),imageDelete() 실행
        IllegalArgumentException updateException = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageUpdate(1L, imageUpdateReqDto, 1L));

        IllegalArgumentException deleteException = assertThrows(IllegalArgumentException.class,
                () -> imageService.imageDelete(1L, 1L));

        //then : 응답 데이터 검증
        /**
         * - "게시물을 찾을 수 없습니다" 에러 메세지 반환 확인
         * - "해당 게시물이 없습니다" 에러 메세지 반환 확인
         * - findById가 2번 실행되었는지 확인
         */
        assertEquals("게시물을 찾을 수 없습니다", updateException.getMessage());
        assertEquals("해당 게시물이 없습니다", deleteException.getMessage());
        verify(postService, times(2)).findByIdAndDeletedIsFalse(1L);
    }
}

