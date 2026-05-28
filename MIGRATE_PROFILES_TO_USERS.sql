-- Migration Script: Synchronize Users from Profiles with Schema Updates
-- This script first adds missing columns to 'users' and then migrates the data.

-- 1. Schema Updates: Add missing columns to users table
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS "companyName" text,
ADD COLUMN IF NOT EXISTS "createdAt" timestamp with time zone DEFAULT now();

-- 2. Data Migration: Update users table with data from profiles
UPDATE public.users u
SET
    -- Critical Fix: Restore Company ID
    "companyId" = COALESCE(u."companyId", p.company_id),
    
    -- Restore Basic Profile Info
    "displayName" = COALESCE(u."displayName", p.full_name),
    "photoURL" = COALESCE(u."photoURL", p.avatar_url),
    "phone" = COALESCE(u."phone", p.phone),
    
    -- New Columns (No COALESCE needed for new empty columns, but good practice if rerunning)
    "companyName" = COALESCE(u."companyName", p.company_name),
    "createdAt" = COALESCE(u."createdAt", p.created_at),
    
    -- Ensure status is active
    status = 'active'
FROM public.profiles p
WHERE u.id = p.id;

-- 3. Verification: Output the results
SELECT 
    u.id, 
    u.email, 
    u."displayName", 
    u."companyId", 
    u."companyName",
    u."createdAt",
    u."photoURL"
FROM public.users u
JOIN public.profiles p ON u.id = p.id;
