//DTO = TRANSPORTAR DADOS
package com.jaf.application.dto;
import com.jaf.application.enums.FuncaoObra;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AlocacaoObraDto {
    @NotNull(message = "O ID do funcionário é obrigatório")
    private Long funcionarioId;

    @NotNull(message = "O ID da obra é obrigatório")
    private Long obraId;

    @NotNull(message = "O cargo na obra é obrigatório")
    private FuncaoObra cargoNaObra;
    private LocalDate dtInicio;
    private LocalDate dtFim;

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

    public FuncaoObra getCargoNaObra() {
        return cargoNaObra;
    }

    public void setCargoNaObra(FuncaoObra cargoNaObra) {
        this.cargoNaObra = cargoNaObra;
    }

    public LocalDate getDtInicio() { return dtInicio; }
    public void setDtInicio(LocalDate dtInicio) { this.dtInicio = dtInicio; }
    public LocalDate getDtFim() { return dtFim; }
    public void setDtFim(LocalDate dtFim) { this.dtFim = dtFim; }
}
