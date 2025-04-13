package com.goorm.clonestagram.post.controller;


import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.post.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.post.service.VideoService;
import com.goorm.clonestagram.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 영상 업로드 요청을 처리하는 컨트롤러
 * - 클라이언트로부터 영상 업로드 요청을 받아 검증 및 서비스 호출을 수행
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/videos")
public class VideoController {

    private final VideoService videoService;

    /**
     * 영상 업로드 (멱등성 적용)
     * - 요청으로부터 파일을 받아 유효성 검사 후, 서비스 계층에 전달
     *
     * @param videoUploadReqDto 업로드할 영상과 관련된 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @param idempotencyKey 멱등성 키 (HTTP 헤더)
     * @return 업로드 성공 시 VideoUploadResDto 반환
     * @throws Exception 업로드 도중 발생할 수 있는 예외
     */
    @PostMapping(value = "/upload")
    public ResponseEntity<VideoUploadResDto> videoUpload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody VideoUploadReqDto videoUploadReqDto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        try {
            VideoUploadResDto result = videoService.videoUploadWithIdempotency(videoUploadReqDto, userDetails, idempotencyKey);
            return ResponseEntity.ok(result);
        } catch (IdempotencyService.IdempotencyProcessingException e) {
            log.warn("Idempotency Processing Exception: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (IllegalArgumentException e) {
            log.warn("Video upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            log.error("Video upload failed during operation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error during video upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 영상 수정
     * - 요청으로 부터 파일, 게시글 내용을 받아 유효성 검사 후 서비스 계층에 넘김
     * - 가능한 수정 방식 : 파일만 수정, 내용만 수정, 둘다 수정, 둘다 수정 안함
     *
     * @param postSeq 게시글의 고유 번호
     * @param videoUpdateReqDto
     * @param userDetails 인증된 사용자 정보
     * @return
     */
    @PutMapping(value = "/{postSeq}")
    public ResponseEntity<VideoUpdateResDto> videoUpdate(@PathVariable("postSeq") Long postSeq,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @RequestBody VideoUpdateReqDto videoUpdateReqDto){

        Long userId = userDetails.getId();

        VideoUpdateResDto result = videoService.videoUpdate(postSeq, videoUpdateReqDto, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 영상 삭제
     * - 삭제를 원하는 게시글의 식별자를 받아 서비스 계층에 넘김
     *
     * @param postSeq 삭제할 게시글 식별자
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity
     */
    @DeleteMapping("/{postSeq}")
    public ResponseEntity<?> videoDelete(@PathVariable Long postSeq,
                                       @AuthenticationPrincipal CustomUserDetails userDetails){

        Long userId = userDetails.getId();

        videoService.videoDelete(postSeq, userId);

        return ResponseEntity.ok("삭제 완료");
    }
}
