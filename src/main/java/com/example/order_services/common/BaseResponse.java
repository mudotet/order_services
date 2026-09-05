package com.example.order_services.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class BaseResponse<T> {
    private final String code;
    private final String message;
    private final T data;
    // Give extra info if have
    private final Map<String, ?> metadata;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(
                EnumCode.SUCCESS.getCode(),
                EnumCode.SUCCESS.getMessage(),
                data,
                Map.of()
        );
    }

    public static BaseResponse<Void> error(EnumCode code, String message) {
        return error(code, message, Map.of());
    }

    public static BaseResponse<Void> error(
            EnumCode code,
            String message,
            Map<String, ?> metadata
    ) {
        return new BaseResponse<>(code.getCode(), message, null, metadata);
    }
}
