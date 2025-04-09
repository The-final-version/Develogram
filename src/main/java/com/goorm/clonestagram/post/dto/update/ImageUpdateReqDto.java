package com.goorm.clonestagram.post.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 수정 요청을 위한 DTO
 * - file, content를 클라이언트에게 받음
 */
@Getter
@Setter
public class ImageUpdateReqDto {

	private String file;
	private String content;
	private List<String> hashTagList;

}
