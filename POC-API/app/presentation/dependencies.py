from __future__ import annotations

import os

from app.application.ports import OcrEnginePort
from app.application.use_cases import ProcessOcrUseCase
from app.infrastructure.filestack_adapter import FilestackAdapter
from app.infrastructure.ocr_space_adapter import OcrSpaceAdapter


def get_ocr_engine() -> OcrEnginePort:
    engine = os.getenv("OCR_ENGINE", "ocr_space").strip().lower()
    if engine == "filestack":
        return FilestackAdapter(
            api_key=os.getenv("FILESTACK_API_KEY", ""),
            policy=os.getenv("FILESTACK_POLICY", ""),
            signature=os.getenv("FILESTACK_SIGNATURE", ""),
        )
    return OcrSpaceAdapter(
        api_key=os.getenv("OCR_SPACE_API_KEY", ""),
    )


def get_process_ocr_use_case() -> ProcessOcrUseCase:
    return ProcessOcrUseCase(get_ocr_engine())
