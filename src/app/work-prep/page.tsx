'use client';

import { useState, useEffect } from 'react';
import { useSupabase } from '@/supabase/provider';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Checkbox } from '@/components/ui/checkbox';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Progress } from '@/components/ui/progress';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { createProjectTask } from '@/app/actions/tasks';
import {
  Phone,
  PhoneCall,
  CheckCircle2,
  Calendar,
  Users,
  Search,
  Building2,
  Filter,
  ArrowLeft,
  Check,
  X,
  Clock,
  Sparkles,
  AlertCircle,
  AlertTriangle,
  PhoneOff,
  UserCheck,
  Loader2,
  Package,
  Boxes,
  ArrowRight,
  ChevronRight,
  ClipboardList,
  FileCheck,
  Printer,
  Share2,
  Plus,
  Send,
  ShieldCheck,
  Zap,
  Trash2
} from 'lucide-react';
import { format, addDays } from 'date-fns';
import { useRouter, useSearchParams } from 'next/navigation';
import { getProjectScope, getBatchProjectScopes } from '@/app/actions/ai-progress';

type SuggestedTask = {
  projectId: string;
  projectName: string;
  taskTitle: string;
  processTitle: string;
  checklistsCount: number;
};

type TaskItem = {
  id: string;
  project_id: string | null;
  title: string;
  description: string | null;
  assigned_to: string | null;
  status: string;
  priority: string | null;
  due_date: string | null;
  is_upcoming: boolean;
  duration: string | null;
  created_by: string | null;
  created_at: string | null;
  project_name?: string;
  assigned_user?: {
    id: string;
    display_name: string;
    email: string;
    phone: string | null;
    photo_url: string | null;
  } | null;
};

type Contractor = {
  id: string;
  name: string;
  category: string | null;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
};

type Employee = {
  id: string;
  display_name: string | null;
  email: string | null;
  phone: string | null;
  photo_url: string | null;
  role: string;
};

type Project = {
  id: string;
  name: string;
  company_id: string;
  site_id?: string | null;
};

type MaterialItem = {
  id: string;
  name: string;
  category: string;
  current_stock: number;
  minimum_stock_level: number;
  unit_of_measurement: string;
  supplier_name: string;
  site_id: string;
  site_name?: string;
};

type MaterialRequirement = {
  materialId: string;
  quantity: number;
};

type CallStatus = 'pending' | 'called' | 'confirmed' | 'no-answer';

