package com.jaf.application.model;

import jakarta.persistence.*;

import java.time.LocalDate;


@Entity
public class Relatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private LocalDate dtEmissao;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
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

    public LocalDate getDtEmissao() {
        return dtEmissao;
    }

    public void setDtEmissao(LocalDate dtEmissao) {
        this.dtEmissao = dtEmissao;
    }

    public Funcionario getFuncionarioResponsavel() {
        return funcionarioResponsavel;
    }

    public void setFuncionarioResponsavel(Funcionario funcionarioResponsavel) {
        this.funcionarioResponsavel = funcionarioResponsavel;
    }
}
