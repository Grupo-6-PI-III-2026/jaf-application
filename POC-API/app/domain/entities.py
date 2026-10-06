from __future__ import annotations

import re
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any


@dataclass
class CatalogItem:
    nome_produto: str
    estoque: str | None = None
    preco: str | None = None
    local: str | None = None
    horario: str | None = None

    def to_dict(self) -> dict[str, Any]:
        data: dict[str, Any] = {"Nome_Produto": self.nome_produto}
        if self.estoque is not None:
            data["Estoque"] = self.estoque
        if self.preco is not None:
            data["Preco"] = self.preco
        if self.local is not None:
            data["Local"] = self.local
        if self.horario is not None:
            data["Horario"] = self.horario
        return data


@dataclass
class OcrExtractionResult:
    extracted_text: str
    processed_at: datetime
    provider: str
    filename: str
    confidence: float | None = None
    provider_payload: dict[str, Any] | None = None
    catalog_items: list[CatalogItem] = field(default_factory=list)


def parse_catalog_items(text: str) -> list[CatalogItem]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    items: list[CatalogItem] = []
    current: dict[str, str] = {}

    for line in lines:
        parsed = _extract_label_value(line)
        if not parsed:
            continue
        label, value = parsed
        normalized_label = _normalize_label(label)

        if normalized_label in ("nomeproduto", "produto", "nome"):
            if current.get("nome_produto"):
                items.append(_item_from_partial(current))
                current = {}
            current["nome_produto"] = value
        elif normalized_label == "estoque":
            current["estoque"] = value
        elif normalized_label in ("preco", "valor"):
            current["preco"] = value
        elif normalized_label == "local":
            current["local"] = value
        elif normalized_label in ("horario", "hora"):
            current["horario"] = value

    if current.get("nome_produto"):
        items.append(_item_from_partial(current))

    return items


def _item_from_partial(data: dict[str, str]) -> CatalogItem:
    return CatalogItem(
        nome_produto=data["nome_produto"],
        estoque=data.get("estoque"),
        preco=data.get("preco"),
        local=data.get("local"),
        horario=data.get("horario"),
    )


def _extract_label_value(line: str) -> tuple[str, str] | None:
    match = re.match(r"^\s*([A-Za-zÀ-ÿ_]+)\s*:\s*(.+?)\s*$", line)
    if not match:
        return None
    return match.group(1).strip(), match.group(2).strip()


def _normalize_label(label: str) -> str:
    return (
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
