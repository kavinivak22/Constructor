-- Add title column to daily_worklogs table
ALTER TABLE daily_worklogs 
ADD COLUMN title TEXT;

-- Update existing records to have a default title (optional but good for consistency)
UPDATE daily_worklogs 
SET title = 'Daily Log' 
WHERE title IS NULL;
