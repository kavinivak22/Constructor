-- Create a bucket for project images
insert into storage.buckets (id, name, public)
values ('project-images', 'project-images', true);

-- Create a bucket for project documents
insert into storage.buckets (id, name, public)
values ('project-documents', 'project-documents', true);

-- Policy to allow authenticated users to upload images
create policy "Authenticated users can upload project images"
on storage.objects for insert
to authenticated
with check ( bucket_id = 'project-images' );

-- Policy to allow public to view project images
create policy "Public can view project images"
on storage.objects for select
to public
using ( bucket_id = 'project-images' );

-- Policy to allow authenticated users to upload documents
create policy "Authenticated users can upload project documents"
on storage.objects for insert
to authenticated
with check ( bucket_id = 'project-documents' );

-- Policy to allow authenticated users to view documents
create policy "Authenticated users can view project documents"
on storage.objects for select
to authenticated
using ( bucket_id = 'project-documents' );

-- Create a table to track documents metadata
create table if not exists documents (
  id uuid default gen_random_uuid() primary key,
  project_id uuid references projects(id) on delete cascade,
  uploader_id uuid references users(id),
  name text not null,
  url text not null,
  size bigint,
  type text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable RLS on documents table
alter table documents enable row level security;

-- Policy for documents table
create policy "Users can view documents for their projects"
on documents for select
to authenticated
using (
  exists (
    select 1 from projects
    where projects.id = documents.project_id
    -- Add check for user membership in project if needed, for now assuming all auth users can see
  )
);

create policy "Users can insert documents"
on documents for insert
to authenticated
with check (true);
