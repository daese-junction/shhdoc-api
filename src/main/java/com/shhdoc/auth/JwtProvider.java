package com.shhdoc.auth;

import com.shhdoc.common.ApiException;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String INVALID_TOKEN = "유효하지 않거나 만료된 토큰입니다.";
    private static final String CLAIM_COMPANY_ID = "companyId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration accessValidity;
    private final Duration refreshValidity;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-validity-minutes}") long accessValidityMinutes,
            @Value("${jwt.refresh-validity-days}") long refreshValidityDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValidity = Duration.ofMinutes(accessValidityMinutes);
        this.refreshValidity = Duration.ofDays(refreshValidityDays);
    }

    public String issueAccessToken(User user) {
        return builder(user, accessValidity)
                // companyId를 문자열로 넣는 이유: JSON 숫자는 파서에 따라 Integer/Long/Double로 갈려서
                .claim(CLAIM_COMPANY_ID, String.valueOf(user.getCompany().getId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .compact();
    }

    /** refresh 토큰에는 권한 정보를 넣지 않는다. 재발급 때 DB에서 다시 읽는다. */
    public String issueRefreshToken(User user) {
        return builder(user, refreshValidity).compact();
    }

    public UserPrincipal parseAccessToken(String token) {
        Claims claims = parse(token);
        return new UserPrincipal(
                Long.valueOf(claims.getSubject()),
                Long.valueOf(claims.get(CLAIM_COMPANY_ID, String.class)),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public Long parseUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    private io.jsonwebtoken.JwtBuilder builder(User user, Duration validity) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validity.toMillis()))
                .signWith(key);
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }
    }
}
