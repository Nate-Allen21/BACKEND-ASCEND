# The System - DER e arquitetura base

## DER conceitual

```mermaid
erDiagram
    USERS ||--o{ USER_ATTRIBUTES : has
    USERS ||--o{ USER_QUESTS : receives
    USERS ||--o{ MISSION_COMPLETIONS : logs
    USERS ||--o{ VAULT_TRANSACTIONS : records
    USERS ||--o{ FOCUS_SESSIONS : starts

    SYSTEM_MISSIONS ||--o{ USER_QUESTS : instantiates

    USERS {
        bigint id PK
        varchar name
        varchar email UK
        varchar password_hash
        varchar player_rank
        int level
        int experience
        int next_level_experience
        int streak_days
        int coins
        date last_routine_date
        datetime created_at
    }

    USER_ATTRIBUTES {
        bigint id PK
        bigint user_id FK
        varchar attribute_type
        int base_value
        int bonus_value
    }

    SYSTEM_MISSIONS {
        bigint id PK
        varchar code UK
        varchar title
        varchar description
        varchar pillar
        varchar kind
        varchar primary_attribute
        varchar secondary_attribute
        int experience_reward
        int coin_reward
        int stat_reward
        boolean required_mission
        boolean penalty_quest
        int focus_minutes
        boolean active
        datetime created_at
    }

    USER_QUESTS {
        bigint id PK
        bigint user_id FK
        bigint system_mission_id FK
        date scheduled_date
        varchar status
        datetime completed_at
        int granted_experience
        int granted_coins
        boolean debuff_applied
        datetime created_at
    }

    MISSION_COMPLETIONS {
        bigint id PK
        bigint user_id FK
        varchar mission_id
        datetime completed_at
    }

    VAULT_TRANSACTIONS {
        bigint id PK
        bigint user_id FK
        varchar type
        varchar description
        int amount
        datetime created_at
    }

    FOCUS_SESSIONS {
        bigint id PK
        bigint user_id FK
        int planned_minutes
        int completed_minutes
        boolean dungeon_clear
        datetime started_at
        datetime finished_at
    }
```

## Como os modulos se conectam

- `users`: identidade do jogador, rank, nivel, streak e moedas.
- `user_attributes`: stats persistidos por usuario, para treino, estudos, tarefas e vida espiritual.
- `system_missions`: catalogo mestre de quests, penalty quests, dungeon runs e quests lendarias.
- `user_quests`: instancia diaria da rotina gerada para o usuario, com status e recompensa concedida.
- `mission_completions`: legado simples do fluxo atual; pode coexistir durante a migracao.
- `vault_transactions`: ledger do modulo `The Vault`.
- `focus_sessions`: historico do Pomodoro/Dungeon Run.

## Regras de negocio sugeridas

- `Treino -> Strength + Vitality`
- `Estudo -> Intelligence`
- `Responsabilidades -> Agility`
- `Social/Igreja -> Spirit`
- Se uma `required_mission` expirar como `FAILED`, o sistema gera `penalty_quest = true` no proximo ciclo.
- Rank recomendado:
  - `E`: nivel 1
  - `D`: niveis 2-3
  - `C`: niveis 4-5
  - `B`: niveis 6-7
  - `A`: niveis 8-9
  - `S`: niveis 10-29
  - `S2`: niveis 30-49
  - `SSS+`: nivel 50+

## Expansao mobile/PWA

- `focus_sessions` e `user_quests` sao a base ideal para notificacoes push.
- `vault_transactions` suporta economia, loja, consumiveis e itens de streak protection.
- O frontend deve consumir cards compactos e modais de 2 cliques no maximo por acao critica.
