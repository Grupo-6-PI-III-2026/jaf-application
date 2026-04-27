from __future__ import annotations

import tempfile
from pathlib import Path

from flask import Flask, jsonify, render_template, request

from ocr_space_service import OCR_SPACE_API_KEY, run_ocr_space


app = Flask(__name__, template_folder=".")


@app.get("/")
def index():
    return render_template("OCR.html")


@app.post("/api/ocr")
def api_ocr():
    if "file" not in request.files:
        return jsonify({"status": "error", "message": "Arquivo nao enviado."}), 400

    file = request.files["file"]
    if not file.filename:
        return jsonify({"status": "error", "message": "Arquivo invalido."}), 400

    api_key = (OCR_SPACE_API_KEY or "").strip()

    if not api_key:
        return (
            jsonify(
                {
                    "status": "error",
                    "message": "OCR_SPACE_API_KEY nao configurada no arquivo ocr_space_service.py.",
                }
            ),
            400,
        )

    suffix = Path(file.filename).suffix or ".bin"
    temp_path: Path | None = None

    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
            file.save(temp_file.name)
            temp_path = Path(temp_file.name)

        result = run_ocr_space(file_path=temp_path, api_key=api_key)
        result["source"]["filename"] = Path(file.filename).name
        return jsonify(result)
    except Exception as exc:
        return jsonify({"status": "error", "message": str(exc)}), 500
    finally:
        if temp_path and temp_path.exists():
            temp_path.unlink(missing_ok=True)


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)
