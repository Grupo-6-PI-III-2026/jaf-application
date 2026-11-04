package com.jaf.application.service;

import com.jaf.application.dto.AtualizarAcessoDto;
import com.jaf.application.dto.FuncionarioPermissoesAcessoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.Permissao;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.exceptions.StaleVersion;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.FuncionarioPermissao;
import com.jaf.application.repository.FuncionarioPermissaoRepository;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

@Service
public class FuncionarioPermissaoService {
    private final FuncionarioPermissaoRepository permissaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CargoPermissaoService cargoPermissaoService;
    private final AcessoObraService acesso;

    public FuncionarioPermissaoService(
            FuncionarioPermissaoRepository permissaoRepository,
            FuncionarioRepository funcionarioRepository,
            CargoPermissaoService cargoPermissaoService,
            AcessoObraService acesso) {
        this.permissaoRepository = permissaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.cargoPermissaoService = cargoPermissaoService;
        this.acesso = acesso;
    }

    public List<Permissao> permissoesDoFuncionario(Funcionario funcionario) {
        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return cargoPermissaoService.permissoesPorCargo(Cargo.ADMIN);
        }

        List<FuncionarioPermissao> permissoesCustomizadas = permissaoRepository.findByFuncionarioId(funcionario.getId());
        if (funcionario.isPermissoesPersonalizadas()) {
            return permissoesCustomizadas.stream()
                    .map(FuncionarioPermissao::getPermissao)
                    .distinct()
                    .toList();
        }

        return cargoPermissaoService.permissoesPorCargo(funcionario.getCargoGlobal());
    }

    public FuncionarioPermissoesAcessoDto buscar(Long funcionarioId) {
        acesso.exigirAdmin();
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        return new FuncionarioPermissoesAcessoDto(
                funcionario.getId(),
                funcionario.getCargoGlobal(),
                nomes(permissoesDoFuncionario(funcionario)), funcionario.isPermissoesPersonalizadas()
        );
    }

    public FuncionarioPermissoesAcessoDto minhasPermissoes() {
        Funcionario funcionario = acesso.atual();
        return new FuncionarioPermissoesAcessoDto(
                funcionario.getId(), funcionario.getCargoGlobal(), nomes(permissoesDoFuncionario(funcionario)), funcionario.isPermissoesPersonalizadas());
    }

    @Transactional
    public FuncionarioPermissoesAcessoDto atualizar(Long funcionarioId, List<String> permissoes) {
        acesso.exigirAdmin();
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        acesso.exigirGestaoDeAcessoDeOutro(funcionario);
        return aplicarPermissoes(funcionario, permissoes);
    }

    /** Muda cargo e permissões na mesma transação: ou tudo vale, ou nada muda. */
    @Transactional
    public FuncionarioPermissoesAcessoDto atualizarAcesso(Long funcionarioId, AtualizarAcessoDto dto) {
        acesso.exigirAdmin();
        Funcionario funcionario = buscarFuncionario(funcionarioId);
        acesso.exigirGestaoDeAcessoDeOutro(funcionario);
        if (dto.getVersao() != null && !dto.getVersao().equals(funcionario.getVersao())) {
            throw new StaleVersion();
        }
        Cargo cargoAnterior = funcionario.getCargoGlobal();
        if (dto.getCargo() != cargoAnterior) {
            acesso.exigirAlteracaoDeCargo(funcionario);
            funcionario.setCargoGlobal(dto.getCargo());
            AuditLog.registrar("CARGO_ALTERADO", "Funcionario", funcionarioId, "cargo=" + cargoAnterior, "cargo=" + dto.getCargo());
        }
        return aplicarPermissoes(funcionario, dto.getPermissoes());
    }

    private FuncionarioPermissoesAcessoDto aplicarPermissoes(Funcionario funcionario, List<String> permissoes) {
        Long funcionarioId = funcionario.getId();
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

        String permissoesAntes = String.join(",", nomes(permissoesDoFuncionario(funcionario)));
        EnumSet<Permissao> padrao = EnumSet.noneOf(Permissao.class);
        padrao.addAll(cargoPermissaoService.permissoesPorCargo(funcionario.getCargoGlobal()));
        boolean igualAoPadrao = padrao.equals(permissoesValidadas);
        funcionario.setPermissoesPersonalizadas(!igualAoPadrao);
        funcionario.setTokenVersion(funcionario.getTokenVersion() + 1);
        permissaoRepository.deleteByFuncionarioId(funcionarioId);
        permissaoRepository.flush();
        permissaoRepository.saveAll(permissoesValidadas.stream()
                .map(permissao -> new FuncionarioPermissao(funcionario, permissao))
                .toList());
        AuditLog.registrar("PERMISSOES_ALTERADAS", "Funcionario", funcionarioId, "permissoes=" + permissoesAntes,
                "permissoes=" + permissoesValidadas.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")));

        return new FuncionarioPermissoesAcessoDto(
                funcionario.getId(),
                funcionario.getCargoGlobal(),
                nomes(permissoesValidadas.stream().toList()), funcionario.isPermissoesPersonalizadas()
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