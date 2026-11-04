package com.jaf.application.exceptions;

public class StaleVersion extends Conflict {
    public static final String CODIGO = "STALE_VERSION";
    public static final String MENSAGEM = "O registro foi alterado por outra pessoa. Recarregue e tente novamente.";

    public StaleVersion() {
        super(MENSAGEM);
    }
}
