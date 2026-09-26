-- Add accountDetails and gst columns to contractors table
ALTER TABLE public.contractors 
ADD COLUMN IF NOT EXISTS "accountDetails" text,
ADD COLUMN IF NOT EXISTS gst text;
