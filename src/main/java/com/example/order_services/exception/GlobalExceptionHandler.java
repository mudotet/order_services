package com.example.order_services.exception;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.common.EnumCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<BaseResponse<Void>> handleApplicationException(ApplicationException exception) {
        EnumCode code = exception.getCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(BaseResponse.error(code, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<BaseResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(EnumCode.BAD_REQUEST, "Validation failed", errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolation() {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(EnumCode.BAD_REQUEST, "Invalid related resource"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<BaseResponse<Void>> handleUnexpectedException() {
        return ResponseEntity.internalServerError()
                .body(BaseResponse.error(EnumCode.INTERNAL_ERROR, EnumCode.INTERNAL_ERROR.getMessage()));
    }
}
