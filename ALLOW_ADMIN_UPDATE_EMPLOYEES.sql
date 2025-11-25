-- Allow admins to update employees in their company
-- Fixed: Use different alias name (current_user is a reserved word in PostgreSQL)

-- Drop the existing restrictive policy
DROP POLICY IF EXISTS "Users can update their own record" ON public.users;

-- Create new policy: users can update their own record OR admins can update company employees
CREATE POLICY "Users can update own record or admins can update company employees"
ON public.users
FOR UPDATE
USING (
  -- User can update their own record
  auth.uid() = id
  OR
  -- Admin can update employees in their company
  EXISTS (
    SELECT 1 FROM public.users AS admin_user
    WHERE admin_user.id = auth.uid()
      AND admin_user.role = 'admin'
      AND admin_user."companyId" = users."companyId"
  )
);
