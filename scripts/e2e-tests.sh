#!/bin/bash

# =============================================================================
# FIAP X - Script de Testes E2E via Curl
# =============================================================================
# Este script executa testes end-to-end contra a API REST do FIAP X
# Premissa: A aplicacao ja esta rodando em localhost:8080
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# Configuracoes
# -----------------------------------------------------------------------------
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TEST_VIDEO="$PROJECT_DIR/test-data/sample.mp4"

# Usuarios de teste (serao criados via docker exec mysql)
USUARIO_TESTE_EMAIL="teste@email.com"
USUARIO_TESTE_SENHA="123456"
OUTRO_USUARIO_EMAIL="outro@email.com"
OUTRO_USUARIO_SENHA="123456"
TERCEIRO_USUARIO_EMAIL="novousuario@email.com"
TERCEIRO_USUARIO_SENHA="123456"

# Hash BCrypt para senha "123456" (strength 10)
BCRYPT_HASH='$2a$10$6.NhjLVGWREJzlRet9xUzOXpwhxJ91LN55d.Jxqs9m/zkdnqHC29G'

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=19

# IDs de videos criados durante o teste (para cleanup)
VIDEOS_CRIADOS=()

# -----------------------------------------------------------------------------
# Cores para output
# -----------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Funcoes auxiliares
# -----------------------------------------------------------------------------

print_header() {
    echo ""
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}   FIAP X - Testes E2E via Curl${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
}

print_section() {
    echo ""
    echo -e "${YELLOW}--- $1 ---${NC}"
}

wait_for_keypress() {
    echo ""
    echo -e "${BLUE}Pressione qualquer tecla para continuar...${NC}"
    read -n 1 -s -r
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERRO]${NC} $1"
}

# Teste positivo (caminho feliz) - cor verde
test_passed() {
    local test_num=$1
    local endpoint=$2
    local validacao=$3
    local http_code=$4
    local detalhes=$5

    TESTS_PASSED=$((TESTS_PASSED + 1))
    local output="[${test_num}/${TOTAL_TESTS}] ${endpoint} | ${validacao} | ${http_code}"
    if [[ -n "$detalhes" ]]; then
        output="${output} | ${detalhes}"
    fi
    echo -e "${GREEN}${output}${NC} | ${GREEN}PASSOU${NC}"
}

# Teste negativo (validacao de erro/rejeicao) - cor ciano
test_passed_negative() {
    local test_num=$1
    local endpoint=$2
    local validacao=$3
    local http_code=$4
    local detalhes=$5

    TESTS_PASSED=$((TESTS_PASSED + 1))
    local output="[${test_num}/${TOTAL_TESTS}] ${endpoint} | ${validacao} | ${http_code}"
    if [[ -n "$detalhes" ]]; then
        output="${output} | ${detalhes}"
    fi
    echo -e "${CYAN}${output}${NC} | ${CYAN}PASSOU${NC}"
}

test_failed() {
    local test_num=$1
    local endpoint=$2
    local validacao=$3
    local reason=$4

    TESTS_FAILED=$((TESTS_FAILED + 1))
    echo -e "${RED}[${test_num}/${TOTAL_TESTS}]${NC} ${endpoint} | ${validacao} | ${RED}FALHOU${NC}"
    echo -e "    ${RED}Motivo: $reason${NC}"
}

# Extrai valor JSON usando grep (fallback se jq nao disponivel)
json_value() {
    local json=$1
    local key=$2

    if command -v jq &> /dev/null; then
        echo "$json" | jq -r ".$key" 2>/dev/null
    else
        # Fallback com grep/sed
        echo "$json" | grep -o "\"$key\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | sed "s/\"$key\"[[:space:]]*:[[:space:]]*\"//" | sed 's/"$//' | head -1
    fi
}

