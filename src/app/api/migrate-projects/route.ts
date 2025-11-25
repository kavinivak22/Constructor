import { createRouteHandlerClient } from '@supabase/auth-helpers-nextjs';
import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

export async function GET() {
    const supabase = createRouteHandlerClient({ cookies });

    try {
        // 1. Fetch all projects with their userIds (we need to cast to any because we removed userIds from the type)
        const { data: projects, error: projectsError } = await supabase
            .from('projects')
            .select('id, userIds');

        if (projectsError) {
            return NextResponse.json({ error: projectsError.message }, { status: 500 });
        }

        if (!projects || projects.length === 0) {
            return NextResponse.json({ message: 'No projects found to migrate.' });
        }

        const updates: Record<string, Set<string>> = {};

        // 2. Aggregate project IDs for each user
        projects.forEach((project: any) => {
            const userIds = project.userIds;
            if (Array.isArray(userIds)) {
                userIds.forEach((userId: string) => {
                    if (!updates[userId]) {
                        updates[userId] = new Set();
                    }
                    updates[userId].add(project.id);
                });
            }
        });

        // 3. Update each user
        const results = [];
        for (const userId of Object.keys(updates)) {
            const newProjectIds = Array.from(updates[userId]);

            // Fetch current user to merge (optional, but safer to avoid overwriting if they already have some)
            // Actually, since we are switching sources of truth, we might want to just set it. 
            // But let's merge to be safe in case some manual edits happened.
            const { data: user, error: fetchError } = await supabase
                .from('users')
                .select('projectIds')
                .eq('id', userId)
                .single();

            if (fetchError) {
                results.push({ userId, status: 'failed', error: fetchError.message });
                continue;
            }

            const currentProjectIds = user.projectIds || [];
            // Merge unique IDs
            const mergedProjectIds = Array.from(new Set([...currentProjectIds, ...newProjectIds]));

            const { error: updateError } = await supabase
                .from('users')
                .update({ projectIds: mergedProjectIds })
                .eq('id', userId);

            if (updateError) {
                results.push({ userId, status: 'failed', error: updateError.message });
            } else {
                results.push({ userId, status: 'success', added: newProjectIds.length, total: mergedProjectIds.length });
            }
        }

        return NextResponse.json({
            message: 'Migration completed',
            processedProjects: projects.length,
            userUpdates: results
        });

    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
