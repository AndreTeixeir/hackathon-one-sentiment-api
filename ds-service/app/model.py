import hashlib
import logging
import os
from dataclasses import dataclass
from typing import Optional

import joblib

logger = logging.getLogger(__name__)

# 0/1 são as classes cruas devolvidas pelo classificador (ver classes_ do
# LogisticRegression treinado). O mapeamento é explícito para não vazar
# "0"/"1" crus na resposta da API.
LABEL_MAP = {0: "Negativo", 1: "Positivo"}


@dataclass(frozen=True)
class ModelResult:
    label: str
    probability: float


class SentimentModel:
    """
    Wrapper do pipeline de ML (scikit-learn).
    - Carrega um Pipeline único (TfidfVectorizer + LogisticRegression) de um
      arquivo .joblib, se existir.
    - Se não existir ou falhar ao carregar, usa fallback heurístico para não
      bloquear a integração. O fallback é sempre explícito e observável via
      `mode`/`fallback_reason` (ver /health) e log de WARNING na inicialização
      — nunca silencioso.
    """

    def __init__(self, model_path: str):
        self.model_path = model_path
        self.pipeline: Optional[object] = None
        self.model_version: Optional[str] = None
        self.fallback_reason: Optional[str] = None

    @property
    def mode(self) -> str:
        return "model" if self.pipeline is not None else "fallback"

    @property
    def model_loaded(self) -> bool:
        return self.pipeline is not None

    def load(self) -> None:
        if not os.path.exists(self.model_path):
            self.fallback_reason = f"Arquivo de modelo não encontrado: {self.model_path}"
            logger.warning(
                "Modelo não encontrado em %s — operando em fallback heurístico.",
                self.model_path,
            )
            return

        try:
            self.pipeline = joblib.load(self.model_path)
            self.model_version = self._compute_version(self.model_path)
        except Exception as exc:
            self.pipeline = None
            self.fallback_reason = f"Falha ao carregar o modelo: {exc}"
            logger.warning(
                "Falha ao carregar modelo em %s (%s) — operando em fallback heurístico.",
                self.model_path,
                exc,
            )

    @staticmethod
    def _compute_version(path: str) -> str:
        """SHA-256 do artefato — muda sempre que o arquivo muda, verificável com `sha256sum`."""
        with open(path, "rb") as f:
            return hashlib.sha256(f.read()).hexdigest()

    def predict(self, text: str) -> ModelResult:
        if self.pipeline is None:
            lowered = text.lower()
            negative_markers = ["ruim", "péssim", "horr", "defeito", "demor", "atras", "não recomendo"]
            if any(m in lowered for m in negative_markers):
                return ModelResult(label="Negativo", probability=0.85)
            return ModelResult(label="Positivo", probability=0.75)

        proba = self.pipeline.predict_proba([text])[0]
        classes = list(self.pipeline.classes_)
        best_idx = int(proba.argmax())
        label = LABEL_MAP.get(int(classes[best_idx]), str(classes[best_idx]))
        return ModelResult(label=label, probability=float(proba[best_idx]))
