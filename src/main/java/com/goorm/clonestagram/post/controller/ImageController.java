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
     * 이미지 업로드 (멱등성 적용)
     * - 요청으로부터 이미지 URL과 내용을 받아 서비스 계층에 전달 (서비스에서 URL 사용 가정)
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
            @RequestBody ImageUploadReqDto imageUploadReqDto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        try {
            log.info("👉 [imageUpload] 진입 (Idempotency Key: {})", idempotencyKey);

            ImageUploadResDto result = imageService.imageUploadWithIdempotency(imageUploadReqDto, userDetails, idempotencyKey);
            log.info("✅ 이미지 업로드 완료 (Idempotency Key: {}): {}", idempotencyKey, result);
            return ResponseEntity.ok(result);

        } catch (IdempotencyService.IdempotencyProcessingException e) {
            log.warn("🚫 Idempotency Processing Exception (Key: {}): {}", idempotencyKey, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (IllegalArgumentException e) {
            log.warn("🚫 Image upload validation failed (Key: {}): {}", idempotencyKey, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            log.error("❌ Image upload failed during operation (Key: {}): {}", idempotencyKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("❌ Unexpected error during image upload (Key: {}): {}", idempotencyKey, e.getMessage(), e);
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
                                                         @RequestBody ImageUpdateReqDto imageUpdateReqDto){
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
    public ResponseEntity<?> imageDelete(@PathVariable Long postSeq,
                                       @AuthenticationPrincipal CustomUserDetails userDetails){

        Long userId = userDetails.getId();

        imageService.imageDelete(postSeq, userId);

        return ResponseEntity.ok("삭제 완료");
    }
}
