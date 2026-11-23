-- PROPOSTA: NAO EXECUTAR EM PRODUCAO SEM AUTORIZACAO EXPLICITA.
-- Habilita RLS e remove privilegios diretos de PUBLIC, anon e authenticated.
-- Requer backup, teste dos consumidores e confirmacao de que o backend JDBC
-- usa um papel proprietario ou BYPASSRLS. Nao afeta tabelas da outra aplicacao.
BEGIN;
DO $$
DECLARE t text; enabled boolean; policy_count integer;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'users','user_sessions','user_attributes','user_quests','system_missions',
    'mission_completions','focus_sessions','vault_transactions','shop_purchases',
    'profile_posts','profile_post_comments','profile_post_likes','social_connections',
    'store_items','cosmetic_ownership','cosmetic_purchase_history','equipped_cosmetics',
    'connect_conversations','connect_messages'
  ] LOOP
    SELECT c.relrowsecurity INTO enabled FROM pg_catalog.pg_class c
      JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='public' AND c.relname=t AND c.relkind IN ('r','p');
    IF NOT FOUND OR enabled THEN
      RAISE EXCEPTION 'Tabela ausente ou RLS ja habilitada: %', t;
    END IF;
    SELECT count(*) INTO policy_count FROM pg_catalog.pg_policies
      WHERE schemaname='public' AND tablename=t;
    IF policy_count <> 0 THEN RAISE EXCEPTION 'Existem policies em %', t; END IF;
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY',t);
    EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC, anon, authenticated',t);
    IF EXISTS (SELECT 1 FROM unnest(ARRAY['anon','authenticated']) AS r(role_name)
               CROSS JOIN unnest(ARRAY['SELECT','INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER']) AS p(privilege_name)
               WHERE has_table_privilege(r.role_name, format('public.%I',t), p.privilege_name)) THEN
      RAISE EXCEPTION 'Privilegio efetivo ainda existe em %; investigar heranca e grants', t;
    END IF;
  END LOOP;
END $$;
COMMIT;
