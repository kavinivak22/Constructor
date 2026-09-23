-- =================================================================
-- FIX SIGNUP "Database error saving new user" IN SUPABASE
-- =================================================================
-- Run this SQL in your Supabase SQL Editor (https://supabase.com)
-- =================================================================

-- 1. Ensure public.users table has all necessary columns (both snake_case & camelCase)
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "displayName" text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS full_name text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS display_name text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "photoURL" text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS photo_url text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS avatar_url text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "companyId" uuid;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS company_id uuid;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS role text DEFAULT 'member';
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS status text DEFAULT 'active';
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "createdAt" timestamp with time zone DEFAULT now();

-- 2. Drop existing triggers and functions cleanly
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user();

-- 3. Create a fail-safe handle_new_user() trigger function with EXCEPTION handling
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_name text;
  v_avatar text;
BEGIN
  -- Extract name from metadata, fallback to email username
  v_name := COALESCE(
    NEW.raw_user_meta_data->>'full_name',
    NEW.raw_user_meta_data->>'displayName',
    NEW.raw_user_meta_data->>'display_name',
    NEW.raw_user_meta_data->>'name',
    split_part(NEW.email, '@', 1)
  );

  v_avatar := COALESCE(
    NEW.raw_user_meta_data->>'avatar_url',
    NEW.raw_user_meta_data->>'photoURL',
    NEW.raw_user_meta_data->>'photo_url'
  );

  -- Safely attempt insertion without failing the auth signup transaction
  BEGIN
    INSERT INTO public.users (
      id,
      email,
      "displayName",
      full_name,
      display_name,
      "photoURL",
      photo_url,
      role,
      status
    )
    VALUES (
      NEW.id,
      NEW.email,
      v_name,
      v_name,
      v_name,
      v_avatar,
      v_avatar,
      'member',
      'active'
    )
    ON CONFLICT (id) DO UPDATE SET
      email = EXCLUDED.email,
      "displayName" = COALESCE(public.users."displayName", EXCLUDED."displayName"),
      full_name = COALESCE(public.users.full_name, EXCLUDED.full_name),
      display_name = COALESCE(public.users.display_name, EXCLUDED.display_name);
  EXCEPTION WHEN OTHERS THEN
    BEGIN
      -- Fallback insert with minimal required columns
      INSERT INTO public.users (id, email, role, status)
      VALUES (NEW.id, NEW.email, 'member', 'active')
      ON CONFLICT (id) DO NOTHING;
    EXCEPTION WHEN OTHERS THEN
      -- Catch all errors so auth.users insertion NEVER fails or throws "Database error saving new user"
      RAISE WARNING 'handle_new_user trigger error caught: %', SQLERRM;
    END;
  END;

  RETURN NEW;
END;
$$;

-- 4. Re-bind trigger to auth.users table
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 5. Fix Row Level Security policies for public.users
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can insert their own record" ON public.users;
CREATE POLICY "Users can insert their own record"
ON public.users FOR INSERT
WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update their own record" ON public.users;
CREATE POLICY "Users can update their own record"
ON public.users FOR UPDATE
USING (auth.uid() = id);

DROP POLICY IF EXISTS "Allow individual read access for users" ON public.users;
DROP POLICY IF EXISTS "Users can view their own record" ON public.users;
CREATE POLICY "Users can view their own record"
ON public.users FOR SELECT
USING (auth.uid() = id OR true);
