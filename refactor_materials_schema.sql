-- Refactor project_materials table
ALTER TABLE public.project_materials 
DROP COLUMN IF EXISTS status,
ADD COLUMN IF NOT EXISTS min_quantity numeric DEFAULT 0;

-- Create notifications table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title text NOT NULL,
    message text NOT NULL,
    read boolean DEFAULT false,
    type text DEFAULT 'info', -- 'info', 'warning', 'success', 'error'
    link text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS for notifications
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Create policies for notifications
CREATE POLICY "Users can view their own notifications" 
ON public.notifications FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own notifications" 
ON public.notifications FOR UPDATE 
USING (auth.uid() = user_id);

-- Allow system/admin to insert notifications (or users for themselves/others if needed)
-- For now, we'll allow authenticated users to insert notifications (e.g. triggered by actions)
CREATE POLICY "Authenticated users can insert notifications" 
ON public.notifications FOR INSERT 
WITH CHECK (auth.role() = 'authenticated');
