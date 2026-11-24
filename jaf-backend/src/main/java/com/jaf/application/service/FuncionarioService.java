package com.jaf.application.service;

import com.jaf.application.armazenamento.ArquivoService;
import com.jaf.application.armazenamento.TipoArquivo;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class FuncionarioService {
    private static final Logger logger = LoggerFactory.getLogger(FuncionarioService.class);
    private static final long FOTO_MAX_BYTES = 5L * 1024L * 1024L;
    private static final Pattern AVATAR_PADRAO = Pattern.compile("^/assets/Geral/avatar-[a-z]+\\.svg$");

    private final PasswordEncoder passwordEncoder;
    private final FuncionarioRepository funcionarioRepository;
    private final GerenciadorTokenJwt gerenciadorTokenJwt;
    private final AuthenticationManager authenticationManager;
    private final AcessoObraService acesso;
    private final ArquivoService arquivos;

    public FuncionarioService(PasswordEncoder passwordEncoder, FuncionarioRepository funcionarioRepository,
                              GerenciadorTokenJwt gerenciadorTokenJwt, AuthenticationManager authenticationManager,
                              AcessoObraService acesso, ArquivoService arquivos) {
        this.passwordEncoder = passwordEncoder;
        this.funcionarioRepository = funcionarioRepository;
        this.gerenciadorTokenJwt = gerenciadorTokenJwt;
        this.authenticationManager = authenticationManager;
        this.acesso = acesso;
        this.arquivos = arquivos;
    }

    public FuncionarioResponseDto paraResposta(Funcionario funcionario) {
        FuncionarioResponseDto resposta = new FuncionarioResponseDto(funcionario);
        if (funcionario.getFotoChave() != null) {
            resposta.setFotoUrl(arquivos.gerarUrl(funcionario.getFotoChave()));
        }
        return resposta;
    }

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        acesso.exigirCriacaoFuncionario(dto.getCargo());
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
        return paraResposta(salvo);
    }

    public List<FuncionarioResponseDto> listar() {
        List<Funcionario> funcionariosEncontrados = funcionarioRepository.findAll();
        if (funcionariosEncontrados.isEmpty()) {
            throw new NoContent("Lista de funcionários vazia.");
        }

        return funcionariosEncontrados.stream()
                .map(this::paraResposta)
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
        return paraResposta(funcionario);
    }

    public FuncionarioResponseDto atualizar(Long id, FuncionarioDto dto) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        acesso.exigirEdicaoFuncionario(existente, dto.getCargo());
        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());
        existente.setCargoGlobal(dto.getCargo());

        return paraResposta(funcionarioRepository.save(existente));
    }

    public FuncionarioResponseDto atualizarPerfil(String emailAtual, FuncionarioPerfilUpdateDto dto) {
        Funcionario existente = buscarPorEmail(emailAtual);

        String nome = dto.getNome().trim();
        String email = dto.getEmail().trim().toLowerCase();
        String fotoUrl = dto.getFotoUrl() == null || dto.getFotoUrl().isBlank() ? null : dto.getFotoUrl().trim();
        boolean avatarPadrao = fotoUrl != null && AVATAR_PADRAO.matcher(fotoUrl).matches();
        // Qualquer outro valor só é aceito como "manter a foto enviada"; URLs externas nunca são gravadas.
        if (fotoUrl != null && !avatarPadrao && existente.getFotoChave() == null) {
            throw new BadRequest("Envie a foto pelo upload ou escolha um dos avatares disponíveis.");
        }

        if (!email.equalsIgnoreCase(existente.getEmail())) {
            funcionarioRepository.findByEmailIgnoreCase(email)
                    .filter(f -> !f.getId().equals(existente.getId()))
                    .ifPresent(f -> {
                        throw new Conflict("E-mail ja cadastrado.");
                    });
        }

        existente.setNome(nome);
        existente.setEmail(email);
        String fotoRemovida = null;
        if (fotoUrl == null || avatarPadrao) {
            fotoRemovida = existente.getFotoChave();
            existente.setFotoChave(null);
            existente.setFotoUrl(fotoUrl);
        }

        Funcionario salvo = funcionarioRepository.save(existente);
        arquivos.removerAposCommit(fotoRemovida);
        return paraResposta(salvo);
    }

    public String atualizarFoto(String email, MultipartFile arquivo) {
        Funcionario existente = buscarPorEmail(email);
        String anterior = existente.getFotoChave();
        existente.setFotoChave(arquivos.armazenar(arquivo, "perfil", TipoArquivo.IMAGENS, FOTO_MAX_BYTES));
        existente.setFotoUrl(null);
        Funcionario salvo = funcionarioRepository.save(existente);
        arquivos.removerAposCommit(anterior);
        return arquivos.gerarUrl(salvo.getFotoChave());
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
        arquivos.removerAposCommit(existente.getFotoChave());
    }

    public void deletar(Long id) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        acesso.exigirExclusaoFuncionario(existente);
        acesso.exigirExclusaoDeOutro(existente);
        funcionarioRepository.deleteById(id);
        arquivos.removerAposCommit(existente.getFotoChave());
    }

    public FuncionarioResponseDto atualizarCargo(Long id, String novoCargo) {
        acesso.exigirAdmin();
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        acesso.exigirGestaoDeAcessoDeOutro(existente);

        com.jaf.application.enums.Cargo cargo;
        try {
            cargo = com.jaf.application.enums.Cargo.valueOf(novoCargo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Cargo inválido: " + novoCargo);
        }

        existente.setCargoGlobal(cargo);
        return paraResposta(funcionarioRepository.save(existente));
    }

  
}
