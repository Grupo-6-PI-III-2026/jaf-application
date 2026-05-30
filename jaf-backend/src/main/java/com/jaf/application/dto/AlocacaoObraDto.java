//DTO = TRANSPORTAR DADOS
package com.jaf.application.dto;
import com.jaf.application.enums.CargoNaObra;
import jakarta.validation.constraints.NotNull;

public class AlocacaoObraDto {
    @NotNull(message = "O ID do funcionário é obrigatório")
    private Long funcionarioId;

    @NotNull(message = "O ID da obra é obrigatório")
    private Long obraId;

    @NotNull(message = "O cargo na obra é obrigatório")
    private CargoNaObra cargoNaObra;

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

    public CargoNaObra getCargoNaObra() {
        return cargoNaObra;
    }

    public void setCargoNaObra(CargoNaObra cargoNaObra) {
        this.cargoNaObra = cargoNaObra;
    }
}
