package com.jaf.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.FuncaoObra;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratoRespostasTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void obraPublicaNaoExponeCamposFinanceirosEAdminMantemContrato() throws Exception {
        Obra obra = obra();

        var publica = mapper.readTree(mapper.writeValueAsString(ObraResponse.de(obra, false)));
        assertEquals(Set.of("id", "titulo", "status", "dtInicio", "dtTerminoPrevisto", "versao"), campos(publica));
        assertFalse(publica.has("orcamento"));
        assertFalse(publica.has("totalRecebido"));

        var administrativa = mapper.readTree(mapper.writeValueAsString(ObraResponse.de(obra, true)));
        assertTrue(administrativa.has("orcamento"));
        assertEquals("1000.00", administrativa.get("orcamento").asText());
        assertTrue(administrativa.has("totalRecebido"));
    }

    @Test
    void gastoEAlocacaoUsamResumosSemSenha() throws Exception {
        Funcionario funcionario = funcionario();
        Obra obra = obra();
        Gasto gasto = new Gasto();
        gasto.setId(3L);
        gasto.setFuncionario(funcionario);
        gasto.setObra(obra);
        gasto.setMetodoPagamento("PIX");
        gasto.setValor(BigDecimal.TEN);
        gasto.setDtGasto(LocalDate.of(2026, 1, 1));

        var gastoJson = mapper.readTree(mapper.writeValueAsString(GastoResponse.de(gasto, false)));
        assertTrue(gastoJson.has("funcionario"));
        assertTrue(gastoJson.has("obra"));
        assertFalse(gastoJson.get("funcionario").has("senha"));
        assertEquals(Set.of("id", "titulo"), campos(gastoJson.get("obra")));

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setId(4L);
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(FuncaoObra.GESTOR_OBRA);
        var alocacaoJson = mapper.readTree(mapper.writeValueAsString(AlocacaoResponse.de(alocacao, false)));
        assertEquals("GESTOR_OBRA", alocacaoJson.get("cargo").asText());
        assertFalse(alocacaoJson.get("funcionario").has("senha"));
    }

    private Obra obra() {
        Obra obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra teste");
        obra.setStatus("PLANEJADA");
        obra.setDtInicio(LocalDate.of(2026, 1, 1));
        obra.setDtTerminoPrevisto(LocalDate.of(2026, 12, 31));
        obra.setOrcamento("1000.00");
        obra.setTotalRecebido(BigDecimal.ZERO);
        return obra;
    }

    private Funcionario funcionario() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(2L);
        funcionario.setNome("Pessoa Teste");
        funcionario.setEmail("pessoa@teste.invalid");
        funcionario.setSenha("nao deve aparecer");
        funcionario.setCargoGlobal(Cargo.ENGENHEIRO);
        return funcionario;
    }

    private Set<String> campos(com.fasterxml.jackson.databind.JsonNode node) {
        Set<String> campos = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(campos::add);
        return campos;
    }
}
