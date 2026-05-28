-- Ensure material_logs table exists
CREATE TABLE IF NOT EXISTS public.material_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    material_id uuid NOT NULL REFERENCES public.project_materials(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES public.users(id),
    change_amount numeric NOT NULL,
    purpose text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.material_logs ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Users can view logs for project materials" ON public.material_logs;
DROP POLICY IF EXISTS "Users can insert logs for project materials" ON public.material_logs;

-- Re-create policies using companyId for broader compatibility
CREATE POLICY "Users can view logs for project materials" 
ON public.material_logs FOR SELECT 
USING (
    EXISTS (
        SELECT 1 FROM public.project_materials pm
        JOIN public.projects p ON pm.project_id = p.id
        WHERE pm.id = material_logs.material_id
        AND (
            -- Check if user is in the same company as the project
            p."companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
            OR 
            -- Fallback to admin check
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);

CREATE POLICY "Users can insert logs for project materials" 
ON public.material_logs FOR INSERT 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.project_materials pm
        JOIN public.projects p ON pm.project_id = p.id
        WHERE pm.id = material_id
        AND (
            p."companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
            OR 
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);

-- Ensure users can view other users (needed for the join to display names)
DROP POLICY IF EXISTS "Users can view all users" ON public.users;
CREATE POLICY "Users can view all users" ON public.users FOR SELECT USING (auth.role() = 'authenticated');
