'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/react-query';
import { getWorklogs, getRecentWorklogs } from '@/app/actions/worklogs';

export function useProjectWorklogs(projectId: string | undefined) {
    return useQuery({
        queryKey: queryKeys.projectWorklogs(projectId || ''),
        queryFn: async () => {
            if (!projectId) return [];

            const result = await getWorklogs(projectId);

            if (!result.success) {
                throw new Error(result.error || 'Failed to fetch worklogs');
            }

            return result.data || [];
        },
        enabled: !!projectId,
    });
}

export function useRecentWorklogs(limit: number = 5) {
    return useQuery({
        queryKey: ['recent-worklogs', limit],
        queryFn: async () => {
            const result = await getRecentWorklogs(limit);

            if (!result.success) {
                throw new Error(result.error || 'Failed to fetch recent worklogs');
            }

            return result.data || [];
        },
    });
}
