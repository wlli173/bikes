#!/bin/sh
# =============================================================================
# Init script — cria o input GELF UDP via API REST do Graylog (idempotente).
# Plano A: roda como service_completed_successfully antes da app subir.
# =============================================================================

set -e

GRAYLOG_API="${GRAYLOG_API:-http://graylog:9000/api}"
GRAYLOG_USER="${GRAYLOG_USER:-admin}"
GRAYLOG_PASS="${GRAYLOG_PASS:-admin}"
INPUT_TITLE="GELF UDP Input"
MAX_RETRIES=60
RETRY_INTERVAL=5

echo "[init-gelf] Aguardando Graylog ficar disponível em ${GRAYLOG_API}..."

# Aguarda a API do Graylog estar respondendo
retries=0
until curl -sf -u "${GRAYLOG_USER}:${GRAYLOG_PASS}" "${GRAYLOG_API}/system/lbstatus" > /dev/null 2>&1; do
  retries=$((retries + 1))
  if [ "$retries" -ge "$MAX_RETRIES" ]; then
    echo "[init-gelf] ERRO: Graylog não ficou disponível após $((MAX_RETRIES * RETRY_INTERVAL))s"
    exit 1
  fi
  echo "[init-gelf] Tentativa ${retries}/${MAX_RETRIES}... aguardando ${RETRY_INTERVAL}s"
  sleep "$RETRY_INTERVAL"
done

echo "[init-gelf] Graylog está respondendo. Verificando se input '${INPUT_TITLE}' já existe..."

# Verifica se o input já existe (idempotente)
EXISTING=$(curl -sf -u "${GRAYLOG_USER}:${GRAYLOG_PASS}" \
  -H "Accept: application/json" \
  "${GRAYLOG_API}/system/inputs" | grep -c "${INPUT_TITLE}" || true)

if [ "$EXISTING" -gt 0 ]; then
  echo "[init-gelf] Input '${INPUT_TITLE}' já existe. Nada a fazer."
  exit 0
fi

echo "[init-gelf] Criando input '${INPUT_TITLE}'..."

# Cria o input GELF UDP — requer header X-Requested-By
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -u "${GRAYLOG_USER}:${GRAYLOG_PASS}" \
  -H "Content-Type: application/json" \
  -H "X-Requested-By: init-gelf-script" \
  -X POST "${GRAYLOG_API}/system/inputs" \
  -d '{
    "title": "'"${INPUT_TITLE}"'",
    "type": "org.graylog2.inputs.gelf.udp.GELFUDPInput",
    "configuration": {
      "bind_address": "0.0.0.0",
      "port": 12201,
      "recv_buffer_size": 262144,
      "decompress_size_limit": 8388608
    },
    "global": true
  }')

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
  echo "[init-gelf] Input criado com sucesso (HTTP ${HTTP_CODE})."
else
  echo "[init-gelf] ERRO: Falha ao criar input (HTTP ${HTTP_CODE})."
  exit 1
fi
