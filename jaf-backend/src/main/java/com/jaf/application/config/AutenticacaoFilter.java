package com.jaf.application.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.jaf.application.service.AutenticacaoService;

import java.io.IOException;

public class AutenticacaoFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutenticacaoFilter.class);

    private final AutenticacaoService autenticacaoService;
    private final GerenciadorTokenJwt jwtTokenManager;

    public AutenticacaoFilter(AutenticacaoService autenticacaoService, GerenciadorTokenJwt jwtTokenManager) {
        this.autenticacaoService = autenticacaoService;
        this.jwtTokenManager = jwtTokenManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String subject = null;
        String jwtToken = extrairToken(request);

        if (jwtToken != null) {
            try {
                subject = jwtTokenManager.getSubjectFromToken(jwtToken);

            } catch (ExpiredJwtException e) {
                LOGGER.warn("[AUTENTICACAO] Token expirado para o usuário '{}': {}",
                        e.getClaims().getSubject(), e.getMessage());

            } catch (MalformedJwtException e) {
                LOGGER.warn("[AUTENTICACAO] Token com formato inválido: {}", e.getMessage());

            } catch (UnsupportedJwtException e) {
                LOGGER.warn("[AUTENTICACAO] Token com algoritmo não suportado: {}", e.getMessage());

            } catch (SecurityException e) {
                LOGGER.warn("[AUTENTICACAO] Assinatura do token inválida (possível adulteração): {}", e.getMessage());

            } catch (IllegalArgumentException e) {
                LOGGER.warn("[AUTENTICACAO] Token ausente ou vazio: {}", e.getMessage());
            }
        }

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            registrarAutenticacaoNoContexto(request, subject, jwtToken);
        }

        filterChain.doFilter(request, response);
    }

    private String extrairToken(HttpServletRequest request) {
        // CORREÇÃO DE SEGURANÇA A07: Suporte a cookie HttpOnly para JWT
        // Prioridade 1: cookie "jwt" HttpOnly (browser - proteção contra XSS)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Prioridade 2: header Authorization: Bearer (Swagger, Postman, server-to-server)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private void registrarAutenticacaoNoContexto(HttpServletRequest request, String subject, String jwtToken) {
        try {
            Long id = Long.valueOf(subject);
            UserDetails userDetails = autenticacaoService.loadUserById(id);
            if (!(userDetails instanceof com.jaf.application.dto.FuncionarioDetalhesDto detalhes)
                    || !jwtTokenManager.tokenVigente(jwtToken, detalhes)) return;
            UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (NumberFormatException | UsernameNotFoundException ex) {
            LOGGER.warn("[AUTENTICACAO] Token rejeitado: usuário inválido");
        }
    }
}
