'use client';

import React, { useState, useRef, useEffect, useMemo } from 'react';
import { format } from 'date-fns';
import {
  Dialog,
  DialogContent,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Users, Package, Image as ImageIcon, Clock, X, Loader2, CheckCircle2 } from 'lucide-react';
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
import { getSalaryProfiles } from '@/app/actions/financials';
import { getProjectMaterials } from '@/app/actions/materials';
import { matchesWorkerType } from '@/lib/worklog-helpers';

interface WorklogDetailDialogProps {
  worklog: any | null;
  isOpen: boolean;
  onClose: () => void;
}

export function WorklogDetailDialog({ worklog, isOpen, onClose }: WorklogDetailDialogProps) {
  const [isFullscreenViewerOpen, setIsFullscreenViewerOpen] = useState(false);
  const [fullscreenPhotoIndex, setFullscreenPhotoIndex] = useState(0);

  const [salaryProfiles, setSalaryProfiles] = useState<any[]>([]);
  const [projectMaterials, setProjectMaterials] = useState<any[]>([]);
  const [isLoadingCost, setIsLoadingCost] = useState(false);

  const plugin = useRef(
    Autoplay({ delay: 3000, stopOnInteraction: true })
  );

  // Load salary / wage profiles and project materials when dialog opens
  useEffect(() => {
    if (!isOpen || !worklog) return;

    let isMounted = true;
    const loadRates = async () => {
      setIsLoadingCost(true);
      try {
        const pId = worklog.project_id || worklog.projectId || worklog.project?.id;
        const [profilesRes, materialsRes] = await Promise.all([
          getSalaryProfiles(),
          pId ? getProjectMaterials(pId) : Promise.resolve({ success: false, data: [] })
        ]);

        if (isMounted) {
          if (Array.isArray(profilesRes)) {
            setSalaryProfiles(profilesRes);
          }
          if (materialsRes?.success && Array.isArray(materialsRes.data)) {
            setProjectMaterials(materialsRes.data);
          }
        }
      } catch (err) {
        console.error('Error fetching rates for worklog cost calculation:', err);
      } finally {
        if (isMounted) setIsLoadingCost(false);
      }
    };

    loadRates();

    return () => {
      isMounted = false;
    };
  }, [isOpen, worklog]);

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

  // Helper to match contractor wage profile
  const findContractorProfile = (contractorName: string, profiles: any[]) => {
    if (!contractorName || !profiles?.length) return null;
    const cleanName = contractorName.toLowerCase().trim();
    return profiles.find((p: any) => {
      const pName = (p.contractors?.name || p.worker_name || '').toLowerCase().trim();
      if (!pName) return false;
      return cleanName === pName || cleanName.includes(pName) || pName.includes(cleanName);
    });
  };

  // Helper to find unit cost from project materials
  const getMaterialUnitCost = (m: any, materialsList: any[]) => {
    const mName = (m.material_name || m.materialName || '').toLowerCase().trim();
    const mId = m.project_material_id || m.projectMaterialId;
    const match = materialsList.find(pm => (mId && pm.id === mId) || (pm.name && pm.name.toLowerCase().trim() === mName));
    return match ? Number(match.cost || 0) : 0;
  };

  // Calculate Labor Cost based on wage profiles
  const laborCostBreakdown = useMemo(() => {
    let totalLaborCost = 0;
    let profilesMatchedCount = 0;

    const entries = laborEntries.map((entry: any) => {
      const contractorName = entry.contractor_name || entry.contractorName || '';
      const profile = findContractorProfile(contractorName, salaryProfiles);
      if (profile) profilesMatchedCount++;

      let entryTotal = 0;
      const workers = (entry.workers || []).map((w: any) => {
        const count = Number(w.count || 0);
        const wType = (w.worker_type || w.workerType || '').trim();
        const wTypeLower = wType.toLowerCase();

        let rate = 0;
        let hasProfileRate = false;

        // 1. Check contractor specific rates
        if (profile) {
          const rates = (profile.rates as Record<string, number>) || {};
          const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
          if (matchingKey && Number(rates[matchingKey]) > 0) {
            rate = Number(rates[matchingKey]);
            hasProfileRate = true;
          } else if (Number(profile.rate) > 0) {
            rate = Number(profile.rate);
            hasProfileRate = true;
          }
        }

        // 2. Global lookup across other profiles for standard category rates
        if (!hasProfileRate && salaryProfiles.length > 0) {
          for (const p of salaryProfiles) {
            const rates = (p.rates as Record<string, number>) || {};
            const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
            if (matchingKey && Number(rates[matchingKey]) > 0) {
              rate = Number(rates[matchingKey]);
              hasProfileRate = true;
              break;
            }
          }
        }

        const subtotal = count * rate;
        entryTotal += subtotal;

        return {
          type: wType,
          count,
          rate,
          subtotal,
          hasProfileRate,
        };
      });

      totalLaborCost += entryTotal;
      return {
        ...entry,
        contractorName,
        profileFound: !!profile,
        entryTotal,
        workersBreakdown: workers,
      };
    });

    return { totalLaborCost, entries, profilesMatchedCount };
  }, [laborEntries, salaryProfiles]);

  // Calculate Materials Cost based on inventory cost per unit
  const materialsCostBreakdown = useMemo(() => {
    let totalMaterialsCost = 0;
    const items = materialEntries.map((m: any) => {
      const qty = Number(m.quantity_consumed || m.quantity || 0);
      const unitCost = getMaterialUnitCost(m, projectMaterials);
      const subtotal = qty * unitCost;
      totalMaterialsCost += subtotal;
      return {
        name: m.material_name || m.materialName || 'Material',
        quantity: qty,
        unit: m.unit || '',
        unitCost,
        subtotal,
        hasCost: unitCost > 0,
      };
    });
    return { totalMaterialsCost, items };
  }, [materialEntries, projectMaterials]);

  const estimatedTotalCost = laborCostBreakdown.totalLaborCost + materialsCostBreakdown.totalMaterialsCost;

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
            <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex flex-col gap-2.5">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold text-sm shrink-0">
                    ₹
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider leading-none">Job Cost of Work</p>
                      {isLoadingCost ? (
                        <Loader2 className="h-2.5 w-2.5 animate-spin text-muted-foreground" />
                      ) : laborCostBreakdown.profilesMatchedCount > 0 ? (
                        <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-0.5">
                          • <CheckCircle2 className="h-2.5 w-2.5" /> Wage Profile Linked
                        </span>
                      ) : (
                        <span className="text-[10px] text-muted-foreground font-medium">
                          • Profile Based
                        </span>
                      )}
                    </div>
                    <p className="text-lg font-bold text-emerald-600 dark:text-emerald-400 leading-tight mt-0.5 font-mono">
                      ₹{estimatedTotalCost.toLocaleString('en-IN')}
                    </p>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 text-xs font-semibold text-muted-foreground self-end sm:self-auto">
                  <span className="bg-background/80 px-2.5 py-1 rounded-lg border border-border flex items-center gap-1">
                    <Users className="h-3 w-3 text-primary" /> Labour: <span className="font-mono text-foreground font-bold">₹{laborCostBreakdown.totalLaborCost.toLocaleString('en-IN')}</span>
                  </span>
                  <span className="bg-background/80 px-2.5 py-1 rounded-lg border border-border flex items-center gap-1">
                    <Package className="h-3 w-3 text-amber-500" /> Materials: <span className="font-mono text-foreground font-bold">₹{materialsCostBreakdown.totalMaterialsCost.toLocaleString('en-IN')}</span>
                  </span>
                </div>
              </div>
            </div>

            {/* Labor Section */}
            {laborCostBreakdown.entries.length > 0 && (
              <div className="space-y-3 pt-3 border-t border-border/40">
                <div className="flex justify-between items-center">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                    <Users className="h-3.5 w-3.5 text-primary" /> Labor & Activity
                  </h4>
                  {laborCostBreakdown.totalLaborCost > 0 && (
                    <span className="text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400">
                      Total: ₹{laborCostBreakdown.totalLaborCost.toLocaleString('en-IN')}
                    </span>
                  )}
                </div>
                <div className="space-y-2">
                  {laborCostBreakdown.entries.map((entry: any, eIdx: number) => (
                    <div key={eIdx} className="p-3.5 rounded-xl bg-muted/40 dark:bg-muted/20 border border-border/50 space-y-2">
                      <div className="flex justify-between items-start gap-2">
                        <div>
                          <span className="font-semibold text-sm text-foreground">{entry.contractorName || 'Contractor Team'}</span>
                          {entry.profileFound && (
                            <span className="ml-2 text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
                              (Wage Profile)
                            </span>
                          )}
                        </div>
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
                      {entry.workersBreakdown && entry.workersBreakdown.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {entry.workersBreakdown.map((w: any, wIdx: number) => (
                            <Badge key={wIdx} variant="outline" className="text-[10px] py-0.5 px-2 bg-background border-border text-foreground font-medium flex items-center gap-1">
                              <span className="text-muted-foreground">{w.type}:</span>
                              <span className="font-bold">{w.count}</span>
                              {w.hasProfileRate ? (
                                <span className="font-mono text-emerald-600 dark:text-emerald-400 font-semibold ml-0.5">
                                  (@ ₹{w.rate} = ₹{w.subtotal.toLocaleString('en-IN')})
                                </span>
                              ) : null}
                            </Badge>
                          ))}
                        </div>
                      )}
                      {entry.entryTotal > 0 && (
                        <div className="flex justify-end pt-1 border-t border-border/20 text-xs text-muted-foreground">
                          <span>Team Daily Wages: <strong className="font-mono text-foreground font-bold">₹{entry.entryTotal.toLocaleString('en-IN')}</strong></span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Materials Section */}
            {materialsCostBreakdown.items.length > 0 && (
              <div className="space-y-2 pt-3 border-t border-border/40">
                <div className="flex justify-between items-center">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                    <Package className="h-3.5 w-3.5 text-amber-500" /> Materials Consumed
                  </h4>
                  {materialsCostBreakdown.totalMaterialsCost > 0 && (
                    <span className="text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400">
                      Total: ₹{materialsCostBreakdown.totalMaterialsCost.toLocaleString('en-IN')}
                    </span>
                  )}
                </div>
                <div className="p-3.5 rounded-xl bg-muted/40 dark:bg-muted/20 border border-border/50 divide-y divide-border/30">
                  {materialsCostBreakdown.items.map((m: any, mIdx: number) => (
                    <div key={mIdx} className="flex justify-between items-center py-2 first:pt-0 last:pb-0 text-xs">
                      <div className="flex flex-col">
                        <span className="text-foreground font-semibold">{m.name}</span>
                        {m.hasCost && (
                          <span className="text-[10px] text-muted-foreground font-mono">
                            ₹{m.unitCost} per {m.unit || 'unit'}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground font-bold">
                          {m.quantity} <span className="text-[10px] font-normal">{m.unit}</span>
                        </span>
                        {m.hasCost && (
                          <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">
                            ₹{m.subtotal.toLocaleString('en-IN')}
                          </span>
                        )}
                      </div>
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
