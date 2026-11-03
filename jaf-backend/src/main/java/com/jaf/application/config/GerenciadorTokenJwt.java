package com.jaf.application.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.jaf.application.dto.FuncionarioDetalhesDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class GerenciadorTokenJwt {

    private final JwtProperties properties;

    public GerenciadorTokenJwt(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(final Authentication authentication) {
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof FuncionarioDetalhesDto funcionario) || funcionario.getId() == null) {
            throw new IllegalStateException("Principal autenticado sem identificador de funcionário");
        }

        return Jwts.builder()
            .setSubject(String.valueOf(funcionario.getId()))
            .claim("tv", funcionario.getTokenVersion())
            .setIssuedAt(new Date(System.currentTimeMillis()))                             // claim "iat"
            .setExpiration(new Date(System.currentTimeMillis() + properties.expiration().toMillis()))
            .signWith(parseSecret())
            .compact();
    }

    public String getSubjectFromToken(String token) {
        return getClaimForToken(token, Claims::getSubject);
    }

    public boolean tokenVigente(String token, FuncionarioDetalhesDto userDetails) {
        Integer tokenVersion = getClaimForToken(token, claims -> claims.get("tv", Integer.class));
        return userDetails.isEnabled() && tokenVersion != null
                && tokenVersion.equals(userDetails.getTokenVersion());
    }

    public <T> T getClaimForToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(parseSecret())    // define a chave para verificar a assinatura
            .build()
            .parseClaimsJws(token)           // parseia e valida assinatura + expiração
            .getBody();                      // retorna o payload (Claims)
    }

    private SecretKey parseSecret() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }
}