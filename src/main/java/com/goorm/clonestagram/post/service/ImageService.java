package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.ImageUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadResDto;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.util.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.ConcurrencyFailureException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

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
    private final HashTagRepository hashTagRepository;
    private final PostHashTagRepository postHashTagRepository;
    private final SoftDeleteRepository softDeleteRepository;
    private final FeedService feedService;
    private final UserService userService;
    private final IdempotencyService idempotencyService;

    /**
     * 이미지 업로드 (멱등성 적용)
     * @param imageUploadReqDto 업로드 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @param idempotencyKey 멱등성 키
     * @return 업로드 결과 DTO
     * @throws Exception
     */
    public ImageUploadResDto imageUploadWithIdempotency(ImageUploadReqDto imageUploadReqDto, CustomUserDetails userDetails, String idempotencyKey) {

        // Supplier<ImageUploadResDto> operation = () -> { ... }; 와 같이 람다로 정의
        Supplier<ImageUploadResDto> imageUploadOperation = () -> {
            // try-catch 블록은 IdempotencyService 내부에서 처리되므로 여기서는 제거 가능
            // try {
                // 실제 이미지 업로드 로직 (기존 imageUpload 메서드 내용)
                Users users = userService.findByIdAndDeletedIsFalse(userDetails.getId()); // getUserId() -> getId() 로 수정

                String fileUrl = imageUploadReqDto.getFile();
                if (fileUrl == null || fileUrl.isBlank()) {
                    throw new IllegalArgumentException("Cloudinary 이미지 URL이 필요합니다.");
                }

                // 1. Entity 생성
                Posts postEntity = imageUploadReqDto.toEntity(fileUrl, users);
                Posts post = postService.save(postEntity);

                // 2. 피드 생성 (아래에서 처리)

                // 3. 해시태그 저장
                for (String tagContent : Optional.ofNullable(imageUploadReqDto.getHashTagList())
                        .orElse(Collections.emptyList())) {
                    HashTags tag = hashTagRepository.findByTagContent(tagContent)
                            .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
                    postHashTagRepository.save(new PostHashTags(null, tag, post));
                }

                 // 피드 생성 로직 (게시물, 해시태그 저장이 완료된 후 호출)
                 feedService.createFeedForFollowers(post);

                // 4. 응답 반환
                return ImageUploadResDto.builder()
                        .content(post.getContent())
                        // .type(post.getContentType()) // Posts 엔티티에 contentType 필드가 없으면 제거 또는 추가 필요
                        .createdAt(post.getCreatedAt())
                        .hashTagList(imageUploadReqDto.getHashTagList())
                        .mediaName(post.getMediaName())
                        .build();
            // } catch (Exception e) {
            //     // Supplier 내부에서 발생하는 체크 예외를 RuntimeException으로 래핑
            //     throw new RuntimeException("이미지 업로드 작업 실패: " + e.getMessage(), e);
            // }
        };

        // IdempotencyService를 통해 작업 실행
        // 결과 타입으로 ImageUploadResDto.class 전달
        return idempotencyService.executeWithIdempotency(idempotencyKey, imageUploadOperation, ImageUploadResDto.class);
    }

    /**
     * 이미지 업로드
     * - 검증이 끝난 파일의 name과 path를 설정하여 DB와 저장소에 저장
     *
     * @param imageUploadReqDto 업로드할 이미지와 관련된 요청 DTO
     *          - controller에서 넘어옴
     * @return 성공시 ImageUploadResDto 반환
     * @throws Exception
     *          - 파일 저장시 IOException 발생
     */
    public ImageUploadResDto imageUpload(ImageUploadReqDto imageUploadReqDto, Long userId) throws Exception {
        // 사용자 검증
        Users users = userService.findByIdAndDeletedIsFalse(userId);
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
        for (String tagContent : Optional.ofNullable(imageUploadReqDto.getHashTagList())
                .orElse(Collections.emptyList())) {
            HashTags tag = hashTagRepository.findByTagContent(tagContent)
                    .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
            postHashTagRepository.save(new PostHashTags(null, tag, post));
        }

        // 4. 응답 반환
        return ImageUploadResDto.builder()
                .content(post.getContent())
                .type(post.getContentType())
                .createdAt(post.getCreatedAt())
                .hashTagList(imageUploadReqDto.getHashTagList())
                .mediaName(post.getMediaName()) // URL 그대로
                .build();
    }


    /**
     * 이미지 수정 (낙관적 락 적용)
     * @param postSeq 게시글 ID
     * @param imageUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @param userId 사용자 ID
     * @return 성공 시 ImageUpdateResDto
     * @throws IllegalArgumentException 게시글을 찾을 수 없거나 권한이 없을 시
     * @throws ConcurrencyFailureException 동시성 충돌 발생 시
     */
    public ImageUpdateResDto imageUpdate(Long postSeq, ImageUpdateReqDto imageUpdateReqDto, Long userId) {
        try {
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

            if(!posts.getUser().getId().equals(userId)){
                throw new IllegalArgumentException("권한이 없는 유저입니다");
            }

            boolean updated = false;

            //2. 이미지 수정 여부 파악
            if(imageUpdateReqDto.getFile() != null && !imageUpdateReqDto.getFile().isEmpty()){
                String fileUrl = imageUpdateReqDto.getFile();
                log.info(">>> 이미지 업데이트 시도: {}", fileUrl);
                posts.setMediaName(fileUrl);
                updated = true;
            }

            //3. 게시글 내용 수정 여부 파악
            if(imageUpdateReqDto.getContent() != null && !imageUpdateReqDto.getContent().trim().isEmpty()){
                String newContent = imageUpdateReqDto.getContent();
                log.info(">>> 내용 업데이트 시도. 현재 content: '{}', 새 content: '{}'", posts.getContent(), newContent);
                //3-1. 수정된 게시글 내용 반영
                posts.setContent(newContent);
                //3-2. 업데이트 되었음을 표시
                updated = true;
                log.info(">>> setContent 호출 후 posts.getContent(): '{}'", posts.getContent());
            }

            //4. 해시태그 수정 여부 파악
            if(imageUpdateReqDto.getHashTagList() != null && !imageUpdateReqDto.getHashTagList().isEmpty()){
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
                    postHashTagRepository.save(new PostHashTags(null,tag,posts));
                    log.info(">>> PostHashTags 저장 완료: Tag={}, Post={}", tag.getId(), posts.getId());
                }
                updated = true;
            }

            if(updated){
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
                    .content(posts.getContent())
                    .type(posts.getContentType())
                    .updatedAt(posts.getUpdatedAt())
                    .hashTagList(imageUpdateReqDto.getHashTagList() == null ? Collections.emptyList() : imageUpdateReqDto.getHashTagList())
                    .build();
            log.info(">>> imageUpdate 종료");
            return responseDto;

        } catch (OptimisticLockingFailureException e) {
            log.warn("이미지 수정 중 버전 충돌 발생: postId={}, userId={}", postSeq, userId, e);
            throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (OptimisticLockException e) {
             log.warn("이미지 수정 중 버전 충돌 발생 (JPA): postId={}, userId={}", postSeq, userId, e);
             throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (IllegalArgumentException e) {
            log.warn("이미지 수정 중 잘못된 인자 또는 권한 오류: postId={}, userId={}, error={}", postSeq, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이미지 수정 중 예상치 못한 오류 발생: postId={}, userId={}", postSeq, userId, e);
            throw new RuntimeException("이미지 수정 중 오류 발생", e);
        }
    }

    /**
     * 이미지 삭제 (Soft Delete, 낙관적 락 적용)
     * @param postSeq 삭제할 게시글 식별자
     * @param userId 사용자 ID
     * @throws IllegalArgumentException 게시글을 찾을 수 없거나 권한이 없을 시
     * @throws ConcurrencyFailureException 동시성 충돌 발생 시
     */
    public void imageDelete(Long postSeq, Long userId) {
        try {
            if (postSeq == null) {
                throw new IllegalArgumentException("게시물 ID가 필요합니다.");
            }

            //1. 식별자를 토대로 게시글 찾아 반환
            Posts posts = postService.findByIdAndDeletedIsFalse(postSeq);
            if (posts == null) {
                throw new IllegalArgumentException("해당 게시물이 없습니다");
            }

            if(!posts.getUser().getId().equals(userId)){
                throw new IllegalArgumentException("권한이 없는 유저입니다");
            }

            //3. Soft Delete 처리
            posts.setDeleted(true);
            posts.setDeletedAt(LocalDateTime.now());

            //4. SoftDelete 로그 저장
            softDeleteRepository.save(new SoftDelete(null, EntityType.POST, posts.getId(), posts.getDeletedAt()));

            //5. 관련 데이터 삭제
            postHashTagRepository.deleteAllByPostsId(posts.getId());
            feedService.deleteFeedsByPostId(postSeq);

            //6. 수정: 명시적으로 save 호출 추가 (낙관적 락 체크 및 테스트 용이성)
            postService.save(posts);

            log.info("이미지 삭제 완료 (Soft Delete): postId={}, userId={}", postSeq, userId);

        } catch (OptimisticLockingFailureException e) {
            log.warn("이미지 삭제 중 버전 충돌 발생: postId={}, userId={}", postSeq, userId, e);
            throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (OptimisticLockException e) {
             log.warn("이미지 삭제 중 버전 충돌 발생 (JPA): postId={}, userId={}", postSeq, userId, e);
             throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (IllegalArgumentException e) {
            log.warn("이미지 삭제 중 잘못된 인자 또는 권한 오류: postId={}, userId={}, error={}", postSeq, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이미지 삭제 중 예상치 못한 오류 발생: postId={}, userId={}", postSeq, userId, e);
            throw new RuntimeException("이미지 삭제 중 오류 발생", e);
        }
    }
}
