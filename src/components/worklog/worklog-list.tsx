'use client';

import { useEffect, useState, useMemo, useRef } from 'react';
import { format, isSameDay } from 'date-fns';
import { Users, Package, Image as ImageIcon, Calendar as CalendarIcon, Clock, ArrowRight, Search, X, MoreVertical, Edit, Trash2, AlertTriangle } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { getWorklogs, deleteWorklog } from '@/app/actions/worklogs';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Calendar } from '@/components/ui/calendar';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover';
import {
    Carousel,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from "@/components/ui/carousel"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import Autoplay from "embla-carousel-autoplay"
import Image from 'next/image';
import { cn } from '@/lib/utils';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';
import { useToast } from '@/components/ui/use-toast';

interface WorklogListProps {
    projectId: string;
    refreshTrigger?: number; // Prop to trigger refresh
}

export function WorklogList({ projectId, refreshTrigger }: WorklogListProps) {
    const [worklogs, setWorklogs] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [date, setDate] = useState<Date | undefined>(undefined);
    const [editingWorklog, setEditingWorklog] = useState<any | null>(null);
    const [deletingWorklogId, setDeletingWorklogId] = useState<string | null>(null);
    const { toast } = useToast();

    // Key to force re-render/fetch
    const [fetchKey, setFetchKey] = useState(0);

    const fetchWorklogs = async () => {
        setLoading(true);
        const result = await getWorklogs(projectId);
        if (result.success && result.data) {
            setWorklogs(result.data);
        }
        setLoading(false);
    };

    useEffect(() => {
        if (projectId) {
            fetchWorklogs();
        }
    }, [projectId, refreshTrigger, fetchKey]);

    const handleDelete = async () => {
        if (!deletingWorklogId) return;

        try {
            const result = await deleteWorklog(deletingWorklogId);
            if (result.success) {
                toast({ title: "Worklog deleted", description: "The worklog has been permanently removed." });
                setFetchKey(prev => prev + 1);
            } else {
                toast({ variant: "destructive", title: "Error", description: result.error || "Failed to delete worklog" });
            }
        } catch (error) {
            toast({ variant: "destructive", title: "Error", description: "An unexpected error occurred" });
        } finally {
            setDeletingWorklogId(null);
        }
    };

    const filteredWorklogs = useMemo(() => {
        let filtered = worklogs;

        // Filter by Date
        if (date) {
            filtered = filtered.filter(log => isSameDay(new Date(log.date), date));
        }

        // Filter by Search Term
        if (searchTerm) {
            const query = searchTerm.toLowerCase();
            filtered = filtered.filter(log => {
                const title = log.title || '';
                const description = log.labor?.map((l: any) => l.work_description).join(' ') || '';
                return title.toLowerCase().includes(query) || description.toLowerCase().includes(query);
            });
        }

        return filtered;
    }, [worklogs, searchTerm, date]);

    if (loading) {
        return (
            <div className="space-y-6">
                <div className="flex items-center space-x-2">
                    <Skeleton className="h-10 w-full" />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {[...Array(6)].map((_, i) => (
                        <div key={i} className="space-y-3">
                            <Skeleton className="h-48 w-full rounded-xl" />
                            <Skeleton className="h-4 w-3/4" />
                            <Skeleton className="h-4 w-1/2" />
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (worklogs.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center text-muted-foreground border-2 border-dashed rounded-xl bg-card/50">
                <CalendarIcon className="h-10 w-10 mb-4 opacity-50" />
                <h3 className="text-lg font-semibold mb-1">No Daily Logs Yet</h3>
                <p>Start documenting progress by creating your first daily worklog.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6 pb-10">
            {/* Filters Bar */}
            <div className="flex items-center gap-2">
                {/* Search Bar */}
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                        placeholder="Search logs..."
                        className="pl-9"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>

                {/* Date Filter */}
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            variant={"outline"}
                            className={cn(
                                "justify-center text-left font-normal",
                                "w-10 px-0 md:w-[240px] md:px-4 md:justify-start", // Mobile vs Desktop styles
                                !date && "text-muted-foreground"
                            )}
                        >
                            <CalendarIcon className={cn("h-4 w-4", "md:mr-2")} />
                            <span className="hidden md:inline">
                                {date ? format(date, "PPP") : "Pick a date"}
                            </span>
                        </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="end">
                        <Calendar
                            mode="single"
                            selected={date}
                            onSelect={setDate}
                            initialFocus
                        />
                    </PopoverContent>
                </Popover>

                {/* Clear Filters Button (only show if any filter is active) */}
                {(searchTerm || date) && (
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                            setSearchTerm('');
                            setDate(undefined);
                        }}
                        title="Clear filters"
                    >
                        <X className="h-4 w-4" />
                    </Button>
                )}
            </div>

            {filteredWorklogs.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
                    <Search className="h-8 w-8 mb-3 opacity-50" />
                    <p>No worklogs found matching your filters</p>
                    <Button variant="link" onClick={() => { setSearchTerm(''); setDate(undefined); }}>
                        Clear all filters
                    </Button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredWorklogs.map((log, index) => (
                        <WorklogFeedCard
                            key={log.id}
                            worklog={log}
                            index={index}
                            onEdit={() => setEditingWorklog(log)}
                            onDelete={() => setDeletingWorklogId(log.id)}
                        />
                    ))}
                </div>
            )}

            {/* Hidden Edit Dialog Trigger */}
            {editingWorklog && (
                <CreateWorklogDialog
                    projectId={projectId}
                    initialData={editingWorklog}
                    worklogId={editingWorklog.id}
                    trigger={<span className="hidden" />} // Invisible trigger, controlled by state inside dialog? 
                    // Actually CreateWorklogDialog controls its own open state usually. 
                    // We need to mount it to open it. 
                    // A better pattern for the Refactored Dialog might be to accept 'open' prop, 
                    // but for minimal changes, we can rely on it mounting.
                    // Wait, the dialog has internal state. We need to pass a key to reset it.
                    key={editingWorklog.id}
                    onSuccess={() => {
                        setEditingWorklog(null);
                        setFetchKey(prev => prev + 1);
                    }}
                />
            )}

            {/* We force the dialog to open by passing a ref or controlled prop? 
                The Refactored code has `const [open, setOpen] = useState(false);`
                To make it open immediately for editing, we might need a small tweak or just click the trigger programmatically.
                HACK: The Refactored Dialog sets state on mount if trigger is clicked? No.
                
                Let's use a simpler approach: The Dialog renders, but starts closed. 
                We need to open it.
                I will add an `open` prop to the CreateWorklogDialog in a future iteration if needed.
                For now, let's wrap it in an Effect or just use the `trigger` prop which is a ReactNode.
                
                Actually, the cleanest way without changing the Dialog again is:
                1. Render the Dialog
                2. Use the `trigger` prop to pass a button that we click? No that's messy.
                3. The User clicks "Edit" in dropdown -> we setEditingWorklog -> Dialog mounts.
                BUT Dialog internal state is `false`.
                
                Let's MODIFY `CreateWorklogDialog` to accept `open` control prop or defaultOpen. 
                Actually, I just modified it in Step 252. I can see `const [open, setOpen] = useState(false);`.
                I should have added `managedOpen` prop.
                
                Correction: I will update `CreateWorklogDialog` in next step to accept `defaultOpen` or `controlledOpen`.
                OR, I can just use a `ref` to click the trigger.
                Let's stick to modifying `WorklogList` now, and I will do a quick patch on `CreateWorklogDialog` to accept `defaultOpen`.
             */}

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={!!deletingWorklogId} onOpenChange={(open) => !open && setDeletingWorklogId(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Delete Worklog?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete this daily log and all associated data (photos, labor entries, materials).
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {/* Correction: To trigger Edit, I will render the Dialog with a key. 
             If I can pass `defaultOpen={true}` ... I will add that to the Dialog component.
             For now, I'll pass a specific prop that I'll add to the Dialog: `forceOpen={true}`.
             */}
            {editingWorklog && (
                <CreateWorklogDialog
                    projectId={projectId}
                    initialData={editingWorklog}
                    worklogId={editingWorklog.id}
                    onSuccess={() => {
                        setEditingWorklog(null);
                        setFetchKey(prev => prev + 1);
                    }}
                    // @ts-ignore - function signature update coming next
                    forceOpen={true}
                />
            )}
        </div>
    );
}

function WorklogFeedCard({ worklog, index, onEdit, onDelete }: { worklog: any; index: number; onEdit: () => void; onDelete: () => void }) {
    // Totals
    const totalWorkers = worklog.labor.reduce((acc: number, entry: any) => {
        return acc + entry.workers.reduce((wAcc: number, w: any) => wAcc + Number(w.count), 0);
    }, 0);
    const totalMaterials = worklog.materials.length;
    const totalPhotos = worklog.photos.length;

    // Autoplay Plugin Reference
    const plugin = useRef(
        Autoplay({ delay: 3000, stopOnInteraction: true })
    );

    // Title & Description
    let title = worklog.title || 'Daily Log';
    let description = '';

    // Smart Title Logic
    if (!worklog.title || worklog.title === 'Daily Log') {
        const categories = Array.from(new Set(worklog.labor?.map((l: any) => l.category).filter(Boolean))) as string[];
        if (categories.length > 0) {
            title = categories.slice(0, 2).join(' & ') + (categories.length > 2 ? '...' : '') + ' Work';
        } else if (worklog.photos?.[0]?.caption) {
            title = worklog.photos[0].caption;
        }
    }

    const rawDescription = worklog.labor?.map((l: any) => l.work_description).filter(Boolean).join('. ') || '';
    if (rawDescription) {
        description = rawDescription.length > 100 ? rawDescription.substring(0, 100) + '...' : rawDescription;
        if ((!title || title === 'Daily Log') && !description) {
            title = `Log for ${format(new Date(worklog.date), 'MMM d')}`;
        }
    } else {
        description = "No detailed description provided for this day.";
    }

    return (
        <Card className={cn(
            "group overflow-hidden border-border/50 transition-all duration-300 hover:shadow-lg hover:-translate-y-1",
            "flex flex-col h-full relative"
        )}>
            {/* Edit/Delete Menu */}
            <div className="absolute top-2 right-2 z-20 opacity-0 group-hover:opacity-100 transition-opacity">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="secondary" size="icon" className="h-8 w-8 rounded-full bg-background/80 backdrop-blur-sm shadow-sm">
                            <MoreVertical className="h-4 w-4" />
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={onEdit}>
                            <Edit className="mr-2 h-4 w-4" /> Edit
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={onDelete} className="text-destructive focus:text-destructive">
                            <Trash2 className="mr-2 h-4 w-4" /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {/* Image Section */}
            <div className={cn(
                "relative bg-muted overflow-hidden w-full",
                "aspect-[4/3]"
            )}>
                {totalPhotos > 1 ? (
                    <Carousel
                        className="w-full h-full"
                        opts={{ loop: true }}
                        plugins={[plugin.current]}
                        onMouseEnter={plugin.current.stop}
                        onMouseLeave={plugin.current.reset}
                    >
                        <CarouselContent>
                            {worklog.photos.map((photo: any, i: number) => (
                                <CarouselItem key={i} className="pl-0">
                                    <div className="relative w-full aspect-[4/3]">
                                        <Image
                                            src={photo.photo_url}
                                            alt={photo.caption || `Photo ${i + 1}`}
                                            fill
                                            className="object-cover"
                                        />
                                    </div>
                                </CarouselItem>
                            ))}
                        </CarouselContent>
                        <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                            <CarouselPrevious className="left-2 bg-black/50 hover:bg-black/70 border-none text-white h-8 w-8" />
                            <CarouselNext className="right-2 bg-black/50 hover:bg-black/70 border-none text-white h-8 w-8" />
                        </div>
                    </Carousel>
                ) : totalPhotos === 1 ? (
                    <Image
                        src={worklog.photos[0].photo_url}
                        alt="Worklog update"
                        fill
                        className="object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                ) : (
                    <div className="flex items-center justify-center h-full text-muted-foreground/30">
                        <ImageIcon className="h-10 w-10" />
                    </div>
                )}

                {/* Date Badge Overlay */}
                <div className="absolute top-3 left-3 z-10 bg-background/90 backdrop-blur-sm px-3 py-1.5 rounded-md shadow-sm border text-xs font-semibold flex flex-col items-center pointer-events-none">
                    <span className="text-muted-foreground uppercase text-[10px] leading-tight">{format(new Date(worklog.date), 'MMM')}</span>
                    <span className="text-lg leading-none font-bold">{format(new Date(worklog.date), 'dd')}</span>
                </div>
            </div>

            {/* Content Section */}
            <div className="flex flex-col flex-1 p-5">
                <div className="flex items-center gap-2 mb-3">
                    <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto text-muted-foreground font-normal gap-1">
                        <Users className="h-3 w-3" /> {totalWorkers} Workers
                    </Badge>
                    {totalMaterials > 0 && (
                        <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto text-muted-foreground font-normal gap-1">
                            <Package className="h-3 w-3" /> {totalMaterials} Mats
                        </Badge>
                    )}
                </div>

                <h3 className="font-bold text-lg mb-2 leading-tight group-hover:text-primary transition-colors line-clamp-2">
                    {title}
                </h3>

                <p className="text-sm text-muted-foreground line-clamp-3 mb-4 flex-1">
                    {description}
                </p>

                <div className="pt-4 border-t w-full flex items-center justify-between mt-auto">
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                        <Clock className="h-3 w-3" /> Posted {format(new Date(worklog.created_at || worklog.date), 'h:mm a')}
                    </span>
                </div>
            </div>
        </Card>
    );
}
