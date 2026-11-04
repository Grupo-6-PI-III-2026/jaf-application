package com.jaf.application.service;

import com.jaf.application.dto.FuncionarioDetalhesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Trilha de auditoria: um evento por linha no logger {@code AUDIT}, com usuário, ação, entidade e valores.
 * Nunca receber senha, token ou hash; o {@code requestId} vem do MDC (ver RequestIdFilter).
 */
public final class AuditLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {
    }

    public static void registrar(String acao, String entidade, Object id, String antes, String depois) {
        LOGGER.info("usuario={} acao={} entidade={} id={} antes=[{}] depois=[{}]",
                usuarioAtual(), acao, entidade, id, antes == null ? "" : antes, depois == null ? "" : depois);
    }

    private static Object usuarioAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null) return "anonimo";
        if (autenticacao.getPrincipal() instanceof FuncionarioDetalhesDto detalhes) return detalhes.getId();
        return autenticacao.getName();
    }
}
