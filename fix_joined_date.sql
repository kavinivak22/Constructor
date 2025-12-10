-- Add created_at column to public.users if it doesn't exist
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS created_at timestamp with time zone DEFAULT now();

-- Update the handle_new_user function to include created_at
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

-- Backfill created_at for existing users from auth.users
UPDATE public.users pu
SET created_at = au.created_at
FROM auth.users au
WHERE pu.id = au.id
AND pu.created_at IS NULL;