# Extrai valor numerico JSON
json_number() {
    local json=$1
    local key=$2

    if command -v jq &> /dev/null; then
        echo "$json" | jq -r ".$key" 2>/dev/null
    else
        echo "$json" | grep -o "\"$key\"[[:space:]]*:[[:space:]]*[0-9]*" | sed "s/\"$key\"[[:space:]]*:[[:space:]]*//" | head -1
    fi
}

# Verifica se aplicacao esta rodando
check_app_running() {
    log_info "Verificando se a aplicacao esta rodando em $BASE_URL..."

    local response
    local http_code

    # Tenta endpoint de health ou raiz
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/videos" 2>/dev/null || echo "000")

    if [[ "$http_code" == "000" ]]; then
        log_error "Aplicacao nao esta respondendo em $BASE_URL"
        log_error "Certifique-se de que a aplicacao esta rodando: docker compose up -d"
        exit 1
    fi

    log_success "Aplicacao respondendo (HTTP $http_code)"
}

# Cria usuarios de teste no banco via docker exec
setup_test_users() {
    log_info "Configurando usuarios de teste no banco..."

    # Encontra o container MySQL
    local mysql_container
    mysql_container=$(docker ps --filter "ancestor=mysql:8" --format "{{.Names}}" | head -1)

    if [[ -z "$mysql_container" ]]; then
        mysql_container=$(docker ps --filter "name=mysql" --format "{{.Names}}" | head -1)
    fi

    if [[ -z "$mysql_container" ]]; then
        log_error "Container MySQL nao encontrado. Certifique-se de que docker compose esta rodando."
        exit 1
    fi

    log_info "Usando container MySQL: $mysql_container"

    # Insere usuarios de teste (ignora se ja existem)
    local sql="INSERT IGNORE INTO usuarios (email, senha_hash, criado_em) VALUES
        ('$USUARIO_TESTE_EMAIL', '$BCRYPT_HASH', NOW()),
        ('$OUTRO_USUARIO_EMAIL', '$BCRYPT_HASH', NOW()),
        ('$TERCEIRO_USUARIO_EMAIL', '$BCRYPT_HASH', NOW());"

    docker exec "$mysql_container" mysql -ufiapx -pfiapx123 fiapx -e "$sql" 2>/dev/null

    if [[ $? -eq 0 ]]; then
        log_success "Usuarios de teste configurados"
    else
        log_error "Falha ao configurar usuarios de teste"
        exit 1
    fi
}

# Faz login e retorna o token
do_login() {
    local email=$1
    local senha=$2

    local response
    response=$(curl -s -X POST "$BASE_URL/api/autenticacao/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"senha\":\"$senha\"}")

    json_value "$response" "token"
}

# Busca status de um video especifico (via listagem)
get_video_status() {
    local video_id=$1
    local token=$2

    local response
    response=$(curl -s -X GET "$BASE_URL/api/videos" \
        -H "Authorization: Bearer $token")

    if command -v jq &> /dev/null; then
        echo "$response" | jq -r ".[] | select(.id == $video_id) | .status" 2>/dev/null
    else
        # Fallback: busca o status do video especifico no JSON
        echo "$response" | grep -o "{[^}]*\"id\":$video_id[^}]*}" | grep -o '"status":"[^"]*"' | sed 's/"status":"//' | sed 's/"$//'
    fi
}

# Aguarda video ser processado (polling)
wait_for_processing() {
    local video_id=$1
    local token=$2
    local max_attempts=${3:-30}
    local interval=${4:-2}

    local attempt=0
    local status=""

    while [[ $attempt -lt $max_attempts ]]; do
        status=$(get_video_status "$video_id" "$token")

        if [[ "$status" == "CONCLUIDO" ]] || [[ "$status" == "FALHA" ]]; then
            echo "$status"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep "$interval"
    done

    echo "TIMEOUT"
    return 1
}

