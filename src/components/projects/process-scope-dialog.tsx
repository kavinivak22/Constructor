'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import {
  Plus,
  Trash2,
  Sparkles,
  Settings2,
  Save,
  CheckCircle2,
  CheckSquare,
  Layers,
  Loader2,
  Copy
} from 'lucide-react';
import {
  getHabitProfiles,
  saveHabitProfile,
  getProjectScope,
  saveProjectScope,
  applyProfileToProject,
  generateTaskChecklistWithGemini,
  type HabitProfile,
  type SubheadingProcess,
  type ProcessTask,
  type QualityCheckItem
} from '@/app/actions/ai-progress';
import { useToast } from '@/hooks/use-toast';

interface ProcessScopeDialogProps {
  projectId: string;
  projectName: string;
  onSuccess: () => void;
  trigger?: React.ReactNode;
}

export function ProcessScopeDialog({ projectId, projectName, onSuccess, trigger }: ProcessScopeDialogProps) {
  const { toast } = useToast();
  const [isOpen, setIsOpen] = useState(false);
  const [habitProfiles, setHabitProfiles] = useState<HabitProfile[]>([]);
  const [selectedProfileId, setSelectedProfileId] = useState<string>('');
  const [processes, setProcesses] = useState<SubheadingProcess[]>([]);
  const [syncToMaster, setSyncToMaster] = useState(false);
  const [newProfileName, setNewProfileName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [generatingChecklistTaskId, setGeneratingChecklistTaskId] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    const [profilesRes, scopeRes] = await Promise.all([
      getHabitProfiles(),
      getProjectScope(projectId)
    ]);

    if (profilesRes.success && profilesRes.data) {
      setHabitProfiles(profilesRes.data);
    }

    if (scopeRes.success && scopeRes.processes) {
      setProcesses(scopeRes.processes);
      if (scopeRes.appliedProfileId) {
        setSelectedProfileId(scopeRes.appliedProfileId);
      }
    }
    setIsLoading(false);
  };

  useEffect(() => {
    if (isOpen) {
      loadData();
    }
  }, [isOpen]);

  const handleSelectProfile = async (profileId: string) => {
    setSelectedProfileId(profileId);
    const target = habitProfiles.find(p => p.id === profileId);
    if (target) {
      // Clone fresh structure
      const cloned = target.processes.map(proc => ({
        ...proc,
        id: `p_${Date.now()}_${Math.random().toString(36).substring(2, 5)}`,
        tasks: proc.tasks.map(t => ({
          ...t,
          id: `t_${Date.now()}_${Math.random().toString(36).substring(2, 5)}`,
          status: 'pending' as const,
          checklists: t.checklists.map(c => ({
            ...c,
            id: `c_${Date.now()}_${Math.random().toString(36).substring(2, 5)}`,
            is_completed: false
          }))
        }))
      }));
      setProcesses(cloned);
      toast({
        title: 'Profile Loaded',
        description: `Loaded processes and checklists from '${target.name}'.`,
      });
    }
  };

  // Add Process
  const handleAddProcess = () => {
    const newProc: SubheadingProcess = {
      id: `p_${Date.now()}`,
      title: `Process Stage ${processes.length + 1}`,
      weight: 15,
      tasks: []
    };
    setProcesses([...processes, newProc]);
  };

  // Delete Process
  const handleDeleteProcess = (procId: string) => {
    setProcesses(processes.filter(p => p.id !== procId));
  };

  // Add Task to Process
  const handleAddTask = (procId: string) => {
    setProcesses(processes.map(p => {
      if (p.id !== procId) return p;
      const newTask: ProcessTask = {
        id: `t_${Date.now()}`,
        title: 'New Site Task',
        status: 'pending',
        checklists: [
          { id: `c_${Date.now()}_1`, title: 'Verify dimensions and level', is_completed: false },
          { id: `c_${Date.now()}_2`, title: 'Inspect material quality & mix', is_completed: false }
        ]
      };
      return { ...p, tasks: [...p.tasks, newTask] };
    }));
  };

  // Delete Task
  const handleDeleteTask = (procId: string, taskId: string) => {
    setProcesses(processes.map(p => {
      if (p.id !== procId) return p;
      return { ...p, tasks: p.tasks.filter(t => t.id !== taskId) };
    }));
  };

  // Add Checklist Item to Task
  const handleAddChecklist = (procId: string, taskId: string) => {
    setProcesses(processes.map(p => {
      if (p.id !== procId) return p;
      return {
        ...p,
        tasks: p.tasks.map(t => {
          if (t.id !== taskId) return t;
          const newChk: QualityCheckItem = {
            id: `c_${Date.now()}`,
            title: 'New Quality Checkpoint',
            is_completed: false
          };
          return { ...t, checklists: [...t.checklists, newChk] };
        })
      };
    }));
  };

  // AI Auto-Generate Quality Checklist for a Task
  const handleGenerateAIChecklist = async (procId: string, taskId: string, taskTitle: string) => {
    setGeneratingChecklistTaskId(taskId);
    const aiRes = await generateTaskChecklistWithGemini(taskTitle);
    setGeneratingChecklistTaskId(null);

    if (aiRes.success && aiRes.checklists) {
      const generatedChecks: QualityCheckItem[] = aiRes.checklists.map((item, idx) => ({
        id: `ai_chk_${Date.now()}_${idx}`,
        title: item,
        is_completed: false
      }));

      setProcesses(processes.map(p => {
        if (p.id !== procId) return p;
        return {
          ...p,
          tasks: p.tasks.map(t => {
            if (t.id !== taskId) return t;
            return { ...t, checklists: generatedChecks };
          })
        };
      }));

      toast({
        title: 'AI Quality Checklist Generated',
        description: `Added ${generatedChecks.length} engineering quality checkpoints for "${taskTitle}".`,
      });
    }
  };

  // Save changes to project & optionally sync master template
  const handleSave = async () => {
    setIsSubmitting(true);
    const selectedProfile = habitProfiles.find(p => p.id === selectedProfileId);
    const masterSyncId = syncToMaster && selectedProfileId ? selectedProfileId : undefined;

    const res = await saveProjectScope(projectId, processes, selectedProfileId, masterSyncId);
    setIsSubmitting(false);

    if (res.success) {
      toast({
        title: 'Construction Scope Saved',
        description: `Project progress updated to ${res.progress}%.${syncToMaster ? ` Master profile '${selectedProfile?.name}' synced!` : ''}`,
      });
      setIsOpen(false);
      onSuccess();
    } else {
      toast({
        title: 'Error',
        description: res.error || 'Failed to save scope.',
        variant: 'destructive'
      });
    }
  };

  // Save as new Habit Profile
  const handleSaveAsNewProfile = async () => {
    if (!newProfileName.trim()) {
      toast({
        title: 'Profile Name Required',
        description: 'Please enter a name for your new Habit Profile (e.g. Residential Villa - Framed).',
        variant: 'destructive'
      });
      return;
    }

    setIsSubmitting(true);
    const res = await saveHabitProfile({
      name: newProfileName.trim(),
      description: `Custom habit profile created from ${projectName}`,
      building_type: 'Custom Construction',
      processes
    });
    setIsSubmitting(false);

    if (res.success && res.data) {
      toast({
        title: 'New Habit Profile Created!',
        description: `'${res.data.name}' has been saved to your company standards.`,
      });
      setSelectedProfileId(res.data.id);
      setNewProfileName('');
      loadData();
    }
  };

  const selectedProfileObj = habitProfiles.find(p => p.id === selectedProfileId);

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="outline" size="sm">
            <Settings2 className="mr-2 h-4 w-4 text-primary" /> Manage Scope & Profiles
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto p-4 sm:p-6">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold font-headline flex items-center gap-2">
            <Layers className="h-5 w-5 text-primary" /> Construction Habit Profiles & Quality Checklists
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground">
            Configure subheading processes, tasks, and site quality checklists for {projectName}.
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="py-12 flex justify-center items-center text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin mr-2" /> Loading Habit Profiles...
          </div>
        ) : (
          <div className="space-y-6 py-2">
            {/* Select Saved Habit Profile */}
            <div className="p-4 rounded-2xl bg-primary/5 border border-primary/20 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Label htmlFor="profile-select" className="text-xs font-bold text-foreground flex items-center gap-1.5">
                  <Copy className="h-4 w-4 text-primary" /> Apply a Saved Construction Habit Profile
                </Label>
                {selectedProfileObj && (
                  <Badge variant="secondary" className="text-xs bg-primary/10 text-primary font-semibold">
                    Active: {selectedProfileObj.name}
                  </Badge>
                )}
              </div>

              <Select value={selectedProfileId} onValueChange={handleSelectProfile}>
                <SelectTrigger id="profile-select" className="bg-background">
                  <SelectValue placeholder="Choose a construction habit profile template..." />
                </SelectTrigger>
                <SelectContent>
                  {habitProfiles.map(p => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name} {p.building_type ? `(${p.building_type})` : ''}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Subheading Processes & Tasks Editor */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-foreground">Subheading Processes & Task Checklists</h4>
                <Button size="sm" variant="outline" onClick={handleAddProcess}>
                  <Plus className="mr-1 h-3.5 w-3.5" /> Add Process Stage
                </Button>
              </div>

              {processes.map((proc, pIdx) => (
                <div key={proc.id} className="p-4 rounded-2xl border border-border/60 bg-muted/20 space-y-3">
                  <div className="flex items-center gap-3">
                    <Input
                      value={proc.title}
                      onChange={(e) => {
                        const updated = processes.map(p => p.id === proc.id ? { ...p, title: e.target.value } : p);
                        setProcesses(updated);
                      }}
                      placeholder="Process Stage Name (e.g. 1. Foundation)"
                      className="font-bold text-sm bg-background flex-1"
                    />
                    <div className="flex items-center gap-1 w-24 shrink-0">
                      <Input
                        type="number"
                        min={0}
                        max={100}
                        value={proc.weight}
                        onChange={(e) => {
                          const updated = processes.map(p => p.id === proc.id ? { ...p, weight: Number(e.target.value) } : p);
                          setProcesses(updated);
                        }}
                        className="text-xs bg-background"
                      />
                      <span className="text-xs font-bold text-muted-foreground">%</span>
                    </div>
                    <Button variant="ghost" size="icon" onClick={() => handleDeleteProcess(proc.id)} className="h-8 w-8 text-destructive">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>

                  {/* Tasks under this process */}
                  <div className="pl-4 space-y-3 border-l-2 border-primary/30">
                    <div className="flex items-center justify-between text-xs font-bold text-muted-foreground">
                      <span>Tasks & Quality Control Checklists ({proc.tasks?.length || 0})</span>
                      <Button variant="ghost" size="sm" onClick={() => handleAddTask(proc.id)} className="h-7 text-xs text-primary">
                        <Plus className="mr-1 h-3 w-3" /> Add Task
                      </Button>
                    </div>

                    {proc.tasks?.map((task) => (
                      <div key={task.id} className="p-3 rounded-xl bg-background border border-border/50 space-y-2">
                        <div className="flex items-center gap-2">
                          <Input
                            value={task.title}
                            onChange={(e) => {
                              const updated = processes.map(p => {
                                if (p.id !== proc.id) return p;
                                return {
                                  ...p,
                                  tasks: p.tasks.map(t => t.id === task.id ? { ...t, title: e.target.value } : t)
                                };
                              });
                              setProcesses(updated);
                            }}
                            placeholder="Task Name (e.g. PCC Bed Concreting)"
                            className="text-xs font-semibold flex-1"
                          />

                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleGenerateAIChecklist(proc.id, task.id, task.title)}
                            disabled={generatingChecklistTaskId === task.id}
                            className="h-8 text-[11px] bg-primary/10 text-primary border-primary/30 hover:bg-primary/20 shrink-0"
                          >
                            {generatingChecklistTaskId === task.id ? (
                              <Loader2 className="h-3 w-3 animate-spin mr-1" />
                            ) : (
                              <Sparkles className="h-3 w-3 mr-1" />
                            )}
                            AI Checklist
                          </Button>

                          <Button variant="ghost" size="icon" onClick={() => handleDeleteTask(proc.id, task.id)} className="h-7 w-7 text-destructive">
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>

                        {/* Quality Checklist Items */}
                        <div className="pl-3 space-y-1.5 pt-1">
                          <div className="flex items-center justify-between text-[11px] text-muted-foreground font-medium">
                            <span className="flex items-center gap-1">
                              <CheckSquare className="h-3 w-3 text-primary" /> Quality Checkpoints
                            </span>
                            <button
                              type="button"
                              onClick={() => handleAddChecklist(proc.id, task.id)}
                              className="text-primary hover:underline text-[11px]"
                            >
                              + Add Checkpoint
                            </button>
                          </div>

                          {task.checklists?.map((chk) => (
                            <div key={chk.id} className="flex items-center gap-2">
                              <Input
                                value={chk.title}
                                onChange={(e) => {
                                  const updated = processes.map(p => {
                                    if (p.id !== proc.id) return p;
                                    return {
                                      ...p,
                                      tasks: p.tasks.map(t => {
                                        if (t.id !== task.id) return t;
                                        return {
                                          ...t,
                                          checklists: t.checklists.map(c => c.id === chk.id ? { ...c, title: e.target.value } : c)
                                        };
                                      })
                                    };
                                  });
                                  setProcesses(updated);
                                }}
                                placeholder="Quality checkpoint detail..."
                                className="text-[11px] h-7 bg-muted/30"
                              />
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => {
                                  const updated = processes.map(p => {
                                    if (p.id !== proc.id) return p;
                                    return {
                                      ...p,
                                      tasks: p.tasks.map(t => {
                                        if (t.id !== task.id) return t;
                                        return {
                                          ...t,
                                          checklists: t.checklists.filter(c => c.id !== chk.id)
                                        };
                                      })
                                    };
                                  });
                                  setProcesses(updated);
                                }}
                                className="h-6 w-6 text-muted-foreground hover:text-destructive"
                              >
                                <Trash2 className="h-3 w-3" />
                              </Button>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            {/* Master Profile Sync Prompt */}
            {selectedProfileObj && (
              <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-start gap-2.5">
                <Checkbox
                  id="sync-master"
                  checked={syncToMaster}
                  onCheckedChange={(c) => setSyncToMaster(!!c)}
                  className="mt-0.5"
                />
                <label htmlFor="sync-master" className="text-xs font-semibold text-foreground cursor-pointer select-none">
                  Make changes in '{selectedProfileObj.name}' Profile too
                  <p className="text-[11px] font-normal text-muted-foreground mt-0.5">
                    Check this box to update your master Habit Profile so future projects automatically get these task & checklist improvements!
                  </p>
                </label>
              </div>
            )}

            {/* Save as New Habit Profile Section */}
            <div className="p-3.5 rounded-xl border border-border/50 bg-background space-y-2">
              <Label className="text-xs font-bold text-foreground">Save Setup as a New Habit Profile Template</Label>
              <div className="flex items-center gap-2">
                <Input
                  value={newProfileName}
                  onChange={(e) => setNewProfileName(e.target.value)}
                  placeholder="e.g. Residential Villa (Unframed Structure)"
                  className="text-xs flex-1"
                />
                <Button size="sm" variant="outline" onClick={handleSaveAsNewProfile} disabled={isSubmitting}>
                  <Save className="mr-1.5 h-3.5 w-3.5" /> Save Profile
                </Button>
              </div>
            </div>
          </div>
        )}

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={() => setIsOpen(false)}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Save & Apply Scope to Project
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
