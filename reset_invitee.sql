
DO $$
DECLARE
    v_user_id uuid;
    v_company_id uuid;
BEGIN
    -- Get admin company ID
    SELECT "companyId" INTO v_company_id 
    FROM public.users 
    WHERE email = 'admin_test_123@example.com';

    -- Ensure user exists (if not, create auth user - simplified for test)
    -- Actually, we can't easily create auth users via SQL. 
    -- We assume the user exists from previous step or we will fail and ask to signup.
    -- Let's check if user exists in public.users
    SELECT id INTO v_user_id FROM public.users WHERE email = 'invitee_flow_test@example.com';

    IF v_user_id IS NOT NULL THEN
        -- Unlink company
        UPDATE public.users SET "companyId" = NULL WHERE id = v_user_id;
        RAISE NOTICE 'Unlinked company for user';
    ELSE
        RAISE NOTICE 'User not found, will need to signup first';
    END IF;

    -- Ensure invite exists
    DELETE FROM public.invites WHERE email = 'invitee_flow_test@example.com';
    INSERT INTO public.invites (email, role, "companyId", status, "projectIds", "createdAt")
    VALUES ('invitee_flow_test@example.com', 'member', v_company_id, 'pending', '{}', now());
    
    RAISE NOTICE 'Reset invite for user';
END $$;
