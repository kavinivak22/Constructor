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
import { useToast } from '@/hooks/use-toast';
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
  MapPin,
  AlertCircle,
  HelpCircle,
  PhoneOff,
  UserCheck,
  Loader2
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

  // Page state
  const [selectedProjectId, setSelectedProjectId] = useState<string>(initialProjectId);
  const [projects, setProjects] = useState<Project[]>([]);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [contractors, setContractors] = useState<Contractor[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Call checklist states
  const [callStatuses, setCallStatuses] = useState<Record<string, CallStatus>>({});

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
          .select('id, name, company_id')
          .eq('company_id', companyId);

        if (projectsError) throw projectsError;
        setProjects(projectsData || []);

        const projectIds = (projectsData || []).map(p => p.id);

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

        // Map database camelCase columns
        const mappedContractors = (contractorsData || []).map(c => ({
          id: c.id,
          name: c.name,
          category: c.category,
          contactPerson: c.contactPerson || c.contact_person || null,
          phone: c.phone,
          email: c.email,
        }));
        setContractors(mappedContractors);

        // 5. Fetch all tasks for company projects
        if (projectIds.length > 0) {
          const { data: tasksData, error: tasksError } = await supabase
            .from('tasks')
            .select('*')
            .in('project_id', projectIds);

          if (tasksError) throw tasksError;

          // Map project names and assigned user details
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
        } else {
          setTasks([]);
        }
      } catch (err: any) {
        console.error('Error fetching work preparation data:', err);
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

  // Load call checklist statuses from localStorage
  useEffect(() => {
    const key = `work-prep-calls-${tomorrowDateStr}`;
    const stored = localStorage.getItem(key);
    if (stored) {
      try {
        setCallStatuses(JSON.parse(stored));
      } catch (e) {
        console.error('Failed to parse call statuses', e);
      }
    }
  }, [tomorrowDateStr]);

  // Save call checklist status change
  const handleStatusChange = (id: string, status: CallStatus) => {
    const newStatuses = { ...callStatuses, [id]: status };
    setCallStatuses(newStatuses);
    localStorage.setItem(`work-prep-calls-${tomorrowDateStr}`, JSON.stringify(newStatuses));

    // Toast feedback
    const labels: Record<CallStatus, string> = {
      pending: 'Cleared call log status.',
      called: 'Marked as Called.',
      confirmed: 'Marked as Confirmed!',
      'no-answer': 'Marked as No Answer.',
    };

    toast({
      title: 'Call Status Updated',
      description: labels[status],
    });
  };

  // Helper date checker
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

  // Filter tasks based on Project and Tomorrow's date
  const tomorrowTasks = tasks.filter(task => {
    // Check if task is scheduled for tomorrow
    const isScheduledForTomorrow = isTaskTomorrow(task.due_date);
    
    // Project filter
    const matchesProject = selectedProjectId === 'all' || task.project_id === selectedProjectId;

    return isScheduledForTomorrow && matchesProject;
  });

  // Split tasks into Standard Tasks and Upcoming Milestones
  const tomorrowStandardTasks = tomorrowTasks.filter(t => !t.is_upcoming);
  const tomorrowMilestones = tomorrowTasks.filter(t => t.is_upcoming);

  // Determine who to call - Team Members (Employees) assigned to tomorrow's tasks
  const assignedEmployees = employees.filter(emp => {
    // Check if employee has any task tomorrow
    const hasTaskTomorrow = tomorrowTasks.some(t => t.assigned_to === emp.id);
    
    // Search query filter
    const matchesSearch = searchQuery.trim() === '' || 
      (emp.display_name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (emp.role || '').toLowerCase().includes(searchQuery.toLowerCase());

    return hasTaskTomorrow && matchesSearch;
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

  // Keyword check helper
  const getTaskMatchingExplanation = (contractor: Contractor) => {
    const category = (contractor.category || '').toLowerCase();
    const name = contractor.name.toLowerCase();

    for (const task of tomorrowTasks) {
      const taskText = `${task.title} ${task.description || ''}`.toLowerCase();
      
      // Direct name check
      if (taskText.includes(name)) {
        return `Mentioned in tomorrow's task: "${task.title}"`;
      }

      // Check category keywords
      for (const [groupKey, keywords] of Object.entries(categoryKeywords)) {
        if (category.includes(groupKey) || groupKey.includes(category)) {
          const matchedKeyword = keywords.find(keyword => taskText.includes(keyword));
          if (matchedKeyword) {
            return `Category matches "${matchedKeyword}" in tomorrow's task: "${task.title}"`;
          }
        }
      }
    }
    return null;
  };

  // Filter and prioritize contractors
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
      // Search query filter
      const matchesSearch = searchQuery.trim() === '' ||
        c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (c.category || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (c.contactPerson || '').toLowerCase().includes(searchQuery.toLowerCase());

      return matchesSearch;
    })
    // Sort: Recommended contractors first, then alphabetical by name
    .sort((a, b) => {
      if (a.isRecommended && !b.isRecommended) return -1;
      if (!a.isRecommended && b.isRecommended) return 1;
      return a.name.localeCompare(b.name);
    });

  // Calculate progress stats for tomorrow's contacts
  const allContacts = [
    ...assignedEmployees.map(e => ({ id: e.id, type: 'employee' })),
    ...prioritizedContractors.filter(c => c.isRecommended).map(c => ({ id: c.id, type: 'contractor' }))
  ];

  const totalContactsToCall = allContacts.length;
  const confirmedCount = allContacts.filter(c => callStatuses[c.id] === 'confirmed').length;
  const calledCount = allContacts.filter(c => callStatuses[c.id] === 'called').length;
  const noAnswerCount = allContacts.filter(c => callStatuses[c.id] === 'no-answer').length;
  const pendingCount = totalContactsToCall - (confirmedCount + calledCount + noAnswerCount);

  // Status icon mapping
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
      {/* Header */}
      <header className="flex flex-col md:flex-row md:items-center gap-4 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-40">
        <div className="flex items-center gap-4 flex-1">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline flex items-center gap-2">
              <PhoneCall className="h-6 w-6 text-primary" />
              Work Preparation
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
            <Loader2Icon className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : (
          <>
            {/* Quick Metrics Summary */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <Card className="glass-card">
                <CardContent className="p-4 flex flex-col justify-center">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">Scheduled Tasks</p>
                  <p className="text-2xl font-bold mt-1 text-foreground">{tomorrowTasks.length}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">
                    {tomorrowStandardTasks.length} tasks • {tomorrowMilestones.length} milestones
                  </p>
                </CardContent>
              </Card>

              <Card className="glass-card">
                <CardContent className="p-4 flex flex-col justify-center">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">People to Call</p>
                  <p className="text-2xl font-bold mt-1 text-foreground">{totalContactsToCall}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">
                    {assignedEmployees.length} employees • {prioritizedContractors.filter(c => c.isRecommended).length} contractors
                  </p>
                </CardContent>
              </Card>

              <Card className="glass-card">
                <CardContent className="p-4 flex flex-col justify-center border-l-4 border-l-emerald-500">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">Confirmed Team</p>
                  <p className="text-2xl font-bold mt-1 text-emerald-500">{confirmedCount}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">Ready for site work</p>
                </CardContent>
              </Card>

              <Card className="glass-card">
                <CardContent className="p-4 flex flex-col justify-center">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">Prep Checklist Progress</p>
                  <div className="w-full bg-muted/40 rounded-full h-1.5 mt-2 overflow-hidden">
                    <div 
                      className="bg-primary h-full rounded-full transition-all duration-500" 
                      style={{ width: `${totalContactsToCall > 0 ? ((confirmedCount + calledCount + noAnswerCount) / totalContactsToCall) * 100 : 0}%` }}
                    />
                  </div>
                  <p className="text-[10px] text-muted-foreground mt-1.5">
                    {confirmedCount + calledCount + noAnswerCount} / {totalContactsToCall} Completed
                  </p>
                </CardContent>
              </Card>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              
              {/* Left Column: Tomorrow's Schedule (5/12 grid width) */}
              <div className="lg:col-span-5 space-y-6">
                <Card className="glass-card h-full">
                  <CardHeader className="pb-3 border-b border-white/5">
                    <CardTitle className="text-base flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-primary" />
                      Tomorrow's Schedule
                    </CardTitle>
                    <CardDescription>
                      Tasks and milestones scheduled for tomorrow
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="p-0">
                    <ScrollArea className="h-[500px]">
                      {tomorrowTasks.length === 0 ? (
                        <div className="flex flex-col items-center justify-center p-8 text-center h-[350px]">
                          <CheckCircle2 className="h-10 w-10 text-emerald-500/80 mb-2" />
                          <p className="font-semibold text-foreground/90">No Work Scheduled</p>
                          <p className="text-sm text-muted-foreground mt-1 max-w-xs">
                            No tasks or upcoming milestones are scheduled for tomorrow for the selected filter.
                          </p>
                        </div>
                      ) : (
                        <div className="divide-y divide-white/5">
                          {/* Milestones / Upcoming Works */}
                          {tomorrowMilestones.length > 0 && (
                            <div className="p-4 space-y-3">
                              <h3 className="text-xs font-bold uppercase text-muted-foreground tracking-wider flex items-center gap-1.5">
                                <Sparkles className="h-3 w-3 text-amber-500" /> Planned Milestones ({tomorrowMilestones.length})
                              </h3>
                              <div className="space-y-3">
                                {tomorrowMilestones.map(milestone => (
                                  <div key={milestone.id} className="p-3 rounded-lg bg-amber-500/5 border border-amber-500/10 space-y-2">
                                    <div className="flex justify-between items-start gap-2">
                                      <p className="font-medium text-sm text-amber-500">{milestone.title}</p>
                                      <Badge variant="outline" className="capitalize text-[10px] font-semibold border-amber-500/20 text-amber-400 shrink-0">
                                        {milestone.priority || 'Medium'}
                                      </Badge>
                                    </div>
                                    {milestone.description && (
                                      <p className="text-xs text-muted-foreground leading-normal">{milestone.description}</p>
                                    )}
                                    <div className="flex items-center justify-between text-[11px] text-muted-foreground">
                                      <span className="flex items-center gap-1">
                                        <Building2 className="h-3 w-3 text-muted-foreground/60" /> {milestone.project_name}
                                      </span>
                                      {milestone.duration && (
                                        <span className="flex items-center gap-1 text-amber-500/80 font-medium">
                                          <Clock className="h-3 w-3" /> {milestone.duration}
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Standard Tasks */}
                          {tomorrowStandardTasks.length > 0 && (
                            <div className="p-4 space-y-3">
                              <h3 className="text-xs font-bold uppercase text-muted-foreground tracking-wider">
                                Active Tasks ({tomorrowStandardTasks.length})
                              </h3>
                              <div className="space-y-3">
                                {tomorrowStandardTasks.map(task => (
                                  <div key={task.id} className="p-3 rounded-lg bg-white/5 border border-white/5 space-y-2">
                                    <div className="flex justify-between items-start gap-2">
                                      <p className="font-medium text-sm text-foreground/90">{task.title}</p>
                                      <Badge 
                                        variant={task.priority === 'high' ? 'destructive' : 'secondary'}
                                        className={`capitalize text-[10px] font-semibold ${
                                          task.priority === 'medium' ? 'bg-amber-500/10 text-amber-500 border-transparent hover:bg-amber-500/20' : ''
                                        }`}
                                      >
                                        {task.priority || 'medium'}
                                      </Badge>
                                    </div>
                                    {task.description && (
                                      <p className="text-xs text-muted-foreground leading-normal">{task.description}</p>
                                    )}
                                    <div className="flex flex-wrap items-center justify-between text-[11px] text-muted-foreground pt-1 border-t border-white/5">
                                      <span className="flex items-center gap-1">
                                        <Building2 className="h-3 w-3 text-muted-foreground/60" /> {task.project_name}
                                      </span>
                                      {task.assigned_user ? (
                                        <span className="font-semibold text-primary">
                                          Assignee: {task.assigned_user.display_name}
                                        </span>
                                      ) : (
                                        <span className="text-red-400 font-medium flex items-center gap-0.5">
                                          <AlertCircle className="h-3 w-3" /> Unassigned
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </ScrollArea>
                  </CardContent>
                </Card>
              </div>

              {/* Right Column: Call Planner Board (7/12 grid width) */}
              <div className="lg:col-span-7 space-y-6">
                <Card className="glass-card">
                  <CardHeader className="pb-3 border-b border-white/5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div>
                      <CardTitle className="text-base flex items-center gap-2">
                        <Users className="h-4 w-4 text-primary" />
                        Call Planner & Checklist
                      </CardTitle>
                      <CardDescription>
                        Call tomorrow's team and contractors to confirm details
                      </CardDescription>
                    </div>

                    {/* Search bar */}
                    <div className="relative w-full sm:w-[200px]">
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
                          Team Members ({assignedEmployees.length})
                        </TabsTrigger>
                        <TabsTrigger 
                          value="contractors" 
                          className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent px-6 text-sm font-medium"
                        >
                          Contractors ({prioritizedContractors.length})
                        </TabsTrigger>
                      </TabsList>

                      {/* Employees tab */}
                      <TabsContent value="team" className="p-0 m-0">
                        <ScrollArea className="h-[460px]">
                          {assignedEmployees.length === 0 ? (
                            <div className="flex flex-col items-center justify-center p-8 text-center h-[350px]">
                              <Users className="h-10 w-10 text-muted-foreground/50 mb-2" />
                              <p className="font-semibold text-foreground/90">No Team Members</p>
                              <p className="text-sm text-muted-foreground mt-1 max-w-xs">
                                {searchQuery ? 'No employees found matching the search.' : 'No team members are assigned to tomorrow\'s tasks.'}
                              </p>
                            </div>
                          ) : (
                            <div className="divide-y divide-white/5">
                              {assignedEmployees.map(emp => {
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
                                          <p className="font-medium text-sm text-foreground">{emp.display_name}</p>
                                          <Badge variant="outline" className="text-[10px] font-semibold border-white/10 text-muted-foreground capitalize">
                                            {emp.role}
                                          </Badge>
                                        </div>
                                        <p className="text-xs text-muted-foreground mt-0.5">{emp.email}</p>
                                        
                                        {/* Assigned Tasks Summary */}
                                        <div className="flex flex-wrap gap-1 mt-1.5">
                                          {tomorrowTasks
                                            .filter(t => t.assigned_to === emp.id)
                                            .map(t => (
                                              <span key={t.id} className="text-[10px] px-1.5 py-0.5 rounded bg-primary/10 text-primary border border-primary/10 max-w-[150px] truncate" title={t.title}>
                                                {t.title}
                                              </span>
                                            ))}
                                        </div>
                                      </div>
                                    </div>

                                    {/* Action items */}
                                    <div className="flex items-center gap-3 shrink-0 ml-auto sm:ml-0">
                                      <div className="flex items-center gap-1.5">
                                        {renderStatusBadge(emp.id)}
                                        
                                        {/* Call Status Actions Dropdown */}
                                        <Select 
                                          value={status} 
                                          onValueChange={(val) => handleStatusChange(emp.id, val as CallStatus)}
                                        >
                                          <SelectTrigger className="w-[32px] h-[32px] p-0 border-white/10 bg-background/50 hover:bg-muted shrink-0 flex items-center justify-center rounded-md">
                                            <HelpCircle className="h-4 w-4 text-muted-foreground" />
                                          </SelectTrigger>
                                          <SelectContent align="end">
                                            <SelectItem value="pending">Reset Status</SelectItem>
                                            <SelectItem value="called">Mark Called</SelectItem>
                                            <SelectItem value="confirmed">Confirm Attendance</SelectItem>
                                            <SelectItem value="no-answer">No Answer</SelectItem>
                                          </SelectContent>
                                        </Select>
                                      </div>

                                      {emp.phone ? (
                                        <Button size="icon" className="h-8 w-8 rounded-full" asChild>
                                          <a href={`tel:${emp.phone}`} title={`Call ${emp.display_name}`}>
                                            <Phone className="h-3.5 w-3.5" />
                                          </a>
                                        </Button>
                                      ) : (
                                        <Button size="icon" variant="outline" className="h-8 w-8 rounded-full opacity-40 cursor-not-allowed" disabled>
                                          <Phone className="h-3.5 w-3.5" />
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

                      {/* Contractors tab */}
                      <TabsContent value="contractors" className="p-0 m-0">
                        <ScrollArea className="h-[460px]">
                          {prioritizedContractors.length === 0 ? (
                            <div className="flex flex-col items-center justify-center p-8 text-center h-[350px]">
                              <Building2 className="h-10 w-10 text-muted-foreground/50 mb-2" />
                              <p className="font-semibold text-foreground/90">No Contractors Found</p>
                              <p className="text-sm text-muted-foreground mt-1 max-w-xs">
                                {searchQuery ? 'No contractors matched your search.' : 'You have no contractors registered in your company account yet.'}
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
                                      c.isRecommended ? 'bg-primary/5 border-l-2 border-l-primary/60' : ''
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
                                              <Badge className="text-[10px] font-semibold bg-white/10 hover:bg-white/15 text-muted-foreground border-transparent uppercase">
                                                {c.category}
                                              </Badge>
                                            )}
                                            {c.isRecommended && (
                                              <Badge className="text-[10px] font-bold bg-primary text-primary-foreground border-transparent flex items-center gap-0.5">
                                                <Sparkles className="h-2.5 w-2.5" /> Recommended
                                              </Badge>
                                            )}
                                          </div>
                                          
                                          {c.contactPerson && (
                                            <p className="text-xs text-muted-foreground mt-0.5">Contact: <span className="font-medium text-foreground/80">{c.contactPerson}</span></p>
                                          )}
                                        </div>
                                      </div>

                                      {/* Action items */}
                                      <div className="flex items-center gap-3 shrink-0 ml-auto sm:ml-0">
                                        <div className="flex items-center gap-1.5">
                                          {renderStatusBadge(c.id)}
                                          
                                          {/* Call Status Actions Dropdown */}
                                          <Select 
                                            value={status} 
                                            onValueChange={(val) => handleStatusChange(c.id, val as CallStatus)}
                                          >
                                            <SelectTrigger className="w-[32px] h-[32px] p-0 border-white/10 bg-background/50 hover:bg-muted shrink-0 flex items-center justify-center rounded-md">
                                              <HelpCircle className="h-4 w-4 text-muted-foreground" />
                                            </SelectTrigger>
                                            <SelectContent align="end">
                                              <SelectItem value="pending">Reset Status</SelectItem>
                                              <SelectItem value="called">Mark Called</SelectItem>
                                              <SelectItem value="confirmed">Confirm Availability</SelectItem>
                                              <SelectItem value="no-answer">No Answer</SelectItem>
                                            </SelectContent>
                                          </Select>
                                        </div>

                                        {c.phone ? (
                                          <Button size="icon" className="h-8 w-8 rounded-full" asChild>
                                            <a href={`tel:${c.phone}`} title={`Call ${c.name}`}>
                                              <Phone className="h-3.5 w-3.5" />
                                            </a>
                                          </Button>
                                        ) : (
                                          <Button size="icon" variant="outline" className="h-8 w-8 rounded-full opacity-40 cursor-not-allowed" disabled>
                                            <Phone className="h-3.5 w-3.5" />
                                          </Button>
                                        )}
                                      </div>
                                    </div>

                                    {/* Task Matching Explanation */}
                                    {c.isRecommended && c.matchExplanation && (
                                      <div className="text-[11px] bg-primary/10 text-primary px-3 py-1.5 rounded border border-primary/10 flex items-start gap-1.5">
                                        <Sparkles className="h-3 w-3 shrink-0 mt-0.5" />
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
                  </CardContent>
                </Card>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

// Minimal loader icon helper
function Loader2Icon({ className }: { className?: string }) {
  return <Loader2 className={className} />;
}
