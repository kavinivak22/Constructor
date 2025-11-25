'use client';

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
import { PlusCircle, ShoppingCart } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { PlaceholderPage } from '@/components/placeholder-page';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';

export default function PurchaseOrdersPage() {
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);

  const projects: Project[] = [];
  const isLoadingProjects = false;

  const handleProjectChange = (projectId: string) => {
    setSelectedProjectId(projectId);
  };

  const selectedProject = useMemo(() => {
    return projects?.find(p => p.id === selectedProjectId) ?? null;
  }, [projects, selectedProjectId]);

  useEffect(() => {
    if (projects && projects.length > 0 && !selectedProjectId) {
        setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);


  return (
    <div className="flex flex-col h-full">
      <header className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            Purchase Orders
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
           <Link href="/projects/create" className="w-full md:w-auto">
            <Button className='w-full md:w-auto'>
                <PlusCircle className="mr-2 h-4 w-4" />
                Create Project
            </Button>
        </Link>
        </div>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {!selectedProjectId && !isLoadingProjects && (
             <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50 p-6">
                <h2 className="text-2xl font-bold font-headline">No Project Selected</h2>
                <p className="max-w-sm mt-2 text-muted-foreground">
                    {projects && projects.length > 0 ? 'Please select a project to view its purchase orders.' : 'Create a project to get started.'}
                </p>
                {(!projects || projects.length === 0) && (
                    <Link href="/projects/create" className='mt-4'>
                        <Button>
                            <PlusCircle className="mr-2 h-4 w-4" />
                            Create Project
                        </Button>
                    </Link>
                )}
            </div>
        )}
        {selectedProjectId && (
             <Card>
                <CardHeader>
                    <CardTitle>Purchase Orders for {selectedProject?.name}</CardTitle>
                    <CardDescription>
                        Create, submit, and approve purchase orders with automated notifications. This feature is coming soon.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <PlaceholderPage title="Purchase Orders Coming Soon" description="The purchase order system is under construction." icon={ShoppingCart} noHeader={true} />
                </CardContent>
             </Card>
        )}
      </main>
    </div>
  );
}
