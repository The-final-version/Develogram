package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.common.service.IdempotencyService;
import com.goorm.clonestagram.util.CustomUserDetails;
import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.ImageUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadResDto;
import com.goorm.clonestagram.post.service.ImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

/**
 * 이미지 업로드 요청을 처리하는 컨트롤러
 * - 클라이언트로부터 이미지 업로드 요청을 받아 검증 및 서비스 호출을 수행
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/images")
public class ImageController {

    private final ImageService imageService;

    /**
     * 이미지 업로드
     * - 요청으로부터 파일, 게시글 내용, 해시태그 목록을 받아 유효성 검사 후 서비스 계층에 넘김
     *
     * @param imageUploadReqDto 업로드할 이미지 URL, 내용, 해시태그 등을 포함한 DTO
     * @param userDetails 인증된 사용자 정보
     * @param idempotencyKey 멱등성 키 (HTTP 헤더)
     * @return 업로드 성공 시 ImageUploadResDto 반환
     * @throws Exception 업로드 도중 발생할 수 있는 예외
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageUploadResDto> imageUpload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ImageUploadReqDto imageUploadReqDto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Long userId = null;
        try {
            userId = userDetails.getId();
            log.info("👉 [imageUpload] 진입 (UserId: {}, Idempotency Key: {})", userId, idempotencyKey);

            ImageUploadResDto result = imageService.imageUploadWithIdempotency(imageUploadReqDto, userId, idempotencyKey);
            log.info("✅ 이미지 업로드 완료 (UserId: {}, Idempotency Key: {}): {}", userId, idempotencyKey, result);
            return ResponseEntity.ok(result);

        } catch (IdempotencyService.IdempotencyProcessingException e) {
            log.warn("🚫 Idempotency Processing Exception (Key: {}): {}", idempotencyKey, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (IllegalArgumentException e) {
            String logUserId = (userId != null) ? userId.toString() : "N/A";
            log.warn("🚫 Image upload validation failed (UserId: {}, Key: {}): {}", logUserId, idempotencyKey, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            String logUserId = (userId != null) ? userId.toString() : "N/A";
            log.error("❌ Image upload failed during operation (UserId: {}, Key: {}): {}", logUserId, idempotencyKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            String logUserId = (userId != null) ? userId.toString() : "N/A";
            log.error("❌ Unexpected error during image upload (UserId: {}, Key: {}): {}", logUserId, idempotencyKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * 이미지 수정
     * - 요청으로 부터 파일, 게시글 내용을 받아 유효성 검사 후 서비스 계층에 넘김
     * - 가능한 수정 방식 : 파일만 수정, 내용만 수정, 둘다 수정, 둘다 수정 안함
     *
     * @param postSeq 게시글의 고유 번호
     * @param imageUpdateReqDto
     * @param userDetails 인증된 사용자 정보
     * @return
     */
    @PutMapping(value = "/{postSeq}")
    public ResponseEntity<ImageUpdateResDto> imageUpdate(@PathVariable("postSeq") Long postSeq,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @Valid @RequestBody ImageUpdateReqDto imageUpdateReqDto){
        Long userId = userDetails.getId();

        ImageUpdateResDto result = imageService.imageUpdate(postSeq, imageUpdateReqDto, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 이미지 삭제
     * - 삭제를 원하는 게시글의 식별자를 받아 서비스 계층에 넘김
     *
     * @param postSeq 삭제할 게시글 식별자
     * @param userDetails 인증된 사용자 정보
     * @return ResponseEntity
     */
    @DeleteMapping("/{postSeq}")
    public ResponseEntity<?> imageDelete(@PathVariable("postSeq") Long postSeq,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        imageService.imageDelete(postSeq, userId);
        return ResponseEntity.ok("삭제 완료");
    }
}
