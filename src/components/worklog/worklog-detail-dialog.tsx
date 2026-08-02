'use client';

import React, { useState, useRef } from 'react';
import { format } from 'date-fns';
import {
  Dialog,
  DialogContent,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Users, Package, Image as ImageIcon, Clock, X, Maximize2 } from 'lucide-react';
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import Autoplay from "embla-carousel-autoplay";
import Image from 'next/image';
import { FullscreenPhotoViewer } from '@/components/worklog/fullscreen-photo-viewer';

interface WorklogDetailDialogProps {
  worklog: any | null;
  isOpen: boolean;
  onClose: () => void;
}

export function WorklogDetailDialog({ worklog, isOpen, onClose }: WorklogDetailDialogProps) {
  const [isFullscreenViewerOpen, setIsFullscreenViewerOpen] = useState(false);
  const [fullscreenPhotoIndex, setFullscreenPhotoIndex] = useState(0);

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
    <>
      <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
        <DialogContent className="max-w-xl p-0 overflow-hidden bg-background/95 dark:bg-card/95 backdrop-blur-2xl border border-border/60 shadow-2xl rounded-3xl text-foreground">
          <div className="relative flex flex-col max-h-[85vh] overflow-y-auto scrollbar-thin">
            {/* Close Button Overlay */}
            <Button
              size="icon"
              variant="ghost"
              onClick={onClose}
              className="absolute top-3 right-3 z-30 h-8 w-8 rounded-full bg-background/80 dark:bg-card/80 backdrop-blur-md border border-border/50 text-foreground hover:bg-muted"
            >
              <X className="h-4 w-4" />
            </Button>

            {/* Image Section */}
            <div className="relative bg-muted/40 overflow-hidden w-full aspect-[4/3] shrink-0 border-b border-border/40 group">
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
                        <div
                          onClick={() => {
                            setFullscreenPhotoIndex(i);
                            setIsFullscreenViewerOpen(true);
                          }}
                          className="relative w-full aspect-[4/3] cursor-pointer"
                        >
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
                    <CarouselPrevious className="left-2 bg-background/80 hover:bg-background border-border text-foreground h-8 w-8" />
                    <CarouselNext className="right-2 bg-background/80 hover:bg-background border-border text-foreground h-8 w-8" />
                  </div>
                </Carousel>
              ) : totalPhotos === 1 ? (
                <div
                  onClick={() => {
                    setFullscreenPhotoIndex(0);
                    setIsFullscreenViewerOpen(true);
                  }}
                  className="relative w-full h-full cursor-pointer"
                >
                  <Image
                    src={photoEntries[0].photo_url || photoEntries[0].url}
                    alt="Worklog update"
                    fill
                    className="object-cover"
                    unoptimized
                  />
                </div>
              ) : (
                <div className="flex items-center justify-center h-full text-muted-foreground/40 bg-muted/30">
                  <ImageIcon className="h-12 w-12 text-muted-foreground/50" />
                </div>
              )}

              {/* Date Badge Overlay */}
              <div className="absolute top-3 left-3 z-10 bg-background/90 dark:bg-card/90 backdrop-blur-md border border-border/60 px-3 py-1.5 rounded-xl shadow-md text-xs font-semibold flex flex-col items-center pointer-events-none text-foreground">
                <span className="text-muted-foreground uppercase text-[10px] leading-tight font-bold">{format(dateObj, 'MMM')}</span>
                <span className="text-lg leading-none font-bold text-foreground">{format(dateObj, 'dd')}</span>
              </div>
            </div>

          {/* Content Section */}
          <div className="flex flex-col p-6 space-y-4">
            {/* Badges Row */}
            <div className="flex flex-wrap items-center gap-2">
              {(worklog.project?.name || worklog.projects?.name) && (
                <Badge variant="secondary" className="text-[11px] px-2.5 py-0.5 h-auto bg-primary/10 text-primary border-primary/20 font-semibold">
                  {worklog.project?.name || worklog.projects?.name}
                </Badge>
              )}
              <Badge variant="outline" className="text-[11px] px-2.5 py-0.5 h-auto bg-muted/50 border-border text-muted-foreground font-medium gap-1">
                <Users className="h-3 w-3 text-muted-foreground" /> {totalWorkers} Workers
              </Badge>
              {totalMaterials > 0 && (
                <Badge variant="outline" className="text-[11px] px-2.5 py-0.5 h-auto bg-muted/50 border-border text-muted-foreground font-medium gap-1">
                  <Package className="h-3 w-3 text-muted-foreground" /> {totalMaterials} Mats
                </Badge>
              )}
            </div>

            {/* Title & Description */}
            <div>
              <h3 className="font-bold text-xl leading-snug text-foreground mb-2">
                {title}
              </h3>
              <p className="text-sm text-foreground/90 whitespace-pre-line leading-relaxed font-sans">
                {description}
              </p>
            </div>

            {/* Job-Costing Cost of Work Banner */}
            <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-500 flex items-center justify-center font-bold text-xs shrink-0">
                  ₹
                </div>
                <div>
                  <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider leading-none">Estimated Job Cost of Work</p>
                  <p className="text-base font-bold text-emerald-600 dark:text-emerald-400 leading-tight mt-0.5">
                    ₹{((totalWorkers * 750) + (totalMaterials * 1200)).toLocaleString('en-IN')}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2 text-xs font-semibold text-muted-foreground self-end sm:self-auto">
                <span className="bg-background/80 px-2 py-0.5 rounded-md border border-border">Labour: ₹{(totalWorkers * 750).toLocaleString('en-IN')}</span>
                <span>•</span>
                <span className="bg-background/80 px-2 py-0.5 rounded-md border border-border">Materials: ₹{(totalMaterials * 1200).toLocaleString('en-IN')}</span>
              </div>
            </div>

            {/* Labor Section */}
            {laborEntries.length > 0 && (
              <div className="space-y-3 pt-3 border-t border-border/40">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5 text-primary" /> Labor & Activity
                </h4>
                <div className="space-y-2">
                  {laborEntries.map((entry: any, eIdx: number) => (
                    <div key={eIdx} className="p-3.5 rounded-xl bg-muted/40 dark:bg-muted/20 border border-border/50 space-y-2">
                      <div className="flex justify-between items-start gap-2">
                        <span className="font-semibold text-sm text-foreground">{entry.contractor_name || entry.contractorName || 'Contractor Team'}</span>
                        {entry.category && (
                          <Badge variant="secondary" className="text-[10px] px-2 py-0.5 bg-primary/10 text-primary border-none font-semibold">
                            {entry.category}
                          </Badge>
                        )}
                      </div>
                      {entry.work_description && (
                        <p className="text-xs text-foreground/80 leading-relaxed">{entry.work_description}</p>
                      )}
                      {entry.work_done_quantity !== null && entry.work_done_quantity !== undefined && (
                        <div className="text-[11px] font-medium text-primary bg-primary/10 border border-primary/20 px-2 py-0.5 rounded-md w-fit flex items-center gap-1">
                          <span className="text-muted-foreground">Work Done:</span>
                          <span className="font-bold text-foreground">{entry.work_done_quantity}</span>
                          {(entry.work_done_unit || entry.unit) && <span className="text-foreground">{entry.work_done_unit || entry.unit}</span>}
                        </div>
                      )}
                      {entry.workers && entry.workers.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {entry.workers.map((w: any, wIdx: number) => (
                            <Badge key={wIdx} variant="outline" className="text-[10px] py-0.5 px-2 bg-background border-border text-muted-foreground font-medium">
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
              <div className="space-y-2 pt-3 border-t border-border/40">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                  <Package className="h-3.5 w-3.5 text-amber-500" /> Materials Consumed
                </h4>
                <div className="p-3.5 rounded-xl bg-muted/40 dark:bg-muted/20 border border-border/50 divide-y divide-border/30">
                  {materialEntries.map((m: any, mIdx: number) => (
                    <div key={mIdx} className="flex justify-between items-center py-2 first:pt-0 last:pb-0 text-xs">
                      <span className="text-foreground font-semibold">{m.material_name || m.materialName || 'Material'}</span>
                      <span className="text-muted-foreground font-bold">
                        {m.quantity_consumed || m.quantity} <span className="text-[10px] font-normal">{m.unit}</span>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Footer Posted Timestamp */}
            <div className="pt-4 border-t border-border/40 flex items-center justify-between text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-muted-foreground" /> Posted {format(createdObj, 'h:mm a')}
              </span>
              {(worklog.creator?.display_name || worklog.created_by_user?.display_name) && (
                <span className="text-xs text-muted-foreground font-medium">
                  By {worklog.creator?.display_name || worklog.created_by_user?.display_name}
                </span>
              )}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    {/* Fullscreen Photo Lightbox Modal */}
    <FullscreenPhotoViewer
      photos={photoEntries}
      initialIndex={fullscreenPhotoIndex}
      isOpen={isFullscreenViewerOpen}
      onClose={() => setIsFullscreenViewerOpen(false)}
      title={title}
      dateLabel={format(dateObj, 'MMM dd, yyyy')}
    />
    </>
  );
}
