package com.jaf.application.service;

import com.jaf.application.dto.NotaFiscalResponse;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.NotaFiscal;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.NotaFiscalRepository;
import com.jaf.application.storage.ArquivoStorage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotaFiscalService {
    private static final Logger log = LoggerFactory.getLogger(NotaFiscalService.class);
    private static final long LIMITE_BYTES = 10L * 1024 * 1024;
    private final NotaFiscalRepository notas;
    private final GastoRepository gastos;
    private final FuncionarioRepository funcionarios;
    private final ArquivoStorage storage;
    private final AcessoObraService acesso;

    public NotaFiscalService(NotaFiscalRepository notas, GastoRepository gastos,
                             FuncionarioRepository funcionarios, ArquivoStorage storage,
                             AcessoObraService acesso) {
        this.notas = notas;
        this.gastos = gastos;
        this.funcionarios = funcionarios;
        this.storage = storage;
        this.acesso = acesso;
    }

    @Transactional
    public NotaFiscalResponse anexar(Long gastoId, MultipartFile arquivo, String numero,
                                     String cnpj, java.math.BigDecimal valor,
                                     java.time.LocalDate dataEmissao) {
        if (arquivo == null || arquivo.isEmpty()) throw new BadRequest("Arquivo inválido.");
        if (arquivo.getSize() > LIMITE_BYTES) throw new BadRequest("O arquivo deve ter no máximo 10 MB.");
        TipoArquivo tipo = detectarTipo(arquivo);
        if (tipo == null) throw new BadRequest("Envie um arquivo PDF, JPG ou PNG.");
        String cnpjNormalizado = normalizarCnpj(cnpj);
        if (cnpjNormalizado != null && !cnpjValido(cnpjNormalizado)) {
            throw new BadRequest("CNPJ do emitente inválido.");
        }
        Gasto gasto = gastos.findById(gastoId)
                .orElseThrow(() -> new NotFoundException("Gasto não encontrado."));
        acesso.exigirObra(gasto.getObra().getId());
        Funcionario criador = funcionarios.findById(acesso.atual().getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException ex) {
            throw new BadRequest("Não foi possível ler o arquivo.");
        }
        String sha256 = sha256(conteudo);
        if (notas.existsByGastoIdAndSha256(gastoId, sha256)) {
            throw new Conflict("Este arquivo já está anexado ao gasto.");
        }
        String chave = "notas/" + gasto.getObra().getId() + "/" + gastoId + "/" + sha256 + "." + tipo.extensao;
        NotaFiscal nota = new NotaFiscal();
        nota.setGasto(gasto);
        nota.setNumero(trim(numero));
        nota.setCnpjEmitente(cnpjNormalizado);
        nota.setValor(valor);
        nota.setDataEmissao(dataEmissao);
        nota.setArquivoChave(chave);
        nota.setArquivoNome(nomeSeguro(arquivo.getOriginalFilename(), tipo.extensao));
        nota.setContentType(tipo.contentType);
        nota.setTamanhoBytes(conteudo.length);
        nota.setSha256(sha256);
        nota.setCriadoPor(criador);
        try {
            storage.salvar(chave, new ByteArrayInputStream(conteudo));
            return NotaFiscalResponse.from(notas.save(nota));
        } catch (IOException ex) {
            throw new BadRequest("Não foi possível armazenar o arquivo.");
        } catch (DataIntegrityViolationException ex) {
            removerSilencioso(chave);
            throw new Conflict("Nota fiscal duplicada ou inválida.");
        }
    }

    @Transactional(readOnly = true)
    public List<NotaFiscalResponse> listar(Long gastoId) {
        Gasto gasto = gasto(gastoId);
        acesso.exigirObra(gasto.getObra().getId());
        return notas.findByGastoIdOrderByIdAsc(gastoId).stream().map(NotaFiscalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotaFiscal download(Long id) {
        NotaFiscal nota = notas.findById(id).orElseThrow(() -> new NotFoundException("Nota fiscal não encontrada."));
        acesso.exigirObra(nota.getGasto().getObra().getId());
        return nota;
    }

    @Transactional
    public void deletar(Long id) {
        NotaFiscal nota = notas.findById(id).orElseThrow(() -> new NotFoundException("Nota fiscal não encontrada."));
        acesso.exigirAdmin();
        notas.delete(nota);
        removerSilencioso(nota.getArquivoChave());
    }

    public InputStream abrirArquivo(NotaFiscal nota) {
        try {
            return storage.abrir(nota.getArquivoChave());
        } catch (NoSuchFileException ex) {
            throw new NotFoundException("Arquivo da nota fiscal não encontrado.");
        } catch (IOException ex) {
            throw new BadRequest("Não foi possível ler o arquivo.");
        }
    }

    private Gasto gasto(Long id) {
        return gastos.findById(id).orElseThrow(() -> new NotFoundException("Gasto não encontrado."));
    }
    private void removerSilencioso(String chave) {
        try {
            storage.remover(chave);
        } catch (IOException ex) {
            log.warn("Não foi possível remover o arquivo de nota fiscal {}", chave, ex);
        }
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String nomeSeguro(String nome, String ext) {
        String base = nome == null ? "nota-fiscal" : PathsCompat.fileName(nome);
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
    private static String normalizarCnpj(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("\\D", "");
    }
    private static boolean cnpjValido(String cnpj) {
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) return false;
        int[] pesos1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] pesos2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        return digito(cnpj, pesos1) == cnpj.charAt(12) - '0'
                && digito(cnpj, pesos2) == cnpj.charAt(13) - '0';
    }
    private static int digito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (cnpj.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível.", ex); }
    }
    private static TipoArquivo detectarTipo(MultipartFile arquivo) {
        try (InputStream input = arquivo.getInputStream()) {
            byte[] h = input.readNBytes(12);
            if (h.length >= 5 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F' && h[4] == '-') return new TipoArquivo("pdf", "application/pdf");
            if (h.length >= 3 && (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255) return new TipoArquivo("jpg", "image/jpeg");
            if (h.length >= 8 && (h[0] & 255) == 137 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return new TipoArquivo("png", "image/png");
            return null;
        } catch (IOException ex) { throw new BadRequest("Não foi possível validar o arquivo."); }
    }
    private record TipoArquivo(String extensao, String contentType) {}
    private static final class PathsCompat {
        private static String fileName(String value) {
            String normalized = value.replace('\\', '/');
            int slash = normalized.lastIndexOf('/');
            return slash >= 0 ? normalized.substring(slash + 1) : normalized;
        }
    }
}
