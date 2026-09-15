#!/bin/sh
set -eu

GRAYLOG_URL="${GRAYLOG_URL:-http://graylog:9000}"
AUTH="${GRAYLOG_USER:-admin}:${GRAYLOG_PASSWORD:-admin}"

echo "Aguardando Graylog ficar ALIVE..."
i=0
while [ "$i" -lt 90 ]; do
  if curl -sf -u "$AUTH" "$GRAYLOG_URL/api/system/lbstatus" | grep -qi ALIVE; then
    echo "Graylog ALIVE"
    break
  fi
  i=$((i + 1))
  sleep 2
done

sleep 5

if curl -sf -u "$AUTH" "$GRAYLOG_URL/api/system/inputs" | grep -q "GELFTCPInput"; then
  echo "Input GELF TCP ja existe"
  exit 0
fi

echo "Criando input GELF TCP na porta 12201..."
i=0
while [ "$i" -lt 20 ]; do
  code=$(curl -sS -o /tmp/graylog-input.json -w "%{http_code}" -u "$AUTH" \
    -H "Content-Type: application/json" \
    -H "X-Requested-By: docker-compose" \
    -X POST "$GRAYLOG_URL/api/system/inputs" \
    -d '{"title":"GELF TCP","type":"org.graylog2.inputs.gelf.tcp.GELFTCPInput","global":true,"configuration":{"bind_address":"0.0.0.0","port":12201,"recv_buffer_size":1048576,"decompress_size_limit":8388608,"max_message_size":2097152,"tls_enable":false,"use_null_delimiter":true,"number_worker_threads":2}}' || true)
  if [ "$code" = "201" ] || [ "$code" = "200" ]; then
    echo "Input GELF TCP criado"
    exit 0
  fi
  if curl -sf -u "$AUTH" "$GRAYLOG_URL/api/system/inputs" | grep -q "GELFTCPInput"; then
    echo "Input GELF TCP ja existe"
    exit 0
  fi
  i=$((i + 1))
  sleep 3
done

echo "Falha ao criar input GELF TCP"
cat /tmp/graylog-input.json || true
exit 1
