-- Drop existing tables and policies to ensure a clean slate.
-- The 'CASCADE' option will also remove dependent objects like policies and triggers.
DROP TABLE IF EXISTS public.companies CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- Create the companies table
CREATE TABLE public.companies (
  id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  name text NOT NULL,
  "ownerId" uuid NOT NULL,
  address text,
  phone text,
  website text,
  "businessType" text,
  "companySize" text,
  "createdAt" timestamp with time zone DEFAULT now() NOT NULL
);

-- Create the users table
CREATE TABLE public.users (
  id uuid NOT NULL PRIMARY KEY,
  email text,
  full_name text,
  phone text,
  photo_url text,
  role text DEFAULT 'member'::text,
  status text DEFAULT 'active'::text,
  company_id uuid,
  project_ids uuid[] DEFAULT ARRAY[]::uuid[]
);

-- Add a foreign key constraint to link users to companies
ALTER TABLE public.users ADD CONSTRAINT users_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE SET NULL;

-- Enable Row Level Security (RLS) for all tables
ALTER TABLE public.companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

--
-- TRIGGER: This function is called by a trigger whenever a new user is created in Supabase's internal 'auth.users' table.
-- It copies the new user's data into our publicly accessible 'public.users' table.
--
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.users (id, email, full_name, photo_url)
  values (
    new.id,
    new.email,
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'avatar_url'
  );
  return new;
end;
$$;

-- Create the trigger that executes the handle_new_user function
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();


--
-- POLICIES
--

-- Helper function to get a user's company ID
CREATE OR REPLACE FUNCTION public.get_my_company_id()
RETURNS UUID AS $$
  SELECT company_id FROM public.users WHERE id = auth.uid();
$$ LANGUAGE sql STABLE;

-- COMPANIES table policies
ALTER POLICY "Users can create their own company" ON "public"."companies" AS PERMISSIVE FOR INSERT TO public WITH CHECK ((auth.uid() = "ownerId"));
CREATE POLICY "Admins can view their own company" ON "public"."companies" AS PERMISSIVE FOR SELECT TO public USING ((get_my_company_id() = id));

-- USERS table policies
CREATE POLICY "Users can view other users in their company" ON public.users FOR SELECT USING (get_my_company_id() = company_id);
CREATE POLICY "Users can update their own profile" ON public.users FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- TEST_WRITES table policies (if you have this for testing)
-- Assuming a test_writes table exists: CREATE TABLE public.test_writes (id serial primary key, user_id uuid, message text);
-- ALTER TABLE public.test_writes ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "Users can write to test table" ON public.test_writes FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
