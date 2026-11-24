package com.jaf.application.controller;

import com.jaf.application.dto.AlterarSenhaDto;
import com.jaf.application.dto.FuncionarioPerfilUpdateDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.service.FuncionarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/funcionarios/me")
@SecurityRequirement(name = "Bearer")
public class FuncionarioPerfilController {

    private final FuncionarioService funcionarioService;

    public FuncionarioPerfilController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public ResponseEntity<FuncionarioResponseDto> meuPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        Funcionario funcionario = funcionarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(funcionarioService.paraResposta(funcionario));
    }

    @PutMapping
    public ResponseEntity<FuncionarioResponseDto> atualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid FuncionarioPerfilUpdateDto dto) {
        return ResponseEntity.ok(funcionarioService.atualizarPerfil(userDetails.getUsername(), dto));
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoUploadResponse> uploadFoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(new FotoUploadResponse(
                funcionarioService.atualizarFoto(userDetails.getUsername(), file)));
    }

    @PatchMapping("/senha")
    public ResponseEntity<Void> alterarSenha(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AlterarSenhaDto dto) {
        funcionarioService.alterarSenha(userDetails.getUsername(), dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> excluirConta(@AuthenticationPrincipal UserDetails userDetails) {
        funcionarioService.excluirConta(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    public record FotoUploadResponse(String url) {}
}
