'use client';

import { useParams, useRouter } from 'next/navigation';
import { type Project } from '@/lib/data';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Calendar, Users, DollarSign, Edit, CheckCircle, Clock, ArrowLeft } from 'lucide-react';
import { Progress } from '@/components/ui/progress';
import { format } from 'date-fns';
import { Button } from '@/components/ui/button';
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel';
import { Checkbox } from '@/components/ui/checkbox';
import Image from 'next/image';
import Link from 'next/link';
import { useProject, useProjectMembersCount } from '@/hooks/queries';

const recentUpdates = [
    {
        progress: 65,
        title: 'Foundation work completed',
        date: '2024-03-15',
        description: 'Successfully completed the foundation work including concrete pouring and reinforcement installation.',
        imageUrl: 'https://picsum.photos/seed/1/600/400'
    },
    {
        progress: 55,
        title: 'Structural framework 80% done',
        date: '2024-03-01',
        description: 'Major progress on structural framework. Steel beams installed and welding work in progress.',
        imageUrl: 'https://picsum.photos/seed/2/600/400'
    },
    {
        progress: 40,
        title: 'Ground preparation finished',
        date: '2024-02-15',
        description: 'Ground leveling and excavation completed. Site ready for foundation work.',
        imageUrl: 'https://picsum.photos/seed/3/600/400'
    },
];

const projectTasks = [
    { id: 'task1', label: 'Complete electrical wiring inspection', date: 'Mar 20, 2024', done: false },
    { id: 'task2', label: 'Order HVAC equipment', date: 'Mar 22, 2024', done: false },
    { id: 'task3', label: 'Submit progress report to client', date: 'Mar 15, 2024', done: true },
    { id: 'task4', label: 'Schedule plumbing installation', date: 'Mar 25, 2024', done: false },
    { id: 'task5', label: 'Review structural engineering plans', date: 'Mar 18, 2024', done: false },
]

const upcomingWorks = [
    { title: 'Roofing Installation', date: 'Apr 1, 2024', duration: '2 weeks', priority: 'high' },
    { title: 'Interior Wall Framing', date: 'Apr 15, 2024', duration: '3 weeks', priority: 'medium' },
    { title: 'Electrical Rough-in', date: 'May 1, 2024', duration: '1 week', priority: 'high' },
    { title: 'Plumbing Rough-in', date: 'May 8, 2024', duration: '1 week', priority: 'high' },
]


