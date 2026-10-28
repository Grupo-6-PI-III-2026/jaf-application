package com.jaf.application;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.Permissao;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioPermissaoRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.service.AutenticacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
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
class FuncionarioAutorizacaoApiTest {
    @MockitoBean GerenciadorTokenJwt tokenJwt;
    @Autowired MockMvc mvc;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired FuncionarioPermissaoRepository permissoes;
    @Autowired AutenticacaoService autenticacao;

    Funcionario responsavel, engenheiro, admin;

    @BeforeEach
    void preparar() {
        responsavel = pessoa("responsavel@seguranca.invalid", Cargo.RESPONSAVEL_ADMINISTRATIVO);
        engenheiro = pessoa("engenheiro@seguranca.invalid", Cargo.ENGENHEIRO);
        admin = pessoa("admin@seguranca.invalid", Cargo.ADMIN);
    }

    private Funcionario pessoa(String email, Cargo cargo) {
        Funcionario funcionario = new Funcionario();
        funcionario.setEmail(email);
        funcionario.setNome(cargo.name());
        funcionario.setSenha("hash-nao-deve-ser-exposto");
        funcionario.setCargoGlobal(cargo);
        return funcionarios.saveAndFlush(funcionario);
    }

    // Simula inclusive authorities privilegiadas/obsoletas: o banco continua soberano.
    private UserRequestPostProcessor privilegiado(Funcionario funcionario) {
        return user(funcionario.getEmail()).authorities(Stream.concat(
                Arrays.stream(Permissao.values()).map(p -> new SimpleGrantedAuthority(p.name())),
                Stream.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).toList());
    }

    private String corpo(Cargo cargo, String email) {
        return "{\"nome\":\"Pessoa Teste\",\"email\":\"" + email
                + "\",\"senha\":\"Teste123!\",\"cargo\":\"" + cargo.name() + "\"}";
    }

    @Test
    void naoAdminNaoCriaAdminMesmoComTodasAuthorities() throws Exception {
        long antes = funcionarios.count();
        for (Funcionario ator : List.of(responsavel, engenheiro)) {
            mvc.perform(post("/funcionarios").with(privilegiado(ator)).contentType("application/json")
                            .content(corpo(Cargo.ADMIN, "novo@seguranca.invalid")))
                    .andExpect(status().isForbidden());
        }
        assertEquals(antes, funcionarios.count());
    }

