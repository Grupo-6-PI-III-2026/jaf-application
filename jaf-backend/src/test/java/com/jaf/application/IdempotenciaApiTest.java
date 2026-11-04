package com.jaf.application;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDetalhesDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.Permissao;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:idempotencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@Transactional
class IdempotenciaApiTest {
    @MockitoBean GerenciadorTokenJwt tokenJwt;
    @Autowired MockMvc mvc;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired ObraRepository obras;
    @Autowired GastoRepository gastos;
    @Autowired AlocacaoObraRepository alocacoes;
    Funcionario engenheiro;
    Obra obra;

    @BeforeEach void preparar() {
        engenheiro = new Funcionario();
        engenheiro.setNome("Engenheiro");
        engenheiro.setEmail(UUID.randomUUID() + "@test.invalid");
        engenheiro.setCargoGlobal(Cargo.ENGENHEIRO);
        engenheiro = funcionarios.saveAndFlush(engenheiro);
        obra = new Obra();
        obra.setTitulo("Obra");
        obra.setStatus("EM_ANDAMENTO");
        obra.setOrcamento("9000");
        obra = obras.saveAndFlush(obra);
        var alocacao = new AlocacaoObra();
        alocacao.setFuncionario(engenheiro);
        alocacao.setObra(obra);
        alocacao.setCargo(com.jaf.application.enums.FuncaoObra.ENGENHEIRO);
        alocacoes.saveAndFlush(alocacao);
    }

    private RequestPostProcessor usuario() {
        var detalhes = new FuncionarioDetalhesDto(engenheiro, List.of(Permissao.CRIAR_GASTO));
        return authentication(new UsernamePasswordAuthenticationToken(detalhes, "", detalhes.getAuthorities()));
    }

    private String gasto(String valor) {
        return """
                {"descricao":"Cimento","categoria":"MATERIAL","metodoPagamento":"PIX","etapa":"ETAPA 1",
                 "valor":%s,"dtGasto":"%s","funcionarioId":%d,"obraId":%d}
                """.formatted(valor, LocalDate.now(), engenheiro.getId(), obra.getId());
    }

    @Test void mesmaChaveCriaUmUnicoGastoEDevolveAMesmaResposta() throws Exception {
        String chave = UUID.randomUUID().toString();

        var primeira = mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                        .contentType("application/json").content(gasto("100.00")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var repetida = mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                        .contentType("application/json").content(gasto("100.00")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertEquals(primeira, repetida);
        assertEquals(1, gastos.count());
    }

    @Test void chaveReutilizadaComOutroConteudoEhRecusada() throws Exception {
        String chave = UUID.randomUUID().toString();
        mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                .contentType("application/json").content(gasto("100.00"))).andExpect(status().isCreated());

        mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                        .contentType("application/json").content(gasto("999.00")))
                .andExpect(status().is(422)).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertEquals(1, gastos.count());
    }

    @Test void semCabecalhoOComportamentoContinuaIgual() throws Exception {
        mvc.perform(post("/gastos").with(usuario()).with(csrf()).contentType("application/json").content(gasto("10.00")))
                .andExpect(status().isCreated());
        mvc.perform(post("/gastos").with(usuario()).with(csrf()).contentType("application/json").content(gasto("10.00")))
                .andExpect(status().isCreated());
        assertEquals(2, gastos.count());
    }

    @Test void chaveInvalidaRetornaErroClaro() throws Exception {
        mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", "nao-e-uuid")
                        .contentType("application/json").content(gasto("10.00")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_INVALID"));
    }

    @Test void falhaDeValidacaoLiberaAChaveParaNovaTentativa() throws Exception {
        String chave = UUID.randomUUID().toString();
        mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                .contentType("application/json").content(gasto("-5"))).andExpect(status().isBadRequest());

        mvc.perform(post("/gastos").with(usuario()).with(csrf()).header("Idempotency-Key", chave)
                        .contentType("application/json").content(gasto("-5")))
                .andExpect(status().isBadRequest());
        assertEquals(0, gastos.count());
    }
}
