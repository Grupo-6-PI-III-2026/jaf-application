package com.jaf.application.armazenamento;

import java.time.Duration;

public interface ArmazenamentoArquivos {

    void salvar(String chave, byte[] conteudo, String contentType);

    /** URL temporária de leitura; o arquivo nunca fica público de forma permanente. */
    String gerarUrl(String chave, Duration validade);

    void remover(String chave);
}
