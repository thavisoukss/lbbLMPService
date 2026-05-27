package com.lbb.lmps.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Service
public class JwtService {

    private final PublicKey publicKey;

    public JwtService() throws Exception {
        this.publicKey = KeyUtils.loadPublicKey();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String validate(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expired");
        } catch (JwtException e) {
            throw new RuntimeException("Invalid token");
        }
    }
}
