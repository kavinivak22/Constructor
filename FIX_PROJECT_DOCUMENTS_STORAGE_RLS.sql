-- Fix Project Documents Bucket & Storage RLS Policies
-- Run this in your Supabase Dashboard > SQL Editor:

-- 1. Ensure the bucket exists and is public
INSERT INTO storage.buckets (id, name, public)
VALUES ('project-documents', 'project-documents', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 2. Drop existing policies on project-documents to avoid duplicate conflicts
DROP POLICY IF EXISTS "Authenticated users can upload project documents" ON storage.objects;
DROP POLICY IF EXISTS "Public can view project documents" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can view project documents" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete project documents" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update project documents" ON storage.objects;

-- 3. Policy to allow authenticated users to upload documents
CREATE POLICY "Authenticated users can upload project documents"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK ( bucket_id = 'project-documents' );

-- 4. Policy to allow viewing / downloading documents
CREATE POLICY "Public can view project documents"
ON storage.objects FOR SELECT
TO public
USING ( bucket_id = 'project-documents' );

CREATE POLICY "Authenticated users can view project documents"
ON storage.objects FOR SELECT
TO authenticated
USING ( bucket_id = 'project-documents' );

-- 5. Policy to allow updating documents
CREATE POLICY "Authenticated users can update project documents"
ON storage.objects FOR UPDATE
TO authenticated
USING ( bucket_id = 'project-documents' );

-- 6. Policy to allow deleting documents
CREATE POLICY "Authenticated users can delete project documents"
ON storage.objects FOR DELETE
TO authenticated
USING ( bucket_id = 'project-documents' );
