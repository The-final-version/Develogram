package com.goorm.clonestagram.post.controller;

import com.goorm.clonestagram.util.CustomUserDetails;
import com.goorm.clonestagram.post.dto.update.ImageUpdateReqDto;
import com.goorm.clonestagram.post.dto.update.ImageUpdateResDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadReqDto;
import com.goorm.clonestagram.post.dto.upload.ImageUploadResDto;
import com.goorm.clonestagram.post.service.ImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
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
public class ImageController {

    private final ImageService imageService;

    /**
     * 이미지 업로드
     * - 요청으로부터 이미지 URL과 내용을 받아 서비스 계층에 전달 (서비스에서 URL 사용 가정)
     *
     * @param imageUploadReqDto 업로드할 이미지 URL, 내용, 해시태그 등을 포함한 DTO
     * @return 업로드 성공 시 ImageUploadResDto 반환
     * @throws Exception 업로드 도중 발생할 수 있는 예외
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/image", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageUploadResDto> imageUpload(
            @AuthenticationPrincipal CustomUserDetails userDetail,
            @RequestBody ImageUploadReqDto imageUploadReqDto
    ) {
        try {
            log.info("👉 [imageUpload] 진입");

            if (userDetail == null) {
                log.warn("🚫 인증된 사용자 정보가 없습니다.");
                return ResponseEntity.status(403).build();
            }
            Long userId = userDetail.getId();
            log.info("✅ 인증된 사용자 ID: {}", userId);

            ImageUploadResDto result = imageService.imageUpload(imageUploadReqDto, userId);
            log.info("✅ 이미지 업로드 완료: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ 이미지 업로드 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }


    /**
     * 이미지 수정
     * - 요청으로 부터 파일, 게시글 내용을 받아 유효성 검사 후 서비스 계층에 넘김
     * - 가능한 수정 방식 : 파일만 수정, 내용만 수정, 둘다 수정, 둘다 수정 안함
     *
     * @param postSeq 게시글의 고유 번호
     * @param imageUpdateReqDto
     * @return
     */
    @PutMapping(value = "/image/{postSeq}")
    public ResponseEntity<ImageUpdateResDto> imageUpdate(@PathVariable("postSeq") Long postSeq,
                                                         @AuthenticationPrincipal CustomUserDetails userDetail,
                                                         @RequestBody ImageUpdateReqDto imageUpdateReqDto){
        Long userId = userDetail.getId();

        return ResponseEntity.ok(imageService.imageUpdate(postSeq, imageUpdateReqDto, userId));
    }

    /**
     * 이미지 삭제
     * - 삭제를 원하는 게시글의 식별자를 받아 서비스 계층에 넘김
     *
     * @param postSeq 삭제할 게시글 식별자
     * @return ResponseEntity
     */
    @DeleteMapping("/image/{postSeq}")
    public ResponseEntity<?> imageDelete(@PathVariable("postSeq") Long postSeq,
        @AuthenticationPrincipal CustomUserDetails userDetail) {

        Long userId = userDetail.getId();
        imageService.imageDelete(postSeq, userId);
        return ResponseEntity.ok("삭제 완료");
    }
}
