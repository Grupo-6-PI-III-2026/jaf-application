from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

import requests

from app.application.ports import OcrEnginePort
from app.domain.entities import OcrExtractionResult


OCR_SPACE_ENDPOINT = "https://api.ocr.space/parse/image"


class OcrSpaceAdapter(OcrEnginePort):
    def __init__(self, api_key: str) -> None:
        self._api_key = (api_key or "").strip()

    def extract_text(self, image_bytes: bytes, filename: str) -> OcrExtractionResult:
        if not self._api_key:
            raise RuntimeError("OCR_SPACE_API_KEY nao configurada.")

        response = requests.post(
            OCR_SPACE_ENDPOINT,
            headers={"apikey": self._api_key},
            files={"file": (filename, image_bytes)},
            data={
                "language": "por",
                "isOverlayRequired": "false",
                "OCREngine": "2",
                "isTable": "true",
                "scale": "true",
            },
            timeout=90,
        )

        if response.status_code >= 400:
            raise RuntimeError(
                f"Falha no OCR.space (HTTP {response.status_code}): {response.text[:500]}"
            )

        try:
            payload = response.json()
        except json.JSONDecodeError as exc:
            raise RuntimeError(
                f"Resposta invalida da OCR.space: {response.text[:500]}"
            ) from exc

        if payload.get("IsErroredOnProcessing"):
            detail = (
                payload.get("ErrorMessage")
                or payload.get("ErrorDetails")
                or "Erro desconhecido no OCR."
            )
            raise RuntimeError(f"OCR.space retornou erro: {detail}")

        return OcrExtractionResult(
            extracted_text=_collect_parsed_text(payload),
            processed_at=datetime.now(timezone.utc),
            provider="ocr.space",
            filename=filename,
            confidence=_collect_mean_confidence(payload),
            provider_payload=payload,
        )


def _collect_parsed_text(ocr_payload: dict[str, Any]) -> str:
    parsed_results = ocr_payload.get("ParsedResults")
    if not isinstance(parsed_results, list):
        return ""

    texts: list[str] = []
    for item in parsed_results:
        if isinstance(item, dict):
            value = item.get("ParsedText")
            if isinstance(value, str) and value.strip():
                texts.append(value.strip())
    return "\n".join(texts).strip()


def _collect_mean_confidence(ocr_payload: dict[str, Any]) -> float | None:
    parsed_results = ocr_payload.get("ParsedResults")
    if not isinstance(parsed_results, list):
        return None

    values: list[float] = []
    for item in parsed_results:
        if not isinstance(item, dict):
            continue
        raw = item.get("MeanConfidencePercentage")
        if isinstance(raw, (int, float)):
            values.append(float(raw))

    if not values:
        return None
    return sum(values) / len(values)
