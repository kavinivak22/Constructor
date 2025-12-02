-- Function to check if current user is admin (Security Definer to avoid recursion)
CREATE OR REPLACE FUNCTION public.auth_is_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.users 
    WHERE id = auth.uid() 
    AND role = 'admin'
  );
$$;

-- Drop the previous policy
DROP POLICY IF EXISTS "Admins can update company users" ON public.users;

-- Re-create policy using the safe function
CREATE POLICY "Admins can update company users" ON public.users
FOR UPDATE USING (
  auth_is_admin() AND 
  "companyId" = get_auth_user_company_id()
) WITH CHECK (
  auth_is_admin() AND 
  (
    "companyId" = get_auth_user_company_id() OR
    "companyId" IS NULL -- Allow removing user
  )
);
