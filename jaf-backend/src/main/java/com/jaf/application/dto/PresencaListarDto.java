package com.jaf.application.dto;

import java.time.LocalDate;

public class PresencaListarDto {
    private Long id;
    private Long funcionarioId;
    private String funcionarioNome;
    private String funcionarioCargo;
    private LocalDate data;
    private Boolean presente;
    private Boolean desabilitado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public String getFuncionarioNome() {
        return funcionarioNome;
    }

    public void setFuncionarioNome(String funcionarioNome) {
        this.funcionarioNome = funcionarioNome;
    }

    public String getFuncionarioCargo() {
        return funcionarioCargo;
    }

    public void setFuncionarioCargo(String funcionarioCargo) {
        this.funcionarioCargo = funcionarioCargo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(Boolean presente) {
        this.presente = presente;
    }

    public Boolean getDesabilitado() {
        return desabilitado;
    }

    public void setDesabilitado(Boolean desabilitado) {
        this.desabilitado = desabilitado;
    }
}
