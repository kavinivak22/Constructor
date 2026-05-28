-- Create project_materials table
CREATE TABLE IF NOT EXISTS public.project_materials (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,
    name text NOT NULL,
    category text,
    quantity numeric DEFAULT 0,
    unit text,
    status text DEFAULT 'needed' CHECK (status IN ('needed', 'ordered', 'delivered')),
    supplier text,
    cost numeric DEFAULT 0,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.project_materials ENABLE ROW LEVEL SECURITY;

-- Create policies
CREATE POLICY "Users can view materials for projects they are part of" 
ON public.project_materials FOR SELECT 
USING (
    EXISTS (
        SELECT 1 FROM public.projects 
        WHERE id = project_materials.project_id 
        AND (
            auth.uid() = ANY(projects."userIds") 
            OR 
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);

CREATE POLICY "Users can insert materials for projects they are part of" 
ON public.project_materials FOR INSERT 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.projects 
        WHERE id = project_id 
        AND (
            auth.uid() = ANY(projects."userIds")
            OR 
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);

CREATE POLICY "Users can update materials for projects they are part of" 
ON public.project_materials FOR UPDATE 
USING (
    EXISTS (
        SELECT 1 FROM public.projects 
        WHERE id = project_materials.project_id 
        AND (
            auth.uid() = ANY(projects."userIds")
            OR 
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);

CREATE POLICY "Users can delete materials for projects they are part of" 
ON public.project_materials FOR DELETE 
USING (
    EXISTS (
        SELECT 1 FROM public.projects 
        WHERE id = project_materials.project_id 
        AND (
            auth.uid() = ANY(projects."userIds")
            OR 
            EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'admin')
        )
    )
);
