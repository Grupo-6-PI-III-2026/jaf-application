package com.jaf.application.service;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AlocacaoObraService {
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;
    private final AcessoObraService acesso;

    public AlocacaoObraService(AlocacaoObraRepository alocacaoObraRepository,
                               FuncionarioRepository funcionarioRepository,
                               ObraRepository obraRepository,
                               AcessoObraService acesso) {
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
        this.acesso = acesso;
    }

    public AlocacaoObra criar(AlocacaoObraDto dto) {
        validarVigencia(dto.getDtInicio(), dto.getDtFim());
        acesso.exigirObra(dto.getObraId());
        if (!acesso.isAdmin() && acesso.atual().getId().equals(dto.getFuncionarioId())) {
            throw new Conflict("Não é permitido criar autoalocação.");
        }
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        LocalDate inicio = dto.getDtInicio() == null ? LocalDate.now() : dto.getDtInicio();
        if (alocacaoObraRepository.existsSobreposta(dto.getFuncionarioId(), dto.getObraId(),
                inicio, dto.getDtFim(), null)) {
            throw new Conflict("Funcionario ja esta alocado nesta obra.");
        }

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(dto.getCargoNaObra());
        alocacao.setDtInicio(inicio);
        alocacao.setDtFim(dto.getDtFim());
        return alocacaoObraRepository.save(alocacao);
    }

    public List<AlocacaoObra> listarPorUsuario(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return alocacaoObraRepository.findAll();
        }
        return alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
    }

    public List<AlocacaoObra> listarPorObra(Long obraId) {
        acesso.exigirObra(obraId);
        return alocacaoObraRepository.findByObraId(obraId);
    }

    public List<AlocacaoObra> listarPorFuncionario(Long funcionarioId) {
        if (!acesso.isAdmin() && !acesso.atual().getId().equals(funcionarioId)) {
            throw new com.jaf.application.exceptions.Forbidden("Operação restrita ao próprio usuário ou ADMIN.");
        }
        return alocacaoObraRepository.findByFuncionarioId(funcionarioId);
    }

    public AlocacaoObra buscarPorId(Long id) {
        AlocacaoObra alocacao = alocacaoObraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alocacao nao encontrada."));
        acesso.exigirObra(alocacao.getObra().getId());
        return alocacao;
    }

    public AlocacaoObra atualizar(Long id, AlocacaoObraDto dto) {
        AlocacaoObra existente = buscarPorId(id);
        validarVigencia(dto.getDtInicio(), dto.getDtFim());
        if (!existente.getFuncionario().getId().equals(dto.getFuncionarioId())
                || !existente.getObra().getId().equals(dto.getObraId())) {
            throw new Conflict("Funcionario e obra não podem ser alterados em uma alocação.");
        }
        existente.setCargo(dto.getCargoNaObra());
        existente.setDtInicio(dto.getDtInicio() == null ? existente.getDtInicio() : dto.getDtInicio());
        existente.setDtFim(dto.getDtFim());
        return alocacaoObraRepository.save(existente);
    }

    public AlocacaoObra encerrar(Long id) {
        AlocacaoObra existente = buscarPorId(id);
        LocalDate hoje = LocalDate.now();
        if (existente.getDtFim() != null && existente.getDtFim().isBefore(hoje)) {
            return existente;
        }
        existente.setDtFim(hoje);
        return alocacaoObraRepository.save(existente);
    }

    public void deletar(Long id) {
        buscarPorId(id);
        alocacaoObraRepository.deleteById(id);
    }

    private void validarVigencia(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new Conflict("A data final não pode ser anterior à data inicial.");
        }
    }
}
