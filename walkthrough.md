# Project Assignment Refactoring Walkthrough

## Overview
We have refactored the project assignment logic to use `users.projectIds` as the single source of truth, removing the data duplication where `projects.userIds` was also used. This simplifies data management and improves consistency.

## Changes Made

### 1. Data Structure
- **Removed** `userIds` and `team` from the `Project` type definition.
- **Updated** `users` table usage to be the primary source for project membership.

### 2. Codebase Updates
- **Project Creation**: Now updates the creator's `projectIds` in the `users` table immediately after creating a project.
- **Project Fetching**: Dashboard, Project List, Worklogs, and Profile pages now fetch projects using `.in('id', user.projectIds)`.
- **Access Control**: Project details page now verifies access against `user.projectIds`.
- **Member Counts**: Now fetched dynamically from the `users` table (`.contains('projectIds', [projectId])`) instead of relying on a static array in the project record.
- **Chat Notifications**: Now fetch recipients from the `users` table.

### 8. Contractor Loading and Saving Fix ✅
* **Source Updates**:
  - Modified [contractors.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/actions/contractors.ts) in both `getContractors` and `createContractor` functions to select `company_id` instead of `companyId` from the `users` table, as the `users` table uses snake_case schema naming. This resolved the "No company found" error that prevented new contractors from being saved.

### 9. Materials Dropdown & Inventory Schema Realignment ✅
* **Database Alignment**: Verified database constraints on `material_logs` and `worklog_materials` point directly to the unified `materials` (inventory) table instead of `project_materials`.
* **Source Updates**:
  - Modified [materials.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/actions/materials.ts) to update `getProjectMaterials`, `updateMaterialStock`, and `getMaterialLogs` to query the `materials` table. Mapped snake_case column names (`current_stock`, `minimum_stock_level`, `unit_of_measurement`, `supplier_name`, `unit_cost`) to the camelCase keys expected by the frontend. Resolved relationship queries by aliasing `users(displayName:display_name, email)`.
  - Modified [page.tsx](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/projects/[projectId]/materials/page.tsx) to load from and insert to the `materials` table instead of `project_materials`, mapping fields to keep full compatibility with client-side state.
  - Modified [actions.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/material-estimation/actions.ts) to insert imported estimation items to the `materials` table.
* **Verification**: Ran `npm run typecheck` which passed successfully. Verified Next.js dev server running on port 9002 compiles all routes cleanly.

## Migration Instructions

Since we cannot run the migration automatically due to database permissions (RLS), please execute the following SQL scripts in your **Supabase SQL Editor**:

### Step 1: Migrate Data
Run the content of `MIGRATE_PROJECT_ASSIGNMENTS.sql`.
This script will:
1.  Initialize| `src/components/dashboard/project-card.tsx` | Mapped `startDate` -> `start_date || startDate` and `clientName` -> `client_name || clientName` for database snake_case compatibility. |
| `src/app/projects/create/page.tsx` | Mapped form's camelCase values to snake_case DB columns (`start_date`, `end_date`, `client_name`, `client_contact`, `project_type`, `company_id`, `thumbnail_url`) on insert, and updated default status to `'active'`. |
| `src/hooks/queries/use-projects.ts` | In `useCreateProject` and `useUpdateProject` hooks, added mapping of camelCase input parameters to their snake_case database columns and mapped UI status values to match the DB CHECK constraint values. |

## Build Result ✅
`npm run build` → **Exit code 0**, all 27 routes compiled successfully (verified after updates to project creation and project card properties).

## Supabase Database Restore ✅
- Successfully restored the paused Supabase project (`yrleyquvxogcgbgbrmfl`) back to `ACTIVE_HEALTHY` state.
- Audited the exact columns of the `projects` and `companies` tables to verify snake_case column names and constraints.

## Verification
- [x] Code changes implemented and verified statically.
- [ ] Data migration pending user execution.

### 10. Daily Worklog Form Validation Errors Fix ✅
* **Problem**: When attempting to save the daily worklog form, validation failed with an empty validation error object. This was caused by the `workers` array in `laborEntrySchema` requiring a minimum of 1 element (`.min(1)`), which failed validation because the UI did not render input fields for worker counts (making the array empty `[]` when loading existing items or empty defaults when adding new teams).
* **Fix**: Restored strict validation after implementing the UI (see Section 11).

