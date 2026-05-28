-- Create employment_history table to track past employees
CREATE TABLE IF NOT EXISTS public.employment_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES auth.users(id),
    company_id uuid NOT NULL REFERENCES public.companies(id),
    role text,
    join_date timestamp with time zone,
    exit_date timestamp with time zone DEFAULT now() NOT NULL,
    exit_reason text, -- 'resigned', 'removed'
    user_details jsonb -- Snapshot of name, email, phone, photoURL
);

-- Enable RLS
ALTER TABLE public.employment_history ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any (to allow re-running)
DROP POLICY IF EXISTS "Admins can view company history" ON public.employment_history;
DROP POLICY IF EXISTS "Users can insert own history" ON public.employment_history;
DROP POLICY IF EXISTS "Admins can insert company history" ON public.employment_history;

-- Policies

-- 1. Admins can view history for their company
CREATE POLICY "Admins can view company history" ON public.employment_history
FOR SELECT USING (
    company_id = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin')
);

-- 2. Users can insert their own history (for resignation)
CREATE POLICY "Users can insert own history" ON public.employment_history
FOR INSERT WITH CHECK (
    auth.uid() = user_id
);

-- 3. Admins can insert history (for removing employees - future proofing)
CREATE POLICY "Admins can insert company history" ON public.employment_history
FOR INSERT WITH CHECK (
    company_id = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin')
);
