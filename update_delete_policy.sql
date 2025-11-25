-- Drop the existing delete policy
drop policy if exists "Users can delete their own documents" on documents;

-- Create a new delete policy that allows uploader OR admin to delete
create policy "Users can delete their own documents or admins"
on documents for delete
to authenticated
using (
  auth.uid() = uploader_id 
  or 
  exists (
    select 1 from users 
    where id = auth.uid() 
    and role = 'admin'
  )
);
