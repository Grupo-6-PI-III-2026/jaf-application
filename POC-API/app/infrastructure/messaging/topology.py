# É o que defini o mapa do "correio"
# onde cada "carta" deve ser entregue.
# Ou seja, define será feito a lógica de roteamento das mensagens/nota fiscal.

EXCHANGE = "ocr.exchange"
DLX = "ocr.dlx"
FILA_SOLICITACOES = "ocr.solicitacao"
FILA_RESULTADOS = "ocr.resultado"
FILA_DLQ = "ocr.solicitacao.dlq"
RK_SOLICITAR = "ocr.solicitar"
RK_RESULTADO = "ocr.resultado"

def declarar_topologia(canal) -> None:
    # Onde entregamos as notas ficais
    canal.exchange_declare(
        exchange=EXCHANGE,
        exchange_type="direct",
        durable=True
    )
    canal.exchange_declare(
        exchange=DLX,
        exchange_type="direct",
        durable=True
    )

    # Onde declaramos as filas e suas propriedades, incluindo a fila de DLQ (Dead Letter Queue)
    canal.queue_declare(
        queue=FILA_SOLICITACOES,
        durable=True,
        arguments={
            "x-dead-letter-exchange": DLX
        }
    )
    canal.queue_declare(
        queue=FILA_RESULTADOS,
        durable=True
    )
    canal.queue_declare(
        queue=FILA_DLQ,
        durable=True
    )

    # Onde vinculamos as filas aos exchanges com suas respectivas chaves de roteamento
    # Binding = regra que define como as mensagens serão roteadas para as filas.
    canal.queue_bind(
        exchange=EXCHANGE,
        queue=FILA_SOLICITACOES,
        routing_key=RK_SOLICITAR
    )
    canal.queue_bind(
        exchange=EXCHANGE,
        queue=FILA_RESULTADOS,
        routing_key=RK_RESULTADO
    )
    canal.queue_bind(
        exchange=DLX,
        queue=FILA_DLQ,
        routing_key=RK_SOLICITAR
    )

    