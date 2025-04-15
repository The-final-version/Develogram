package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.service.HashtagService;
import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.ImageUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadResDto;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.service.UserService;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.user.domain.entity.User;
import com.goorm.clonestagram.user.domain.service.UserExternalQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 업로드 요청을 처리하는 서비스
 * - 검증이 완료된 이미지를 받아 업로드 서비스 수행
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ImageService {

    private final PostService postService;
    private final HashtagService hashtagService;
    private final HashTagRepository hashTagRepository;
    private final PostHashTagRepository postHashTagRepository;
    private final SoftDeleteRepository softDeleteRepository;
    private final FeedService feedService;
    private final UserService userService;
    private final IdempotencyService idempotencyService;
    private final UserExternalQueryService userService;     // 유저 도메인 수정

    /**
     * 이미지 업로드
     * - 검증이 끝난 파일의 name과 path를 설정하여 DB와 저장소에 저장
     *
     * @param imageUploadReqDto 업로드할 이미지와 관련된 요청 DTO
     *                          - controller에서 넘어옴
     * @return 성공시 ImageUploadResDto 반환
     * @throws Exception - 파일 저장시 IOException 발생
     */
    public ImageUploadResDto imageUpload(ImageUploadReqDto imageUploadReqDto, Long userId) throws Exception {
        // 사용자 검증
        User users = userService.findByIdAndDeletedIsFalse(userId);
        if (users == null) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        }

        String fileUrl = imageUploadReqDto.getFile();

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Cloudinary 이미지 URL이 필요합니다.");
        }

        // 1. Entity 생성 (Cloudinary URL 그대로 사용)
        Posts postEntity = imageUploadReqDto.toEntity(fileUrl, users);
        Posts post = postService.save(postEntity);

        if (post == null) {
            throw new IllegalArgumentException("게시물 생성에 실패했습니다.");
        }

        // 2. 피드 생성
        feedService.createFeedForFollowers(post);

        // 3. 해시태그 저장

        hashtagService.saveHashtags(post, imageUploadReqDto.getHashTagList());

        // 4. 응답 반환
        return ImageUploadResDto.builder()
            .content(post.getContent())
            .type(post.getContentType())
            .createdAt(post.getCreatedAt())
            .postId(post.getId())
            .hashTagList(imageUploadReqDto.getHashTagList().stream().toList())
            .mediaName(post.getMediaName()) // URL 그대로
            .build();
    }

    /**
     * @param postSeq           게시글 ID
     * @param imageUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @return 성공시 ImageUpdateResDto
     * @throws IllegalArgumentException 게시글을 찾을 수 없을시 발생
     */
    public ImageUpdateResDto imageUpdate(Long postSeq, ImageUpdateReqDto imageUpdateReqDto, Long userId) {
        log.info(">>> imageUpdate 시작 - postSeq: {}, userId: {}", postSeq, userId);
        if (postSeq == null) {
            throw new IllegalArgumentException("게시물 ID가 필요합니다.");
        }

        //1. 게시글 ID를 통해 게시글을 찾아 반환
        Posts posts = postService.findByIdAndDeletedIsFalse(postSeq);
        if (posts == null) {
            throw new IllegalArgumentException("게시물을 찾을 수 없습니다");
        }
        log.info(">>> 조회 직후 posts.getContent(): '{}'", posts.getContent());

        if (!posts.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없는 유저입니다");
        }

        boolean updated = false;

        //2. 이미지 수정 여부 파악
        if (imageUpdateReqDto.getFile() != null && !imageUpdateReqDto.getFile().isEmpty()) {
            String fileUrl = imageUpdateReqDto.getFile();
            log.info(">>> 이미지 업데이트 시도: {}", fileUrl);
            posts.setMediaName(fileUrl);
            updated = true;
        }

        //3. 게시글 내용 수정 여부 파악
        if (imageUpdateReqDto.getContent() != null && !imageUpdateReqDto.getContent().trim().isEmpty()) {
            String newContent = imageUpdateReqDto.getContent();
            log.info(">>> 내용 업데이트 시도. 현재 content: '{}', 새 content: '{}'", posts.getContent(), newContent);
            //3-1. 수정된 게시글 내용 반영
            posts.setContent(newContent);
            //3-2. 업데이트 되었음을 표시
            updated = true;
            log.info(">>> setContent 호출 후 posts.getContent(): '{}'", posts.getContent());
        }

        //4. 해시태그 수정 여부 파악
        if (imageUpdateReqDto.getHashTagList() != null && !imageUpdateReqDto.getHashTagList().isEmpty()) {
            log.info(">>> 해시태그 업데이트 시도: {}", imageUpdateReqDto.getHashTagList());
            //4-1. 기존의 해시태그 리스트 삭제
            postHashTagRepository.deleteAllByPostsId(posts.getId());
            log.info(">>> 기존 해시태그 삭제 완료 (Post ID: {})", posts.getId());

            //4-2. 새롭게 해시 태그 리스트 저장
            for (String tagContent : imageUpdateReqDto.getHashTagList()) {
                log.info(">>> 새 해시태그 처리 중: {}", tagContent);
                //4-2. tagList에서 tag 내용 하나를 추출한 후 조회
                HashTags tag = hashTagRepository.findByTagContent(tagContent)
                    //4-2. tag가 저장되어 있지 않으면 새롭게 저장
                    .orElseGet(() -> {
                        log.info(">>> 새 해시태그 저장: {}", tagContent);
                        return hashTagRepository.save(new HashTags(null, tagContent));
                    });
                //4-3. 추출된 태그의 id와 피드의 id를 관계테이블에 저장
                postHashTagRepository.save(new PostHashTags(null, tag, posts));
                log.info(">>> PostHashTags 저장 완료: Tag={}, Post={}", tag.getId(), posts.getId());
            }
            updated = true;
        }

        if (updated) {
            log.info(">>> updated=true, saveAndFlush 호출 전 posts.getContent(): '{}'", posts.getContent());
            // 변경 감지 대신 명시적 saveAndFlush 호출
            postService.saveAndFlush(posts); // saveAndFlush() 호출 (반환값 사용 안함)
            log.info(">>> saveAndFlush 호출 후 posts.getContent(): '{}'", posts.getContent());
            // if (posts == null) { // 반환값 사용 안하므로 관련 검사 제거
            //     throw new IllegalArgumentException("게시물 업데이트에 실패했습니다.");
            // }
        } else {
            log.info(">>> updated=false, 변경 없음");
        }

        log.info(">>> 응답 DTO 생성 전 posts.getContent(): '{}'", posts.getContent());
        //6. 모든 작업이 완료된 경우 응답 반환
        // 영속성 컨텍스트 내에서 변경된 'posts' 엔티티를 사용하여 DTO 생성
        ImageUpdateResDto responseDto = ImageUpdateResDto.builder()
            .content(posts.getContent()) // 원본 posts 변수 사용
            .type(posts.getContentType()) // 원본 posts 변수 사용
            .updatedAt(posts.getUpdatedAt())
            .hashTagList(imageUpdateReqDto.getHashTagList())
            .build();
        log.info(">>> imageUpdate 종료");
        return responseDto;
    }

    /**
     * @param postSeq 삭제할 게시글 식별자
     */
    public void imageDelete(Long postSeq, Long userId) {
        if (postSeq == null) {
            throw new IllegalArgumentException("게시물 ID가 필요합니다.");
        }

        //1. 식별자를 토대로 게시글 찾아 반환
        Posts posts = postService.findByIdAndDeletedIsFalse(postSeq);
        if (posts == null) {
            throw new IllegalArgumentException("해당 게시물이 없습니다");
        }

        if (!posts.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없는 유저입니다");
        }

        //3. DB에서 데이터 삭제
        posts.setDeleted(true);
        posts.setDeletedAt(LocalDateTime.now());
        softDeleteRepository.save(new SoftDelete(null, EntityType.POST, posts.getId(), posts.getDeletedAt()));
        postHashTagRepository.deleteAllByPostsId(posts.getId());

        // 피드 삭제 로직 추가
        feedService.deleteFeedsByPostId(postSeq);
    }

    public ImageUploadResDto imageUploadWithIdempotency(ImageUploadReqDto imageUploadReqDto, Long userId, String idempotencyKey) {
        Supplier<ImageUploadResDto> operation = () -> {
            try {
                return performImageUpload(imageUploadReqDto, userId);
            } catch (Exception e) {
                throw new RuntimeException("이미지 업로드 처리 중 오류 발생", e);
            }
        };

        return idempotencyService.executeWithIdempotency(
            idempotencyKey,
            operation,
            ImageUploadResDto.class
        );
    }

    private ImageUploadResDto performImageUpload(ImageUploadReqDto imageUploadReqDto, Long userId) throws Exception {
        Users users = userService.findByIdAndDeletedIsFalse(userId);
        if (users == null) {
            throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");
        }

        String fileUrl = imageUploadReqDto.getFile();

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Cloudinary 이미지 URL이 필요합니다.");
        }

        Posts postEntity = imageUploadReqDto.toEntity(fileUrl, users);
        Posts post = postService.save(postEntity);

        if (post == null) {
            throw new IllegalArgumentException("게시물 생성에 실패했습니다.");
        }

        feedService.createFeedForFollowers(post);

        hashtagService.saveHashtags(post, imageUploadReqDto.getHashTagList());

        return ImageUploadResDto.builder()
            .content(post.getContent())
            .type(post.getContentType())
            .createdAt(post.getCreatedAt())
            .postId(post.getId())
            .hashTagList(imageUploadReqDto.getHashTagList().stream().toList())
            .mediaName(post.getMediaName())
            .build();
    }
}
