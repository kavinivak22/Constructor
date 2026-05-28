
-- Enable RLS on invites if not already enabled (it should be)
ALTER TABLE public.invites ENABLE ROW LEVEL SECURITY;

-- Policy to allow users to view invites sent to their email
CREATE POLICY "Users can view their own invites"
ON public.invites
FOR SELECT
USING (
  email = auth.email()
);
