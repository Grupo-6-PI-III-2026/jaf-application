package com.jaf.application.enums;

/**
 * Enum para representar cargos específicos na construção civil
 * Usado quando um funcionário é alocado a uma obra específica
 */
public enum CargoNaObra {
    ENGENHEIRO_CIVIL("Engenheiro Civil"),
    ENGENHEIRO_ELETRICISTA("Engenheiro Eletricista"),
    MESTRE_OBRAS("Mestre de Obras"),
    ENCARREGADO("Encarregado"),
    PEDREIRO("Pedreiro"),
    ELETRICISTA("Eletricista"),
    ENCANADOR("Encanador"),
    MARCENEIRO("Marceneiro"),
    PINTOR("Pintor"),
    AJUDANTE("Ajudante"),
    SERVENTE("Servente"),
    SEGURANCA_TRABALHO("Segurança do Trabalho"),
    TOPOGRAFO("Topógrafo"),
    ARQUITETO("Arquiteto"),
    SOLDADOR("Soldador"),
    OPERADOR_MAQUINAS("Operador de Máquinas"),
    ASSISTENTE_ADMINISTRATIVO("Assistente Administrativo"),
    OUTRO("Outro");

    private final String descricao;

    CargoNaObra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}