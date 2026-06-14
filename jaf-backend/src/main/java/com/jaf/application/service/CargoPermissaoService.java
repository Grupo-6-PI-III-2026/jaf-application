package com.jaf.application.service;

import com.jaf.application.dto.CargoPermissoesDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.Permissao;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.model.CargoPermissao;
import com.jaf.application.repository.CargoPermissaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@Service
public class CargoPermissaoService {
    private static final List<Cargo> CARGOS_CONFIGURAVEIS = List.of(
            Cargo.RESPONSAVEL_ADMINISTRATIVO,
            Cargo.ENGENHEIRO
    );

    private final CargoPermissaoRepository repository;

    public CargoPermissaoService(CargoPermissaoRepository repository) {
        this.repository = repository;
    }

    public List<CargoPermissoesDto> listarCargosSistema() {
        return List.of(
                new CargoPermissoesDto(Cargo.ADMIN, nomes(Permissao.values())),
                new CargoPermissoesDto(Cargo.RESPONSAVEL_ADMINISTRATIVO, nomes(permissoesPorCargo(Cargo.RESPONSAVEL_ADMINISTRATIVO))),
                new CargoPermissoesDto(Cargo.ENGENHEIRO, nomes(permissoesPorCargo(Cargo.ENGENHEIRO)))
        );
    }

    public List<Permissao> permissoesPorCargo(Cargo cargo) {
        if (cargo == null) {
            return Cargo.ENGENHEIRO.getPermissoes();
        }

        if (cargo == Cargo.ADMIN) {
            return Arrays.asList(Permissao.values());
        }

        List<CargoPermissao> configuradas = repository.findByCargo(cargo);
        if (configuradas.isEmpty()) {
            return cargo.getPermissoes();
        }

        return configuradas.stream()
                .map(CargoPermissao::getPermissao)
                .distinct()
                .toList();
    }

    @Transactional
    public CargoPermissoesDto atualizar(Cargo cargo, List<String> permissoes) {
        if (!CARGOS_CONFIGURAVEIS.contains(cargo)) {
            throw new BadRequest("Apenas perfis não administradores podem ter permissões customizadas.");
        }

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

        repository.deleteByCargo(cargo);
        repository.flush();
        repository.saveAll(permissoesValidadas.stream()
                .map(permissao -> new CargoPermissao(cargo, permissao))
                .toList());

        return new CargoPermissoesDto(cargo, nomes(permissoesValidadas.stream().toList()));
    }

    private List<String> nomes(Permissao[] permissoes) {
        return Arrays.stream(permissoes).map(Enum::name).toList();
    }

    private List<String> nomes(List<Permissao> permissoes) {
        return permissoes.stream().map(Enum::name).toList();
    }
}