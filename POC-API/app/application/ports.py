from __future__ import annotations

from abc import ABC, abstractmethod

from app.domain.entities import OcrExtractionResult


class OcrEnginePort(ABC):
    @abstractmethod
    def extract_text(self, image_bytes: bytes, filename: str) -> OcrExtractionResult:
        """Extrai texto de uma imagem e retorna um resultado de domínio."""
