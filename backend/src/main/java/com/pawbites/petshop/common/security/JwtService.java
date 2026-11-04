package com.pawbites.petshop.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawbites.petshop.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long expireSeconds;

  public JwtService(
      ObjectMapper objectMapper,
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expire-hours}") long expireHours
  ) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.expireSeconds = expireHours * 3600;
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", String.valueOf(user.getId()));
    payload.put("userId", user.getId());
    payload.put("username", user.getUsername());
    payload.put("nickname", user.getNickname());
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusSeconds(expireSeconds).getEpochSecond());

    String headerPart = encodeJson(header);
    String payloadPart = encodeJson(payload);
    String content = headerPart + "." + payloadPart;
    return content + "." + sign(content);
  }

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
      String username = String.valueOf(payload.get("username"));
      String nickname = String.valueOf(payload.get("nickname"));
      return new AuthPrincipal(userId.longValue(), username, nickname);
    } catch (Exception exception) {
      return null;
    }
  }

  private String encodeJson(Map<String, Object> value) {
    try {
      return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode JWT", exception);
    }
  }

  private String sign(String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to sign JWT", exception);
    }
  }

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
