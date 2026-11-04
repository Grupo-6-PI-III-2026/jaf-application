package com.jaf.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Nomes e argumentos precisam ser idênticos aos de POC-API/app/infrastructure/messaging/topology.py:
// o RabbitMQ recusa (PRECONDITION_FAILED) redeclarar uma fila existente com argumentos diferentes.
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ocr.exchange";
    public static final String DLX = "ocr.dlx";
    public static final String FILA_SOLICITACOES = "ocr.solicitacao";
    // Filas de resultado v2: as originais não aceitam argumentos novos (PRECONDITION_FAILED).
    public static final String FILA_RESULTADOS = "ocr.resultado.v2";
    public static final String FILA_RESULTADOS_DLQ = "ocr.resultado.dlq";
    // Fila antiga: continua sendo consumida até esvaziar as mensagens publicadas antes da troca.
    public static final String FILA_RESULTADOS_LEGADA = "ocr.resultado";
    public static final String FILA_DLQ = "ocr.solicitacao.dlq";
    public static final String RK_SOLICITAR = "ocr.solicitar";
    public static final String RK_RESULTADO = "ocr.resultado.v2";
    public static final String RK_RESULTADO_LEGADO = "ocr.resultado";
    public static final String RK_RESULTADO_MORTO = "ocr.resultado.dead";

    @Bean
    public DirectExchange ocrExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange ocrDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue filaSolicitacoes() {
        return QueueBuilder.durable(FILA_SOLICITACOES)
                .deadLetterExchange(DLX)
                .build();
    }

    @Bean
    public Queue filaResultados() {
        return QueueBuilder.durable(FILA_RESULTADOS)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(RK_RESULTADO_MORTO)
                .build();
    }

    @Bean
    public Queue filaResultadosDlq() {
        return QueueBuilder.durable(FILA_RESULTADOS_DLQ).build();
    }

    @Bean
    public Queue filaResultadosLegada() {
        return QueueBuilder.durable(FILA_RESULTADOS_LEGADA).build();
    }

    @Bean
    public Queue filaDlq() {
        return QueueBuilder.durable(FILA_DLQ).build();
    }

    @Bean
    public Binding bindingSolicitacoes() {
        return BindingBuilder.bind(filaSolicitacoes()).to(ocrExchange()).with(RK_SOLICITAR);
    }

    @Bean
    public Binding bindingResultados() {
        return BindingBuilder.bind(filaResultados()).to(ocrExchange()).with(RK_RESULTADO);
    }

    @Bean
    public Binding bindingResultadosLegada() {
        return BindingBuilder.bind(filaResultadosLegada()).to(ocrExchange()).with(RK_RESULTADO_LEGADO);
    }

    @Bean
    public Binding bindingResultadosDlq() {
        return BindingBuilder.bind(filaResultadosDlq()).to(ocrDlx()).with(RK_RESULTADO_MORTO);
    }

    // Sem x-dead-letter-routing-key, a mensagem rejeitada mantém a routing key original (ocr.solicitar).
    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(filaDlq()).to(ocrDlx()).with(RK_SOLICITAR);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