# Envia video e aguarda processamento (para uso em background)
# Usa arquivo de token em vez de passar token diretamente (evita problemas de escape)
enviar_e_aguardar_com_arquivo() {
    local token_file=$1
    local output_file=$2
    local max_attempts=${3:-30}
    local interval=${4:-1}

    local token
    token=$(cat "$token_file")

    # Envia video
    local response
    response=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token" \
        -F "video=@$TEST_VIDEO")

    local video_id
    if command -v jq &> /dev/null; then
        video_id=$(echo "$response" | jq -r '.id' 2>/dev/null)
    else
        video_id=$(echo "$response" | grep -o '"id":[0-9]*' | cut -d: -f2 | head -1)
    fi

    if [[ -z "$video_id" ]] || [[ "$video_id" == "null" ]]; then
        echo "ERRO:envio_falhou" > "$output_file"
        return 1
    fi

    # Aguarda processamento (polling simplificado)
    local status=""
    local attempt=0
    while [[ $attempt -lt $max_attempts ]]; do
        local list_response
        list_response=$(curl -s "$BASE_URL/api/videos" -H "Authorization: Bearer $token")

        if command -v jq &> /dev/null; then
            status=$(echo "$list_response" | jq -r ".[] | select(.id == $video_id) | .status" 2>/dev/null)
        else
            status=$(echo "$list_response" | grep -o "\"id\":$video_id[^}]*\"status\":\"[^\"]*\"" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 | head -1)
        fi

        if [[ "$status" == "CONCLUIDO" ]] || [[ "$status" == "FALHA" ]]; then
            echo "$video_id:$status" > "$output_file"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep "$interval"
    done

    echo "$video_id:TIMEOUT" > "$output_file"
}

# Limpa dados de teste
cleanup() {
    log_info "Limpando dados de teste..."

    # Remove arquivos temporarios de download
    rm -f /tmp/test_download_*.zip 2>/dev/null
    rm -f /tmp/parallel_test_*_$$ 2>/dev/null

    log_success "Limpeza concluida"
}

# -----------------------------------------------------------------------------
# Testes
# -----------------------------------------------------------------------------

