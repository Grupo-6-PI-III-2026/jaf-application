package com.jaf.application.service;

import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AcessoObraService {
    private final FuncionarioRepository funcionarios;
    private final AlocacaoObraRepository alocacoes;

    public AcessoObraService(FuncionarioRepository funcionarios, AlocacaoObraRepository alocacoes) {
        this.funcionarios = funcionarios;
        this.alocacoes = alocacoes;
    }

    public Funcionario atual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new Forbidden("Autenticação obrigatória.");
        return funcionarios.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new Forbidden("Usuário autenticado não encontrado."));
    }

    public boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && funcionarios.findByEmailIgnoreCase(auth.getName())
                .map(f -> f.getCargoGlobal() == Cargo.ADMIN).orElse(false);
    }

    public void exigirAdmin() {
        if (!isAdmin()) throw new Forbidden("Operação restrita a ADMIN.");
    }

    public void exigirCriacaoFuncionario(Cargo cargo) {
        if (cargo == Cargo.ADMIN) exigirAdmin();
    }

    public void exigirEdicaoFuncionario(Funcionario funcionario, Cargo novoCargo) {
        boolean alteraCargo = funcionario.getCargoGlobal() != novoCargo;
        if (funcionario.getCargoGlobal() == Cargo.ADMIN || alteraCargo) {
            exigirAdmin();
        }
        if (alteraCargo) exigirGestaoDeAcessoDeOutro(funcionario);
    }

    public void exigirExclusaoFuncionario(Funcionario funcionario) {
        if (funcionario.getCargoGlobal() == Cargo.ADMIN) exigirAdmin();
    }

    public void exigirGestaoDeAcessoDeOutro(Funcionario alvo) {
        exigirOutroFuncionario(alvo, "Não é permitido alterar o próprio cargo ou as próprias permissões.");
    }

    public void exigirExclusaoDeOutro(Funcionario alvo) {
        exigirOutroFuncionario(alvo, "Para excluir a própria conta, use a opção do seu perfil.");
    }

    private void exigirOutroFuncionario(Funcionario alvo, String mensagem) {
        if (atual().getId().equals(alvo.getId())) throw new Forbidden(mensagem);
    }

    public void exigirObra(Long obraId) {
        Funcionario usuario = atual();
        if (usuario.getCargoGlobal() != Cargo.ADMIN
                && !alocacoes.existsByFuncionarioIdAndObraId(usuario.getId(), obraId)) {
            throw new Forbidden("Usuário não possui acesso a esta obra.");
        }
    }

    public void exigirRegistroReembolso(Long obraId) {
        Cargo cargo = atual().getCargoGlobal();
        if (cargo != Cargo.ADMIN && cargo != Cargo.RESPONSAVEL_ADMINISTRATIVO) {
            throw new Forbidden("Somente ADMIN e RESPONSAVEL_ADMINISTRATIVO podem registrar reembolsos.");
        }
        exigirObra(obraId);
    }
}