    @Test
    void putNaoPermitePromoverNemGerenciarCargoDeNaoAdmin() throws Exception {
        for (Cargo cargo : List.of(Cargo.ADMIN, Cargo.RESPONSAVEL_ADMINISTRATIVO)) {
            mvc.perform(put("/funcionarios/{id}", engenheiro.getId()).with(privilegiado(responsavel))
                            .contentType("application/json").content(corpo(cargo, "alterado@seguranca.invalid")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/funcionarios/{id}", responsavel.getId()).with(privilegiado(responsavel))
                        .contentType("application/json").content(corpo(Cargo.ADMIN, responsavel.getEmail())))
                .andExpect(status().isForbidden());
        assertEquals(Cargo.ENGENHEIRO, funcionarios.findById(engenheiro.getId()).orElseThrow().getCargoGlobal());
        assertEquals("engenheiro@seguranca.invalid", engenheiro.getEmail());
        assertEquals(Cargo.RESPONSAVEL_ADMINISTRATIVO, responsavel.getCargoGlobal());
    }

    @Test
    void patchCargoSempreExigeAdminPersistido() throws Exception {
        for (Cargo cargo : List.of(Cargo.ADMIN, Cargo.GESTOR_OBRA)) {
            mvc.perform(patch("/funcionarios/{id}/cargo", engenheiro.getId()).with(privilegiado(responsavel))
                            .contentType("application/json").content("{\"cargo\":\"" + cargo.name() + "\"}"))
                    .andExpect(status().isForbidden());
        }
        assertEquals(Cargo.ENGENHEIRO, engenheiro.getCargoGlobal());
    }

    @Test
    void naoAdminNaoEditaRebaixaOuExcluiOutroAdmin() throws Exception {
        for (Cargo cargo : List.of(Cargo.ADMIN, Cargo.ENGENHEIRO)) {
            mvc.perform(put("/funcionarios/{id}", admin.getId()).with(privilegiado(responsavel))
                            .contentType("application/json").content(corpo(cargo, "tomada@seguranca.invalid")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(delete("/funcionarios/{id}", admin.getId()).with(privilegiado(responsavel)))
                .andExpect(status().isForbidden());
        assertEquals(Cargo.ADMIN, admin.getCargoGlobal());
        assertEquals("admin@seguranca.invalid", admin.getEmail());
        assertTrue(funcionarios.existsById(admin.getId()));
    }

    @Test
    void gestaoDePermissoesNaoAceitaEditarFuncionarioComoAdmin() throws Exception {
        mvc.perform(get("/permissoes/cargos").with(privilegiado(responsavel))).andExpect(status().isForbidden());
        mvc.perform(put("/permissoes/cargos/ENGENHEIRO").with(privilegiado(responsavel))
                        .contentType("application/json").content("{\"permissoes\":[\"EDITAR_FUNCIONARIO\"]}"))
                .andExpect(status().isForbidden());
        for (Funcionario alvo : List.of(responsavel, engenheiro, admin)) {
            mvc.perform(get("/funcionarios/{id}/permissoes", alvo.getId()).with(privilegiado(responsavel)))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/funcionarios/{id}/permissoes", alvo.getId()).with(privilegiado(responsavel))
                            .contentType("application/json").content("{\"permissoes\":[\"EDITAR_FUNCIONARIO\"]}"))
                    .andExpect(status().isForbidden());
            assertTrue(permissoes.findByFuncionarioId(alvo.getId()).isEmpty());
        }
    }

    @Test
    void responsavelContinuaCadastrandoEAtualizandoDadosNormais() throws Exception {
        mvc.perform(post("/funcionarios").with(privilegiado(responsavel)).contentType("application/json")
                        .content(corpo(Cargo.PEDREIRO, "novo@seguranca.invalid")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cargo").value("PEDREIRO"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(put("/funcionarios/{id}", engenheiro.getId()).with(privilegiado(responsavel))
                        .contentType("application/json").content(corpo(Cargo.ENGENHEIRO, "editado@seguranca.invalid")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("editado@seguranca.invalid"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        assertEquals("hash-nao-deve-ser-exposto", engenheiro.getSenha());
    }

    @Test
    void adminRealPodeConcederRemoverAdminEEditarOutroAdmin() throws Exception {
        mvc.perform(post("/funcionarios").with(privilegiado(admin)).contentType("application/json")
                        .content(corpo(Cargo.ADMIN, "outroadmin@seguranca.invalid")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(patch("/funcionarios/{id}/cargo", engenheiro.getId()).with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"cargo\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("ADMIN"));
        mvc.perform(put("/funcionarios/{id}", engenheiro.getId()).with(privilegiado(admin))
                        .contentType("application/json").content(corpo(Cargo.ENGENHEIRO, "editado@seguranca.invalid")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("ENGENHEIRO"));
        Funcionario outroAdmin = funcionarios.findByEmailIgnoreCase("outroadmin@seguranca.invalid").orElseThrow();
        mvc.perform(delete("/funcionarios/{id}", outroAdmin.getId()).with(privilegiado(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminNaoAlteraProprioCargoPermissoesNemExcluiPropriaContaPelaGestao() throws Exception {
        String mensagem = "Não é permitido alterar o próprio cargo ou as próprias permissões.";
        mvc.perform(patch("/funcionarios/{id}/cargo", admin.getId()).with(privilegiado(admin))
                        .contentType("application/json").content("{\"cargo\":\"ENGENHEIRO\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(mensagem));
        mvc.perform(put("/funcionarios/{id}", admin.getId()).with(privilegiado(admin))
                        .contentType("application/json").content(corpo(Cargo.ENGENHEIRO, admin.getEmail())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(mensagem));
        mvc.perform(put("/funcionarios/{id}/permissoes", admin.getId()).with(privilegiado(admin))
                        .contentType("application/json").content("{\"permissoes\":[\"VISUALIZAR_OBRA\"]}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(mensagem));
        mvc.perform(delete("/funcionarios/{id}", admin.getId()).with(privilegiado(admin)))
                .andExpect(status().isForbidden());
        assertEquals(Cargo.ADMIN, funcionarios.findById(admin.getId()).orElseThrow().getCargoGlobal());
        assertTrue(permissoes.findByFuncionarioId(admin.getId()).isEmpty());
        assertTrue(funcionarios.existsById(admin.getId()));

        mvc.perform(put("/funcionarios/{id}", admin.getId()).with(privilegiado(admin))
                        .contentType("application/json").content(corpo(Cargo.ADMIN, admin.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("ADMIN"));
        mvc.perform(patch("/funcionarios/{id}/cargo", engenheiro.getId()).with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"cargo\":\"RESPONSAVEL_ADMINISTRATIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("RESPONSAVEL_ADMINISTRATIVO"));
        mvc.perform(put("/funcionarios/{id}/permissoes", engenheiro.getId()).with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"permissoes\":[\"VISUALIZAR_OBRA\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void regrasDoCargoAdminSaoFixas() throws Exception {
        mvc.perform(put("/permissoes/cargos/ADMIN").with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"permissoes\":[\"VISUALIZAR_OBRA\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("As permissões do cargo ADMIN são fixas."));
    }

    @Test
    void consultaPropriaSemAuthoritiesUsaMesmoResolverDaAutenticacao() throws Exception {
        for (Funcionario pessoa : List.of(engenheiro, responsavel, admin)) {
            Object[] esperadas = autenticacao.loadUserByUsername(pessoa.getEmail()).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).filter(nome -> !nome.startsWith("ROLE_")).toArray();
            mvc.perform(get("/funcionarios/me/permissoes").with(user(pessoa.getEmail())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.funcionarioId").value(pessoa.getId()))
                    .andExpect(jsonPath("$.cargo").value(pessoa.getCargoGlobal().name()))
                    .andExpect(jsonPath("$.permissoes", containsInAnyOrder(esperadas)))
                    .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist());
        }
    }

    @Test
    void consultaPropriaRefleteRegrasEOverrideIndividualPersistidos() throws Exception {
        mvc.perform(put("/permissoes/cargos/ENGENHEIRO").with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"permissoes\":[\"VISUALIZAR_OBRA\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/funcionarios/me/permissoes").with(user(engenheiro.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissoes", containsInAnyOrder("VISUALIZAR_OBRA")));
        mvc.perform(put("/funcionarios/{id}/permissoes", engenheiro.getId()).with(user(admin.getEmail()))
                        .contentType("application/json").content("{\"permissoes\":[\"REGISTRAR_PRESENCA\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/funcionarios/me/permissoes").with(user(engenheiro.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissoes", containsInAnyOrder("REGISTRAR_PRESENCA")));
        assertEquals(List.of("REGISTRAR_PRESENCA"), autenticacao.loadUserByUsername(engenheiro.getEmail())
                .getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(nome -> !nome.startsWith("ROLE_")).toList());
        mvc.perform(get("/funcionarios/{id}/permissoes", engenheiro.getId()).with(user(admin.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissoes", containsInAnyOrder("REGISTRAR_PRESENCA")));
    }

    @Test
    void consultaPropriaNaoAceitaAnonimoOuContaInexistente() throws Exception {
        mvc.perform(get("/funcionarios/me/permissoes")).andExpect(status().isForbidden());
        mvc.perform(get("/funcionarios/me/permissoes").with(user("removido@seguranca.invalid").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rebaixamentoNoBancoRevogaGestaoMesmoComAuthoritiesAntigas() throws Exception {
        admin.setCargoGlobal(Cargo.ENGENHEIRO);
        funcionarios.saveAndFlush(admin);
        mvc.perform(put("/permissoes/cargos/ENGENHEIRO").with(privilegiado(admin))
                        .contentType("application/json").content("{\"permissoes\":[\"EDITAR_FUNCIONARIO\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/funcionarios").with(privilegiado(admin)).contentType("application/json")
                        .content(corpo(Cargo.ADMIN, "novo@seguranca.invalid")))
                .andExpect(status().isForbidden());
    }
}