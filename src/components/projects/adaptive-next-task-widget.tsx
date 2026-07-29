'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Progress } from '@/components/ui/progress';
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Zap,
  Loader2
} from 'lucide-react';
import { getProjectScope, saveProjectScope, type SubheadingProcess, type ProcessTask } from '@/app/actions/ai-progress';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';

interface AdaptiveNextTaskWidgetProps {
  projectId: string;
  projectName: string;
  onProgressUpdated?: (newProgress: number) => void;
}

export function AdaptiveNextTaskWidget({ projectId, projectName, onProgressUpdated }: AdaptiveNextTaskWidgetProps) {
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [appliedProfileId, setAppliedProfileId] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [currentTaskIndex, setCurrentTaskIndex] = useState(0);

  const fetchScope = async () => {
    setIsLoading(true);
    const res = await getProjectScope(projectId);
    if (res.success && res.processes) {
      setProcesses(res.processes);
      setAppliedProfileId(res.appliedProfileId);

      // Find first uncompleted task index as initial default
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
  const flatTaskList: { task: ProcessTask; processTitle: string; processId: string }[] = [];
  for (const proc of processes) {
    if (proc.tasks) {
      for (const t of proc.tasks) {
        flatTaskList.push({
          task: t,
          processTitle: proc.title,
          processId: proc.id
        });
      }
    }
  }

  const activeItem = flatTaskList[currentTaskIndex] || flatTaskList[0];
  const activeTask = activeItem?.task;
  const activeProcessTitle = activeItem?.processTitle;

  // Handle toggling a checklist item in real-time WITHOUT auto-completing the task
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

          // DO NOT auto-change task status to 'completed' here so task stays until logged!
          let newStatus = t.status;
          const completedCount = updatedChecklists.filter(c => c.is_completed).length;
          if (completedCount > 0 && t.status === 'pending') {
            newStatus = 'in-progress';
          }

          return {
            ...t,
            status: newStatus,
            checklists: updatedChecklists
          };
        })
      };
    });

    setProcesses(updatedProcesses);
    const saveRes = await saveProjectScope(projectId, updatedProcesses, appliedProfileId);

    if (saveRes.success && saveRes.progress !== undefined) {
      if (onProgressUpdated) onProgressUpdated(saveRes.progress);
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
    return null; // No scope configured yet
  }

  const completedChecks = activeTask.checklists?.filter(c => c.is_completed).length || 0;
  const totalChecks = activeTask.checklists?.length || 0;
  const checkProgressPct = totalChecks > 0 ? Math.round((completedChecks / totalChecks) * 100) : 0;

  return (
    <Card className="glass-card border-primary/30 bg-gradient-to-r from-primary/10 via-background/80 to-primary/5 shadow-xl relative overflow-hidden">
      <CardHeader className="p-4 sm:p-5 pb-2 space-y-2">
        {/* Top Row: NEXT TASK badge + Clean Manual Navigation Pill */}
        <div className="flex items-center justify-between gap-2">
          <Badge variant="outline" className="bg-primary/15 text-primary border-primary/30 text-xs px-2.5 py-0.5 font-bold uppercase tracking-wider shrink-0">
            <Zap className="h-3 w-3 mr-1" /> Next Task
          </Badge>

          {/* Clean Manual Task Navigation Controls (never cut off) */}
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

        {/* Quality Checks Done Badge placed directly above the checklist items (arrow 2 in markup) */}
        {totalChecks > 0 && (
          <div className="pt-0.5">
            <Badge variant="secondary" className="text-xs font-semibold bg-primary/10 text-primary border border-primary/20 px-2.5 py-0.5">
              {completedChecks} / {totalChecks} Quality Checks Done ({checkProgressPct}%)
            </Badge>
          </div>
        )}
      </CardHeader>

      <CardContent className="p-4 sm:p-5 pt-2 space-y-4">
        {/* Checklist Grid */}
        {totalChecks > 0 ? (
          <div className="space-y-2">
            <Progress value={checkProgressPct} className="h-1.5 bg-muted" />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
              {activeTask.checklists.map((check) => (
                <div
                  key={check.id}
                  onClick={() => handleToggleChecklist(check.id)}
                  className={`p-2.5 rounded-xl border transition-all cursor-pointer flex items-start gap-2.5 ${
                    check.is_completed
                      ? 'bg-emerald-500/10 border-emerald-500/30 text-foreground'
                      : 'bg-background/60 hover:bg-muted/40 border-border/50 text-foreground'
                  }`}
                >
                  <Checkbox
                    id={check.id}
                    checked={check.is_completed}
                    onCheckedChange={() => handleToggleChecklist(check.id)}
                    className="mt-0.5"
                  />
                  <span className={`text-xs font-medium leading-tight ${check.is_completed ? 'line-through text-muted-foreground' : ''}`}>
                    {check.title}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="text-xs text-muted-foreground italic">No quality checklist items specified for this task.</p>
        )}

        {/* Bottom Full-Width Log Work Button */}
        <div className="pt-2">
          <CreateWorklogDialog
            projectId={projectId}
            initialTitle={activeTask.title}
            trigger={
              <Button size="default" className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-bold shadow-md h-11 text-sm rounded-xl">
                <Plus className="mr-1.5 h-4 w-4" /> Log Work for This Task
              </Button>
            }
            onSuccess={() => fetchScope()}
          />
        </div>
      </CardContent>
    </Card>
  );
}
