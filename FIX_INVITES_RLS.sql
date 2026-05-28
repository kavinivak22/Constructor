-- Create a secure function to check if user is admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin');
$$;

-- Ensure get_auth_user_company_id exists and is secure
CREATE OR REPLACE FUNCTION public.get_auth_user_company_id()
RETURNS uuid
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT "companyId" FROM public.users WHERE id = auth.uid();
$$;

-- Drop existing policy
DROP POLICY IF EXISTS "Allow invite access" ON public.invites;

-- Create new policy using secure functions
CREATE POLICY "Allow invite access" ON public.invites
FOR ALL USING (
    "companyId" = get_auth_user_company_id() AND 
    is_admin()
) WITH CHECK (
    "companyId" = get_auth_user_company_id() AND 
    is_admin()
);
