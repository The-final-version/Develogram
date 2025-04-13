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
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영상 업로드 요청을 처리하는 서비스
 * - 검증이 완료된 영상을 받아 업로드 서비스 수행
 */
@Service
@Transactional
@RequiredArgsConstructor
public class VideoService {

    private final PostsRepository postsRepository;
    private final UserRepository userRepository;
    private final HashTagRepository hashTagRepository;
    private final PostHashTagRepository postHashTagRepository;
    private final SoftDeleteRepository softDeleteRepository;
    private final FeedService feedService;
    private final HashtagService hashtagService;
    private final IdempotencyService idempotencyService;

    /**
     * 영상 업로드
     * - 검증이 끝난 파일의 name과 path를 설정하여 DB와 저장소에 저장
     *
     * @param videoUploadReqDto 업로드할 영상과 관련된 요청 DTO
     *                          - controller에서 넘어옴
     * @return 성공시 ImageUploadResDto 반환
     * @throws Exception - 파일 저장시 IOException 발생
     */
    public VideoUploadResDto videoUpload(VideoUploadReqDto videoUploadReqDto, Long userId) {

        Users users = userRepository.findByIdAndDeletedIsFalse(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        String fileUrl = videoUploadReqDto.getFile();

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Cloudinary 이미지 URL이 필요합니다.");
        }

        //4. unique 파일명과 videoUploadReqDto의 값들을 활용해 Entity 객체 생성 후 저장
        Posts postEntity = videoUploadReqDto.toEntity(fileUrl, users);
        Posts post = postsRepository.save(postEntity);

        //5. Dto에 있는 HashTagList를 저장
        hashtagService.saveHashtags(post, videoUploadReqDto.getHashTagList());

        //6. 모든 작업이 완료된 경우 응답 반환
        // 피드 생성 로직 추가
        feedService.createFeedForFollowers(post);

        //5. 모든 작업이 완료된 경우 응답 반환
        return VideoUploadResDto.builder()
            .content(post.getContent())
            .type(post.getContentType())
            .createdAt(post.getCreatedAt())
            .hashTagList(videoUploadReqDto.getHashTagList().stream().toList())
            .build();
    }

    /**
     * @param postSeq           게시글 ID
     * @param videoUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @return 성공시 ImageUpdateResDto
     * @throws IllegalArgumentException 게시글을 찾을 수 없을시 발생
     */
    public VideoUpdateResDto videoUpdate(Long postSeq, VideoUpdateReqDto videoUpdateReqDto, Long userId) {

        boolean updated = false;
        //1. 게시글 ID를 통해 게시글을 찾아 반환
        Posts posts = postsRepository.findByIdAndDeletedIsFalse(postSeq)
            .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다"));

        if (!posts.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없는 유저입니다");
        }

        //2. 영상 수정 여부 파악
        if (videoUpdateReqDto.getFile() != null && !videoUpdateReqDto.getFile().isEmpty()) {

            String fileUrl = videoUpdateReqDto.getFile();

            posts.setMediaName(fileUrl);

            updated = true;
        }

        //3. 게시글 내용 수정 여부 파악
        if (videoUpdateReqDto.getContent() != null && !videoUpdateReqDto.getContent().trim().isEmpty()) {
            //3-1. 수정된 게시글 내용 반영
            posts.setContent(videoUpdateReqDto.getContent());

            //3-2. 업데이트 되었음을 표시
            updated = true;
        }

        //4. 해시태그 수정 여부 파악
        if (videoUpdateReqDto.getHashTagList() != null) {
            //4-1. 기존의 해시태그 리스트 삭제
            postHashTagRepository.deleteAllByPostsId(posts.getId());

            //4-2. 새롭게 해시 태그 리스트 저장
            for (String tagContent : Optional.ofNullable(videoUpdateReqDto.getHashTagList())
                .orElse(Collections.emptyList())) {
                // 유효성 검사 추가: null 또는 공백 문자열 필터링
                if (tagContent != null && !tagContent.trim().isEmpty()) {
                    //4-2. tagList에서 tag 내용 하나를 추출한 후 조회
                    HashTags tag = hashTagRepository.findByTagContent(tagContent)
                        //4-2. tag가 저장되어 있지 않으면 새롭게 저장
                        .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
                    //4-3. 추출된 태그의 id와 피드의 id를 관계테이블에 저장
                    postHashTagRepository.save(new PostHashTags(null, tag, posts));
                } // 유효성 검사 끝
            }
            // 수정: 빈 리스트가 들어와도 updated=true가 될 수 있으므로, 실제 저장된 태그가 있는지 확인하거나 로직 수정 필요.
            // 여기서는 일단 updated=true 유지. (개선 필요시 추가 논의)
            updated = true;
        }

        Posts updatedPost;
        if (updated) {
            //5. 업데이트된 게시글을 DB에 저장
            updatedPost = postsRepository.save(posts);
        } else {
            updatedPost = posts;
        }

        //6. 모든 작업이 완료된 경우 응답 반환
        return VideoUpdateResDto.builder()
            .content(updatedPost.getContent())
            .type(updatedPost.getContentType())
            .updatedAt(updatedPost.getUpdatedAt())
            .hashTagList(videoUpdateReqDto.getHashTagList())
            .build();
    }

    /**
     * @param postSeq 삭제할 게시글 식별자
     */
    public void videoDelete(Long postSeq, Long userId) {
        //1. 식별자를 토대로 게시글 찾아 반환
        Posts posts = postsRepository.findByIdAndDeletedIsFalse(postSeq)
            .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다"));

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

    public VideoUploadResDto videoUploadWithIdempotency(VideoUploadReqDto videoUploadReqDto, Long userId, String idempotencyKey) {
        Supplier<VideoUploadResDto> operation = () -> {
            try {
                return performVideoUpload(videoUploadReqDto, userId);
            } catch (Exception e) {
                throw new RuntimeException("비디오 업로드 처리 중 오류 발생", e);
            }
        };

        return idempotencyService.executeWithIdempotency(
            idempotencyKey,
            operation,
            VideoUploadResDto.class
        );
    }

    private VideoUploadResDto performVideoUpload(VideoUploadReqDto videoUploadReqDto, Long userId) {
        Users users = userRepository.findByIdAndDeletedIsFalse(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        String fileUrl = videoUploadReqDto.getFile();

        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Cloudinary 이미지 URL이 필요합니다.");
        }

        //4. unique 파일명과 videoUploadReqDto의 값들을 활용해 Entity 객체 생성 후 저장
        Posts postEntity = videoUploadReqDto.toEntity(fileUrl, users);
        Posts post = postsRepository.save(postEntity);

        //5. Dto에 있는 HashTagList를 저장
        hashtagService.saveHashtags(post, videoUploadReqDto.getHashTagList());

        //6. 모든 작업이 완료된 경우 응답 반환
        // 피드 생성 로직 추가
        feedService.createFeedForFollowers(post);

        //5. 모든 작업이 완료된 경우 응답 반환
        return VideoUploadResDto.builder()
            .content(post.getContent())
            .type(post.getContentType())
            .createdAt(post.getCreatedAt())
            .hashTagList(videoUploadReqDto.getHashTagList().stream().toList())
            .build();
    }
}
