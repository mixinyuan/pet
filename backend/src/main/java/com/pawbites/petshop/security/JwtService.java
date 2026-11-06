package com.pawbites.petshop.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.User;

/**
 * 手写 JWT 服务（不依赖 jjwt 等第三方库），负责令牌的签发与解析校验。
 *
 * <p>关键设计点：
 * <ul>
 *   <li>签名算法固定为 HS256（HMAC-SHA256），验签时不信任 token 头部的 alg 字段，防止算法降级攻击；</li>
 *   <li>验签采用常量时间比较，逐字节异或后统一判定，防止时序攻击；</li>
 *   <li>解析时强制校验 exp 过期时间，缺失或已过期均视为无效令牌；</li>
 *   <li>各段编码统一使用 Base64 URL 安全编码且无填充，符合 JWT 规范。</li>
 * </ul>
 */
@Service
public class JwtService {

  /** Base64 URL 安全编码器（无填充），用于编码 JWT 的 header/payload/签名段。 */
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  /** Base64 URL 安全解码器，用于还原 payload 段的原始 JSON 字节。 */
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  /** Jackson 类型引用：payload JSON → {@code Map<String, Object>}。 */
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  @Resource
  private ObjectMapper objectMapper;

  /** JWT 签名密钥，来自配置项 app.jwt.secret。 */
  @Value("${app.jwt.secret}")
  private String secret;

  /** 令牌有效期（小时），来自配置项 app.jwt.expire-hours。 */
  @Value("${app.jwt.expire-hours}")
  private long expireHours;

  /**
   * 启动期配置校验（fail-fast）：签名密钥为空白或 UTF-8 字节数不足 32、
   * 或令牌有效期不足 1 小时，均立即抛出 {@link IllegalStateException} 终止启动，
   * 避免应用携带弱密钥/非法有效期进入运行态后才暴露问题。
   *
   * @throws IllegalStateException 密钥未配置或过短、有效期非法时抛出，启动即失败
   */
  @PostConstruct
  public void validateConfiguration() {
    if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT secret 未配置或过短：请设置环境变量 JWT_SECRET（至少 32 字节）");
    }
    if (expireHours < 1) {
      throw new IllegalStateException("JWT 有效期配置不合法：请设置环境变量 JWT_EXPIRE_HOURS（app.jwt.expire-hours 至少为 1 小时，当前 " + expireHours + "）");
    }
  }

  /**
   * 为指定用户签发 JWT。
   *
   * <p>header 固定为 {@code {"alg":"HS256","typ":"JWT"}}；payload 携带 sub/userId/username/nickname/role，
   * 以及签发时间 iat 和过期时间 exp（当前时间 + expireHours 小时）。
   *
   * @param user 已通过认证（登录或注册成功）的用户实体
   * @return 形如 header.payload.signature 的三段式 JWT 字符串
   */
  public String generateToken(User user) {
    Instant now = Instant.now();
    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", String.valueOf(user.getId()));
    payload.put("userId", user.getId());
    // 用户名/昵称为 null 时落空串而非字符串 "null"（nickname 允许为空）
    payload.put("username", Objects.toString(user.getUsername(), ""));
    payload.put("nickname", Objects.toString(user.getNickname(), ""));
    // 角色（ADMIN/USER），非 ADMIN 一律按普通用户落库，防脏值进入令牌
    payload.put("role", StatusConstants.ROLE_ADMIN.equals(user.getRole())
        ? StatusConstants.ROLE_ADMIN
        : StatusConstants.ROLE_USER);
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusSeconds(expireHours * 3600).getEpochSecond());

    String headerPart = encodeJson(header);
    String payloadPart = encodeJson(payload);
    String content = headerPart + "." + payloadPart;
    return content + "." + sign(content);
  }

  /**
   * 解析并校验 JWT，还原认证主体。
   *
   * <p>校验流程：结构必须为三段 → 常量时间比较签名 → 校验 exp 未过期 → 解析 payload。
   * 任何一步失败或抛出异常均不向外传播，统一返回 {@code null}。
   *
   * @param token 待校验的 JWT 字符串
   * @return 认证主体；以下任一情况返回 {@code null}：格式错误（段数不为 3、Base64/JSON 解析失败）、
   *         签名不符、exp 缺失或已过期
   */
  public AuthPrincipal parseToken(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return null;
      }

      String content = parts[0] + "." + parts[1];
      if (!constantTimeEquals(sign(content), parts[2])) {
        return null;
      }

      Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), MAP_TYPE);
      Number exp = (Number) payload.get("exp");
      if (exp == null || exp.longValue() < Instant.now().getEpochSecond()) {
        return null;
      }

      Number userId = (Number) payload.get("userId");
      // 兼容历史 token 中可能存在的 null 值，统一还原为空串
      String username = Objects.toString(payload.get("username"), "");
      String nickname = Objects.toString(payload.get("nickname"), "");
      // 兼容升级前签发的旧 token（无 role claim）：默认按普通用户 USER 处理
      String role = StatusConstants.ROLE_ADMIN.equals(payload.get("role"))
          ? StatusConstants.ROLE_ADMIN
          : StatusConstants.ROLE_USER;
      return new AuthPrincipal(userId.longValue(), username, nickname, role);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * 将 Map 序列化为 JSON 并做 Base64 URL 安全编码。
   *
   * @param value 待编码的 header 或 payload
   * @return Base64 URL 安全编码（无填充）后的字符串
   * @throws IllegalStateException JSON 序列化失败时抛出
   */
  private String encodeJson(Map<String, Object> value) {
    try {
      return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode JWT", exception);
    }
  }

  /**
   * 使用 HMAC-SHA256（即 HS256）对内容签名。
   *
   * @param content 待签名内容（header.payload 两段拼接）
   * @return Base64 URL 安全编码后的签名字符串
   * @throws IllegalStateException 获取/初始化 Mac 或计算签名失败时抛出
   */
  private String sign(String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to sign JWT", exception);
    }
  }

  /**
   * 常量时间字符串比较：逐字节异或后统一判定结果，无论差异出现在哪个字节耗时基本一致，
   * 防止通过比较耗时逐字节爆破签名的时序攻击。
   *
   * @param left  期望值（本地重新计算的签名）
   * @param right 实际值（token 携带的签名）
   * @return 内容完全一致返回 true；长度不同直接返回 false
   */
  private boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
    if (leftBytes.length != rightBytes.length) {
      return false;
    }
    int result = 0;
    for (int index = 0; index < leftBytes.length; index++) {
      result |= leftBytes[index] ^ rightBytes[index];
    }
    return result == 0;
  }
}
