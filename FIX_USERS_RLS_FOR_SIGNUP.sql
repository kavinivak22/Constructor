-- Allow users to insert/update their own user record
-- This is needed when accepting invites or creating companies

-- Policy for INSERT (creating user record)
CREATE POLICY "Users can insert their own record"
ON public.users
FOR INSERT
WITH CHECK (auth.uid() = id);

-- Policy for UPDATE (updating user record)
CREATE POLICY "Users can update their own record"
ON public.users
FOR UPDATE
USING (auth.uid() = id);
