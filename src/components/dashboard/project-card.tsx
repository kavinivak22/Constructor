'use client';
import Image from 'next/image';
import { PlaceHolderImages } from '@/lib/placeholder-images';
import type { Project } from '@/lib/data';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Calendar } from 'lucide-react';
import { useMemo } from 'react';
import { format } from 'date-fns';
import Link from 'next/link';

interface ProjectCardProps {
  project: Project;
}

export function ProjectCard({ project }: ProjectCardProps) {
  const placeholder = PlaceHolderImages.find(p => p.id === project.id);
  const imageUrl = project.thumbnail_url ?? placeholder?.imageUrl ?? `https://picsum.photos/seed/${project.id}/600/400`;
  const imageHint = placeholder?.imageHint ?? 'construction project';

  const formattedDate = useMemo(() => {
    const sDate = project.start_date || project.startDate;
    if (!sDate) return 'N/A';
    try {
      return format(new Date(sDate), 'MMM d');
    } catch {
      return 'N/A';
    }
  }, [project.start_date, project.startDate]);

  const budgetDisplay = project.budget
    ? project.budget >= 10_000_000
      ? `₹${(project.budget / 10_000_000).toFixed(1)}Cr`
      : project.budget >= 100_000
      ? `₹${(project.budget / 100_000).toFixed(1)}L`
      : `₹${(project.budget / 1_000).toFixed(0)}k`
    : null;

  return (
    <Link href={`/projects/${project.id}`} className="block">
      <Card className="glass-card flex flex-col overflow-hidden h-full">
        <div className="p-4">
          <div className="flex items-start justify-between mb-1 gap-2 min-w-0">
            <div className="min-w-0 flex-1">
              <h3 className="font-bold text-md leading-tight truncate">{project.name}</h3>
              {(project.client_name || project.clientName) && (
                <p className="text-sm text-muted-foreground truncate">{project.client_name || project.clientName}</p>
              )}
            </div>
            <Badge variant="outline" className="capitalize text-xs font-semibold border-green-300 bg-green-50 text-green-700 shrink-0">
              {project.status}
            </Badge>
          </div>
        </div>
        <div className="relative h-40 w-full">
          <Image
            src={imageUrl}
            alt={project.name}
            fill
            className="object-cover"
            data-ai-hint={imageHint}
          />
        </div>
        <CardContent className="flex-grow p-4 space-y-3 flex flex-col">
          <div className="space-y-1 flex-grow">
            <div className="flex justify-between items-center text-sm font-medium text-muted-foreground">
              <p>Progress</p>
              <p className="font-semibold text-foreground">{project.progress ?? 0}%</p>
            </div>
            <Progress value={project.progress ?? 0} aria-label={`${project.progress ?? 0}% complete`} className="h-2" />
          </div>
          <div className='flex items-center justify-between text-sm text-muted-foreground pt-2'>
            <div className='flex items-center gap-2'>
              <Calendar className="h-4 w-4" />
              <span>{formattedDate}</span>
            </div>
            {budgetDisplay && (
              <div className='flex items-center gap-1'>
                <span className='font-semibold'>{budgetDisplay}</span>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
