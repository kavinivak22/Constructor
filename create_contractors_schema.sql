-- Create contractors table
CREATE TABLE IF NOT EXISTS public.contractors (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    "companyId" uuid NOT NULL REFERENCES public.companies(id) ON DELETE CASCADE,
    name text NOT NULL,
    category text,
    "contactPerson" text,
    phone text,
    email text,
    "createdAt" timestamp with time zone DEFAULT now() NOT NULL,
    "updatedAt" timestamp with time zone DEFAULT now() NOT NULL
);

-- Enable RLS
ALTER TABLE public.contractors ENABLE ROW LEVEL SECURITY;

-- RLS Policies

-- Allow read access to members of the same company
CREATE POLICY "Allow read access to company members" ON public.contractors
FOR SELECT USING (
    "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
);

-- Allow insert access to members of the same company
CREATE POLICY "Allow insert access to company members" ON public.contractors
FOR INSERT WITH CHECK (
    "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
);

-- Allow update access to members of the same company
CREATE POLICY "Allow update access to company members" ON public.contractors
FOR UPDATE USING (
    "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
);

-- Allow delete access to members of the same company
CREATE POLICY "Allow delete access to company members" ON public.contractors
FOR DELETE USING (
    "companyId" = (SELECT "companyId" FROM public.users WHERE id = auth.uid())
);
