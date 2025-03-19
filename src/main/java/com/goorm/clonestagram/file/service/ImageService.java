package com.goorm.clonestagram.file.service;

import com.goorm.clonestagram.file.domain.Posts;
import com.goorm.clonestagram.file.dto.ImageUpdateReqDto;
import com.goorm.clonestagram.file.dto.ImageUpdateResDto;
import com.goorm.clonestagram.file.dto.ImageUploadReqDto;
import com.goorm.clonestagram.file.dto.ImageUploadResDto;
import com.goorm.clonestagram.file.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이미지 업로드 요청을 처리하는 서비스
 * - 검증이 완료된 이미지를 받아 업로드 서비스 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final PostsRepository postsRepository;

    @Value("${image.path}")
    private String uploadFolder;

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
    public ImageUploadResDto imageUpload(ImageUploadReqDto imageUploadReqDto/*, long userId*/) throws Exception {
//        User userEntity = userRepository.findByid(userId).orElseThrow(null);

        //1. unique 파일명을 생성하기 위해 uuid 사용
        UUID uuid = UUID.randomUUID();
        String imageFileName = uuid + "_" + imageUploadReqDto.getFile().getOriginalFilename();
        log.info(imageFileName);
        //2. unique 파일명과 upload 위치를 활용해 파일 경로 생성
        Path imageFilePath = Paths.get(uploadFolder).resolve(imageFileName);

        try{
            //3. 파일 경로를 활용해 디렉토리를 생성하고 파일을 저장
            Files.createDirectories(Paths.get(uploadFolder));
            Files.write(imageFilePath, imageUploadReqDto.getFile().getBytes());

        }catch (IOException e){
            throw new RuntimeException("파일 저장 중 오류 발생 : " + e.getMessage());
        }

        //4. unique 파일명과 imageUploadReqDto의 값들을 활용해 Entity 객체 생성 후 저장
        Posts postEntity = imageUploadReqDto.toEntity(imageFileName);
        Posts post = postsRepository.save(postEntity);

        //5. 모든 작업이 완료된 경우 응답 반환
        return ImageUploadResDto.builder()
                .content(post.getContent())
                .type(post.getContentType())
                .createdAt(post.getCreatedAt())
                .build();
    }

    /**
     *
     * @param postSeq 게시글 ID
     * @param imageUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @return 성공시 ImageUpdateResDto
     * @exception IllegalArgumentException 게시글을 찾을 수 없을시 발생
     */
    public ImageUpdateResDto imageUpdate(Long postSeq, ImageUpdateReqDto imageUpdateReqDto) {

        boolean updated = false;
        //1. 게시글 ID를 통해 게시글을 찾아 반환
        Posts posts = postsRepository.findById(postSeq)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다"));

        //2. 이미지 수정 여부 파악
        if(imageUpdateReqDto.getFile() != null && !imageUpdateReqDto.getFile().isEmpty()){

            //2-1. unique 파일명을 생성하기 위해 uuid 사용
            UUID uuid = UUID.randomUUID();
            String imageFileName = uuid + "_" + imageUpdateReqDto.getFile().getOriginalFilename();

            //2-2. unique 파일명과 upload 위치를 활용해 파일 경로 생성
            Path imageFilePath = Paths.get(uploadFolder).resolve(imageFileName);

            try{
                //2-3. 파일 경로를 활용해 디렉토리를 생성하고 파일을 저장
                Files.write(imageFilePath, imageUpdateReqDto.getFile().getBytes());
            }catch (IOException e){
                throw new RuntimeException("파일 저장 중 오류 발생 : " + e.getMessage());
            }
            //2-4. 수정된 파일명 반영
            String oldFileName = posts.getMediaName();
            posts.setMediaName(imageFileName);

            //2-5. 기존의 파일 삭제
            Path oldImagePath = Paths.get(uploadFolder + oldFileName);
            try{
                Files.deleteIfExists(oldImagePath);
            } catch (IOException e){
                throw new RuntimeException("파일 삭제 중 오류 발생 : " + e.getMessage());
            }

            //2-6. 업데이트 되었음을 표시
            updated = true;
        }

        //3. 게시글 내용 수정 여부 파악
        if(imageUpdateReqDto.getContent() != null && !imageUpdateReqDto.getContent().trim().isEmpty()){
            //3-1. 수정된 게시글 내용 반영
            posts.setContent(imageUpdateReqDto.getContent());

            //3-2. 업데이트 되었음을 표시
            updated = true;
        }

        Posts updatedPost;
        if(updated){
            //4. 업데이트 된 경우 업데이트일시 반영
            posts.setUpdatedAt(LocalDateTime.now());

            //5. 업데이트된 게시글을 DB에 저장
            updatedPost = postsRepository.save(posts);
        }else{
            updatedPost = posts;
        }


        //6. 모든 작업이 완료된 경우 응답 반환
        return ImageUpdateResDto.builder()
                .content(updatedPost.getContent())
                .type(updatedPost.getContentType())
                .updatedAt(updatedPost.getUpdatedAt())
                .build();
    }
}
