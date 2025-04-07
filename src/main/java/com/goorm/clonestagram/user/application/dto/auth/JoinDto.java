package com.goorm.clonestagram.user.application.dto.auth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = JoinDto.JoinDtoBuilder.class)
public class JoinDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "유효한 이메일을 입력해주세요.")
    private String email;


    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
    @Size(max = 20, message = "비밀번호는 20자 이하이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 재입력을 입력해주세요.")
    private String confirmPassword;

    @NotBlank(message = "이름을 입력해주세요.")
    private String username;

    @JsonPOJOBuilder(withPrefix = "")
    public static class JoinDtoBuilder {
    }
}
