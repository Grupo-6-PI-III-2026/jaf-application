from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from datetime import datetime, timezone
from typing import Any

from app.application.ports import OcrEnginePort
from app.domain.entities import OcrExtractionResult


_CONTENT_TYPE_MAP = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".gif": "image/gif",
    ".bmp": "image/bmp",
    ".webp": "image/webp",
    ".tiff": "image/tiff",
    ".pdf": "application/pdf",
}


class FilestackAdapter(OcrEnginePort):
    def __init__(
        self,
        api_key: str,
        policy: str = "",
        signature: str = "",
    ) -> None:
        self._api_key = (api_key or "").strip()
        self._policy = (policy or "").strip()
        self._signature = (signature or "").strip()

    def extract_text(self, image_bytes: bytes, filename: str) -> OcrExtractionResult:
        if not self._api_key:
            raise RuntimeError("FILESTACK_API_KEY nao configurada.")
        if bool(self._policy) != bool(self._signature):
            raise RuntimeError(
                "Preencha AMBOS Policy e Signature, ou deixe os dois em branco."
            )

        handle = self._upload(image_bytes, filename)
        payload = self._run_ocr(handle)
        return OcrExtractionResult(
            extracted_text=_extract_filestack_text(payload),
            processed_at=datetime.now(timezone.utc),
            provider="filestack",
            filename=filename,
            confidence=_extract_filestack_confidence(payload),
            provider_payload=payload,
        )

    def _upload(self, image_bytes: bytes, filename: str) -> str:
        url = f"https://www.filestackapi.com/api/store/S3?key={self._api_key}"
        boundary = "----FilestackBoundary7x9z"
        ext = os.path.splitext(filename)[1].lower()
        file_ct = _CONTENT_TYPE_MAP.get(ext, "application/octet-stream")

        body = (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="fileUpload"; filename="{filename}"\r\n'
            f"Content-Type: {file_ct}\r\n\r\n"
        ).encode() + image_bytes + f"\r\n--{boundary}--\r\n".encode()

        req = urllib.request.Request(
            url,
            data=body,
            headers={
                "Content-Type": f"multipart/form-data; boundary={boundary}",
                "Content-Length": str(len(body)),
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read().decode()
        except urllib.error.HTTPError as exc:
            body_err = exc.read().decode(errors="replace")
            raise RuntimeError(f"Erro no upload (HTTP {exc.code}): {body_err[:300]}") from exc

        try:
            data = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"Upload retornou resposta invalida:\n{raw[:300]}") from exc

        handle = data.get("handle")
        if not handle:
            raise RuntimeError(f"Handle nao encontrado na resposta:\n{raw[:300]}")
        return handle

    def _run_ocr(self, handle: str) -> dict[str, Any]:
        if self._policy and self._signature:
            url = (
                "https://cdn.filestackcontent.com/"
                f"security=p:{self._policy},s:{self._signature}"
                f"/ocr/{handle}"
            )
        else:
            url = f"https://cdn.filestackcontent.com/{self._api_key}/ocr/{handle}"

        req = urllib.request.Request(url, method="GET")
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read().decode()
        except urllib.error.HTTPError as exc:
            body_err = exc.read().decode(errors="replace")
            if exc.code == 400:
                raise RuntimeError(
                    "Erro 400 - Bad Request. Verifique Policy e Signature.\n"
                    f"Detalhe: {body_err[:400]}"
                ) from exc
            if exc.code in (401, 403):
                raise RuntimeError(
                    f"Erro {exc.code} - Nao autorizado. "
                    "API Key, Policy ou Signature invalidas ou sem permissao OCR.\n"
                    f"Detalhe: {body_err[:400]}"
                ) from exc
            raise RuntimeError(f"Erro HTTP {exc.code}:\n{body_err[:400]}") from exc

        if not raw.strip():
            raise RuntimeError(
                "A API retornou resposta vazia. Verifique Security/Policy/Signature "
                "e se o OCR esta disponivel no plano Filestack."
            )

        try:
            return json.loads(raw)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"Resposta nao e JSON valido:\n{raw[:500]}") from exc


def _extract_filestack_text(payload: dict[str, Any]) -> str:
    data = payload.get("data")
    if isinstance(data, dict):
        text = data.get("text")
        if isinstance(text, str) and text.strip():
            return text.strip()
    text = payload.get("text")
    if isinstance(text, str):
        return text.strip()
    return ""


def _extract_filestack_confidence(payload: dict[str, Any]) -> float | None:
    data = payload.get("data")
    if isinstance(data, dict):
        value = data.get("confidence")
        if isinstance(value, (int, float)):
            return float(value)
    value = payload.get("confidence")
    if isinstance(value, (int, float)):
        return float(value)
    return None
