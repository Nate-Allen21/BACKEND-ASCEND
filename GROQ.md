# Groq no ASCEND local

O container `ascend-local-backend` está configurado para `https://api.groq.com/openai/v1`, com o modelo `openai/gpt-oss-20b` e o protocolo `chat-completions` já suportado pelo backend.

A chave fica fora do projeto, em `%USERPROFILE%\.ascend\backend.local.env`, e é carregada como variável de ambiente do container. Não copie esse arquivo para o frontend nem publique seu conteúdo.

Para usar: abra **Assistente IA**, marque a autorização de envio ao provedor e envie sua pergunta. Sem autorização, o backend não envia o contexto à Groq. O plano gratuito da conta foi confirmado pelo usuário; nenhuma assinatura foi contratada. Limites e disponibilidade são controlados pela Groq.

`http://localhost:8081/api/health/ai` informa provedor, modelo e se existe chave configurada, sem revelar a chave.

O atalho `INICIAR.cmd` continua iniciando o container configurado. A configuração local não publica nem modifica um backend remoto. O container anterior, parado, foi preservado como `ascend-local-backend-before-groq` para eventual rollback. O banco de dados permanece no container original.

Verificação: saúde do backend com `provider=groq` e `apiKeyConfigured=true`; requisição real à Groq retornou HTTP 200 com texto gerado. A conversa autenticada pela interface não foi executada com uma conta do usuário.

Referências: https://console.groq.com/docs/openai e https://console.groq.com/docs/rate-limits.
