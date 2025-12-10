
'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { createClientComponentClient } from '@supabase/auth-helpers-nextjs';
import { useRouter } from 'next/navigation';

import type { SupabaseClient, User } from '@supabase/auth-helpers-nextjs';


type SupabaseContextType = {
    supabase: SupabaseClient;
    user: User | null;
    isLoading: boolean;
};

const SupabaseContext = createContext<SupabaseContextType | undefined>(undefined);

export const SupabaseProvider = ({ children }: { children: React.ReactNode }) => {
    const [supabase] = useState(() => createClientComponentClient());
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const router = useRouter();

    useEffect(() => {
        const {
            data: { subscription },
        } = supabase.auth.onAuthStateChange((event, session) => {
            setIsLoading(true);
            if (session) {
                setUser(session.user);
            } else {
                setUser(null);
            }
            setIsLoading(false);
        });

        // The auth listener should be cleaned up on unmount
        return () => {
            subscription.unsubscribe();
        };
    }, [router, supabase]);

    return (
        <SupabaseContext.Provider value={{ supabase, user, isLoading }}>
            {children}
        </SupabaseContext.Provider>
    );
};

export const useSupabase = () => {
    const context = useContext(SupabaseContext);
    if (context === undefined) {
        throw new Error('useSupabase must be used within a SupabaseProvider');
    }
    return context;
};
