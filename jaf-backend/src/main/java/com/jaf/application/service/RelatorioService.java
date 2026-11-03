package com.jaf.application.service;

import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Relatorio;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.RelatorioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class RelatorioService {

    private final RelatorioRepository relatorioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final AcessoObraService acesso;

    public RelatorioService(RelatorioRepository relatorioRepository,
                            FuncionarioRepository funcionarioRepository,
                            AcessoObraService acesso) {
        this.relatorioRepository = relatorioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.acesso = acesso;
    }

    public Relatorio criar(RelatorioDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioResponsavelId())
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado"));

        Relatorio relatorio = new Relatorio();
        relatorio.setTitulo(dto.getTitulo());
        relatorio.setDtEmissao(dto.getDtEmissao());
        relatorio.setFuncionarioResponsavel(funcionario);

        return relatorioRepository.save(relatorio);
    }

    public List<Relatorio> listar() {
        if (acesso.isAdmin()) return relatorioRepository.findAll();
        return relatorioRepository.findAll().stream()
                .filter(r -> r.getFuncionarioResponsavel().getId().equals(acesso.atual().getId()))
                .toList();
    }

    public Relatorio buscarPorId(Long id) {
        Relatorio relatorio = relatorioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Relatorio nao encontrado"));
        if (!acesso.isAdmin() && !relatorio.getFuncionarioResponsavel().getId().equals(acesso.atual().getId())) {
            throw new Forbidden("Usuário não possui acesso a este relatório.");
        }
        return relatorio;
    }

    public Relatorio atualizar(Long id, RelatorioDto dto) {
        Relatorio existente = buscarPorId(id);
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioResponsavelId())
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado"));

        existente.setTitulo(dto.getTitulo());
        existente.setDtEmissao(dto.getDtEmissao());
        existente.setFuncionarioResponsavel(funcionario);

        return relatorioRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!relatorioRepository.existsById(id)) {
            throw new NotFoundException("Relatorio nao encontrado");
        }
        relatorioRepository.deleteById(id);
    }
}
