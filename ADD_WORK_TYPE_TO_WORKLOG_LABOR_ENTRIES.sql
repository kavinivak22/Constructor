-- Migration: Add work_type, payout_class, and work_done columns to worklog_labor_entries
-- Run this script in the Supabase SQL Editor (https://supabase.com -> Project -> SQL Editor)

ALTER TABLE public.worklog_labor_entries 
ADD COLUMN IF NOT EXISTS work_type text DEFAULT 'nmr',
ADD COLUMN IF NOT EXISTS payout_class text DEFAULT 'nmr',
ADD COLUMN IF NOT EXISTS work_done_quantity numeric,
ADD COLUMN IF NOT EXISTS work_done_unit text;

-- Notify PostgREST to reload schema cache
NOTIFY pgrst, 'reload schema';
