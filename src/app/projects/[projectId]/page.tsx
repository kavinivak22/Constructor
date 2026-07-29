'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { type Project } from '@/lib/data';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Calendar, Users, IndianRupee, Edit, CheckCircle, Clock, ArrowLeft } from 'lucide-react';
import { Progress } from '@/components/ui/progress';
import { format } from 'date-fns';
import { Button } from '@/components/ui/button';
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel';
import { Checkbox } from '@/components/ui/checkbox';
import Image from 'next/image';
import Link from 'next/link';
import { useProject, useProjectMembersCount } from '@/hooks/queries';
import { useProjectWorklogs } from '@/hooks/queries/use-worklogs';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';
import { WorklogDetailDialog } from '@/components/worklog/worklog-detail-dialog';



import { getProjectTasks, getUpcomingWorks, toggleTaskStatus, deleteTaskAction, type TaskItem } from '@/app/actions/tasks';
import { CreateTaskDialog } from '@/components/projects/create-task-dialog';
import { EditTaskDialog } from '@/components/projects/edit-task-dialog';
import { EditProjectDialog } from '@/components/projects/edit-project-dialog';
import { AdaptiveNextTaskWidget } from '@/components/projects/adaptive-next-task-widget';
import { BuildingPlanSummaryCard } from '@/components/projects/building-plan-summary-card';
import { CreateUpcomingDialog } from '@/components/projects/create-upcoming-dialog';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Trash2 } from 'lucide-react';



