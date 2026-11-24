package com.jaf.application.armazenamento;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

/** Bucket privado; as credenciais vêm da role da instância EC2 (cadeia padrão do SDK). */
@Component
@ConditionalOnProperty(name = "app.armazenamento.tipo", havingValue = "s3")
public class ArmazenamentoS3 implements ArmazenamentoArquivos, DisposableBean {
    private final S3Client cliente;
    private final S3Presigner presigner;
    private final String bucket;

    public ArmazenamentoS3(@Value("${app.armazenamento.s3.bucket:}") String bucket,
                           @Value("${app.armazenamento.s3.regiao:us-east-1}") String regiao,
                           @Value("${app.armazenamento.s3.endpoint:}") String endpoint) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("Defina ARMAZENAMENTO_S3_BUCKET para usar o armazenamento S3.");
        }
        this.bucket = bucket;
        Region region = Region.of(regiao);
        S3ClientBuilder clienteBuilder = S3Client.builder().region(region);
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(region);
        if (!endpoint.isBlank()) {
            // Serviços compatíveis com S3 (MinIO, R2) costumam exigir path-style.
            S3Configuration configuracao = S3Configuration.builder().pathStyleAccessEnabled(true).build();
            clienteBuilder.endpointOverride(URI.create(endpoint)).serviceConfiguration(configuracao);
            presignerBuilder.endpointOverride(URI.create(endpoint)).serviceConfiguration(configuracao);
        }
        this.cliente = clienteBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public void salvar(String chave, byte[] conteudo, String contentType) {
        cliente.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(chave)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(conteudo));
    }

    @Override
    public String gerarUrl(String chave, Duration validade) {
        GetObjectRequest leitura = GetObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .responseContentDisposition("inline")
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(validade)
                        .getObjectRequest(leitura)
                        .build())
                .url()
                .toString();
    }

    @Override
    public void remover(String chave) {
        cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
    }

    @Override
    public void destroy() {
        cliente.close();
        presigner.close();
    }
}