### 11. Labor Team Worker Counts Logging UI ✅
* **Nested Field Array**: Implemented a nested `useFieldArray` hook for `labor.${index}.workers` inside the `LaborEntryForm` component in [create-worklog-dialog.tsx](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/components/worklog/create-worklog-dialog.tsx).
* **Presets & Custom Categories Selection**:
  - Provided a category dropdown containing options: `Mason`, `MC` (Male Coolie), `FC` (Female Coolie), `Helper`, `Supervisor`, `Electrician`, `Plumber`, `Carpenter`, and `Custom...`.
  - Added an inline conditional input field for custom worker types when `Custom...` is selected, allowing users to type in their own custom worker roles directly.
  - Linked a count input field (`type="number"`) for specifying the count for each category.
* **Totals Calculation**: Added a reactive total worker counter at the bottom of each labor card that calculates and displays the sum of all worker counts in that team.
* **Schema Validation & Defaults**:
  - Restored strict `.min(1)` validations on the client and server [worklogs.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/actions/worklogs.ts) so that each team is validated to have at least one worker logged.
  - Set default appended teams to contain one preset worker log (`Mason`, count `1`) out of the box.
* **Verification**: Ran `npm run typecheck` which passed successfully. Verified dev server compiles and updates cleanly.

### 12. Missing Daily Worklogs Title Column Fix ✅
* **Problem**: The `daily_worklogs` table did not have a `title` column in the Supabase database schema, causing "Could not find the 'title' column of 'daily_worklogs' in the schema cache" errors when trying to create or edit worklogs.
* **Fix**: Executed the SQL migration script `fix_missing_worklog_title.sql` via Supabase to add the `title` column (as `TEXT`) and set a default title (`'Daily Log'`) for existing logs. Verified that the column is now present and mapped correctly in the database schema.

### 13. Worklog Editing/Deleting "Worklog not found" Fix ✅
* **Problem**: When editing or deleting a daily worklog, it threw a "Worklog not found" error. This was caused by the database query selecting `project:projects(companyId)`, which failed because the database column is named `company_id`. The error in fetching the joined project object was caught and surfaced as a general "Worklog not found" message.
* **Fix**: Modified [worklogs.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/actions/worklogs.ts) in both `deleteWorklog` and `updateWorklog` functions to alias `companyId:company_id` in the select query. This aligns the query with the Supabase schema while maintaining full compatibility with the existing TypeScript codebase checks for `worklog.project.companyId`.

### 14. Inventory Material Stock Deduction Trigger Realignment ✅
* **Problem**: Material consumption from worklogs was not deducting stock from the project inventory. The database trigger function `deduct_material_inventory()` was still targeting the legacy, empty `project_materials` table and column `quantity`.
* **Fix**: Re-executed and updated the trigger function `deduct_material_inventory()` via SQL on Supabase. It now correctly targets `public.materials` and updates `current_stock` when a new material log is recorded inside a worklog.

### 15. Detailed Worklog Feed Rendering ✅
* **Problem**: The Daily Worklog page only displayed a summary card with a photo and a short description, hiding contractor logs, detailed activities, worker counts, and specific material logs.
* **Fix**: Upgraded the `WorklogFeedCard` inside [worklog-list.tsx](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/components/worklog/worklog-list.tsx) with an expandable details section toggled via a "Show Details" / "Hide Details" button. When expanded, it renders:
  - **Labor & Activity**: Individual contractor logs showing their specific task descriptions and a breakdown of worker counts by category (e.g., `Mason: 2`, `MC: 3`, `FC: 1`).
  - **Materials Consumed**: A clear list detailing the quantity and unit of each material consumed (e.g., `10 bags of Cement`).

### 16. Work Done Quantity & Unit Logging ✅
* **Database Updates**: Added `work_done_quantity` (NUMERIC) and `work_done_unit` (TEXT) columns to the `public.worklog_labor_entries` table.
* **Source Updates**:
  - Modified [create-worklog-dialog.tsx](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/components/worklog/create-worklog-dialog.tsx) to add `workDoneQuantity` and `workDoneUnit` to the Zod validation schemas and form default values. Added structured numeric input for quantity and text input for unit inside the `LaborEntryForm` layout.
  - Modified [worklogs.ts](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/app/actions/worklogs.ts) to parse `workDoneQuantity` and `workDoneUnit` in the server-side validator, and write these values to the `worklog_labor_entries` table on create and update inserts.
  - Modified [worklog-list.tsx](file:///c:/Users/Kavin%20Bharathi/OneDrive/Desktop/Constructor-a1f734a624c5e868a1ea2e0e062255c9495e6b1c/src/components/worklog/worklog-list.tsx) to render a visual badge (e.g. `Work Done: 500 sq ft`) next to the contractor description inside the expandable details view.
* **Verification**: Ran `npm run typecheck` which passed successfully. Verified dev server compiles and updates cleanly.
