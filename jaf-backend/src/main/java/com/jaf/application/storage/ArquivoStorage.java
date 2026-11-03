package com.jaf.application.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ArquivoStorage {
    void salvar(String chave, InputStream conteudo) throws IOException;
    InputStream abrir(String chave) throws IOException;
    void remover(String chave) throws IOException;
}
