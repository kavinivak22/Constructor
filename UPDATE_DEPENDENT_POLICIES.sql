-- Update RLS policies to remove dependency on projects.userIds
-- and instead use users.projectIds

-- 1. Expenses Table Policies
DROP POLICY IF EXISTS "Users can view expenses for their projects" ON public.expenses;
DROP POLICY IF EXISTS "Users can create expenses for their projects" ON public.expenses;

CREATE POLICY "Users can view expenses for their projects" 
ON public.expenses FOR SELECT 
USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            "projectId" = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);

CREATE POLICY "Users can create expenses for their projects" 
ON public.expenses FOR INSERT 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            "projectId" = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);

-- 2. Project Materials Table Policies
DROP POLICY IF EXISTS "Users can view materials for projects they are part of" ON public.project_materials;
DROP POLICY IF EXISTS "Users can insert materials for projects they are part of" ON public.project_materials;
DROP POLICY IF EXISTS "Users can update materials for projects they are part of" ON public.project_materials;
DROP POLICY IF EXISTS "Users can delete materials for projects they are part of" ON public.project_materials;

CREATE POLICY "Users can view materials for projects they are part of" 
ON public.project_materials FOR SELECT 
USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            project_id = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);

CREATE POLICY "Users can insert materials for projects they are part of" 
ON public.project_materials FOR INSERT 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            project_id = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);

CREATE POLICY "Users can update materials for projects they are part of" 
ON public.project_materials FOR UPDATE 
USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            project_id = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);

CREATE POLICY "Users can delete materials for projects they are part of" 
ON public.project_materials FOR DELETE 
USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            project_id = ANY("projectIds") 
            OR role = 'admin'
        )
    )
);
