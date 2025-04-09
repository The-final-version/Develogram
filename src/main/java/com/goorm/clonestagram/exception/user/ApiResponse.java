package com.goorm.clonestagram.exception.user;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;


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

    /* ===== 성공 ===== */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, HttpStatus.OK.value(),
            null, null, null, data);
    }

    /* ===== 실패 ===== */
    public static <T> ApiResponse<T> error(ErrorCode ec) {
        return new ApiResponse<>(ResultType.ERROR, ec.getStatus().value(),
            ec.getCode(), ec.getMessage(), null, null);
    }
    public static <T> ApiResponse<T> error(ErrorCode ec, BindingResult br) {
        return new ApiResponse<>(ResultType.ERROR, ec.getStatus().value(),
            ec.getCode(), ec.getMessage(),
            createErrorDetail(br), null);
    }

    /* ===== BindingResult → detail list ===== */
    private static List<String> createErrorDetail(BindingResult br) {
        List<String> details = new ArrayList<>();
        for (FieldError fe : br.getFieldErrors()) {

            String reason;
            if (contains(fe, "NotNull", "NotBlank", "NotEmpty"))       reason = "필수 입력값 누락";
            else if (contains(fe, "Size", "Length"))                  reason = "길이 제한 위반";
            else if (contains(fe, "Pattern"))                         reason = "형식 오류";
            else if (contains(fe, "Min", "Max", "Range"))             reason = "값 범위 초과";
            else if (contains(fe, "typeMismatch"))                    reason = "타입 불일치";
            else                                                      reason = "검증 실패";

            StringBuilder msg = new StringBuilder()
                .append("필드 [").append(fe.getField()).append("] - ").append(reason);
            if (fe.getRejectedValue() != null) {
                msg.append(" (입력값: ").append(fe.getRejectedValue()).append(")");
            }
            if (fe.getDefaultMessage() != null) {
                msg.append(" : ").append(fe.getDefaultMessage());
            }
            details.add(msg.toString());
        }
        return details;
    }
    private static boolean contains(FieldError fe, String... keys) {
        if (fe.getCodes() == null) return false;
        for (String c : fe.getCodes()) for (String k : keys)
            if (c != null && c.contains(k)) return true;
        return false;
    }
}
