package com.company.receipt.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ReceiptNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReceiptNotFoundException(
            ReceiptNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFoundException(
            CardNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExistsException(
            CardAlreadyExistsException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(InvalidCardCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCardCredentialsException(
            InvalidCardCredentialsException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(InvalidMatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMatchException(
            InvalidMatchException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ApiCallException.class)
    public ResponseEntity<ErrorResponse> handleApiCallException(
            ApiCallException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .error("Service Unavailable")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(Map.of(
                "apiProvider", ex.getApiProvider(),
                "statusCode", ex.getStatusCode()
            ))
            .build();
        
        log.error("API call failed: provider={}, statusCode={}", 
                 ex.getApiProvider(), ex.getStatusCode(), ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @ExceptionHandler(SyncFailedException.class)
    public ResponseEntity<ErrorResponse> handleSyncFailedException(
            SyncFailedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(Map.of(
                "cardId", ex.getCardId(),
                "syncType", ex.getSyncType()
            ))
            .build();
        
        log.error("Sync failed: cardId={}, syncType={}", 
                 ex.getCardId(), ex.getSyncType(), ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(ex.getErrors())
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .error("Too Many Requests")
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(Map.of(
                "apiProvider", ex.getApiProvider(),
                "retryAfterSeconds", ex.getRetryAfterSeconds()
            ))
            .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode("VALIDATION_ERROR")
            .message("입력값 검증에 실패했습니다")
            .path(request.getDescription(false).replace("uri=", ""))
            .details(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage
            ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode("CONSTRAINT_VIOLATION")
            .message("제약 조건 위반")
            .path(request.getDescription(false).replace("uri=", ""))
            .details(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String message = "데이터 무결성 제약 조건 위반";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "중복된 데이터가 존재합니다";
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .errorCode("DATA_INTEGRITY_VIOLATION")
            .message(message)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        log.error("Data integrity violation", ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .errorCode("ACCESS_DENIED")
            .message("접근 권한이 없습니다")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다",
            ex.getName(), ex.getValue());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(generateTraceId())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .errorCode("TYPE_MISMATCH")
            .message(message)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        String traceId = generateTraceId();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .errorCode("INTERNAL_ERROR")
            .message("처리 중 오류가 발생했습니다. 관리자에게 문의하세요.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        log.error("Unexpected error occurred. TraceId: {}", traceId, ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private String traceId;
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String path;
        private Map<String, ?> details;
    }
}
