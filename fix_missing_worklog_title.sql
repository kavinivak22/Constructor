-- Add title column to daily_worklogs table safely
ALTER TABLE public.daily_worklogs 
ADD COLUMN IF NOT EXISTS title TEXT;

-- Update existing records to have a default title to avoid null issues
UPDATE public.daily_worklogs 
SET title = 'Daily Log' 
WHERE title IS NULL OR title = '';

-- Optional: Make it not null after populating
-- ALTER TABLE public.daily_worklogs ALTER COLUMN title SET NOT NULL;
