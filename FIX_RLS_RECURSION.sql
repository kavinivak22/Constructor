-- Fix for Infinite Recursion in RLS Policies

-- 1. Create a secure function to get the current user's company ID
-- This function runs with SECURITY DEFINER, meaning it bypasses RLS
CREATE OR REPLACE FUNCTION public.get_auth_user_company_id()
RETURNS uuid
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT "companyId" FROM public.users WHERE id = auth.uid();
$$;

-- 2. Update Users Policy to use the function
DROP POLICY IF EXISTS "Allow individual read access for users" ON public.users;

CREATE POLICY "Allow individual read access for users" ON public.users
FOR SELECT USING (
  (auth.uid() = id) OR
  (
    "companyId" IS NOT NULL AND
    "companyId" = get_auth_user_company_id()
  )
);

-- 3. Update Projects Policy (Optimization)
DROP POLICY IF EXISTS "Allow project access to assigned users" ON public.projects;

CREATE POLICY "Allow project access to assigned users" ON public.projects
FOR ALL USING (
    "companyId" = get_auth_user_company_id()
);

-- 4. Update Chat Messages Policy (Optimization)
DROP POLICY IF EXISTS "Allow chat access to project members" ON public.chat_messages;

CREATE POLICY "Allow chat access to project members" ON public.chat_messages
FOR ALL USING (
    project_id IN (
        SELECT id FROM public.projects WHERE "companyId" = get_auth_user_company_id()
    )
);

-- 5. Update Invites Policy (Optimization)
DROP POLICY IF EXISTS "Allow invite access" ON public.invites;

CREATE POLICY "Allow invite access" ON public.invites
FOR ALL USING (
    "companyId" = get_auth_user_company_id() AND 
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
) WITH CHECK (
    "companyId" = get_auth_user_company_id() AND 
    EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
);
