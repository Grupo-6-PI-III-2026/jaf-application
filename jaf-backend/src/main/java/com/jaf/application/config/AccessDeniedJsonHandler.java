package com.jaf.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class AccessDeniedJsonHandler implements AccessDeniedHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessDeniedJsonHandler.class);
    public static final String CSRF_HEADER = "X-CSRF-Retry";
    private final ObjectMapper objectMapper;

    public AccessDeniedJsonHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (exception instanceof CsrfException) {
            // Cookie XSRF-TOKEN ausente/divergente (ex.: sobrescrito por outro app em localhost):
            // emite um novo para o cliente repetir a requisição uma vez.
            LOGGER.warn("[CSRF] {} em {} {}", exception.getClass().getSimpleName(), request.getMethod(), request.getRequestURI());
            CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
            repositorio.saveToken(repositorio.generateToken(request), request, response);
            response.setHeader(CSRF_HEADER, "true");
        }
        objectMapper.writeValue(response.getOutputStream(), new GlobalExceptionHandler.ErrorResponse(
                LocalDateTime.now(), 403, "Forbidden",
                "Você não tem permissão para acessar este recurso", request.getRequestURI()));
    }
}
