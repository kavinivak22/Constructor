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
import { useTranslation } from '@/lib/i18n/language-context';

export default function WorklogPage() {
  const { supabase, user } = useSupabase();
  const { t } = useTranslation();
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

  // Set selected project based on URL or default to first
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
        setSelectedProjectId(projects[0].id);
      }
    }
  }, [projects, selectedProjectId, urlProjectId]);

  const handleWorklogCreated = () => {
    setRefreshTrigger(prev => prev + 1);
  };

  return (
    <div className="flex flex-col h-full">
      <header className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-40">
        <div className="flex items-center gap-2">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            {t('worklog.title')}
          </h1>
        </div>
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4 w-full md:w-auto">
          {isLoadingProjects ? (
            <Skeleton className="h-10 w-full md:w-48" />
          ) : (
            projects && projects.length > 0 && (
              <Select onValueChange={handleProjectChange} value={selectedProjectId ?? undefined}>
                <SelectTrigger className="w-full md:w-[180px]">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map(project => (
                    <SelectItem key={project.id} value={project.id}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )
          )}
          {selectedProjectId && (
            <CreateWorklogDialog
              projectId={selectedProjectId}
              onSuccess={handleWorklogCreated}
            />
          )}
        </div>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {!selectedProjectId && !isLoadingProjects && (
          <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50">
            <h2 className="text-2xl font-bold font-headline">{t('worklog.no_project_selected')}</h2>
            <p className="max-w-sm mt-2 text-muted-foreground">
              {projects && projects.length > 0 ? t('worklog.select_project_desc') : t('worklog.create_project_desc')}
            </p>
            {(!projects || projects.length === 0) && (
              <Link href="/projects/create" className='mt-4'>
                <Button>
                  <PlusCircle className="mr-2 h-4 w-4" />
                  {t('worklog.create_project')}
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
