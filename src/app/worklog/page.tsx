'use client';

import { useSearchParams } from 'next/navigation';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useMemo, useState, useEffect } from 'react';
import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { PlusCircle } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { useSupabase } from '@/supabase/provider';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';
import { WorklogList } from '@/components/worklog/worklog-list';

export default function WorklogPage() {
  const { supabase, user } = useSupabase();
  const searchParams = useSearchParams();
  const urlProjectId = searchParams.get('projectId');
  const urlWorklogId = searchParams.get('worklogId');

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState(true);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    const fetchProjects = async () => {
      if (!user) {
        setIsLoadingProjects(false);
        setProjects([]);
        return;
      }

      setIsLoadingProjects(true);
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
          setIsLoadingProjects(false);
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
        setProjects([]);
      } finally {
        setIsLoadingProjects(false);
      }
    };

    fetchProjects();
  }, [supabase, user]);

  const handleProjectChange = (projectId: string) => {
    setSelectedProjectId(projectId);
  };

  const selectedProject = useMemo(() => {
    return projects?.find(p => p.id === selectedProjectId) ?? null;
  }, [projects, selectedProjectId]);

  // Set selected project based on URL or default to "all"
  useEffect(() => {
    if (projects && projects.length > 0) {
      if (urlProjectId) {
        // Verify the user has access to this project (it should be in the fetched projects list)
        const projectExists = projects.find(p => p.id === urlProjectId);
        if (projectExists) {
          setSelectedProjectId(urlProjectId);
          return;
        }
      }

      if (!selectedProjectId) {
        setSelectedProjectId("all");
      }
    }
  }, [projects, selectedProjectId, urlProjectId]);

  const handleWorklogCreated = () => {
    setRefreshTrigger(prev => prev + 1);
  };

  return (
    <div className="flex flex-col h-full bg-transparent">
      <header className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 border-b border-white/10 dark:border-white/5 md:px-6 shrink-0 glass sticky top-0 z-40">
        <div className="flex items-center gap-2">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline text-foreground">
            Daily Worklog
          </h1>
        </div>
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4 w-full md:w-auto">
          {isLoadingProjects ? (
            <Skeleton className="h-10 w-full md:w-48 bg-white/10 dark:bg-black/20" />
          ) : (
            projects && projects.length > 0 && (
              <Select onValueChange={handleProjectChange} value={selectedProjectId ?? undefined}>
                <SelectTrigger className="w-full md:w-[180px] glass border-white/10 dark:border-white/5">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent className="glass border-white/10 dark:border-white/5">
                  <SelectItem value="all" className="focus:bg-white/10 dark:focus:bg-white/5">
                    All Projects
                  </SelectItem>
                  {projects.map(project => (
                    <SelectItem key={project.id} value={project.id} className="focus:bg-white/10 dark:focus:bg-white/5">
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )
          )}
          {selectedProjectId && (
            <CreateWorklogDialog
              projectId={selectedProjectId === 'all' ? undefined : selectedProjectId}
              onSuccess={handleWorklogCreated}
            />
          )}
        </div>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-transparent">
        {!selectedProjectId && !isLoadingProjects && (
          <div className="flex flex-col items-center justify-center h-[60vh] text-center rounded-2xl border border-white/10 dark:border-white/5 glass-card p-8">
            <h2 className="text-2xl font-bold font-headline text-foreground">No Project Selected</h2>
            <p className="max-w-sm mt-2 text-muted-foreground">
              {projects && projects.length > 0 ? 'Please select a project to view its worklogs.' : 'Create a project to get started.'}
            </p>
            {(!projects || projects.length === 0) && (
              <Link href="/projects/create" className='mt-4'>
                <Button className="glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5">
                  <PlusCircle className="mr-2 h-4 w-4" />
                  Create Project
                </Button>
              </Link>
            )}
          </div>
        )}
        {selectedProjectId && (
          <WorklogList
            projectId={selectedProjectId}
            refreshTrigger={refreshTrigger}
            highlightWorklogId={urlWorklogId}
          />
        )}
      </main>
    </div>
  );
}
