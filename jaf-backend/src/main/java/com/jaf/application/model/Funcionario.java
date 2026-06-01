package com.jaf.application.model;

import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.TipoFuncionario;
import jakarta.persistence.*;

@Entity
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String senha;
    private String documento; // CPF/RG para controle de presença

    @Enumerated(EnumType.STRING)
    private Cargo cargoGlobal;

    @Enumerated(EnumType.STRING)
    private TipoFuncionario tipoFuncionario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Cargo getCargoGlobal() {
        return cargoGlobal;
    }

    public void setCargoGlobal(Cargo cargoGlobal) {
        this.cargoGlobal = cargoGlobal;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public TipoFuncionario getTipoFuncionario() {
        return tipoFuncionario;
    }

    public void setTipoFuncionario(TipoFuncionario tipoFuncionario) {
        this.tipoFuncionario = tipoFuncionario;
    }
}
