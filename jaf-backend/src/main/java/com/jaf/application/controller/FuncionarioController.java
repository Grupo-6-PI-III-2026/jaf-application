package com.jaf.application.controller;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.config.CookieProperties;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioUpdateRequest;
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
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.jaf.application.config.ClientIpResolver;
import com.jaf.application.exceptions.TooManyRequests;
import com.jaf.application.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@SecurityRequirement(name = "cookieAuth")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final FuncionarioPermissaoService funcionarioPermissaoService;
    private final AuthenticationManager authenticationManager;
    private final GerenciadorTokenJwt gerenciadorTokenJwt;
    private final CookieProperties cookieProperties;
    private final LoginAttemptService loginAttempts;
    private final ClientIpResolver clientIpResolver;

    public FuncionarioController(
            FuncionarioService funcionarioService,
            FuncionarioPermissaoService funcionarioPermissaoService,
            AuthenticationManager authenticationManager,
            GerenciadorTokenJwt gerenciadorTokenJwt,
            CookieProperties cookieProperties,
            LoginAttemptService loginAttempts,
            ClientIpResolver clientIpResolver) {
        this.funcionarioService = funcionarioService;
        this.funcionarioPermissaoService = funcionarioPermissaoService;
        this.authenticationManager = authenticationManager;
        this.gerenciadorTokenJwt = gerenciadorTokenJwt;
        this.cookieProperties = cookieProperties;
        this.loginAttempts = loginAttempts;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<FuncionarioTokenDto> login(@Valid @RequestBody FuncionarioLoginDto loginDto,
                                                     HttpServletResponse response, HttpServletRequest request) {
        String email = loginDto.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        String ip = clientIpResolver.resolve(request);
        if (loginAttempts.bloqueado(ip, email)) {
            throw new TooManyRequests("Muitas tentativas. Aguarde alguns minutos e tente novamente.");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginDto.getSenha()));
            loginAttempts.limpar(ip, email);
        } catch (BadCredentialsException ex) {
            loginAttempts.registrarFalha(ip, email);
            throw ex;
        }
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = gerenciadorTokenJwt.generateToken(authentication);
        
        // CORREÇÃO DE SEGURANÇA A07: Envio de token via cookie HttpOnly em vez de no corpo da resposta
        // Antes: return ResponseEntity.ok(new FuncionarioTokenDto(loginDto.getEmail(), token));
        // Agora: Token enviado via cookie com flags de segurança
        response.addHeader("Set-Cookie", construirCookie(token).toString());
        
        // Busca informações do funcionário para retornar no DTO (sem o token)
        var funcionario = funcionarioService.buscarPorEmail(loginDto.getEmail());
        FuncionarioTokenDto dto = new FuncionarioTokenDto();
        dto.setEmail(funcionario.getEmail());
        dto.setNome(funcionario.getNome());
        dto.setId(funcionario.getId());
        dto.setCargo(funcionario.getCargoGlobal());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // CORREÇÃO DE SEGURANÇA A07: Limpa o cookie HttpOnly no logout
        response.addHeader("Set-Cookie", construirCookie("", java.time.Duration.ZERO).toString());
        
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie construirCookie(String token) {
        return construirCookie(token, cookieProperties.maxAge());
    }

    private ResponseCookie construirCookie(String token, java.time.Duration maxAge) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .sameSite(cookieProperties.sameSite())
                .maxAge(maxAge)
                .build();
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
    public ResponseEntity<FuncionarioResponseDto> atualizar(@PathVariable Long id,
                                                             @Valid @RequestBody FuncionarioUpdateRequest dto) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_FUNCIONARIO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        funcionarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioResponseDto> inativar(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.alterarStatus(id, false));
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<FuncionarioResponseDto> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.alterarStatus(id, true));
    }

    @PatchMapping("/{id}/cargo")
    @PreAuthorize("@acessoObraService.isAdmin()")
    public ResponseEntity<FuncionarioResponseDto> atualizarCargo(
            @PathVariable Long id,
            @Valid @RequestBody com.jaf.application.dto.AtualizarCargoDto body) {
        return ResponseEntity.ok(funcionarioService.atualizarCargo(id, body.cargo().name()));
    }

    @GetMapping("/{id}/permissoes")
    @PreAuthorize("@acessoObraService.isAdmin()")
    public ResponseEntity<FuncionarioPermissoesAcessoDto> buscarPermissoes(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioPermissaoService.buscar(id));
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("@acessoObraService.isAdmin()")
    public ResponseEntity<FuncionarioPermissoesAcessoDto> atualizarPermissoes(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarCargoPermissoesDto dto) {
        return ResponseEntity.ok(funcionarioPermissaoService.atualizar(id, dto.getPermissoes()));
    }

    @GetMapping("/me/permissoes")
    public ResponseEntity<FuncionarioPermissoesAcessoDto> minhasPermissoes() {
        return ResponseEntity.ok(funcionarioPermissaoService.minhasPermissoes());
    }
}
