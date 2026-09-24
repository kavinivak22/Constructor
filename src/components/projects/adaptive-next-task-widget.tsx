'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Progress } from '@/components/ui/progress';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Zap,
  Loader2,
  Link2,
  Sparkles,
  CheckCircle2,
  RotateCcw,
  Search,
  Calendar as CalendarIcon,
  Users,
  Image as ImageIcon,
  X,
  AlertCircle,
} from 'lucide-react';
import { useSupabase } from '@/supabase/provider';
import { getProjectScope, saveProjectScope, type SubheadingProcess, type ProcessTask } from '@/app/actions/ai-progress';
import { getWorklogs } from '@/app/actions/worklogs';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';
import { FullscreenPhotoViewer } from '@/components/worklog/fullscreen-photo-viewer';
import { useToast } from '@/hooks/use-toast';
import { format } from 'date-fns';
import Image from 'next/image';

interface AdaptiveNextTaskWidgetProps {
  projectId: string;
  projectName: string;
  onProgressUpdated?: (newProgress: number) => void;
}

export function AdaptiveNextTaskWidget({ projectId, projectName, onProgressUpdated }: AdaptiveNextTaskWidgetProps) {
  const { supabase } = useSupabase();
  const { toast } = useToast();
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [appliedProfileId, setAppliedProfileId] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [currentTaskIndex, setCurrentTaskIndex] = useState(0);

  // Project Worklogs for linking and autodetection
  const [worklogs, setWorklogs] = useState<any[]>([]);
  const [isLinkDialogOpen, setIsLinkDialogOpen] = useState(false);
  const [linkSearchTerm, setLinkSearchTerm] = useState('');

  // Lightbox for site photos
  const [viewerPhotos, setViewerPhotos] = useState<any[]>([]);
  const [viewerIndex, setViewerIndex] = useState(0);
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [viewerTitle, setViewerTitle] = useState('');
  const [viewerDate, setViewerDate] = useState('');

  const fetchScope = async () => {
    setIsLoading(true);
    try {
      const [scopeClientRes, logsRes] = await Promise.all([
        supabase.from('projects').select('scope_data').eq('id', projectId).single(),
        getWorklogs(projectId),
      ]);

      if (logsRes?.success && logsRes.data) {
        setWorklogs(logsRes.data);
      }

      if (!scopeClientRes.error && scopeClientRes.data?.scope_data) {
        const scope = typeof scopeClientRes.data.scope_data === 'string'
          ? JSON.parse(scopeClientRes.data.scope_data)
          : scopeClientRes.data.scope_data;
        const procs = scope.processes || [];
        setProcesses(procs);
        setAppliedProfileId(scope.appliedProfileId);
        const flatList = procs.flatMap((p: any) => p.tasks || []);
        const firstUncompleted = flatList.findIndex((t: any) => t.status !== 'completed');
        if (firstUncompleted !== -1) {
          setCurrentTaskIndex(firstUncompleted);
        }
        setIsLoading(false);
        return;
      }
    } catch (e) {
      console.warn('Direct query fallback to Server Action');
    }

    const [res, logsRes] = await Promise.all([
      getProjectScope(projectId),
      getWorklogs(projectId),
    ]);

    if (logsRes?.success && logsRes.data) {
      setWorklogs(logsRes.data);
    }

    if (res.success && res.processes) {
      setProcesses(res.processes);
      setAppliedProfileId(res.appliedProfileId);

      const flatList = res.processes.flatMap(p => p.tasks || []);
      const firstUncompleted = flatList.findIndex(t => t.status !== 'completed');
      if (firstUncompleted !== -1) {
        setCurrentTaskIndex(firstUncompleted);
      }
    }
    setIsLoading(false);
  };

  useEffect(() => {
    if (projectId) {
      fetchScope();
    }
  }, [projectId]);

  // Flatten all tasks into a single navigational array
  const flatTaskList: { task: ProcessTask; processTitle: string; processId: string }[] = useMemo(() => {
    const list: { task: ProcessTask; processTitle: string; processId: string }[] = [];
    for (const proc of processes) {
      if (proc.tasks) {
        for (const t of proc.tasks) {
          list.push({
            task: t,
            processTitle: proc.title,
            processId: proc.id,
          });
        }
      }
    }
    return list;
  }, [processes]);

  const activeItem = flatTaskList[currentTaskIndex] || flatTaskList[0];
  const activeTask = activeItem?.task;
  const activeProcessTitle = activeItem?.processTitle;

  // Linked worklogs for the active task (supports multiple IDs + legacy worklog_id)
  const linkedWorklogIds: string[] = useMemo(() => {
    if (!activeTask) return [];
    if (Array.isArray(activeTask.worklog_ids) && activeTask.worklog_ids.length > 0) {
      return activeTask.worklog_ids;
    }
    if (activeTask.worklog_id) {
      return [activeTask.worklog_id];
    }
    return [];
  }, [activeTask]);

  const linkedWorklogs = useMemo(() => {
    return worklogs.filter(w => linkedWorklogIds.includes(w.id));
  }, [worklogs, linkedWorklogIds]);

  // Smart Autodetection Algorithm: find unlinked worklogs matching task title keywords
  const autodetectedWorklogs = useMemo(() => {
    if (!activeTask || !worklogs.length) return [];

    const stopWords = new Set(['and', 'the', 'for', 'with', 'task', 'work', 'site', 'stage', 'phase', 'part']);
    const cleanTokens = activeTask.title
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, ' ')
      .split(/\s+/)
      .filter(w => w.length > 2 && !stopWords.has(w));

    if (cleanTokens.length === 0) return [];

    const unlinked = worklogs.filter(w => !linkedWorklogIds.includes(w.id));

    const scored = unlinked.map(w => {
      let score = 0;
      const wTitle = (w.title || '').toLowerCase();
      const laborDesc = (w.labor || []).map((l: any) => `${l.work_description || ''} ${l.category || ''}`).join(' ').toLowerCase();
      const combinedText = `${wTitle} ${laborDesc}`;

      // Exact title match gets maximum score
      if (wTitle === activeTask.title.toLowerCase().trim()) {
        score += 100;
      }

      // Check keywords
      for (const token of cleanTokens) {
        if (wTitle.includes(token)) score += 15;
        else if (combinedText.includes(token)) score += 8;
      }

      return { worklog: w, score };
    });

    return scored
      .filter(item => item.score >= 15)
      .sort((a, b) => b.score - a.score)
      .slice(0, 2)
      .map(item => item.worklog);
  }, [activeTask, worklogs, linkedWorklogIds]);

  // Handle toggling a checklist item
  const handleToggleChecklist = async (checkId: string) => {
    if (!activeItem || !activeTask) return;

    const updatedProcesses = processes.map(p => {
      if (p.id !== activeItem.processId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask.id) return t;

          const updatedChecklists = t.checklists.map(c =>
            c.id === checkId ? { ...c, is_completed: !c.is_completed } : c
          );

          return {
            ...t,
            checklists: updatedChecklists,
          };
        }),
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);
    if (saveRes.success && saveRes.progress !== undefined && onProgressUpdated) {
      onProgressUpdated(saveRes.progress);
    }
  };

  // Link a worklog to the active task (supports multiple)
  const handleLinkWorklog = async (logId: string) => {
    if (!activeItem || !activeTask) return;

    const currentIds = activeTask.worklog_ids || (activeTask.worklog_id ? [activeTask.worklog_id] : []);
    if (currentIds.includes(logId)) return;

    const newIds = [...currentIds, logId];

    const updatedProcesses = processes.map(p => {
      if (p.id !== activeItem.processId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask.id) return t;
          return {
            ...t,
            worklog_ids: newIds,
            worklog_id: newIds[0],
            status: t.status === 'pending' ? 'in-progress' : t.status,
          };
        }),
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);
    if (saveRes.success) {
      toast({
        title: 'Worklog Linked',
        description: 'Daily log has been assigned as site proof for this task.',
      });
      if (saveRes.progress !== undefined && onProgressUpdated) {
        onProgressUpdated(saveRes.progress);
      }
    }
  };

  // Unlink a worklog from the active task
  const handleUnlinkWorklog = async (logId: string) => {
    if (!activeItem || !activeTask) return;

    const currentIds = activeTask.worklog_ids || (activeTask.worklog_id ? [activeTask.worklog_id] : []);
    const newIds = currentIds.filter(id => id !== logId);

    const updatedProcesses = processes.map(p => {
      if (p.id !== activeItem.processId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask.id) return t;
          return {
            ...t,
            worklog_ids: newIds,
            worklog_id: newIds.length > 0 ? newIds[0] : null,
          };
        }),
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);
    if (saveRes.success) {
      toast({
        title: 'Worklog Unlinked',
        description: 'The worklog was removed from this task.',
      });
      if (saveRes.progress !== undefined && onProgressUpdated) {
        onProgressUpdated(saveRes.progress);
      }
    }
  };

  // Complete task & advance to next pending task
  const handleCompleteTask = async () => {
    if (!activeItem || !activeTask) return;

    if (linkedWorklogIds.length === 0) {
      toast({
        variant: 'destructive',
        title: 'Site Evidence Required',
        description: 'Please link or log at least one worklog for this task before completing it.',
      });
      return;
    }

    // Auto mark all checklists done upon completion
    const updatedProcesses = processes.map(p => {
      if (p.id !== activeItem.processId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask.id) return t;
          return {
            ...t,
            status: 'completed' as const,
            checklists: t.checklists.map(c => ({ ...c, is_completed: true })),
          };
        }),
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);

    if (saveRes.success) {
      toast({
        title: 'Task Completed! 🎉',
        description: `"${activeTask.title}" has been marked complete with proof of work.`,
      });

      if (saveRes.progress !== undefined && onProgressUpdated) {
        onProgressUpdated(saveRes.progress);
      }

      // Smoothly advance to the next uncompleted task
      const updatedFlat = updatedProcesses.flatMap(p => p.tasks || []);
      const nextPendingIndex = updatedFlat.findIndex((t, idx) => idx > currentTaskIndex && t.status !== 'completed');
      if (nextPendingIndex !== -1) {
        setCurrentTaskIndex(nextPendingIndex);
      } else {
        const firstUnfinished = updatedFlat.findIndex(t => t.status !== 'completed');
        if (firstUnfinished !== -1) setCurrentTaskIndex(firstUnfinished);
      }
    }
  };

  // Reopen a completed task
  const handleReopenTask = async () => {
    if (!activeItem || !activeTask) return;

    const updatedProcesses = processes.map(p => {
      if (p.id !== activeItem.processId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask.id) return t;
          return {
            ...t,
            status: 'in-progress' as const,
          };
        }),
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);
    if (saveRes.success) {
      toast({
        title: 'Task Reopened',
        description: `"${activeTask.title}" has been set back to In Progress.`,
      });
      if (saveRes.progress !== undefined && onProgressUpdated) {
        onProgressUpdated(saveRes.progress);
      }
    }
  };

  if (isLoading) {
    return (
      <Card className="glass-card border-primary/20 bg-gradient-to-r from-primary/5 via-background to-primary/5">
        <CardContent className="p-6 flex items-center justify-center text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin mr-2" /> Loading next task quality checklist...
        </CardContent>
      </Card>
    );
  }

  if (!activeTask || flatTaskList.length === 0) {
    return null;
  }

  const completedChecks = activeTask.checklists?.filter(c => c.is_completed).length || 0;
  const totalChecks = activeTask.checklists?.length || 0;
  const checkProgressPct = totalChecks > 0 ? Math.round((completedChecks / totalChecks) * 100) : 0;
  const isCompleted = activeTask.status === 'completed';

  return (
    <Card className="glass-card border-primary/30 bg-gradient-to-r from-primary/10 via-background/80 to-primary/5 shadow-xl relative overflow-hidden">
      <CardHeader className="p-4 sm:p-5 pb-2 space-y-2">
        {/* Top Row: NEXT TASK badge / Status + Clean Manual Navigation Pill */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Badge 
              variant="outline" 
              className={cn(
                "text-xs px-2.5 py-0.5 font-bold uppercase tracking-wider shrink-0",
                isCompleted 
                  ? "bg-emerald-500/15 text-emerald-500 border-emerald-500/30" 
                  : "bg-primary/15 text-primary border-primary/30"
              )}
            >
              {isCompleted ? <CheckCircle2 className="h-3 w-3 mr-1" /> : <Zap className="h-3 w-3 mr-1" />}
              {isCompleted ? "Completed Task" : "Next Task"}
            </Badge>

            {linkedWorklogIds.length > 0 && !isCompleted && (
              <Badge variant="secondary" className="text-[10px] px-2 py-0.5 bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 font-semibold">
                In Progress ({linkedWorklogIds.length} Logs)
              </Badge>
            )}
          </div>

          {/* Clean Manual Task Navigation Controls */}
          <div className="flex items-center gap-1 bg-background/80 px-2 py-0.5 rounded-xl border border-border/50 shadow-sm shrink-0">
            <Button
              variant="ghost"
              size="icon"
              disabled={currentTaskIndex === 0}
              onClick={() => setCurrentTaskIndex(prev => Math.max(0, prev - 1))}
              className="h-6 w-6 rounded-lg p-0 hover:bg-muted"
              title="Previous Task Checklist"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <span className="text-[11px] font-bold text-foreground tracking-tight whitespace-nowrap px-1">
              {currentTaskIndex + 1} / {flatTaskList.length}
            </span>
            <Button
              variant="ghost"
              size="icon"
              disabled={currentTaskIndex === flatTaskList.length - 1}
              onClick={() => setCurrentTaskIndex(prev => Math.min(flatTaskList.length - 1, prev + 1))}
              className="h-6 w-6 rounded-lg p-0 hover:bg-muted"
              title="Next Task Checklist"
            >
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>

        {/* Stage Title */}
        <p className="text-xs font-semibold text-muted-foreground/80 tracking-wide">
          {activeProcessTitle}
        </p>

        {/* Task Title */}
        <CardTitle className="text-lg sm:text-xl font-bold font-headline text-foreground leading-snug">
          {activeTask.title}
        </CardTitle>

        {/* Quality Checks Progress */}
        {totalChecks > 0 && (
          <div className="pt-0.5">
            <Badge variant="secondary" className="text-xs font-semibold bg-primary/10 text-primary border border-primary/20 px-2.5 py-0.5">
              {completedChecks} / {totalChecks} Quality Checks Done ({checkProgressPct}%)
            </Badge>
          </div>
        )}
      </CardHeader>

      <CardContent className="p-4 sm:p-5 pt-2 space-y-4">
        {/* Quality Checklist Items */}
        {totalChecks > 0 ? (
          <div className="space-y-2">
            <Progress value={checkProgressPct} className="h-1.5 bg-muted" />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
              {activeTask.checklists.map((check) => (
                <div
                  key={check.id}
                  onClick={() => handleToggleChecklist(check.id)}
                  className={cn(
                    "p-2.5 rounded-xl border transition-all cursor-pointer flex items-start gap-2.5",
                    check.is_completed
                      ? "bg-emerald-500/10 border-emerald-500/30 text-foreground"
                      : "bg-background/60 hover:bg-muted/40 border-border/50 text-foreground"
                  )}
                >
                  <Checkbox
                    id={check.id}
                    checked={check.is_completed}
                    onCheckedChange={() => handleToggleChecklist(check.id)}
                    className="mt-0.5"
                  />
                  <span className={cn("text-xs font-medium leading-tight", check.is_completed && "line-through text-muted-foreground")}>
                    {check.title}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="text-xs text-muted-foreground italic">No quality checklist items specified for this task.</p>
        )}

        {/* =============================================================== */}
        {/* LINKED WORKLOGS SECTION (Proof of Work)                         */}
        {/* =============================================================== */}
        <div className="space-y-2.5 pt-2 border-t border-white/10 dark:border-white/5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-foreground flex items-center gap-1.5">
              <Link2 className="h-3.5 w-3.5 text-primary" />
              Proof of Work ({linkedWorklogs.length} Linked)
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsLinkDialogOpen(true)}
              className="h-7 text-xs text-primary hover:text-primary/80 font-semibold px-2 hover:bg-white/10"
            >
              {linkedWorklogs.length > 0 ? "+ Link Another Log" : "🔗 Link Existing Log"}
            </Button>
          </div>

          {/* List of currently linked worklogs */}
          {linkedWorklogs.length > 0 ? (
            <div className="space-y-2">
              {linkedWorklogs.map((log) => {
                const totalWorkers = (log.labor || []).reduce((acc: number, entry: any) => {
                  return acc + (entry.workers || []).reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0);
                }, 0);
                const hasPhotos = log.photos && log.photos.length > 0;

                return (
                  <div
                    key={log.id}
                    className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-background/70 border border-emerald-500/30 text-xs shadow-sm"
                  >
                    <div className="flex items-center gap-2.5 min-w-0 flex-1">
                      {/* Photo Thumbnail or Date Icon */}
                      {hasPhotos ? (
                        <button
                          type="button"
                          onClick={() => {
                            setViewerPhotos(log.photos);
                            setViewerIndex(0);
                            setViewerTitle(log.title || 'Site Photo');
                            setViewerDate(format(new Date(log.date), 'MMM dd, yyyy'));
                            setIsViewerOpen(true);
                          }}
                          className="relative w-9 h-9 rounded-lg overflow-hidden shrink-0 border border-white/20 hover:scale-105 transition-transform"
                          title="View site photos"
                        >
                          <Image src={log.photos[0].photo_url} alt="Site" fill className="object-cover" unoptimized />
                          {log.photos.length > 1 && (
                            <span className="absolute bottom-0 right-0 bg-black/80 text-[8px] text-white px-1 font-bold rounded-tl">
                              +{log.photos.length - 1}
                            </span>
                          )}
                        </button>
                      ) : (
                        <div className="w-9 h-9 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
                          <CalendarIcon className="h-4 w-4" />
                        </div>
                      )}

                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-foreground truncate">
                            {log.title || 'Daily Log'}
                          </span>
                          <span className="text-[10px] text-muted-foreground whitespace-nowrap">
                            {format(new Date(log.date), 'dd MMM yyyy')}
                          </span>
                        </div>
                        <div className="text-[11px] text-muted-foreground flex items-center gap-2">
                          <span>{totalWorkers} workers</span>
                          {log.creator && (
                            <span className="text-muted-foreground/75 truncate">
                              • By {log.creator.display_name || log.creator.email?.split('@')[0]}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleUnlinkWorklog(log.id)}
                      className="h-7 w-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-lg shrink-0"
                      title="Unlink this worklog"
                    >
                      <X className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="p-3 rounded-xl border border-dashed border-amber-500/40 bg-amber-500/5 text-xs text-amber-700 dark:text-amber-300 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">No worklog assigned to this task yet.</p>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  Link an existing log or log new work to attach proof and unlock completion.
                </p>
              </div>
            </div>
          )}

          {/* Autodetection Suggestion Ribbon */}
          {autodetectedWorklogs.length > 0 && (
            <div className="p-3 rounded-xl border border-primary/30 bg-primary/5 space-y-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-primary">
                <Sparkles className="h-3.5 w-3.5 text-primary" />
                Autodetected Matching Worklog:
              </div>
              {autodetectedWorklogs.map((log) => (
                <div key={log.id} className="flex items-center justify-between gap-2 text-xs bg-background/80 p-2 rounded-lg border border-primary/20">
                  <div className="truncate flex-1">
                    <strong className="text-foreground">{log.title}</strong>
                    <span className="text-muted-foreground ml-2 text-[10px]">
                      {format(new Date(log.date), 'dd MMM')}
                    </span>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleLinkWorklog(log.id)}
                    className="h-7 text-xs border-primary/30 text-primary hover:bg-primary hover:text-primary-foreground font-semibold px-2.5"
                  >
                    + Link to Task
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* =============================================================== */}
        {/* BOTTOM ACTIONS: LOG NEW WORK / COMPLETE TASK                   */}
        {/* =============================================================== */}
        <div className="pt-2 flex flex-col sm:flex-row gap-2.5">
          {/* Log New Work Shortcut (automatically links to task upon saving) */}
          <div className="flex-1">
            <CreateWorklogDialog
              projectId={projectId}
              initialTitle={activeTask.title}
              trigger={
                <Button 
                  variant="outline" 
                  size="default" 
                  className="w-full glass border-white/10 dark:border-white/5 hover:bg-white/10 text-foreground font-bold h-11 text-xs sm:text-sm rounded-xl"
                >
                  <Plus className="mr-1.5 h-4 w-4" /> Log New Work
                </Button>
              }
              onSuccess={(newLogId) => {
                if (newLogId) {
                  handleLinkWorklog(newLogId);
                }
                fetchScope();
              }}
            />
          </div>

          {/* Complete Task & Advance Button */}
          <div className="flex-1">
            {isCompleted ? (
              <Button
                variant="outline"
                onClick={handleReopenTask}
                className="w-full border-white/10 hover:bg-white/10 text-muted-foreground font-medium h-11 text-xs sm:text-sm rounded-xl"
              >
                <RotateCcw className="mr-1.5 h-4 w-4" /> Reopen Task
              </Button>
            ) : (
              <Button
                size="default"
                disabled={linkedWorklogIds.length === 0}
                onClick={handleCompleteTask}
                className={cn(
                  "w-full font-bold shadow-md h-11 text-xs sm:text-sm rounded-xl transition-all",
                  linkedWorklogIds.length > 0 
                    ? "bg-emerald-600 hover:bg-emerald-500 text-white" 
                    : "bg-muted text-muted-foreground opacity-60 cursor-not-allowed"
                )}
                title={linkedWorklogIds.length === 0 ? "Link at least one worklog to complete" : "Mark completed and advance"}
              >
                <CheckCircle2 className="mr-1.5 h-4 w-4" /> Complete & Advance →
              </Button>
            )}
          </div>
        </div>
      </CardContent>

      {/* =============================================================== */}
      {/* MANUAL WORKLOG SELECTION DIALOG                                 */}
      {/* =============================================================== */}
      <Dialog open={isLinkDialogOpen} onOpenChange={setIsLinkDialogOpen}>
        <DialogContent className="max-w-xl max-h-[85vh] flex flex-col p-0 overflow-hidden glass border border-white/10 dark:border-white/5 rounded-2xl">
          <DialogHeader className="p-4 sm:p-6 border-b border-white/10 bg-white/5 pb-4">
            <DialogTitle className="text-lg font-bold flex items-center gap-2">
              <Link2 className="h-5 w-5 text-primary" /> Link Existing Worklog
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              Select any daily worklog from <strong>{projectName}</strong> to assign as proof of work for "{activeTask.title}".
            </DialogDescription>
            <div className="relative mt-2">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
              <Input
                placeholder="Search worklogs by title, description..."
                className="pl-8 h-9 text-xs glass border-white/10 focus-visible:ring-primary"
                value={linkSearchTerm}
                onChange={(e) => setLinkSearchTerm(e.target.value)}
              />
            </div>
          </DialogHeader>

          <div className="flex-1 overflow-y-auto p-4 space-y-2">
            {worklogs.length === 0 ? (
              <p className="text-center py-8 text-xs text-muted-foreground italic">
                No worklogs have been recorded for this project yet.
              </p>
            ) : (
              worklogs
                .filter(w => {
                  if (!linkSearchTerm) return true;
                  const query = linkSearchTerm.toLowerCase();
                  const title = (w.title || '').toLowerCase();
                  const desc = (w.labor || []).map((l: any) => l.work_description).join(' ').toLowerCase();
                  return title.includes(query) || desc.includes(query);
                })
                .map(w => {
                  const isAlreadyLinked = linkedWorklogIds.includes(w.id);
                  const totalWorkers = (w.labor || []).reduce((acc: number, entry: any) => {
                    return acc + (entry.workers || []).reduce((wAcc: number, item: any) => wAcc + Number(item.count || 0), 0);
                  }, 0);

                  return (
                    <div
                      key={w.id}
                      className={cn(
                        "flex items-center justify-between gap-3 p-3 rounded-xl border text-xs transition-all",
                        isAlreadyLinked
                          ? "bg-emerald-500/10 border-emerald-500/30"
                          : "bg-background/80 hover:bg-muted/40 border-white/10"
                      )}
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-bold text-foreground text-sm truncate">
                            {w.title || 'Daily Log'}
                          </span>
                          <span className="text-[10px] text-muted-foreground">
                            {format(new Date(w.date), 'dd MMM yyyy')}
                          </span>
                        </div>
                        <div className="text-[11px] text-muted-foreground flex items-center gap-2 mt-0.5">
                          <span className="flex items-center gap-1">
                            <Users className="h-3 w-3" /> {totalWorkers} workers
                          </span>
                          {w.photos?.length > 0 && (
                            <span className="flex items-center gap-1">
                              <ImageIcon className="h-3 w-3" /> {w.photos.length} photos
                            </span>
                          )}
                        </div>
                      </div>

                      {isAlreadyLinked ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUnlinkWorklog(w.id)}
                          className="h-8 text-xs text-destructive hover:bg-destructive/10 font-semibold"
                        >
                          Unlink
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => {
                            handleLinkWorklog(w.id);
                          }}
                          className="h-8 text-xs bg-primary hover:bg-primary/90 text-primary-foreground font-semibold px-3"
                        >
                          Link
                        </Button>
                      )}
                    </div>
                  );
                })
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Fullscreen Photo Viewer Lightbox */}
      <FullscreenPhotoViewer
        photos={viewerPhotos}
        initialIndex={viewerIndex}
        isOpen={isViewerOpen}
        onClose={() => setIsViewerOpen(false)}
        title={viewerTitle}
        dateLabel={viewerDate}
      />
    </Card>
  );
}
