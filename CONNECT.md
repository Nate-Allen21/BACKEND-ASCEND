# ASCEND Connect

O Connect usa as sessões Bearer existentes e as tabelas `connect_conversations` e `connect_messages`. O par de usuários é ordenado e protegido por restrição única; a conversa guarda o último ID lido por participante. Cada mensagem tem chave de idempotência por remetente e há índices para histórico e limite de envio. O Hibernate cria as tabelas com a configuração `spring.jpa.hibernate.ddl-auto=update` existente. Confira a política de migração e as permissões do banco antes de implantar.

## API

Todos os endpoints exigem `Authorization: Bearer <token>`:

- `GET /api/connect/users?q=...`: busca por nome, identificador público completo ou ID.
- `GET /api/connect/conversations`: conversas e contadores de não lidas.
- `POST /api/connect/conversations/{userId}`: cria ou encontra uma conversa individual.
- `GET /api/connect/conversations/{id}/messages?before={messageId}`: até 50 mensagens anteriores, em ordem crescente.
- `POST /api/connect/conversations/{id}/messages`: `{ "content": "...", "clientId": "UUID" }`.
- `POST /api/connect/conversations/{id}/read`: `{ "messageId": 123 }`; retorna 204.

A identidade do remetente vem da sessão; o acesso exige participação e respeita bloqueios e visibilidade do outro perfil. O limite é de 30 mensagens por minuto por usuário. A interface consulta o servidor a cada cinco segundos enquanto a aba está visível e recupera mensagens persistidas após interrupções. Não há recibo de entrega: apenas envio persistido e leitura confirmada são apresentados. A confirmação significa que a conversa foi aberta, não que houve leitura humana.

O indicador aparece no menu e na barra superior. O aviso discreto no próprio site pode ser desativado na tela de mensagens; a preferência fica no navegador, por conta. O Connect não solicita permissão para notificações do navegador. O projeto não tinha uma preferência geral de notificações para reutilizar.

## Execução e testes

Configure o banco seguindo `BACKEND-ASCEND/README.md`, execute `gradlew.bat bootRun` na pasta do backend e `npm install` e `npm run dev` em `FRONTEND-ASCEND`. Configure a URL da API conforme o README do frontend. Use duas contas para exercitar a conversa.

Execute `gradlew.bat test` no backend e `npm test -- --workers=2` no frontend. O teste de navegador com duas contas reais fica em `tests/connect.e2e.spec.js`: inicie um backend de teste isolado e defina `CONNECT_E2E_API` como a URL `/api` dessa instância antes de executar o Playwright. Ele cria contas e mensagens no banco configurado, portanto não o execute contra dados de produção.

## Limitações

A atualização usa polling, não WebSocket. As preferências de aviso são locais ao navegador. O H2 usado nos testes não substitui testes de concorrência e implantação no PostgreSQL de produção; a estratégia de criação automática de esquema também deve ser avaliada para esse ambiente.
