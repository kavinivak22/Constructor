'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Progress } from '@/components/ui/progress';
import {
  CheckCircle2,
  Clock,
  Sparkles,
  ChevronRight,
  ClipboardCheck,
  Plus,
  ShieldCheck,
  Zap,
  Loader2
} from 'lucide-react';
import { getProjectScope, saveProjectScope, type SubheadingProcess, type ProcessTask } from '@/app/actions/ai-progress';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';
import { useToast } from '@/hooks/use-toast';

interface AdaptiveNextTaskWidgetProps {
  projectId: string;
  projectName: string;
  onProgressUpdated?: (newProgress: number) => void;
}

export function AdaptiveNextTaskWidget({ projectId, projectName, onProgressUpdated }: AdaptiveNextTaskWidgetProps) {
  const { toast } = useToast();
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [appliedProfileId, setAppliedProfileId] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const fetchScope = async () => {
    setIsLoading(true);
    const res = await getProjectScope(projectId);
    if (res.success && res.processes) {
      setProcesses(res.processes);
      setAppliedProfileId(res.appliedProfileId);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    if (projectId) {
      fetchScope();
    }
  }, [projectId]);

  // Find the current active process and next task with its quality checklist
  let activeProcess: SubheadingProcess | null = null;
  let activeTask: ProcessTask | null = null;

  for (const proc of processes) {
    const nextTask = proc.tasks?.find(t => t.status !== 'completed');
    if (nextTask) {
      activeProcess = proc;
      activeTask = nextTask;
      break;
    }
  }

  // Handle toggling a checklist item in real-time
  const handleToggleChecklist = async (checkId: string) => {
    if (!activeProcess || !activeTask) return;

    setIsSaving(true);
    const updatedProcesses = processes.map(p => {
      if (p.id !== activeProcess!.id) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== activeTask!.id) return t;

          const updatedChecklists = t.checklists.map(c =>
            c.id === checkId ? { ...c, is_completed: !c.is_completed } : c
          );

          // Auto-mark task in-progress or completed
          let newStatus = t.status;
          const completedCount = updatedChecklists.filter(c => c.is_completed).length;
          if (completedCount === updatedChecklists.length && updatedChecklists.length > 0) {
            newStatus = 'completed';
          } else if (completedCount > 0) {
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
    setIsSaving(false);

    if (saveRes.success && saveRes.progress !== undefined) {
      if (onProgressUpdated) onProgressUpdated(saveRes.progress);
      toast({
        title: 'Quality Check Updated',
        description: `Project progress updated to ${saveRes.progress}%.`,
      });
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

  if (!activeTask || !activeProcess) {
    return null; // All tasks completed or scope not configured yet
  }

  const completedChecks = activeTask.checklists?.filter(c => c.is_completed).length || 0;
  const totalChecks = activeTask.checklists?.length || 0;
  const checkProgressPct = totalChecks > 0 ? Math.round((completedChecks / totalChecks) * 100) : 0;

  return (
    <Card className="glass-card border-primary/30 bg-gradient-to-r from-primary/10 via-background/80 to-primary/5 shadow-xl relative overflow-hidden">
      <div className="absolute top-0 right-0 p-4 opacity-10 pointer-events-none">
        <ShieldCheck className="h-32 w-32 text-primary" />
      </div>

      <CardHeader className="p-4 sm:p-5 pb-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="bg-primary/15 text-primary border-primary/30 text-xs px-2.5 py-0.5 font-bold uppercase tracking-wider">
              <Zap className="h-3 w-3 mr-1" /> Next Target Task
            </Badge>
            <span className="text-xs text-muted-foreground font-medium truncate max-w-[200px]">
              {activeProcess.title}
            </span>
          </div>
          {totalChecks > 0 && (
            <Badge variant="secondary" className="text-xs font-semibold bg-primary/10 text-primary">
              {completedChecks} / {totalChecks} Quality Checks Done ({checkProgressPct}%)
            </Badge>
          )}
        </div>

        <CardTitle className="text-lg sm:text-xl font-bold font-headline text-foreground mt-2 flex items-center gap-2">
          {activeTask.title}
        </CardTitle>
        <CardDescription className="text-xs text-muted-foreground">
          Site Quality Inspection Checklist — Check off items on site before logging work.
        </CardDescription>
      </CardHeader>

      <CardContent className="p-4 sm:p-5 pt-2 space-y-4">
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

        <div className="pt-2 flex flex-wrap items-center justify-between gap-3 border-t border-border/40">
          <span className="text-xs text-muted-foreground flex items-center gap-1">
            <Clock className="h-3.5 w-3.5 text-primary" /> Active Phase Inspection
          </span>
          <CreateWorklogDialog
            projectId={projectId}
            trigger={
              <Button size="sm" className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold shadow-md">
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
