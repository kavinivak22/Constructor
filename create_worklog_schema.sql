-- Create Daily Worklogs Schema

-- 1. daily_worklogs table
CREATE TABLE IF NOT EXISTS public.daily_worklogs (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,
    date date NOT NULL,
    created_by uuid NOT NULL REFERENCES public.users(id),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.daily_worklogs ENABLE ROW LEVEL SECURITY;

-- Policies for daily_worklogs
-- Access if user is admin OR if project_id is in user's projectIds array
CREATE POLICY "Users can view worklogs for their projects" ON public.daily_worklogs
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            role = 'admin' 
            OR 
            daily_worklogs.project_id = ANY("projectIds")
        )
    )
);

CREATE POLICY "Users can create worklogs for their projects" ON public.daily_worklogs
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() 
        AND (
            role = 'admin' 
            OR 
            project_id = ANY("projectIds")
        )
    )
);

-- 2. worklog_labor_entries table (Contractor/Team level)
CREATE TABLE IF NOT EXISTS public.worklog_labor_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    worklog_id uuid NOT NULL REFERENCES public.daily_worklogs(id) ON DELETE CASCADE,
    contractor_name text NOT NULL,
    category text, -- e.g., 'Masonry', 'Painting'
    work_description text,
    payment_status text DEFAULT 'Pending' CHECK (payment_status IN ('Paid', 'On Payday', 'Pending')),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.worklog_labor_entries ENABLE ROW LEVEL SECURITY;

-- Policies for worklog_labor_entries (inherit from worklog)
CREATE POLICY "Users can view labor entries for their projects" ON public.worklog_labor_entries
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_labor_entries.worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

CREATE POLICY "Users can create labor entries for their projects" ON public.worklog_labor_entries
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

-- 3. worklog_worker_counts table (Dynamic worker types)
CREATE TABLE IF NOT EXISTS public.worklog_worker_counts (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    labor_entry_id uuid NOT NULL REFERENCES public.worklog_labor_entries(id) ON DELETE CASCADE,
    worker_type text NOT NULL, -- e.g., 'Mason', 'Helper'
    count numeric NOT NULL DEFAULT 0,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.worklog_worker_counts ENABLE ROW LEVEL SECURITY;

-- Policies for worklog_worker_counts
CREATE POLICY "Users can view worker counts for their projects" ON public.worklog_worker_counts
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.worklog_labor_entries
        JOIN public.daily_worklogs ON worklog_labor_entries.worklog_id = daily_worklogs.id
        WHERE worklog_labor_entries.id = worklog_worker_counts.labor_entry_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

CREATE POLICY "Users can create worker counts for their projects" ON public.worklog_worker_counts
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.worklog_labor_entries
        JOIN public.daily_worklogs ON worklog_labor_entries.worklog_id = daily_worklogs.id
        WHERE worklog_labor_entries.id = labor_entry_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

-- 4. worklog_materials table
CREATE TABLE IF NOT EXISTS public.worklog_materials (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    worklog_id uuid NOT NULL REFERENCES public.daily_worklogs(id) ON DELETE CASCADE,
    project_material_id uuid REFERENCES public.project_materials(id),
    material_name text NOT NULL, -- Fallback name if project_material_id is null or just for display
    quantity_consumed numeric NOT NULL DEFAULT 0,
    unit text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.worklog_materials ENABLE ROW LEVEL SECURITY;

-- Policies for worklog_materials
CREATE POLICY "Users can view worklog materials for their projects" ON public.worklog_materials
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_materials.worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

CREATE POLICY "Users can create worklog materials for their projects" ON public.worklog_materials
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

-- 5. worklog_photos table
CREATE TABLE IF NOT EXISTS public.worklog_photos (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    worklog_id uuid NOT NULL REFERENCES public.daily_worklogs(id) ON DELETE CASCADE,
    photo_url text NOT NULL,
    caption text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.worklog_photos ENABLE ROW LEVEL SECURITY;

-- Policies for worklog_photos
CREATE POLICY "Users can view worklog photos for their projects" ON public.worklog_photos
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_photos.worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

CREATE POLICY "Users can create worklog photos for their projects" ON public.worklog_photos
FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.daily_worklogs
        WHERE daily_worklogs.id = worklog_id
        AND EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() 
            AND (
                role = 'admin' 
                OR 
                daily_worklogs.project_id = ANY("projectIds")
            )
        )
    )
);

-- 6. Trigger to deduct material inventory
CREATE OR REPLACE FUNCTION public.deduct_material_inventory()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF NEW.project_material_id IS NOT NULL THEN
        UPDATE public.project_materials
        SET quantity = quantity - NEW.quantity_consumed
        WHERE id = NEW.project_material_id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_worklog_material_insert ON public.worklog_materials;
CREATE TRIGGER on_worklog_material_insert
    AFTER INSERT ON public.worklog_materials
    FOR EACH ROW
    EXECUTE FUNCTION public.deduct_material_inventory();

-- Create Storage Bucket for Worklog Photos if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('worklog_photos', 'worklog_photos', true)
ON CONFLICT (id) DO NOTHING;

-- Storage Policies
-- Drop existing policies if they exist to avoid errors on re-run
DROP POLICY IF EXISTS "Give users access to own folder 1u74u_0" ON storage.objects;
DROP POLICY IF EXISTS "Give users access to own folder 1u74u_1" ON storage.objects;
DROP POLICY IF EXISTS "Give users access to own folder 1u74u_2" ON storage.objects;
DROP POLICY IF EXISTS "Give users access to own folder 1u74u_3" ON storage.objects;

CREATE POLICY "Give users access to own folder 1u74u_0" ON storage.objects FOR SELECT TO public USING (bucket_id = 'worklog_photos' AND auth.role() = 'authenticated');
CREATE POLICY "Give users access to own folder 1u74u_1" ON storage.objects FOR INSERT TO public WITH CHECK (bucket_id = 'worklog_photos' AND auth.role() = 'authenticated');
CREATE POLICY "Give users access to own folder 1u74u_2" ON storage.objects FOR UPDATE TO public USING (bucket_id = 'worklog_photos' AND auth.role() = 'authenticated');
CREATE POLICY "Give users access to own folder 1u74u_3" ON storage.objects FOR DELETE TO public USING (bucket_id = 'worklog_photos' AND auth.role() = 'authenticated');
