-- Add UPDATE policy so users can accept invites
CREATE POLICY "Users can update their own email invites"
ON public.invites
FOR UPDATE
USING (auth.email() = email);

-- Add companyName column to invites table
ALTER TABLE invites 
ADD COLUMN IF NOT EXISTS "companyName" TEXT;

-- Backfill existing invites with company names
UPDATE invites
SET "companyName" = companies.name
FROM companies
WHERE invites."companyId" = companies.id
AND invites."companyName" IS NULL;
