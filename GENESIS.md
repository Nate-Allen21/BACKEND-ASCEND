# ASCEND Market e Studio

O frontend usa Vite 7.3.8 e módulos JavaScript. O backend usa Spring Boot 4.0.10, Java 21, JPA e PostgreSQL. As sessões Bearer, o saldo em `users.coins`, o histórico em `vault_transactions` e as relações de propriedade e equipamento já existiam. A Genesis usa a mesma carteira. O preço e a disponibilidade vêm do banco. As contas começam com o saldo definido pelas regras existentes.

## Configuração

1. Em um banco ASCEND existente, execute `database/genesis-postgresql.sql` com o proprietário das tabelas antes de iniciar a nova API. A migração adiciona metadados de arte, versão da carteira, auditoria e uma restrição contra novas gravações negativas. A restrição usa `NOT VALID` para preservar dados antigos. Depois de conferir os saldos históricos, o DBA pode executar `ALTER TABLE users VALIDATE CONSTRAINT ck_ascend_wallet_nonnegative`. Use uma conexão JDBC autorizada a acessar a tabela de auditoria com RLS. Em uma instalação vazia, inicialize primeiro o esquema base pelo fluxo de instalação do ASCEND.
2. Configure `ASCEND_COSMETIC_ADMIN_IDS` no backend com os IDs numéricos dos administradores, separados por vírgulas. Vazio desabilita a administração. O acesso fica em **ASCEND Market → Administrar cosméticos**. A configuração de recompensa futura é auditada e não concede AC por si só.
3. Para testar créditos em um banco isolado, use o perfil Spring `local` ou `test` e defina `ASCEND_COSMETIC_TEST_USER_IDS`. Um administrador pode ajustar até 10.000 AC por operação nessas contas, com motivo obrigatório. Mantenha esses perfis e IDs fora da produção.
4. Inicie a API com `gradlew.bat bootRun`. No frontend, configure `VITE_API_URL` para a URL da API com sufixo `/api`, execute `npm ci` e `npm run dev`. Para distribuição, use `npm run build`. Os nove arquivos de arte são copiados por Vite para `dist/cosmetics/genesis`.

## Contrato autenticado

Todas as rotas abaixo têm o prefixo `/api/cosmetics`. O usuário da carteira vem da sessão.

| Método e recurso | Resultado ou corpo |
| --- | --- |
| GET / | Saldo e itens disponíveis ou adquiridos, com raridade, preço, arte e estado |
| GET /items/{id} | Detalhes de um item acessível |
| GET /balance | Saldo em AC |
| GET /inventory | Itens adquiridos |
| GET /equipped | Itens equipados |
| POST /purchase | Corpo `{"itemId":"genesis-frame-01"}` |
| PUT /equipped | Mesmo corpo da compra. Substitui o item no slot |
| DELETE /equipped/AVATAR_FRAME | Restaura o avatar sem moldura |
| GET /transactions?page=0 | Extrato individual, 50 entradas por página |
| GET /capabilities | Indica se a conta pode administrar cosméticos |
| GET /admin?page=0 | Catálogo de coleções, aquisições, transações e configuração de recompensa |
| PUT /admin/items/{id} | Corpo com `price` inteiro positivo e `active` booleano |
| PUT /admin/reward | Corpo com `amount` inteiro não negativo |
| POST /admin/test-balance | Corpo com `userId`, `delta` e `reason` |

A compra bloqueia a carteira e grava débito, propriedade e recibo em uma transação. A unicidade por usuário/item evita duplicatas. A versão da linha de usuário protege contra gravações concorrentes por outros fluxos. Uma compra duplicada retorna 409. Um saldo insuficiente retorna 400. A desativação retira um item de novas vendas e preserva o inventário de quem já o adquiriu. A auditoria administrativa fica em `cosmetic_audit`.

## Outra coleção

Valide o manifesto e os recursos, use IDs inéditos e copie apenas a arte para `public/cosmetics/<coleção-em-minúsculas>/`. Adicione o catálogo no formato fornecido em `src/main/resources/cosmetics/<coleção>/catalog.json`. O backend importa novos IDs ao iniciar e preserva os preços e estados já administrados. Novas molduras usam `PROFILE_FRAME` e compartilham o slot `AVATAR_FRAME` com os itens anteriores. Para outra categoria, implemente sua forma de exibição antes de publicar a arte no Market. O cabeçalho da primeira vitrine destaca a Genesis.

## Arquivos da entrega

- Frontend criado: `js/genesis.js`, `js/genesis-admin.js`, `js/cosmetic-avatar.js`, `css/genesis.css`, seis arquivos em `public/cosmetics/genesis/frames/` e três em `thumbnails/`.
- Frontend alterado: `index.html`, `js/script.js`, `tests/experience.spec.js`.
- Backend criado: catálogo Genesis, `CosmeticAudit`, `CosmeticAdministration`, `CosmeticAdminController`, `ForbiddenException`, `FrameArtwork`, `GenesisIntegrationTest`, esta documentação e a migração SQL.
- Backend alterado: `StoreItem`, `User`, `CosmeticItemResponse`, `LeaderboardEntryResponse`, `EquippedCosmeticRepository`, `CosmeticStoreService`, `PlayerStatusService`, `CosmeticStoreController`, `ProfileController`, `ConnectController` e `GlobalExceptionHandler`.

## Verificação

Use `gradlew.bat test` no backend e `npm test` no frontend. O Playwright usa Chromium. O teste de duas contas do Connect requer `CONNECT_E2E_API` apontando para uma API com banco descartável. Para H2 em memória e pool com zero conexões ociosas, inclua `DB_CLOSE_DELAY=-1` na URL de teste.
