const fs = require('fs');
const path = require('path');
const { createClient } = require('@supabase/supabase-js');

async function migrate() {
    console.log('Starting migration...');

    // 1. Read .env file
    const envPath = path.resolve(__dirname, '../.env');
    if (!fs.existsSync(envPath)) {
        console.error('Error: .env file not found at', envPath);
        process.exit(1);
    }

    const envContent = fs.readFileSync(envPath, 'utf8');
    const envVars = {};
    envContent.split('\n').forEach(line => {
        const match = line.match(/^([^=]+)=(.*)$/);
        if (match) {
            const key = match[1].trim();
            let value = match[2].trim();
            // Remove quotes if present
            if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.slice(1, -1);
            }
            envVars[key] = value;
        }
    });

    console.log('Available Env Vars:', Object.keys(envVars));

    const supabaseUrl = envVars['NEXT_PUBLIC_SUPABASE_URL'];
    let supabaseKey = envVars['SUPABASE_SERVICE_ROLE_KEY'] || envVars['SERVICE_ROLE_KEY'];

    if (!supabaseKey) {
        console.log('Service role key not found, falling back to anon key (might fail due to RLS).');
        supabaseKey = envVars['NEXT_PUBLIC_SUPABASE_ANON_KEY'];
    } else {
        console.log('Using Service Role Key for migration.');
    }

    if (!supabaseUrl || !supabaseKey) {
        console.error('Error: Supabase credentials not found in .env');
        process.exit(1);
    }

    console.log('Supabase URL found:', supabaseUrl);

    const supabase = createClient(supabaseUrl, supabaseKey, {
        auth: {
            autoRefreshToken: false,
            persistSession: false
        }
    });

    // 2. Fetch all projects with their userIds
    const { data: projects, error: projectsError } = await supabase
        .from('projects')
        .select('id, userIds');

    if (projectsError) {
        console.error('Error fetching projects:', projectsError.message);
        process.exit(1);
    }

    if (!projects || projects.length === 0) {
        console.log('No projects found to migrate (or RLS blocked access).');
        return;
    }

    console.log(`Found ${projects.length} projects. Processing...`);

    const updates = {};

    // 3. Aggregate project IDs for each user
    projects.forEach((project) => {
        const userIds = project.userIds;
        if (Array.isArray(userIds)) {
            userIds.forEach((userId) => {
                if (!updates[userId]) {
                    updates[userId] = new Set();
                }
                updates[userId].add(project.id);
            });
        }
    });

    // 4. Update each user
    console.log(`Found ${Object.keys(updates).length} users to update.`);

    for (const userId of Object.keys(updates)) {
        const newProjectIds = Array.from(updates[userId]);

        // Fetch current user to merge
        const { data: user, error: fetchError } = await supabase
            .from('users')
            .select('projectIds')
            .eq('id', userId)
            .single();

        if (fetchError) {
            console.error(`Error fetching user ${userId}:`, fetchError.message);
            continue;
        }

        const currentProjectIds = user.projectIds || [];
        const mergedProjectIds = Array.from(new Set([...currentProjectIds, ...newProjectIds]));

        const { error: updateError } = await supabase
            .from('users')
            .update({ projectIds: mergedProjectIds })
            .eq('id', userId);

        if (updateError) {
            console.error(`Error updating user ${userId}:`, updateError.message);
        } else {
            console.log(`Updated user ${userId}: Added ${newProjectIds.length} projects. Total: ${mergedProjectIds.length}`);
        }
    }

    console.log('Migration completed successfully.');
}

migrate();
