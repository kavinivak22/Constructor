'use client';

import React from 'react';
import { format } from 'date-fns';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Calendar, Users, Package, Clock, User, CheckCircle2, FileText, Image as ImageIcon } from 'lucide-react';
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel';
import Image from 'next/image';

interface WorklogDetailDialogProps {
  worklog: any | null;
  isOpen: boolean;
  onClose: () => void;
}

export function WorklogDetailDialog({ worklog, isOpen, onClose }: WorklogDetailDialogProps) {
  if (!worklog) return null;

  // Aggregate descriptions from labor entries
  const mainDescription =
    worklog.labor?.map((l: any) => l.work_description).filter(Boolean).join('\n') ||
    worklog.notes ||
    'No additional notes provided for this worklog.';

  const title = worklog.title || 'Daily Site Worklog';
  const formattedDate = worklog.date ? format(new Date(worklog.date), 'EEEE, MMMM dd, yyyy') : 'N/A';

  const photos = worklog.photos || [];
  const laborEntries = worklog.labor || [];
  const materialEntries = worklog.materials || [];

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto p-4 sm:p-6 glass-card border-primary/20 rounded-2xl">
        <DialogHeader className="space-y-2 pb-3 border-b border-border/40">
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs bg-primary/10 text-primary border-primary/30">
              {worklog.project?.name || worklog.projects?.name || 'Project Log'}
            </Badge>
            <span className="text-xs text-muted-foreground font-medium flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" />
              {formattedDate}
            </span>
          </div>

          <DialogTitle className="text-xl font-bold font-headline text-foreground leading-snug">
            {title}
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground flex items-center gap-2">
            <span>Logged by: <strong className="text-foreground">{worklog.created_by_user?.display_name || worklog.creator?.display_name || 'Site Engineer'}</strong></span>
            <span>•</span>
            <span className="flex items-center gap-1 text-emerald-500 font-semibold">
              <CheckCircle2 className="h-3.5 w-3.5" /> Verified
            </span>
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 pt-3">
          {/* Photo Gallery Carousel */}
          {photos.length > 0 ? (
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <ImageIcon className="h-3.5 w-3.5 text-primary" /> Site Photos ({photos.length})
              </h4>
              <Carousel className="w-full">
                <CarouselContent>
                  {photos.map((photo: any, index: number) => (
                    <CarouselItem key={index} className="basis-full">
                      <Card className="overflow-hidden border-border/40 bg-black/40 relative rounded-xl">
                        <div className="relative aspect-[16/9] w-full">
                          <Image
                            src={photo.photo_url || photo.url}
                            alt={photo.caption || `Site update photo ${index + 1}`}
                            fill
                            className="object-cover"
                          />
                        </div>
                        {photo.caption && (
                          <div className="p-2.5 bg-background/90 backdrop-blur-sm text-xs text-foreground font-medium border-t border-border/40">
                            {photo.caption}
                          </div>
                        )}
                      </Card>
                    </CarouselItem>
                  ))}
                </CarouselContent>
                {photos.length > 1 && (
                  <>
                    <CarouselPrevious className="left-2 bg-background/80 hover:bg-background h-8 w-8" />
                    <CarouselNext className="right-2 bg-background/80 hover:bg-background h-8 w-8" />
                  </>
                )}
              </Carousel>
            </div>
          ) : (
            <div className="p-3 rounded-xl bg-muted/20 border border-border/30 text-xs text-muted-foreground flex items-center gap-2">
              <ImageIcon className="h-4 w-4 opacity-50" /> No site photos attached to this update.
            </div>
          )}

          {/* Detailed Work Description */}
          <div className="space-y-1.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <FileText className="h-3.5 w-3.5 text-primary" /> Work Details & Progress
            </h4>
            <div className="p-3.5 rounded-xl bg-muted/30 border border-border/40 text-xs leading-relaxed text-foreground whitespace-pre-line font-sans">
              {mainDescription}
            </div>
          </div>

          {/* Workforce & Labor Breakdown */}
          {laborEntries.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Users className="h-3.5 w-3.5 text-primary" /> Labor & Workforce Breakdown
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {laborEntries.map((l: any, idx: number) => (
                  <div key={idx} className="p-2.5 rounded-xl bg-muted/40 border border-border/40 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-semibold text-foreground">{l.category || l.worker_role || 'Labor Task'}</p>
                      <p className="text-[11px] text-muted-foreground">{l.contractors?.name || 'Contractor Team'}</p>
                    </div>
                    <Badge variant="secondary" className="font-bold text-xs bg-primary/10 text-primary border-primary/20">
                      {l.worker_count || l.count || 1} Present
                    </Badge>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Materials Consumed Breakdown */}
          {materialEntries.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Package className="h-3.5 w-3.5 text-amber-500" /> Materials Consumed
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {materialEntries.map((m: any, idx: number) => (
                  <div key={idx} className="p-2.5 rounded-xl bg-muted/40 border border-border/40 flex items-center justify-between text-xs">
                    <span className="font-semibold text-foreground">{m.materials?.name || m.material_name || 'Material'}</span>
                    <Badge className="bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30 text-xs font-bold">
                      {m.quantity_used || m.quantity || 0} {m.unit || 'units'}
                    </Badge>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
