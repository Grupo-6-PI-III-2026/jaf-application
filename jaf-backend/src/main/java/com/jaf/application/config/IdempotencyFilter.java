package com.jaf.application.config;

import com.jaf.application.dto.FuncionarioDetalhesDto;
import com.jaf.application.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

public class IdempotencyFilter extends OncePerRequestFilter {
    public static final String HEADER = "Idempotency-Key";

    private static final Pattern CAMINHOS = Pattern.compile("^/(gastos|obras|alocacoes|funcionarios|presencas)$|^/gastos/\\d+/reembolsos$");

    private final IdempotencyService service;

    public IdempotencyFilter(IdempotencyService service) {
        this.service = service;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || request.getHeader(HEADER) == null
                || !CAMINHOS.matcher(caminho(request)).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof FuncionarioDetalhesDto usuario)) {
            chain.doFilter(request, response);
            return;
        }

        UUID chave;
        try {
            chave = UUID.fromString(request.getHeader(HEADER).trim());
        } catch (IllegalArgumentException e) {
            escreverErro(response, HttpServletResponse.SC_BAD_REQUEST, "Cabeçalho Idempotency-Key inválido.", "IDEMPOTENCY_KEY_INVALID");
            return;
        }

        byte[] corpo = request.getInputStream().readAllBytes();
        String caminho = caminho(request);
        String fingerprint = sha256(request.getMethod() + " " + caminho + " " + new String(corpo, StandardCharsets.UTF_8));
        var resultado = service.reservar(chave, usuario.getId(), request.getMethod(), caminho, fingerprint);

        switch (resultado.tipo()) {
            case REPETIDA -> {
                response.setStatus(resultado.status());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                if (resultado.corpo() != null) response.getWriter().write(resultado.corpo());
            }
            case EM_ANDAMENTO -> escreverErro(response, HttpServletResponse.SC_CONFLICT,
                    "A operação ainda está sendo processada. Aguarde e confira antes de tentar de novo.", "IDEMPOTENCY_IN_PROGRESS");
            case CHAVE_REUTILIZADA -> escreverErro(response, 422,
                    "A chave de idempotência já foi usada com outro conteúdo.", "IDEMPOTENCY_KEY_REUSED");
            case NOVA -> executar(request, response, chain, corpo, chave, usuario.getId());
        }
    }

    private void executar(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                          byte[] corpo, UUID chave, Long usuarioId) throws ServletException, IOException {
        var requisicao = new CorpoEmCache(request, corpo);
        var resposta = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(requisicao, resposta);
        } catch (IOException | ServletException | RuntimeException e) {
            service.liberar(chave, usuarioId);
            throw e;
        }
        int status = resposta.getStatus();
        if (status >= 200 && status < 300) {
            service.concluir(chave, usuarioId, status, new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8));
        } else {
            service.liberar(chave, usuarioId);
        }
        resposta.copyBodyToResponse();
    }

    private static String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return uri.length() > 1 && uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    private static void escreverErro(HttpServletResponse response, int status, String mensagem, String codigo) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + mensagem + "\",\"code\":\"" + codigo + "\"}");
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class CorpoEmCache extends HttpServletRequestWrapper {
        private final byte[] corpo;

        CorpoEmCache(HttpServletRequest request, byte[] corpo) {
            super(request);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            var origem = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return origem.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
                @Override public int read() { return origem.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
