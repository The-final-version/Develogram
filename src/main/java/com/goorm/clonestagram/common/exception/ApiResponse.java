package com.goorm.clonestagram.common.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final ResultType result;
    private final int         httpStatus;
    private final String      errorCode;
    private final String      errorMessage;   // **프론트가 읽던 위치 그대로 유지**
    private final List<String> errorDetail;
    private final T           data;

    private ApiResponse(ResultType result, int status,
        String errorCode, String errorMessage,
        List<String> errorDetail, T data) {
        this.result       = result;
        this.httpStatus   = status;
        this.errorCode    = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetail  = errorDetail;
        this.data         = data;
    }
    /**
     * 에러 응답 (요약 메시지와 상세 메시지 모두 전달)
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, List<String> errorDetail, int httpStatus, BindingResult br) {
        return new ApiResponse<>(ResultType.ERROR, httpStatus, errorCode, errorMessage, createErrorDetail(br, errorDetail), null);
    }


    /**
     * 에러 응답 (요약 메시지와 상세 메시지 모두 전달)
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, List<String> errorDetail, int httpStatus) {
        return new ApiResponse<>(ResultType.ERROR, httpStatus, errorCode, errorMessage, errorDetail, null);
    }

    /* ===== 실패 ===== */
    public static <T> ApiResponse<T> error(ErrorCode ec) {
        return new ApiResponse<>(ResultType.ERROR, ec.getHttpStatus().value(),
            ec.getCode(), ec.getMessage(), null, null);
    }

    /* ===== BindingResult → detail list ===== */
    public static <T> ApiResponse<T> error(ErrorCode ec, List<String> errorDetail) {
        return new ApiResponse<>(ResultType.ERROR, ec.getHttpStatus().value(),
            ec.getCode(), ec.getMessage(), errorDetail, null);
    }

    private static List<String> createErrorDetail(BindingResult bindingResult, List<String> errorDetail) {
        List<String> errorList = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("필드 [").append(fieldError.getField()).append("]: ");

            // FieldError의 코드 배열을 통해 누락 또는 형식 오류인지 판단
            String[] codes = fieldError.getCodes();
            boolean isMissing = false;
            boolean isTypeMismatch = false;
            if (codes != null) {
                for (String code : codes) {
                    if (code != null) {
                        if (code.contains("NotNull") || code.contains("NotBlank") || code.contains("NotEmpty")) {
                            isMissing = true;
                        }
                        if (code.contains("typeMismatch")) {
                            isTypeMismatch = true;
                        }
                    }
                }
            }
            if (isMissing) {
                errorMsg.append("필수 입력값이 누락되었습니다. ");
            }
            if (isTypeMismatch) {
                errorMsg.append("형식이 올바르지 않습니다. ");
            }
            // 기본 에러 메시지 추가
            errorMsg.append(fieldError.getDefaultMessage());
            errorList.add(errorMsg.toString());
        }
		errorList.addAll(errorDetail);
        return errorList;
    }
}
