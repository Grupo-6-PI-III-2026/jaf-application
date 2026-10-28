package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.CargoPermissaoRepository;
import com.jaf.application.repository.FuncionarioPermissaoRepository;
import com.jaf.application.repository.FuncionarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioAutorizacaoTest {
    @Mock FuncionarioRepository funcionarios;
    @Mock AlocacaoObraRepository alocacoes;
    @Mock CargoPermissaoRepository cargos;
    @Mock FuncionarioPermissaoRepository individuais;
    @Mock PasswordEncoder encoder;
    @Mock GerenciadorTokenJwt jwt;
    @Mock AuthenticationManager authenticationManager;

    AcessoObraService acesso;
    FuncionarioService service;
    CargoPermissaoService cargoService;
    FuncionarioPermissaoService permissaoService;
    Funcionario ator;

    @BeforeEach
    void preparar() {
        acesso = new AcessoObraService(funcionarios, alocacoes);
        service = new FuncionarioService(encoder, funcionarios, jwt, authenticationManager, acesso);
        cargoService = new CargoPermissaoService(cargos, acesso);
        permissaoService = new FuncionarioPermissaoService(individuais, funcionarios, cargoService, acesso);
        ator = new Funcionario();
        ator.setId(1L);
        ator.setEmail("responsavel@test.invalid");
        ator.setCargoGlobal(Cargo.RESPONSAVEL_ADMINISTRATIVO);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ator.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("EDITAR_FUNCIONARIO"))));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private void persistirAtor() {
        when(funcionarios.findByEmailIgnoreCase(ator.getEmail())).thenReturn(Optional.of(ator));
    }

    @Test
    void criacaoDeAdminFalhaNoServicoAntesDeSalvarOuCriptografar() {
        persistirAtor();
        FuncionarioDto dto = new FuncionarioDto();
        dto.setCargo(Cargo.ADMIN);
        assertThrows(Forbidden.class, () -> service.criar(dto));
        verify(funcionarios, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void atualizacaoViaDtoNaoPromoveNemModificaDadosAntesDaAutorizacao() {
        persistirAtor();
        when(funcionarios.findById(ator.getId())).thenReturn(Optional.of(ator));
        FuncionarioDto dto = new FuncionarioDto();
        dto.setCargo(Cargo.ADMIN);
        dto.setEmail("tomada@test.invalid");
        assertThrows(Forbidden.class, () -> service.atualizar(ator.getId(), dto));
        assertEquals(Cargo.RESPONSAVEL_ADMINISTRATIVO, ator.getCargoGlobal());
        assertEquals("responsavel@test.invalid", ator.getEmail());
        verify(funcionarios, never()).save(any());
    }

    @Test
    void adminAlvoNaoPodeSerEditadoOuExcluidoPorNaoAdmin() {
        persistirAtor();
        Funcionario alvo = new Funcionario();
        alvo.setId(2L);
        alvo.setCargoGlobal(Cargo.ADMIN);
        when(funcionarios.findById(2L)).thenReturn(Optional.of(alvo));
        FuncionarioDto dto = new FuncionarioDto();
        dto.setCargo(Cargo.ADMIN);
        assertThrows(Forbidden.class, () -> service.atualizar(2L, dto));
        assertThrows(Forbidden.class, () -> service.deletar(2L));
        verify(funcionarios, never()).save(any());
        verify(funcionarios, never()).deleteById(any());
    }

    @Test
    void gestaoDeCargoEPermissoesProtegidaTambemSemController() {
        persistirAtor();
        assertAll(
                () -> assertThrows(Forbidden.class, () -> service.atualizarCargo(2L, "ADMIN")),
                () -> assertThrows(Forbidden.class, () -> cargoService.listarCargosSistema()),
                () -> assertThrows(Forbidden.class, () -> cargoService.atualizar(Cargo.ENGENHEIRO, List.of())),
                () -> assertThrows(Forbidden.class, () -> permissaoService.buscar(2L)),
                () -> assertThrows(Forbidden.class, () -> permissaoService.atualizar(2L, List.of())));
        verifyNoInteractions(cargos, individuais);
        verify(funcionarios, never()).save(any());
    }

    @Test
    void adminNaoAlteraProprioAcessoMasGerenciaOutros() {
        ator.setCargoGlobal(Cargo.ADMIN);
        persistirAtor();
        when(funcionarios.findById(ator.getId())).thenReturn(Optional.of(ator));
        FuncionarioDto rebaixamento = new FuncionarioDto();
        rebaixamento.setCargo(Cargo.ENGENHEIRO);
        String mensagem = "Não é permitido alterar o próprio cargo ou as próprias permissões.";
        assertAll(
                () -> assertEquals(mensagem, assertThrows(Forbidden.class,
                        () -> service.atualizarCargo(ator.getId(), "ENGENHEIRO")).getMessage()),
                () -> assertEquals(mensagem, assertThrows(Forbidden.class,
                        () -> service.atualizar(ator.getId(), rebaixamento)).getMessage()),
                () -> assertEquals(mensagem, assertThrows(Forbidden.class,
                        () -> permissaoService.atualizar(ator.getId(), List.of("VISUALIZAR_OBRA"))).getMessage()),
                () -> assertThrows(Forbidden.class, () -> service.deletar(ator.getId())),
                () -> assertEquals("As permissões do cargo ADMIN são fixas.", assertThrows(BadRequest.class,
                        () -> cargoService.atualizar(Cargo.ADMIN, List.of())).getMessage()));
        assertEquals(Cargo.ADMIN, ator.getCargoGlobal());
        verify(funcionarios, never()).save(any());
        verify(funcionarios, never()).deleteById(any());
        verifyNoInteractions(cargos, individuais);

        Funcionario outro = new Funcionario();
        outro.setId(2L);
        outro.setCargoGlobal(Cargo.ENGENHEIRO);
        when(funcionarios.findById(2L)).thenReturn(Optional.of(outro));
        when(funcionarios.save(outro)).thenReturn(outro);
        assertEquals(Cargo.RESPONSAVEL_ADMINISTRATIVO, service.atualizarCargo(2L, "RESPONSAVEL_ADMINISTRATIVO").getCargo());
    }

    @Test
    void guardasNaoBloqueiamCadastroEEdicaoComumDeNaoAdmin() {
        assertDoesNotThrow(() -> acesso.exigirCriacaoFuncionario(Cargo.ENGENHEIRO));
        assertDoesNotThrow(() -> acesso.exigirEdicaoFuncionario(ator, Cargo.RESPONSAVEL_ADMINISTRATIVO));
        assertDoesNotThrow(() -> acesso.exigirExclusaoFuncionario(ator));
        verifyNoInteractions(funcionarios);
    }

    @Test
    void adminPersistidoTemAcessoSemAuthorityAdminEContaInexistenteNaoTem() {
        ator.setCargoGlobal(Cargo.ADMIN);
        persistirAtor();
        assertDoesNotThrow(acesso::exigirAdmin);
        ator.setCargoGlobal(Cargo.ENGENHEIRO);
        assertThrows(Forbidden.class, acesso::exigirAdmin);
        when(funcionarios.findByEmailIgnoreCase(ator.getEmail())).thenReturn(Optional.empty());
        assertThrows(Forbidden.class, acesso::exigirAdmin);
        SecurityContextHolder.clearContext();
        assertThrows(Forbidden.class, acesso::exigirAdmin);
    }
}