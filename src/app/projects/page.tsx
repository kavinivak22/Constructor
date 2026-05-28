'use client';

import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import { Plus, Search, Filter } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import Link from 'next/link';
import { useSupabase } from '@/supabase/provider';
import { useEffect, useMemo, useState } from 'react';
import { ProjectCard } from '@/components/dashboard/project-card';
import { Card } from '@/components/ui/card';

export default function ProjectsPage() {
  const { supabase, user } = useSupabase();
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [sortOrder, setSortOrder] = useState('newest');
  const [projects, setProjects] = useState<Project[]>([]);
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
      setIsLoading(true);
      try {
        // 1. Fetch user's project IDs from public.project_members table
        const { data: membersData, error: membersError } = await supabase
          .from('project_members')
          .select('project_id')
          .eq('user_id', user.id);

        if (membersError) throw membersError;

        const projectIds = membersData?.map(m => m.project_id) || [];

        if (projectIds.length === 0) {
          setProjects([]);
          return;
        }

        // 2. Fetch projects using the IDs
        const { data, error } = await supabase
          .from('projects')
          .select('*')
          .in('id', projectIds);

        if (error) throw error;
        setProjects(data || []);
      } catch (error) {
        console.error('Error fetching projects:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchProjects();
  }, [supabase, user]);

  const filteredAndSortedProjects = useMemo(() => {
    if (!projects) return [];

    return projects
      .filter(project =>
        project.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
      .filter(project =>
        statusFilter === 'all' ? true : project.status === statusFilter
      )
      .sort((a, b) => {
        // Use snake_case column from DB with camelCase fallback
        const dateA = new Date((a.start_date || a.startDate) as any);
        const dateB = new Date((b.start_date || b.startDate) as any);
        if (sortOrder === 'newest') {
          return dateB.getTime() - dateA.getTime();
        } else {
          return dateA.getTime() - dateB.getTime();
        }
      });
  }, [projects, searchQuery, statusFilter, sortOrder]);

  return (
    <div className="flex flex-col h-full">
      <header className="flex flex-row items-center justify-between gap-4 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-10">
        <h1 className="text-2xl font-bold tracking-tight font-headline">
          Projects
        </h1>
        {isAdminOrManager && (
          <Link href="/projects/create">
            <Button>
              <Plus className="mr-2" />
              New Project
            </Button>
          </Link>
        )}
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-transparent">
        <div className="mb-6 flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              type="search"
              placeholder="Search projects..."
              className="pl-10 w-full"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Select onValueChange={setStatusFilter} value={statusFilter}>
            <SelectTrigger className="w-auto min-w-[3rem] sm:w-[180px]">
              <div className="flex items-center gap-2">
                <Filter className="h-4 w-4" />
                <span className="hidden sm:inline">
                  <SelectValue placeholder="All Status" />
                </span>
              </div>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Status</SelectItem>
              <SelectItem value="active">Active</SelectItem>
              <SelectItem value="on-hold">On Hold</SelectItem>
              <SelectItem value="completed">Completed</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {isLoading && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(8)].map((_, i) => <Skeleton key={i} className="h-[22rem]" />)}
          </div>
        )}

        {!isLoading && filteredAndSortedProjects.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filteredAndSortedProjects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        )}

        {!isLoading && filteredAndSortedProjects.length === 0 && (
          <Card className="glass-card flex flex-col items-center justify-center h-64 text-center p-6">
            <h3 className="text-xl font-bold font-headline">No Projects Found</h3>
            <p className="max-w-sm mt-2 text-muted-foreground">
              No projects match your current filters. Try adjusting your search or filter criteria.
            </p>
          </Card>
        )}
      </main>
    </div>
  );
}
