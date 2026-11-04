package com.pawbites.petshop.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pawbites.petshop.result.ApiResponse;

/**
 * 全局异常处理器：拦截控制器抛出的异常，统一转换为 {@link ApiResponse} 错误响应。
 *
 * <p>映射关系：业务异常 → 400（透传业务码与信息）；参数校验失败 → 400；
 * 唯一键冲突（DuplicateKeyException）→ 409；权限不足（AccessDeniedException）→ 403；
 * 其余未捕获异常 → 500 兜底。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理业务异常：透传异常携带的业务码与提示信息，HTTP 状态码返回 400。
   *
   * @param exception 业务异常
   * @return 统一错误响应（code 为业务码，message 为异常信息）
   */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBusinessException(BusinessException exception) {
    return ApiResponse.fail(exception.getCode(), exception.getMessage());
  }

  /**
   * 处理参数校验异常（{@code @Valid} 请求体校验、表单绑定、约束违反），HTTP 状态码返回 400。
   *
   * <p>不回显具体校验细节，统一提示“请求参数不合法”。
   *
   * @param exception 参数校验相关异常
   * @return 统一错误响应（code=400）
   */
  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidationException(Exception exception) {
    return ApiResponse.fail(400, "请求参数不合法");
  }

  /**
   * 处理数据库唯一键冲突（如用户名重复），HTTP 状态码返回 409。
   *
   * @param exception 数据库唯一约束冲突异常
   * @return 统一错误响应（code=409）
   */
  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponse<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
    return ApiResponse.fail(409, "数据已存在");
  }

  /**
   * 处理权限不足异常，HTTP 状态码返回 403。
   *
   * @param exception 访问被拒异常
   * @return 统一错误响应（code=403）
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException exception) {
    return ApiResponse.fail(403, "没有操作权限");
  }

  /**
   * 兜底处理未被上方处理器捕获的所有异常，HTTP 状态码返回 500。
   *
   * <p>不向前端泄露堆栈细节，统一提示“服务暂时不可用”。
   *
   * @param exception 未明确处理的异常
   * @return 统一错误响应（code=500）
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(Exception exception) {
    return ApiResponse.fail(500, "服务暂时不可用");
  }
}
