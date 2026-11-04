package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.dto.ObraUpdateDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.exceptions.StaleVersion;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

@Service
@Transactional
public class ObraService {
    private static final Logger logger = LoggerFactory.getLogger(ObraService.class);

    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final FuncionarioRepository funcionarioRepository;

    public ObraService(ObraRepository obraRepository,
                       AlocacaoObraRepository alocacaoObraRepository,
                       FuncionarioRepository funcionarioRepository) {
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public Obra criar(ObraDto dto) {
        return criar(dto, null, false);
    }

    public Obra criar(ObraDto dto, Funcionario criador, boolean podeAlocar) {
        logger.info("Tentando criar nova obra: {}", dto.getTitulo());

        Funcionario responsavel = null;
        if (dto.getResponsavelId() != null) {
            if (!podeAlocar) {
                throw new Forbidden("Você não tem permissão para alocar o responsável da obra.");
            }
            responsavel = funcionarioRepository.findById(dto.getResponsavelId())
                    .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
            if (!responsavel.isAtivo()) {
                throw new Conflict("O responsável selecionado está inativo.");
            }
        }

        if (obraRepository.existsByTitulo(dto.getTitulo())) {
            logger.warn("Tentativa de criar obra com título duplicado: {}", dto.getTitulo());
            throw new Conflict("Obra ja existente.");
        }

        if (dto.getDtTerminoPrevisto().isBefore(dto.getDtInicio())) {
            logger.warn("Data de término prevista anterior à data de início para obra: {}", dto.getTitulo());
            throw new BadRequest("Data de término prevista deve ser posterior à data de início.");
        }

        Obra obra = new Obra();
        obra.setTitulo(dto.getTitulo());
        obra.setOrcamento(new BigDecimal(dto.getOrcamento()));
        obra.setStatus(dto.getStatus());
        obra.setDtInicio(dto.getDtInicio());
        obra.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        Obra salva = obraRepository.save(obra);
        if (responsavel != null) {
            alocar(salva, responsavel);
        }
        if (criador != null && criador.getCargoGlobal() != Cargo.ADMIN
                && (responsavel == null || !responsavel.getId().equals(criador.getId()))) {
            alocar(salva, criador);
        }
        logger.info("Obra criada com sucesso: ID={}, Titulo={}", salva.getId(), salva.getTitulo());
        return salva;
    }

    private void alocar(Obra obra, Funcionario funcionario) {
        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(com.jaf.application.enums.FuncaoObra.GESTOR_OBRA);
        alocacao.setDtInicio(java.time.LocalDate.now());
        alocacaoObraRepository.save(alocacao);
    }

    public Funcionario buscarFuncionario(String email) {
        return funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
    }

    public List<Obra> listarPorUsuario(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return obraRepository.findAll();
        }

        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
        if (alocacoes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> obraIds = new HashSet<>();
        alocacoes.forEach(alocacao -> obraIds.add(alocacao.getObra().getId()));
        return obraRepository.findByIdIn(obraIds);
    }

    public Page<Obra> listarPaginadoPorUsuario(String email, int page, int size, String sortBy, String sortDir, String status) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        Pageable pageable = PaginacaoUtil.validar(page, size, sortBy, sortDir,
                Set.of("id", "titulo", "status", "dtInicio", "dtTerminoPrevisto"), 100);

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            if (status != null && !status.isEmpty()) {
                return obraRepository.findAllByStatus(status, pageable);
            }
            return obraRepository.findAll(pageable);
        }

        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
        if (alocacoes.isEmpty()) {
            return Page.empty();
        }

        Set<Long> obraIds = new HashSet<>();
        alocacoes.forEach(alocacao -> obraIds.add(alocacao.getObra().getId()));

        if (status != null && !status.isEmpty()) {
            return obraRepository.findByIdInAndStatus(obraIds, status, pageable);
        }
        return obraRepository.findByIdIn(obraIds, pageable);
    }

    public Obra buscarPorIdComEscopo(Long id, String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = buscarPorId(id);

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return obra;
        }

        boolean alocado = alocacaoObraRepository.existsAtiva(funcionario.getId(), id, java.time.LocalDate.now());
        if (!alocado) {
            throw new Forbidden("Funcionario nao esta alocado nesta obra.");
        }
        return obra;
    }

    public Obra buscarPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));
    }

    public Obra atualizar(Long id, ObraDto dto, boolean admin) {
        Obra existente = obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));
        validarVersao(dto.getVersao(), existente);

        if (dto.getDtTerminoPrevisto().isBefore(dto.getDtInicio())) {
            throw new BadRequest("Data de término prevista deve ser posterior à data de início.");
        }

        BigDecimal novoOrcamento = new BigDecimal(dto.getOrcamento());
        if (!admin && existente.getOrcamento().compareTo(novoOrcamento) != 0) {
            throw new Forbidden("Somente administradores podem alterar o orçamento da obra.");
        }

        existente.setTitulo(dto.getTitulo());
        existente.setOrcamento(novoOrcamento);
        existente.setStatus(dto.getStatus());
        existente.setDtInicio(dto.getDtInicio());
        existente.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(existente);
    }

    public Obra atualizarParcial(Long id, ObraUpdateDto dto) {
        Obra existente = obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));
        validarVersao(dto.getVersao(), existente);

        if (dto.getDtTerminoPrevisto().isBefore(dto.getDtInicio())) {
            throw new BadRequest("Data de término prevista deve ser posterior à data de início.");
        }

        existente.setTitulo(dto.getTitulo());
        existente.setStatus(dto.getStatus());
        existente.setDtInicio(dto.getDtInicio());
        existente.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(existente);
    }

    private void validarVersao(Long versao, Obra existente) {
        if (versao != null && !versao.equals(existente.getVersao())) {
            throw new StaleVersion();
        }
    }

    public void deletar(Long id) {
        logger.info("Tentando deletar obra: ID={}", id);

        if (!obraRepository.existsById(id)) {
            logger.warn("Tentativa de deletar obra inexistente: ID={}", id);
            throw new NotFoundException("Obra nao encontrada");
        }

        if (alocacaoObraRepository.existsByObraId(id)) {
            logger.warn("Tentativa de deletar obra com alocações vinculadas: ID={}", id);
            throw new Conflict("Não é possível excluir esta obra pois existem alocações vinculadas a ela. Remova as alocações antes de excluir a obra.");
        }

        try {
            obraRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Tentativa de deletar obra com registros vinculados: ID={}", id);
            throw new Conflict("Não é possível excluir esta obra pois existem registros vinculados a ela.");
        }
        logger.info("Obra deletada com sucesso: ID={}", id);
    }
}
