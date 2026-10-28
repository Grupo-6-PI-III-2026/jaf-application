package com.jaf.application.service;

import com.jaf.application.dto.PresencaDto;
import com.jaf.application.dto.PresencaListarDto;
import com.jaf.application.dto.PresencaResponseDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Presenca;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import com.jaf.application.repository.PresencaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PresencaService {
    private final PresencaRepository presencaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final AcessoObraService acesso;

    public PresencaService(
            PresencaRepository presencaRepository,
            FuncionarioRepository funcionarioRepository,
            ObraRepository obraRepository,
            AlocacaoObraRepository alocacaoObraRepository,
            AcessoObraService acesso) {
        this.presencaRepository = presencaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.acesso = acesso;
    }

    public PresencaResponseDto criar(PresencaDto dto) {
        acesso.exigirObra(dto.getObraId());
        validarManual(dto);
        var funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Funcionario não encontrado."));
        
        var obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));

        // Verificar se funcionário está alocado na obra
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioIdAndObraId(
                dto.getFuncionarioId(), dto.getObraId());
        
        if (alocacoes.isEmpty()) {
            throw new Conflict("Funcionario não está alocado nesta obra.");
        }

        // Verificar se já existe presença para este funcionário nesta obra nesta data
        if (presencaRepository.existsByFuncionarioIdAndObraIdAndData(
                dto.getFuncionarioId(), dto.getObraId(), dto.getData())) {
            throw new Conflict("Já existe registro de presença para este funcionário nesta data.");
        }

        Presenca presenca = new Presenca();
        presenca.setFuncionario(funcionario);
        presenca.setObra(obra);
        presenca.setData(dto.getData());
        presenca.setPresente(dto.getPresente());
        presenca.setHorarioEntrada(dto.getHorarioEntrada());
        presenca.setHorarioSaida(dto.getHorarioSaida());

        Presenca salva = presencaRepository.save(presenca);
        return converterParaResponseDto(salva);
    }

    public List<PresencaListarDto> listarPorObraEData(Long obraId, LocalDate data) {
        acesso.exigirObra(obraId);
        if (!obraRepository.existsById(obraId)) throw new NotFoundException("Obra não encontrada.");
        if (data == null) throw new BadRequest("Informe uma data válida.");
        // Buscar todas as alocações da obra
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByObraId(obraId);
        
        // Buscar presenças já registradas para esta data
        List<Presenca> presencasExistentes = presencaRepository.findByObraIdAndData(obraId, data);
        
        return alocacoes.stream()
            .filter(a -> alocadoNaData(a, data))
            .collect(Collectors.toMap(a -> a.getFuncionario().getId(), a -> a, (a, b) -> a))
            .values().stream().map(alocacao -> {
            PresencaListarDto dto = new PresencaListarDto();
            
            // Buscar presença existente para este funcionário nesta data
            Optional<Presenca> presencaOpt = presencasExistentes.stream()
                    .filter(p -> p.getFuncionario().getId().equals(alocacao.getFuncionario().getId()))
                    .findFirst();
            
            if (presencaOpt.isPresent()) {
                Presenca presenca = presencaOpt.get();
                dto.setId(presenca.getId());
                dto.setPresente(presenca.getPresente());
                dto.setDesabilitado(false);
            } else {
                dto.setPresente(false);
                dto.setDesabilitado(false);
            }
            
            dto.setFuncionarioId(alocacao.getFuncionario().getId());
            dto.setFuncionarioNome(alocacao.getFuncionario().getNome());
            dto.setFuncionarioCargo(alocacao.getCargo().name());
            dto.setData(data);
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Lista registros de presença existentes no intervalo (visão agregada semanal/período).
     * Diferente do dia único, não expande alocados sem registro.
     */
    public List<PresencaListarDto> listarPorObraEPeriodo(Long obraId, LocalDate from, LocalDate to) {
        acesso.exigirObra(obraId);
        if (!obraRepository.existsById(obraId)) {
            throw new NotFoundException("Obra não encontrada.");
        }
        if (from == null || to == null) {
            throw new BadRequest("Informe as datas inicial e final do período.");
        }
        if (to.isBefore(from)) {
            throw new BadRequest("A data final deve ser maior ou igual à data inicial.");
        }

        Map<Long, Cargo> cargoPorFuncionario = alocacaoObraRepository.findByObraId(obraId).stream()
                .collect(Collectors.toMap(
                        a -> a.getFuncionario().getId(),
                        AlocacaoObra::getCargo,
                        (primeiro, ignorado) -> primeiro
                ));

        return presencaRepository.findByObraIdAndDataBetween(obraId, from, to).stream()
                .sorted((a, b) -> {
                    int porData = b.getData().compareTo(a.getData());
                    if (porData != 0) return porData;
                    return a.getFuncionario().getNome().compareToIgnoreCase(b.getFuncionario().getNome());
                })
                .map(presenca -> {
                    PresencaListarDto dto = new PresencaListarDto();
                    dto.setId(presenca.getId());
                    dto.setFuncionarioId(presenca.getFuncionario().getId());
                    dto.setFuncionarioNome(presenca.getFuncionario().getNome());
                    Cargo cargo = cargoPorFuncionario.get(presenca.getFuncionario().getId());
                    dto.setFuncionarioCargo(cargo != null ? cargo.name() : "");
                    dto.setData(presenca.getData());
                    dto.setPresente(presenca.getPresente());
                    dto.setDesabilitado(false);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public PresencaResponseDto buscarPorId(Long id) {
        Presenca presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        acesso.exigirObra(presenca.getObra().getId());
        return converterParaResponseDto(presenca);
    }

    public PresencaResponseDto atualizar(Long id, PresencaDto dto) {
        Presenca existente = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        acesso.exigirObra(existente.getObra().getId());
        acesso.exigirObra(dto.getObraId());
        validarManual(dto);

        var funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Funcionario não encontrado."));
        
        var obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));

        // Verificar se existe outra presença para o mesmo funcionário/obra/data (excluindo a atual)
        Optional<Presenca> duplicata = presencaRepository.findByFuncionarioIdAndObraIdAndData(
                dto.getFuncionarioId(), dto.getObraId(), dto.getData());
        
        if (duplicata.isPresent() && !duplicata.get().getId().equals(id)) {
            throw new Conflict("Já existe registro de presença para este funcionário nesta data.");
        }

        existente.setFuncionario(funcionario);
        existente.setObra(obra);
        existente.setData(dto.getData());
        existente.setPresente(dto.getPresente());
        existente.setHorarioEntrada(dto.getHorarioEntrada());
        existente.setHorarioSaida(dto.getHorarioSaida());

        Presenca atualizada = presencaRepository.save(existente);
        return converterParaResponseDto(atualizada);
    }

    public void deletar(Long id) {
        Presenca existente = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        acesso.exigirObra(existente.getObra().getId());
        presencaRepository.deleteById(id);
    }

    public void alternarPresenca(Long id) {
        Presenca presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        acesso.exigirObra(presenca.getObra().getId());
        if (presenca.getData().isAfter(LocalDate.now())) throw new BadRequest("Não é possível registrar presença futura.");
        presenca.setPresente(!presenca.getPresente());
        if (!presenca.getPresente()) {
            presenca.setHorarioEntrada(null);
            presenca.setHorarioSaida(null);
        }
        presencaRepository.save(presenca);
    }

    private boolean alocadoNaData(AlocacaoObra alocacao, LocalDate data) {
        return (alocacao.getDtInicio() == null || !data.isBefore(alocacao.getDtInicio()))
                && (alocacao.getDtFim() == null || !data.isAfter(alocacao.getDtFim()));
    }

    private void validarManual(PresencaDto dto) {
        if (dto.getData() == null || dto.getData().isAfter(LocalDate.now()))
            throw new BadRequest("Presença exige data não futura.");
        if (alocacaoObraRepository.findByFuncionarioIdAndObraId(dto.getFuncionarioId(), dto.getObraId())
                .stream().noneMatch(a -> alocadoNaData(a, dto.getData())))
            throw new Conflict("Funcionário não está alocado na obra na data informada.");
        if (Boolean.FALSE.equals(dto.getPresente()) && (dto.getHorarioEntrada() != null || dto.getHorarioSaida() != null))
            throw new BadRequest("Ausência não pode ter horários.");
        if (dto.getHorarioSaida() != null && (dto.getHorarioEntrada() == null || !dto.getHorarioSaida().isAfter(dto.getHorarioEntrada())))
            throw new BadRequest("Saída deve ser posterior à entrada.");
    }

    private PresencaResponseDto converterParaResponseDto(Presenca presenca) {
        PresencaResponseDto dto = new PresencaResponseDto();
        dto.setId(presenca.getId());
        dto.setFuncionarioId(presenca.getFuncionario().getId());
        dto.setFuncionarioNome(presenca.getFuncionario().getNome());
        dto.setObraId(presenca.getObra().getId());
        dto.setObraTitulo(presenca.getObra().getTitulo());
        dto.setData(presenca.getData());
        dto.setPresente(presenca.getPresente());
        dto.setHorarioEntrada(presenca.getHorarioEntrada());
        dto.setHorarioSaida(presenca.getHorarioSaida());
        return dto;
    }
}
