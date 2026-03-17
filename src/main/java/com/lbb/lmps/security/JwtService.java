package com.lbb.lmps.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Service
public class JwtService {

   // private final KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    public JwtService() throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey();
        this.publicKey  = KeyUtils.loadPublicKey();
    }

    public String generateToken(String username){

        return Jwts.builder()
                .setSubject(username)
                .setIssuer("DemoApp")
                .setAudience("mobile-app")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+3600000))
                .claim("role", "USER")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // ໃນ JwtService.java
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey) // key ທີ່ທ່ານໃຊ້ sign token
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String validate(String token){
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();

        } catch (ExpiredJwtException e){
            throw new RuntimeException("Token expired");
        } catch (JwtException e){
            throw new RuntimeException("Invalid token");
        }
    }
}