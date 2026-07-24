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
  HelpCircle,
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
  RefreshCw,
  Info
} from 'lucide-react';
import { format, addDays } from 'date-fns';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';

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
  isRequiredForTomorrow?: boolean;
  matchedTaskTitle?: string;
  allocatedQty?: number;
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
  const [isLoading, setIsLoading] = useState(true);

  // Step 1 State: Selected Tasks for tomorrow's prep
  const [selectedTaskIds, setSelectedTaskIds] = useState<Set<string>>(new Set());

  // Step 1 Modal: Quick task creation
  const [isAddTaskOpen, setIsAddTaskOpen] = useState(false);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskProjectId, setNewTaskProjectId] = useState('');
  const [newTaskPriority, setNewTaskPriority] = useState('medium');
  const [isSubmittingTask, setIsSubmittingTask] = useState(false);

  // Step 2 State: Call status tracking
  const [callStatuses, setCallStatuses] = useState<Record<string, CallStatus>>({});
  const [searchQuery, setSearchQuery] = useState('');

  // Step 3 State: Planned Material Allocations (materialId -> quantity)
  const [materialAllocations, setMaterialAllocations] = useState<Record<string, number>>({});

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

        // Default new task project if available
        if (projectsData && projectsData.length > 0) {
          setNewTaskProjectId(projectsData[0].id);
        }

        // 3. Fetch employees (users in the same company)
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

        // 5. Fetch materials for company sites
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

        // 6. Fetch tasks for company projects
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

  // Save call checklist status change
  const handleStatusChange = (id: string, status: CallStatus) => {
    const newStatuses = { ...callStatuses, [id]: status };
    setCallStatuses(newStatuses);
    localStorage.setItem(`work-prep-calls-${tomorrowDateStr}`, JSON.stringify(newStatuses));

    const labels: Record<CallStatus, string> = {
      pending: 'Cleared call log status.',
      called: 'Marked as Contacted.',
      confirmed: 'Marked as Confirmed!',
      'no-answer': 'Marked as No Answer.',
    };

    toast({
      title: 'Call Status Updated',
      description: labels[status],
    });
  };

  // Date checker helper
  const isTaskTomorrow = (dueDateStr: string | null | undefined) => {
    if (!dueDateStr) return false;
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
    const isScheduledForTomorrow = isTaskTomorrow(task.due_date);
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

  // Handle quick task creation for tomorrow
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
      toast({
        title: 'Task Created for Tomorrow',
        description: `"${newTaskTitle}" has been added to tomorrow's schedule.`,
      });

      const selectedProj = projects.find(p => p.id === newTaskProjectId);
      const newTaskObj: TaskItem = {
        ...result.task,
        project_name: selectedProj?.name || 'Project',
        assigned_user: null,
      };

      setTasks(prev => [newTaskObj, ...prev]);
      setSelectedTaskIds(prev => new Set(prev).add(newTaskObj.id));

      setNewTaskTitle('');
      setIsAddTaskOpen(false);
    } else {
      toast({
        title: 'Error',
        description: result.error || 'Failed to create task.',
        variant: 'destructive',
      });
    }
  };

  // Employees assigned to selected tasks
  const selectedAssignedEmployees = employees.filter(emp => {
    const matchesTask = selectedTasksList.some(t => t.assigned_to === emp.id);
    const matchesSearch = searchQuery.trim() === '' ||
      (emp.display_name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (emp.role || '').toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTask && matchesSearch;
  });

  // Recommendation Keyword Mappings for Contractors
  const categoryKeywords: Record<string, string[]> = {
    'mason': ['mason', 'masonry', 'brick', 'cement', 'concrete', 'plaster', 'block', 'slab', 'wall', 'mortar', 'stone', 'structural'],
    'carpenter': ['carpenter', 'carpentry', 'wood', 'door', 'window', 'frame', 'cabinet', 'plywood', 'furniture', 'interior'],
    'electrician': ['electrician', 'electrical', 'wire', 'wiring', 'cable', 'switch', 'light', 'panel', 'breaker', 'power', 'conduit'],
    'plumber': ['plumber', 'plumbing', 'pipe', 'leak', 'water', 'drain', 'tap', 'sink', 'toilet', 'faucet', 'sanitary'],
    'painter': ['painter', 'painting', 'paint', 'coat', 'brush', 'roller', 'wallcoat', 'primer', 'polish'],
    'tiler': ['tiler', 'tile', 'tiling', 'floor', 'flooring', 'marble', 'granite', 'grout'],
    'supervisor': ['supervisor', 'supervise', 'manage', 'inspect', 'audit', 'site manager'],
    'helper': ['helper', 'coolie', 'laborer', 'shifting', 'clean', 'carrying', 'unloading', 'site clean']
  };

  const getTaskMatchingExplanation = (contractor: Contractor) => {
    const category = (contractor.category || '').toLowerCase();
    const name = contractor.name.toLowerCase();

    for (const task of selectedTasksList) {
      const taskText = `${task.title} ${task.description || ''}`.toLowerCase();

      if (taskText.includes(name)) {
        return `Matches task: "${task.title}"`;
      }

      for (const [groupKey, keywords] of Object.entries(categoryKeywords)) {
        if (category.includes(groupKey) || groupKey.includes(category)) {
          const matchedKeyword = keywords.find(keyword => taskText.includes(keyword));
          if (matchedKeyword) {
            return `Category matches "${matchedKeyword}" in task: "${task.title}"`;
          }
        }
      }
    }
    return null;
  };

  const prioritizedContractors = contractors
    .map(c => {
      const matchExplanation = getTaskMatchingExplanation(c);
      return {
        ...c,
        isRecommended: !!matchExplanation,
        matchExplanation
      };
    })
    .filter(c => {
      const matchesSearch = searchQuery.trim() === '' ||
        c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (c.category || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (c.contactPerson || '').toLowerCase().includes(searchQuery.toLowerCase());
      return matchesSearch;
    })
    .sort((a, b) => {
      if (a.isRecommended && !b.isRecommended) return -1;
      if (!a.isRecommended && b.isRecommended) return 1;
      return a.name.localeCompare(b.name);
    });

  // Material keyword matching for Step 3
  const materialKeywords: Record<string, string[]> = {
    'cement': ['cement', 'concrete', 'slab', 'plaster', 'mortar', 'masonry', 'foundation', 'brick'],
    'sand': ['sand', 'mortar', 'plaster', 'concrete', 'flooring'],
    'brick': ['brick', 'wall', 'masonry', 'block', 'partition'],
    'paint': ['paint', 'painting', 'primer', 'coat', 'wallcoat'],
    'tile': ['tile', 'tiling', 'flooring', 'bathroom', 'kitchen'],
    'wire': ['wire', 'wiring', 'electrical', 'cable', 'conduit', 'switch'],
    'pipe': ['pipe', 'plumbing', 'drain', 'water', 'sanitary'],
    'wood': ['wood', 'carpentry', 'door', 'window', 'plywood', 'frame']
  };

  // Materials relevant to selected tasks
  const relevantMaterials = materials.map(mat => {
    const matName = mat.name.toLowerCase();
    const matCat = mat.category.toLowerCase();
    
    let matchedTaskTitle: string | undefined = undefined;

    for (const task of selectedTasksList) {
      const taskText = `${task.title} ${task.description || ''}`.toLowerCase();
      
      if (taskText.includes(matName) || taskText.includes(matCat)) {
        matchedTaskTitle = task.title;
        break;
      }

      for (const [key, words] of Object.entries(materialKeywords)) {
        if (matName.includes(key) || matCat.includes(key)) {
          const matchedWord = words.find(w => taskText.includes(w));
          if (matchedWord) {
            matchedTaskTitle = task.title;
            break;
          }
        }
      }
      if (matchedTaskTitle) break;
    }

    return {
      ...mat,
      isRequiredForTomorrow: !!matchedTaskTitle,
      matchedTaskTitle,
      allocatedQty: materialAllocations[mat.id] || 0
    };
  }).sort((a, b) => {
    if (a.isRequiredForTomorrow && !b.isRequiredForTomorrow) return -1;
    if (!a.isRequiredForTomorrow && b.isRequiredForTomorrow) return 1;
    return a.name.localeCompare(b.name);
  });

  // Materials stock issues count
  const criticalMaterialCount = relevantMaterials.filter(m => m.isRequiredForTomorrow && m.current_stock === 0).length;
  const lowMaterialCount = relevantMaterials.filter(m => m.isRequiredForTomorrow && m.current_stock > 0 && m.current_stock <= m.minimum_stock_level).length;

  // Contacts calculation
  const contactsToCall = [
    ...selectedAssignedEmployees.map(e => e.id),
    ...prioritizedContractors.filter(c => c.isRecommended).map(c => c.id)
  ];
  const confirmedContactsCount = contactsToCall.filter(id => callStatuses[id] === 'confirmed').length;

  // Overall Preparation Readiness Percentage Calculation
  const taskWeight = selectedTasksList.length > 0 ? 35 : 0;
  const contactWeight = contactsToCall.length > 0 ? (confirmedContactsCount / contactsToCall.length) * 45 : 45;
  const materialDeduction = (criticalMaterialCount * 15) + (lowMaterialCount * 5);
  const materialScore = Math.max(0, 20 - materialDeduction);
  
  const readinessIndex = Math.min(100, Math.round(taskWeight + contactWeight + materialScore));

  // Process & Dispatch Prep Plan
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
      totalContactsToCall: contactsToCall.length,
      criticalMaterialCount
    };

    localStorage.setItem(`work-prep-manifest-${tomorrowDateStr}`, JSON.stringify(dispatchData));

    toast({
      title: 'Work Prep Processed Successfully!',
      description: `Tomorrow's prep plan is finalized with ${readinessIndex}% readiness index.`,
    });
  };

  // Copy Summary for WhatsApp / Chat
  const handleCopySummary = () => {
    const tasksSummary = selectedTasksList.map((t, i) => `${i + 1}. ${t.title} (${t.project_name})`).join('\n');
    const teamSummary = selectedAssignedEmployees.map(e => `• ${e.display_name} (${e.role}) - ${callStatuses[e.id] === 'confirmed' ? 'Confirmed' : 'Pending'}`).join('\n');
    const contractorSummary = prioritizedContractors.filter(c => c.isRecommended).map(c => `• ${c.name} (${c.category}) - ${callStatuses[c.id] === 'confirmed' ? 'Confirmed' : 'Pending'}`).join('\n');

    const summaryText = `*Tomorrow's Site Work Prep Plan (${formattedTomorrowHeader})*\n\n` +
      `*Readiness Score:* ${readinessIndex}%\n\n` +
      `*PLANNED WORK (${selectedTasksList.length}):*\n${tasksSummary || 'No tasks selected'}\n\n` +
      `*CONFIRMED TEAM (${selectedAssignedEmployees.length}):*\n${teamSummary || 'None'}\n\n` +
      `*CONTRACTORS (${prioritizedContractors.filter(c => c.isRecommended).length}):*\n${contractorSummary || 'None'}\n\n` +
      `*MATERIALS STATUS:* ${criticalMaterialCount > 0 ? `⚠️ ${criticalMaterialCount} Out of Stock items!` : '✅ Materials Checked'}\n\n` +
      `Generated via Constructor App.`;

    navigator.clipboard.writeText(summaryText);
    toast({
      title: 'Summary Copied to Clipboard!',
      description: 'Ready to paste on WhatsApp or Team Hub.',
    });
  };

  // Print manifest
  const handlePrintManifest = () => {
    window.print();
  };

  // Render status badge for call planner
  const renderStatusBadge = (id: string) => {
    const status = callStatuses[id] || 'pending';
    switch (status) {
      case 'confirmed':
        return (
          <Badge className="bg-emerald-500 hover:bg-emerald-600 text-white border-transparent flex items-center gap-1">
            <UserCheck className="h-3 w-3" /> Confirmed
          </Badge>
        );
      case 'called':
        return (
          <Badge className="bg-blue-500 hover:bg-blue-600 text-white border-transparent flex items-center gap-1">
            <Phone className="h-3 w-3" /> Contacted
          </Badge>
        );
      case 'no-answer':
        return (
          <Badge className="bg-red-500 hover:bg-red-600 text-white border-transparent flex items-center gap-1">
            <PhoneOff className="h-3 w-3" /> No Answer
          </Badge>
        );
      default:
        return (
          <Badge variant="outline" className="text-muted-foreground flex items-center gap-1">
            <Clock className="h-3 w-3" /> Pending Call
          </Badge>
        );
    }
  };

  return (
    <div className="flex flex-col h-full bg-transparent">
      {/* Top Header */}
      <header className="flex flex-col md:flex-row md:items-center gap-4 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-40">
        <div className="flex items-center gap-4 flex-1">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline flex items-center gap-2">
              <PhoneCall className="h-6 w-6 text-primary" />
              Work Preparation Wizard
            </h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Tomorrow's Plan: <span className="font-semibold text-foreground">{formattedTomorrowHeader}</span>
            </p>
          </div>
        </div>

        {/* Project Filter */}
        <div className="flex items-center gap-2 max-w-xs ml-auto">
          <Filter className="h-4 w-4 text-muted-foreground shrink-0" />
          <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
            <SelectTrigger className="w-[180px] bg-background/50 border-white/10 glass-card">
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

      {/* Main Content */}
      <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6 bg-transparent">
        {isLoading ? (
          <div className="flex justify-center items-center h-64">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : (
          <>
            {/* 4-Step Progress Stepper Navigation Header */}
            <Card className="glass-card overflow-hidden">
              <CardContent className="p-4 sm:p-6">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-2 sm:gap-4 relative">
                  {/* Step 1 */}
                  <button
                    onClick={() => setCurrentStep(1)}
                    className={`flex items-center gap-3 p-3 rounded-xl border text-left transition-all ${
                      currentStep === 1
                        ? 'bg-primary/10 border-primary text-primary shadow-sm ring-1 ring-primary/30'
                        : selectedTasksList.length > 0
                        ? 'bg-white/5 border-emerald-500/30 text-foreground'
                        : 'bg-white/5 border-white/10 text-muted-foreground hover:bg-white/10'
                    }`}
                  >
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                      currentStep === 1
                        ? 'bg-primary text-primary-foreground'
                        : selectedTasksList.length > 0
                        ? 'bg-emerald-500 text-white'
                        : 'bg-muted text-muted-foreground'
                    }`}>
                      {selectedTasksList.length > 0 && currentStep !== 1 ? <Check className="h-4 w-4" /> : '1'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold uppercase tracking-wider line-clamp-1">Step 1</p>
                      <p className="text-sm font-semibold truncate">Plan Work</p>
                    </div>
                  </button>

                  {/* Step 2 */}
                  <button
                    onClick={() => setCurrentStep(2)}
                    className={`flex items-center gap-3 p-3 rounded-xl border text-left transition-all ${
                      currentStep === 2
                        ? 'bg-primary/10 border-primary text-primary shadow-sm ring-1 ring-primary/30'
                        : confirmedContactsCount > 0
                        ? 'bg-white/5 border-emerald-500/30 text-foreground'
                        : 'bg-white/5 border-white/10 text-muted-foreground hover:bg-white/10'
                    }`}
                  >
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                      currentStep === 2
                        ? 'bg-primary text-primary-foreground'
                        : confirmedContactsCount > 0
                        ? 'bg-emerald-500 text-white'
                        : 'bg-muted text-muted-foreground'
                    }`}>
                      {confirmedContactsCount > 0 && currentStep !== 2 ? <Check className="h-4 w-4" /> : '2'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold uppercase tracking-wider line-clamp-1">Step 2</p>
                      <p className="text-sm font-semibold truncate">Contractors & Team</p>
                    </div>
                  </button>

                  {/* Step 3 */}
                  <button
                    onClick={() => setCurrentStep(3)}
                    className={`flex items-center gap-3 p-3 rounded-xl border text-left transition-all ${
                      currentStep === 3
                        ? 'bg-primary/10 border-primary text-primary shadow-sm ring-1 ring-primary/30'
                        : criticalMaterialCount === 0
                        ? 'bg-white/5 border-emerald-500/30 text-foreground'
                        : 'bg-white/5 border-amber-500/30 text-amber-500 hover:bg-white/10'
                    }`}
                  >
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                      currentStep === 3
                        ? 'bg-primary text-primary-foreground'
                        : criticalMaterialCount === 0
                        ? 'bg-emerald-500 text-white'
                        : 'bg-amber-500 text-white'
                    }`}>
                      {currentStep !== 3 && criticalMaterialCount === 0 ? <Check className="h-4 w-4" /> : '3'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold uppercase tracking-wider line-clamp-1">Step 3</p>
                      <p className="text-sm font-semibold truncate">Check Materials</p>
                    </div>
                  </button>

                  {/* Step 4 */}
                  <button
                    onClick={() => setCurrentStep(4)}
                    className={`flex items-center gap-3 p-3 rounded-xl border text-left transition-all ${
                      currentStep === 4
                        ? 'bg-primary/10 border-primary text-primary shadow-sm ring-1 ring-primary/30'
                        : isDispatched
                        ? 'bg-emerald-500/10 border-emerald-500 text-emerald-500'
                        : 'bg-white/5 border-white/10 text-muted-foreground hover:bg-white/10'
                    }`}
                  >
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                      currentStep === 4
                        ? 'bg-primary text-primary-foreground'
                        : isDispatched
                        ? 'bg-emerald-500 text-white'
                        : 'bg-muted text-muted-foreground'
                    }`}>
                      {isDispatched ? <Check className="h-4 w-4" /> : '4'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold uppercase tracking-wider line-clamp-1">Step 4</p>
                      <p className="text-sm font-semibold truncate">Process Prep</p>
                    </div>
                  </button>
                </div>
              </CardContent>
            </Card>

            {/* ========================================================================= */}
            {/* STEP 1: PLAN THE WORK */}
            {/* ========================================================================= */}
            {currentStep === 1 && (
              <div className="space-y-6">
                <Card className="glass-card">
                  <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-4">
                    <div>
                      <CardTitle className="text-lg flex items-center gap-2">
                        <ClipboardList className="h-5 w-5 text-primary" />
                        Step 1: Plan Tomorrow's Work Schedule
                      </CardTitle>
                      <CardDescription>
                        Select tasks & milestones for tomorrow, or add new work items to the plan.
                      </CardDescription>
                    </div>

                    <Dialog open={isAddTaskOpen} onOpenChange={setIsAddTaskOpen}>
                      <DialogTrigger asChild>
                        <Button size="sm">
                          <Plus className="h-4 w-4 mr-1.5" />
                          Add Task for Tomorrow
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="sm:max-w-[425px]">
                        <form onSubmit={handleCreateQuickTask}>
                          <DialogHeader>
                            <DialogTitle>Add Task Scheduled for Tomorrow</DialogTitle>
                            <DialogDescription>
                              Quickly add a task to tomorrow's preparation plan ({formattedTomorrowHeader}).
                            </DialogDescription>
                          </DialogHeader>
                          <div className="grid gap-4 py-4">
                            <div className="grid gap-2">
                              <Label htmlFor="task-title">Task Title</Label>
                              <Input
                                id="task-title"
                                value={newTaskTitle}
                                onChange={(e) => setNewTaskTitle(e.target.value)}
                                placeholder="e.g. Pour concrete for column 4"
                                required
                              />
                            </div>
                            <div className="grid gap-2">
                              <Label htmlFor="task-project">Project</Label>
                              <Select value={newTaskProjectId} onValueChange={setNewTaskProjectId}>
                                <SelectTrigger id="task-project">
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
                                <SelectTrigger id="task-priority">
                                  <SelectValue placeholder="Select priority" />
                                </SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="low">Low</SelectItem>
                                  <SelectItem value="medium">Medium</SelectItem>
                                  <SelectItem value="high">High</SelectItem>
                                </SelectContent>
                              </Select>
                            </div>
                          </div>
                          <DialogFooter>
                            <Button type="submit" disabled={isSubmittingTask}>
                              {isSubmittingTask && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                              Create & Add to Plan
                            </Button>
                          </DialogFooter>
                        </form>
                      </DialogContent>
                    </Dialog>
                  </CardHeader>
                  <CardContent className="p-6">
                    {tomorrowAllTasks.length === 0 ? (
                      <div className="flex flex-col items-center justify-center p-12 text-center">
                        <CheckCircle2 className="h-12 w-12 text-emerald-500/80 mb-3" />
                        <h3 className="text-base font-bold text-foreground">No Tasks Scheduled for Tomorrow</h3>
                        <p className="text-sm text-muted-foreground mt-1 max-w-md">
                          There are no tasks or milestones scheduled for tomorrow yet. Click "Add Task for Tomorrow" above to create one now!
                        </p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="flex items-center justify-between text-xs text-muted-foreground pb-2 border-b border-white/5">
                          <span>Include in Tomorrow's Work Prep</span>
                          <span>{selectedTaskIds.size} of {tomorrowAllTasks.length} Selected</span>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {tomorrowAllTasks.map(task => {
                            const isSelected = selectedTaskIds.has(task.id);
                            return (
                              <div
                                key={task.id}
                                onClick={() => toggleTaskSelection(task.id)}
                                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                                  isSelected
                                    ? 'bg-primary/10 border-primary shadow-sm'
                                    : 'bg-white/5 border-white/10 opacity-60 hover:opacity-100'
                                }`}
                              >
                                <div className="flex items-start gap-3">
                                  <Checkbox
                                    checked={isSelected}
                                    onCheckedChange={() => toggleTaskSelection(task.id)}
                                    className="mt-1"
                                  />
                                  <div className="flex-1 space-y-1">
                                    <div className="flex items-center justify-between gap-2">
                                      <p className="font-semibold text-sm text-foreground">{task.title}</p>
                                      <Badge
                                        variant={task.priority === 'high' ? 'destructive' : 'secondary'}
                                        className="capitalize text-[10px]"
                                      >
                                        {task.priority || 'medium'}
                                      </Badge>
                                    </div>
                                    {task.description && (
                                      <p className="text-xs text-muted-foreground line-clamp-2">{task.description}</p>
                                    )}
                                    <div className="flex items-center justify-between text-[11px] text-muted-foreground pt-2">
                                      <span className="flex items-center gap-1 font-medium text-foreground/70">
                                        <Building2 className="h-3 w-3" /> {task.project_name}
                                      </span>
                                      {task.assigned_user ? (
                                        <span className="text-primary font-semibold">
                                          Assignee: {task.assigned_user.display_name}
                                        </span>
                                      ) : (
                                        <span className="text-amber-500 font-medium flex items-center gap-0.5">
                                          <AlertCircle className="h-3 w-3" /> Unassigned
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    <div className="flex justify-end pt-6 mt-6 border-t border-white/5">
                      <Button onClick={() => setCurrentStep(2)} disabled={selectedTasksList.length === 0}>
                        Step 2: Choose Contractors & Team
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 2: CHOOSE CONTRACTORS & TEAM */}
            {/* ========================================================================= */}
            {currentStep === 2 && (
              <div className="space-y-6">
                <Card className="glass-card">
                  <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-4">
                    <div>
                      <CardTitle className="text-lg flex items-center gap-2">
                        <Users className="h-5 w-5 text-primary" />
                        Step 2: Assign & Confirm Contractors / Team
                      </CardTitle>
                      <CardDescription>
                        Call and confirm readiness of team members & recommended contractors for tomorrow's work.
                      </CardDescription>
                    </div>

                    <div className="relative w-full sm:w-[220px]">
                      <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                      <Input
                        type="search"
                        placeholder="Search contacts..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-8 h-8 text-xs bg-background/50 border-white/10 rounded-md"
                      />
                    </div>
                  </CardHeader>
                  <CardContent className="p-0">
                    <Tabs defaultValue="team" className="w-full">
                      <TabsList className="w-full justify-start rounded-none border-b border-white/5 bg-transparent p-0 h-10">
                        <TabsTrigger
                          value="team"
                          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent px-6 text-sm font-medium"
                        >
                          Team Members ({selectedAssignedEmployees.length})
                        </TabsTrigger>
                        <TabsTrigger
                          value="contractors"
                          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent px-6 text-sm font-medium"
                        >
                          Contractors ({prioritizedContractors.length})
                        </TabsTrigger>
                      </TabsList>

                      {/* Employees Tab */}
                      <TabsContent value="team" className="p-0 m-0">
                        <ScrollArea className="h-[450px]">
                          {selectedAssignedEmployees.length === 0 ? (
                            <div className="flex flex-col items-center justify-center p-8 text-center h-[300px]">
                              <Users className="h-10 w-10 text-muted-foreground/50 mb-2" />
                              <p className="font-semibold text-foreground/90">No Team Members Assigned</p>
                              <p className="text-sm text-muted-foreground mt-1 max-w-xs">
                                No employees are currently assigned to tomorrow's selected tasks.
                              </p>
                            </div>
                          ) : (
                            <div className="divide-y divide-white/5">
                              {selectedAssignedEmployees.map(emp => {
                                const status = callStatuses[emp.id] || 'pending';
                                return (
                                  <div key={emp.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-white/5 transition-colors">
                                    <div className="flex items-center gap-3">
                                      <Avatar className="h-10 w-10 border border-white/10 shrink-0">
                                        <AvatarImage src={emp.photo_url || undefined} alt={emp.display_name || ''} />
                                        <AvatarFallback>{(emp.display_name || 'U').charAt(0)}</AvatarFallback>
                                      </Avatar>
                                      <div>
                                        <div className="flex items-center gap-2">
                                          <p className="font-semibold text-sm text-foreground">{emp.display_name}</p>
                                          <Badge variant="outline" className="text-[10px] font-semibold uppercase">
                                            {emp.role}
                                          </Badge>
                                        </div>
                                        <p className="text-xs text-muted-foreground mt-0.5">{emp.email}</p>
                                      </div>
                                    </div>

                                    <div className="flex items-center gap-3 shrink-0 ml-auto sm:ml-0">
                                      {renderStatusBadge(emp.id)}
                                      <Select
                                        value={status}
                                        onValueChange={(val) => handleStatusChange(emp.id, val as CallStatus)}
                                      >
                                        <SelectTrigger className="w-[120px] h-8 text-xs border-white/10 bg-background/50">
                                          <SelectValue placeholder="Set Status" />
                                        </SelectTrigger>
                                        <SelectContent align="end">
                                          <SelectItem value="pending">Reset</SelectItem>
                                          <SelectItem value="called">Mark Called</SelectItem>
                                          <SelectItem value="confirmed">Confirm Readiness</SelectItem>
                                          <SelectItem value="no-answer">No Answer</SelectItem>
                                        </SelectContent>
                                      </Select>

                                      {emp.phone ? (
                                        <Button size="sm" className="h-8 gap-1.5" asChild>
                                          <a href={`tel:${emp.phone}`}>
                                            <Phone className="h-3.5 w-3.5" /> Call
                                          </a>
                                        </Button>
                                      ) : (
                                        <Button size="sm" variant="outline" className="h-8 opacity-40 cursor-not-allowed" disabled>
                                          No Phone
                                        </Button>
                                      )}
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </ScrollArea>
                      </TabsContent>

                      {/* Contractors Tab */}
                      <TabsContent value="contractors" className="p-0 m-0">
                        <ScrollArea className="h-[450px]">
                          {prioritizedContractors.length === 0 ? (
                            <div className="flex flex-col items-center justify-center p-8 text-center h-[300px]">
                              <Building2 className="h-10 w-10 text-muted-foreground/50 mb-2" />
                              <p className="font-semibold text-foreground/90">No Contractors Found</p>
                              <p className="text-sm text-muted-foreground mt-1 max-w-xs">
                                No registered contractors match your query.
                              </p>
                            </div>
                          ) : (
                            <div className="divide-y divide-white/5">
                              {prioritizedContractors.map(c => {
                                const status = callStatuses[c.id] || 'pending';
                                return (
                                  <div
                                    key={c.id}
                                    className={`p-4 flex flex-col gap-3 hover:bg-white/5 transition-colors ${
                                      c.isRecommended ? 'bg-primary/5 border-l-2 border-l-primary' : ''
                                    }`}
                                  >
                                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                                      <div className="flex items-start gap-3">
                                        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary border border-primary/20 shrink-0">
                                          <Building2 className="h-4 w-4" />
                                        </div>
                                        <div>
                                          <div className="flex flex-wrap items-center gap-2">
                                            <p className="font-semibold text-sm text-foreground">{c.name}</p>
                                            {c.category && (
                                              <Badge className="text-[10px] font-semibold bg-white/10 text-muted-foreground uppercase">
                                                {c.category}
                                              </Badge>
                                            )}
                                            {c.isRecommended && (
                                              <Badge className="text-[10px] font-bold bg-primary text-primary-foreground flex items-center gap-0.5">
                                                <Sparkles className="h-2.5 w-2.5" /> Recommended
                                              </Badge>
                                            )}
                                          </div>
                                          {c.contactPerson && (
                                            <p className="text-xs text-muted-foreground mt-0.5">Contact: {c.contactPerson}</p>
                                          )}
                                        </div>
                                      </div>

                                      <div className="flex items-center gap-3 shrink-0 ml-auto sm:ml-0">
                                        {renderStatusBadge(c.id)}
                                        <Select
                                          value={status}
                                          onValueChange={(val) => handleStatusChange(c.id, val as CallStatus)}
                                        >
                                          <SelectTrigger className="w-[120px] h-8 text-xs border-white/10 bg-background/50">
                                            <SelectValue placeholder="Set Status" />
                                          </SelectTrigger>
                                          <SelectContent align="end">
                                            <SelectItem value="pending">Reset</SelectItem>
                                            <SelectItem value="called">Mark Called</SelectItem>
                                            <SelectItem value="confirmed">Confirm Availability</SelectItem>
                                            <SelectItem value="no-answer">No Answer</SelectItem>
                                          </SelectContent>
                                        </Select>

                                        {c.phone ? (
                                          <Button size="sm" className="h-8 gap-1.5" asChild>
                                            <a href={`tel:${c.phone}`}>
                                              <Phone className="h-3.5 w-3.5" /> Call
                                            </a>
                                          </Button>
                                        ) : (
                                          <Button size="sm" variant="outline" className="h-8 opacity-40 cursor-not-allowed" disabled>
                                            No Phone
                                          </Button>
                                        )}
                                      </div>
                                    </div>

                                    {c.isRecommended && c.matchExplanation && (
                                      <div className="text-[11px] bg-primary/10 text-primary px-3 py-1.5 rounded-md border border-primary/10 flex items-center gap-1.5">
                                        <Sparkles className="h-3 w-3 shrink-0" />
                                        <span>{c.matchExplanation}</span>
                                      </div>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </ScrollArea>
                      </TabsContent>
                    </Tabs>

                    <div className="flex justify-between p-6 border-t border-white/5">
                      <Button variant="outline" onClick={() => setCurrentStep(1)}>
                        <ArrowLeft className="h-4 w-4 mr-2" /> Back to Step 1
                      </Button>
                      <Button onClick={() => setCurrentStep(3)}>
                        Step 3: Check Materials Availability
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 3: CHECK MATERIALS AVAILABILITY */}
            {/* ========================================================================= */}
            {currentStep === 3 && (
              <div className="space-y-6">
                {criticalMaterialCount > 0 && (
                  <Alert variant="destructive" className="bg-red-500/10 border-red-500/40 text-red-400">
                    <AlertTriangle className="h-4 w-4 text-red-500" />
                    <AlertTitle className="font-bold">Stockout Alert for Tomorrow's Work!</AlertTitle>
                    <AlertDescription className="text-xs mt-1">
                      {criticalMaterialCount} material(s) required for tomorrow's work currently have 0 units in stock! Please order or restock them before starting site work.
                    </AlertDescription>
                  </Alert>
                )}

                <Card className="glass-card">
                  <CardHeader className="border-b border-white/5 pb-4">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Boxes className="h-5 w-5 text-primary" />
                      Step 3: Verify Material Availability & Inventory Stock
                    </CardTitle>
                    <CardDescription>
                      Check stock levels for materials required by tomorrow's planned work items.
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="p-6 space-y-6">
                    {relevantMaterials.length === 0 ? (
                      <div className="flex flex-col items-center justify-center p-12 text-center">
                        <Package className="h-12 w-12 text-muted-foreground/50 mb-3" />
                        <h3 className="text-base font-bold text-foreground">No Materials Listed in Site Inventory</h3>
                        <p className="text-sm text-muted-foreground mt-1 max-w-md">
                          Add materials to your project inventory to enable automatic stock checks.
                        </p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                          {relevantMaterials.map(material => {
                            const isStockout = material.current_stock === 0;
                            const isLowStock = material.current_stock > 0 && material.current_stock <= material.minimum_stock_level;

                            return (
                              <div
                                key={material.id}
                                className={`p-4 rounded-xl border space-y-3 ${
                                  material.isRequiredForTomorrow
                                    ? isStockout
                                      ? 'bg-red-500/10 border-red-500/40'
                                      : isLowStock
                                      ? 'bg-amber-500/10 border-amber-500/40'
                                      : 'bg-primary/5 border-primary/30'
                                    : 'bg-white/5 border-white/10 opacity-70'
                                }`}
                              >
                                <div className="flex items-start justify-between gap-2">
                                  <div>
                                    <p className="font-semibold text-sm text-foreground">{material.name}</p>
                                    <p className="text-xs text-muted-foreground capitalize">{material.category} • {material.site_name}</p>
                                  </div>
                                  {isStockout ? (
                                    <Badge variant="destructive" className="text-[10px] font-bold">
                                      Out of Stock
                                    </Badge>
                                  ) : isLowStock ? (
                                    <Badge className="bg-amber-500 text-white text-[10px] font-bold">
                                      Low Stock
                                    </Badge>
                                  ) : (
                                    <Badge className="bg-emerald-500 text-white text-[10px] font-bold">
                                      In Stock
                                    </Badge>
                                  )}
                                </div>

                                {material.isRequiredForTomorrow && (
                                  <div className="text-[11px] bg-white/5 p-2 rounded border border-white/5 text-primary flex items-center gap-1.5">
                                    <Sparkles className="h-3 w-3 shrink-0" />
                                    <span className="truncate">Matches: {material.matchedTaskTitle}</span>
                                  </div>
                                )}

                                <div className="flex items-center justify-between text-xs pt-1 border-t border-white/5">
                                  <span className="text-muted-foreground">Current Stock:</span>
                                  <span className="font-bold text-foreground text-sm">
                                    {material.current_stock} {material.unit_of_measurement}
                                  </span>
                                </div>

                                <div className="flex items-center justify-between text-[11px] text-muted-foreground">
                                  <span>Min Threshold:</span>
                                  <span>{material.minimum_stock_level} {material.unit_of_measurement}</span>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    <div className="flex justify-between pt-6 mt-6 border-t border-white/5">
                      <Button variant="outline" onClick={() => setCurrentStep(2)}>
                        <ArrowLeft className="h-4 w-4 mr-2" /> Back to Step 2
                      </Button>
                      <Button onClick={() => setCurrentStep(4)}>
                        Step 4: Finalize & Process Work Prep
                        <ArrowRight className="h-4 w-4 ml-2" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}

            {/* ========================================================================= */}
            {/* STEP 4: PROCESS & FINALIZE WORK PREP */}
            {/* ========================================================================= */}
            {currentStep === 4 && (
              <div className="space-y-6">
                {/* Readiness Score Card */}
                <Card className="glass-card overflow-hidden border-primary/30 bg-gradient-to-r from-primary/10 via-transparent to-transparent">
                  <CardContent className="p-6">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                      <div className="space-y-2 flex-1">
                        <div className="flex items-center gap-2">
                          <Badge className="bg-primary text-primary-foreground font-bold">
                            Work Prep Summary Manifest
                          </Badge>
                          <span className="text-xs text-muted-foreground font-medium">{formattedTomorrowHeader}</span>
                        </div>
                        <h2 className="text-xl font-bold text-foreground">Tomorrow's Readiness Index</h2>
                        <p className="text-sm text-muted-foreground max-w-lg">
                          Comprehensive evaluation based on confirmed tasks, worker readiness, and material stock levels.
                        </p>

                        <div className="space-y-1.5 pt-2">
                          <div className="flex justify-between text-xs font-semibold">
                            <span>Site Preparation Score</span>
                            <span className={readinessIndex >= 80 ? 'text-emerald-500' : readinessIndex >= 50 ? 'text-amber-500' : 'text-red-500'}>
                              {readinessIndex}% Ready
                            </span>
                          </div>
                          <Progress value={readinessIndex} className="h-2.5" />
                        </div>
                      </div>

                      <div className="flex flex-col sm:flex-row gap-3 shrink-0">
                        <Button variant="outline" onClick={handleCopySummary} className="gap-2">
                          <Share2 className="h-4 w-4" /> Share Summary
                        </Button>
                        <Button variant="outline" onClick={handlePrintManifest} className="gap-2">
                          <Printer className="h-4 w-4" /> Print Sheet
                        </Button>
                        <Button onClick={handleProcessPrep} disabled={isDispatched} className="gap-2 bg-emerald-600 hover:bg-emerald-700 text-white">
                          <Send className="h-4 w-4" />
                          {isDispatched ? 'Prep Processed!' : 'Process & Dispatch Prep'}
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {isDispatched && (
                  <Alert className="bg-emerald-500/10 border-emerald-500/30 text-emerald-400">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                    <AlertTitle className="font-bold">Work Prep Finalized & Dispatched</AlertTitle>
                    <AlertDescription className="text-xs mt-1">
                      This prep manifest was processed at {dispatchedAt || 'today'} and saved for tomorrow's site operations.
                    </AlertDescription>
                  </Alert>
                )}

                {/* Summary Breakdown Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  {/* Confirmed Tasks */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <ClipboardList className="h-4 w-4 text-primary" /> Confirmed Work Items
                        </span>
                        <Badge variant="outline">{selectedTasksList.length}</Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-4">
                      <ScrollArea className="h-[250px]">
                        <div className="space-y-2">
                          {selectedTasksList.map((t, idx) => (
                            <div key={t.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 space-y-1 text-xs">
                              <p className="font-semibold text-foreground">{idx + 1}. {t.title}</p>
                              <p className="text-[11px] text-muted-foreground">{t.project_name}</p>
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>

                  {/* Confirmed Workforce */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <Users className="h-4 w-4 text-primary" /> Confirmed Workforce
                        </span>
                        <Badge variant="outline">{confirmedContactsCount} Confirmed</Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-4">
                      <ScrollArea className="h-[250px]">
                        <div className="space-y-2">
                          {selectedAssignedEmployees.map(e => (
                            <div key={e.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div>
                                <p className="font-semibold text-foreground">{e.display_name}</p>
                                <p className="text-[11px] text-muted-foreground capitalize">{e.role}</p>
                              </div>
                              {renderStatusBadge(e.id)}
                            </div>
                          ))}
                          {prioritizedContractors.filter(c => c.isRecommended).map(c => (
                            <div key={c.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div>
                                <p className="font-semibold text-foreground">{c.name}</p>
                                <p className="text-[11px] text-muted-foreground capitalize">{c.category || 'Contractor'}</p>
                              </div>
                              {renderStatusBadge(c.id)}
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>

                  {/* Materials Audit Summary */}
                  <Card className="glass-card">
                    <CardHeader className="pb-3 border-b border-white/5">
                      <CardTitle className="text-base flex items-center justify-between">
                        <span className="flex items-center gap-2">
                          <Boxes className="h-4 w-4 text-primary" /> Material Readiness
                        </span>
                        <Badge variant={criticalMaterialCount > 0 ? "destructive" : "outline"}>
                          {criticalMaterialCount > 0 ? `${criticalMaterialCount} Deficits` : 'Verified'}
                        </Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-4">
                      <ScrollArea className="h-[250px]">
                        <div className="space-y-2">
                          {relevantMaterials.filter(m => m.isRequiredForTomorrow).map(m => (
                            <div key={m.id} className="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                              <div>
                                <p className="font-semibold text-foreground">{m.name}</p>
                                <p className="text-[11px] text-muted-foreground">{m.current_stock} {m.unit_of_measurement} in stock</p>
                              </div>
                              {m.current_stock === 0 ? (
                                <Badge variant="destructive" className="text-[10px]">Stockout</Badge>
                              ) : (
                                <Badge className="bg-emerald-500 text-white text-[10px]">OK</Badge>
                              )}
                            </div>
                          ))}
                        </div>
                      </ScrollArea>
                    </CardContent>
                  </Card>
                </div>

                <div className="flex justify-between pt-4">
                  <Button variant="outline" onClick={() => setCurrentStep(3)}>
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
