# Observabilidade local (Prometheus + Grafana)

## O que isso faz

- A aplicação expõe métricas Micrometer em **`http://localhost:8080/actuator/prometheus`**.
- O **Prometheus** (`docker compose`) coleta essas métricas a cada 5s.
- O **Grafana** carrega o datasource Prometheus e o dashboard **Pix Payment Engine — Overview** (HTTP, p95, HikariCP, heap).

## Subir a stack

```powershell
docker compose up -d prometheus grafana
```

Deixe a API rodando no host (`.\mvnw spring-boot:run`) na porta **8080**.

## URLs

| Serviço    | URL |
|-----------|-----|
| Prometheus | http://localhost:9090 |
| Grafana    | http://localhost:3000 (login `admin` / `admin`) |
| Métricas raw | http://localhost:8080/actuator/prometheus |

No Prometheus → **Status → Targets**: o job `pix-payment-engine` deve estar **UP**.

## Durante teste de estresse (k6, hey, Insomnia)

1. Abra o Grafana → dashboard **Pix Payment Engine — Overview**.
2. Ajuste o intervalo de tempo (canto superior direito) para **Last 5 minutes**.
3. Rode a carga; observe **req/s**, **p95**, **Hikari pending** (fila no pool) e **heap**.

### Script k6 (opcional)

Com [k6](https://k6.io/) instalado, na raiz do projeto:

```powershell
k6 run observability/k6/stress-payments.js
```

Variável opcional: `BASE_URL` (default `http://localhost:8080`).

Antes de um teste longo, aumente o saldo do remetente no Postgres para evitar `422` por falta de fundos.

## Linux

Se `host.docker.internal` não resolver, o `docker-compose.yml` já inclui `extra_hosts` no serviço Prometheus. Atualize o Docker Compose para versão que suporta `host-gateway`.

## Produção

Não exponha `/actuator/prometheus` publicamente sem autenticação ou rede restrita; use scrape interno ou sidecar.
