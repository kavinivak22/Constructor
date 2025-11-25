'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState } from 'react';

// Query Client configuration with optimal defaults for the app
const queryClientConfig = {
    defaultOptions: {
        queries: {
            // Cache data for 5 minutes by default
            staleTime: 5 * 60 * 1000,
            // Keep unused data in cache for 10 minutes
            gcTime: 10 * 60 * 1000,
            // Refetch on window focus for fresh data
            refetchOnWindowFocus: true,
            // Retry failed requests 3 times with exponential backoff
            retry: 3,
            retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 30000),
        },
        mutations: {
            // Retry mutations once on failure
            retry: 1,
        },
    },
};

// Provider component that wraps the app
export function ReactQueryProvider({ children }: { children: React.ReactNode }) {
    // Create a new QueryClient instance per request to avoid sharing state
    const [queryClient] = useState(() => new QueryClient(queryClientConfig));

    return (
        <QueryClientProvider client={queryClient}>
            {children}
            {process.env.NODE_ENV === 'development' && (
                <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" />
            )}
        </QueryClientProvider>
    );
}

// Export a function to create query keys consistently
export const queryKeys = {
    // Projects
    projects: ['projects'] as const,
    project: (id: string) => ['projects', id] as const,
    projectMembers: (id: string) => ['projects', id, 'members'] as const,
    projectExpenses: (id: string) => ['projects', id, 'expenses'] as const,
    projectMaterials: (id: string) => ['projects', id, 'materials'] as const,

    // Users/Employees
    users: ['users'] as const,
    user: (id: string) => ['users', id] as const,
    userProfile: ['userProfile'] as const,
    employees: ['employees'] as const,

    // Invites
    invites: ['invites'] as const,
    invite: (id: string) => ['invites', id] as const,

    // Expenses
    expenses: ['expenses'] as const,
    expense: (id: string) => ['expenses', id] as const,

    // Notifications
    notifications: ['notifications'] as const,
    unreadNotifications: ['notifications', 'unread'] as const,

    // Materials
    materials: ['materials'] as const,
    material: (id: string) => ['materials', id] as const,

    // Purchase Orders
    purchaseOrders: ['purchaseOrders'] as const,
    purchaseOrder: (id: string) => ['purchaseOrders', id] as const,
};
