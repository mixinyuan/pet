package com.pawbites.petshop.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.pawbites.petshop.result.ApiResponse;

/**
 * 全局异常处理器：拦截控制器抛出的异常，统一转换为 {@link ApiResponse} 错误响应，
 * 并保证 HTTP 状态码与业务码一致，避免 4xx 客户端错误被兜底逻辑吞成 500。
 *
 * <p>映射关系：业务异常按业务码映射（401/404/409，其余 400）；请求体不可读、参数类型
 * 不匹配、缺少必填参数、参数校验失败 → 400；请求路径不存在 → 404；请求方法不支持 → 405；
 * 唯一键冲突（DuplicateKeyException）→ 409；权限不足（AccessDeniedException）→ 403；
 * 其余未捕获异常 → 500 兜底并记录 error 日志。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 处理业务异常：透传异常携带的业务码与提示信息，并按业务码映射真实 HTTP 状态码
   * （401 → UNAUTHORIZED、404 → NOT_FOUND、409 → CONFLICT，其余 → BAD_REQUEST），
   * 保证前端既能拿到正确的状态码又能读到原始业务码与信息。
   *
   * @param exception 业务异常
   * @return 统一错误响应（code/message 保留异常原值，HTTP 状态码与业务码对齐）
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    log.warn("业务异常：code={}, message={}", exception.getCode(), exception.getMessage());
    HttpStatus status = switch (exception.getCode()) {
      case 401 -> HttpStatus.UNAUTHORIZED;
      case 404 -> HttpStatus.NOT_FOUND;
      case 409 -> HttpStatus.CONFLICT;
      default -> HttpStatus.BAD_REQUEST;
    };
    return ResponseEntity.status(status)
        .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
  }

  /**
   * 处理请求体参数校验异常（{@code @Valid @RequestBody}），HTTP 状态码返回 400。
   *
   * <p>拼接首个字段级校验错误的字段名与提示，如“请求参数不合法：username 不能为空”，
   * 便于前端定位具体问题字段。
   *
   * @param exception 请求体参数校验异常
   * @return 统一错误响应（code=400，message 附带首个校验失败的字段与原因）
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
    String detail = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .orElse("参数不合法");
    return ApiResponse.fail(400, "请求参数不合法：" + detail);
  }

  /**
   * 处理表单绑定与约束违反异常（表单校验、{@code @Validated} 参数约束），HTTP 状态码返回 400。
   *
   * <p>不回显具体校验细节，统一提示“请求参数不合法”。
   *
   * @param exception 参数校验相关异常
   * @return 统一错误响应（code=400）
   */
  @ExceptionHandler({BindException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidationException(Exception exception) {
    return ApiResponse.fail(400, "请求参数不合法");
  }

  /**
   * 处理请求体不可读异常（JSON 格式错误、字段类型不匹配导致反序列化失败），
   * HTTP 状态码返回 400。
   *
   * @param exception 请求体解析异常
   * @return 统一错误响应（code=400）
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
    return ApiResponse.fail(400, "请求体格式不合法");
  }

  /**
   * 处理请求参数类型不匹配异常（如路径/查询参数无法转换为目标类型），HTTP 状态码返回 400。
   *
   * @param exception 参数类型不匹配异常
   * @return 统一错误响应（code=400）
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
    return ApiResponse.fail(400, "请求参数类型不合法");
  }

  /**
   * 处理缺少必填请求参数异常（{@code @RequestParam required=true} 缺失），HTTP 状态码返回 400。
   *
   * @param exception 缺少必填参数异常
   * @return 统一错误响应（code=400）
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException exception) {
    return ApiResponse.fail(400, "缺少必填参数");
  }

  /**
   * 处理请求方法不支持异常（如对仅支持 GET 的接口发 POST），HTTP 状态码返回 405。
   *
   * @param exception 请求方法不支持异常
   * @return 统一错误响应（code=405）
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ApiResponse<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
    return ApiResponse.fail(405, "请求方法不支持");
  }

  /**
   * 处理静态资源/路径不存在异常（未匹配到任何控制器映射），HTTP 状态码返回 404。
   *
   * @param exception 请求路径不存在异常
   * @return 统一错误响应（code=404）
   */
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException exception) {
    return ApiResponse.fail(404, "请求路径不存在");
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
    return ApiResponse.fail(409, "数据已存在，请勿重复提交");
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
   * <p>记录 error 级别日志（含完整堆栈）便于事后排查，但不向前端泄露堆栈细节，
   * 统一提示“服务暂时不可用”。
   *
   * @param exception 未明确处理的异常
   * @return 统一错误响应（code=500）
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(Exception exception) {
    log.error("未处理异常", exception);
    return ApiResponse.fail(500, "服务暂时不可用");
  }
}
