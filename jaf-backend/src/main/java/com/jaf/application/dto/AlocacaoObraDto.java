package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import jakarta.validation.constraints.NotNull;

public class AlocacaoObraDto {
    @NotNull(message = "O ID do funcionário é obrigatório")
    private Long funcionarioId;

    @NotNull(message = "O ID da obra é obrigatório")
    private Long obraId;

    @NotNull(message = "O cargo na obra é obrigatório")
    private Cargo cargoNaObra;

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public Long getObraId() {
        return obraId;
    }

    public void setObraId(Long obraId) {
        this.obraId = obraId;
    }

    public Cargo getCargoNaObra() {
        return cargoNaObra;
    }

    public void setCargoNaObra(Cargo cargoNaObra) {
        this.cargoNaObra = cargoNaObra;
    }
}
