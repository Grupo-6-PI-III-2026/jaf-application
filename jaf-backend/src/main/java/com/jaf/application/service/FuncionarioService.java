package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.*;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    @Autowired
    private AuthenticationManager authenticationManager;

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        if (funcionarioRepository.existsByNome(dto.getNome())){
            throw new Conflict("Usuário já existe.");
        }
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.getNome());
        funcionario.setEmail(dto.getEmail());
        funcionario.setSenha(dto.getSenha());
        funcionario.setCargoGlobal(dto.getCargo());
        return new FuncionarioResponseDto(funcionarioRepository.save(funcionario));
    }

    public List<FuncionarioResponseDto> listar() {
                if (funcionarioRepository == null){
                    throw new NoContent("Lista de funcionários vazia.");
                }
        return funcionarioRepository.findAll()
                .stream()
                .map(FuncionarioResponseDto::new)
                .toList();
        funcionarioRepository.findByEmail(dto.getEmail())
                .ifPresent(f -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email ja cadastrado");
                });

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(dto.getNome());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoFuncionario.setCargoGlobal(dto.getCargo());

        Funcionario salvo = funcionarioRepository.save(novoFuncionario);
        return new FuncionarioResponseDto(salvo);
    }

    public FuncionarioTokenDto autenticar(Funcionario funcionario) {
        final UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(
                funcionario.getEmail(), funcionario.getSenha());

        final Authentication authentication = this.authenticationManager.authenticate(credentials);

        Funcionario funcionarioAutenticado =
                funcionarioRepository.findByEmail(funcionario.getEmail())
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email de funcionario nao encontrado"));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        final String token = gerenciadorTokenJwt.generateToken(authentication);

        return FuncionarioMapper.of(funcionarioAutenticado, token);
    }

    public List<FuncionarioListarDto> listarTodos() {
        List<Funcionario> funcionariosEncontrados = funcionarioRepository.findAll();
        return funcionariosEncontrados.stream().map(FuncionarioMapper::of).toList();
    }

    public FuncionarioResponseDto buscarPorId(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        return new FuncionarioResponseDto(funcionario);
    }

    public FuncionarioResponseDto atualizar(Long id, FuncionarioDto dto) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());
        existente.setCargoGlobal(dto.getCargo());

        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

    public void deletar(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        funcionarioRepository.deleteById(id);
    }

}
