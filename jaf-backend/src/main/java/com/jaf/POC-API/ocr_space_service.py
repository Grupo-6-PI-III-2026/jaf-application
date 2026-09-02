from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests


# Configure sua chave da OCR.space aqui.
OCR_SPACE_API_KEY = "K83732583388957"
OCR_SPACE_ENDPOINT = "https://api.ocr.space/parse/image"


def _extract_label_value(line: str) -> tuple[str, str] | None:
    match = re.match(r"^\s*([A-Za-zÀ-ÿ_]+)\s*:\s*(.+?)\s*$", line)
    if not match:
        return None
    return match.group(1).strip(), match.group(2).strip()


def _normalize_label(label: str) -> str:
    normalized = (
        label.lower()
        .replace("ç", "c")
        .replace("ã", "a")
        .replace("á", "a")
        .replace("â", "a")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("ú", "u")
        .replace("_", "")
        .replace(" ", "")
    )
    return normalized


def _extract_catalog_items(text: str) -> list[dict[str, Any]]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    items: list[dict[str, Any]] = []
    current: dict[str, Any] = {}

    for line in lines:
        parsed = _extract_label_value(line)
        if not parsed:
            continue
        label, value = parsed
        normalized_label = _normalize_label(label)

        if normalized_label in ("nomeproduto", "produto", "nome"):
            if current:
                items.append(current)
                current = {}
            current["Nome_Produto"] = value
        elif normalized_label == "estoque":
            current["Estoque"] = value
        elif normalized_label in ("preco", "valor"):
            current["Preco"] = value
        elif normalized_label == "local":
            current["Local"] = value
        elif normalized_label in ("horario", "hora"):
            current["Horario"] = value

    if current:
        items.append(current)

    return [item for item in items if item.get("Nome_Produto")]


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


def run_ocr_space(file_path: Path, api_key: str) -> dict[str, Any]:
    if not api_key.strip():
        raise RuntimeError("OCR_SPACE_API_KEY nao configurada.")

    with file_path.open("rb") as file_obj:
        response = requests.post(
            OCR_SPACE_ENDPOINT,
            headers={"apikey": api_key.strip()},
            files={"file": (file_path.name, file_obj)},
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
        raise RuntimeError(f"Falha no OCR.space (HTTP {response.status_code}): {response.text[:500]}")

    try:
        payload = response.json()
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Resposta invalida da OCR.space: {response.text[:500]}") from exc

    if payload.get("IsErroredOnProcessing"):
        detail = payload.get("ErrorMessage") or payload.get("ErrorDetails") or "Erro desconhecido no OCR."
        raise RuntimeError(f"OCR.space retornou erro: {detail}")

    raw_text = _collect_parsed_text(payload)
    catalog_items = _extract_catalog_items(raw_text)
    return {
        "status": "success",
        "processed_at": datetime.now(timezone.utc).isoformat(),
        "source": {
            "filename": file_path.name,
            "provider": "ocr.space",
        },
        "ocr": {
            "raw_text": raw_text,
            "provider_payload": payload,
        },
        "dados_extraidos": {
            "tipo": "catalogo_produtos",
            "total_itens": len(catalog_items),
            "itens": catalog_items,
        },
    }
