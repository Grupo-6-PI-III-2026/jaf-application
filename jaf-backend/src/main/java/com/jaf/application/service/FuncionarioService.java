package com.jaf.application.service;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioExternoDto;
import com.jaf.application.dto.FuncionarioListarDto;
import com.jaf.application.dto.FuncionarioMapper;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.dto.FuncionarioTokenDto;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
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
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AlocacaoObraRepository alocacaoObraRepository;

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        if (funcionarioRepository.existsByNome(dto.getNome())) {
            throw new Conflict("Usuário já existe.");
        }

        funcionarioRepository.findByEmailIgnoreCase(dto.getEmail())
                .ifPresent(f -> {
                    throw new Conflict("E-mail já cadastrado.");
                });

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(dto.getNome());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        novoFuncionario.setCargoGlobal(dto.getCargo());
        novoFuncionario.setDocumento(dto.getDocumento());
        novoFuncionario.setTipoFuncionario(dto.getTipoFuncionario() != null ? dto.getTipoFuncionario() : TipoFuncionario.INTERNO);

        Funcionario salvo = funcionarioRepository.save(novoFuncionario);
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
        UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(
                funcionario.getEmail(), funcionario.getSenha());

        Authentication authentication = this.authenticationManager.authenticate(credentials);

        Funcionario funcionarioAutenticado = funcionarioRepository.findByEmailIgnoreCase(funcionario.getEmail())
                .orElseThrow(() -> new NotFoundException("Email de funcionário não encontrado."));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = gerenciadorTokenJwt.generateToken(authentication);

        return FuncionarioMapper.of(funcionarioAutenticado, token);
    }

    public Funcionario buscarPorEmail(String email) {
            return funcionarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    public FuncionarioResponseDto criarExterno(FuncionarioExternoDto dto) {
        if (funcionarioRepository.existsByNome(dto.getNome())) {
            throw new Conflict("Funcionário já existe.");
        }

        if (dto.getEmail() != null) {
            funcionarioRepository.findByEmailIgnoreCase(dto.getEmail())
                    .ifPresent(f -> {
                        throw new Conflict("E-mail já cadastrado.");
                    });
        }

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(dto.getNome());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setDocumento(dto.getDocumento());
        novoFuncionario.setTipoFuncionario(dto.getTipoFuncionario());
        novoFuncionario.setSenha(null); // Externos não têm senha

        Funcionario salvo = funcionarioRepository.save(novoFuncionario);
        return new FuncionarioResponseDto(salvo);
    }

    public List<FuncionarioListarDto> listarTodos() {
        List<Funcionario> funcionariosEncontrados = funcionarioRepository.findAll();
        return funcionariosEncontrados.stream()
                .map(f -> {
                    Integer quantidadeAlocacoes = alocacaoObraRepository.findByFuncionarioId(f.getId()).size();
                    return FuncionarioMapper.of(f, quantidadeAlocacoes);
                })
                .toList();
    }

    public List<FuncionarioListarDto> listarExternos() {
        List<Funcionario> externos = funcionarioRepository.findByTipoFuncionario(TipoFuncionario.EXTERNO);
        return externos.stream()
                .map(f -> {
                    Integer quantidadeAlocacoes = alocacaoObraRepository.findByFuncionarioId(f.getId()).size();
                    return FuncionarioMapper.of(f, quantidadeAlocacoes);
                })
                .toList();
    }

    public List<FuncionarioListarDto> listarInternos() {
        List<Funcionario> internos = funcionarioRepository.findByTipoFuncionario(TipoFuncionario.INTERNO);
        return internos.stream()
                .map(f -> {
                    Integer quantidadeAlocacoes = alocacaoObraRepository.findByFuncionarioId(f.getId()).size();
                    return FuncionarioMapper.of(f, quantidadeAlocacoes);
                })
                .toList();
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
        existente.setDocumento(dto.getDocumento());
        if (dto.getTipoFuncionario() != null) {
            existente.setTipoFuncionario(dto.getTipoFuncionario());
        }

        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

    public void deletar(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        funcionarioRepository.deleteById(id);
    }

  
}
