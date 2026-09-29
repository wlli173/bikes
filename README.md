# 🚲 Bikes — CRUD de Usuários com Observabilidade DevOps

Projeto acadêmico de DevOps do IFC. Aplicação CRUD de usuários em **Spring Boot 4.1.1 / Java 25**, com stack completa de observabilidade: monitoramento (Prometheus + Grafana), logs centralizados (Graylog + OpenSearch + MongoDB) e pipeline CI/CD (GitHub Actions).

---

## 📋 Divisão de Tarefas

| Integrante | Itens | Descrição |
|------------|-------|-----------|
| **Willighan** | 1 | Pipeline CI/CD (GitHub Actions + GHCR) — ✅ Concluído |
| **Lucas** | 2, 3 | Prometheus + Grafana (dashboards) |
| **Victor** | 4, 5, 6 | Graylog + docker-compose.yml + README |

> **Nota:** As dependências `spring-boot-starter-actuator` e `micrometer-registry-prometheus` foram adicionadas pelo Victor de forma mínima para que o compose funcione (healthcheck + Prometheus). Isso **invade a área do Lucas** e deve ser combinado antes do merge na branch principal.

---

## 🏗️ Arquitetura da Stack

```
┌────────────────────────────────────────────────────────────────┐
│                      docker compose up --build                  │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┤
│  MySQL   │ MongoDB  │OpenSearch│ Graylog  │Prometheus│ Grafana  │
│  8.0.39  │  6.0.20  │  2.15.0  │  6.1.16  │ v3.4.1   │ 11.6.0  │
│  :3306   │  :27017  │  :9200   │  :9000   │  :9090   │  :3000   │
│(internal)│(internal)│(internal)│  (host)  │  (host)  │  (host)  │
├──────────┴──────────┴──────┬───┴──────────┼──────────┴──────────┤
│                            │              │                      │
│   ┌─ graylog-init ─────┐  │              │                      │
│   │ Cria input GELF via │  │              │                      │
│   │ API REST (idempt.)  │  │              │                      │
│   └─────────┬───────────┘  │              │                      │
│             ▼              │              │                      │
│   ┌─── Bikes App ──────────┼──────────────┤                      │
│   │ Spring Boot 4.1.1      │ GELF UDP     │ /actuator/prometheus │
│   │ Java 25 (Alpine JRE)   │ :12201       │ scrape 5s            │
│   │ :8080 (host)           │              │                      │
│   └────────────────────────┴──────────────┴──────────────────────┘
│
│   ┌─── k6 loadtest ────┐  (perfil: loadtest)
│   │ grafana/k6:0.56.0   │  docker compose --profile loadtest up loadtest
│   └─────────────────────┘
└────────────────────────────────────────────────────────────────┘
```

### Tabela de Serviços

| Serviço | URL de Acesso | Porta Host | Credenciais |
|---------|--------------|------------|-------------|
| **Bikes App** | http://localhost:8080/api/v1/usuarios | 8080 | — |
| **Actuator Health** | http://localhost:8080/actuator/health | 8080 | — |
| **Prometheus** | http://localhost:9090 | 9090 | — |
| **Grafana** | http://localhost:3000 | 3000 | admin/admin (ou acesso anônimo como Viewer) |
| **Graylog** | http://localhost:9000 | 9000 | admin/admin |
| MySQL | — | não exposta | root/root |
| MongoDB | — | não exposta | — |
| OpenSearch | — | não exposta | segurança desabilitada |

---

## ⚙️ Pré-requisitos

| Requisito | Mínimo | Verificação |
|-----------|--------|-------------|
| Docker | 24+ | `docker --version` |
| Docker Compose | V2+ | `docker compose version` |
| Memória alocada ao Docker | **6 GB** (recomendado 8 GB) | Docker Desktop → Settings → Resources |
| vm.max_map_count (WSL2/Linux) | 262144 | Ver seção Troubleshooting |

---

## 🚀 Execução

### Comando Único

```bash
docker compose up --build
```

> **Tempo estimado de subida**: Primeira execução (build + download de 2.5GB de imagens): ~6 minutos. Execuções subsequentes (cache): ~42 segundos.

### Validação

Aguarde até que todos os serviços estejam healthy:

```bash
docker compose ps
```

Todos os serviços devem mostrar status `healthy`.

### Gerar Tráfego (popular dashboards)

```bash
docker compose --profile loadtest up loadtest
```

### Parar e Limpar

```bash
docker compose down -v --remove-orphans
```

---

## 🔧 Decisões Técnicas

