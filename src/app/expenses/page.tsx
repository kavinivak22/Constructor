'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Project } from '@/lib/data';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Loader2, Plus, Receipt } from 'lucide-react';
import Link from 'next/link';

export default function ExpensesSelectionPage() {
    const { supabase, user } = useSupabase();
    const router = useRouter();
    const [projects, setProjects] = useState<Project[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchProjects = async () => {
            if (!user) return;
            try {
                const { data: userData, error: userError } = await supabase
                    .from('users')
                    .select('projectIds')
                    .eq('id', user.id)
                    .single();

                if (userError) throw userError;

                const projectIds = userData?.projectIds || [];

                if (projectIds.length === 0) {
                    setProjects([]);
                    setIsLoading(false);
                    return;
                }

                const { data, error } = await supabase
                    .from('projects')
                    .select('*')
                    .in('id', projectIds);

                if (error) throw error;

                const fetchedProjects = data || [];
                setProjects(fetchedProjects);

                // If only one project, redirect immediately
                if (fetchedProjects.length === 1) {
                    router.push(`/projects/${fetchedProjects[0].id}/expenses`);
                }
            } catch (error) {
                console.error('Error fetching projects:', error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchProjects();
    }, [supabase, user, router]);

    if (isLoading) {
        return (
            <div className="flex h-screen items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="container max-w-4xl py-10">
            <h1 className="text-3xl font-bold mb-6 font-headline">Select Project</h1>
            <p className="text-muted-foreground mb-8">Choose a project to manage expenses.</p>

            {projects.length === 0 ? (
                <Card className="text-center p-8">
                    <CardContent className="flex flex-col items-center gap-4">
                        <Receipt className="h-12 w-12 text-muted-foreground" />
                        <h3 className="text-lg font-semibold">No Projects Found</h3>
                        <p className="text-muted-foreground">Create a project to start tracking expenses.</p>
                        <Link href="/projects/create">
                            <Button>
                                <Plus className="mr-2 h-4 w-4" />
                                Create Project
                            </Button>
                        </Link>
                    </CardContent>
                </Card>
            ) : (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {projects.map((project) => (
                        <Link key={project.id} href={`/projects/${project.id}/expenses`}>
                            <Card className="hover:bg-accent/50 transition-colors cursor-pointer h-full">
                                <CardHeader>
                                    <CardTitle>{project.name}</CardTitle>
                                </CardHeader>
                                <CardContent>
                                    <p className="text-sm text-muted-foreground">{project.location}</p>
                                </CardContent>
                            </Card>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
}
