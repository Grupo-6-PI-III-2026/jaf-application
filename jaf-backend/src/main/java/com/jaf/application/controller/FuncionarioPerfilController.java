package com.jaf.application.controller;

import com.jaf.application.dto.AlterarSenhaDto;
import com.jaf.application.dto.FuncionarioPerfilUpdateDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.dto.ExcluirContaDto;
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
import java.io.InputStream;
import java.util.Set;

@RestController
@RequestMapping("/funcionarios/me")
@SecurityRequirement(name = "Bearer")
public class FuncionarioPerfilController {

    private final FuncionarioService funcionarioService;

    private static final Path UPLOAD_DIR = Paths.get("uploads", "fotos");
        private static final long FOTO_MAX_BYTES = 5L * 1024L * 1024L;
        private static final Set<String> TIPOS_IMAGEM_ACEITOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
        );

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
        if (file.getSize() > FOTO_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto deve ter no maximo 5 MB.");
        }
        String ext = detectarExtensao(file);
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem JPG, PNG, WEBP ou GIF.");
        }
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
    public ResponseEntity<Void> excluirConta(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody @Valid ExcluirContaDto dto) {
        funcionarioService.excluirConta(userDetails.getUsername(), dto.senhaAtual());
        return ResponseEntity.noContent().build();
    }

    private String detectarExtensao(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(header);
            if (read >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff) return "jpg";
            if (read >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N'
                    && header[3] == 'G' && (header[4] & 0xff) == 0x0d && (header[5] & 0xff) == 0x0a
                    && (header[6] & 0xff) == 0x1a && (header[7] & 0xff) == 0x0a) return "png";
            if (read >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                    && header[3] == '8') return "gif";
            if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                    && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                    && header[10] == 'B' && header[11] == 'P') return "webp";
            return null;
        }
    }

    public record FotoUploadResponse(String url) {}
}
