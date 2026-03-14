package com.sptech.school.jaf.dto;

public class Gasto {
    private Integer id;
    private Double valor;
    private String descricao;

    private Funcionario funcionario;
    private Obra obra;

    public Gasto() {
    }

    public Gasto(Integer id, Double valor, String descricao, Funcionario funcionario, Obra obra) {
        this.id = id;
        this.valor = valor;
        this.descricao = descricao;
        this.funcionario = funcionario;
        this.obra = obra;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }
}
