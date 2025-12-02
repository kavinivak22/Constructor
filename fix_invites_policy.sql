-- Fix RLS policy for invites table to allow users to view invites for their own email
-- This is needed for the post-auth invite flow where the user might not be in the company yet

-- Drop existing restrictive policies that might be blocking access
DROP POLICY IF EXISTS "Users can view invites for their company" ON invites;
DROP POLICY IF EXISTS "Allow all invites view" ON invites;
DROP POLICY IF EXISTS "select_invites_policy" ON invites;
DROP POLICY IF EXISTS "Users can view their own email invites or company invites" ON invites;

-- Create new policy allowing users to view invites for their own email OR their company
CREATE POLICY "Users can view their own email invites or company invites"
ON public.invites
FOR SELECT
USING (
  -- Allow if the invite email matches the authenticated user's email
  auth.email() = email
  OR 
  -- Allow if the invite is for the user's company (for admin viewing pending invites)
  (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE users.id = auth.uid() 
        AND users."companyId" = invites."companyId"
    )
  )
);
