package com.jaf.application.model;

import jakarta.persistence.*;

import javax.xml.crypto.Data;

@Entity
public class Relatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private Data dtEmissao;

    @ManyToOne
    @JoinColumn(name = "id")
    private Funcionario funcionarioResponsavel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Data getDtEmissao() {
        return dtEmissao;
    }

    public void setDtEmissao(Data dtEmissao) {
        this.dtEmissao = dtEmissao;
    }

    public Funcionario getFuncionarioResponsavel() {
        return funcionarioResponsavel;
    }

    public void setFuncionarioResponsavel(Funcionario funcionarioResponsavel) {
        this.funcionarioResponsavel = funcionarioResponsavel;
    }
}
