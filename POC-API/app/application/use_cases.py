from __future__ import annotations

from app.application.ports import OcrEnginePort
from app.domain.entities import OcrExtractionResult, parse_catalog_items


class ProcessOcrUseCase:
    def __init__(self, ocr_engine: OcrEnginePort) -> None:
        self._ocr_engine = ocr_engine

    def execute(self, image_bytes: bytes, filename: str) -> OcrExtractionResult:
        result = self._ocr_engine.extract_text(image_bytes, filename)
        result.catalog_items = parse_catalog_items(result.extracted_text)
        return result
