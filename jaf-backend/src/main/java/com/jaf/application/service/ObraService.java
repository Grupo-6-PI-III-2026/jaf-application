package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import com.jaf.application.repository.PresencaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ObraService {
    private static final Logger logger = LoggerFactory.getLogger(ObraService.class);

    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final GastoRepository gastoRepository;
    private final PresencaRepository presencaRepository;

    public ObraService(ObraRepository obraRepository,
                       AlocacaoObraRepository alocacaoObraRepository,
                       FuncionarioRepository funcionarioRepository,
                       GastoRepository gastoRepository,
                       PresencaRepository presencaRepository) {
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.gastoRepository = gastoRepository;
        this.presencaRepository = presencaRepository;
    }

    public Obra criar(ObraDto dto) {
        logger.info("Tentando criar nova obra: {}", dto.getTitulo());

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
        obra.setOrcamento(dto.getOrcamento());
        obra.setStatus(dto.getStatus());
        obra.setDtInicio(dto.getDtInicio());
        obra.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        Obra salva = obraRepository.save(obra);
        logger.info("Obra criada com sucesso: ID={}, Titulo={}", salva.getId(), salva.getTitulo());
        return salva;
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

    public Obra buscarPorIdComEscopo(Long id, String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = buscarPorId(id);

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return obra;
        }

        boolean alocado = alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), id);
        if (!alocado) {
            throw new Forbidden("Funcionario nao esta alocado nesta obra.");
        }
        return obra;
    }

    public Obra buscarPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));
    }

    public Obra atualizar(Long id, ObraDto dto) {
        Obra existente = obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        if (dto.getDtTerminoPrevisto().isBefore(dto.getDtInicio())) {
            throw new BadRequest("Data de término prevista deve ser posterior à data de início.");
        }

        existente.setTitulo(dto.getTitulo());
        existente.setOrcamento(dto.getOrcamento());
        existente.setStatus(dto.getStatus());
        existente.setDtInicio(dto.getDtInicio());
        existente.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        logger.info("Tentando deletar obra: ID={}", id);

        if (!obraRepository.existsById(id)) {
            logger.warn("Tentativa de deletar obra inexistente: ID={}", id);
            throw new NotFoundException("Obra nao encontrada");
        }

        logger.info("Deletando registros relacionados da obra ID={}", id);

        logger.info("Deletando alocações da obra ID={}", id);
        alocacaoObraRepository.deleteByObraId(id);

        logger.info("Deletando gastos da obra ID={}", id);
        gastoRepository.deleteByObraId(id);

        logger.info("Deletando presenças da obra ID={}", id);
        presencaRepository.deleteByObraId(id);

        logger.info("Deletando obra ID={}", id);
        obraRepository.deleteById(id);
        logger.info("Obra deletada com sucesso: ID={}", id);
    }
}
