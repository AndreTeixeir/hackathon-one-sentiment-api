import os

from fastapi import FastAPI, HTTPException
from .schemas import PredictRequest, PredictResponse
from .model import SentimentModel

app = FastAPI(title="ds-service", version="0.1.0")

MODEL_PATH = os.getenv("MODEL_PATH", "models/sentiment.joblib")
model = SentimentModel(model_path=MODEL_PATH)


@app.on_event("startup")
def on_startup() -> None:
    model.load()


@app.get("/health")
def health():
    return {
        "status": "ok",
        "mode": model.mode,
        "model_loaded": model.model_loaded,
        "model_version": model.model_version,
        "fallback_reason": model.fallback_reason,
        "classes": ["Negativo", "Positivo"],
    }


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    text = (req.text or "").strip()
    if len(text) < 3:
        raise HTTPException(status_code=400, detail="Campo 'text' deve ter pelo menos 3 caracteres.")

    result = model.predict(text)
    return PredictResponse(label=result.label, probability=result.probability)
