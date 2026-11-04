package com.pawbites.petshop.result;

import java.time.LocalDateTime;

/**
 * 统一响应体：所有接口的返回均包装为本结构。
 *
 * <p>约定：code=0 表示成功，非 0 表示失败（通常与 HTTP 状态码对齐，如 400/403/409/500）。
 *
 * @param code      业务码：0 成功，非 0 失败
 * @param message   提示信息，成功固定为 "success"
 * @param data      业务数据，失败时为 null
 * @param timestamp 服务器生成响应的时间
 */
public record ApiResponse<T>(int code, String message, T data, LocalDateTime timestamp) {

  /**
   * 构造携带数据的成功响应。
   *
   * @param data 业务数据
   * @param <T>  数据类型
   * @return code=0、message="success" 的成功响应
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(0, "success", data, LocalDateTime.now());
  }

  /**
   * 构造无数据的成功响应。
   *
   * @return code=0 的成功响应，data 为 null
   */
  public static ApiResponse<Void> success() {
    return success(null);
  }

  /**
   * 构造失败响应。
   *
   * @param code    业务错误码（非 0）
   * @param message 错误提示信息
   * @return 携带错误码与提示信息的失败响应
   */
  public static ApiResponse<Void> fail(int code, String message) {
    return new ApiResponse<>(code, message, null, LocalDateTime.now());
  }
}
