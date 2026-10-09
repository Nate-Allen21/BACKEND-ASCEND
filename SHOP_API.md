# ASCEND Shop API

Compras são processadas no servidor e persistidas no PostgreSQL. O cliente envia **apenas o ID do item**, nunca o preço. O backend também valida que o item pertence à rotação diária.

`GET /api/shop` — Authorization: `Bearer <token>` -> `{ "coins": 0, "purchases": [], "offers": [] }`, com 9 ofertas rotativas diárias, calculadas pelo backend.

`POST /api/shop/purchase` — mesmo header + body `{ "itemId": "pausa-estrategica" }` -> estado atualizado.

A transação SQL registra `shop_purchases` e `vault_transactions`, desconta o saldo, usa lock por usuário e impede repetição diária por índice único.

**Limitação atual:** inventário e ativação de temporizadores permanecem locais por dispositivo; as compras e o saldo são sincronizados. Não trate recompensas como bens físicos ou pagamentos reais.
