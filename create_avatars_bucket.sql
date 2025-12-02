-- Create a bucket for user avatars
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true);

-- Policy to allow authenticated users to upload their own avatar
create policy "Authenticated users can upload avatars"
on storage.objects for insert
to authenticated
with check ( bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1] );

-- Policy to allow public to view avatars
create policy "Public can view avatars"
on storage.objects for select
to public
using ( bucket_id = 'avatars' );

-- Policy to allow users to update their own avatar
create policy "Users can update own avatar"
on storage.objects for update
to authenticated
using ( bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1] );

-- Policy to allow users to delete their own avatar
create policy "Users can delete own avatar"
on storage.objects for delete
to authenticated
using ( bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1] );
