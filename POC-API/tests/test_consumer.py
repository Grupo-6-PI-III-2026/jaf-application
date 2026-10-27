import base64
import json
import unittest
from datetime import datetime
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from app.domain.entities import OcrExtractionResult
from app.infrastructure.messaging import consumer
from app.infrastructure.messaging.topology import EXCHANGE, RK_RESULTADO


def _mensagem(**sobrescritas):
    corpo = {
        "jobId": "job-1",
        "filename": "nota.png",
        "imagemBase64": base64.b64encode(b"\x01\x02\x03").decode("ascii"),
    }
    corpo.update(sobrescritas)
    return json.dumps(corpo).encode()


class AoReceberSolicitacaoTest(unittest.TestCase):
    def setUp(self):
        self.canal = MagicMock()
        self.metodo = SimpleNamespace(delivery_tag=7)

    def _resultado_publicado(self):
        self.canal.basic_publish.assert_called_once()
        kwargs = self.canal.basic_publish.call_args.kwargs
        self.assertEqual(EXCHANGE, kwargs["exchange"])
        self.assertEqual(RK_RESULTADO, kwargs["routing_key"])
        self.assertEqual("application/json", kwargs["properties"].content_type)
        return json.loads(kwargs["body"])

    @patch.object(consumer, "get_process_ocr_use_case")
    def test_ocr_concluido_publica_resultado_e_confirma(self, fabrica):
        fabrica.return_value.execute.return_value = OcrExtractionResult(
            extracted_text="TOTAL R$ 10,00",
            processed_at=datetime.now(),
            provider="ocr_space",
            filename="nota.png",
            confidence=91.0,
        )

        consumer.ao_receber_solicitacao(self.canal, self.metodo, None, _mensagem())

        fabrica.return_value.execute.assert_called_once_with(image_bytes=b"\x01\x02\x03", filename="nota.png")
        self.assertEqual(
            {"jobId": "job-1", "status": "CONCLUIDO", "textoBruto": "TOTAL R$ 10,00", "confianca": 91.0},
            self._resultado_publicado(),
        )
        self.canal.basic_ack.assert_called_once_with(delivery_tag=7)
        self.canal.basic_nack.assert_not_called()

    @patch.object(consumer, "get_process_ocr_use_case")
    def test_falha_no_ocr_publica_erro_com_job_id(self, fabrica):
        fabrica.return_value.execute.side_effect = RuntimeError("timeout")

        consumer.ao_receber_solicitacao(self.canal, self.metodo, None, _mensagem())

        self.assertEqual({"jobId": "job-1", "status": "ERRO", "erro": "timeout"}, self._resultado_publicado())
        self.canal.basic_ack.assert_called_once_with(delivery_tag=7)

    @patch.object(consumer, "get_process_ocr_use_case")
    def test_mensagem_invalida_vai_para_dlq(self, fabrica):
        invalidas = [
            b"nao e json",
            json.dumps({"filename": "nota.png", "imagemBase64": "AQID"}).encode(),
            _mensagem(imagemBase64="***nao-base64***"),
        ]
        for corpo in invalidas:
            with self.subTest(corpo=corpo):
                self.canal.reset_mock()

                consumer.ao_receber_solicitacao(self.canal, self.metodo, None, corpo)

                self.canal.basic_nack.assert_called_once_with(delivery_tag=7, requeue=False)
                self.canal.basic_publish.assert_not_called()
                self.canal.basic_ack.assert_not_called()
        fabrica.assert_not_called()


if __name__ == "__main__":
    unittest.main()
