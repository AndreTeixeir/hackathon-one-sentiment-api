"""
Testes de SentimentModel: modo modelo, fallback (arquivo ausente e
corrompido), e a assinatura das probabilidades fixas do fallback vs.
variáveis do modelo real — a distinção que o /health desta correção existe
para tornar observável.
"""
import hashlib
from pathlib import Path

from app.model import SentimentModel

REAL_MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "sentiment.joblib"


def test_carrega_modelo_real_sem_warning_e_expoe_metadados():
    model = SentimentModel(model_path=str(REAL_MODEL_PATH))
    model.load()

    assert model.mode == "model"
    assert model.model_loaded is True
    assert model.fallback_reason is None
    expected_hash = hashlib.sha256(REAL_MODEL_PATH.read_bytes()).hexdigest()
    assert model.model_version == expected_hash


def test_arquivo_ausente_cai_em_fallback_observavel():
    model = SentimentModel(model_path="models/nao-existe-de-verdade.joblib")
    model.load()

    assert model.mode == "fallback"
    assert model.model_loaded is False
    assert model.model_version is None
    assert "não encontrado" in model.fallback_reason


def test_arquivo_corrompido_cai_em_fallback_observavel(tmp_path):
    arquivo_lixo = tmp_path / "lixo.joblib"
    arquivo_lixo.write_text("isto nao e um pickle valido, so lixo de teste")

    model = SentimentModel(model_path=str(arquivo_lixo))
    model.load()

    assert model.mode == "fallback"
    assert model.model_loaded is False
    assert "Falha ao carregar" in model.fallback_reason


def test_fallback_usa_probabilidades_fixas_conhecidas():
    model = SentimentModel(model_path="models/nao-existe-de-verdade.joblib")
    model.load()

    positivo = model.predict("texto qualquer sem nenhuma palavra-chave negativa")
    negativo = model.predict("produto ruim, muito defeito")

    assert positivo.label == "Positivo"
    assert positivo.probability == 0.75
    assert negativo.label == "Negativo"
    assert negativo.probability == 0.85


def test_modelo_real_devolve_probabilidade_variavel_nao_fixa():
    model = SentimentModel(model_path=str(REAL_MODEL_PATH))
    model.load()

    positivo = model.predict("produto excelente, recomendo muito, adorei")
    negativo = model.predict("produto péssimo, veio com defeito e atrasou")

    assert positivo.label == "Positivo"
    assert negativo.label == "Negativo"
    # A assinatura do bug original: fallback sempre devolve exatamente
    # 0.75/0.85. O modelo real nunca deveria coincidir com esses valores.
    assert positivo.probability not in (0.75, 0.85)
    assert negativo.probability not in (0.75, 0.85)