### 1. Multistage Build (Dockerfile)
O Dockerfile usa dois estágios:
- **Build** (`maven:3.9.16-eclipse-temurin-25-alpine`): JDK + Maven para compilar e gerar o JAR.
- **Runtime** (`eclipse-temurin:25-jre-alpine`): apenas JRE, sem ferramentas de build.

**Ganho de tamanho**: a imagem base de build (maven) tem ~800 MB; a imagem final construída sobre JRE (bikes-app) tem ~419 MB de disk usage. Redução de praticamente metade do tamanho, sem carregar o código fonte para runtime.

Configurações de segurança: usuário não-root (`spring`), `MaxRAMPercentage=75`, timezone `America/Sao_Paulo`.

### 2. Biblioteca GELF (logback-gelf)
**Escolha**: `de.siegmar:logback-gelf:6.1.2`

**Por quê**:
- Compatível com Java 25 e Logback 1.5.38 (versão gerenciada pelo Spring Boot 4.1.1)
- Zero dependências extras (só precisa do Logback que já existe)
- Ativamente mantida (release de Set/2025)
- Suporta UDP GELF nativamente
- Descartadas: `biz.paluch.logging:logstash-gelf` (compatibilidade incerta com Logback 1.5.x) e Docker GELF log driver (impede startup se Graylog não está pronto)

### 3. Healthchecks vs depends_on simples
O `depends_on` sem `condition` apenas garante que o container *iniciou*, não que está *pronto*. Serviços como OpenSearch e Graylog levam 60-120s para aceitar conexões. Sem healthchecks com `start_period` generoso, os dependentes falham com connection refused.

Cada serviço tem healthcheck real:
- MySQL: `mysqladmin ping`
- MongoDB: `mongosh --eval "db.adminCommand('ping')"`
- OpenSearch: `curl /_cluster/health`
- Graylog: `curl /api/system/lbstatus`
- App: `wget --spider /actuator/health` (Alpine sem curl — usa wget do busybox)
- Prometheus: `wget --spider /-/healthy`
- Grafana: `curl /api/health`

### 4. Limites de Memória (heaps)
A stack inteira consome em torno de 2.5 GB a 3 GB (reais) em idle, mas os limites configurados somam ~6,5 GB para absorver o tráfego do loadtest. Os principais consumos observados no `docker stats` e seus limites são:

| Serviço | Consumo Medido (idle) | Limite | Heap / Config |
|---------|-----------------------|--------|---------------|
| OpenSearch | ~908 MB | 1,5 GB | -Xms512m -Xmx512m |
| Graylog | ~1 GB | 2 GB | -Xms512m -Xmx512m |
| MySQL | ~152 MB | 1 GB | innodb-buffer-pool-size=64M |
| MongoDB | ~93 MB | 512 MB | wiredTigerCacheSizeGB=0.25 |
| App | ~246 MB | 1 GB | MaxRAMPercentage=75 |
| Prometheus | ~25 MB | 256 MB | — |
| Grafana | ~85 MB | 256 MB | — |

### 5. Input GELF Provisionado Automaticamente
O input GELF UDP é criado **sem intervenção manual**:

Um container de inicialização (`graylog-init`) aguarda o Graylog ficar healthy e cria o input via API REST (POST `/api/system/inputs` com o header obrigatório `X-Requested-By`). É idempotente: verifica se o input já existe antes de criar, então re-execuções do compose não geram duplicatas.

> O mecanismo de *content pack* (`GRAYLOG_CONTENT_PACKS_AUTO_INSTALL`) foi **deliberadamente desabilitado**: o arquivo `graylog/contentpacks/gelf-udp-input.json` está no formato legado v1, que o Graylog 6.x não aceita, e mantê-lo ativo criaria risco de input duplicado competindo pela porta 12201. O arquivo permanece no repositório apenas como referência do que o input provisiona.

A app depende do init com `condition: service_completed_successfully`, garantindo que o input existe **antes** do primeiro log GELF.

### 6. H2 Console Desabilitado
O `spring.h2.console.enabled=false` desabilita o console H2 em runtime. Em um projeto DevSecOps, expor um console de banco em produção é um risco de segurança. O H2 continua sendo usado apenas nos testes (via `src/test/resources/application.properties`).

### 7. Portas Internas
MySQL, MongoDB e OpenSearch **não expõem portas no host** — apenas a rede interna do compose (`bikes-net`). Isso elimina conflitos com serviços locais (ex: MySQL na 3306) e reduz a superfície de ataque.

