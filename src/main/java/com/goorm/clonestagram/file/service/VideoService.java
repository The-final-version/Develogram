package com.goorm.clonestagram.file.service;

import com.goorm.clonestagram.file.EntityType;
import com.goorm.clonestagram.file.domain.Posts;
import com.goorm.clonestagram.file.domain.SoftDelete;
import com.goorm.clonestagram.file.dto.update.VideoUpdateReqDto;
import com.goorm.clonestagram.file.dto.update.VideoUpdateResDto;
import com.goorm.clonestagram.file.dto.upload.VideoUploadReqDto;
import com.goorm.clonestagram.file.dto.upload.VideoUploadResDto;
import com.goorm.clonestagram.file.repository.PostsRepository;
import com.goorm.clonestagram.file.repository.SoftDeleteRepository;
import com.goorm.clonestagram.hashtag.entity.HashTags;
import com.goorm.clonestagram.hashtag.entity.PostHashTags;
import com.goorm.clonestagram.hashtag.repository.PostHashTagRepository;
import com.goorm.clonestagram.hashtag.repository.HashTagRepository;
import com.goorm.clonestagram.user.domain.User;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

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

    @Value("${video.path}")
    private String uploadFolder;

    /**
     * 영상 업로드
     * - 검증이 끝난 파일의 name과 path를 설정하여 DB와 저장소에 저장
     *
     * @param videoUploadReqDto 업로드할 영상과 관련된 요청 DTO
     *          - controller에서 넘어옴
     * @return 성공시 ImageUploadResDto 반환
     * @throws Exception
     *          - 파일 저장시 IOException 발생
     */
    public VideoUploadResDto videoUpload(VideoUploadReqDto videoUploadReqDto, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        //1. unique 파일명을 생성하기 위해 uuid 사용
        UUID uuid = UUID.randomUUID();
        String videoFileName = uuid + "_" + videoUploadReqDto.getFile().getOriginalFilename();
        //2. unique 파일명과 upload 위치를 활용해 파일 경로 생성
        Path videoFilePath = Paths.get(uploadFolder).resolve(videoFileName);

        try{
            //3. 파일 경로를 활용해 디렉토리를 생성하고 파일을 저장
            Files.createDirectories(Paths.get(uploadFolder));
            Files.write(videoFilePath, videoUploadReqDto.getFile().getBytes());

        }catch (IOException e){
            throw new RuntimeException("파일 저장 중 오류 발생 : " + e.getMessage());
        }

        //4. unique 파일명과 videoUploadReqDto의 값들을 활용해 Entity 객체 생성 후 저장
        Posts postEntity = videoUploadReqDto.toEntity(videoFileName, user);
        Posts post = postsRepository.save(postEntity);

        //5. Dto에 있는 HashTagList를 저장
        for (String tagContent : Optional.ofNullable(videoUploadReqDto.getHashTagList())
                                        .orElse(Collections.emptyList())) {
            //5-1. tagList에서 tag 내용 하나를 추출한 후 조회
            HashTags tag = hashTagRepository.findByTagContent(tagContent)
                    //5-2. tag가 저장되어 있지 않으면 새롭게 저장
                    .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
            //5-3. 추출된 태그의 id와 피드의 id를 관계테이블에 저장
            postHashTagRepository.save(new PostHashTags(null,tag,post));
        }

        //6. 모든 작업이 완료된 경우 응답 반환
        return VideoUploadResDto.builder()
                .content(post.getContent())
                .type(post.getContentType())
                .createdAt(post.getCreatedAt())
                .hashTagList(videoUploadReqDto.getHashTagList())
                .build();
    }

    /**
     *
     * @param postSeq 게시글 ID
     * @param videoUpdateReqDto 수정할 게시글과 관련된 요청 DTO
     * @return 성공시 ImageUpdateResDto
     * @exception IllegalArgumentException 게시글을 찾을 수 없을시 발생
     */
    public VideoUpdateResDto videoUpdate(Long postSeq, VideoUpdateReqDto videoUpdateReqDto, Long userId) {

        boolean updated = false;
        //1. 게시글 ID를 통해 게시글을 찾아 반환
        Posts posts = postsRepository.findById(postSeq)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다"));

        if(!posts.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("권한이 없는 유저입니다");
        }

        //2. 영상 수정 여부 파악
        if(videoUpdateReqDto.getFile() != null && !videoUpdateReqDto.getFile().isEmpty()){

            //2-1. unique 파일명을 생성하기 위해 uuid 사용
            UUID uuid = UUID.randomUUID();
            String videoFileName = uuid + "_" + videoUpdateReqDto.getFile().getOriginalFilename();

            //2-2. unique 파일명과 upload 위치를 활용해 파일 경로 생성
            Path videoFilePath = Paths.get(uploadFolder).resolve(videoFileName);

            try{
                //2-3. 파일 경로를 활용해 디렉토리를 생성하고 파일을 저장
                Files.write(videoFilePath, videoUpdateReqDto.getFile().getBytes());
            }catch (IOException e){
                throw new RuntimeException("파일 저장 중 오류 발생 : " + e.getMessage());
            }
            //2-4. 수정된 파일명 반영
            String oldFileName = posts.getMediaName();
            posts.setMediaName(videoFileName);

            //2-5. 기존의 파일 삭제
            Path oldVideoPath = Paths.get(uploadFolder).resolve(oldFileName);
            try{
                Files.deleteIfExists(oldVideoPath);
            } catch (IOException e){
                throw new RuntimeException("파일 삭제 중 오류 발생 : " + e.getMessage());
            }

            //2-6. 업데이트 되었음을 표시
            updated = true;
        }

        //3. 게시글 내용 수정 여부 파악
        if(videoUpdateReqDto.getContent() != null && !videoUpdateReqDto.getContent().trim().isEmpty()){
            //3-1. 수정된 게시글 내용 반영
            posts.setContent(videoUpdateReqDto.getContent());

            //3-2. 업데이트 되었음을 표시
            updated = true;
        }

        //4. 해시태그 수정 여부 파악
        if(videoUpdateReqDto.getHashTagList() != null && !videoUpdateReqDto.getHashTagList().isEmpty()){
            //4-1. 기존의 해시태그 리스트 삭제
            postHashTagRepository.deleteAllByPostsId(posts.getId());

            //4-2. 새롭게 해시 태그 리스트 저장
            for (String tagContent : Optional.ofNullable(videoUpdateReqDto.getHashTagList())
                                            .orElse(Collections.emptyList())) {
                //4-2. tagList에서 tag 내용 하나를 추출한 후 조회
                HashTags tag = hashTagRepository.findByTagContent(tagContent)
                        //4-2. tag가 저장되어 있지 않으면 새롭게 저장
                        .orElseGet(() -> hashTagRepository.save(new HashTags(null, tagContent)));
                //4-3. 추출된 태그의 id와 피드의 id를 관계테이블에 저장
                postHashTagRepository.save(new PostHashTags(null,tag,posts));
            }
        }

        Posts updatedPost;
        if(updated){
            //5. 업데이트된 게시글을 DB에 저장
            updatedPost = postsRepository.save(posts);
        }else{
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
     *
     * @param postSeq 삭제할 게시글 식별자
     */
    public void videoDelete(Long postSeq, Long userId) {
        //1. 식별자를 토대로 게시글 찾아 반환
        Posts posts = postsRepository.findById(postSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다"));

        if(!posts.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("권한이 없는 유저입니다");
        }

        //2. 기존에 존재하던 파일 삭제
        Path filePath = Paths.get(uploadFolder).resolve(posts.getMediaName());
        try{
            Files.deleteIfExists(filePath);
        } catch (IOException e){
            throw new RuntimeException("파일 삭제 중 오류 발생 : " + e.getMessage());
        }

        //3. DB에서 데이터 삭제
        posts.setDeleted(true);
        posts.setDeletedAt(LocalDateTime.now());
        softDeleteRepository.save(new SoftDelete(null, EntityType.POST, posts.getId(), posts.getDeletedAt()));
        postHashTagRepository.deleteAllByPostsId(posts.getId());
    }
}
