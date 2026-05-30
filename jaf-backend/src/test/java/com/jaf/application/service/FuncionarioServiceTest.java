package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        funcionario.setEmail("joao@email.com");
        funcionario.setSenha("hashedPassword");
        funcionario.setCargoGlobal(Cargo.ADMIN);

        funcionarioDto = new FuncionarioDto();
        funcionarioDto.setNome("João Silva");
        funcionarioDto.setEmail("joao@email.com");
        funcionarioDto.setSenha("password123");
        funcionarioDto.setCargo(Cargo.ADMIN);
    }

    @Test
    void testCriarFuncionarioComSucesso() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

        FuncionarioResponseDto resultado = funcionarioService.criar(funcionarioDto);

        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void testCriarFuncionarioComNomeExistente() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(true);

        assertThrows(Conflict.class, () -> funcionarioService.criar(funcionarioDto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void testCriarFuncionarioComEmailExistente() {
        when(funcionarioRepository.existsByNome(anyString())).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionario));

        assertThrows(Conflict.class, () -> funcionarioService.criar(funcionarioDto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void testBuscarPorIdComSucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        FuncionarioResponseDto resultado = funcionarioService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(funcionarioRepository).findById(1L);
    }

    @Test
    void testBuscarPorIdNaoExistente() {
        when(funcionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> funcionarioService.buscarPorId(99L));
        verify(funcionarioRepository).findById(99L);
    }
}
