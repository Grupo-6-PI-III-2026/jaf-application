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

def ao_receber_mensagem(ch, method, properties, body):
    print(f"Mensagem recebida: {body}")

# Liga consumidor à fila OCR
canal.basic_consume(queue="ocr.fila", on_message_callback=ao_receber_mensagem, auto_ack=True)
print("Aguardando mensagens. Para sair pressione CTRL+C")
canal.start_consuming()