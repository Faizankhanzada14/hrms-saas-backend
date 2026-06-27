package com.hrms.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "hrms.jwt")
public class JwtProperties {

    /** Base64-encoded secret key, must be >= 256 bits for HS256. */
    private String secret;

    /** Access token validity in milliseconds. */
    private long accessTokenExpirationMs = 900_000; // 15 minutes

    /** Refresh token validity in milliseconds. */
    private long refreshTokenExpirationMs = 604_800_000; // 7 days

    private String issuer = "hrms-backend";

    /** Maximum failed login attempts before account lock. */
    private int maxFailedAttempts = 5;
}