export default function ProjectDetailsPage() {
    const { projectId } = useParams();
    const projectIdString = (Array.isArray(projectId) ? projectId[0] : projectId) || '';
    const router = useRouter();
    const { supabase, user } = useSupabase();
    const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string; company_id?: string | null } | null>(null);

    const { toast } = useToast();
    const [tasks, setTasks] = useState<TaskItem[]>([]);
    const [upcomingWorks, setUpcomingWorks] = useState<TaskItem[]>([]);
    const [employees, setEmployees] = useState<{ id: string; display_name: string; email: string }[]>([]);
    const [isLoadingTasks, setIsLoadingTasks] = useState(true);
    const [isLoadingUpcoming, setIsLoadingUpcoming] = useState(true);
    const [refreshTasksTrigger, setRefreshTasksTrigger] = useState(0);

    const [selectedWorklog, setSelectedWorklog] = useState<any | null>(null);
    const [isWorklogDialogOpen, setIsWorklogDialogOpen] = useState(false);

    // Fetch project tasks and upcoming works
    useEffect(() => {
        const fetchTasksData = async () => {
            if (!projectIdString) return;
            setIsLoadingTasks(true);
            const taskData = await getProjectTasks(projectIdString);
            setTasks(taskData);
            setIsLoadingTasks(false);
        };

        const fetchUpcomingData = async () => {
            if (!projectIdString) return;
            setIsLoadingUpcoming(true);
            const upcomingData = await getUpcomingWorks(projectIdString);
            setUpcomingWorks(upcomingData);
            setIsLoadingUpcoming(false);
        };

        fetchTasksData();
        fetchUpcomingData();
    }, [projectIdString, refreshTasksTrigger]);

    // Fetch company employees for task assignment
    useEffect(() => {
        const fetchEmployees = async () => {
            const { data: { user: currentUser } } = await supabase.auth.getUser();
            if (!currentUser) return;

            const { data: userProfile } = await supabase
                .from('users')
                .select('company_id')
                .eq('id', currentUser.id)
                .single();

            if (userProfile?.company_id) {
                const { data } = await supabase
                    .from('users')
                    .select('id, display_name, email')
                    .eq('company_id', userProfile.company_id);
                if (data) {
                    setEmployees(data);
                }
            }
        };
        fetchEmployees();
    }, [supabase]);

    const handleToggleTask = async (taskId: string, currentStatus: string) => {
        setTasks(prev =>
            prev.map(t => t.id === taskId ? { ...t, status: currentStatus === 'completed' ? 'pending' : 'completed' } : t)
        );

        const result = await toggleTaskStatus(taskId, currentStatus);
        if (!result.success) {
            toast({
                title: 'Error',
                description: result.error || 'Failed to update task.',
                variant: 'destructive'
            });
            const taskData = await getProjectTasks(projectIdString);
            setTasks(taskData);
        }
    };

    const handleDeleteTask = async (taskId: string) => {
        if (!confirm('Are you sure you want to delete this task?')) return;

        setTasks(prev => prev.filter(t => t.id !== taskId));

        const result = await deleteTaskAction(taskId);
        if (!result.success) {
            toast({
                title: 'Error',
                description: result.error || 'Failed to delete task.',
                variant: 'destructive'
            });
            const taskData = await getProjectTasks(projectIdString);
            setTasks(taskData);
        } else {
            toast({
                title: 'Task Deleted',
                description: 'Project task has been removed.',
            });
        }
    };

    useEffect(() => {
        const fetchProfile = async () => {
            if (!user) return;
            const { data } = await supabase
                .from('users')
                .select('id, role')
                .eq('id', user.id)
                .single();
            if (data) {
                setCurrentUserProfile(data);
            }
        };
        fetchProfile();
    }, [user, supabase]);

    const isAdminOrManager = currentUserProfile?.role === 'admin' || currentUserProfile?.role === 'manager';

    // Use React Query hooks for data fetching
    const { data: project, isLoading, error } = useProject(projectIdString);
    const { data: memberCount = 0 } = useProjectMembersCount(projectIdString);

    const { data: worklogs = [] } = useProjectWorklogs(projectIdString);

    const getFormattedDate = (date: string | null | undefined, formatStr: string = 'MMM d, yyyy') => {
        if (!date) return 'N/A';
        return format(new Date(date), formatStr);
    };

    const budget = 850000;
    const spent = 552500;
    const spentPercentage = (spent / budget) * 100;
    const remaining = budget - spent;


    if (isLoading) {
        return <ProjectDetailsSkeleton />;
    }

    if (!project) {
        return (
            <div className="flex flex-col h-full">
                <header className="flex items-center gap-2 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-10">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
                        Project Not Found
                    </h1>
                </header>
                <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-transparent">
                    <Card className="glass-card">
                        <CardContent className='p-6'>
                            <p>The project you are looking for does not exist or you do not have permission to view it.</p>
                        </CardContent>
                    </Card>
                </main>
            </div>
        );
    }

    return (
        <div className="flex flex-col h-full bg-transparent">
            <header className="flex items-center gap-4 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-40">
                <Button variant="ghost" size="icon" onClick={() => router.back()}>
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div className="flex-1">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline truncate">
                        {project.name}
                    </h1>
                    <div className="flex items-center gap-2 mt-1">
                        {project.thumbnail_url && (
                            <div className="relative w-5 h-5 rounded-full overflow-hidden shrink-0">
                                <Image src={project.thumbnail_url} alt={project.client_name || project.clientName || 'Client'} fill className="object-cover" />
                            </div>
                        )}
                        <p className="text-sm text-muted-foreground">{project.client_name || project.clientName || 'Internal Project'}</p>
                    </div>
                </div>
                <div className="flex items-center gap-2 ml-auto">
                    <div className="text-sm text-muted-foreground hidden sm:block">Current Stage:</div>
                    <Badge variant="outline" className="capitalize shrink-0">
                        Foundation
                    </Badge>
                </div>
                {isAdminOrManager && (
                    <EditProjectDialog
                        project={project}
                        trigger={
                            <Button variant="outline" size="sm">
                                <Edit className="mr-2 h-3 w-3" />
                                Edit
                            </Button>
                        }
                    />
                )}
            </header>
            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6 bg-transparent">
                <Card className="glass-card">
                    <CardHeader>
                        <CardTitle>Overall Progress</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-4'>
                        <div>
                            <div className="flex justify-between items-center text-sm font-medium text-muted-foreground mb-2">
                                <p>Progress</p>
                                <p className="font-semibold text-foreground text-lg">{project.progress ?? 0}%</p>
                            </div>
                            <Progress value={project.progress ?? 0} aria-label={`${project.progress ?? 0}% complete`} className="h-2" />
                        </div>
                        <div className='grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-muted-foreground pt-2'>
                            <div className='flex items-center gap-2'>
                                <Calendar className="h-4 w-4" />
                                <div>
                                    <p className='text-xs'>End Date</p>
                                    <p className='font-semibold text-foreground'>{getFormattedDate(project.endDate as string)}</p>
                                </div>
                            </div>
                            <div className='flex items-center gap-2'>
                                <IndianRupee className="h-4 w-4" />
                                <div>
                                    <p className='text-xs'>Budget</p>
                                    <p className='font-semibold text-foreground'>₹8.5L</p>
                                </div>
                            </div>
                            <div className='flex items-center gap-2'>
                                <Users className="h-4 w-4" />
                                <div>
                                    <p className='text-xs'>Team Size</p>
                                    <p className='font-semibold text-foreground'>{memberCount} members</p>
                                </div>
                            </div>
                            <div className='flex items-center gap-2'>
                                <Progress value={65} className="w-6 h-6" />
                                <div>
                                    <p className='text-xs'>Spent</p>
                                    <p className='font-semibold text-foreground'>65%</p>
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                <div className='flex flex-wrap gap-2'>
                    <Button asChild>
                        <Link href={`/projects/${projectId}/materials`}>View Materials</Link>
                    </Button>
                    <Button variant="outline" asChild>
                        <Link href={`/projects/${projectId}/expenses`}>View Expenses</Link>
                    </Button>
                    <Button variant="outline" asChild>
                        <Link href={`/projects/${projectId}/building-plan`}>Building Plan</Link>
                    </Button>
                    <Button variant="outline" asChild>
                        <Link href={`/work-prep?projectId=${projectIdString}`}>Tomorrow's Prep</Link>
                    </Button>
                    <CreateWorklogDialog
                        projectId={projectIdString}
                        trigger={<Button variant="outline">Add Update</Button>}
                        onSuccess={() => window.location.reload()}
                    />
                </div>

                <AdaptiveNextTaskWidget
                    projectId={projectIdString}
                    projectName={project.name}
                />

                <BuildingPlanSummaryCard
                    projectId={projectIdString}
                    projectName={project.name}
                />

                {worklogs.length > 0 && (
                    <div>
                        <h2 className="text-xl font-bold mb-4">Recent Updates</h2>
                        <Carousel opts={{ align: "start", loop: true }} className="w-full">
                            <CarouselContent className="-ml-4">
                                {worklogs.map((worklog: any, index: number) => {
                                    // Aggregate descriptions from labor entries
                                    const description = worklog.labor?.map((l: any) => l.work_description).filter(Boolean).join('. ') || 'No description provided.';

                                    // Use explicit title if available, otherwise generate smart title
                                    let title = worklog.title || 'Daily Log';

                                    // If title is the default "Daily Log", try to generate a more descriptive one for backward compatibility
                                    if (!worklog.title || worklog.title === 'Daily Log') {
                                        // 1. Try to use categories
                                        const categories = Array.from(new Set(worklog.labor?.map((l: any) => l.category).filter(Boolean))) as string[];
                                        if (categories.length > 0) {
                                            title = categories.slice(0, 2).join(' & ') + (categories.length > 2 ? '...' : '') + ' Work';
                                        }
                                        // 2. Try to use photo caption
                                        else if (worklog.photos?.[0]?.caption) {
                                            title = worklog.photos[0].caption;
                                        }
                                        // 3. Fallback to description summary
                                        else if (description !== 'No description provided.') {
                                            // Take first sentence or first 40 chars
                                            const firstSentence = description.split(/[.!?]/)[0];
                                            title = firstSentence.length > 40 ? firstSentence.substring(0, 40) + '...' : firstSentence;
                                        }
                                    }

                                    // Use first photo or placeholder
                                    const imageUrl = worklog.photos?.[0]?.photo_url || 'https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=2940&auto=format&fit=crop';
                                     return (
                                        <CarouselItem key={worklog.id} className="pl-4 basis-[85%] md:basis-1/2 lg:basis-1/3">
                                            <div
                                                onClick={() => {
                                                    setSelectedWorklog(worklog);
                                                    setIsWorklogDialogOpen(true);
                                                }}
                                                className="cursor-pointer block h-full"
                                            >
                                                <Card className="glass-card h-full hover:scale-[1.02] transition-transform duration-300">
                                                    <CardContent className="p-0 flex flex-col h-full">
                                                        <div className="relative aspect-[3/2] w-full overflow-hidden rounded-t-lg">
                                                            <Image
                                                                src={imageUrl}
                                                                alt={title}
                                                                fill
                                                                className="object-cover"
                                                                data-ai-hint="construction update"
                                                            />
                                                        </div>
                                                        <div className='p-4 flex-1 flex flex-col'>
                                                            <div className="flex justify-between items-center text-xs text-muted-foreground mb-2">
                                                                <p>{format(new Date(worklog.date), 'MMM dd, yyyy')}</p>
                                                            </div>
                                                            <h3 className="font-bold mb-1 line-clamp-1">{title}</h3>
                                                            <p className="text-sm text-muted-foreground line-clamp-3">{description}</p>
                                                        </div>
                                                    </CardContent>
                                                </Card>
                                            </div>
                                        </CarouselItem>
                                    );
                                })}
                            </CarouselContent>
                            <CarouselPrevious variant="ghost" className="absolute left-2 top-1/2 -translate-y-1/2 z-10 hidden sm:flex h-8 w-8 rounded-full bg-white/50 hover:bg-white/75 text-foreground" />
                            <CarouselNext variant="ghost" className="absolute right-2 top-1/2 -translate-y-1/2 z-10 hidden sm:flex h-8 w-8 rounded-full bg-white/50 hover:bg-white/75 text-foreground" />
                        </Carousel>
                    </div>
                )}

                <Card className="glass-card">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <CheckCircle className="text-primary h-5 w-5" />
                            Project Tasks
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {isLoadingTasks ? (
                            <div className="space-y-3">
                                {[...Array(3)].map((_, i) => (
                                    <div key={i} className="flex items-start gap-3">
                                        <Skeleton className="h-4 w-4 rounded mt-1" />
                                        <div className="space-y-1 flex-1">
                                            <Skeleton className="h-4 w-3/4" />
                                            <Skeleton className="h-3 w-1/4" />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : tasks.length > 0 ? (
                            <div className="space-y-4 max-h-[350px] overflow-y-auto pr-1">
                                {tasks.map(task => {
                                    const isCheckboxDisabled = !isAdminOrManager && task.assigned_to !== user?.id;
                                    return (
                                        <div key={task.id} className="flex items-start justify-between py-1 group">
                                            <div className="flex items-start gap-3 flex-1">
                                                <Checkbox
                                                    id={task.id}
                                                    checked={task.status === 'completed'}
                                                    onCheckedChange={() => handleToggleTask(task.id, task.status)}
                                                    disabled={isCheckboxDisabled}
                                                    className="mt-1"
                                                />
                                                <div className="flex-1">
                                                    <label
                                                        htmlFor={task.id}
                                                        className={`font-medium cursor-pointer select-none transition-all ${
                                                            task.status === 'completed' ? 'line-through text-muted-foreground/60' : 'text-foreground/90'
                                                        }`}
                                                    >
                                                        {task.title}
                                                    </label>
                                                    <div className="text-xs text-muted-foreground flex flex-wrap gap-x-3 gap-y-1 mt-1">
                                                        {task.due_date && (
                                                            <span className="flex items-center gap-1">
                                                                <Clock className="h-3 w-3" /> {getFormattedDate(task.due_date)}
                                                            </span>
                                                        )}
                                                        {task.assigned_user && (
                                                            <span className="font-semibold text-primary/80">
                                                                Assignee: {task.assigned_user.display_name}
                                                            </span>
                                                        )}
                                                        {task.priority && (
                                                            <span className={`capitalize font-bold ${
                                                                task.priority === 'high' ? 'text-red-500' : task.priority === 'medium' ? 'text-amber-500' : 'text-slate-400'
                                                            }`}>
                                                                {task.priority}
                                                            </span>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                            {isAdminOrManager && (
                                                <div className="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 shrink-0 ml-2">
                                                    <EditTaskDialog
                                                        task={task}
                                                        employees={employees}
                                                        onSuccess={() => setRefreshTasksTrigger(prev => prev + 1)}
                                                    />
                                                    <Button
                                                        size="icon"
                                                        variant="ghost"
                                                        className="h-7 w-7 p-0 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/20"
                                                        onClick={() => handleDeleteTask(task.id)}
                                                    >
                                                        <Trash2 className="h-3.5 w-3.5" />
                                                    </Button>
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground text-center py-6">No project tasks. Create one below!</p>
                        )}

                        {isAdminOrManager && (
                            <CreateTaskDialog
                                projectId={projectIdString}
                                employees={employees}
                                onSuccess={() => setRefreshTasksTrigger(prev => prev + 1)}
                            />
                        )}
                    </CardContent>
                </Card>

                <Card className="glass-card">
                    <CardHeader>
                        <CardTitle>Budget Overview</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-2">
                            <div className="flex justify-between text-sm">
                                <span>Total Budget</span>
                                <span>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(budget)}</span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <span>Amount Spent</span>
                                <span>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(spent)}</span>
                            </div>
                            <div className="relative h-4 rounded-full bg-muted overflow-hidden">
                                <div className="absolute top-0 left-0 h-full bg-primary" style={{ width: `${spentPercentage}%` }}></div>
                            </div>
                            <div className="flex justify-between text-sm font-bold">
                                <span>Remaining</span>
                                <span>{new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(remaining)}</span>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </main>

            <WorklogDetailDialog
                worklog={selectedWorklog}
                isOpen={isWorklogDialogOpen}
                onClose={() => setIsWorklogDialogOpen(false)}
            />
        </div>
    );
}

function ProjectDetailsSkeleton() {
    return (
        <div className="flex flex-col h-full bg-transparent">
            <header className="flex items-center gap-4 p-4 md:px-6 shrink-0 bg-transparent sticky top-0 z-10">
                <Skeleton className="h-9 w-9" />
                <div className='flex-1'>
                    <Skeleton className="h-7 w-48 mb-2" />
                    <Skeleton className="h-5 w-24" />
                </div>
                <Skeleton className="h-8 w-20" />
                <Skeleton className="h-9 w-20" />
            </header>
            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6 bg-transparent">
                <Skeleton className="h-48 w-full" />
                <div className="flex gap-2">
                    <Skeleton className="h-10 w-28" />
                    <Skeleton className="h-10 w-28" />
                    <Skeleton className="h-10 w-28" />
                </div>
                <Skeleton className="h-6 w-40 mb-4" />
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <Skeleton className="h-64" />
                    <Skeleton className="h-64" />
                    <Skeleton className="h-64" />
                </div>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <Skeleton className="h-80" />
                    <Skeleton className="h-80" />
                </div>
                <Skeleton className="h-40" />
            </main>
        </div>
    );
}

