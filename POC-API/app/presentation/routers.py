from __future__ import annotations

from flask import Blueprint, jsonify, request

from app.domain.entities import OcrExtractionResult
from app.presentation.dependencies import get_process_ocr_use_case

ocr_bp = Blueprint("ocr", __name__)


@ocr_bp.post("/api/ocr")
def api_ocr():
    if "file" not in request.files:
        return jsonify({"status": "error", "message": "Arquivo nao enviado."}), 400

    file = request.files["file"]
    if not file.filename:
        return jsonify({"status": "error", "message": "Arquivo invalido."}), 400

    try:
        use_case = get_process_ocr_use_case()
        result = use_case.execute(image_bytes=file.read(), filename=file.filename)
        return jsonify(_to_api_response(result))
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500


def _to_api_response(result: OcrExtractionResult) -> dict:
    return {
        "status": "success",
        "processed_at": result.processed_at.isoformat(),
        "source": {
            "filename": result.filename,
            "provider": result.provider,
        },
        "ocr": {
            "raw_text": result.extracted_text,
            "confidence": result.confidence,
            "provider_payload": result.provider_payload,
        },
        "dados_extraidos": {
            "tipo": "catalogo_produtos",
            "total_itens": len(result.catalog_items),
            "itens": [item.to_dict() for item in result.catalog_items],
        },
    }
