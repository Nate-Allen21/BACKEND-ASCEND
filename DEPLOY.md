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

## Recuperação de senha

1. No Resend, verifique o domínio de envio com os registros DNS indicados e crie uma chave restrita ao envio nesse domínio. Desative o rastreamento de cliques e aberturas para os e-mails de recuperação. A mensagem usa um link direto com o token no fragmento.
2. Em Render → BACKEND-ASCEND → Environment, configure `RESEND_API_KEY`, `ASCEND_RECOVERY_FROM` (por exemplo, `ASCEND <acesso@DOMINIO-VERIFICADO>`) e `ASCEND_RECOVERY_ORIGIN` (origem HTTPS oficial do frontend, como `https://ascend22.vercel.app`). Confirme o domínio no projeto Vercel. A origem aceita apenas esquema, host e porta opcional.
3. Configure `ASCEND_RECOVERY_TRUST_RENDER_EDGE=true` em serviços cujo ingresso esteja restrito à borda Render/Cloudflare. Nesse modo o limite usa o IP enviado pela borda em `CF-Connecting-IP`. Nos demais ambientes, o limite usa o endereço da conexão. Mantenha o encaminhamento automático de cabeçalhos do servlet desativado em origens sem proxy confiável.
4. Antes da migration, faça um backup do banco. Use a tela Database → Backups do projeto Supabase para confirmar um backup recuperável, ou use `pg_dump --format=custom --file=ascend-before-recovery.dump --host=HOST-DO-SESSION-POOLER --port=5432 --username=USUARIO --dbname=postgres --password` com TLS verificado. Guarde o arquivo fora do repositório, em armazenamento restrito. Confira o catálogo com `pg_restore --list` e ensaie a restauração em banco isolado.
5. Execute uma vez `database/migrations/2026-11-27-password-recovery.sql` no PostgreSQL, com o papel proprietário usado pela aplicação. Se o executor da migration e o papel JDBC forem diferentes, conceda ao papel JDBC USAGE no schema e nas sequências, e SELECT, INSERT, UPDATE e DELETE nas três tabelas de `ascend_recovery`. Mantenha esse schema fora dos schemas expostos pela Data API. A migration é transacional e cria exclusivamente objetos novos. Executá-la novamente acusa que o schema já existe.
6. Publique primeiro a API e confirme `/api/health`. Libere o frontend logo em seguida e confirme que `/recuperar.html` carrega no domínio oficial. Durante a troca, aguarde o encerramento das instâncias antigas antes do teste de sessões simultâneas. Para reversão, volte aos builds anteriores e preserve as tabelas.
7. Com uma conta própria cadastrada, solicite o e-mail, confirme o domínio do link, redefina a senha e faça login. Confira em outro navegador a revogação da sessão prévia e compare o progresso antes/depois. O painel do Resend permite distinguir aceitação da mensagem e entrega. Evite compartilhar o link ou os valores das senhas.

| POST | Entrada JSON | Resposta |
| --- | --- | --- |
| `/api/auth/forgot-password` | `email` | 202 e texto genérico para e-mails válidos |
| `/api/auth/reset-password/validate` | `token` | 204 para link vigente, 400 para link indisponível |
| `/api/auth/reset-password` | `token`, `newPassword`, `confirmPassword` | 204 após troca da senha e revogação das sessões |

Em cada janela de 15 minutos, são aceitas até 20 solicitações por IP e três por e-mail. O limite por e-mail também responde 202. A validação e a redefinição compartilham 30 tentativas por IP, e a redefinição limita oito tentativas por conta. O limite por IP ou de redefinição responde 429. Um pedido de emissão aceito revoga o link anterior. O envio ocorre fora da transação de autenticação e permite até três tentativas por pedido, com intervalo de 30 segundos. Cada tentativa gera um token novo.

### Testes PostgreSQL

Use uma instância **local descartável** com papel `ascend_test`, permissão CREATEDB e senha exclusiva de fixture `local-recovery-only`. Defina `ASCEND_RECOVERY_TEST_PORT` como a porta dessa instância e execute `gradlew test`. A suíte cria um banco com nome aleatório em loopback, registra contas, aplica a migration real e verifica os endpoints e a integração HTTP com um receptor local. Descarte a instância de teste ao terminar.
