package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.ObraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ObraService {
    private final ObraRepository obraRepository;

    public ObraService(ObraRepository obraRepository) {
        this.obraRepository = obraRepository;
    }

    public Obra criar(ObraDto dto) {
        Obra obra = new Obra();
        obra.setTitulo(dto.getTitulo());
        obra.setOrcamento(dto.getOrcamento());
        obra.setStatus(dto.getStatus());
        obra.setDtInicio(dto.getDtInicio());
        obra.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(obra);
    }

    public List<Obra> listar() {
        return obraRepository.findAll();
    }

    public Obra buscarPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra nao encontrada"));
    }

    public Obra atualizar(Long id, ObraDto dto) {
        Obra existente = buscarPorId(id);
        existente.setTitulo(dto.getTitulo());
        existente.setOrcamento(dto.getOrcamento());
        existente.setStatus(dto.getStatus());
        existente.setDtInicio(dto.getDtInicio());
        existente.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!obraRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra nao encontrada");
        }
        obraRepository.deleteById(id);
    }
}
