package com.goorm.clonestagram.post.service;

import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.util.CustomUserDetails;
import com.goorm.clonestagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.ConcurrencyFailureException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 영상 업로드 요청을 처리하는 서비스
 * - 검증이 완료된 영상을 받아 업로드 서비스 수행
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VideoService {

    private final PostService postService;
    private final UserService userService;
    private final HashTagRepository hashTagRepository;
    private final PostHashTagRepository postHashTagRepository;
    private final SoftDeleteRepository softDeleteRepository;
    private final FeedService feedService;
    private final IdempotencyService idempotencyService;

    /**
     * 영상 업로드 (멱등성 적용)
     * @param videoUploadReqDto 업로드 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @param idempotencyKey 멱등성 키
     * @return 업로드 결과 DTO
     */
    public VideoUploadResDto videoUploadWithIdempotency(VideoUploadReqDto videoUploadReqDto, CustomUserDetails userDetails, String idempotencyKey) {

        Supplier<VideoUploadResDto> videoUploadOperation = () -> {
            Users users = userService.findByIdAndDeletedIsFalse(userDetails.getId());

            String fileUrl = videoUploadReqDto.getFile();
            if (fileUrl == null || fileUrl.isBlank()) {
                throw new IllegalArgumentException("Cloudinary 비디오 URL이 필요합니다.");
            }

            Posts postEntity = videoUploadReqDto.toEntity(fileUrl, users);
            Posts post = postService.save(postEntity);

            for (String tagContent : Optional.ofNullable(videoUploadReqDto.getHashTagList())
                    .orElse(Collections.emptyList())) {
                HashTags tag = hashTagRepository.findByTagContent(tagContent)
                        .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
                postHashTagRepository.save(new PostHashTags(null, tag, post));
            }

            feedService.createFeedForFollowers(post);

            return VideoUploadResDto.builder()
                    .content(post.getContent())
                    .type(post.getContentType())
                    .createdAt(post.getCreatedAt())
                    .hashTagList(videoUploadReqDto.getHashTagList())
                    .build();
        };

        return idempotencyService.executeWithIdempotency(idempotencyKey, videoUploadOperation, VideoUploadResDto.class);
    }

    /**
     * 영상 수정 (낙관적 락 적용)
     * @param postSeq 게시글 ID
     * @param videoUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @param userId 사용자 ID
     * @return 성공 시 VideoUpdateResDto
     * @throws IllegalArgumentException 게시글을 찾을 수 없거나 권한이 없을 시
     * @throws ConcurrencyFailureException 동시성 충돌 발생 시
     */
    public VideoUpdateResDto videoUpdate(Long postSeq, VideoUpdateReqDto videoUpdateReqDto, Long userId) {
        try {
            boolean updated = false;
            // 1. 게시글 조회 (트랜잭션 내)
            Posts posts = postService.findByIdAndDeletedIsFalse(postSeq);

            // 2. 권한 확인
            if(!posts.getUser().getId().equals(userId)){
                throw new IllegalArgumentException("권한이 없는 유저입니다");
            }

            // 3. 내용 및 미디어 수정
            if(videoUpdateReqDto.getFile() != null && !videoUpdateReqDto.getFile().isEmpty()){
                String fileUrl = videoUpdateReqDto.getFile();
                posts.setMediaName(fileUrl);
                updated = true;
            }
            if(videoUpdateReqDto.getContent() != null && !videoUpdateReqDto.getContent().trim().isEmpty()){
                posts.setContent(videoUpdateReqDto.getContent());
                updated = true;
            }

            // 4. 해시태그 수정
            if(videoUpdateReqDto.getHashTagList() != null) { // null 체크만 (비어있는 리스트 허용)
                postHashTagRepository.deleteAllByPostsId(posts.getId());
                for (String tagContent : videoUpdateReqDto.getHashTagList()) {
                    if (tagContent != null && !tagContent.trim().isEmpty()) { // 태그 내용 유효성 검사
                        HashTags tag = hashTagRepository.findByTagContent(tagContent)
                                .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
                        postHashTagRepository.save(new PostHashTags(null, tag, posts));
                    }
                }
                updated = true; // 해시태그 목록 제공 시 무조건 updated로 간주 (내용 없어도)
            }

            // 5. 저장 (JPA 변경 감지 또는 명시적 save)
            // 트랜잭션 커밋 시점에 OptimisticLockException 발생 가능성 있음
            if(updated){
                // 명시적 save 호출은 필수는 아님 (변경 감지로도 동작)
                postService.save(posts);
            }

            // 6. 결과 반환
            return VideoUpdateResDto.builder()
                    .content(posts.getContent())
                    // .type(posts.getContentType()) // 필요시 추가
                    .updatedAt(posts.getUpdatedAt()) // updatedAt 포함
                    .hashTagList(videoUpdateReqDto.getHashTagList())
                    // .mediaName(posts.getMediaName()) // 필요시 추가
                    .build();

        } catch (OptimisticLockingFailureException e) { // Spring Data JPA 예외 우선 처리
            log.warn("비디오 수정 중 버전 충돌 발생: postId={}, userId={}", postSeq, userId, e);
            throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (OptimisticLockException e) { // JPA 표준 예외
             log.warn("비디오 수정 중 버전 충돌 발생 (JPA): postId={}, userId={}", postSeq, userId, e);
             throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * 영상 삭제 (Soft Delete, 낙관적 락 적용)
     * @param postSeq 삭제할 게시글 식별자
     * @param userId 사용자 ID
     * @throws IllegalArgumentException 게시글을 찾을 수 없거나 권한이 없을 시
     * @throws ConcurrencyFailureException 동시성 충돌 발생 시
     */
    public void videoDelete(Long postSeq, Long userId) {
        try {
            // 1. 게시글 조회
            Posts posts = postService.findByIdAndDeletedIsFalse(postSeq);

            // 2. 권한 확인
            if(!posts.getUser().getId().equals(userId)){
                throw new IllegalArgumentException("권한이 없는 유저입니다");
            }

            // 3. Soft Delete 처리 (deleted, deletedAt 업데이트)
            // 이 변경 사항이 커밋될 때 버전 충돌 감지 가능
            posts.setDeleted(true);
            posts.setDeletedAt(LocalDateTime.now());

            // 4. SoftDelete 로그 저장 (이 작업은 Posts 엔티티 버전과 별개일 수 있음)
            softDeleteRepository.save(new SoftDelete(null, EntityType.POST, posts.getId(), posts.getDeletedAt()));

            // 5. 관련 데이터 삭제 (해시태그, 피드)
            postHashTagRepository.deleteAllByPostsId(posts.getId());
            feedService.deleteFeedsByPostId(postSeq);

            // JPA 변경 감지에 의해 posts 업데이트 쿼리가 나가거나,
            // 명시적으로 save를 호출할 경우 버전 체크가 이루어짐.
            postService.save(posts);

        } catch (OptimisticLockingFailureException e) {
            log.warn("비디오 삭제 중 버전 충돌 발생: postId={}, userId={}", postSeq, userId, e);
            throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        } catch (OptimisticLockException e) {
             log.warn("비디오 삭제 중 버전 충돌 발생 (JPA): postId={}, userId={}", postSeq, userId, e);
             throw new ConcurrencyFailureException("다른 사용자에 의해 정보가 변경되었습니다. 다시 시도해주세요.", e);
        }
    }
}
