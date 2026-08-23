package com.jaf.application.controller;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioListarDto;
import com.jaf.application.dto.FuncionarioLoginDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.dto.AtualizarCargoPermissoesDto;
import com.jaf.application.dto.FuncionarioPermissoesAcessoDto;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.dto.FuncionarioTokenDto;
import com.jaf.application.service.FuncionarioPermissaoService;
import com.jaf.application.service.FuncionarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@SecurityRequirement(name = "Bearer")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final FuncionarioPermissaoService funcionarioPermissaoService;
    private final AuthenticationManager authenticationManager;
    private final GerenciadorTokenJwt gerenciadorTokenJwt;

    public FuncionarioController(
            FuncionarioService funcionarioService,
            FuncionarioPermissaoService funcionarioPermissaoService,
            AuthenticationManager authenticationManager,
            GerenciadorTokenJwt gerenciadorTokenJwt) {
        this.funcionarioService = funcionarioService;
        this.funcionarioPermissaoService = funcionarioPermissaoService;
        this.authenticationManager = authenticationManager;
        this.gerenciadorTokenJwt = gerenciadorTokenJwt;
    }

    @PostMapping("/login")
    public ResponseEntity<FuncionarioTokenDto> login(@Valid @RequestBody FuncionarioLoginDto loginDto, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getSenha())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = gerenciadorTokenJwt.generateToken(authentication);
        
        // CORREÇÃO DE SEGURANÇA A07: Envio de token via cookie HttpOnly em vez de no corpo da resposta
        // Antes: return ResponseEntity.ok(new FuncionarioTokenDto(loginDto.getEmail(), token));
        // Agora: Token enviado via cookie com flags de segurança
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);      // Não acessível via JavaScript (proteção contra XSS)
        // Secure deve ser true em produção (HTTPS), false em desenvolvimento (HTTP)
        jwtCookie.setSecure(false);       // Ajustado para desenvolvimento (localhost sem HTTPS)
        jwtCookie.setPath("/");            // Disponível em todo o domínio
        jwtCookie.setMaxAge(3600);         // 1 hora de validade (mesma configuração do JWT)
        response.addCookie(jwtCookie);
        
        // Busca informações do funcionário para retornar no DTO (sem o token)
        var funcionario = funcionarioService.buscarPorEmail(loginDto.getEmail());
        FuncionarioTokenDto dto = new FuncionarioTokenDto();
        dto.setEmail(funcionario.getEmail());
        dto.setNome(funcionario.getNome());
        dto.setId(funcionario.getId());
        dto.setCargo(funcionario.getCargoGlobal());
        dto.setToken(null); // Token está no cookie, não no corpo da resposta
        
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // CORREÇÃO DE SEGURANÇA A07: Limpa o cookie HttpOnly no logout
        Cookie jwtCookie = new Cookie("jwt", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);       // Ajustado para desenvolvimento (localhost sem HTTPS)
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Remove o cookie imediatamente
        response.addCookie(jwtCookie);
        
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRIAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioResponseDto> criar(@Valid @RequestBody FuncionarioDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criar(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VISUALIZAR_FUNCIONARIOS', 'CRIAR_ALOCACAO')")
    public ResponseEntity<List<FuncionarioListarDto>> listar() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_FUNCIONARIOS')")
    public ResponseEntity<FuncionarioResponseDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioResponseDto> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDto dto) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_FUNCIONARIO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        funcionarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cargo")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioResponseDto> atualizarCargo(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String novoCargo = body.get("cargo");
        return ResponseEntity.ok(funcionarioService.atualizarCargo(id, novoCargo));
    }

    @GetMapping("/{id}/permissoes")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioPermissoesAcessoDto> buscarPermissoes(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioPermissaoService.buscar(id));
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioPermissoesAcessoDto> atualizarPermissoes(
            @PathVariable Long id,
            @RequestBody AtualizarCargoPermissoesDto dto) {
        return ResponseEntity.ok(funcionarioPermissaoService.atualizar(id, dto.getPermissoes()));
    }
}
