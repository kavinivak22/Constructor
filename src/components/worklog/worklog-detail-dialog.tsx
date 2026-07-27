'use client';

import React, { useRef } from 'react';
import { format } from 'date-fns';
import {
  Dialog,
  DialogContent,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Users, Package, Image as ImageIcon, Clock, X } from 'lucide-react';
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import Autoplay from "embla-carousel-autoplay";
import Image from 'next/image';
import { cn } from '@/lib/utils';

interface WorklogDetailDialogProps {
  worklog: any | null;
  isOpen: boolean;
  onClose: () => void;
}

export function WorklogDetailDialog({ worklog, isOpen, onClose }: WorklogDetailDialogProps) {
  const plugin = useRef(
    Autoplay({ delay: 3000, stopOnInteraction: true })
  );

  if (!worklog) return null;

  // Calculate Totals matching WorklogFeedCard
  const laborEntries = worklog.labor || [];
  const materialEntries = worklog.materials || [];
  const photoEntries = worklog.photos || [];

  const totalWorkers = laborEntries.reduce((acc: number, entry: any) => {
    const workers = entry.workers || [];
    return acc + workers.reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0);
  }, 0);

  const totalMaterials = materialEntries.length;
  const totalPhotos = photoEntries.length;

  // Title & Description matching WorklogFeedCard
  let title = worklog.title || 'Daily Log';
  let description = '';

  if (!worklog.title || worklog.title === 'Daily Log') {
    const categories = Array.from(new Set(laborEntries.map((l: any) => l.category).filter(Boolean))) as string[];
    if (categories.length > 0) {
      title = categories.slice(0, 2).join(' & ') + (categories.length > 2 ? '...' : '') + ' Work';
    } else if (photoEntries[0]?.caption) {
      title = photoEntries[0].caption;
    }
  }

  const rawDescription = laborEntries.map((l: any) => l.work_description).filter(Boolean).join('. ') || worklog.notes || '';
  if (rawDescription) {
    description = rawDescription;
  } else {
    description = "No detailed description provided for this day.";
  }

  const dateObj = worklog.date ? new Date(worklog.date) : new Date();
  const createdObj = worklog.created_at || worklog.date ? new Date(worklog.created_at || worklog.date) : new Date();

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent className="max-w-xl p-0 overflow-hidden glass-card border-white/10 dark:border-white/5 rounded-3xl backdrop-blur-3xl shadow-2xl">
        <div className="relative flex flex-col max-h-[85vh] overflow-y-auto scrollbar-thin">
          {/* Close Button Overlay */}
          <Button
            size="icon"
            variant="ghost"
            onClick={onClose}
            className="absolute top-3 right-3 z-30 h-8 w-8 rounded-full glass border border-white/10 dark:border-white/5 text-foreground hover:bg-white/20"
          >
            <X className="h-4 w-4" />
          </Button>

          {/* Image Section (Exact replica of WorklogFeedCard) */}
          <div className="relative bg-white/5 dark:bg-black/20 overflow-hidden w-full aspect-[4/3] shrink-0">
            {totalPhotos > 1 ? (
              <Carousel
                className="w-full h-full"
                opts={{ loop: true }}
                plugins={[plugin.current]}
                onMouseEnter={plugin.current.stop}
                onMouseLeave={plugin.current.reset}
              >
                <CarouselContent>
                  {photoEntries.map((photo: any, i: number) => (
                    <CarouselItem key={i} className="pl-0">
                      <div className="relative w-full aspect-[4/3]">
                        <Image
                          src={photo.photo_url || photo.url}
                          alt={photo.caption || `Photo ${i + 1}`}
                          fill
                          className="object-cover"
                          unoptimized
                        />
                      </div>
                    </CarouselItem>
                  ))}
                </CarouselContent>
                <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                  <CarouselPrevious className="left-2 bg-black/50 hover:bg-black/70 border-none text-white h-8 w-8" />
                  <CarouselNext className="right-2 bg-black/50 hover:bg-black/70 border-none text-white h-8 w-8" />
                </div>
              </Carousel>
            ) : totalPhotos === 1 ? (
              <Image
                src={photoEntries[0].photo_url || photoEntries[0].url}
                alt="Worklog update"
                fill
                className="object-cover"
                unoptimized
              />
            ) : (
              <div className="flex items-center justify-center h-full text-muted-foreground/30">
                <ImageIcon className="h-12 w-12 text-muted-foreground/45" />
              </div>
            )}

            {/* Date Badge Overlay */}
            <div className="absolute top-3 left-3 z-10 glass border border-white/10 dark:border-white/5 px-3 py-1.5 rounded-xl shadow-sm text-xs font-semibold flex flex-col items-center pointer-events-none text-foreground">
              <span className="text-muted-foreground uppercase text-[10px] leading-tight">{format(dateObj, 'MMM')}</span>
              <span className="text-lg leading-none font-bold text-foreground">{format(dateObj, 'dd')}</span>
            </div>
          </div>

          {/* Content Section (Exact replica of WorklogFeedCard) */}
          <div className="flex flex-col p-6 space-y-4">
            {/* Badges Row */}
            <div className="flex flex-wrap items-center gap-2">
              {(worklog.project?.name || worklog.projects?.name) && (
                <Badge variant="secondary" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-foreground font-semibold bg-white/10">
                  {worklog.project?.name || worklog.projects?.name}
                </Badge>
              )}
              <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-muted-foreground font-normal gap-1">
                <Users className="h-3 w-3 text-muted-foreground" /> {totalWorkers} Workers
              </Badge>
              {totalMaterials > 0 && (
                <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-muted-foreground font-normal gap-1">
                  <Package className="h-3 w-3 text-muted-foreground" /> {totalMaterials} Mats
                </Badge>
              )}
            </div>

            {/* Title & Description */}
            <div>
              <h3 className="font-bold text-xl leading-tight text-foreground mb-2">
                {title}
              </h3>
              <p className="text-sm text-muted-foreground/90 whitespace-pre-line leading-relaxed">
                {description}
              </p>
            </div>

            {/* Labor Section */}
            {laborEntries.length > 0 && (
              <div className="space-y-3 pt-2 border-t border-white/10 dark:border-white/5">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5" /> Labor & Activity
                </h4>
                <div className="space-y-2">
                  {laborEntries.map((entry: any, eIdx: number) => (
                    <div key={eIdx} className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 space-y-2">
                      <div className="flex justify-between items-start gap-2">
                        <span className="font-semibold text-sm text-foreground">{entry.contractor_name || entry.contractorName || 'Contractor Team'}</span>
                        {entry.category && (
                          <Badge variant="secondary" className="text-[10px] px-1.5 py-0 bg-white/10 text-foreground border-none">
                            {entry.category}
                          </Badge>
                        )}
                      </div>
                      {entry.work_description && (
                        <p className="text-xs text-muted-foreground/90">{entry.work_description}</p>
                      )}
                      {entry.work_done_quantity !== null && entry.work_done_quantity !== undefined && (
                        <div className="text-[11px] font-medium text-primary bg-primary/10 border border-primary/20 px-2 py-0.5 rounded-md w-fit flex items-center gap-1">
                          <span className="text-muted-foreground/90">Work Done:</span>
                          <span className="font-bold text-foreground">{entry.work_done_quantity}</span>
                          {(entry.work_done_unit || entry.unit) && <span className="text-foreground">{entry.work_done_unit || entry.unit}</span>}
                        </div>
                      )}
                      {entry.workers && entry.workers.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {entry.workers.map((w: any, wIdx: number) => (
                            <Badge key={wIdx} variant="outline" className="text-[10px] py-0 px-2 glass border-white/5 text-muted-foreground font-normal">
                              {w.worker_type || w.workerType}: <span className="font-bold text-foreground ml-0.5">{w.count}</span>
                            </Badge>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Materials Section */}
            {materialEntries.length > 0 && (
              <div className="space-y-2 pt-2 border-t border-white/10 dark:border-white/5">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                  <Package className="h-3.5 w-3.5" /> Materials Consumed
                </h4>
                <div className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 divide-y divide-white/5">
                  {materialEntries.map((m: any, mIdx: number) => (
                    <div key={mIdx} className="flex justify-between items-center py-1.5 first:pt-0 last:pb-0 text-xs">
                      <span className="text-foreground font-medium">{m.material_name || m.materialName || 'Material'}</span>
                      <span className="text-muted-foreground font-semibold">
                        {m.quantity_consumed || m.quantity} <span className="text-[10px] font-normal">{m.unit}</span>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Footer Posted Timestamp */}
            <div className="pt-4 border-t border-white/10 dark:border-white/5 flex items-center justify-between text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-muted-foreground" /> Posted {format(createdObj, 'h:mm a')}
              </span>
              {(worklog.creator?.display_name || worklog.created_by_user?.display_name) && (
                <span className="text-xs text-muted-foreground/80 font-medium">
                  By {worklog.creator?.display_name || worklog.created_by_user?.display_name}
                </span>
              )}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
