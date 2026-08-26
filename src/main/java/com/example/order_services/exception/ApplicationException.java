package com.example.order_services.exception;

import com.example.order_services.common.EnumCode;
import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
    private final EnumCode code;

    public ApplicationException(EnumCode code, String message) {
        super(message);
        this.code = code;
    }
}