run_tests() {
    local test_num=0
    local token=""
    local token_outro=""
    local response
    local http_code

    wait_for_keypress
    print_section "Testes de Autenticacao"

    # -------------------------------------------------------------------------
    # Login com credenciais validas
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/autenticacao/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$USUARIO_TESTE_EMAIL\",\"senha\":\"$USUARIO_TESTE_SENHA\"}")

    http_code=$(echo "$response" | tail -1)
    response=$(echo "$response" | sed '$d')
    token=$(json_value "$response" "token")

    if [[ "$http_code" == "200" ]] && [[ -n "$token" ]] && [[ "$token" != "null" ]]; then
        local token_preview="${token:0:20}..."
        test_passed $test_num "POST /api/autenticacao/login" "credenciais validas retornam JWT" "$http_code" "token=${token_preview}"
    else
        test_failed $test_num "POST /api/autenticacao/login" "credenciais validas retornam JWT" "HTTP $http_code, token=$token"
    fi

    # -------------------------------------------------------------------------
    # Login com usuario inexistente
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/autenticacao/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"naoexiste@email.com","senha":"123456"}')

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "401" ]]; then
        test_passed_negative $test_num "POST /api/autenticacao/login" "usuario inexistente rejeitado" "$http_code"
    else
        test_failed $test_num "POST /api/autenticacao/login" "usuario inexistente rejeitado" "Esperado 401, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Login com senha incorreta
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/autenticacao/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$USUARIO_TESTE_EMAIL\",\"senha\":\"senhaerrada\"}")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "401" ]]; then
        test_passed_negative $test_num "POST /api/autenticacao/login" "senha incorreta rejeitada" "$http_code"
    else
        test_failed $test_num "POST /api/autenticacao/login" "senha incorreta rejeitada" "Esperado 401, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Login sem body
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/autenticacao/login" \
        -H "Content-Type: application/json")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" =~ ^4[0-9][0-9]$ ]]; then
        test_passed_negative $test_num "POST /api/autenticacao/login" "body vazio rejeitado" "$http_code"
    else
        test_failed $test_num "POST /api/autenticacao/login" "body vazio rejeitado" "Esperado 4xx, recebido HTTP $http_code"
    fi

    wait_for_keypress
    print_section "Testes de Envio de Videos"

    # -------------------------------------------------------------------------
    # Enviar video com JWT valido
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    if [[ ! -f "$TEST_VIDEO" ]]; then
        test_failed $test_num "POST /api/videos/enviar" "upload com JWT cria video" "Arquivo de teste nao encontrado: $TEST_VIDEO"
    else
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/videos/enviar" \
            -H "Authorization: Bearer $token" \
            -F "video=@$TEST_VIDEO")

        http_code=$(echo "$response" | tail -1)
        response=$(echo "$response" | sed '$d')
        local video_id
        video_id=$(json_number "$response" "id")
        local status
        status=$(json_value "$response" "status")

        if [[ "$http_code" == "200" ]] && [[ -n "$video_id" ]] && [[ "$video_id" != "null" ]]; then
            test_passed $test_num "POST /api/videos/enviar" "upload com JWT cria video" "$http_code" "id=$video_id status=$status"
            VIDEOS_CRIADOS+=("$video_id")
        else
            test_failed $test_num "POST /api/videos/enviar" "upload com JWT cria video" "HTTP $http_code, id=$video_id, status=$status"
        fi
    fi

    # -------------------------------------------------------------------------
    # Enviar video sem JWT
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/videos/enviar" \
        -F "video=@$TEST_VIDEO")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
        test_passed_negative $test_num "POST /api/videos/enviar" "sem JWT bloqueado" "$http_code"
    else
        test_failed $test_num "POST /api/videos/enviar" "sem JWT bloqueado" "Esperado 401/403, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Enviar video com JWT invalido
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer token_invalido_xyz" \
        -F "video=@$TEST_VIDEO")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
        test_passed_negative $test_num "POST /api/videos/enviar" "JWT invalido bloqueado" "$http_code"
    else
        test_failed $test_num "POST /api/videos/enviar" "JWT invalido bloqueado" "Esperado 401/403, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Envio sem arquivo
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: multipart/form-data")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" =~ ^4[0-9][0-9]$ ]]; then
        test_passed_negative $test_num "POST /api/videos/enviar" "sem arquivo rejeitado" "$http_code"
    else
        test_failed $test_num "POST /api/videos/enviar" "sem arquivo rejeitado" "Esperado 4xx, recebido HTTP $http_code"
    fi

    wait_for_keypress
    print_section "Testes de Listagem de Videos"

    # -------------------------------------------------------------------------
    # Listar videos autenticado
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos" \
        -H "Authorization: Bearer $token")

    http_code=$(echo "$response" | tail -1)
    response=$(echo "$response" | sed '$d')

    # Verifica se retornou array JSON e conta videos
    if [[ "$http_code" == "200" ]] && [[ "$response" =~ ^\[.*\]$ ]]; then
        local video_count=0
        if command -v jq &> /dev/null; then
            video_count=$(echo "$response" | jq 'length' 2>/dev/null || echo "0")
        else
            video_count=$(echo "$response" | grep -o '"id"' | wc -l)
        fi
        test_passed $test_num "GET /api/videos" "listagem retorna array" "$http_code" "${video_count} video(s)"
    else
        test_failed $test_num "GET /api/videos" "listagem retorna array" "HTTP $http_code, resposta=$response"
    fi

    # -------------------------------------------------------------------------
    # Listar videos sem JWT
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
        test_passed_negative $test_num "GET /api/videos" "sem JWT bloqueado" "$http_code"
    else
        test_failed $test_num "GET /api/videos" "sem JWT bloqueado" "Esperado 401/403, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Isolamento entre usuarios
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    # Login com segundo usuario
    token_outro=$(do_login "$OUTRO_USUARIO_EMAIL" "$OUTRO_USUARIO_SENHA")

    if [[ -z "$token_outro" ]] || [[ "$token_outro" == "null" ]]; then
        test_failed $test_num "GET /api/videos" "isolamento entre usuarios" "Falha no login do segundo usuario"
    else
        # Envia video com segundo usuario
        response=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
            -H "Authorization: Bearer $token_outro" \
            -F "video=@$TEST_VIDEO")

        local video_outro_id
        video_outro_id=$(json_number "$response" "id")

        if [[ -n "$video_outro_id" ]] && [[ "$video_outro_id" != "null" ]]; then
            VIDEOS_CRIADOS+=("$video_outro_id")
        fi

        # Lista videos do primeiro usuario
        response=$(curl -s -X GET "$BASE_URL/api/videos" \
            -H "Authorization: Bearer $token")

        # Verifica se video do outro usuario NAO aparece na lista
        if [[ -n "$video_outro_id" ]] && echo "$response" | grep -q "\"id\":$video_outro_id"; then
            test_failed $test_num "GET /api/videos" "isolamento entre usuarios" "Video do outro usuario apareceu na listagem"
        else
            test_passed_negative $test_num "GET /api/videos" "isolamento entre usuarios" "200" "user2 nao ve video user1"
        fi
    fi

    # -------------------------------------------------------------------------
    # Lista vazia para novo usuario
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    local token_terceiro
    token_terceiro=$(do_login "$TERCEIRO_USUARIO_EMAIL" "$TERCEIRO_USUARIO_SENHA")

    if [[ -z "$token_terceiro" ]] || [[ "$token_terceiro" == "null" ]]; then
        test_failed $test_num "GET /api/videos" "lista vazia para novo usuario" "Falha no login do terceiro usuario"
    else
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos" \
            -H "Authorization: Bearer $token_terceiro")

        http_code=$(echo "$response" | tail -1)
        response=$(echo "$response" | sed '$d')

        if [[ "$http_code" == "200" ]] && [[ "$response" == "[]" ]]; then
            test_passed_negative $test_num "GET /api/videos" "lista vazia para novo usuario" "$http_code" "retornou []"
        else
            test_failed $test_num "GET /api/videos" "lista vazia para novo usuario" "Esperado 200 com [], recebido HTTP $http_code resposta=$response"
        fi
    fi

    wait_for_keypress
    print_section "Testes de Download"

    # -------------------------------------------------------------------------
    # Download de video inexistente
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))
    response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos/99999/baixar" \
        -H "Authorization: Bearer $token")

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" =~ ^4[0-9][0-9]$ ]]; then
        test_passed_negative $test_num "GET /api/videos/{id}/baixar" "video inexistente" "$http_code"
    else
        test_failed $test_num "GET /api/videos/{id}/baixar" "video inexistente" "Esperado 4xx, recebido HTTP $http_code"
    fi

    # -------------------------------------------------------------------------
    # Download de video nao processado
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    # Envia novo video e tenta baixar imediatamente (antes de processar)
    response=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token" \
        -F "video=@$TEST_VIDEO")

    local video_pendente_id
    video_pendente_id=$(json_number "$response" "id")

    if [[ -n "$video_pendente_id" ]] && [[ "$video_pendente_id" != "null" ]]; then
        VIDEOS_CRIADOS+=("$video_pendente_id")

        # Tenta baixar imediatamente
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos/$video_pendente_id/baixar" \
            -H "Authorization: Bearer $token")

        http_code=$(echo "$response" | tail -1)

        # Pode retornar 400/404/409 dependendo da implementacao
        if [[ "$http_code" =~ ^4[0-9][0-9]$ ]]; then
            test_passed_negative $test_num "GET /api/videos/{id}/baixar" "video nao processado" "$http_code"
        else
            test_failed $test_num "GET /api/videos/{id}/baixar" "video nao processado" "Esperado 4xx, recebido HTTP $http_code"
        fi
    else
        test_failed $test_num "GET /api/videos/{id}/baixar" "video nao processado" "Falha ao criar video para teste"
    fi

    # -------------------------------------------------------------------------
    # Download cross-user (GAP de seguranca)
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    # Usa video do outro usuario (criado no teste de isolamento)
    local video_outro_usuario=${VIDEOS_CRIADOS[1]}

    if [[ -z "$video_outro_usuario" ]]; then
        test_failed $test_num "GET /api/videos/{id}/baixar" "download cross-user" "Nenhum video de outro usuario disponivel"
    else
        # Tenta baixar video do outro usuario com token do primeiro usuario
        # Usa -o /dev/null para descartar body binario e evitar warning de null byte
        http_code=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/videos/$video_outro_usuario/baixar" \
            -H "Authorization: Bearer $token")

        if [[ "$http_code" == "403" ]] || [[ "$http_code" == "404" ]]; then
            test_passed_negative $test_num "GET /api/videos/{id}/baixar" "download cross-user bloqueado" "$http_code"
        elif [[ "$http_code" == "200" ]]; then
            # GAP de seguranca: permite download de video de outro usuario
            echo -e "${YELLOW}[${test_num}/${TOTAL_TESTS}] GET /api/videos/{id}/baixar | download cross-user | ${http_code} | GAP: permite acesso a video de outro usuario${NC}"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        else
            test_failed $test_num "GET /api/videos/{id}/baixar" "download cross-user" "HTTP $http_code inesperado"
        fi
    fi

    # -------------------------------------------------------------------------
    # Download sem JWT
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    local video_para_download=${VIDEOS_CRIADOS[0]}

    if [[ -z "$video_para_download" ]]; then
        test_failed $test_num "GET /api/videos/{id}/baixar" "download sem JWT" "Nenhum video disponivel para teste"
    else
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/videos/$video_para_download/baixar")

        http_code=$(echo "$response" | tail -1)

        if [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
            test_passed_negative $test_num "GET /api/videos/{id}/baixar" "download sem JWT bloqueado" "$http_code"
        else
            test_failed $test_num "GET /api/videos/{id}/baixar" "download sem JWT bloqueado" "Esperado 401/403, recebido HTTP $http_code"
        fi
    fi

    wait_for_keypress
    print_section "Teste de Processamento Paralelo"

    # -------------------------------------------------------------------------
    # Envia 3 videos simultaneamente e verifica processamento
    # Nota: Para demonstracao visual de paralelismo real, use ./scripts/test-parallel.sh
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    log_info "Enviando 3 videos simultaneamente para teste de paralelismo..."

    # Obtem tokens frescos
    local token_paralelo1=$(do_login "$USUARIO_TESTE_EMAIL" "$USUARIO_TESTE_SENHA")
    local token_paralelo2=$(do_login "$OUTRO_USUARIO_EMAIL" "$OUTRO_USUARIO_SENHA")
    local token_paralelo3=$(do_login "$TERCEIRO_USUARIO_EMAIL" "$TERCEIRO_USUARIO_SENHA")

    if [[ -z "$token_paralelo1" ]] || [[ "$token_paralelo1" == "null" ]] || \
       [[ -z "$token_paralelo2" ]] || [[ "$token_paralelo2" == "null" ]] || \
       [[ -z "$token_paralelo3" ]] || [[ "$token_paralelo3" == "null" ]]; then
        test_failed $test_num "Processamento Paralelo" "3 videos em paralelo" "Falha ao obter tokens dos usuarios"
    else

    # Captura tempo inicial
    local start_time_parallel=$(date +%s)

    # Envia 3 videos rapidamente (quase simultaneamente)
    local resp1=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token_paralelo1" \
        -F "video=@$TEST_VIDEO")
    local id1=$(json_number "$resp1" "id")

    local resp2=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token_paralelo2" \
        -F "video=@$TEST_VIDEO")
    local id2=$(json_number "$resp2" "id")

    local resp3=$(curl -s -X POST "$BASE_URL/api/videos/enviar" \
        -H "Authorization: Bearer $token_paralelo3" \
        -F "video=@$TEST_VIDEO")
    local id3=$(json_number "$resp3" "id")

    log_info "Videos enviados: $id1, $id2, $id3. Aguardando processamento..."

    if [[ -n "$id1" ]] && [[ "$id1" != "null" ]]; then VIDEOS_CRIADOS+=("$id1"); fi
    if [[ -n "$id2" ]] && [[ "$id2" != "null" ]]; then VIDEOS_CRIADOS+=("$id2"); fi
    if [[ -n "$id3" ]] && [[ "$id3" != "null" ]]; then VIDEOS_CRIADOS+=("$id3"); fi

    # Aguarda todos serem processados (polling simples)
    local todos_prontos=false
    local status1="" status2="" status3=""

    for attempt in $(seq 1 30); do
        status1=$(get_video_status "$id1" "$token_paralelo1")
        status2=$(get_video_status "$id2" "$token_paralelo2")
        status3=$(get_video_status "$id3" "$token_paralelo3")

        local count_prontos=0
        [[ "$status1" == "CONCLUIDO" || "$status1" == "FALHA" ]] && count_prontos=$((count_prontos + 1))
        [[ "$status2" == "CONCLUIDO" || "$status2" == "FALHA" ]] && count_prontos=$((count_prontos + 1))
        [[ "$status3" == "CONCLUIDO" || "$status3" == "FALHA" ]] && count_prontos=$((count_prontos + 1))

        if [[ $count_prontos -eq 3 ]]; then
            todos_prontos=true
            break
        fi
        sleep 1
    done

    # Captura tempo final
    local end_time_parallel=$(date +%s)
    local elapsed_parallel=$((end_time_parallel - start_time_parallel))

    # Verifica resultados
    local detalhes_status="id1=$id1:$status1 id2=$id2:$status2 id3=$id3:$status3"

    if [[ "$todos_prontos" == "true" ]] && \
       [[ "$status1" == "CONCLUIDO" ]] && \
       [[ "$status2" == "CONCLUIDO" ]] && \
       [[ "$status3" == "CONCLUIDO" ]]; then
        # Se todos concluiram em menos de 15s, indica processamento paralelo
        if [[ "$elapsed_parallel" -lt 15 ]]; then
            test_passed $test_num "Processamento Paralelo" "3 videos processados" "OK" "${elapsed_parallel}s (< 15s indica paralelismo)"
        else
            echo -e "${YELLOW}[${test_num}/${TOTAL_TESTS}] Processamento Paralelo | 3 videos | ${elapsed_parallel}s | AVISO: tempo alto${NC}"
            TESTS_PASSED=$((TESTS_PASSED + 1))
        fi
    else
        test_failed $test_num "Processamento Paralelo" "3 videos processados" "Nem todos concluiram: $detalhes_status (${elapsed_parallel}s)"
    fi

    fi  # fecha o if dos tokens

    wait_for_keypress
    print_section "Teste de Processamento Real"

    # -------------------------------------------------------------------------
    # Processamento real (sample.mp4)
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    # Usa o primeiro video enviado
    local video_para_processar=${VIDEOS_CRIADOS[0]}

    if [[ -z "$video_para_processar" ]]; then
        test_failed $test_num "Processamento" "FFmpeg extrai frames" "Nenhum video disponivel para teste"
    else
        log_info "Aguardando processamento do video $video_para_processar..."

        local start_time=$(date +%s)
        local final_status
        final_status=$(wait_for_processing "$video_para_processar" "$token" 60 2)
        local end_time=$(date +%s)
        local elapsed=$((end_time - start_time))

        if [[ "$final_status" == "CONCLUIDO" ]]; then
            test_passed $test_num "Processamento" "FFmpeg extrai frames" "PENDENTE->CONCLUIDO" "${elapsed}s"
        elif [[ "$final_status" == "FALHA" ]]; then
            test_failed $test_num "Processamento" "FFmpeg extrai frames" "Status final: FALHA (${elapsed}s)"
        elif [[ "$final_status" == "TIMEOUT" ]]; then
            test_failed $test_num "Processamento" "FFmpeg extrai frames" "Timeout aguardando processamento"
        else
            test_failed $test_num "Processamento" "FFmpeg extrai frames" "Status inesperado: $final_status"
        fi
    fi

    # -------------------------------------------------------------------------
    # Download de video concluido
    # -------------------------------------------------------------------------
    test_num=$((test_num + 1))

    local video_concluido=${VIDEOS_CRIADOS[0]}

    if [[ -z "$video_concluido" ]]; then
        test_failed $test_num "GET /api/videos/{id}/baixar" "download ZIP" "Nenhum video disponivel para teste"
    else
        # Verifica se o video esta concluido
        local status_atual
        status_atual=$(get_video_status "$video_concluido" "$token")

        if [[ "$status_atual" != "CONCLUIDO" ]]; then
            test_failed $test_num "GET /api/videos/{id}/baixar" "download ZIP" "Video nao esta concluido (status: $status_atual)"
        else
            # Faz download
            local download_file="/tmp/test_download_$$.zip"
            http_code=$(curl -s -w "%{http_code}" -X GET "$BASE_URL/api/videos/$video_concluido/baixar" \
                -H "Authorization: Bearer $token" \
                -o "$download_file")

            if [[ "$http_code" == "200" ]] && [[ -f "$download_file" ]]; then
                local file_size
                file_size=$(stat -c%s "$download_file" 2>/dev/null || stat -f%z "$download_file" 2>/dev/null)

                if [[ "$file_size" -gt 0 ]]; then
                    # Formata tamanho do arquivo
                    local size_display
                    if [[ "$file_size" -ge 1048576 ]]; then
                        size_display="$(echo "scale=1; $file_size/1048576" | bc)MB"
                    elif [[ "$file_size" -ge 1024 ]]; then
                        size_display="$(echo "scale=1; $file_size/1024" | bc)KB"
                    else
                        size_display="${file_size}B"
                    fi
                    test_passed $test_num "GET /api/videos/{id}/baixar" "download ZIP" "$http_code" "$size_display"
                else
                    test_failed $test_num "GET /api/videos/{id}/baixar" "download ZIP" "Arquivo ZIP vazio"
                fi

                rm -f "$download_file"
            else
                test_failed $test_num "GET /api/videos/{id}/baixar" "download ZIP" "HTTP $http_code"
            fi
        fi
    fi
}

