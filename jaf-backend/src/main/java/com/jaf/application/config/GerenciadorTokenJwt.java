package com.jaf.application.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GerenciadorTokenJwt {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.validity}")
    private long jwtTokenValidity;

    public String generateToken(final Authentication authentication) {
        // Coleta todas as authorities (roles/perfis) do usuário separadas por vírgula
        final String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        return Jwts.builder()
            .setSubject(authentication.getName())           // claim "sub": quem é o usuário
            .claim("authorities", authorities)              // claim customizado: perfis do usuário
            .setIssuedAt(new Date(System.currentTimeMillis()))                             // claim "iat"
            .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity * 1_000)) // claim "exp"
            .signWith(parseSecret())                        // assina com HMAC-SHA256
            .compact();                                     // serializa para String
    }

    public String getUsernameFromToken(String token) {
        return getClaimForToken(token, Claims::getSubject);
    }


    public Date getExpirationDateFromToken(String token) {
        return getClaimForToken(token, Claims::getExpiration);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public <T> T getClaimForToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date(System.currentTimeMillis()));
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(parseSecret())    // define a chave para verificar a assinatura
            .build()
            .parseClaimsJws(token)           // parseia e valida assinatura + expiração
            .getBody();                      // retorna o payload (Claims)
    }

    private SecretKey parseSecret() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secret));
    }
}