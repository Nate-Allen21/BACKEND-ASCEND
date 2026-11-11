# ASCEND Shop API

Compras são processadas no servidor e persistidas no PostgreSQL. O cliente envia **apenas o ID do item**, nunca o preço. O backend também valida que o item pertence à rotação diária.

`GET /api/shop` — Authorization: `Bearer <token>` -> `{ "coins": 0, "purchases": [], "offers": [] }`, com 9 ofertas rotativas diárias, calculadas pelo backend.

`POST /api/shop/purchase` — mesmo header + body `{ "itemId": "pausa-estrategica" }` -> estado atualizado.

A transação SQL registra `shop_purchases` e `vault_transactions`, desconta o saldo, usa lock por usuário e impede repetição diária por índice único.

**Limitação atual:** inventário e ativação de temporizadores permanecem locais por dispositivo; as compras e o saldo são sincronizados. Não trate recompensas como bens físicos ou pagamentos reais.

## Cosméticos de perfil

Os endpoints abaixo exigem autenticação e usam o saldo e o preço definidos pelo servidor:

- `GET /api/cosmetics` — catálogo, saldo, itens pertencentes e itens equipados.
- `POST /api/cosmetics/purchase` — recebe `{ "itemId": "theme-aurora" }` e registra uma compra única.
- `PUT /api/cosmetics/equipped` — recebe `{ "itemId": "theme-aurora" }` para equipar um item já adquirido.
- `DELETE /api/cosmetics/equipped/{slot}` — remove o item equipado no slot informado.

Os itens são inseridos sem apagar o catálogo existente. JPA cria/atualiza as tabelas `store_items`, `cosmetic_ownership`, `equipped_cosmetics` e `cosmetic_purchase_history` durante a inicialização com `spring.jpa.hibernate.ddl-auto=update`. O perfil próprio e os perfis públicos incluem `equippedCosmetics` para exibir cosméticos equipados.
