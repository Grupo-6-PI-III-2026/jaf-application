import base64
import json

import pika

from app.infrastructure.messaging.conexao import abrir_conexao
from app.infrastructure.messaging.topology import (
    EXCHANGE,
    FILA_SOLICITACOES,
    RK_RESULTADO,
    declarar_topologia,
)
from app.presentation.dependencies import get_process_ocr_use_case


def ao_receber_solicitacao(canal, metodo, propriedades, corpo):
    # Envelope quebrado: sem jobId não há para quem responder -> DLQ.
    try:
        mensagem = json.loads(corpo)
        job_id = mensagem["jobId"]
        filename = mensagem["filename"]
        imagem_bytes = base64.b64decode(mensagem["imagemBase64"], validate=True)
    except (ValueError, KeyError, TypeError) as erro:
        print(f"Mensagem invalida enviada para a DLQ: {erro}")
        canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=False)
        return

    print(f"[{job_id}] Processando {filename}...")

    # OCR falhou mas o jobId é conhecido -> avisamos o backend em vez de mandar para a DLQ.
    try:
        resultado = get_process_ocr_use_case().execute(
            image_bytes=imagem_bytes, filename=filename
        )
        resposta = {
            "jobId": job_id,
            "status": "CONCLUIDO",
            "textoBruto": resultado.extracted_text,
            "confianca": resultado.confidence,
        }
    except Exception as erro:
        resposta = {"jobId": job_id, "status": "ERRO", "erro": str(erro)}

    canal.basic_publish(
        exchange=EXCHANGE,
        routing_key=RK_RESULTADO,
        body=json.dumps(resposta),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
        ),
    )
    canal.basic_ack(delivery_tag=metodo.delivery_tag)
    print(f"[{job_id}] {resposta['status']}")


def main():
    conexao = abrir_conexao()
    canal = conexao.channel()

    declarar_topologia(canal)
    canal.basic_qos(prefetch_count=1)
    canal.basic_consume(
        queue=FILA_SOLICITACOES,
        on_message_callback=ao_receber_solicitacao,
    )

    print("Worker OCR aguardando solicitacoes. Para sair pressione CTRL+C")
    try:
        canal.start_consuming()
    except KeyboardInterrupt:
        canal.stop_consuming()
    finally:
        conexao.close()


if __name__ == "__main__":
    main()
