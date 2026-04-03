package com.sptech.school.jaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sptech.school.jaf.dto.OcrResultDTO;
import org.filestack.Client;
import org.filestack.Config;
import org.filestack.FileLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OcrService {

    @Value("${filestack.api-key}")
    private String apiKey;

    @Value("${filestack.policy}")
    private String policy;

    @Value("${filestack.signature}")
    private String signature;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public OcrResultDTO processarNotaFiscal(MultipartFile arquivo)
            throws IOException, InterruptedException {

        File arquivoTemp = converterParaArquivoTemp(arquivo);

        try {

            String handle = uploadParaFilestack(arquivoTemp);

            String textoOcr = extrairTextoOcr(handle);

            if (textoOcr == null || textoOcr.isBlank()) {
                throw new IOException("OCR não retornou texto.");
            }

            return interpretarCamposNf(textoOcr);

        } finally {

            arquivoTemp.delete();

        }
    }


    private File converterParaArquivoTemp(MultipartFile arquivo)
            throws IOException {

        String nomeOriginal =
                arquivo.getOriginalFilename() != null
                        ? arquivo.getOriginalFilename()
                        : "nf-temp";

        File temp =
                File.createTempFile(
                        "jaf-nf-",
                        "-" + nomeOriginal
                );

        try (
                FileOutputStream fos =
                        new FileOutputStream(temp)
        ) {

            fos.write(
                    arquivo.getBytes()
            );

        }

        return temp;
    }


    private String uploadParaFilestack(File arquivo)
            throws IOException {

        Config config =
                new Config(apiKey);

        Client client =
                new Client(config);

        FileLink fileLink =
                client.upload(
                        arquivo.getAbsolutePath(),
                        false
                );

        return fileLink.getHandle();
    }


   private String extrairTextoOcr(String handle)
        throws IOException, InterruptedException {

    // codifica somente o JSON do policy
    String policyEncoded =
            java.net.URLEncoder.encode(
                    policy,
                    java.nio.charset.StandardCharsets.UTF_8
            );

    String url =
            "https://cdn.filestackcontent.com/security=p:"
                    + policyEncoded
                    + ",s:"
                    + signature
                    + "/ocr/"
                    + handle;

    HttpRequest request =
            HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

    HttpResponse<String> response =
            httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

    if (response.statusCode() != 200) {

        throw new IOException(
                "Erro OCR Filestack HTTP "
                        + response.statusCode()
                        + " body: "
                        + response.body()
        );

    }

    com.fasterxml.jackson.databind.JsonNode root =
            objectMapper.readTree(
                    response.body()
            );

    return root
            .path("text")
            .asText("");
}


    private OcrResultDTO interpretarCamposNf(String textoOcr)
            throws IOException, InterruptedException {

        String prompt =
                """
                Você receberá o texto extraído por OCR de uma Nota Fiscal brasileira.
                Extraia exatamente os campos abaixo e retorne SOMENTE um JSON válido.
                Se não encontrar algum campo, use null.

                {
                  "cnpjEmitente": "...",
                  "dataEmissao": "...",
                  "numeroNf": "...",
                  "valorTotal": "...",
                  "destinatario": "...",
                  "descricaoItens": "..."
                }

                Texto:
                """
                        + textoOcr;

        String requestBody =
                objectMapper.writeValueAsString(
                        new java.util.LinkedHashMap<>() {{
                            put("model", "claude-sonnet-4-20250514");
                            put("max_tokens", 1024);
                            put(
                                    "messages",
                                    java.util.List.of(
                                            java.util.Map.of(
                                                    "role",
                                                    "user",
                                                    "content",
                                                    prompt
                                            )
                                    )
                            );
                        }}
                );

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        "https://api.anthropic.com/v1/messages"
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "x-api-key",
                                anthropicApiKey
                        )
                        .header(
                                "anthropic-version",
                                "2023-06-01"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Erro ao chamar API Anthropic. HTTP Status: "
                            + response.statusCode()
                            + " body: "
                            + response.body()
            );

        }

        JsonNode responseNode =
                objectMapper.readTree(
                        response.body()
                );

        String jsonExtraido =
                responseNode
                        .path("content")
                        .get(0)
                        .path("text")
                        .asText("");

        if (jsonExtraido.isBlank()) {

            throw new IOException(
                    "Claude não retornou JSON válido."
            );

        }

        JsonNode camposNf =
                objectMapper.readTree(
                        jsonExtraido
                );

        OcrResultDTO resultado =
                new OcrResultDTO();

        resultado.setCnpjEmitente(
                camposNf.path("cnpjEmitente").asText(null)
        );

        resultado.setDataEmissao(
                camposNf.path("dataEmissao").asText(null)
        );

        resultado.setNumeroNf(
                camposNf.path("numeroNf").asText(null)
        );

        resultado.setValorTotal(
                camposNf.path("valorTotal").asText(null)
        );

        resultado.setDestinatario(
                camposNf.path("destinatario").asText(null)
        );

        resultado.setDescricaoItens(
                camposNf.path("descricaoItens").asText(null)
        );

        resultado.setTextoOriginalOcr(
                textoOcr
        );

        return resultado;
    }

}