package com.example.order_services.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EnumCode {
    SUCCESS("SUCCESS", "Request succeeded", HttpStatus.OK),
    BAD_REQUEST("BAD_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    EnumCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
