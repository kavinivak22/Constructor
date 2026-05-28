-- Profile Enhancement Schema Changes

-- 1. Create profile_change_requests table
CREATE TABLE public.profile_change_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    company_id uuid NOT NULL REFERENCES public.companies(id) ON DELETE CASCADE,
    field_name text NOT NULL, -- 'displayName' or 'phone'
    new_value text,
    status text DEFAULT 'pending'::text NOT NULL, -- 'pending', 'approved', 'rejected'
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.profile_change_requests ENABLE ROW LEVEL SECURITY;

-- RLS Policies for profile_change_requests

-- Users can view their own requests
CREATE POLICY "Users can view own requests" ON public.profile_change_requests
FOR SELECT USING (auth.uid() = user_id);

-- Users can create requests for themselves
CREATE POLICY "Users can create own requests" ON public.profile_change_requests
FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Admins can view all requests for their company
CREATE POLICY "Admins can view company requests" ON public.profile_change_requests
FOR SELECT USING (
    company_id = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin')
);

-- Admins can update requests for their company (approve/reject)
CREATE POLICY "Admins can update company requests" ON public.profile_change_requests
FOR UPDATE USING (
    company_id = (SELECT "companyId" FROM public.users WHERE id = auth.uid() AND role = 'admin')
);

-- 2. Modify notifications table to make project_id nullable
ALTER TABLE public.notifications ALTER COLUMN project_id DROP NOT NULL;

-- Update notifications policy to handle null project_id if necessary
-- The existing policy "Allow notification access to the recipient" checks user_id = auth.uid(), which is still valid.
