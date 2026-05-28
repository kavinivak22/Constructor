-- Fix the relationship between documents and users in Supabase schema cache
-- By default, uploader_id was referencing auth.users(id), which prevents PostgREST
-- from automatically joining the public.users profile table.
-- Dropping the constraint and pointing it to public.users(id) resolves this.

ALTER TABLE public.documents 
DROP CONSTRAINT IF EXISTS documents_uploader_id_fkey,
ADD CONSTRAINT documents_uploader_id_fkey 
FOREIGN KEY (uploader_id) REFERENCES public.users(id) ON DELETE SET NULL;
