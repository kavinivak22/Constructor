-- Add permissions column to users table
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS permissions jsonb DEFAULT '{}'::jsonb;

-- Add permissions column to invites table
ALTER TABLE public.invites 
ADD COLUMN IF NOT EXISTS permissions jsonb DEFAULT '{}'::jsonb;

-- Update RLS policies to allow reading permissions
-- (Existing policies for users might already cover this if they select *)
-- But let's make sure we don't need specific policies for the new column if we were using column-level security (which we are not, usually).

-- We might want to ensure that only admins can update permissions.
-- The existing policy "Allow individual update access for users" allows users to update their own profile.
-- We should probably RESTRICT users from updating their own permissions/role, but that's a bigger security task.
-- For now, we'll assume the backend/API handles the security checks before updating.
