-- Diagnostic: Check if the table exists in public schema
DO $$
DECLARE
    table_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'users'
    ) INTO table_exists;

    IF NOT table_exists THEN
        RAISE NOTICE 'WARNING: Table public.users does not exist. Please check if you are in the correct project.';
    ELSE
        RAISE NOTICE 'Table public.users found. Proceeding with update...';
    END IF;
END $$;

-- Attempt to add column (using IF EXISTS to avoid error if table is missing)
ALTER TABLE IF EXISTS public.users ADD COLUMN IF NOT EXISTS created_at timestamp with time zone DEFAULT now();

-- Update the handle_new_user function
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.users (id, email, full_name, photo_url, created_at)
  VALUES (
    new.id,
    new.email,
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'avatar_url',
    new.created_at
  );
  RETURN new;
END;
$$;

-- Backfill created_at for existing users
-- Wrapped in DO block to avoid error if table doesn't exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
        UPDATE public.users pu
        SET created_at = au.created_at
        FROM auth.users au
        WHERE pu.id = au.id
        AND pu.created_at IS NULL;
    END IF;
END $$;
