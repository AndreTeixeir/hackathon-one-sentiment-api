from fastapi import FastAPI
from pydantic import BaseModel
from textblob import TextBlob
import uvicorn

app = FastAPI(title="Hackathon Sentiment API (Modo Teste)")

# 1. O que o Java vai mandar (JSON com "text")
# Nota: O Java manda "text", então aqui tem que ser "text"
class SentimentRequest(BaseModel):
    text: str

# 2. O que o Java espera receber (JSON com "prediction" e "probability")
class SentimentResponse(BaseModel):
    prediction: str
    probability: float

@app.post("/predict", response_model=SentimentResponse)
def predict(req: SentimentRequest):
    # Lógica simples (Estepe) usando TextBlob
    # Isso substitui o arquivo .joblib por enquanto
    analise = TextBlob(req.text)
    polaridade = analise.sentiment.polarity  # Vai de -1 a +1

    # Regra de Classificação
    if polaridade > 0:
        sentimento = "Positivo"
    elif polaridade < 0:
        sentimento = "Negativo"
    else:
        sentimento = "Neutro"

    # Retorno exato que o Java espera
    return {
        "prediction": sentimento,
        "probability": abs(polaridade)
    }

if __name__ == "__main__":
    # Roda o servidor na porta 5000
    uvicorn.run(app, host="0.0.0.0", port=5000)