'use client';

import { useState, useEffect } from 'react';
import { useSupabase } from '@/supabase/provider';
import { ProjectCard } from '@/components/dashboard/project-card';
import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Calendar, ClipboardCheck, FileText, Package, Plus, Receipt, UserPlus, AlertTriangle, Hammer, Image as ImageIcon, Loader2, Pencil, Trash2, Check, X, PhoneCall } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import Link from 'next/link';
import { AlertFlipper } from '@/components/dashboard/alert-flipper';
import { useProjects, useRecentWorklogs } from '@/hooks/queries';
import { QuickActions } from '@/components/dashboard/quick-actions';
import { formatDistanceToNow } from 'date-fns';
import { getPersonalTasks, createPersonalTask, toggleTaskStatus, deleteTaskAction, editTaskAction, type TaskItem } from '@/app/actions/tasks';
import { useToast } from '@/hooks/use-toast';
import { useI18n } from '@/lib/i18n-context';




const alerts = [
    {
        id: 'alert1',
        icon: AlertTriangle,
        title: 'Low Stock Warning',
        description: 'Cement stock is running low (20 bags left) for the Downtown Office Reno project.',
        time: '2 hours ago',
        variant: 'warning' as const,
    },
    {
        id: 'alert2',
        icon: FileText,
        title: 'Approval Overdue',
        description: 'Purchase Order #7890 for the Suburban Villa is awaiting your approval.',
        time: '1 day ago',
        variant: 'danger' as const,
    },
    {
        id: 'alert3',
        icon: ClipboardCheck,
        title: 'Task Nearing Deadline',
        description: 'Task "Finalize electrical plan" for Coastal Bridge Repair is due tomorrow.',
        time: '22 hours ago',
        variant: 'warning' as const,
    },
];

const alertVariants = {
    warning: "bg-yellow-50 border-yellow-200 text-yellow-800",
    danger: "bg-red-50 border-red-200 text-red-800",
}

function getWorklogDescription(worklog: any) {
    const parts = [];

    // 1. Labor
    if (worklog.labor && worklog.labor.length > 0) {
        const firstLabor = worklog.labor[0];
        if (firstLabor.work_description) {
            parts.push(`${firstLabor.contractor_name}: ${firstLabor.work_description}`);
        } else {
            parts.push(`${firstLabor.contractor_name} worked on site`);
        }
    }

    // 2. Materials
    if (worklog.materials && worklog.materials.length > 0) {
        const firstMaterial = worklog.materials[0];
        const count = worklog.materials.length;
        let materialText = `used ${firstMaterial.quantity_consumed} ${firstMaterial.unit || ''} of ${firstMaterial.material_name}`;
        if (count > 1) {
            materialText += ` and ${count - 1} other items`;
        }
        parts.push(materialText);
    }

    // 3. Photos
    if (worklog.photos && worklog.photos.length > 0) {
        const count = worklog.photos.length;
        parts.push(`added ${count} photo${count > 1 ? 's' : ''}`);
    }

    if (parts.length === 0) {
        return worklog.title;
    }

    // Join parts naturally
    if (parts.length === 1) {
        // Capitalize first letter if it's not a name (Labor usually starts with name, others might not)
        // But "used..." and "added..." are lowercase, so we should capitalize them if they are first.
        // Labor already starts with Name (Capitalized).
        const text = parts[0];
        return text.charAt(0).toUpperCase() + text.slice(1);
    }

    if (parts.length === 2) {
        const first = parts[0];
        return first.charAt(0).toUpperCase() + first.slice(1) + ' and ' + parts[1];
    }

    // 3 or more
    const last = parts.pop();
    const first = parts[0];
    parts[0] = first.charAt(0).toUpperCase() + first.slice(1);
    return parts.join(', ') + ', and ' + last;
}

