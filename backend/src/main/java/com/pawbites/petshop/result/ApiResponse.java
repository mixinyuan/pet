package com.pawbites.petshop.result;

import java.time.LocalDateTime;

public record ApiResponse<T>(int code, String message, T data, LocalDateTime timestamp) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(0, "success", data, LocalDateTime.now());
  }

  public static ApiResponse<Void> success() {
    return success(null);
  }

  public static ApiResponse<Void> fail(int code, String message) {
    return new ApiResponse<>(code, message, null, LocalDateTime.now());
  }
}
