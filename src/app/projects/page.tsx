'use client';

import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import { Plus, Search } from 'lucide-react';
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

  useEffect(() => {
    const fetchProjects = async () => {
      if (!user) return;
      setIsLoading(true);
      try {
        // 1. Fetch user's projectIds from public.users table
        const { data: userData, error: userError } = await supabase
          .from('users')
          .select('projectIds')
          .eq('id', user.id)
          .single();

        if (userError) throw userError;

        const projectIds = userData?.projectIds || [];

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
        const dateA = a.startDate instanceof Date ? a.startDate : new Date(a.startDate as any);
        const dateB = b.startDate instanceof Date ? b.startDate : new Date(b.startDate as any);
        if (sortOrder === 'newest') {
          return dateB.getTime() - dateA.getTime();
        } else {
          return dateA.getTime() - dateB.getTime();
        }
      });
  }, [projects, searchQuery, statusFilter, sortOrder]);

  return (
    <div className="flex flex-col h-full">
      <header className="flex flex-row items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <h1 className="text-2xl font-bold tracking-tight font-headline">
          Projects
        </h1>
        <Link href="/projects/create">
          <Button>
            <Plus className="mr-2" />
            New Project
          </Button>
        </Link>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-secondary">
        <div className="mb-6 space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              type="search"
              placeholder="Search projects..."
              className="pl-10"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="flex flex-col sm:flex-row gap-4">
            <Select onValueChange={setStatusFilter} value={statusFilter}>
              <SelectTrigger>
                <SelectValue placeholder="All Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="planning">Planning</SelectItem>
                <SelectItem value="in progress">In Progress</SelectItem>
                <SelectItem value="completed">Completed</SelectItem>
              </SelectContent>
            </Select>
            <Select onValueChange={setSortOrder} value={sortOrder}>
              <SelectTrigger>
                <SelectValue placeholder="Sort by" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="newest">Newest First</SelectItem>
                <SelectItem value="oldest">Oldest First</SelectItem>
              </SelectContent>
            </Select>
          </div>
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
          <Card className="flex flex-col items-center justify-center h-64 text-center p-6 bg-card">
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
