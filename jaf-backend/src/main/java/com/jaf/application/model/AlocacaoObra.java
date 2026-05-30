package com.jaf.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jaf.application.enums.CargoNaObra;
import jakarta.persistence.*;

@Entity
public class AlocacaoObra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Enumerated(EnumType.STRING)
    private CargoNaObra cargo;

    @JsonProperty("cargo")
    public CargoNaObra getCargo() {
        return cargo;
    }

    public void setCargo(CargoNaObra cargo) {
        this.cargo = cargo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
