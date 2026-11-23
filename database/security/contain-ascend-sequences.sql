-- PROPOSTA: nao executar em producao sem autorizacao e verificacao dos consumidores.
-- Somente sequences identificadas como pertencentes a tabelas ASCEND.
BEGIN;
DO $$
DECLARE item record; role_name text; privilege_name text; owned_count integer;
BEGIN
  FOR item IN SELECT * FROM (VALUES
    ('connect_conversations','connect_conversations_id_seq'),
    ('connect_messages','connect_messages_id_seq'),
    ('cosmetic_ownership','cosmetic_ownership_id_seq'),
    ('cosmetic_purchase_history','cosmetic_purchase_history_id_seq'),
    ('equipped_cosmetics','equipped_cosmetics_id_seq'),
    ('focus_sessions','focus_sessions_id_seq'),
    ('mission_completions','mission_completions_id_seq'),
    ('profile_post_comments','profile_post_comments_id_seq'),
    ('profile_post_likes','profile_post_likes_id_seq'),
    ('profile_posts','profile_posts_id_seq'),
    ('shop_purchases','shop_purchases_id_seq'),
    ('social_connections','social_connections_id_seq'),
    ('system_missions','system_missions_id_seq'),
    ('user_attributes','user_attributes_id_seq'),
    ('user_quests','user_quests_id_seq'),
    ('user_sessions','user_sessions_id_seq'),
    ('users','users_id_seq'),
    ('vault_transactions','vault_transactions_id_seq')
  ) AS x(table_name, sequence_name) LOOP
    SELECT count(*) INTO owned_count FROM pg_catalog.pg_class s
      JOIN pg_catalog.pg_namespace sn ON sn.oid=s.relnamespace
      JOIN pg_catalog.pg_depend d ON d.objid=s.oid AND d.classid='pg_class'::regclass AND d.deptype IN ('a','i')
      JOIN pg_catalog.pg_class t ON t.oid=d.refobjid
      JOIN pg_catalog.pg_namespace tn ON tn.oid=t.relnamespace
      WHERE sn.nspname='public' AND s.relname=item.sequence_name AND s.relkind='S'
        AND tn.nspname='public' AND t.relname=item.table_name;
    IF owned_count <> 1 THEN RAISE EXCEPTION 'Vinculo inesperado para %',item.sequence_name; END IF;
    EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE public.%I FROM PUBLIC, anon, authenticated',item.sequence_name);
    FOREACH role_name IN ARRAY ARRAY['anon','authenticated'] LOOP
      FOREACH privilege_name IN ARRAY ARRAY['USAGE','SELECT','UPDATE'] LOOP
        IF has_sequence_privilege(role_name,format('public.%I',item.sequence_name),privilege_name) THEN
          RAISE EXCEPTION 'Privilegio efetivo de sequence ainda existe: % % %',role_name,privilege_name,item.sequence_name;
        END IF;
      END LOOP;
    END LOOP;
  END LOOP;
END $$;
COMMIT;
