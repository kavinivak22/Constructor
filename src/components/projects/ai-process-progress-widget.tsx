'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import {
  Layers,
  CheckCircle2,
  Clock,
  Sparkles,
  Settings2,
  ListTodo,
  CheckSquare,
  Loader2
} from 'lucide-react';
import { getProjectScope, saveProjectScope, type SubheadingProcess } from '@/app/actions/ai-progress';
import { ProcessScopeDialog } from '@/components/projects/process-scope-dialog';
import { useToast } from '@/hooks/use-toast';

interface AIProcessProgressWidgetProps {
  projectId: string;
  projectName: string;
  onProgressUpdated?: (newProgress: number) => void;
}

export function AIProcessProgressWidget({ projectId, projectName, onProgressUpdated }: AIProcessProgressWidgetProps) {
  const { toast } = useToast();
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [appliedProfileId, setAppliedProfileId] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);

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

  const handleToggleTaskStatus = async (procId: string, taskId: string, currentStatus: string) => {
    const newStatus = currentStatus === 'completed' ? 'pending' : 'completed';

    const updatedProcesses = processes.map(p => {
      if (p.id !== procId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== taskId) return t;
          return {
            ...t,
            status: newStatus as any,
            checklists: t.checklists.map(c => ({
              ...c,
              is_completed: newStatus === 'completed'
            }))
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

  const handleToggleChecklist = async (procId: string, taskId: string, checkId: string) => {
    const updatedProcesses = processes.map(p => {
      if (p.id !== procId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== taskId) return t;

          const updatedChecklists = t.checklists.map(c =>
            c.id === checkId ? { ...c, is_completed: !c.is_completed } : c
          );

          let newStatus = t.status;
          const completedCount = updatedChecklists.filter(c => c.is_completed).length;
          if (completedCount === updatedChecklists.length && updatedChecklists.length > 0) {
            newStatus = 'completed';
          } else if (completedCount > 0) {
            newStatus = 'in-progress';
          }

          return {
            ...t,
            status: newStatus as any,
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
      <Card className="glass-card">
        <CardContent className="p-6 flex items-center justify-center text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin mr-2" /> Loading construction scope breakdown...
        </CardContent>
      </Card>
    );
  }

  if (processes.length === 0) {
    return (
      <Card className="glass-card border-dashed border-primary/40 p-5 sm:p-6 text-center space-y-3">
        <div className="h-12 w-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
          <Layers className="h-6 w-6" />
        </div>
        <div className="space-y-1 max-w-md mx-auto">
          <h3 className="font-bold text-base sm:text-lg text-foreground">Select a Building Construction Plan</h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Choose your project type (e.g. Framed Villa, Load-Bearing House, Commercial Building) to start tracking stage-by-stage site progress and quality checklists.
          </p>
        </div>
        <div className="pt-2">
          <ProcessScopeDialog
            projectId={projectId}
            projectName={projectName}
            onSuccess={() => fetchScope()}
            trigger={
              <Button size="default" className="w-full sm:w-auto bg-primary text-primary-foreground font-semibold px-6 py-2.5 rounded-xl shadow-md">
                <Settings2 className="mr-2 h-4 w-4" /> Choose Building Construction Plan
              </Button>
            }
          />
        </div>
      </Card>
    );
  }

  return (
    <Card className="glass-card border-border/60">
      <CardHeader className="p-4 sm:p-6 pb-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <CardTitle className="text-base sm:text-lg font-bold font-headline text-foreground flex items-center gap-2">
            <Layers className="h-5 w-5 text-primary" /> Construction Stages & Quality Checklists
          </CardTitle>
          <CardDescription className="text-xs text-muted-foreground">
            Stage-by-stage progress breakdown with quality control checklists.
          </CardDescription>
        </div>

        <ProcessScopeDialog
          projectId={projectId}
          projectName={projectName}
          onSuccess={() => fetchScope()}
          trigger={
            <Button variant="outline" size="sm" className="w-full sm:w-auto shrink-0 font-medium">
              <Settings2 className="mr-1.5 h-3.5 w-3.5 text-primary" /> Change Building Plan
            </Button>
          }
        />
      </CardHeader>

      <CardContent className="p-4 sm:p-6 pt-0 space-y-4">
        <Accordion type="single" collapsible className="w-full space-y-2">
          {processes.map((proc, pIdx) => {
            const totalTasks = proc.tasks?.length || 0;
            const completedTasks = proc.tasks?.filter(t => t.status === 'completed').length || 0;
            const procProgressPct = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

            return (
              <AccordionItem key={proc.id || pIdx} value={proc.id || `p_${pIdx}`} className="border border-border/40 rounded-xl px-3 bg-muted/20">
                <AccordionTrigger className="hover:no-underline py-3">
                  <div className="flex-1 text-left flex items-center justify-between gap-3 pr-2">
                    <div className="space-y-1">
                      <h4 className="text-sm font-bold text-foreground">{proc.title}</h4>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <span>Weight: {proc.weight}%</span>
                        <span>•</span>
                        <span>{completedTasks}/{totalTasks} Tasks Done</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="w-20 hidden sm:block">
                        <Progress value={procProgressPct} className="h-2" />
                      </div>
                      <Badge variant="outline" className={`text-xs font-bold ${procProgressPct === 100 ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/30' : 'bg-primary/10 text-primary border-primary/20'}`}>
                        {procProgressPct}%
                      </Badge>
                    </div>
                  </div>
                </AccordionTrigger>

                <AccordionContent className="pt-2 pb-4 space-y-3 border-t border-border/30">
                  {proc.tasks && proc.tasks.length > 0 ? (
                    proc.tasks.map((task) => {
                      const completedChecks = task.checklists?.filter(c => c.is_completed).length || 0;
                      const totalChecks = task.checklists?.length || 0;

                      return (
                        <div key={task.id} className="p-3 rounded-xl bg-background/80 border border-border/50 space-y-2">
                          <div className="flex items-start justify-between gap-3">
                            <div className="flex items-start gap-2.5">
                              <Checkbox
                                id={task.id}
                                checked={task.status === 'completed'}
                                onCheckedChange={() => handleToggleTaskStatus(proc.id, task.id, task.status)}
                                className="mt-0.5"
                              />
                              <div>
                                <label
                                  htmlFor={task.id}
                                  className={`text-xs font-semibold cursor-pointer select-none ${
                                    task.status === 'completed' ? 'line-through text-muted-foreground' : 'text-foreground'
                                  }`}
                                >
                                  {task.title}
                                </label>
                                {task.description && (
                                  <p className="text-[11px] text-muted-foreground mt-0.5">{task.description}</p>
                                )}
                              </div>
                            </div>

                            <Badge
                              variant="secondary"
                              className={`text-[10px] capitalize font-semibold shrink-0 ${
                                task.status === 'completed'
                                  ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                                  : task.status === 'in-progress'
                                  ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
                                  : 'bg-muted text-muted-foreground'
                              }`}
                            >
                              {task.status}
                            </Badge>
                          </div>

                          {/* Nested Quality Control Checklists */}
                          {totalChecks > 0 && (
                            <div className="pl-6 space-y-1.5 pt-1 border-t border-border/30">
                              <div className="flex items-center justify-between text-[11px] font-medium text-muted-foreground">
                                <span className="flex items-center gap-1">
                                  <CheckSquare className="h-3 w-3 text-primary" /> Quality Checkpoints
                                </span>
                                <span>{completedChecks}/{totalChecks} Verified</span>
                              </div>
                              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                                {task.checklists.map((chk) => (
                                  <div
                                    key={chk.id}
                                    onClick={() => handleToggleChecklist(proc.id, task.id, chk.id)}
                                    className={`p-1.5 px-2 rounded-lg border text-[11px] cursor-pointer flex items-center gap-2 ${
                                      chk.is_completed
                                        ? 'bg-emerald-500/10 border-emerald-500/20 text-foreground'
                                        : 'bg-muted/30 border-border/40 text-muted-foreground hover:bg-muted/60'
                                    }`}
                                  >
                                    <Checkbox
                                      id={chk.id}
                                      checked={chk.is_completed}
                                      onCheckedChange={() => handleToggleChecklist(proc.id, task.id, chk.id)}
                                      className="h-3.5 w-3.5"
                                    />
                                    <span className={`truncate ${chk.is_completed ? 'line-through' : ''}`}>
                                      {chk.title}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <p className="text-xs text-muted-foreground py-2">No tasks created under this process stage.</p>
                  )}
                </AccordionContent>
              </AccordionItem>
            );
          })}
        </Accordion>
      </CardContent>
    </Card>
  );
}
