-- Drop the userIds column from the projects table
-- This should be run AFTER verifying that users.projectIds has been correctly backfilled.

ALTER TABLE projects DROP COLUMN "userIds";
