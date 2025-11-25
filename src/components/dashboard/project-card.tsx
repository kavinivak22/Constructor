'use client';
import Image from 'next/image';
import { PlaceHolderImages } from '@/lib/placeholder-images';
import type { Project } from '@/lib/data';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Calendar, Users } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useMemo } from 'react';
import { useCollection, useMemoFirebase } from '@/firebase';
import { collection, query, Timestamp } from 'firebase/firestore';
import { useFirestore } from '@/firebase';
import { type User } from '@/lib/data';
import { format } from 'date-fns';
import Link from 'next/link';

interface ProjectCardProps {
  project: Project;
}

export function ProjectCard({ project }: ProjectCardProps) {
  const placeholder = PlaceHolderImages.find(p => p.id === project.imageId);
  const imageUrl = placeholder?.imageUrl ?? `https://picsum.photos/seed/${project.id}/600/400`;
  const imageHint = placeholder?.imageHint ?? 'construction project';

  const getFormattedDate = () => {
    if (!project.startDate) return 'N/A';
    // Firestore Timestamps need to be converted to JS Dates
    if (project.startDate instanceof Timestamp) {
      return format(project.startDate.toDate(), 'MMM d');
    }
    // Handle string dates (e.g., from mock data or older saves)
    return format(new Date(project.startDate), 'MMM d');
  };


  return (
    <Link href={`/projects/${project.id}`} className="block">
      <Card className="flex flex-col overflow-hidden transition-all duration-300 hover:shadow-lg hover:-translate-y-1 h-full">
        <div className="p-4">
          <div className="flex items-start justify-between mb-1 gap-2 min-w-0">
            <div className="min-w-0 flex-1">
              <h3 className="font-bold text-md leading-tight truncate">{project.name}</h3>
              <p className="text-sm text-muted-foreground truncate">ABC Corp</p>
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
              <span>{getFormattedDate()}</span>
            </div>
            <div className='flex items-center gap-2'>
              <span className='font-semibold'>$</span>
              <span>850k</span>
            </div>
            <div className='flex items-center gap-2'>
              {/* Member count removed to avoid N+1 queries */}
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
