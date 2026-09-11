package com.hackathonteam7.mainprojectbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** secret 은 HS256 서명에 쓰이므로 최소 32바이트여야 한다. */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
