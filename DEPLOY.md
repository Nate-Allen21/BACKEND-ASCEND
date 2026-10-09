# Render

Use o serviço Docker existente conectado a `Nate-Allen21/BACKEND-ASCEND`, branch `main`, com deploy automático por commit. O Dockerfile compila com Java 21; não depende do Java instalado no computador. Health check: `/api/health`.

O `render.yaml` descreve o serviço no plano Free. Se o serviço não é gerenciado por Blueprint, as variáveis devem ser ajustadas no painel Render; alterar este arquivo sozinho não substitui variáveis existentes.

Preserve o banco atual e configure:

```text
ASCEND_DB_URL=jdbc:postgresql://HOST:PORT/BANCO?sslmode=require
ASCEND_DB_USER=USUARIO
ASCEND_DB_PASSWORD=SENHA
ASCEND_CORS_ALLOWED_ORIGINS=https://SEU-SITE.vercel.app
ASCEND_AI_PROVIDER=groq
ASCEND_AI_BASE_URL=https://api.groq.com/openai/v1
ASCEND_AI_MODEL=openai/gpt-oss-20b
ASCEND_AI_RESPONSE_MODE=chat-completions
ASCEND_AI_API_KEY=CHAVE_CONFIGURADA_COMO_SEGREDO_NO_RENDER
```

O código também aceita `GROQ_API_KEY`. Se existirem chaves antigas em `ASCEND_AI_KEY` ou `ASCEND_AI_API_KEY`, atualize-as ou remova-as: elas têm prioridade sobre `GROQ_API_KEY`. Não inclua chaves no GitHub. A configuração do Docker local não é transferida ao Render automaticamente.

Esta versão adiciona persistência das compras e rotas `/api/shop`. O Hibernate mantém o comportamento existente de atualização do esquema; não execute os scripts `database/reset-*` no deploy.

Após publicar, confira `/api/health`, `/api/health/ai` e o login do site. Use a URL deste serviço em `VITE_API_URL` (Vercel) e `EXPO_PUBLIC_API_URL` (Expo). O plano Free da Groq tem cotas; não foi contratado nenhum serviço pago por esta configuração.
