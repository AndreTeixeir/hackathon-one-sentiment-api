# Guia de Deploy na Oracle Cloud Infrastructure (OCI)

Este guia detalha o processo de deploy da Sentiment API na OCI Free Tier.

## Pré-requisitos

1. Conta na Oracle Cloud (Free Tier)
2. Chave SSH gerada localmente
3. Conhecimento básico de terminal Linux

## Passo 1: Criar Conta na OCI

Se ainda não tem conta:
1. Acesse https://cloud.oracle.com
2. Clique em "Start for free"
3. Complete o cadastro (requer cartão de crédito, mas não cobra)

## Passo 2: Criar VM Compute

### 2.1 Acessar o Console

1. Faça login em https://cloud.oracle.com
2. Navegue para: Menu → Compute → Instances
3. Clique em "Create Instance"

### 2.2 Configurar a Instância

**Nome:** sentiment-api-server

**Placement:** Deixe o padrão

**Image and Shape:**
- Image: Ubuntu 22.04
- Shape: Escolha uma das opções Free Tier:
  - `VM.Standard.A1.Flex` (ARM - 4 OCPUs, 24GB RAM - RECOMENDADO)
  - `VM.Standard.E2.1.Micro` (AMD - 1 OCPU, 1GB RAM)

**Networking:**
- Crie uma nova VCN ou use existente
- Marque "Assign a public IPv4 address"

**Add SSH keys:**
- Faça upload da sua chave pública (`~/.ssh/id_rsa.pub`)
- Ou gere um novo par de chaves

**Boot volume:** Deixe o padrão (50GB)

### 2.3 Criar a Instância

Clique em "Create" e aguarde a instância ficar "Running".

## Passo 3: Configurar Security List

### 3.1 Acessar Security List

1. Na página da instância, clique na Subnet
2. Clique na Security List associada

### 3.2 Adicionar Ingress Rules

Adicione as seguintes regras:

| Source CIDR | Protocol | Destination Port | Description |
|-------------|----------|------------------|-------------|
| 0.0.0.0/0 | TCP | 80 | HTTP |
| 0.0.0.0/0 | TCP | 8080 | Backend API |
| 0.0.0.0/0 | TCP | 8000 | DS Service |

## Passo 4: Conectar via SSH

```bash
# Substitua pelo IP público da sua instância
ssh -i ~/.ssh/id_rsa ubuntu@SEU_IP_PUBLICO
```

## Passo 5: Configurar a VM

Execute o script de setup:

```bash
# Baixar e executar script de setup
curl -sSL https://raw.githubusercontent.com/AndreTeixeir/hackathon-one-sentiment-api/main/scripts/setup-oci-vm.sh -o setup.sh
chmod +x setup.sh
./setup.sh
```

**Importante:** Após o script, faça logout e login novamente:
```bash
exit
ssh -i ~/.ssh/id_rsa ubuntu@SEU_IP_PUBLICO
```

## Passo 6: Clonar e Configurar o Projeto

```bash
cd ~/sentiment-api
git clone https://github.com/AndreTeixeir/hackathon-one-sentiment-api.git .
```

## Passo 7: Configurar Variáveis de Ambiente

```bash
cp .env.example .env
nano .env
```

Edite as senhas se desejar (opcional para ambiente de demonstração).

## Passo 8: Executar Deploy

```bash
./scripts/deploy-oci.sh
```

O script irá:
1. Verificar dependências
2. Construir imagens Docker
3. Iniciar todos os containers
4. Verificar saúde dos serviços

## Passo 9: Verificar o Deploy

Após o deploy, teste os endpoints:

```bash
# Health check do backend
curl http://localhost:8080/actuator/health

# Health check do DS Service
curl http://localhost:8000/health

# Teste de análise de sentimento
curl -X POST http://localhost:8080/api/v1/sentiment \
  -H "Content-Type: application/json" \
  -d '{"text": "Este produto é excelente!"}'
```

## Passo 10: Acessar a Aplicação

Acesse no navegador:
- Frontend: `http://SEU_IP_PUBLICO`
- API: `http://SEU_IP_PUBLICO/api/v1/sentiment`

## Comandos Úteis

```bash
# Ver logs de todos os containers
docker-compose logs -f

# Ver logs de um container específico
docker-compose logs -f backend

# Reiniciar serviços
docker-compose restart

# Parar todos os serviços
docker-compose down

# Reconstruir e reiniciar
docker-compose up -d --build
```

## Troubleshooting

### Container não inicia

```bash
# Verificar logs
docker-compose logs backend

# Verificar status
docker-compose ps
```

### Erro de conexão com banco

```bash
# Verificar se PostgreSQL está rodando
docker-compose exec postgres pg_isready

# Verificar logs do PostgreSQL
docker-compose logs postgres
```

### Erro de porta já em uso

```bash
# Verificar processos nas portas
sudo netstat -tlnp | grep -E '80|8080|8000'

# Matar processo se necessário
sudo kill -9 PID
```

### Erro de memória (VM pequena)

Se estiver usando a VM AMD (1GB RAM), pode ser necessário criar swap:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## Custos

A OCI Free Tier inclui:
- 2 VMs AMD ou 4 VMs ARM (Always Free)
- 200GB de Block Volume
- 10GB de Object Storage
- Load Balancer (1 instância)

**Não há cobrança** enquanto você permanecer dentro dos limites do Free Tier.

## Próximos Passos

1. Configurar domínio personalizado
2. Adicionar certificado SSL (Let's Encrypt)
3. Configurar backups automáticos
4. Implementar CI/CD com GitHub Actions
