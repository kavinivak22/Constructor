-- Fix RLS policy for companies table to allow members to view their company details

-- Drop existing policy if it exists to avoid conflicts (though this is a new policy name)
DROP POLICY IF EXISTS "Allow read access for company members" ON public.companies;

-- Create new policy allowing users to view their own company
-- We use the existing helper function get_auth_user_company_id() if available, 
-- or a direct subquery to be safe and self-contained.
-- Using a direct subquery here to ensure it works even if the helper function is missing or changed.

CREATE POLICY "Allow read access for company members" ON public.companies
FOR SELECT USING (
  id = (
    SELECT "companyId" 
    FROM public.users 
    WHERE id = auth.uid()
  )
);
