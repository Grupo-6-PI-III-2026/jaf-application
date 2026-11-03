package com.jaf.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jaf.application.enums.Cargo;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    @JsonIgnore
    private String senha;

    @Column(name = "foto_url", length = 512)
    private String fotoUrl;

    @Enumerated(EnumType.STRING)
    private Cargo cargoGlobal;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "desativado_em")
    @JsonIgnore
    private LocalDateTime desativadoEm;

    @Column(name = "token_version", nullable = false)
    @JsonIgnore
    private Integer tokenVersion = 0;

    @Column(name = "permissoes_personalizadas", nullable = false)
    private boolean permissoesPersonalizadas;

    @Version
    @Column(nullable = false)
    @JsonIgnore
    private Long versao;

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDesativadoEm() {
        return desativadoEm;
    }

    public void setDesativadoEm(LocalDateTime desativadoEm) {
        this.desativadoEm = desativadoEm;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }
    public boolean isPermissoesPersonalizadas() { return permissoesPersonalizadas; }
    public void setPermissoesPersonalizadas(boolean permissoesPersonalizadas) { this.permissoesPersonalizadas = permissoesPersonalizadas; }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Cargo getCargoGlobal() {
        return cargoGlobal;
    }

    public void setCargoGlobal(Cargo cargoGlobal) {
        this.cargoGlobal = cargoGlobal;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
}
