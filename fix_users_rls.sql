-- Allow admins to update users in their company (e.g. to remove them)
DROP POLICY IF EXISTS "Admins can update company users" ON public.users;

CREATE POLICY "Admins can update company users" ON public.users
FOR UPDATE USING (
  "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin')
) WITH CHECK (
  "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin') OR
  "companyId" IS NULL -- Allow setting companyId to null (removing user)
);
