#!/bin/bash
# Script de deploy para Oracle Cloud Infrastructure (OCI)
# Execute na VM da OCI após clonar o repositório

set -e

echo "=========================================="
echo "  Sentiment API - Deploy para OCI"
echo "=========================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para log
log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Verifica se Docker está instalado
if ! command -v docker &> /dev/null; then
    log "Instalando Docker..."
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker $USER
    warn "Docker instalado. Faça logout e login novamente, depois execute este script novamente."
    exit 0
fi

# Verifica se docker-compose está instalado
if ! command -v docker-compose &> /dev/null; then
    log "Instalando Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# Verifica se está no diretório correto
if [ ! -f "docker-compose.yml" ]; then
    error "Arquivo docker-compose.yml não encontrado. Execute este script na raiz do projeto."
fi

# Cria arquivo .env se não existir
if [ ! -f ".env" ]; then
    log "Criando arquivo .env a partir do exemplo..."
    cp .env.example .env
    warn "Edite o arquivo .env com suas configurações antes de continuar."
    warn "Execute: nano .env"
    exit 0
fi

# Para containers existentes
log "Parando containers existentes..."
docker-compose down 2>/dev/null || true

# Remove imagens antigas (opcional)
read -p "Deseja remover imagens antigas para rebuild completo? (s/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    log "Removendo imagens antigas..."
    docker-compose down --rmi all 2>/dev/null || true
fi

# Build e start dos containers
log "Construindo e iniciando containers..."
docker-compose up -d --build

# Aguarda containers ficarem saudáveis
log "Aguardando serviços iniciarem..."
sleep 30

# Verifica status dos containers
log "Status dos containers:"
docker-compose ps

# Verifica health dos serviços
log "Verificando saúde dos serviços..."

# Verifica PostgreSQL
if docker-compose exec -T postgres pg_isready -U sentiment_user -d sentimentdb > /dev/null 2>&1; then
    echo -e "  PostgreSQL: ${GREEN}OK${NC}"
else
    echo -e "  PostgreSQL: ${RED}FALHA${NC}"
fi

# Verifica DS Service
if curl -s http://localhost:8000/health > /dev/null 2>&1; then
    echo -e "  DS Service: ${GREEN}OK${NC}"
else
    echo -e "  DS Service: ${RED}FALHA${NC}"
fi

# Verifica Backend
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "  Backend:    ${GREEN}OK${NC}"
else
    echo -e "  Backend:    ${RED}FALHA${NC}"
fi

# Obtém IP público
PUBLIC_IP=$(curl -s ifconfig.me 2>/dev/null || echo "IP não disponível")

echo ""
echo "=========================================="
echo "  Deploy concluído!"
echo "=========================================="
echo ""
echo "Acesse a aplicação em:"
echo "  - Domínio:  https://sentiment.andreteixeira.dev.br"
echo "  - Frontend: http://${PUBLIC_IP}:8080"
echo "  - API:      http://${PUBLIC_IP}:8080/api/v1/sentiment"
echo "  - Health:   http://${PUBLIC_IP}:8080/actuator/health"
echo ""
echo "Para ver logs:"
echo "  docker-compose logs -f"
echo ""
echo "Para parar:"
echo "  docker-compose down"
echo ""
