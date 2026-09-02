package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.AlterarSenhaDto;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioPerfilUpdateDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Funcionario funcionario;
    private FuncionarioDto funcionarioDto;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");
        funcionario.setEmail("joao@teste.com");
        funcionario.setSenha("senha123");
        funcionario.setCargoGlobal(Cargo.ENGENHEIRO);

        funcionarioDto = new FuncionarioDto();
        funcionarioDto.setNome("João Silva");
        funcionarioDto.setEmail("joao@teste.com");
        funcionarioDto.setSenha("senha123");
        funcionarioDto.setCargo(Cargo.ENGENHEIRO);
    }

    @Test
    void criarFuncionario_Sucesso() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("senhaCriptografada");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

        var resultado = funcionarioService.criar(funcionarioDto);

        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(funcionarioRepository, times(1)).save(any(Funcionario.class));
    }

    @Test
    void criarFuncionario_NomeDuplicado_Conflict() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(true);

        assertThrows(Conflict.class, () -> funcionarioService.criar(funcionarioDto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void criarFuncionario_EmailDuplicado_Conflict() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));

        assertThrows(Conflict.class, () -> funcionarioService.criar(funcionarioDto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void buscarPorEmail_Existente_Sucesso() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));

        var resultado = funcionarioService.buscarPorEmail("joao@teste.com");

        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(funcionarioRepository, times(1)).findByEmailIgnoreCase(anyString());
    }

    @Test
    void buscarPorEmail_Inexistente_NotFoundException() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> funcionarioService.buscarPorEmail("inexistente@teste.com"));
    }

    @Test
    void buscarPorId_Existente_Sucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        var resultado = funcionarioService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(funcionarioRepository, times(1)).findById(1L);
    }

    @Test
    void buscarPorId_Inexistente_NotFoundException() {
        when(funcionarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> funcionarioService.buscarPorId(999L));
    }

    @Test
    void atualizarPerfil_Sucesso() {
        FuncionarioPerfilUpdateDto dto = new FuncionarioPerfilUpdateDto();
        dto.setNome("João Atualizado");
        dto.setEmail("joao.atualizado@teste.com");

        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.findByEmailIgnoreCase("joao.atualizado@teste.com")).thenReturn(Optional.empty());
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

        var resultado = funcionarioService.atualizarPerfil("joao@teste.com", dto);

        assertNotNull(resultado);
        verify(funcionarioRepository, times(1)).save(any(Funcionario.class));
    }

    @Test
    void alterarSenha_SenhasNaoConferem_BadRequest() {
        AlterarSenhaDto dto = new AlterarSenhaDto();
        dto.setSenhaAtual("senha123");
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmacaoSenha("senhaDiferente");

        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));

        assertThrows(BadRequest.class, () -> funcionarioService.alterarSenha("joao@teste.com", dto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void alterarSenha_SenhaAtualInvalida_Forbidden() {
        AlterarSenhaDto dto = new AlterarSenhaDto();
        dto.setSenhaAtual("senhaErrada");
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmacaoSenha("novaSenha123");

        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(Forbidden.class, () -> funcionarioService.alterarSenha("joao@teste.com", dto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void alterarSenha_Sucesso() {
        AlterarSenhaDto dto = new AlterarSenhaDto();
        dto.setSenhaAtual("senha123");
        dto.setNovaSenha("novaSenha123");
        dto.setConfirmacaoSenha("novaSenha123");

        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("novaSenhaCriptografada");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

        funcionarioService.alterarSenha("joao@teste.com", dto);

        verify(funcionarioRepository, times(1)).save(any(Funcionario.class));
    }

    @Test
    void atualizarCargo_Sucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

        var resultado = funcionarioService.atualizarCargo(1L, "GESTOR_OBRA");

        assertNotNull(resultado);
        verify(funcionarioRepository, times(1)).save(any(Funcionario.class));
    }

    @Test
    void atualizarCargo_CargoInvalido_IllegalArgumentException() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        assertThrows(IllegalArgumentException.class, () -> funcionarioService.atualizarCargo(1L, "CARGO_INEXISTENTE"));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }
}