function getWorklogIcon(worklog: any) {
    if (worklog.labor && worklog.labor.length > 0) return Hammer;
    if (worklog.materials && worklog.materials.length > 0) return Package;
    if (worklog.photos && worklog.photos.length > 0) return ImageIcon;
    return FileText;
}

export default function DashboardPage() {
    const { t } = useI18n();
    const { data: projects = [], isLoading } = useProjects();
    const { data: recentWorklogs = [], isLoading: isLoadingWorklogs } = useRecentWorklogs();
    const { supabase, user } = useSupabase();
    const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string } | null>(null);

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

    const { toast } = useToast();
    const [personalTasks, setPersonalTasks] = useState<TaskItem[]>([]);
    const [newTaskTitle, setNewTaskTitle] = useState('');
    const [isCreatingTask, setIsCreatingTask] = useState(false);
    const [isLoadingTasks, setIsLoadingTasks] = useState(true);
    const [tomorrowTasksCount, setTomorrowTasksCount] = useState<number | null>(null);
    const [tomorrowDateLabel, setTomorrowDateLabel] = useState('');

    useEffect(() => {
        if (!user) return;
        const fetchPersonalTasks = async () => {
            setIsLoadingTasks(true);
            const data = await getPersonalTasks();
            setPersonalTasks(data);
            setIsLoadingTasks(false);
        };
        fetchPersonalTasks();
    }, [user]);

    useEffect(() => {
        if (!user) return;
        const fetchTomorrowCount = async () => {
            try {
                const todayDate = new Date();
                const tomorrowDate = new Date(todayDate);
                tomorrowDate.setDate(todayDate.getDate() + 1);
                
                // Format matching YYYY-MM-DD
                const yyyy = tomorrowDate.getFullYear();
                const mm = String(tomorrowDate.getMonth() + 1).padStart(2, '0');
                const dd = String(tomorrowDate.getDate()).padStart(2, '0');
                const tomorrowStr = `${yyyy}-${mm}-${dd}`;
                
                setTomorrowDateLabel(tomorrowDate.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' }));
                
                const { data: profile } = await supabase
                    .from('users')
                    .select('company_id')
                    .eq('id', user.id)
                    .single();
                    
                if (profile?.company_id) {
                    const { data: projectsData } = await supabase
                        .from('projects')
                        .select('id')
                        .eq('company_id', profile.company_id);
                        
                    const projectIds = (projectsData || []).map(p => p.id);
                    if (projectIds.length > 0) {
                        const { count, error } = await supabase
                            .from('tasks')
                            .select('*', { count: 'exact', head: true })
                            .in('project_id', projectIds)
                            .eq('due_date', tomorrowStr);
                            
                        if (!error && count !== null) {
                            setTomorrowTasksCount(count);
                        } else {
                            setTomorrowTasksCount(0);
                        }
                    } else {
                        setTomorrowTasksCount(0);
                    }
                } else {
                    setTomorrowTasksCount(0);
                }
            } catch (e) {
                console.error('Error fetching tomorrow tasks count:', e);
                setTomorrowTasksCount(0);
            }
        };
        fetchTomorrowCount();
    }, [user, supabase]);

    const handleToggleTask = async (taskId: string, currentStatus: string) => {
        setPersonalTasks(prev =>
            prev.map(t => t.id === taskId ? { ...t, status: currentStatus === 'completed' ? 'pending' : 'completed' } : t)
        );

        const result = await toggleTaskStatus(taskId, currentStatus);
        if (!result.success) {
            toast({
                title: 'Error',
                description: result.error || 'Failed to update task.',
                variant: 'destructive'
            });
            const data = await getPersonalTasks();
            setPersonalTasks(data);
        }
    };

    const [editingTaskId, setEditingTaskId] = useState<string | null>(null);
    const [editingTaskTitle, setEditingTaskTitle] = useState('');

    const startEditing = (id: string, currentTitle: string) => {
        setEditingTaskId(id);
        setEditingTaskTitle(currentTitle);
    };

    const handleSaveEdit = async (e: React.FormEvent, taskId: string) => {
        e.preventDefault();
        if (!editingTaskTitle.trim()) return;

        setPersonalTasks(prev =>
            prev.map(t => t.id === taskId ? { ...t, title: editingTaskTitle.trim() } : t)
        );
        setEditingTaskId(null);

        const result = await editTaskAction(taskId, { title: editingTaskTitle.trim() });
        if (!result.success) {
            toast({
                title: 'Error',
                description: result.error || 'Failed to edit task.',
                variant: 'destructive'
            });
            const data = await getPersonalTasks();
            setPersonalTasks(data);
        } else {
            toast({
                title: 'Task Updated',
                description: 'Personal task has been updated.',
            });
        }
    };

    const handleDeleteTask = async (taskId: string) => {
        setPersonalTasks(prev => prev.filter(t => t.id !== taskId));

        const result = await deleteTaskAction(taskId);
        if (!result.success) {
            toast({
                title: 'Error',
                description: result.error || 'Failed to delete task.',
                variant: 'destructive'
            });
            const data = await getPersonalTasks();
            setPersonalTasks(data);
        } else {
            toast({
                title: 'Task Deleted',
                description: 'Personal task has been removed.',
            });
        }
    };

    const handleAddTask = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newTaskTitle.trim()) return;

        setIsCreatingTask(true);
        const result = await createPersonalTask(newTaskTitle.trim());
        setIsCreatingTask(false);

        if (result.success && result.task) {
            setPersonalTasks(prev => [result.task as TaskItem, ...prev]);
            setNewTaskTitle('');
            toast({
                title: 'Task Created',
                description: 'Personal task has been added.',
            });
        } else {
            toast({
                title: 'Error',
                description: result.error || 'Failed to create task.',
                variant: 'destructive',
            });
        }
    };

    const isAdminOrManager = currentUserProfile?.role === 'admin' || currentUserProfile?.role === 'manager';

    return (
        <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-transparent">
            <div className="space-y-6">

                {/* Alerts Flipper */}
                <AlertFlipper
                    alerts={alerts}
                    alertVariants={alertVariants}
                    autoplayDelay={5000}
                />


                {/* Quick Actions */}
                <QuickActions />

                {/* Tomorrow's Work Prep Card (Minimized to conserve space) */}
                <Card className="glass-card overflow-hidden border-primary/20 bg-gradient-to-r from-primary/10 via-primary/5 to-transparent shadow-sm">
                    <CardContent className="p-3.5 sm:p-4">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                            <div className="flex items-center gap-3">
                                <div className="h-9 w-9 rounded-xl bg-primary/15 text-primary flex items-center justify-center shrink-0">
                                    <PhoneCall className="h-4 w-4" />
                                </div>
                                <div className="space-y-0.5">
                                    <div className="flex items-center gap-2">
                                        <h3 className="text-sm font-bold text-foreground">
                                            {t('tomorrowPrepTitle', "Tomorrow's Work Preparation")}
                                        </h3>
                                        <Badge variant="outline" className="text-[10px] py-0 px-1.5 h-4 border-primary/30 text-primary bg-primary/10">
                                            {tomorrowDateLabel || t('tomorrow', 'Tomorrow')}
                                        </Badge>
                                    </div>
                                    <p className="text-xs text-muted-foreground">
                                        {tomorrowTasksCount === null ? (
                                            t('loadingSchedule', "Loading tomorrow's schedule...")
                                        ) : tomorrowTasksCount > 0 ? (
                                            `${tomorrowTasksCount} ${t('tasksScheduledShort', 'tasks scheduled for tomorrow. Call assignees and contractors to prepare.')}`
                                        ) : (
                                            t('noPlansForTomorrow', 'No plans for tomorrow yet!')
                                        )}
                                    </p>
                                </div>
                            </div>

                            <Button asChild size="sm" className="shrink-0 h-8 text-xs rounded-lg px-3 group">
                                <Link href="/work-prep">
                                    <PhoneCall className="mr-1.5 h-3.5 w-3.5 transition-transform group-hover:scale-110" />
                                    {tomorrowTasksCount && tomorrowTasksCount > 0 ? t('viewPrepBoard', 'View Prep Board') : t('startPrep', 'Start Prep')}
                                </Link>
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                {/* Active Projects Section */}
                <div>
                    <div className="section-header">
                        <h2 className="section-title">{t('activeProjects', 'Active Projects')}</h2>
                        <Link href="/projects" className="view-all-link">{t('viewAll', 'View All')}</Link>
                    </div>
                    {isLoading && (
                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                            {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-[22rem]" />)}
                        </div>
                    )}
                    {!isLoading && projects.length > 0 && (
                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                            {projects.map((project) => (
                                <ProjectCard key={project.id} project={project} />
                            ))}
                        </div>
                    )}
                    {!isLoading && projects.length === 0 && (
                        <Card className="glass-card flex flex-col items-center justify-center h-64 text-center p-6">
                            <CardHeader>
                                <CardTitle>{t('noProjectsYet', 'No Projects Yet')}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="max-w-sm mt-2 text-muted-foreground">{t('getStartedProject', 'Get started by creating your first project.')}</p>
                                {isAdminOrManager && (
                                    <Link href="/projects/create" className='mt-4'>
                                        <Button>
                                            <Plus className="mr-2 h-4 w-4" />
                                            {t('createProject', 'Create Project')}
                                        </Button>
                                    </Link>
                                )}
                            </CardContent>
                        </Card>
                    )}
                </div>

                {/* My Tasks & Recent Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <Card className="glass-card">
                        <CardHeader>
                            <CardTitle>{t('myTasks', 'My Tasks')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleAddTask} className="flex gap-2 mb-4">
                                <Input
                                    type="text"
                                    placeholder={t('addTaskPlaceholder', 'Add a new personal task...')}
                                    value={newTaskTitle}
                                    onChange={(e) => setNewTaskTitle(e.target.value)}
                                    disabled={isCreatingTask}
                                    className="h-9 flex-1"
                                />
                                <Button type="submit" size="sm" disabled={isCreatingTask || !newTaskTitle.trim()}>
                                    {isCreatingTask ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
                                </Button>
                            </form>

                            {isLoadingTasks ? (
                                <div className="space-y-3">
                                    {[...Array(3)].map((_, i) => (
                                        <div key={i} className="flex items-center space-x-3">
                                            <Skeleton className="h-4 w-4 rounded" />
                                            <Skeleton className="h-4 w-48" />
                                        </div>
                                    ))}
                                </div>
                            ) : personalTasks.length > 0 ? (
                                <div className="space-y-3 max-h-[250px] overflow-y-auto pr-1">
                                    {personalTasks.map(task => (
                                        <div key={task.id} className="flex items-center justify-between py-1 group">
                                            {editingTaskId === task.id ? (
                                                <form onSubmit={(e) => handleSaveEdit(e, task.id)} className="flex items-center gap-2 w-full">
                                                    <Input
                                                        type="text"
                                                        value={editingTaskTitle}
                                                        onChange={(e) => setEditingTaskTitle(e.target.value)}
                                                        className="h-8 py-1 px-2 text-sm flex-1"
                                                        autoFocus
                                                    />
                                                    <Button type="submit" size="sm" variant="ghost" className="h-8 w-8 p-0">
                                                        <Check className="h-4 w-4 text-green-500" />
                                                     </Button>
                                                    <Button type="button" size="sm" variant="ghost" className="h-8 w-8 p-0" onClick={() => setEditingTaskId(null)}>
                                                        <X className="h-4 w-4 text-red-500" />
                                                    </Button>
                                                </form>
                                            ) : (
                                                <>
                                                    <div className="flex items-center space-x-3">
                                                        <Checkbox
                                                            id={task.id}
                                                            checked={task.status === 'completed'}
                                                            onCheckedChange={() => handleToggleTask(task.id, task.status)}
                                                        />
                                                        <label
                                                            htmlFor={task.id}
                                                            className={`text-sm font-medium leading-none cursor-pointer select-none transition-all ${
                                                                task.status === 'completed' ? 'line-through text-muted-foreground/60' : 'text-foreground/90'
                                                            }`}
                                                        >
                                                            {task.title}
                                                        </label>
                                                    </div>
                                                    <div className="opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                                                        <Button
                                                            size="icon"
                                                            variant="ghost"
                                                            className="h-7 w-7 p-0"
                                                            onClick={() => startEditing(task.id, task.title)}
                                                        >
                                                            <Pencil className="h-3.5 w-3.5" />
                                                        </Button>
                                                        <Button
                                                            size="icon"
                                                            variant="ghost"
                                                            className="h-7 w-7 p-0 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/20"
                                                            onClick={() => handleDeleteTask(task.id)}
                                                        >
                                                            <Trash2 className="h-3.5 w-3.5" />
                                                        </Button>
                                                    </div>
                                                </>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="text-sm text-muted-foreground text-center py-4">{t('noTasksYet', 'No tasks yet. Add one above to get started!')}</p>
                            )}
                        </CardContent>
                    </Card>
                    <Card className="glass-card">
                        <CardHeader>
                            <CardTitle>{t('recentActivity', 'Recent Activity')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-4">
                                {isLoadingWorklogs ? (
                                    [...Array(3)].map((_, i) => (
                                        <div key={i} className="flex items-start space-x-3">
                                            <Skeleton className="h-4 w-4 rounded-full" />
                                            <div className="space-y-1">
                                                <Skeleton className="h-4 w-48" />
                                                <Skeleton className="h-3 w-24" />
                                            </div>
                                        </div>
                                    ))
                                ) : recentWorklogs.length > 0 ? (
                                    recentWorklogs.map((worklog: any, index: number) => {
                                        const Icon = getWorklogIcon(worklog);
                                        return (
                                            <Link
                                                href={`/worklog?projectId=${worklog.project_id}&worklogId=${worklog.id}`}
                                                key={worklog.id}
                                                className="relative flex items-start gap-4 group cursor-pointer p-3 -mx-3 rounded-lg hover:bg-muted/50 transition-all duration-200"
                                            >
                                                {/* Timeline Line */}
                                                {index !== recentWorklogs.length - 1 && (
                                                    <div className="absolute left-[21px] top-10 bottom-[-20px] w-px bg-border/50 group-hover:bg-border transition-colors" />
                                                )}

                                                {/* Icon Bubble */}
                                                <div className="relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border bg-background shadow-sm group-hover:border-primary/50 group-hover:text-primary transition-colors">
                                                    <Icon className="h-4 w-4" />
                                                </div>

                                                <div className="flex-1 space-y-1 pt-0.5">
                                                    <p className="text-sm leading-tight text-foreground/90 group-hover:text-primary transition-colors">
                                                        <span className="font-semibold text-foreground">{worklog.project?.name}</span>
                                                        <span className="mx-1.5 text-muted-foreground/40">•</span>
                                                        {getWorklogDescription(worklog)}
                                                    </p>
                                                    <p className="text-[11px] font-medium text-muted-foreground/60 uppercase tracking-wider">
                                                        {formatDistanceToNow(new Date(worklog.date), { addSuffix: true })}
                                                    </p>
                                                </div>
                                            </Link>
                                        );
                                    })
                                ) : (
                                    <p className="text-sm text-muted-foreground">{t('noRecentActivity', 'No recent activity.')}</p>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </main>
    );
}
