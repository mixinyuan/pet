package com.pawbites.petshop.common.exception;

import com.pawbites.petshop.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBusinessException(BusinessException exception) {
    return ApiResponse.fail(exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidationException(Exception exception) {
    return ApiResponse.fail(400, "请求参数不合法");
  }

  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponse<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
    return ApiResponse.fail(409, "数据已存在");
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException exception) {
    return ApiResponse.fail(403, "没有操作权限");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(Exception exception) {
    return ApiResponse.fail(500, "服务暂时不可用");
  }
}
