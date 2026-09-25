"""
Testes de contrato: consome os payloads reais de contracts/examples/ — o
único ponto do repositório que os define, e que nenhum teste lia até esta
frente (par do teste Java equivalente, ContractExamplesTest).

Roda contra o modelo real (ds-service/models/sentiment.joblib), não mockado.
"""
import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app


def _contracts_examples_dir() -> Path:
    d = Path(__file__).resolve().parent
    for _ in range(6):
        candidate = d / "contracts" / "examples"
        if candidate.is_dir():
            return candidate
        d = d.parent
    raise FileNotFoundError(
        f"contracts/examples não encontrado subindo a partir de {Path(__file__).resolve()}"
    )


CONTRACTS_DIR = _contracts_examples_dir()


@pytest.mark.parametrize(
    "filename,expected_label",
    [
        ("positive.json", "Positivo"),
        ("negative.json", "Negativo"),
    ],
)
def test_contract_example_predicts_expected_label(filename, expected_label):
    payload = json.loads((CONTRACTS_DIR / filename).read_text(encoding="utf-8"))
    assert payload["text"].strip() != ""

    with TestClient(app) as client:
        response = client.post("/predict", json=payload)

    assert response.status_code == 200
    body = response.json()
    assert body["label"] == expected_label
    assert 0.0 <= body["probability"] <= 1.0
