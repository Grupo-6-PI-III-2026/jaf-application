package com.jaf.application.service;

import com.jaf.application.dto.PresencaDto;
import com.jaf.application.dto.PresencaListarDto;
import com.jaf.application.dto.PresencaResponseDto;
import com.jaf.application.enums.Cargo;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PresencaService {
    private final PresencaRepository presencaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;

    public PresencaService(
            PresencaRepository presencaRepository,
            FuncionarioRepository funcionarioRepository,
            ObraRepository obraRepository,
            AlocacaoObraRepository alocacaoObraRepository) {
        this.presencaRepository = presencaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
    }

    public PresencaResponseDto criar(PresencaDto dto) {
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
        // Buscar todas as alocações da obra
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByObraId(obraId);
        
        // Buscar presenças já registradas para esta data
        List<Presenca> presencasExistentes = presencaRepository.findByObraIdAndData(obraId, data);
        
        return alocacoes.stream().map(alocacao -> {
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

    public PresencaResponseDto buscarPorId(Long id) {
        Presenca presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        return converterParaResponseDto(presenca);
    }

    public PresencaResponseDto atualizar(Long id, PresencaDto dto) {
        Presenca existente = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));

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
        if (!presencaRepository.existsById(id)) {
            throw new NotFoundException("Presença não encontrada.");
        }
        presencaRepository.deleteById(id);
    }

    public void alternarPresenca(Long id) {
        Presenca presenca = presencaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Presença não encontrada."));
        
        presenca.setPresente(!presenca.getPresente());
        presencaRepository.save(presenca);
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
