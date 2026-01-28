#!/bin/bash
# Script de teste de processamento paralelo de videos
# Uso: ./scripts/test-parallel.sh

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TEST_VIDEO="$PROJECT_DIR/test-data/sample.mp4"

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Teste de Processamento Paralelo de Videos ===${NC}"
echo ""

# Verificar video de teste
if [[ ! -f "$TEST_VIDEO" ]]; then
    echo -e "${RED}Erro: Video de teste nao encontrado: $TEST_VIDEO${NC}"
    exit 1
fi

# Obter tokens
echo -e "${BLUE}Obtendo tokens dos 3 usuarios...${NC}"
TOKEN1=$(curl -s -X POST "$BASE_URL/api/autenticacao/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"teste@email.com","senha":"123456"}' | jq -r .token)

TOKEN2=$(curl -s -X POST "$BASE_URL/api/autenticacao/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"outro@email.com","senha":"123456"}' | jq -r .token)

TOKEN3=$(curl -s -X POST "$BASE_URL/api/autenticacao/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"novousuario@email.com","senha":"123456"}' | jq -r .token)

if [[ -z "$TOKEN1" || "$TOKEN1" == "null" ]] || \
   [[ -z "$TOKEN2" || "$TOKEN2" == "null" ]] || \
   [[ -z "$TOKEN3" || "$TOKEN3" == "null" ]]; then
    echo -e "${RED}Erro: Falha ao obter tokens${NC}"
    exit 1
fi

echo -e "${GREEN}Tokens obtidos com sucesso${NC}"
echo ""

# Funcao para enviar video e aguardar processamento
process_video() {
    local user_num=$1
    local token=$2
    local output_file=$3

    # Enviar video
    local response
    response=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token" \
        -F "video=@$TEST_VIDEO")

    local video_id
    video_id=$(echo "$response" | jq -r .id)

    if [[ -z "$video_id" || "$video_id" == "null" ]]; then
        echo "User$user_num:ERRO:envio_falhou" > "$output_file"
        return 1
    fi

    echo "User$user_num: Video $video_id enviado" >&2

    # Aguardar processamento (max 30 tentativas, 1s intervalo)
    for i in $(seq 1 30); do
        local status
        status=$(curl -s "$BASE_URL/api/videos" \
            -H "Authorization: Bearer $token" | \
            jq -r ".[] | select(.id == $video_id) | .status")

        if [[ "$status" == "CONCLUIDO" || "$status" == "FALHA" ]]; then
            echo "User$user_num:$video_id:$status" > "$output_file"
            return 0
        fi
        sleep 1
    done

    echo "User$user_num:$video_id:TIMEOUT" > "$output_file"
}

# Arquivos temporarios para resultados
TMP_DIR=$(mktemp -d)
TMP1="$TMP_DIR/result1"
TMP2="$TMP_DIR/result2"
TMP3="$TMP_DIR/result3"

echo -e "${BLUE}Enviando 3 videos simultaneamente...${NC}"
START=$(date +%s)

# Executar em paralelo
process_video 1 "$TOKEN1" "$TMP1" &
PID1=$!
process_video 2 "$TOKEN2" "$TMP2" &
PID2=$!
process_video 3 "$TOKEN3" "$TMP3" &
PID3=$!

echo "Aguardando processamento (PIDs: $PID1, $PID2, $PID3)..."

# Aguardar todos
wait $PID1 $PID2 $PID3

END=$(date +%s)
ELAPSED=$((END - START))

echo ""
echo -e "${BLUE}=== RESULTADOS ===${NC}"

# Mostrar resultados
R1=$(cat "$TMP1" 2>/dev/null || echo "User1:ERRO:arquivo_nao_encontrado")
R2=$(cat "$TMP2" 2>/dev/null || echo "User2:ERRO:arquivo_nao_encontrado")
R3=$(cat "$TMP3" 2>/dev/null || echo "User3:ERRO:arquivo_nao_encontrado")

echo "  $R1"
echo "  $R2"
echo "  $R3"
echo "  Tempo total: ${ELAPSED}s"

# Extrair status
S1=$(echo "$R1" | cut -d: -f3)
S2=$(echo "$R2" | cut -d: -f3)
S3=$(echo "$R3" | cut -d: -f3)

echo ""
if [[ "$S1" == "CONCLUIDO" && "$S2" == "CONCLUIDO" && "$S3" == "CONCLUIDO" ]]; then
    if [[ $ELAPSED -lt 10 ]]; then
        echo -e "${GREEN}SUCESSO! 3 videos processados em paralelo em ${ELAPSED}s${NC}"
        echo -e "${GREEN}Tempo < 10s indica processamento paralelo real!${NC}"
    else
        echo -e "${YELLOW}SUCESSO com aviso: Tempo de ${ELAPSED}s pode indicar processamento parcialmente sequencial${NC}"
    fi
else
    echo -e "${RED}FALHA: Nem todos os videos foram processados corretamente${NC}"
    exit 1
fi

# Limpar
rm -rf "$TMP_DIR"
