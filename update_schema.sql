-- Add category column to documents table
alter table documents add column if not exists category text default 'General';

-- Create a table to track document reads
create table if not exists document_reads (
  id uuid default gen_random_uuid() primary key,
  document_id uuid references documents(id) on delete cascade,
  user_id uuid references users(id) on delete cascade,
  read_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(document_id, user_id)
);

-- Enable RLS on document_reads table
alter table document_reads enable row level security;

-- Policy for document_reads table
create policy "Users can insert their own reads"
on document_reads for insert
to authenticated
with check (auth.uid() = user_id);

create policy "Users can view their own reads"
on document_reads for select
to authenticated
using (auth.uid() = user_id);
