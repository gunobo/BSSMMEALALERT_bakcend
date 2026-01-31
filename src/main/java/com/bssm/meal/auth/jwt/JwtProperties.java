package com.bssm.meal.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessExpireMillis;
    private long refreshExpireMillis;

    // Getter / Setter
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessExpireMillis() { return accessExpireMillis; }
    public void setAccessExpireMillis(long accessExpireMillis) { this.accessExpireMillis = accessExpireMillis; }

    public long getRefreshExpireMillis() { return refreshExpireMillis; }
    public void setRefreshExpireMillis(long refreshExpireMillis) { this.refreshExpireMillis = refreshExpireMillis; }
}
