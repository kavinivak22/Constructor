-- Fix RLS policy violation for projects table creation and role matching
-- 1. Update has_role function to check both user_roles table AND users table role column
CREATE OR REPLACE FUNCTION public.has_role(_user_id uuid, _role app_role)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.user_roles
    WHERE user_id = _user_id
      AND role = _role
  ) OR EXISTS (
    SELECT 1
    FROM public.users
    WHERE id = _user_id
      AND (
        role = _role::text
        OR (role = 'admin')
        OR (_role::text = 'project_manager' AND role IN ('admin', 'manager', 'project_manager'))
        OR (_role::text = 'supervisor' AND role IN ('admin', 'manager', 'project_manager', 'supervisor'))
        OR (_role::text = 'worker' AND role IN ('admin', 'manager', 'project_manager', 'supervisor', 'worker', 'member'))
      )
  );
$$;

-- 2. Fix sync_user_role_company trigger function to select from public.users instead of non-existent public.profiles
CREATE OR REPLACE FUNCTION public.sync_user_role_company()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.company_id IS NULL THEN
    SELECT company_id INTO NEW.company_id
    FROM public.users
    WHERE id = NEW.user_id;
  END IF;
  
  RETURN NEW;
END;
$$;

-- 3. Backfill user_roles for existing users
INSERT INTO public.user_roles (user_id, role, company_id, status)
SELECT 
  id, 
  CASE 
    WHEN role = 'admin' THEN 'admin'::app_role
    WHEN role IN ('manager', 'project_manager') THEN 'project_manager'::app_role
    WHEN role = 'supervisor' THEN 'supervisor'::app_role
    ELSE 'worker'::app_role
  END as role,
  company_id,
  'active'
FROM public.users u
WHERE u.company_id IS NOT NULL 
  AND NOT EXISTS (
    SELECT 1 FROM public.user_roles ur WHERE ur.user_id = u.id
  );

-- 4. Create trigger to automatically sync users to user_roles on insert/update
CREATE OR REPLACE FUNCTION public.sync_users_to_user_roles()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  target_role app_role;
BEGIN
  IF NEW.company_id IS NOT NULL AND NEW.role IS NOT NULL THEN
    target_role := CASE 
      WHEN NEW.role = 'admin' THEN 'admin'::app_role
      WHEN NEW.role IN ('manager', 'project_manager') THEN 'project_manager'::app_role
      WHEN NEW.role = 'supervisor' THEN 'supervisor'::app_role
      ELSE 'worker'::app_role
    END;

    INSERT INTO public.user_roles (user_id, role, company_id, status)
    VALUES (NEW.id, target_role, NEW.company_id, 'active')
    ON CONFLICT (user_id, role) DO UPDATE 
    SET company_id = EXCLUDED.company_id, status = 'active';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS sync_users_to_user_roles_trigger ON public.users;

CREATE TRIGGER sync_users_to_user_roles_trigger
AFTER INSERT OR UPDATE OF role, company_id ON public.users
FOR EACH ROW
EXECUTE FUNCTION sync_users_to_user_roles();
