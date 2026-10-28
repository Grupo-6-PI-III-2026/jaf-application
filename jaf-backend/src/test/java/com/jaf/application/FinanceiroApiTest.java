package com.jaf.application;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.enums.Cargo;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@Transactional
class FinanceiroApiTest {
    @MockitoBean GerenciadorTokenJwt tokenJwt;
    @Autowired MockMvc mvc;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired ObraRepository obras;
    @Autowired GastoRepository gastos;
    @Autowired AlocacaoObraRepository alocacoes;
    Obra obra;
    Gasto gasto;
    Funcionario engenheiro, responsavel, admin;

    private Funcionario pessoa(String email, Cargo cargo) {
        var pessoa = new Funcionario(); pessoa.setEmail(email); pessoa.setNome(cargo.name()); pessoa.setCargoGlobal(cargo);
        return funcionarios.saveAndFlush(pessoa);
    }
    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor usuario(Funcionario f, String... permissoes) {
        return user(f.getEmail()).authorities(java.util.Arrays.stream(permissoes).map(SimpleGrantedAuthority::new).toList());
    }
    @BeforeEach void preparar() {
        engenheiro = pessoa("engenheiro@test.invalid", Cargo.ENGENHEIRO);
        responsavel = pessoa("responsavel@test.invalid", Cargo.RESPONSAVEL_ADMINISTRATIVO);
        admin = pessoa("admin@test.invalid", Cargo.ADMIN);
        obra = new Obra(); obra.setTitulo("Obra"); obra.setOrcamento("9000"); obra = obras.saveAndFlush(obra);
        for (var pessoa : List.of(engenheiro, responsavel)) {
            var alocacao = new AlocacaoObra(); alocacao.setFuncionario(pessoa); alocacao.setObra(obra);
            alocacao.setCargo(pessoa.getCargoGlobal()); alocacoes.saveAndFlush(alocacao);
        }
        gasto = new Gasto(); gasto.setObra(obra); gasto.setFuncionario(engenheiro); gasto.setMetodoPagamento("REEMBOLSO");
        gasto.setValor(new BigDecimal("100.00")); gasto.setDtGasto(LocalDate.now()); gasto = gastos.saveAndFlush(gasto);
    }

    @Test void financeiroEDashboardNegadosMesmoComPermissaoDeVisualizarGastos() throws Exception {
        for (var pessoa : List.of(engenheiro, responsavel)) {
            mvc.perform(get("/obras/{id}/financeiro", obra.getId()).with(usuario(pessoa, "VISUALIZAR_GASTOS"))).andExpect(status().isForbidden());
            mvc.perform(put("/obras/{id}/financeiro", obra.getId()).with(usuario(pessoa)).contentType("application/json").content("{\"totalRecebido\":1000}"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/dashboard/stats").param("obraId", obra.getId().toString()).with(usuario(pessoa, "VISUALIZAR_GASTOS")))
                    .andExpect(status().isForbidden());
        }
    }
    @Test void respostasDeObraEGastoNaoVazamOrcamentoOuRecebido() throws Exception {
        mvc.perform(get("/obras/{id}", obra.getId()).with(usuario(engenheiro, "VISUALIZAR_OBRA")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orcamento").doesNotExist()).andExpect(jsonPath("$.totalRecebido").doesNotExist());
        mvc.perform(get("/gastos/{id}", gasto.getId()).with(usuario(engenheiro, "VISUALIZAR_GASTOS")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.obra.orcamento").doesNotExist()).andExpect(jsonPath("$.obra.totalRecebido").doesNotExist());
    }
    @Test void cargosEAlocacaoControlamRegistroEPermissaoControlaLeitura() throws Exception {
        String corpo = "{\"valor\":30,\"dataPagamento\":\"" + LocalDate.now() + "\",\"observacao\":\"Parcela\"}";
        mvc.perform(post("/gastos/{id}/reembolsos", gasto.getId()).with(usuario(engenheiro, "EDITAR_GASTO")).contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
        alocacoes.deleteAll(); alocacoes.flush();
        mvc.perform(post("/gastos/{id}/reembolsos", gasto.getId()).with(usuario(responsavel)).contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/gastos/{id}/reembolsos", gasto.getId()).with(usuario(admin)).contentType("application/json").content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PARCIAL")).andExpect(jsonPath("$.totalPago").value(30))
                .andExpect(jsonPath("$.saldoPendente").value(70)).andExpect(jsonPath("$.pagamentos.length()").value(1));
        mvc.perform(get("/gastos/{id}/reembolsos", gasto.getId()).with(usuario(engenheiro)))
                .andExpect(status().isForbidden());
    }
    @Test void datasDecimaisEStatusInvalidosRetornam400() throws Exception {
        mvc.perform(get("/presencas/obra/{id}/data/2026-02-30", obra.getId()).with(usuario(admin, "VISUALIZAR_PRESENCAS")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/gastos").param("statusReembolso", "INVALIDO").with(usuario(admin, "VISUALIZAR_GASTOS")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/gastos/{id}/reembolsos", gasto.getId()).with(usuario(admin)).contentType("application/json")
                .content("{\"valor\":0.001,\"dataPagamento\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void presencaManualExigePermissaoELeituraExigeEscopo() throws Exception {
        String corpo = "{\"funcionarioId\":" + engenheiro.getId() + ",\"obraId\":" + obra.getId() + ",\"data\":\"" + LocalDate.now() + "\",\"presente\":true}";
        mvc.perform(post("/presencas").with(usuario(engenheiro, "VISUALIZAR_PRESENCAS")).contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/presencas").with(usuario(engenheiro, "REGISTRAR_PRESENCA")).contentType("application/json").content(corpo))
                .andExpect(status().isCreated());
        var fora = obras.saveAndFlush(new Obra());
        mvc.perform(get("/presencas/obra/{id}/data/{data}", fora.getId(), LocalDate.now()).with(usuario(engenheiro, "VISUALIZAR_PRESENCAS")))
                .andExpect(status().isForbidden());
    }
}