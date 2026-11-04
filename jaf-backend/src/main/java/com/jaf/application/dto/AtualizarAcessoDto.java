package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AtualizarAcessoDto {
    @NotNull(message = "O cargo é obrigatório")
    private Cargo cargo;
    private List<String> permissoes;
    private Long versao;

    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public List<String> getPermissoes() { return permissoes; }
    public void setPermissoes(List<String> permissoes) { this.permissoes = permissoes; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
}