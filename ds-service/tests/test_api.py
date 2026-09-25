"""
Testes da API do ds-service (TestClient, modelo real carregado a partir de
ds-service/models/sentiment.joblib — sem mock).
"""
from fastapi.testclient import TestClient

from app.main import app


def test_health_expoe_todos_os_campos_no_modo_modelo():
    with TestClient(app) as client:
        response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["mode"] == "model"
    assert body["model_loaded"] is True
    assert isinstance(body["model_version"], str) and len(body["model_version"]) == 64
    assert body["fallback_reason"] is None
    assert set(body["classes"]) == {"Positivo", "Negativo"}


def test_predict_positivo_com_probabilidade_variavel():
    with TestClient(app) as client:
        response = client.post("/predict", json={"text": "produto excelente, recomendo muito"})

    assert response.status_code == 200
    body = response.json()
    assert body["label"] == "Positivo"
    assert body["probability"] not in (0.75, 0.85)
    assert 0.0 <= body["probability"] <= 1.0


def test_predict_negativo_com_probabilidade_variavel():
    with TestClient(app) as client:
        response = client.post("/predict", json={"text": "produto péssimo, veio com defeito"})

    assert response.status_code == 200
    body = response.json()
    assert body["label"] == "Negativo"
    assert body["probability"] not in (0.75, 0.85)


def test_texto_curto_e_rejeitado():
    with TestClient(app) as client:
        response = client.post("/predict", json={"text": "ab"})

    # Validação do Pydantic (Field min_length=3) intercepta antes do handler —
    # 422, não o 400 que o `if len(text) < 3` de main.py sugere (esse trecho
    # é código morto, nunca alcançado; mesmo padrão encontrado no lado do
    # backend Java na Etapa 2 desta correção).
    assert response.status_code == 422
