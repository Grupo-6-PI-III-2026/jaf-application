# JAF Frontend

React + TypeScript + Vite.

## Como rodar

```bash
npm ci
npm run dev
```

A API é acessada por `VITE_API_URL` (padrão `/api`, via proxy do nginx em produção).

## Verificações

```bash
npm run lint
npx tsc -b
npx vitest run
```

Última execução local: `tsc -b` e `lint` sem erros; `vitest run` com 124 testes passando e 10 falhando
(`NovoFuncionario.test.tsx` e `NovaObra.test.tsx`, já falhavam antes das últimas alterações).

## Build de produção

```bash
docker build -t jaf-frontend .
```

A imagem final usa `nginx` (porta 80) e roda como root; migrar para `nginx-unprivileged` exige ajustar
porta no `nginx.conf`, `docker-compose.yml` e no target group do Terraform.

## Variáveis do backend relevantes para o frontend

`APP_COOKIE_SECURE` (padrão `true`; cookie `HttpOnly`, `SameSite=Strict`), `JWT_EXPIRATION` (segundos),
`APP_TRUSTED_PROXY_HOPS`. Não versionar valores de segredos.
