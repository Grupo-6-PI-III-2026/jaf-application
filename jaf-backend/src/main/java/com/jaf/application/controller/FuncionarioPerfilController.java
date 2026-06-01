package com.jaf.application.controller;

import com.jaf.application.dto.AlterarSenhaDto;
import com.jaf.application.dto.FuncionarioPerfilUpdateDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.service.FuncionarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/funcionarios/me")
@SecurityRequirement(name = "Bearer")
public class FuncionarioPerfilController {

    private final FuncionarioService funcionarioService;

    private static final Path UPLOAD_DIR = Paths.get("uploads", "fotos");

    public FuncionarioPerfilController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public ResponseEntity<FuncionarioResponseDto> meuPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        Funcionario funcionario = funcionarioService.buscarPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(new FuncionarioResponseDto(funcionario));
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
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo invalido.");
        }

        String ext = getExtensao(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + "." + ext;

        Path uploadDir = UPLOAD_DIR.toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path destino = uploadDir.resolve(filename);

        file.transferTo(destino.toFile());

        String url = "/uploads/fotos/" + filename;
        return ResponseEntity.ok(new FotoUploadResponse(url));
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

    private String getExtensao(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public record FotoUploadResponse(String url) {}
}
