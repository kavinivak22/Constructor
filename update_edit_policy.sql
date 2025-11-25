-- Create a policy that allows uploader OR admin to update documents
create policy "Users can update their own documents or admins"
on documents for update
to authenticated
using (
  auth.uid() = uploader_id 
  or 
  exists (
    select 1 from users 
    where id = auth.uid() 
    and role = 'admin'
  )
)
with check (
  auth.uid() = uploader_id 
  or 
  exists (
    select 1 from users 
    where id = auth.uid() 
    and role = 'admin'
  )
);
