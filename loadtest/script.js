import http from "k6/http";
import { check, sleep } from "k6";
function randomString(length) {
  const charset = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let res = '';
  for (let i = 0; i < length; i++) res += charset[Math.floor(Math.random() * charset.length)];
  return res;
}

// ============================================================================
// Script de geração de carga para popular dashboards do Grafana e logs do Graylog.
// Perfil: docker compose --profile loadtest up loadtest
// ============================================================================

export const options = {
  stages: [
    { duration: "30s", target: 5 },
    { duration: "1m", target: 10 },
    { duration: "30s", target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || "http://app:8080";

export default function () {
  // POST — criar usuário
  const username = `user_${randomString(8)}`;
  const payload = JSON.stringify({
    username: username,
    password: "senha123",
  });
  const headers = { "Content-Type": "application/json" };

  const createRes = http.post(`${BASE_URL}/api/v1/usuarios`, payload, {
    headers,
  });
  check(createRes, {
    "POST /usuarios — 201": (r) => r.status === 201,
  });

  // GET — listar todos
  const listRes = http.get(`${BASE_URL}/api/v1/usuarios`);
  check(listRes, {
    "GET /usuarios — 200": (r) => r.status === 200,
  });

  // GET /{id} — buscar por ID (usa o ID do usuário criado)
  if (createRes.status === 201) {
    const userId = JSON.parse(createRes.body).id;

    const getRes = http.get(`${BASE_URL}/api/v1/usuarios/${userId}`);
    check(getRes, {
      "GET /usuarios/{id} — 200": (r) => r.status === 200,
    });

    // PATCH — alterar senha
    const patchRes = http.patch(
      `${BASE_URL}/api/v1/usuarios/${userId}`,
      JSON.stringify({ password: "novaSenha456" }),
      { headers }
    );
    check(patchRes, {
      "PATCH /usuarios/{id} — 200": (r) => r.status === 200,
    });
  }

  // GET /demo/log — gerar logs de todos os níveis
  http.get(`${BASE_URL}/api/v1/demo/log`);

  // GET /demo/log/error — gerar erro 500
  http.get(`${BASE_URL}/api/v1/demo/log/error`);

  // GET inexistente — gerar 404
  http.get(`${BASE_URL}/api/v1/usuarios/999999`);

  sleep(1);
}
