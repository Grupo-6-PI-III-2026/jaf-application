package com.jaf.application.service;

import com.jaf.application.dto.ObraResumoDto;
import com.jaf.application.dto.PessoaDaObraDto;
import com.jaf.application.enums.FuncaoObra;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.GastoRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ObraResumoService {
    private final ObraService obraService;
    private final GastoRepository gastoRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;

    public ObraResumoService(ObraService obraService, GastoRepository gastoRepository,
                             AlocacaoObraRepository alocacaoObraRepository) {
        this.obraService = obraService;
        this.gastoRepository = gastoRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
    }

    /**
     * Uma página de obras com responsável e total gasto, calculados em lote (sem consulta por obra).
     * O total só é calculado para ADMIN: para os demais o campo nem existe na resposta.
     */
    @Transactional(readOnly = true)
    public Page<ObraResumoDto> resumo(String email, int page, int size, String sortBy, String sortDir, String status,
                                      boolean admin, boolean podeVerAlocacoes) {
        Page<Obra> obras = obraService.listarPaginadoPorUsuario(email, page, size, sortBy, sortDir, status);
        List<Long> ids = obras.getContent().stream().map(Obra::getId).toList();
        if (ids.isEmpty()) return obras.map(obra -> null);

        Map<Long, BigDecimal> totais = admin
                ? gastoRepository.totaisPorObra(ids).stream()
                        .collect(Collectors.toMap(GastoRepository.TotalPorObra::getObraId, GastoRepository.TotalPorObra::getTotal))
                : Map.of();
        Map<Long, ObraResumoDto.Responsavel> responsaveis = podeVerAlocacoes ? responsaveis(ids) : Map.of();

        return obras.map(obra -> new ObraResumoDto(obra.getId(), obra.getTitulo(), obra.getStatus(), obra.getDtInicio(),
                obra.getDtTerminoPrevisto(), responsaveis.get(obra.getId()),
                admin ? totais.getOrDefault(obra.getId(), BigDecimal.ZERO) : null));
    }

    // Prefere o gestor da obra; sem gestor, a alocação mais antiga.
    private Map<Long, ObraResumoDto.Responsavel> responsaveis(List<Long> obraIds) {
        Map<Long, AlocacaoObra> escolhida = new HashMap<>();
        for (AlocacaoObra alocacao : alocacaoObraRepository.findComFuncionarioByObraIds(obraIds)) {
            AlocacaoObra atual = escolhida.get(alocacao.getObra().getId());
            if (atual == null || (atual.getCargo() != FuncaoObra.GESTOR_OBRA && alocacao.getCargo() == FuncaoObra.GESTOR_OBRA)) {
                escolhida.put(alocacao.getObra().getId(), alocacao);
            }
        }
        return escolhida.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new ObraResumoDto.Responsavel(e.getValue().getFuncionario().getId(), e.getValue().getFuncionario().getNome())));
    }

    @Transactional(readOnly = true)
    public List<PessoaDaObraDto> pessoas(Long obraId) {
        Map<Long, PessoaDaObraDto> pessoas = new LinkedHashMap<>();
        for (AlocacaoObra alocacao : alocacaoObraRepository.findByObraId(obraId)) {
            pessoas.putIfAbsent(alocacao.getFuncionario().getId(),
                    new PessoaDaObraDto(alocacao.getFuncionario().getId(), alocacao.getFuncionario().getNome(), alocacao.getCargo()));
        }
        // Autores de gastos que já saíram da obra continuam filtráveis.
        for (GastoRepository.PessoaDoGasto autor : gastoRepository.pessoasComGastoNaObra(obraId)) {
            pessoas.putIfAbsent(autor.getId(), new PessoaDaObraDto(autor.getId(), autor.getNome(), null));
        }
        return pessoas.values().stream()
                .sorted(Comparator.comparing(PessoaDaObraDto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}