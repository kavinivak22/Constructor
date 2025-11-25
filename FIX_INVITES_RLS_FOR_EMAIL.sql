-- Fix RLS policy for invites table to allow users to view invites for their own email
-- This is needed for the post-auth invite flow

-- Drop existing restrictive policies that might be blocking access
DROP POLICY IF EXISTS "Users can view invites for their company" ON invites;
DROP POLICY IF EXISTS "Allow all invites view" ON invites;
DROP POLICY IF EXISTS "select_invites_policy" ON invites;

-- Create new policy allowing users to view invites for their own email OR their company
CREATE POLICY "Users can view their own email invites or company invites"
ON public.invites
FOR SELECT
USING (
  -- Allow if the invite email matches the authenticated user's email
  auth.email() = email
  OR 
  -- Allow if the invite is for the user's company (for admin viewing pending invites)
  get_user_company_id() = "companyId"
);

-- Ensure the policy for admins to insert invites exists
DROP POLICY IF EXISTS "insert_invites_policy" ON invites;
CREATE POLICY "Admins can insert invites for their company"
ON public.invites
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM users
    WHERE users.id = auth.uid()
    AND users."companyId" = invites."companyId"
    AND users.role = 'admin'
  )
);

-- Ensure the policy for updating invites exists (for accepting invites)
DROP POLICY IF EXISTS "update_invites_policy" ON invites;
CREATE POLICY "Users can update their own invites"
ON public.invites
FOR UPDATE
USING (auth.email() = email)
WITH CHECK (auth.email() = email);
