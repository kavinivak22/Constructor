'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Project } from '@/lib/data';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  Loader2,
  Plus,
  Receipt,
  Building2,
  MapPin,
  ArrowRight,
  Search
} from 'lucide-react';
import Link from 'next/link';

export default function ExpensesSelectionPage() {
    const { supabase, user } = useSupabase();
    const router = useRouter();
    const [projects, setProjects] = useState<Project[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string } | null>(null);

    useEffect(() => {
        const fetchProfile = async () => {
            if (!user) return;
            const { data } = await supabase
                .from('users')
                .select('id, role')
                .eq('id', user.id)
                .single();
            if (data) {
                setCurrentUserProfile(data);
            }
        };
        fetchProfile();
    }, [user, supabase]);

    const isAdminOrManager = currentUserProfile?.role === 'admin' || currentUserProfile?.role === 'manager';

    useEffect(() => {
        const fetchProjects = async () => {
            if (!user) return;
            try {
                const { data: membersData, error: membersError } = await supabase
                    .from('project_members')
                    .select('project_id')
                    .eq('user_id', user.id);

                if (membersError) throw membersError;

                const projectIds = membersData?.map(m => m.project_id) || [];

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

    const filteredProjects = projects.filter(p =>
        p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (p.location && p.location.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    if (isLoading) {
        return (
            <div className="flex h-screen items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-transparent p-4 sm:p-6 md:p-8 max-w-5xl mx-auto space-y-6">
            {/* Header section */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl sm:text-3xl font-bold tracking-tight font-headline flex items-center gap-2.5">
                        <Receipt className="h-7 w-7 text-primary shrink-0" />
                        Select Project
                    </h1>
                    <p className="text-xs sm:text-sm text-muted-foreground mt-1">
                        Choose a project to manage site expenses, receipts & payouts.
                    </p>
                </div>

                {isAdminOrManager && (
                    <Link href="/projects/create">
                        <Button size="sm" className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold shadow-md rounded-xl">
                            <Plus className="mr-1.5 h-4 w-4" /> Create Project
                        </Button>
                    </Link>
                )}
            </div>

            {/* Search filter bar */}
            {projects.length > 0 && (
                <div className="relative">
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="Search project by name or location..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-10 h-10 bg-background/50 border-white/10 glass-card rounded-xl text-xs sm:text-sm"
                    />
                </div>
            )}

            {/* Content area */}
            {projects.length === 0 ? (
                <Card className="glass-card text-center p-8 sm:p-12 border-white/10 rounded-2xl">
                    <CardContent className="flex flex-col items-center gap-4">
                        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                            <Receipt className="h-8 w-8" />
                        </div>
                        <div>
                            <h3 className="text-lg font-bold text-foreground">No Projects Found</h3>
                            <p className="text-xs sm:text-sm text-muted-foreground mt-1">
                                Create a project to start tracking expenses and financial logs.
                            </p>
                        </div>
                        {isAdminOrManager && (
                            <Link href="/projects/create">
                                <Button className="mt-2">
                                    <Plus className="mr-2 h-4 w-4" />
                                    Create Project
                                </Button>
                            </Link>
                        )}
                    </CardContent>
                </Card>
            ) : filteredProjects.length === 0 ? (
                <div className="text-center p-8 border-2 border-dashed border-white/10 rounded-2xl">
                    <p className="text-sm font-semibold text-muted-foreground">No projects match "{searchQuery}"</p>
                </div>
            ) : (
                <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                    {filteredProjects.map((project) => (
                        <Link key={project.id} href={`/projects/${project.id}/expenses`}>
                            <Card className="glass-card p-4 sm:p-5 rounded-2xl border border-white/10 hover:border-primary/50 transition-all hover:shadow-lg cursor-pointer group flex flex-col justify-between h-full">
                                <div>
                                    <div className="flex items-center justify-between gap-2">
                                        <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary border border-primary/20 group-hover:scale-105 transition-transform shrink-0">
                                            <Building2 className="h-5 w-5" />
                                        </div>
                                        <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-400 border-emerald-500/30 font-semibold px-2 py-0.5">
                                            Active Site
                                        </Badge>
                                    </div>

                                    <h3 className="font-bold text-base sm:text-lg text-foreground group-hover:text-primary transition-colors font-headline mt-3.5 line-clamp-1">
                                        {project.name}
                                    </h3>
                                    
                                    {project.location && (
                                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground mt-1">
                                            <MapPin className="h-3.5 w-3.5 text-primary shrink-0" />
                                            <span className="truncate">{project.location}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="flex items-center justify-between text-xs font-semibold text-primary pt-3 mt-4 border-t border-white/5 group-hover:text-primary">
                                    <span>Manage Expenses</span>
                                    <ArrowRight className="h-4 w-4 transform group-hover:translate-x-1 transition-transform" />
                                </div>
                            </Card>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
}
