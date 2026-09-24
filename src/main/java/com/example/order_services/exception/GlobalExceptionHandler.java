package com.example.order_services.exception;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.common.EnumCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convert MVC-layer exceptions into HTTP statuses and a consistent BaseResponse structure. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<BaseResponse<Void>> handleRouteNotFound() {
        return ResponseEntity.status(404).body(BaseResponse.error(EnumCode.NOT_FOUND, "Route not found"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<BaseResponse<Void>> handleAccessDeniedException() {
        return ResponseEntity.status(403).body(BaseResponse.error(EnumCode.FORBIDDEN, "Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<BaseResponse<Void>> handleAuthenticationException() {
        return ResponseEntity.status(401).body(BaseResponse.error(EnumCode.UNAUTHORIZED, "Authentication required"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<BaseResponse<Void>> handleInvalidRequestBody() {
        return ResponseEntity.badRequest().body(BaseResponse.error(EnumCode.BAD_REQUEST, "Invalid request body"));
    }

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
        // Return errors by field name so the UI can attach messages to the corresponding inputs.
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

    // Return a generic message for unclassified errors to avoid exposing internal details in the response.
    @ExceptionHandler(Exception.class)
    ResponseEntity<BaseResponse<Void>> handleUnexpectedException() {
        return ResponseEntity.internalServerError()
                .body(BaseResponse.error(EnumCode.INTERNAL_ERROR, EnumCode.INTERNAL_ERROR.getMessage()));
    }
}
