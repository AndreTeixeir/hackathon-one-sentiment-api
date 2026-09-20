"""
Testes de regressão do modelo: corpus fixo com rótulos e probabilidades
esperadas, ancorado no SHA-256 do artefato. Se alguém trocar o .joblib
(retreino, correção, ou reintrodução do bug de fallback), este teste
detecta a mudança — seja porque o hash não bate mais (força atualização
consciente da âncora), seja porque uma predição mudou de classe/probabilidade.
"""
import hashlib
from pathlib import Path

import joblib
import pytest

MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "sentiment.joblib"

# Âncora: SHA-256 do artefato commitado nesta branch (Etapa 1). Se isto não
# bater, o .joblib mudou — atualizar aqui só depois de validar manualmente
# que o CORPUS abaixo continua correto. Não é para silenciar o teste.
EXPECTED_MODEL_VERSION = "e658513705ca048b2664fd3a4e7585f15ea611b3c05c3938e9c4eb17012d6c1e"

LABEL_MAP = {0: "Negativo", 1: "Positivo"}

# (texto, rótulo esperado, probabilidade esperada da classe prevista, tolerância)
CORPUS = [
    ("produto excelente, recomendo muito, adorei", "Positivo", 0.9238, 0.01),
    ("produto péssimo, veio com defeito e atrasou", "Negativo", 0.8220, 0.01),
    ("Recebi o produto. Ainda vou testar melhor.", "Positivo", 0.8218, 0.01),
    ("Atendimento excelente, resolveu meu problema rapidamente! Recomendo a todos.", "Positivo", 0.8872, 0.01),
    ("Demorou muito e veio com defeito. Péssima experiência, não comprem!", "Negativo", 0.7811, 0.01),
    ("Fui hoje ao parque e consegui aproveitar bastante, as filas estavam pequenas e rápidas.", "Positivo", 0.8071, 0.01),
    ("Limitando número de visitantes no parque. Estava insuportavelmente lotado.", "Negativo", 0.6606, 0.01),
]


@pytest.fixture(scope="module")
def pipeline():
    return joblib.load(MODEL_PATH)


def test_model_version_anchor():
    actual_hash = hashlib.sha256(MODEL_PATH.read_bytes()).hexdigest()
    assert actual_hash == EXPECTED_MODEL_VERSION, (
        "O artefato do modelo mudou (SHA-256 diferente do esperado). "
        "Se isso for intencional (retreino, correção), valide o CORPUS "
        "abaixo manualmente e só então atualize EXPECTED_MODEL_VERSION — "
        "não é para silenciar este teste sem revalidar."
    )


@pytest.mark.parametrize("texto,rotulo_esperado,prob_esperada,tolerancia", CORPUS)
def test_corpus_de_regressao(pipeline, texto, rotulo_esperado, prob_esperada, tolerancia):
    proba = pipeline.predict_proba([texto])[0]
    idx = proba.argmax()
    label = LABEL_MAP[int(pipeline.classes_[idx])]

    assert label == rotulo_esperado, (
        f"Classe mudou para {texto!r}: esperado {rotulo_esperado}, veio {label} "
        f"(p={proba[idx]:.4f}). O .joblib provavelmente mudou de comportamento."
    )
    assert proba[idx] == pytest.approx(prob_esperada, abs=tolerancia), (
        f"Probabilidade mudou significativamente para {texto!r}: "
        f"esperado ~{prob_esperada}, veio {proba[idx]:.4f}."
    )


@pytest.mark.xfail(
    reason="ADR-004 §6.1: modelo sensível a acentuação — 'pessimo' sem acento "
    "classifica como Positivo (errado); vocabulário TF-IDF tem 168 tokens "
    "acentuados. Não corrigir normalizando só na inferência (pioraria o "
    "resultado, o vocabulário treinado tem acentos). Documenta a limitação "
    "sem deixar o CI vermelho.",
    strict=True,
)
def test_acentuacao_pessimo_sem_acento_deveria_ser_negativo(pipeline):
    proba = pipeline.predict_proba(["produto pessimo, veio com defeito e atrasou"])[0]
    label = LABEL_MAP[int(pipeline.classes_[proba.argmax()])]
    assert label == "Negativo"
