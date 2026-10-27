import base64
import json
import sys
import uuid
from pathlib import Path

import pika

from app.infrastructure.messaging.conexao import abrir_conexao
from app.infrastructure.messaging.topology import (
    EXCHANGE,
    RK_SOLICITAR,
    declarar_topologia,
)


def publicar_solicitacao(caminho_imagem: str) -> str:
    imagem = Path(caminho_imagem)
    job_id = str(uuid.uuid4())
    mensagem = {
        "jobId": job_id,
        "filename": imagem.name,
        "imagemBase64": base64.b64encode(imagem.read_bytes()).decode("ascii"),
    }

    conexao = abrir_conexao()
    try:
        canal = conexao.channel()
        declarar_topologia(canal)
        canal.basic_publish(
            exchange=EXCHANGE,
            routing_key=RK_SOLICITAR,
            body=json.dumps(mensagem),
            properties=pika.BasicProperties(
                content_type="application/json",
                delivery_mode=2,
            ),
        )
    finally:
        conexao.close()
    return job_id


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Uso: python -m app.infrastructure.messaging.produtor <caminho-da-imagem>")
        sys.exit(1)
    print(f"Solicitacao publicada. jobId={publicar_solicitacao(sys.argv[1])}")
