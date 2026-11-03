package com.jaf.application.controller;

import com.jaf.application.dto.NotaFiscalResponse;
import com.jaf.application.model.NotaFiscal;
import com.jaf.application.service.NotaFiscalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@SecurityRequirement(name = "Bearer")
public class NotaFiscalController {
    private final NotaFiscalService service;
    public NotaFiscalController(NotaFiscalService service) { this.service = service; }

    @PostMapping(value = "/gastos/{gastoId}/notas-fiscais", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CRIAR_GASTO','EDITAR_GASTO')")
    public ResponseEntity<NotaFiscalResponse> anexar(@PathVariable Long gastoId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cnpjEmitente,
            @RequestParam(required = false) BigDecimal valor,
            @RequestParam(required = false) LocalDate dataEmissao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.anexar(gastoId, arquivo, numero, cnpjEmitente, valor, dataEmissao));
    }

    @GetMapping("/gastos/{gastoId}/notas-fiscais")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public List<NotaFiscalResponse> listar(@PathVariable Long gastoId) { return service.listar(gastoId); }

    @GetMapping("/notas-fiscais/{id}/arquivo")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<InputStreamResource> arquivo(@PathVariable Long id) {
        NotaFiscal nota = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(nota.getContentType()))
                .contentLength(nota.getTamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nota.getArquivoNome()).build().toString())
                .body(new InputStreamResource(service.abrirArquivo(nota)));
    }

    @DeleteMapping("/notas-fiscais/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
