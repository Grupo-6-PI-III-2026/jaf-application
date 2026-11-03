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
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioPermissaoRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.PresencaRepository;
import com.jaf.application.repository.RelatorioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
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
    private static final String MENSAGEM_REGISTROS_VINCULADOS =
            "Não é possível excluir este usuário pois existem registros vinculados a ele. "
                    + "Caso queira desativá-lo, utilize a opção de inativar usuário.";
    private static final String MENSAGEM_CONTA_COM_REGISTROS =
            "Não é possível excluir sua conta pois existem registros vinculados a ela. "
                    + "Peça a um administrador para inativá-la.";

    private final PasswordEncoder passwordEncoder;
    private final FuncionarioRepository funcionarioRepository;
    private final GerenciadorTokenJwt gerenciadorTokenJwt;
    private final AuthenticationManager authenticationManager;
    private final AcessoObraService acesso;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final PresencaRepository presencaRepository;
    private final GastoRepository gastoRepository;
    private final RelatorioRepository relatorioRepository;
    private final FuncionarioPermissaoRepository funcionarioPermissaoRepository;

    public FuncionarioService(PasswordEncoder passwordEncoder, FuncionarioRepository funcionarioRepository,
                              GerenciadorTokenJwt gerenciadorTokenJwt, AuthenticationManager authenticationManager,
                              AcessoObraService acesso, AlocacaoObraRepository alocacaoObraRepository,
                              PresencaRepository presencaRepository, GastoRepository gastoRepository,
                              RelatorioRepository relatorioRepository,
                              FuncionarioPermissaoRepository funcionarioPermissaoRepository) {
        this.passwordEncoder = passwordEncoder;
        this.funcionarioRepository = funcionarioRepository;
        this.gerenciadorTokenJwt = gerenciadorTokenJwt;
        this.authenticationManager = authenticationManager;
        this.acesso = acesso;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.presencaRepository = presencaRepository;
        this.gastoRepository = gastoRepository;
        this.relatorioRepository = relatorioRepository;
        this.funcionarioPermissaoRepository = funcionarioPermissaoRepository;
    }

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        acesso.exigirCriacaoFuncionario(dto.getCargo());
        logger.info("Tentando criar novo funcionário: {}", dto.getEmail());

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

        acesso.exigirEdicaoFuncionario(existente, dto.getCargo());
        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());
        existente.setCargoGlobal(dto.getCargo());

        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

    public FuncionarioResponseDto atualizarPerfil(String emailAtual, FuncionarioPerfilUpdateDto dto) {
        Funcionario existente = buscarPorEmail(emailAtual);

        String nome = dto.getNome().trim();
        String email = dto.getEmail().trim().toLowerCase();
        String fotoUrl = dto.getFotoUrl() == null || dto.getFotoUrl().isBlank() ? null : dto.getFotoUrl().trim();

        if (!email.equalsIgnoreCase(existente.getEmail())) {
            funcionarioRepository.findByEmailIgnoreCase(email)
                    .filter(f -> !f.getId().equals(existente.getId()))
                    .ifPresent(f -> {
                        throw new Conflict("E-mail ja cadastrado.");
                    });
        }

        existente.setNome(nome);
        existente.setEmail(email);
        existente.setFotoUrl(fotoUrl);

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

    @Transactional
    public void excluirConta(String email) {
        Funcionario existente = buscarPorEmail(email);
        exigirSemRegistrosVinculados(existente, MENSAGEM_CONTA_COM_REGISTROS);
        removerPermissoesIndividuais(existente);
        excluir(existente, MENSAGEM_CONTA_COM_REGISTROS);
    }

    @Transactional
    public void deletar(Long id) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        acesso.exigirExclusaoFuncionario(existente);
        acesso.exigirExclusaoDeOutro(existente);
        exigirSemRegistrosVinculados(existente, MENSAGEM_REGISTROS_VINCULADOS);
        removerPermissoesIndividuais(existente);
        excluir(existente, MENSAGEM_REGISTROS_VINCULADOS);
    }

    public FuncionarioResponseDto alterarStatus(Long id, boolean ativo) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        acesso.exigirExclusaoFuncionario(existente);
        acesso.exigirInativacaoDeOutro(existente);
        existente.setAtivo(ativo);
        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

    private void exigirSemRegistrosVinculados(Funcionario funcionario, String mensagem) {
        Long id = funcionario.getId();
        if (alocacaoObraRepository.existsByFuncionarioId(id)
                || presencaRepository.existsByFuncionarioId(id)
                || gastoRepository.existsByFuncionarioId(id)
                || relatorioRepository.existsByFuncionarioResponsavelId(id)) {
            throw new Conflict(mensagem);
        }
    }

    private void removerPermissoesIndividuais(Funcionario funcionario) {
        funcionarioPermissaoRepository.deleteByFuncionarioId(funcionario.getId());
    }

    private void excluir(Funcionario funcionario, String mensagemConflito) {
        try {
            funcionarioRepository.deleteById(funcionario.getId());
            funcionarioRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new Conflict(mensagemConflito);
        }
    }

    public FuncionarioResponseDto atualizarCargo(Long id, String novoCargo) {
        acesso.exigirAdmin();
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        acesso.exigirGestaoDeAcessoDeOutro(existente);
        acesso.exigirAlteracaoDeCargo(existente);

        com.jaf.application.enums.Cargo cargo;
        try {
            cargo = com.jaf.application.enums.Cargo.valueOf(novoCargo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Cargo inválido: " + novoCargo);
        }

        existente.setCargoGlobal(cargo);
        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

  
}
