import pika
from config import (
    RABBITMQ_HOST, 
    RABBITMQ_PORT, 
    RABBITMQ_USER, 
    RABBITMQ_PASSWORD
)

# Credencias do RabbitMQ
credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)

# Conexão com o RabbitMQ
parametrosConexao = pika.ConnectionParameters(
    host=RABBITMQ_HOST,
    port=RABBITMQ_PORT,
    credentials=credentials
)

conexao = pika.BlockingConnection(parametrosConexao)

canal = conexao.channel()

# Declaração da fila OCR
canal.queue_declare(queue="ocr.fila", durable=True)

# Publicação para exchange
canal.basic_publish(
    exchange='',
    routing_key='ocr.fila',
    body='Mensagem de teste',
    properties=pika.BasicProperties(
        delivery_mode=2,  # Torna a mensagem persistente
    )
)
print("Mensagem publicada com sucesso.")

# Fechamento da conexão com o RabbitMQ
conexao.close()