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
    <Link href={`/projects/${project.id}`} className="block group">
      <Card className="glass-card flex flex-col overflow-hidden h-full transition-all duration-300 group-hover:shadow-lg group-hover:-translate-y-0.5 border border-white/10 dark:border-white/5 rounded-2xl">
        <div className="p-3.5 sm:p-4">
          <div className="flex items-start justify-between mb-1 gap-2 min-w-0">
            <div className="min-w-0 flex-1">
              <h3 className="font-bold text-sm sm:text-base leading-tight truncate text-foreground group-hover:text-primary transition-colors">{project.name}</h3>
              {(project.client_name || project.clientName) && (
                <p className="text-xs sm:text-sm text-muted-foreground truncate">{project.client_name || project.clientName}</p>
              )}
            </div>
            <Badge variant="outline" className="capitalize text-[10px] sm:text-xs font-semibold border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 shrink-0 rounded-full px-2 py-0.5">
              {project.status}
            </Badge>
          </div>
        </div>
        <div className="relative h-36 sm:h-44 w-full overflow-hidden bg-muted">
          <Image
            src={imageUrl}
            alt={project.name}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-105"
            data-ai-hint={imageHint}
          />
        </div>
        <CardContent className="flex-grow p-3.5 sm:p-4 space-y-3 flex flex-col justify-between">
          <div className="space-y-1.5 flex-grow">
            <div className="flex justify-between items-center text-xs sm:text-sm font-medium text-muted-foreground">
              <span>Progress</span>
              <span className="font-semibold text-foreground">{project.progress ?? 0}%</span>
            </div>
            <Progress value={project.progress ?? 0} aria-label={`${project.progress ?? 0}% complete`} className="h-1.5 sm:h-2" />
          </div>
          <div className='flex items-center justify-between text-xs sm:text-sm text-muted-foreground pt-1.5 border-t border-border/40'>
            <div className='flex items-center gap-1.5'>
              <Calendar className="h-3.5 w-3.5 sm:h-4 sm:w-4 text-primary/80" />
              <span>{formattedDate}</span>
            </div>
            {budgetDisplay && (
              <div className='flex items-center gap-1'>
                <span className='font-bold text-foreground'>{budgetDisplay}</span>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
