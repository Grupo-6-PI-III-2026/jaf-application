package com.jaf.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class FuncionarioPerfilUpdateDto {
    @NotBlank(message = "Nome nao pode ser vazio!")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres!")
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String nome;

    @NotBlank(message = "E-mail nao deve ser vazio!")
    @Email(message = "E-mail invalido!")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Size(max = 512, message = "Foto URL deve ter no maximo 512 caracteres")
    @Pattern(regexp = "^$|^/uploads/fotos/[0-9a-fA-F-]{36}\\.(jpg|png|gif|webp)$|^/assets/Geral/avatar-[a-z]+\\.svg$",
            message = "URL de foto inválida")
    private String fotoUrl;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
