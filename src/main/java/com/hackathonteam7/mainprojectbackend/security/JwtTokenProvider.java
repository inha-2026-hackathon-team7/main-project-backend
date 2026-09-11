package com.hackathonteam7.mainprojectbackend.security;

import com.hackathonteam7.mainprojectbackend.config.JwtProperties;
import com.hackathonteam7.mainprojectbackend.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ORG_ID = "orgId";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, String email, Role role, Long orgId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_ORG_ID, orgId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.expirationSeconds())))
                .signWith(secretKey())
                .compact();
    }

    /** 서명·만료 검증에 실패하면 io.jsonwebtoken.JwtException 계열 예외를 던진다. */
    public PrincipalUser parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        Long orgId = claims.get(CLAIM_ORG_ID, Long.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new PrincipalUser(userId, orgId, role);
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