---

## 📸 Evidências

As evidências devem ser salvas em `docs/evidencias/`. Prints necessários:

| # | Evidência | Arquivo | O que mostra |
|---|-----------|---------|-------------|
| 1 | Pipeline CI/CD | `pipeline-github-actions.png` | Workflow verde no GitHub Actions |
| 2 | Imagem no GHCR | `ghcr-imagem.png` | Imagem publicada no GitHub Container Registry |
| 3 | Compose healthy | `docker-compose-ps.png` | `docker compose ps` com todos os serviços healthy |
| 4 | Dashboard Grafana | `grafana-dashboard.png` | Dashboard "Bikes — JVM & Spring Boot" com dados reais (após loadtest) |
| 5 | Graylog — INFO | `graylog-logs-info.png` | Busca no Graylog filtrando logs de nível INFO |
| 6 | Graylog — DEBUG | `graylog-logs-debug.png` | Busca no Graylog filtrando logs de nível DEBUG |
| 7 | Graylog — ERROR | `graylog-logs-error.png` | Busca no Graylog filtrando logs de nível ERROR |
| 8 | Input GELF | `graylog-input-gelf.png` | System > Inputs mostrando "GELF UDP Input" ativo |

### Como tirar os prints

1. Suba a stack: `docker compose up --build`
2. Aguarde todos os serviços ficarem healthy: `docker compose ps`
3. Tire o print #3
4. Execute o loadtest: `docker compose --profile loadtest up loadtest`
5. Abra o Grafana (http://localhost:3000) → Dashboard "Bikes — JVM & Spring Boot" → Print #4
6. Abra o Graylog (http://localhost:9000, admin/admin)
7. Na busca, filtre por `level_name:INFO` → Print #5
8. Filtre por `level_name:DEBUG` → Print #6
9. Filtre por `level_name:ERROR` → Print #7
10. Vá em System > Inputs → Print #8
11. No GitHub, vá em Actions → Print #1
12. No GitHub, vá em Packages → Print #2

---

## 🔥 Troubleshooting

### OpenSearch falha com "max virtual memory areas vm.max_map_count [65530] is too low"

No Windows com WSL2, execute:

```bash
wsl -d docker-desktop -u root -- sysctl -w vm.max_map_count=262144
```

Para persistir, crie/edite `%USERPROFILE%\.wslconfig`:

```ini
[wsl2]
kernelCommandLine = "sysctl.vm.max_map_count=262144"
```

Reinicie o WSL: `wsl --shutdown`

### Porta ocupada

Se alguma porta estiver em uso (8080, 9090, 3000, 9000), identifique o processo:

```bash
netstat -ano | findstr :8080
```

E encerre o processo ou altere a porta no `docker-compose.yml`.

### Pouca memória

Se containers são mortos com OOMKilled:
1. Aumente a memória do Docker Desktop (Settings → Resources → Memory)
2. Mínimo recomendado: 6 GB, ideal: 8 GB

### App não conecta ao MySQL

Verifique se o container MySQL está healthy:

```bash
docker compose ps mysql
docker compose logs mysql
```

---

## ✅ Mapeamento de Critérios da Rubrica

| Critério | Pontuação | Arquivo/Evidência |
|----------|-----------|-------------------|
| **1. Pipeline CI/CD** | 2,0 pts | `.github/workflows/ci-cd.yml` + `pipeline-github-actions.png` + `ghcr-imagem.png` |
| **2. Monitoramento (Prometheus)** | 1,5 pts | `prometheus/prometheus.yml` + `pom.xml` (actuator/micrometer) + `application.properties` |
| **3. Dashboards (Grafana)** | 2,0 pts | `grafana/provisioning/` + `grafana-dashboard.png` |
| **4. Logs (Graylog)** | 1,5 pts | `graylog/` + `logback-spring.xml` + `graylog-logs-*.png` + `graylog-input-gelf.png` |
| **5. Orquestração (docker-compose)** | 2,0 pts | `docker-compose.yml` + `.env` + `docker-compose-ps.png` |
| **6. README** | 1,0 pt | `README.md` + `docs/evidencias/` |
| **Dockerfile Multistage** | Obrigatório | `Dockerfile` (NÃO alterado) |

### Descontos evitados

| Desconto | Como foi evitado |
|----------|-----------------|
| Falha no startup (-1,0 pt) | Healthchecks reais com `start_period` generoso + `depends_on: condition` |
| Falta de prints (-0,5 pt) | Seção de evidências com instruções detalhadas de cada print |
