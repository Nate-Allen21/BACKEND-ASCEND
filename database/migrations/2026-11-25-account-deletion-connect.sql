-- Account deletion keeps the other participant's conversation, but removes the
-- deleted participant reference. The production public schema was inspected
-- before applying this migration.
BEGIN;

DO $$
DECLARE item record;
BEGIN
  FOR item IN
    SELECT c.conname, c.conrelid::regclass AS table_name
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
    WHERE c.contype = 'f'
      AND c.confrelid = 'public.users'::regclass
      AND c.conrelid = 'public.connect_conversations'::regclass
      AND a.attname IN ('first_user_id','second_user_id')
  LOOP
    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', item.table_name, item.conname);
  END LOOP;
END $$;

ALTER TABLE public.connect_conversations ALTER COLUMN first_user_id DROP NOT NULL;
ALTER TABLE public.connect_conversations ALTER COLUMN second_user_id DROP NOT NULL;

ALTER TABLE public.connect_conversations
  ADD CONSTRAINT fk_connect_conversation_first_user FOREIGN KEY (first_user_id) REFERENCES public.users(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_connect_conversation_second_user FOREIGN KEY (second_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

COMMIT;