export default function ProjectDetailsPage() {
    const { projectId } = useParams();
    const router = useRouter();
    const projectIdString = Array.isArray(projectId) ? projectId[0] : projectId;

    // Use React Query hooks for data fetching
    const { data: project, isLoading, error } = useProject(projectIdString);
    const { data: memberCount = 0 } = useProjectMembersCount(projectIdString);

    const getFormattedDate = (date: string | undefined, formatStr: string = 'MMM d, yyyy') => {
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
                <header className="flex items-center gap-2 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
                        Project Not Found
                    </h1>
                </header>
                <main className="flex-1 p-4 overflow-y-auto md:p-6 bg-secondary">
                    <Card>
                        <CardContent className='p-6'>
                            <p>The project you are looking for does not exist or you do not have permission to view it.</p>
                        </CardContent>
                    </Card>
                </main>
            </div>
        );
    }

    return (
        <div className="flex flex-col h-full bg-secondary">
            <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                <Button variant="ghost" size="icon" onClick={() => router.back()}>
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div className="flex-1">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline truncate">
                        {project.name}
                    </h1>
                    <p className="text-sm text-muted-foreground">ABC Corp</p>
                </div>
                <div className="flex items-center gap-2 ml-auto">
                    <div className="text-sm text-muted-foreground hidden sm:block">Current Stage:</div>
                    <Badge variant="outline" className="capitalize shrink-0">
                        Foundation
                    </Badge>
                </div>
                <Button variant="outline" size="sm" asChild>
                    <Link href="#">
                        <Edit className="mr-2 h-3 w-3" />
                        Edit
                    </Link>
                </Button>
            </header>
            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6">
                <Card>
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
                                <DollarSign className="h-4 w-4" />
                                <div>
                                    <p className='text-xs'>Budget</p>
                                    <p className='font-semibold text-foreground'>$850k</p>
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
                    <Button variant="outline">Add Update</Button>
                </div>

                <div>
                    <h2 className="text-xl font-bold mb-4">Recent Updates</h2>
                    <Carousel opts={{ align: "start", loop: true }} className="w-full">
                        <CarouselContent className="-ml-4">
                            {recentUpdates.map((update, index) => (
                                <CarouselItem key={index} className="pl-4 md:basis-1/2 lg:basis-1/3">
                                    <Card>
                                        <CardContent className="p-0">
                                            <Image src={update.imageUrl} alt={update.title} width={600} height={400} className="rounded-t-lg aspect-[3/2] object-cover" data-ai-hint="construction update" />
                                            <div className='p-4'>
                                                <div className="flex justify-between items-center text-xs text-muted-foreground mb-2">
                                                    <p className='font-semibold text-primary'>{update.progress}% Complete</p>
                                                    <p>{format(new Date(update.date), 'MMM dd, yyyy')}</p>
                                                </div>
                                                <h3 className="font-bold mb-1">{update.title}</h3>
                                                <p className="text-sm text-muted-foreground">{update.description}</p>
                                            </div>
                                        </CardContent>
                                    </Card>
                                </CarouselItem>
                            ))}
                        </CarouselContent>
                        <CarouselPrevious variant="ghost" className="absolute left-2 top-1/2 -translate-y-1/2 z-10 hidden sm:flex h-8 w-8 rounded-full bg-white/50 hover:bg-white/75 text-foreground" />
                        <CarouselNext variant="ghost" className="absolute right-2 top-1/2 -translate-y-1/2 z-10 hidden sm:flex h-8 w-8 rounded-full bg-white/50 hover:bg-white/75 text-foreground" />
                    </Carousel>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <CheckCircle className="text-primary h-5 w-5" />
                                Project Tasks
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            {projectTasks.map(task => (
                                <div key={task.id} className="flex items-start gap-3">
                                    <Checkbox id={task.id} checked={task.done} className="mt-1" />
                                    <div>
                                        <label htmlFor={task.id} className={`font-medium ${task.done ? 'line-through text-muted-foreground' : ''}`}>{task.label}</label>
                                        <p className="text-xs text-muted-foreground flex items-center gap-1"><Clock className="h-3 w-3" /> {task.date}</p>
                                    </div>
                                </div>
                            ))}
                            <Button variant="outline" className="w-full mt-4">Add Task</Button>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader>
                            <CardTitle>Upcoming Works</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            {upcomingWorks.map(work => (
                                <div key={work.title} className="flex items-start justify-between">
                                    <div>
                                        <p className="font-semibold">{work.title}</p>
                                        <p className="text-sm text-muted-foreground flex items-center gap-2">
                                            <Calendar className="h-4 w-4" />
                                            <span>{work.date}</span>
                                            <Clock className="h-4 w-4" />
                                            <span>{work.duration}</span>
                                        </p>
                                    </div>
                                    <Badge variant={work.priority === 'high' ? 'destructive' : 'secondary'} className="capitalize">{work.priority}</Badge>
                                </div>
                            ))}
                        </CardContent>
                    </Card>
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Budget Overview</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-2">
                            <div className="flex justify-between text-sm">
                                <span>Total Budget</span>
                                <span>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(budget)}</span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <span>Amount Spent</span>
                                <span>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(spent)}</span>
                            </div>
                            <div className="relative h-4 rounded-full bg-muted overflow-hidden">
                                <div className="absolute top-0 left-0 h-full bg-primary" style={{ width: `${spentPercentage}%` }}></div>
                            </div>
                            <div className="flex justify-between text-sm font-bold">
                                <span>Remaining</span>
                                <span>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(remaining)}</span>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </main>
        </div>
    );
}

function ProjectDetailsSkeleton() {
    return (
        <div className="flex flex-col h-full bg-secondary">
            <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                <Skeleton className="h-9 w-9" />
                <div className='flex-1'>
                    <Skeleton className="h-7 w-48 mb-2" />
                    <Skeleton className="h-5 w-24" />
                </div>
                <Skeleton className="h-8 w-20" />
                <Skeleton className="h-9 w-20" />
            </header>
            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6">
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

