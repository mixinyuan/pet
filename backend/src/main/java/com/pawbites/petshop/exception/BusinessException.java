package com.pawbites.petshop.exception;

/**
 * 业务异常：携带业务错误码 code 与提示信息 message，
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为统一响应体。
 */
public class BusinessException extends RuntimeException {

  /** 业务错误码（非 HTTP 状态码，如 400、409），会透传给前端。 */
  private final int code;

  /**
   * 以默认错误码 400 构造业务异常。
   *
   * @param message 业务错误提示信息
   */
  public BusinessException(String message) {
    this(400, message);
  }

  /**
   * 以指定错误码构造业务异常。
   *
   * @param code    业务错误码
   * @param message 业务错误提示信息
   */
  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * 获取业务错误码。
   *
   * @return 业务错误码
   */
  public int getCode() {
    return code;
  }
}