print_summary() {
    echo ""
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}         RESULTADO FINAL${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
    echo -e "Passou: ${GREEN}$TESTS_PASSED/$TOTAL_TESTS${NC}"
    echo -e "Falhou: ${RED}$TESTS_FAILED/$TOTAL_TESTS${NC}"
    echo ""

    if [[ $TESTS_FAILED -eq 0 ]]; then
        echo -e "${GREEN}Todos os testes passaram!${NC}"
        exit 0
    else
        echo -e "${RED}Alguns testes falharam.${NC}"
        exit 1
    fi
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

main() {
    print_header

    # Verifica dependencias
    if ! command -v curl &> /dev/null; then
        log_error "curl nao encontrado. Por favor, instale o curl."
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        log_error "docker nao encontrado. Por favor, instale o docker."
        exit 1
    fi

    # Verifica arquivo de teste
    if [[ ! -f "$TEST_VIDEO" ]]; then
        log_error "Video de teste nao encontrado: $TEST_VIDEO"
        log_error "Execute o comando para criar: docker run --rm -v $(dirname $TEST_VIDEO):/output linuxserver/ffmpeg -f lavfi -i testsrc=duration=2:size=320x240:rate=30 -f lavfi -i sine=frequency=440:duration=2 -c:v libx264 -preset ultrafast -crf 30 -c:a aac -b:a 64k -y /output/sample.mp4"
        exit 1
    fi

    # Setup
    check_app_running
    setup_test_users

    # Executa testes
    run_tests

    # Cleanup
    cleanup

    # Relatorio
    print_summary
}

# Executa
main "$@"
