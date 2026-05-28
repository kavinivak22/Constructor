-- Migration Script: Backfill users.projectIds from projects.userIds
-- Run this script in the Supabase SQL Editor to migrate project assignments.

DO $$
DECLARE
    proj RECORD;
    uid TEXT;
    user_exists BOOLEAN;
BEGIN
    -- 1. Ensure all users have an initialized projectIds array (no nulls)
    UPDATE users SET "projectIds" = '{}' WHERE "projectIds" IS NULL;

    -- 2. Iterate through all projects
    FOR proj IN SELECT id, "userIds" FROM projects LOOP
        
        -- Check if userIds is not null and has elements
        IF proj."userIds" IS NOT NULL AND array_length(proj."userIds", 1) > 0 THEN
            
            -- Iterate through each user ID in the project's userIds array
            FOREACH uid IN ARRAY proj."userIds" LOOP
                
                -- Check if the user exists to avoid foreign key errors (though users table usually matches)
                -- We cast uid to uuid because users.id is usually uuid, but userIds in projects might be text.
                -- Adjust casting if necessary. Assuming both are compatible.
                
                BEGIN
                    UPDATE users
                    SET "projectIds" = array_append("projectIds", proj.id)
                    WHERE id = uid::uuid
                    AND NOT ("projectIds" @> ARRAY[proj.id]); -- Prevent duplicates
                EXCEPTION WHEN OTHERS THEN
                    RAISE NOTICE 'Could not update user % for project %: %', uid, proj.id, SQLERRM;
                END;
                
            END LOOP;
        END IF;
    END LOOP;
END $$;
