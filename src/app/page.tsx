
'use client';

import { ProjectCard } from '@/components/dashboard/project-card';
import { Project } from '@/lib/data';
import { Button } from '@/components/ui/button';
import { Calendar, ClipboardCheck, FileText, Package, Plus, Receipt, UserPlus, AlertTriangle } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import Link from 'next/link';
import { AlertFlipper } from '@/components/dashboard/alert-flipper';
import { useProjects } from '@/hooks/queries';

const tasks = [
    { id: "task1", label: "Approve PO #7891" },
    { id: "task2", label: "Call concrete supplier" },
    { id: "task3", label: "Review electrical blueprints" },
]

const activities = [
    { icon: FileText, text: "Downtown Office Complex updated to 65% completion", time: "31 minutes ago", color: "text-blue-500" },
    { icon: Package, text: "Low stock alert: Cement (50 bags remaining)", time: "about 2 hours ago", color: "text-orange-500" },
    { icon: Receipt, text: "New expense added: Equipment rental - $2,500", time: "about 4 hours ago", color: "text-green-500" },
    { icon: UserPlus, text: "John Smith added to Residential Tower Project", time: "about 8 hours ago", color: "text-purple-500" },
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

export default function DashboardPage() {
    const { data: projects = [], isLoading } = useProjects();

    return (
        <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-secondary">
            <div className="space-y-6">

                {/* Alerts Flipper */}
                <AlertFlipper
                    alerts={alerts}
                    alertVariants={alertVariants}
                    autoplayDelay={5000}
                />

                {/* Active Projects Section */}
                <div>
                    <div className="section-header">
                        <h2 className="section-title">Active Projects</h2>
                        <Link href="/projects" className="view-all-link">View All</Link>
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
                                <CardTitle>No Projects Yet</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="max-w-sm mt-2 text-muted-foreground">Get started by creating your first project.</p>
                                <Link href="/projects/create" className='mt-4'>
                                    <Button>
                                        <Plus className="mr-2 h-4 w-4" />
                                        Create Project
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
                            <CardTitle>My Tasks</CardTitle>
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
                            <CardTitle>Recent Activity</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-4">
                                {activities.map((activity, index) => (
                                    <div key={index} className="flex items-start space-x-3">
                                        <div className={`mt-1 ${activity.color}`}>
                                            <activity.icon className="h-4 w-4" />
                                        </div>
                                        <div>
                                            <p className="text-sm">{activity.text}</p>
                                            <p className="text-xs text-muted-foreground">{activity.time}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </main>
    );
}
