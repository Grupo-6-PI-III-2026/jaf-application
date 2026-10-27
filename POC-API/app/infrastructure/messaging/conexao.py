import pika

from config import RABBITMQ_HOST, RABBITMQ_PASSWORD, RABBITMQ_PORT, RABBITMQ_USER


def abrir_conexao() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    parametros = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
        # O callback bloqueia o loop de I/O durante o OCR (até 90s); com o heartbeat
        # padrão de 60s o broker derrubaria a conexão no meio do processamento.
        heartbeat=600,
    )
    return pika.BlockingConnection(parametros)
