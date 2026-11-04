package com.jaf.application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;

/** Teto das listas sem paginação: acima dele a resposta é cortada e marcada com X-Truncated. */
final class ListaLimitada {
    static final int LIMITE = 1000;
    static final String HEADER = "X-Truncated";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListaLimitada.class);

    private ListaLimitada() {
    }

    static <T> ResponseEntity<List<T>> responder(List<T> lista, String origem) {
        if (lista.size() <= LIMITE) return ResponseEntity.ok(lista);
        LOGGER.warn("Lista de {} cortada em {} itens (total {}); use os endpoints paginados.", origem, LIMITE, lista.size());
        return ResponseEntity.ok().header(HEADER, "true").body(lista.subList(0, LIMITE));
    }
}