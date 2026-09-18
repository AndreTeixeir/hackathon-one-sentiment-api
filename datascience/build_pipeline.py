"""
Combina o TfidfVectorizer e o LogisticRegression treinados (dois objetos
separados) num único sklearn.pipeline.Pipeline, e exporta o resultado para
ds-service/models/sentiment.joblib.

Por que isso existe: ds-service/app/model.py carrega um único artefato e
chama pipeline.predict_proba([texto]) diretamente. Os artefatos originais
do treino (sentiment_model.pkl e tfidf_vectorizer.pkl) são dois objetos
separados — o texto precisa passar pelo vetorizador antes do classificador.
Este script faz essa combinação uma vez, para não exigir que o serviço saiba
que existe uma etapa de vetorização.

Requer scikit-learn==1.6.1 (mesma versão usada no treino) para evitar
InconsistentVersionWarning ao carregar os .pkl de origem.

Uso:
    pip install -r requirements.txt
    python build_pipeline.py
"""

from pathlib import Path

import joblib
from sklearn.pipeline import Pipeline

DATASCIENCE_DIR = Path(__file__).parent
VECTORIZER_PATH = DATASCIENCE_DIR / "tfidf_vectorizer.pkl"
MODEL_PATH = DATASCIENCE_DIR / "sentiment_model.pkl"
OUTPUT_PATH = DATASCIENCE_DIR.parent / "ds-service" / "models" / "sentiment.joblib"


def main() -> None:
    vectorizer = joblib.load(VECTORIZER_PATH)
    classifier = joblib.load(MODEL_PATH)

    pipeline = Pipeline([
        ("tfidf", vectorizer),
        ("clf", classifier),
    ])

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, OUTPUT_PATH)

    print(f"Pipeline salvo em: {OUTPUT_PATH}")
    print(f"Classes: {pipeline.classes_}")

    proba = pipeline.predict_proba(["produto excelente, recomendo"])[0]
    print(f"Teste de sanidade (texto positivo): classes_={pipeline.classes_}, proba={proba}")


if __name__ == "__main__":
    main()
