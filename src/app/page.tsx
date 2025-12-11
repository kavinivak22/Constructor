'use client';

import { ProjectCard } from '@/components/dashboard/project-card';
import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import { Calendar, ClipboardCheck, FileText, Package, Plus, Receipt, UserPlus, AlertTriangle, Hammer, Image as ImageIcon } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import Link from 'next/link';
import { AlertFlipper } from '@/components/dashboard/alert-flipper';
import { useProjects, useRecentWorklogs } from '@/hooks/queries';
import { QuickActions } from '@/components/dashboard/quick-actions';
import { formatDistanceToNow } from 'date-fns';
import { useTranslation } from '@/lib/i18n/language-context';

const tasks = [
    { id: "task1", label: "Approve PO #7891" },
    { id: "task2", label: "Call concrete supplier" },
    { id: "task3", label: "Review electrical blueprints" },
]



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
    const { data: projects = [], isLoading } = useProjects();
    const { data: recentWorklogs = [], isLoading: isLoadingWorklogs } = useRecentWorklogs();
    const { t } = useTranslation();

    return (
        <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-secondary">
            <div className="space-y-6">

                {/* Alerts Flipper */}
                <AlertFlipper
                    alerts={alerts}
                    alertVariants={alertVariants}
                    autoplayDelay={5000}
                />


                {/* Quick Actions */}
                <QuickActions />

                {/* Active Projects Section */}
                <div>
                    <div className="section-header">
                        <h2 className="section-title">{t('dashboard.active_projects')}</h2>
                        <Link href="/projects" className="view-all-link">{t('dashboard.view_all')}</Link>
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
                        <Card className="flex flex-col items-center justify-center h-64 text-center p-6">
                            <CardHeader>
                                <CardTitle>{t('dashboard.no_projects')}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="max-w-sm mt-2 text-muted-foreground">{t('dashboard.create_project_desc')}</p>
                                <Link href="/projects/create" className='mt-4'>
                                    <Button>
                                        <Plus className="mr-2 h-4 w-4" />
                                        {t('dashboard.create_project')}
                                    </Button>
                                </Link>
                            </CardContent>
                        </Card>
                    )}
                </div>

                {/* My Tasks & Recent Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('dashboard.my_tasks')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-3">
                                {tasks.map(task => (
                                    <div key={task.id} className="flex items-center space-x-3">
                                        <Checkbox id={task.id} />
                                        <label htmlFor={task.id} className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                            {task.label}
                                        </label>
                                    </div>
                                ))}
                            </div>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('dashboard.recent_activity')}</CardTitle>
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
                                    <p className="text-sm text-muted-foreground">{t('dashboard.no_activity')}</p>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </main>
    );
}
