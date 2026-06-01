package com.jaf.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AlterarSenhaDto {
    @NotBlank(message = "Senha atual obrigatoria")
    private String senhaAtual;

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{6,}$",
            message = "Senha deve ter pelo menos 1 letra maiuscula, 1 minuscula e 1 numero"
    )
        @NotBlank(message = "Nova senha obrigatoria")
        @Size(min = 6, max = 100, message = "Nova senha deve ter entre 6 e 100 caracteres")
    private String novaSenha;

        @NotBlank(message = "Confirmacao de senha obrigatoria")
    private String confirmacaoSenha;

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