export default function WorkPrepPage() {
  const { supabase, user } = useSupabase();
  const searchParams = useSearchParams();
  const router = useRouter();
  const { toast } = useToast();

  const initialProjectId = searchParams.get('projectId') || 'all';

  // Date setup - Tomorrow
  const today = new Date();
  const tomorrow = addDays(today, 1);
  const tomorrowDateStr = format(tomorrow, 'yyyy-MM-dd');
  const formattedTomorrowHeader = format(tomorrow, 'EEEE, MMMM d, yyyy');

  // Wizard active step (1 to 4)
  const [currentStep, setCurrentStep] = useState<number>(1);

  // Data State
  const [selectedProjectId, setSelectedProjectId] = useState<string>(initialProjectId);
  const [projects, setProjects] = useState<Project[]>([]);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [contractors, setContractors] = useState<Contractor[]>([]);
  const [materials, setMaterials] = useState<MaterialItem[]>([]);
  const [suggestedTasks, setSuggestedTasks] = useState<SuggestedTask[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Step 1 State: Selected Tasks & Task-Level Assignments
  const [selectedTaskIds, setSelectedTaskIds] = useState<Set<string>>(new Set());
  // taskId -> contractorId
  const [taskContractors, setTaskContractors] = useState<Record<string, string>>({});
  // taskId -> MaterialRequirement[]
  const [taskMaterials, setTaskMaterials] = useState<Record<string, MaterialRequirement[]>>({});

  // Step 1 Modal: Quick task creation
  const [isAddTaskOpen, setIsAddTaskOpen] = useState(false);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskProjectId, setNewTaskProjectId] = useState('');
  const [newTaskPriority, setNewTaskPriority] = useState('medium');
  const [newTaskContractorId, setNewTaskContractorId] = useState<string>('none');
  const [newTaskSelectedMaterialId, setNewTaskSelectedMaterialId] = useState<string>('none');
  const [newTaskMaterialQty, setNewTaskMaterialQty] = useState<number>(10);
  const [newTaskMaterialReqs, setNewTaskMaterialReqs] = useState<MaterialRequirement[]>([]);
  const [isSubmittingTask, setIsSubmittingTask] = useState(false);

  // Step 2 State: Call status tracking
  const [callStatuses, setCallStatuses] = useState<Record<string, CallStatus>>({});
  const [searchQuery, setSearchQuery] = useState('');

  // Step 4 State: Final Dispatched Manifest
  const [isDispatched, setIsDispatched] = useState<boolean>(false);
  const [dispatchedAt, setDispatchedAt] = useState<string | null>(null);

  // Sync query param with state
  useEffect(() => {
    const projectIdParam = searchParams.get('projectId');
    if (projectIdParam) {
      setSelectedProjectId(projectIdParam);
    }
  }, [searchParams]);

  // Fetch initial metadata
  useEffect(() => {
    const fetchData = async () => {
      if (!user) return;
      setIsLoading(true);

      try {
        // 1. Fetch user profile
        const { data: userProfile, error: profileError } = await supabase
          .from('users')
          .select('company_id')
          .eq('id', user.id)
          .single();

        if (profileError || !userProfile?.company_id) {
          throw new Error('No company profile found.');
        }

        const companyId = userProfile.company_id;

        // 2. Fetch projects
        const { data: projectsData, error: projectsError } = await supabase
          .from('projects')
          .select('id, name, company_id, site_id')
          .eq('company_id', companyId);

        if (projectsError) throw projectsError;
        setProjects(projectsData || []);

        const projectIds = (projectsData || []).map(p => p.id);
        const siteIds = Array.from(new Set((projectsData || []).map(p => p.site_id).filter(Boolean))) as string[];

        if (projectsData && projectsData.length > 0) {
          setNewTaskProjectId(projectsData[0].id);
        }

        // 3. Fetch employees
        const { data: employeesData, error: employeesError } = await supabase
          .from('users')
          .select('id, display_name, email, phone, photo_url, role')
          .eq('company_id', companyId);

        if (employeesError) throw employeesError;
        setEmployees(employeesData || []);

        // 4. Fetch contractors
        const { data: contractorsData, error: contractorsError } = await supabase
          .from('contractors')
          .select('*')
          .eq('companyId', companyId)
          .order('name');

        if (contractorsError) throw contractorsError;

        const mappedContractors = (contractorsData || []).map(c => ({
          id: c.id,
          name: c.name,
          category: c.category,
          contactPerson: c.contactPerson || c.contact_person || null,
          phone: c.phone,
          email: c.email,
        }));
        setContractors(mappedContractors);

        // 5. Fetch materials
        if (siteIds.length > 0) {
          const { data: materialsData, error: materialsError } = await supabase
            .from('materials')
            .select('*, site:sites(name)')
            .in('site_id', siteIds)
            .order('name', { ascending: true });

          if (!materialsError && materialsData) {
            const mappedMaterials: MaterialItem[] = materialsData.map((m: any) => ({
              id: m.id,
              name: m.name,
              category: m.category || 'General',
              current_stock: Number(m.current_stock || 0),
              minimum_stock_level: Number(m.minimum_stock_level || 0),
              unit_of_measurement: m.unit_of_measurement || 'units',
              supplier_name: m.supplier_name || 'N/A',
              site_id: m.site_id,
              site_name: m.site?.name || 'Site',
            }));
            setMaterials(mappedMaterials);
          }
        }

        // 6. Fetch tasks
        if (projectIds.length > 0) {
          const { data: tasksData, error: tasksError } = await supabase
            .from('tasks')
            .select('*')
            .in('project_id', projectIds);

          if (tasksError) throw tasksError;

          const mappedTasks = (tasksData || []).map((t: any) => {
            const project = (projectsData || []).find(p => p.id === t.project_id);
            const assignee = (employeesData || []).find(e => e.id === t.assigned_to);

            return {
              ...t,
              project_name: project?.name || 'Unknown Project',
              assigned_user: assignee ? {
                id: assignee.id,
                display_name: assignee.display_name || 'Unassigned',
                email: assignee.email || '',
                phone: assignee.phone,
                photo_url: assignee.photo_url,
              } : null
            } as TaskItem;
          });

          setTasks(mappedTasks);

          // By default, select all tasks due tomorrow
          const tomorrowIds = mappedTasks
            .filter(t => isTaskTomorrow(t.due_date))
            .map(t => t.id);
          setSelectedTaskIds(new Set(tomorrowIds));

          // Auto-suggest next uncompleted task from each project's Building Plan (Fast Single-Query Batch)
          const scopeMap = await getBatchProjectScopes(projectIds);
          const suggestions: SuggestedTask[] = [];
          for (const p of projectsData || []) {
            const scope = scopeMap[p.id];
            if (scope && scope.processes) {
              for (const proc of scope.processes) {
                const nextTask = proc.tasks?.find(t => t.status !== 'completed');
                if (nextTask) {
                  const alreadyScheduled = mappedTasks.some(mt => mt.project_id === p.id && mt.title.toLowerCase() === nextTask.title.toLowerCase());
                  if (!alreadyScheduled) {
                    suggestions.push({
                      projectId: p.id,
                      projectName: p.name,
                      taskTitle: nextTask.title,
                      processTitle: proc.title,
                      checklistsCount: nextTask.checklists?.length || 0
                    });
                  }
                  break;
                }
              }
            }
          }
          setSuggestedTasks(suggestions);
        }
      } catch (err: any) {
        console.error('Error loading work preparation data:', err);
        toast({
          title: 'Error loading data',
          description: err.message || 'Failed to fetch resources.',
          variant: 'destructive',
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user, supabase, toast]);

  // Load call checklist statuses and dispatch state from localStorage
  useEffect(() => {
    const callKey = `work-prep-calls-${tomorrowDateStr}`;
    const storedCalls = localStorage.getItem(callKey);
    if (storedCalls) {
      try {
        setCallStatuses(JSON.parse(storedCalls));
      } catch (e) {
        console.error('Failed to parse call statuses', e);
      }
    }

    const dispatchKey = `work-prep-manifest-${tomorrowDateStr}`;
    const storedDispatch = localStorage.getItem(dispatchKey);
    if (storedDispatch) {
      try {
        const parsed = JSON.parse(storedDispatch);
        if (parsed.isDispatched) {
          setIsDispatched(true);
          setDispatchedAt(parsed.dispatchedAt);
        }
      } catch (e) {
        console.error('Failed to parse dispatch state', e);
      }
    }
  }, [tomorrowDateStr]);

  // Save call status change
  const handleStatusChange = (id: string, status: CallStatus) => {
    const newStatuses = { ...callStatuses, [id]: status };
    setCallStatuses(newStatuses);
    localStorage.setItem(`work-prep-calls-${tomorrowDateStr}`, JSON.stringify(newStatuses));

    const labels: Record<CallStatus, string> = {
      pending: 'Reset status.',
      called: 'Marked as Contacted.',
      confirmed: 'Marked as Confirmed Readiness!',
      'no-answer': 'Marked as No Answer.',
    };

    toast({
      title: 'Status Updated',
      description: labels[status],
    });
  };

  // Date checker helper (resilient string & selection checking)
  const isTaskTomorrow = (dueDateStr: string | null | undefined, taskId?: string) => {
    if (taskId && selectedTaskIds.has(taskId)) return true;
    if (!dueDateStr) return false;
    const cleanDueDate = String(dueDateStr).substring(0, 10);
    if (cleanDueDate === tomorrowDateStr) return true;

    try {
      const taskDate = new Date(dueDateStr);
      const tomorrowDate = new Date(tomorrowDateStr);
      return (
        taskDate.getFullYear() === tomorrowDate.getFullYear() &&
        taskDate.getMonth() === tomorrowDate.getMonth() &&
        taskDate.getDate() === tomorrowDate.getDate()
      );
    } catch (e) {
      return false;
    }
  };

  // Filter tasks scheduled for tomorrow based on selected project
  const tomorrowAllTasks = tasks.filter(task => {
    const isScheduledForTomorrow = isTaskTomorrow(task.due_date, task.id);
    const matchesProject = selectedProjectId === 'all' || task.project_id === selectedProjectId;
    return isScheduledForTomorrow && matchesProject;
  });

  // Selected tasks for the prep plan
  const selectedTasksList = tomorrowAllTasks.filter(t => selectedTaskIds.has(t.id));

  // Handle task selection toggle
  const toggleTaskSelection = (taskId: string) => {
    setSelectedTaskIds(prev => {
      const next = new Set(prev);
      if (next.has(taskId)) {
        next.delete(taskId);
      } else {
        next.add(taskId);
      }
      return next;
    });
  };

  // Handle contractor assignment for a task
  const handleAssignContractorToTask = (taskId: string, contractorId: string) => {
    setTaskContractors(prev => ({
      ...prev,
      [taskId]: contractorId,
    }));
  };

  // Handle material attachment for a task
  const handleAddMaterialToTask = (taskId: string, materialId: string, quantity: number) => {
    if (!materialId || materialId === 'none') return;
    setTaskMaterials(prev => {
      const existing = prev[taskId] || [];
      const index = existing.findIndex(m => m.materialId === materialId);
      let updated: MaterialRequirement[];
      if (index >= 0) {
        updated = [...existing];
        updated[index] = { materialId, quantity: updated[index].quantity + quantity };
      } else {
        updated = [...existing, { materialId, quantity }];
      }
      return { ...prev, [taskId]: updated };
    });
  };

  const handleRemoveMaterialFromTask = (taskId: string, materialId: string) => {
    setTaskMaterials(prev => {
      const existing = prev[taskId] || [];
      const updated = existing.filter(m => m.materialId !== materialId);
      return { ...prev, [taskId]: updated };
    });
  };

  const handleUpdateMaterialQtyInTask = (taskId: string, materialId: string, quantity: number) => {
    setTaskMaterials(prev => {
      const existing = prev[taskId] || [];
      const updated = existing.map(m =>
        m.materialId === materialId ? { ...m, quantity: Math.max(0, quantity) } : m
      );
      return { ...prev, [taskId]: updated };
    });
  };

  // Quick Task Creation Handler (with contractor & materials)
  const handleAddMaterialToNewTaskModal = () => {
    if (newTaskSelectedMaterialId === 'none') return;
    setNewTaskMaterialReqs(prev => {
      const idx = prev.findIndex(m => m.materialId === newTaskSelectedMaterialId);
      if (idx >= 0) {
        const copy = [...prev];
        copy[idx] = { materialId: newTaskSelectedMaterialId, quantity: copy[idx].quantity + newTaskMaterialQty };
        return copy;
      }
      return [...prev, { materialId: newTaskSelectedMaterialId, quantity: newTaskMaterialQty }];
    });
    setNewTaskSelectedMaterialId('none');
  };

  const handleCreateQuickTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTaskTitle.trim() || !newTaskProjectId) return;

    setIsSubmittingTask(true);
    const result = await createProjectTask({
      projectId: newTaskProjectId,
      title: newTaskTitle.trim(),
      dueDate: tomorrowDateStr,
      priority: newTaskPriority,
    });
    setIsSubmittingTask(false);

    if (result.success && result.task) {
      const createdTaskId = result.task.id;

      // Assign contractor if selected
      if (newTaskContractorId && newTaskContractorId !== 'none') {
        setTaskContractors(prev => ({ ...prev, [createdTaskId]: newTaskContractorId }));
      }

      // Assign materials if selected
      if (newTaskMaterialReqs.length > 0) {
        setTaskMaterials(prev => ({ ...prev, [createdTaskId]: newTaskMaterialReqs }));
      }

      toast({
        title: 'Task Created & Configured!',
        description: `"${newTaskTitle}" added to tomorrow's plan.`,
      });

      const selectedProj = projects.find(p => p.id === newTaskProjectId);
      const newTaskObj: TaskItem = {
        ...result.task,
        project_name: selectedProj?.name || 'Project',
        assigned_user: null,
      };

      setTasks(prev => [newTaskObj, ...prev]);
      setSelectedTaskIds(prev => new Set(prev).add(createdTaskId));

      // Reset modal state
      setNewTaskTitle('');
      setNewTaskContractorId('none');
      setNewTaskMaterialReqs([]);
      setIsAddTaskOpen(false);
    } else {
      toast({
        title: 'Error',
        description: result.error || 'Failed to create task.',
        variant: 'destructive',
      });
    }
  };

  const handleAddSuggestedTask = async (suggested: SuggestedTask) => {
    const result = await createProjectTask({
      projectId: suggested.projectId,
      title: suggested.taskTitle,
      dueDate: tomorrowDateStr,
      priority: 'high',
    });

    if (result.success && result.task) {
      const createdTaskId = result.task.id;
      const newTaskObj: TaskItem = {
        ...result.task,
        due_date: tomorrowDateStr,
        project_name: suggested.projectName,
        assigned_user: null,
      };

      setTasks(prev => [newTaskObj, ...prev]);
      setSelectedTaskIds(prev => new Set(prev).add(createdTaskId));
      setSuggestedTasks(prev => prev.filter(s => !(s.taskTitle === suggested.taskTitle && s.projectId === suggested.projectId)));

      toast({
        title: 'Suggested Task Added to Prep!',
        description: `"${suggested.taskTitle}" added to tomorrow's work plan.`,
      });
    } else {
      toast({
        title: 'Error',
        description: result.error || 'Failed to add suggested task.',
        variant: 'destructive',
      });
    }
  };

  // Step 2: Assigned Contractors & Employees for tomorrow's selected tasks
  const assignedContractorIdsForTomorrow = Array.from(new Set(
    selectedTasksList
      .map(t => taskContractors[t.id])
      .filter(Boolean)
  ));

  const assignedContractorsForTomorrow = contractors.filter(c => assignedContractorIdsForTomorrow.includes(c.id));
  const assignedEmployeesForTomorrow = employees.filter(emp => selectedTasksList.some(t => t.assigned_to === emp.id));

  const totalContactsToCall = [...assignedContractorsForTomorrow.map(c => c.id), ...assignedEmployeesForTomorrow.map(e => e.id)];
  const confirmedContactsCount = totalContactsToCall.filter(id => callStatuses[id] === 'confirmed').length;

  // Step 3: Material Requirements Calculation across selected tasks for tomorrow
  const requiredMaterialsAggregated: Record<string, number> = {};

  selectedTasksList.forEach(task => {
    const reqs = taskMaterials[task.id] || [];
    reqs.forEach(req => {
      requiredMaterialsAggregated[req.materialId] = (requiredMaterialsAggregated[req.materialId] || 0) + req.quantity;
    });
  });

  const materialsAuditList = materials.map(mat => {
    const requiredQty = requiredMaterialsAggregated[mat.id] || 0;
    const isRequired = requiredQty > 0;
    const isDeficit = isRequired && mat.current_stock < requiredQty;
    const deficitQty = Math.max(0, requiredQty - mat.current_stock);
    const isLowBuffer = isRequired && !isDeficit && mat.current_stock <= mat.minimum_stock_level;

    return {
      ...mat,
      requiredQty,
      isRequired,
      isDeficit,
      deficitQty,
      isLowBuffer,
    };
  }).filter(m => m.isRequired && (searchQuery === '' || m.name.toLowerCase().includes(searchQuery.toLowerCase())));

  const criticalMaterialCount = materialsAuditList.filter(m => m.isDeficit).length;

  // Overall Preparation Readiness Index Calculation
  const taskWeight = selectedTasksList.length > 0 ? 30 : 0;
  const contractorAssignedWeight = selectedTasksList.length > 0
    ? (selectedTasksList.filter(t => taskContractors[t.id]).length / selectedTasksList.length) * 20
    : 20;
  const contactConfirmationWeight = totalContactsToCall.length > 0
    ? (confirmedContactsCount / totalContactsToCall.length) * 30
    : 30;
  const materialDeficitDeduction = criticalMaterialCount * 15;
  const materialScore = Math.max(0, 20 - materialDeficitDeduction);

  const readinessIndex = Math.min(100, Math.round(taskWeight + contractorAssignedWeight + contactConfirmationWeight + materialScore));

  // Dispatch Handler
  const handleProcessPrep = () => {
    const nowStr = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    setIsDispatched(true);
    setDispatchedAt(nowStr);

    const dispatchData = {
      isDispatched: true,
      dispatchedAt: nowStr,
      readinessIndex,
      selectedTaskCount: selectedTasksList.length,
      confirmedContactsCount,
      totalContactsToCall: totalContactsToCall.length,
      criticalMaterialCount
    };

    localStorage.setItem(`work-prep-manifest-${tomorrowDateStr}`, JSON.stringify(dispatchData));

    toast({
      title: 'Work Prep Finalized & Dispatched!',
      description: `Tomorrow's site plan recorded with ${readinessIndex}% readiness index.`,
    });
  };

  // Copy Summary to Clipboard
  const handleCopySummary = () => {
    const tasksSummary = selectedTasksList.map((t, i) => {
      const contractorName = contractors.find(c => c.id === taskContractors[t.id])?.name || 'Unassigned Contractor';
      const matReqs = (taskMaterials[t.id] || []).map(r => {
        const mat = materials.find(m => m.id === r.materialId);
        return `${mat?.name || 'Material'}: ${r.quantity} ${mat?.unit_of_measurement || ''}`;
      }).join(', ');

      return `${i + 1}. ${t.title} (${t.project_name})\n   Contractor: ${contractorName}\n   Materials: ${matReqs || 'None specified'}`;
    }).join('\n\n');

    const summaryText = `*Tomorrow's Site Work Prep Plan (${formattedTomorrowHeader})*\n\n` +
      `*Readiness Score:* ${readinessIndex}%\n\n` +
      `*PLANNED WORK & CONTRACTORS (${selectedTasksList.length}):*\n${tasksSummary || 'No tasks selected'}\n\n` +
      `*WORKFORCE CONFIRMED:* ${confirmedContactsCount} / ${totalContactsToCall.length}\n` +
      `*MATERIAL DEFICITS:* ${criticalMaterialCount > 0 ? `⚠️ ${criticalMaterialCount} Material Shortages!` : '✅ Stock Sufficient'}\n\n` +
      `Generated via Constructor App.`;

    navigator.clipboard.writeText(summaryText);
    toast({
      title: 'Summary Copied to Clipboard!',
      description: 'Ready to share on WhatsApp or Team Hub.',
    });
  };

  // Print Sheet Handler
  const handlePrintManifest = () => {
    window.print();
  };

  // Helper badge renderer for contact status
  const renderStatusBadge = (id: string) => {
    const status = callStatuses[id] || 'pending';
    switch (status) {
      case 'confirmed':
        return (
          <Badge className="bg-emerald-500 hover:bg-emerald-600 text-white flex items-center gap-1 text-[10px] font-semibold">
            <UserCheck className="h-3 w-3" /> Confirmed
          </Badge>
        );
      case 'called':
        return (
          <Badge className="bg-blue-500 hover:bg-blue-600 text-white flex items-center gap-1 text-[10px] font-semibold">
            <Phone className="h-3 w-3" /> Contacted
          </Badge>
        );
      case 'no-answer':
        return (
          <Badge className="bg-red-500 hover:bg-red-600 text-white flex items-center gap-1 text-[10px] font-semibold">
            <PhoneOff className="h-3 w-3" /> No Answer
          </Badge>
        );
      default:
        return (
          <Badge variant="outline" className="text-muted-foreground flex items-center gap-1 text-[10px] font-medium border-white/10">
            <Clock className="h-3 w-3" /> Pending Call
          </Badge>
        );
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-transparent">
      {/* App Header Bar matching standard App Layout */}
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 md:px-6 shrink-0 sticky top-0 z-40 bg-background/80 backdrop-blur-md border-b border-white/5">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" className="h-9 w-9 shrink-0" onClick={() => router.back()}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-lg sm:text-2xl font-bold tracking-tight font-headline flex items-center gap-2">
              <PhoneCall className="h-5 w-5 sm:h-6 sm:w-6 text-primary shrink-0" />
              Work Preparation Wizard
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground mt-0.5">
              Tomorrow: <span className="font-semibold text-foreground">{formattedTomorrowHeader}</span>
            </p>
          </div>
        </div>

        {/* Project Switcher */}
        <div className="flex items-center gap-2 w-full sm:w-auto sm:max-w-xs sm:ml-auto">
          <Filter className="h-4 w-4 text-muted-foreground shrink-0" />
          <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
            <SelectTrigger className="w-full sm:w-[180px] bg-background/50 border-white/10 glass-card text-xs sm:text-sm">
              <SelectValue placeholder="All Projects" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Projects</SelectItem>
              {projects.map(project => (
                <SelectItem key={project.id} value={project.id}>
                  {project.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </header>

      {/* Main Content Body */}
      <main className="flex-1 p-3 sm:p-4 md:p-6 space-y-4 sm:space-y-6 bg-transparent max-w-7xl mx-auto w-full">
        {isLoading ? (
          <div className="flex flex-col justify-center items-center h-64 space-y-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-xs text-muted-foreground">Loading site schedule and resource data...</p>
          </div>
        ) : (
          <>

            {/* ========================================================================= */}
            {/* STEP 1: PLAN WORK & ASSIGN CONTRACTOR + MATERIALS PER TASK */}
            {/* ========================================================================= */}
            {currentStep === 1 && (
              <div className="space-y-4 sm:space-y-6">
                <Card className="glass-card">
                  <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
                    <div>
                      <CardTitle className="text-base sm:text-lg flex items-center gap-2">
                        <ClipboardList className="h-5 w-5 text-primary shrink-0" />
                        Step 1: Plan Work, Assign Contractors & Select Materials
                      </CardTitle>
                      <CardDescription className="text-xs sm:text-sm">
                        Select tomorrow's tasks, assign responsible contractors, and specify required materials.
                      </CardDescription>
                    </div>

                    {/* Add Task Dialog with Contractor & Material Pickers */}
                    <Dialog open={isAddTaskOpen} onOpenChange={setIsAddTaskOpen}>
                      <DialogTrigger asChild>
                        <Button size="sm" className="w-full sm:w-auto shrink-0 bg-primary hover:bg-primary/90">
                          <Plus className="h-4 w-4 mr-1.5" />
                          Add Task for Tomorrow
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="sm:max-w-[500px] w-[95vw] glass-card border-white/10 max-h-[90vh] overflow-y-auto">
                        <form onSubmit={handleCreateQuickTask}>
                          <DialogHeader>
                            <DialogTitle className="flex items-center gap-2">
                              <Plus className="h-5 w-5 text-primary" />
                              Add Task Scheduled for Tomorrow
                            </DialogTitle>
                            <DialogDescription className="text-xs sm:text-sm">
                              Create a new task and configure contractor & material requirements.
                            </DialogDescription>
                          </DialogHeader>
                          
                          <div className="grid gap-4 py-4">
                            {/* Task Title */}
                            <div className="grid gap-2">
                              <Label htmlFor="task-title">Task Title</Label>
                              <Input
                                id="task-title"
                                value={newTaskTitle}
                                onChange={(e) => setNewTaskTitle(e.target.value)}
                                placeholder="e.g. Brick Wall Plastering - Block A"
                                required
                                className="bg-background/50 border-white/10"
                              />
                            </div>

                            {/* Project & Priority */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                              <div className="grid gap-2">
                                <Label htmlFor="task-project">Project</Label>
                                <Select value={newTaskProjectId} onValueChange={setNewTaskProjectId}>
                                  <SelectTrigger id="task-project" className="bg-background/50 border-white/10">
                                    <SelectValue placeholder="Select project" />
                                  </SelectTrigger>
                                  <SelectContent>
                                    {projects.map(p => (
                                      <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                                    ))}
                                  </SelectContent>
                                </Select>
                              </div>

                              <div className="grid gap-2">
                                <Label htmlFor="task-priority">Priority</Label>
                                <Select value={newTaskPriority} onValueChange={setNewTaskPriority}>
                                  <SelectTrigger id="task-priority" className="bg-background/50 border-white/10">
                                    <SelectValue placeholder="Select priority" />
                                  </SelectTrigger>
                                  <SelectContent>
                                    <SelectItem value="low">Low Priority</SelectItem>
                                    <SelectItem value="medium">Medium Priority</SelectItem>
                                    <SelectItem value="high">High Priority</SelectItem>
                                  </SelectContent>
                                </Select>
                              </div>
                            </div>

                            {/* Contractor Selection */}
                            <div className="grid gap-2 pt-2 border-t border-white/5">
                              <Label htmlFor="task-contractor" className="flex items-center gap-1.5 text-xs font-semibold">
                                <Building2 className="h-3.5 w-3.5 text-primary" /> Assign Contractor
                              </Label>
                              <Select value={newTaskContractorId} onValueChange={setNewTaskContractorId}>
                                <SelectTrigger id="task-contractor" className="bg-background/50 border-white/10">
                                  <SelectValue placeholder="Select Contractor" />
                                </SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="none">No Contractor / In-House Team</SelectItem>
                                  {contractors.map(c => (
                                    <SelectItem key={c.id} value={c.id}>
                                      {c.name} {c.category ? `(${c.category})` : ''}
                                    </SelectItem>
                                  ))}
                                </SelectContent>
                              </Select>
                            </div>

                            {/* Material Requirement Selection */}
                            <div className="grid gap-2 pt-2 border-t border-white/5">
                              <Label className="flex items-center gap-1.5 text-xs font-semibold">
                                <Boxes className="h-3.5 w-3.5 text-primary" /> Required Materials
                              </Label>

                              <div className="flex gap-2">
                                <Select value={newTaskSelectedMaterialId} onValueChange={setNewTaskSelectedMaterialId}>
                                  <SelectTrigger className="flex-1 bg-background/50 border-white/10 text-xs">
                                    <SelectValue placeholder="Pick material" />
                                  </SelectTrigger>
                                  <SelectContent>
                                    <SelectItem value="none">Select material...</SelectItem>
                                    {materials.map(m => (
                                      <SelectItem key={m.id} value={m.id}>
                                        {m.name} ({m.unit_of_measurement})
                                      </SelectItem>
                                    ))}
                                  </SelectContent>
                                </Select>

                                <Input
                                  type="number"
                                  min="1"
                                  value={newTaskMaterialQty}
                                  onChange={(e) => setNewTaskMaterialQty(Math.max(1, Number(e.target.value)))}
                                  className="w-20 bg-background/50 border-white/10 text-xs"
                                  placeholder="Qty"
                                />

                                <Button type="button" variant="outline" size="sm" onClick={handleAddMaterialToNewTaskModal}>
                                  Add
                                </Button>
                              </div>

                              {newTaskMaterialReqs.length > 0 && (
                                <div className="space-y-1 mt-2">
                                  {newTaskMaterialReqs.map(r => {
                                    const mat = materials.find(m => m.id === r.materialId);
                                    return (
                                      <div key={r.materialId} className="flex items-center justify-between text-xs bg-white/5 p-2 rounded border border-white/5">
                                        <span>{mat?.name}</span>
                                        <div className="flex items-center gap-2">
                                          <span className="font-semibold text-primary">{r.quantity} {mat?.unit_of_measurement}</span>
                                          <button
                                            type="button"
                                            onClick={() => setNewTaskMaterialReqs(prev => prev.filter(m => m.materialId !== r.materialId))}
                                            className="text-red-400 hover:text-red-300"
                                          >
                                            <Trash2 className="h-3.5 w-3.5" />
                                          </button>
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              )}
                            </div>
                          </div>

                          <DialogFooter>
                            <Button type="submit" disabled={isSubmittingTask} className="w-full sm:w-auto">
                              {isSubmittingTask && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                              Create & Include Task
                            </Button>
                          </DialogFooter>
                        </form>
                      </DialogContent>
                    </Dialog>
                  </CardHeader>

                  <CardContent className="p-4 sm:p-6 space-y-4">
                    {/* Auto-Suggested Tasks Banner from Building Construction Plan */}
                    {suggestedTasks.length > 0 && (
                      <div className="p-4 rounded-2xl bg-gradient-to-r from-primary/15 via-primary/5 to-background border border-primary/30 space-y-3 shadow-md">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-primary flex items-center gap-1.5">
                          <Sparkles className="h-3.5 w-3.5 text-primary shrink-0" /> Suggestions
                        </h4>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          {suggestedTasks.map((s, idx) => (
                            <div key={idx} className="p-3 rounded-xl bg-background/80 border border-primary/20 space-y-2 flex flex-col justify-between">
                              <div>
                                <div className="flex items-center justify-between gap-2">
                                  <span className="text-[11px] font-bold text-primary">{s.projectName}</span>
                                  {s.checklistsCount > 0 && (
                                    <span className="text-[10px] text-muted-foreground">{s.checklistsCount} Quality Checks</span>
                                  )}
                                </div>
                                <h5 className="font-bold text-sm text-foreground mt-0.5">{s.taskTitle}</h5>
                                <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">{s.processTitle}</p>
                              </div>
                              <Button
                                size="sm"
                                onClick={() => handleAddSuggestedTask(s)}
                                className="w-full mt-2 bg-primary/15 hover:bg-primary/25 text-primary font-bold text-xs border border-primary/30 h-8.5 rounded-lg"
                              >
                                <Plus className="h-3.5 w-3.5 mr-1" /> Add to Tomorrow's Prep
                              </Button>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                    {tomorrowAllTasks.length === 0 ? (
                      <div className="flex flex-col items-center justify-center p-8 sm:p-12 text-center border-2 border-dashed border-white/10 rounded-2xl">
                        <CheckCircle2 className="h-10 w-10 sm:h-12 sm:w-12 text-emerald-500/80 mb-3" />
                        <h3 className="text-base font-bold text-foreground">No Tasks Scheduled for Tomorrow</h3>
                        <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-md">
                          Click "Add Task for Tomorrow" above to create and configure a task for tomorrow!
                        </p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="flex items-center justify-between text-xs text-muted-foreground pb-2 border-b border-white/5">
                          <span>Include Tasks in Tomorrow's Plan</span>
                          <span className="font-semibold text-foreground">{selectedTaskIds.size} of {tomorrowAllTasks.length} Selected</span>
                        </div>

                        {/* Task Cards with Inline Contractor & Material Assigners */}
                        <div className="space-y-4">
                          {tomorrowAllTasks.map(task => {
                            const isSelected = selectedTaskIds.has(task.id);
                            const currentContractorId = taskContractors[task.id] || 'none';
                            const currentMaterials = taskMaterials[task.id] || [];

                            return (
                              <div
                                key={task.id}
                                className={`p-4 rounded-xl border space-y-3 transition-all ${
                                  isSelected
                                    ? 'bg-primary/5 border-primary/50 shadow-sm'
                                    : 'bg-white/5 border-white/10 opacity-60'
                                }`}
                              >
                                {/* Task Title Header & Selection Checkbox */}
                                <div className="flex items-start gap-3">
                                  <Checkbox
                                    checked={isSelected}
                                    onCheckedChange={() => toggleTaskSelection(task.id)}
                                    className="mt-1 h-5 w-5 shrink-0"
                                  />
                                  <div className="flex-1 min-w-0">
                                    <div className="flex items-center justify-between gap-2">
                                      <p className="font-semibold text-sm sm:text-base text-foreground">{task.title}</p>
                                      <Badge
                                        variant={task.priority === 'high' ? 'destructive' : 'secondary'}
                                        className="capitalize text-[10px] shrink-0"
                                      >
                                        {task.priority || 'medium'}
                                      </Badge>
                                    </div>
                                    <p className="text-xs text-muted-foreground mt-0.5">{task.project_name}</p>
                                  </div>
                                </div>

                                {isSelected && (
                                  <div className="pt-3 border-t border-white/5 space-y-3 pl-8">
                                    {/* Contractor Picker Row */}
                                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                                      <span className="text-xs font-semibold flex items-center gap-1 text-foreground/80">
                                        <Building2 className="h-3.5 w-3.5 text-primary" /> Assigned Contractor:
                                      </span>
                                      <Select
                                        value={currentContractorId}
                                        onValueChange={(val) => handleAssignContractorToTask(task.id, val)}
                                      >
                                        <SelectTrigger className="w-full sm:w-[240px] h-8 text-xs bg-background/50 border-white/10">
                                          <SelectValue placeholder="Select contractor" />
                                        </SelectTrigger>
                                        <SelectContent>
                                          <SelectItem value="none">No Contractor / In-House</SelectItem>
                                          {contractors.map(c => (
                                            <SelectItem key={c.id} value={c.id}>
                                              {c.name} {c.category ? `(${c.category})` : ''}
                                            </SelectItem>
                                          ))}
                                        </SelectContent>
                                      </Select>
                                    </div>

                                    {/* Material Picker Row */}
                                    <div className="space-y-2">
                                      <div className="flex items-center justify-between">
                                        <span className="text-xs font-semibold flex items-center gap-1 text-foreground/80">
                                          <Boxes className="h-3.5 w-3.5 text-primary" /> Required Materials:
                                        </span>
                                      </div>

                                      {/* Attached Materials List with Editable Quantity */}
                                      {currentMaterials.length > 0 && (
                                        <div className="flex flex-wrap gap-2 pt-1">
                                          {currentMaterials.map(req => {
                                            const mat = materials.find(m => m.id === req.materialId);
                                            return (
                                              <div key={req.materialId} className="inline-flex items-center gap-1.5 py-1 px-2.5 rounded-xl bg-background/80 border border-primary/30 text-xs shadow-sm">
                                                <span className="font-semibold text-foreground">{mat?.name}:</span>
                                                <Input
                                                  type="number"
                                                  min="0"
                                                  step="any"
                                                  value={req.quantity || ''}
                                                  onChange={(e) => handleUpdateMaterialQtyInTask(task.id, req.materialId, parseFloat(e.target.value) || 0)}
                                                  className="w-16 h-7 text-center font-bold text-primary bg-background border-primary/40 focus:border-primary text-xs px-1 py-0 rounded-md"
                                                />
                                                <span className="text-muted-foreground font-medium text-[11px]">{mat?.unit_of_measurement}</span>
                                                <button
                                                  onClick={() => handleRemoveMaterialFromTask(task.id, req.materialId)}
                                                  className="text-muted-foreground hover:text-red-400 ml-1 p-0.5"
                                                  title="Remove material"
                                                >
                                                  <X className="h-3.5 w-3.5" />
                                                </button>
                                              </div>
                                            );
                                          })}
                                        </div>
                                      )}

                                      {/* Quick Add Material to Task */}
                                      <div className="flex gap-2 max-w-md pt-1">
                                        <Select onValueChange={(val) => handleAddMaterialToTask(task.id, val, 10)}>
                                          <SelectTrigger className="h-8 text-xs bg-background/50 border-white/10 flex-1">
                                            <SelectValue placeholder="+ Select material to add..." />
                                          </SelectTrigger>
                                          <SelectContent>
                                            {materials.map(m => (
                                              <SelectItem key={m.id} value={m.id}>
                                                {m.name} ({m.unit_of_measurement})
                                              </SelectItem>
                                            ))}
                                          </SelectContent>
                                        </Select>
                                      </div>
                                    </div>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    <div className="flex justify-end pt-4 sm:pt-6 mt-4 sm:mt-6 border-t border-white/5">
                      <Button onClick={() => setCurrentStep(2)} disabled={selectedTasksList.length === 0} className="w-full sm:w-auto gap-2">
                        Step 2: Contact Workforce & Contractors
                        <ArrowRight className="h-4 w-4" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 2: WORKFORCE & CONTRACTOR CALL LIST */}
            {/* ========================================================================= */}
            {currentStep === 2 && (
              <div className="space-y-4 sm:space-y-6">
                <Card className="glass-card">
                  <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-4">
                    <div>
                      <CardTitle className="text-base sm:text-lg flex items-center gap-2">
                        <Users className="h-5 w-5 text-primary shrink-0" />
                        Step 2: Confirm Assigned Workforce & Contractors
                      </CardTitle>
                      <CardDescription className="text-xs sm:text-sm">
                        Call contractors and team members assigned to tomorrow's selected tasks.
                      </CardDescription>
                    </div>
                  </CardHeader>

                  <CardContent className="p-4 sm:p-6 space-y-4">
                    {totalContactsToCall.length === 0 ? (
                      <div className="flex flex-col items-center justify-center p-8 text-center border-2 border-dashed border-white/10 rounded-xl">
                        <Users className="h-10 w-10 text-muted-foreground/50 mb-2" />
                        <p className="font-semibold text-foreground text-sm sm:text-base">No Assigned Contacts Found</p>
                        <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-xs">
                          Assign contractors to tomorrow's tasks in Step 1 to generate your call checklist.
                        </p>
                      </div>
                    ) : (
                      <div className="divide-y divide-white/5">
                        {/* Assigned Contractors */}
                        {assignedContractorsForTomorrow.map(c => {
                          const status = callStatuses[c.id] || 'pending';
                          const assignedTaskTitles = selectedTasksList
                            .filter(t => taskContractors[t.id] === c.id)
                            .map(t => t.title);

                          return (
                            <div key={c.id} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                              <div className="flex items-start gap-3">
                                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary border border-primary/20 shrink-0 mt-0.5">
                                  <Building2 className="h-4 w-4" />
                                </div>
                                <div className="min-w-0">
                                  <div className="flex items-center gap-2">
                                    <p className="font-semibold text-xs sm:text-sm text-foreground truncate">{c.name}</p>
                                    <Badge className="text-[9px] font-semibold uppercase bg-white/10 text-muted-foreground">
                                      Contractor
                                    </Badge>
                                  </div>
                                  <p className="text-xs text-primary font-medium mt-0.5 truncate">
                                    Task: {assignedTaskTitles.join(', ') || 'Assigned Work'}
                                  </p>
                                </div>
                              </div>

                              <div className="flex items-center justify-between sm:justify-end gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-white/5">
                                {renderStatusBadge(c.id)}

                                <div className="flex items-center gap-2">
                                  <Select
                                    value={status}
                                    onValueChange={(val) => handleStatusChange(c.id, val as CallStatus)}
                                  >
                                    <SelectTrigger className="w-[120px] h-8 text-xs bg-background/50 border-white/10">
                                      <SelectValue placeholder="Set Status" />
                                    </SelectTrigger>
                                    <SelectContent align="end">
                                      <SelectItem value="pending">Reset</SelectItem>
                                      <SelectItem value="called">Mark Called</SelectItem>
                                      <SelectItem value="confirmed">Confirmed</SelectItem>
                                      <SelectItem value="no-answer">No Answer</SelectItem>
                                    </SelectContent>
                                  </Select>

                                  {c.phone ? (
                                    <Button size="sm" className="h-8 gap-1 text-xs bg-emerald-600 hover:bg-emerald-700 text-white" asChild>
                                      <a href={`tel:${c.phone}`}>
                                        <Phone className="h-3.5 w-3.5" /> Call
                                      </a>
                                    </Button>
                                  ) : (
                                    <Button size="sm" variant="outline" className="h-8 text-xs opacity-40" disabled>
                                      No Phone
                                    </Button>
                                  )}
                                </div>
                              </div>
                            </div>
                          );
                        })}

                        {/* Assigned Employees */}
                        {assignedEmployeesForTomorrow.map(emp => {
                          const status = callStatuses[emp.id] || 'pending';
                          return (
                            <div key={emp.id} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                              <div className="flex items-center gap-3">
                                <Avatar className="h-9 w-9 border border-white/10 shrink-0">
                                  <AvatarImage src={emp.photo_url || undefined} />
                                  <AvatarFallback>{(emp.display_name || 'U').charAt(0)}</AvatarFallback>
                                </Avatar>
                                <div className="min-w-0">
                                  <p className="font-semibold text-xs sm:text-sm text-foreground truncate">{emp.display_name}</p>
                                  <p className="text-[11px] text-muted-foreground uppercase font-mono">{emp.role}</p>
                                </div>
                              </div>

                              <div className="flex items-center justify-between sm:justify-end gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-white/5">
                                {renderStatusBadge(emp.id)}

                                <div className="flex items-center gap-2">
                                  <Select
                                    value={status}
                                    onValueChange={(val) => handleStatusChange(emp.id, val as CallStatus)}
                                  >
                                    <SelectTrigger className="w-[120px] h-8 text-xs bg-background/50 border-white/10">
                                      <SelectValue placeholder="Set Status" />
                                    </SelectTrigger>
                                    <SelectContent align="end">
                                      <SelectItem value="pending">Reset</SelectItem>
                                      <SelectItem value="called">Mark Called</SelectItem>
                                      <SelectItem value="confirmed">Confirmed</SelectItem>
                                      <SelectItem value="no-answer">No Answer</SelectItem>
                                    </SelectContent>
                                  </Select>

                                  {emp.phone ? (
                                    <Button size="sm" className="h-8 gap-1 text-xs bg-emerald-600 hover:bg-emerald-700 text-white" asChild>
                                      <a href={`tel:${emp.phone}`}>
                                        <Phone className="h-3.5 w-3.5" /> Call
                                      </a>
                                    </Button>
                                  ) : (
                                    <Button size="sm" variant="outline" className="h-8 text-xs opacity-40" disabled>
                                      No Phone
                                    </Button>
                                  )}
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    <div className="flex flex-col sm:flex-row justify-between gap-3 pt-4 border-t border-white/5">
                      <Button variant="outline" onClick={() => setCurrentStep(1)} className="w-full sm:w-auto">
                        <ArrowLeft className="h-4 w-4 mr-2" /> Back to Step 1
                      </Button>
                      <Button onClick={() => setCurrentStep(3)} className="w-full sm:w-auto gap-2">
                        Step 3: Check Required Materials Stock
                        <ArrowRight className="h-4 w-4" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 3: REQUIRED MATERIALS STOCK AUDIT */}
            {/* ========================================================================= */}
            {currentStep === 3 && (
              <div className="space-y-4 sm:space-y-6">
                {criticalMaterialCount > 0 && (
                  <Alert variant="destructive" className="bg-red-500/10 border-red-500/40 text-red-400">
                    <AlertTriangle className="h-4 w-4 text-red-500 shrink-0" />
                    <AlertTitle className="font-bold text-xs sm:text-sm">Stock Deficit Warning!</AlertTitle>
                    <AlertDescription className="text-[11px] sm:text-xs mt-1">
                      {criticalMaterialCount} required material(s) have stock deficits for tomorrow's planned work items.
                    </AlertDescription>
                  </Alert>
                )}

                <Card className="glass-card">
                  <CardHeader className="border-b border-white/5 pb-4">
                    <CardTitle className="text-base sm:text-lg flex items-center gap-2">
                      <Boxes className="h-5 w-5 text-primary shrink-0" />
                      Step 3: Verify Required Materials Availability
                    </CardTitle>
                    <CardDescription className="text-xs sm:text-sm">
                      Compare total required quantities for tomorrow's tasks against current site stock.
                    </CardDescription>
                  </CardHeader>

                  <CardContent className="p-4 sm:p-6 space-y-4">
                    {materialsAuditList.length === 0 ? (
                      <div className="flex flex-col items-center justify-center p-8 text-center border-2 border-dashed border-white/10 rounded-xl">
                        <Package className="h-10 w-10 text-muted-foreground/50 mb-2" />
                        <p className="font-semibold text-foreground text-sm sm:text-base">No Required Materials Specified</p>
                        <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-xs">
                          Attach required materials to tasks in Step 1 to run an automated stock audit.
                        </p>
                      </div>
                    ) : (
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
                        {materialsAuditList.map(material => {
                          return (
                            <div
                              key={material.id}
                              className={`p-4 rounded-xl border space-y-3 ${
                                material.isDeficit
                                  ? 'bg-red-500/10 border-red-500/40 shadow-sm'
                                  : material.isLowBuffer
                                  ? 'bg-amber-500/10 border-amber-500/40 shadow-sm'
                                  : 'bg-primary/5 border-primary/30 shadow-sm'
                              }`}
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div className="min-w-0">
                                  <p className="font-semibold text-xs sm:text-sm text-foreground truncate">{material.name}</p>
                                  <p className="text-[11px] text-muted-foreground capitalize truncate">{material.category}</p>
                                </div>
                                {material.isDeficit ? (
                                  <Badge variant="destructive" className="text-[9px] font-bold shrink-0">
                                    Shortage
                                  </Badge>
                                ) : material.isLowBuffer ? (
                                  <Badge className="bg-amber-500 text-white text-[9px] font-bold shrink-0">
                                    Low Stock
                                  </Badge>
                                ) : (
                                  <Badge className="bg-emerald-500 text-white text-[9px] font-bold shrink-0">
                                    In Stock
                                  </Badge>
                                )}
                              </div>

                              <div className="space-y-1.5 text-xs pt-1 border-t border-white/5">
                                <div className="flex justify-between text-muted-foreground">
                                  <span>Required Qty:</span>
                                  <span className="font-bold text-foreground">{material.requiredQty} {material.unit_of_measurement}</span>
                                </div>
                                <div className="flex justify-between text-muted-foreground">
                                  <span>Current Stock:</span>
                                  <span className="font-bold text-foreground">{material.current_stock} {material.unit_of_measurement}</span>
                                </div>
                                {material.isDeficit && (
                                  <div className="flex justify-between text-red-400 font-semibold pt-1 border-t border-white/5">
                                    <span>Stock Deficit:</span>
                                    <span>Short by {material.deficitQty} {material.unit_of_measurement}</span>
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    <div className="flex flex-col sm:flex-row justify-between gap-3 pt-4 sm:pt-6 mt-4 border-t border-white/5">
                      <Button variant="outline" onClick={() => setCurrentStep(2)} className="w-full sm:w-auto">
                        <ArrowLeft className="h-4 w-4 mr-2" /> Back to Step 2
                      </Button>
                      <Button onClick={() => setCurrentStep(4)} className="w-full sm:w-auto gap-2">
                        Step 4: Finalize & Dispatch Prep
                        <ArrowRight className="h-4 w-4" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 4: FINALIZE & DISPATCH WORK PREP */}
            {/* ========================================================================= */}
            {currentStep === 4 && (
              <div className="space-y-4 sm:space-y-6">
                <Card className="glass-card border-primary/30 bg-gradient-to-r from-primary/10 via-transparent to-transparent">
                  <CardContent className="p-4 sm:p-6">
                    <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 sm:gap-6">
                      <div className="space-y-2 flex-1">
                        <div className="flex items-center gap-2">
                          <Badge className="bg-primary text-primary-foreground font-bold text-xs">
                            Tomorrow's Prep Manifest
                          </Badge>
                          <span className="text-xs text-muted-foreground">{formattedTomorrowHeader}</span>
                        </div>
                        <h2 className="text-lg sm:text-xl font-bold text-foreground">Tomorrow's Site Readiness Index</h2>

                        <div className="space-y-1.5 pt-2 max-w-md">
                          <div className="flex justify-between text-xs font-semibold">
                            <span>Readiness Score</span>
                            <span className={readinessIndex >= 80 ? 'text-emerald-400' : readinessIndex >= 50 ? 'text-amber-400' : 'text-red-400'}>
                              {readinessIndex}% Ready
                            </span>
                          </div>
                          <Progress value={readinessIndex} className="h-2.5" />
                        </div>
                      </div>

                      <div className="flex flex-col sm:flex-row gap-2.5 shrink-0 w-full lg:w-auto">
                        <Button variant="outline" onClick={handleCopySummary} className="gap-2 text-xs sm:text-sm w-full sm:w-auto">
                          <Share2 className="h-4 w-4" /> Share Summary
                        </Button>
                        <Button variant="outline" onClick={handlePrintManifest} className="gap-2 text-xs sm:text-sm w-full sm:w-auto">
                          <Printer className="h-4 w-4" /> Print Sheet
                        </Button>
                        <Button onClick={handleProcessPrep} disabled={isDispatched} className="gap-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs sm:text-sm w-full sm:w-auto">
                          <Send className="h-4 w-4" />
                          {isDispatched ? 'Prep Dispatched!' : 'Process & Dispatch Prep'}
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {isDispatched && (
                  <Alert className="bg-emerald-500/10 border-emerald-500/30 text-emerald-400">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
                    <AlertTitle className="font-bold text-xs sm:text-sm">Prep Plan Finalized & Dispatched</AlertTitle>
                    <AlertDescription className="text-[11px] sm:text-xs mt-1">
                      Saved at {dispatchedAt || 'today'} for tomorrow's site team.
                    </AlertDescription>
                  </Alert>
                )}

                {/* Summary Manifest Breakdown */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6">
                  {/* Confirmed Tasks & Contractors */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5 p-4 sm:p-6">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <ClipboardList className="h-4 w-4 text-primary shrink-0" /> Tasks & Contractors
                        </span>
                        <Badge variant="outline">{selectedTasksList.length}</Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-3 sm:p-4">
                      <ScrollArea className="h-[220px] sm:h-[250px]">
                        <div className="space-y-2">
                          {selectedTasksList.map((t, idx) => {
                            const contractor = contractors.find(c => c.id === taskContractors[t.id]);
                            return (
                              <div key={t.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 space-y-1 text-xs">
                                <p className="font-semibold text-foreground">{idx + 1}. {t.title}</p>
                                <p className="text-[11px] text-primary">Contractor: {contractor?.name || 'In-House / Unassigned'}</p>
                              </div>
                            );
                          })}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>

                  {/* Confirmed Workforce Calls */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5 p-4 sm:p-6">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <Users className="h-4 w-4 text-primary shrink-0" /> Confirmed Workforce
                        </span>
                        <Badge variant="outline">{confirmedContactsCount} / {totalContactsToCall.length}</Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-3 sm:p-4">
                      <ScrollArea className="h-[220px] sm:h-[250px]">
                        <div className="space-y-2">
                          {assignedContractorsForTomorrow.map(c => (
                            <div key={c.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div className="min-w-0 pr-2">
                                <p className="font-semibold text-foreground truncate">{c.name}</p>
                                <p className="text-[11px] text-muted-foreground">Contractor</p>
                              </div>
                              {renderStatusBadge(c.id)}
                            </div>
                          ))}
                          {assignedEmployeesForTomorrow.map(e => (
                            <div key={e.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div className="min-w-0 pr-2">
                                <p className="font-semibold text-foreground truncate">{e.display_name}</p>
                                <p className="text-[11px] text-muted-foreground">{e.role}</p>
                              </div>
                              {renderStatusBadge(e.id)}
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>

                  {/* Material Deficit Audit */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5 p-4 sm:p-6">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <Boxes className="h-4 w-4 text-primary shrink-0" /> Required Materials
                        </span>
                        <Badge variant={criticalMaterialCount > 0 ? "destructive" : "outline"}>
                          {criticalMaterialCount > 0 ? `${criticalMaterialCount} Deficits` : 'OK'}
                        </Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-3 sm:p-4">
                      <ScrollArea className="h-[220px] sm:h-[250px]">
                        <div className="space-y-2">
                          {materialsAuditList.map(m => (
                            <div key={m.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div className="min-w-0 pr-2">
                                <p className="font-semibold text-foreground truncate">{m.name}</p>
                                <p className="text-[11px] text-muted-foreground">Req: {m.requiredQty} | Stock: {m.current_stock} {m.unit_of_measurement}</p>
                              </div>
                              {m.isDeficit ? (
                                <Badge variant="destructive" className="text-[10px] shrink-0">Short</Badge>
                              ) : (
                                <Badge className="bg-emerald-500 text-white text-[10px] shrink-0">OK</Badge>
                              )}
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>
                </div>

                <div className="flex justify-between pt-2 sm:pt-4">
                  <Button variant="outline" onClick={() => setCurrentStep(3)} className="w-full sm:w-auto">
                    <ArrowLeft className="h-4 w-4 mr-2" /> Back to Step 3
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
