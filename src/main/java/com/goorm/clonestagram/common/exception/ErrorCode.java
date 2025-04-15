package com.goorm.clonestagram.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** AUTH = 인증·인가,  USER = 사용자/도메인 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /* ========== AUTH (인증·인가) ========== */
    AUTH_INVALID_CREDENTIALS   (HttpStatus.UNAUTHORIZED, "AUTH_001", "이메일 또는 비밀번호가 잘못되었습니다."),
    AUTH_DUPLICATE_LOGIN       (HttpStatus.FORBIDDEN,    "AUTH_002", "다른 기기에서 로그인되었습니다."),
    AUTH_INVALID_JWT           (HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 JWT 토큰입니다."),
    AUTH_TOKEN_MISSING         (HttpStatus.UNAUTHORIZED, "AUTH_005", "JWT 토큰이 존재하지 않습니다."),
    AUTH_EXPIRED_JWT           (HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 JWT 토큰입니다."),

    /* ========== USER (도메인) ========== */
    USER_NOT_FOUND             (HttpStatus.NOT_FOUND,    "USER_001", "해당 사용자가 존재하지 않습니다."),
    USER_DUPLICATE_EMAIL       (HttpStatus.CONFLICT,     "USER_002", "이미 사용 중인 이메일입니다."),
    USER_DUPLICATE_name    (HttpStatus.CONFLICT,     "USER_003", "이미 사용 중인 사용자 이름입니다."),
    USER_PASSWORD_MISMATCH     (HttpStatus.BAD_REQUEST,  "USER_119", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    USER_BIO_LENGTH            (HttpStatus.BAD_REQUEST,  "USER_101", "자기소개는 300자 이내로 작성해야 합니다."),
    USER_IMAGE_URL_BLANK       (HttpStatus.BAD_REQUEST,  "USER_102", "URL을 입력해주세요."),
    USER_IMAGE_URL_LENGTH      (HttpStatus.BAD_REQUEST,  "USER_103", "URL은 255자 이하여야 합니다."),
    USER_IMAGE_URL_SPACE       (HttpStatus.BAD_REQUEST,  "USER_104", "URL에 공백이 포함될 수 없습니다."),
    USER_EMAIL_BLANK           (HttpStatus.BAD_REQUEST,  "USER_105", "이메일을 입력해주세요."),
    USER_EMAIL_FORMAT          (HttpStatus.BAD_REQUEST,  "USER_106", "올바른 이메일 형식이 아닙니다."),
    USER_EMAIL_LENGTH          (HttpStatus.BAD_REQUEST,  "USER_107", "이메일은 50자 이하로 입력해주세요."),
    USER_NAME_BLANK            (HttpStatus.BAD_REQUEST,  "USER_108", "이름을 입력해주세요."),
    USER_NAME_TOO_SHORT        (HttpStatus.BAD_REQUEST,  "USER_109", "이름은 2자 이상이어야 합니다."),
    USER_NAME_TOO_LONG         (HttpStatus.BAD_REQUEST,  "USER_110", "이름은 20자 이하이어야 합니다."),
    USER_NAME_NUMBER           (HttpStatus.BAD_REQUEST,  "USER_111", "이름에 숫자가 포함될 수 없습니다."),
    USER_NAME_SPECIAL          (HttpStatus.BAD_REQUEST,  "USER_112", "이름에 특수문자가 포함될 수 없습니다."),
    USER_PASSWORD_BLANK        (HttpStatus.BAD_REQUEST,  "USER_113", "비밀번호를 입력해주세요."),
    USER_PASSWORD_TOO_SHORT    (HttpStatus.BAD_REQUEST,  "USER_114", "비밀번호는 6자 이상이어야 합니다."),
    USER_PASSWORD_TOO_LONG     (HttpStatus.BAD_REQUEST,  "USER_115", "비밀번호는 20자 이하이어야 합니다."),
    USER_PASSWORD_SPACE        (HttpStatus.BAD_REQUEST,  "USER_116", "비밀번호에 공백이 포함될 수 없습니다."),
    USER_PASSWORD_NO_NUMBER    (HttpStatus.BAD_REQUEST,  "USER_117", "비밀번호는 최소 하나의 숫자를 포함해야 합니다."),
    USER_PASSWORD_NO_SPECIAL   (HttpStatus.BAD_REQUEST,  "USER_118", "비밀번호는 최소 하나의 특수문자를 포함해야 합니다."),


    /* ========== 검증 ========== */
    INVALID_PARAMETER         (HttpStatus.BAD_REQUEST,  "USER_100", "잘못된 요청입니다."),


    /* ========== 시스템 ========== */
    USER_DATABASE_ERROR        (HttpStatus.INTERNAL_SERVER_ERROR, "USER_998", "데이터베이스 처리 중 오류가 발생했습니다."),

    /* ========== 공통 에러 ========== */
    BODY_NOT_READABLE(HttpStatus.BAD_REQUEST, "C-003", "요청 바디가 누락되었거나 형식이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E-405", "지원하지 않는 HTTP 메서드입니다.");

    private final HttpStatus httpStatus;
    private final String     code;
    private final String     message;
}
