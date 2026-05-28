
CREATE POLICY "Allow all invites view"
ON public.invites
FOR SELECT
USING (true);
