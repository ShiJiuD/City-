package com.cityart.utils;

  import io.jsonwebtoken.Claims;
  import io.jsonwebtoken.Jwts;
  import io.jsonwebtoken.security.Keys;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Component;

  import javax.crypto.SecretKey;
  import java.nio.charset.StandardCharsets;
  import java.util.Date;

  @Component
  public class JwtUtil {

      @Value("${cityart.jwt.secret}")
      private String secret;

      @Value("${cityart.jwt.expiration}")
      private Long expiration;

      /** 密钥缓存，避免每次调用重复生成 */
      private volatile SecretKey cachedKey;

      /** 生成密钥对 */
      private SecretKey getKey() {
          if (cachedKey == null) {
              synchronized (this) {
                  if (cachedKey == null) {
                      cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                  }
              }
          }
          return cachedKey;
      }

      /**
       * 生成token【重载1：携带角色信息，核心方法】
       * @param Id 用户id
       * @param role 身份：USER / ADMIN
       */
      public String generateToken(Long Id, String role) {
          return Jwts.builder()
                  .claim("Id", Id)
                  .claim("Role", role)
                  .issuedAt(new Date())
                  .expiration(new Date(System.currentTimeMillis() + expiration))
                  .signWith(getKey())
                  .compact();
      }

      /** 解析 token（验签） */
      public Claims parseToken(String token) {
          return Jwts.parser()
                  .verifyWith(getKey())
                  .build()
                  .parseSignedClaims(token)
                  .getPayload();
      }

      /** 验证 token 是否有效 */
      public boolean isValid(String token) {
          try {
              parseToken(token);
              return true;
          } catch (Exception e) {
              return false;
          }
      }

      /** 从 token 获取 Id */
      public Long getId(String token) {
          return parseToken(token).get("Id", Long.class);
      }

      /** 从token获取角色身份 USER / ADMIN */
      public String getRole(String token) {
          return parseToken(token).get("Role", String.class);
      }

      /** 获取 token 剩余有效时间（毫秒），已过期返回 0 */
      public long getRemainingTtl(String token) {
          try {
              Claims claims = parseToken(token);
              long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
              return Math.max(remaining, 0);
          } catch (Exception e) {
              return 0;
          }
      }
  }