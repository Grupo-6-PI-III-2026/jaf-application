from flask import Flask

from app.presentation.routers import ocr_bp


def create_app() -> Flask:
    application = Flask(__name__)
    application.register_blueprint(ocr_bp)
    return application


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=True)
