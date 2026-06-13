package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.AlterarSenhaDto;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioListarDto;
import com.jaf.application.dto.FuncionarioMapper;
import com.jaf.application.dto.FuncionarioPerfilUpdateDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.dto.FuncionarioTokenDto;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioService {
    private static final Logger logger = LoggerFactory.getLogger(FuncionarioService.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    @Autowired
    private AuthenticationManager authenticationManager;

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        logger.info("Tentando criar novo funcionário: {}", dto.getEmail());

        if (funcionarioRepository.existsByNome(dto.getNome())) {
            logger.warn("Tentativa de criar funcionário com nome duplicado: {}", dto.getNome());
            throw new Conflict("Usuário já existe.");
        }

        funcionarioRepository.findByEmailIgnoreCase(dto.getEmail())
                .ifPresent(f -> {
                    logger.warn("Tentativa de criar funcionário com email duplicato: {}", dto.getEmail());
                    throw new Conflict("E-mail já cadastrado.");
                });

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(dto.getNome());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoFuncionario.setCargoGlobal(dto.getCargo());

        Funcionario salvo = funcionarioRepository.save(novoFuncionario);
        logger.info("Funcionário criado com sucesso: ID={}, Email={}", salvo.getId(), salvo.getEmail());
        return new FuncionarioResponseDto(salvo);
    }

    public List<FuncionarioResponseDto> listar() {
        List<Funcionario> funcionariosEncontrados = funcionarioRepository.findAll();
        if (funcionariosEncontrados.isEmpty()) {
            throw new NoContent("Lista de funcionários vazia.");
        }

        return funcionariosEncontrados.stream()
                .map(FuncionarioResponseDto::new)
                .toList();
    }

    public FuncionarioTokenDto autenticar(Funcionario funcionario) {
        logger.info("Tentativa de autenticação: {}", funcionario.getEmail());

        UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(
                funcionario.getEmail(), funcionario.getSenha());

        Authentication authentication = this.authenticationManager.authenticate(credentials);

        Funcionario funcionarioAutenticado = funcionarioRepository.findByEmailIgnoreCase(funcionario.getEmail())
                .orElseThrow(() -> new NotFoundException("Email de funcionário não encontrado."));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = gerenciadorTokenJwt.generateToken(authentication);

        logger.info("Autenticação bem-sucedida: Email={}, Cargo={}", funcionarioAutenticado.getEmail(), funcionarioAutenticado.getCargoGlobal());
        return FuncionarioMapper.of(funcionarioAutenticado, token);
    }

    public Funcionario buscarPorEmail(String email) {
            return funcionarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
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

    public FuncionarioResponseDto atualizarPerfil(String emailAtual, FuncionarioPerfilUpdateDto dto) {
        Funcionario existente = buscarPorEmail(emailAtual);

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(existente.getEmail())) {
            funcionarioRepository.findByEmailIgnoreCase(dto.getEmail())
                    .filter(f -> !f.getId().equals(existente.getId()))
                    .ifPresent(f -> {
                        throw new Conflict("E-mail ja cadastrado.");
                    });
        }

        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());

        if (dto.getFotoUrl() != null) {
            String fotoUrl = dto.getFotoUrl().isBlank() ? null : dto.getFotoUrl();
            existente.setFotoUrl(fotoUrl);
        }

        Funcionario salvo = funcionarioRepository.save(existente);
        return new FuncionarioResponseDto(salvo);
    }

    public void alterarSenha(String email, AlterarSenhaDto dto) {
        logger.info("Tentativa de alteração de senha: {}", email);

        Funcionario existente = buscarPorEmail(email);

        if (!dto.getNovaSenha().equals(dto.getConfirmacaoSenha())) {
            logger.warn("Alteração de senha falhou: senhas não conferem para o usuário {}", email);
            throw new BadRequest("Nova senha e confirmacao nao conferem.");
        }

        if (!passwordEncoder.matches(dto.getSenhaAtual(), existente.getSenha())) {
            logger.warn("Alteração de senha falhou: senha atual inválida para o usuário {}", email);
            throw new Forbidden("Senha atual invalida.");
        }

        existente.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        funcionarioRepository.save(existente);
        logger.info("Senha alterada com sucesso para o usuário: {}", email);
    }

    public void excluirConta(String email) {
        Funcionario existente = buscarPorEmail(email);
        funcionarioRepository.delete(existente);
    }

    public void deletar(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        funcionarioRepository.deleteById(id);
    }

    public FuncionarioResponseDto atualizarCargo(Long id, String novoCargo) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        com.jaf.application.enums.Cargo cargo;
        try {
            cargo = com.jaf.application.enums.Cargo.valueOf(novoCargo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cargo inválido: " + novoCargo);
        }

        existente.setCargoGlobal(cargo);
        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

  
}
