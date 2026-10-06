package com.jaf.application.service;

import com.jaf.application.dto.FuncionarioPermissoesAcessoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.Permissao;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.FuncionarioPermissao;
import com.jaf.application.repository.FuncionarioPermissaoRepository;
import com.jaf.application.repository.FuncionarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

@Service
public class FuncionarioPermissaoService {
    private final FuncionarioPermissaoRepository permissaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CargoPermissaoService cargoPermissaoService;

    public FuncionarioPermissaoService(
            FuncionarioPermissaoRepository permissaoRepository,
            FuncionarioRepository funcionarioRepository,
            CargoPermissaoService cargoPermissaoService) {
        this.permissaoRepository = permissaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.cargoPermissaoService = cargoPermissaoService;
    }

    public List<Permissao> permissoesDoFuncionario(Funcionario funcionario) {
        List<FuncionarioPermissao> permissoesCustomizadas = permissaoRepository.findByFuncionarioId(funcionario.getId());
        if (!permissoesCustomizadas.isEmpty()) {
            return permissoesCustomizadas.stream()
                    .map(FuncionarioPermissao::getPermissao)
                    .distinct()
                    .toList();
        }

        Cargo cargo = funcionario.getCargoGlobal() != null ? funcionario.getCargoGlobal() : Cargo.ENGENHEIRO;
        return cargoPermissaoService.permissoesPorCargo(cargo);
    }

    public FuncionarioPermissoesAcessoDto buscar(Long funcionarioId) {
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        return new FuncionarioPermissoesAcessoDto(
                funcionario.getId(),
                funcionario.getCargoGlobal(),
                nomes(permissoesDoFuncionario(funcionario))
        );
    }

    @Transactional
    public FuncionarioPermissoesAcessoDto atualizar(Long funcionarioId, List<String> permissoes) {
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        EnumSet<Permissao> permissoesValidadas = EnumSet.noneOf(Permissao.class);

        if (permissoes != null) {
            for (String permissao : permissoes) {
                try {
                    permissoesValidadas.add(Permissao.valueOf(permissao));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequest("Permissão inválida: " + permissao);
                }
            }
        }

        permissaoRepository.deleteByFuncionarioId(funcionarioId);
        permissaoRepository.flush();
        permissaoRepository.saveAll(permissoesValidadas.stream()
                .map(permissao -> new FuncionarioPermissao(funcionario, permissao))
                .toList());

        return new FuncionarioPermissoesAcessoDto(
                funcionario.getId(),
                funcionario.getCargoGlobal(),
                nomes(permissoesValidadas.stream().toList())
        );
    }

    private Funcionario buscarFuncionario(Long funcionarioId) {
        return funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private List<String> nomes(List<Permissao> permissoes) {
        return permissoes.stream().map(Enum::name).toList();
    }
}