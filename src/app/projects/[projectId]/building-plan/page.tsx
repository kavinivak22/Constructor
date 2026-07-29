'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Building2 } from 'lucide-react';
import { AIProcessProgressWidget } from '@/components/projects/ai-process-progress-widget';
import { useProject } from '@/hooks/queries';
import { Skeleton } from '@/components/ui/skeleton';

export default function BuildingPlanPage() {
  const { projectId } = useParams();
  const projectIdString = (Array.isArray(projectId) ? projectId[0] : projectId) || '';
  const router = useRouter();
  const { data: project, isLoading: isLoadingProject } = useProject(projectIdString);

  return (
    <div className="flex flex-col min-h-screen bg-transparent">
      {/* Top Header */}
      <header className="flex items-center gap-2 sm:gap-4 px-3 sm:px-6 py-2 shrink-0 glass sticky top-0 z-10 border-b border-white/10 h-11 sm:h-14">
        <Button variant="ghost" size="icon" className="h-7 w-7 sm:h-8 sm:w-8 shrink-0" onClick={() => router.back()}>
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1 min-w-0">
          <h1 className="text-xs sm:text-lg font-bold tracking-tight font-headline truncate leading-none flex items-center gap-2">
            Building Construction Plan & Quality Checks
          </h1>
          <p className="text-[10px] sm:text-xs text-muted-foreground truncate leading-none mt-0.5">
            {project?.name || 'Project Site'}
          </p>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 p-4 sm:p-6 md:p-8 max-w-5xl mx-auto w-full space-y-6">
        {isLoadingProject ? (
          <Skeleton className="h-64 w-full rounded-2xl" />
        ) : (
          <AIProcessProgressWidget
            projectId={projectIdString}
            projectName={project?.name || 'Project'}
          />
        )}
      </main>
    </div>
  );
}
