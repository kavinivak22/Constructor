'use client';

import { useEffect, useState, useMemo, useRef } from 'react';
import { format, isSameDay } from 'date-fns';
import { Users, Package, Image as ImageIcon, Calendar as CalendarIcon, Clock, ArrowRight, Search, X, MoreVertical, Edit, Trash2, AlertTriangle, Maximize2 } from 'lucide-react';
import { FullscreenPhotoViewer } from '@/components/worklog/fullscreen-photo-viewer';
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
import { useToast } from '@/hooks/use-toast';
import { useSupabase } from '@/supabase/provider';

interface WorklogListProps {
    projectId: string;
    refreshTrigger?: number; // Prop to trigger refresh
    highlightWorklogId?: string | null;
}

export function WorklogList({ projectId, refreshTrigger, highlightWorklogId }: WorklogListProps) {
    const { supabase, user } = useSupabase();
    const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string } | null>(null);

    useEffect(() => {
        if (user) {
            const fetchProfile = async () => {
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
        }
    }, [user, supabase]);

    const [worklogs, setWorklogs] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [date, setDate] = useState<Date | undefined>(undefined);
    const [editingWorklog, setEditingWorklog] = useState<any | null>(null);
    const [deletingWorklogId, setDeletingWorklogId] = useState<string | null>(null);
    const { toast } = useToast();

    // Key to force re-render/fetch
    const [fetchKey, setFetchKey] = useState(0);

    // Refs for scrolling
    const worklogRefs = useRef<{ [key: string]: HTMLDivElement | null }>({});

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

    // Scroll to highlighted worklog
    useEffect(() => {
        if (!loading && highlightWorklogId && worklogs.length > 0) {
            const element = worklogRefs.current[highlightWorklogId];
            if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                // Optional: Add a temporary highlight class
                element.classList.add('ring-2', 'ring-primary');
                setTimeout(() => {
                    element.classList.remove('ring-2', 'ring-primary');
                }, 2000);
            }
        }
    }, [loading, highlightWorklogId, worklogs]);

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
        let filtered = [...worklogs];

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

        // Secondary Sort: Ensure for logs on the same date, the last updated/created log appears first!
        filtered.sort((a, b) => {
            const dateA = new Date(a.date).getTime();
            const dateB = new Date(b.date).getTime();
            if (dateA !== dateB) {
                return dateB - dateA;
            }
            const updatedA = new Date(a.updated_at || a.created_at || 0).getTime();
            const updatedB = new Date(b.updated_at || b.created_at || 0).getTime();
            return updatedB - updatedA;
        });

        return filtered;
    }, [worklogs, searchTerm, date]);

    if (loading) {
        return (
            <div className="space-y-6">
                <div className="flex items-center space-x-2">
                    <Skeleton className="h-10 w-full bg-white/10 dark:bg-black/20" />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {[...Array(6)].map((_, i) => (
                        <div key={i} className="space-y-3 p-4 rounded-2xl glass-card border-none bg-white/5 dark:bg-black/20">
                            <Skeleton className="h-48 w-full rounded-xl bg-white/10 dark:bg-black/20" />
                            <Skeleton className="h-4 w-3/4 bg-white/10 dark:bg-black/20 mt-2" />
                            <Skeleton className="h-4 w-1/2 bg-white/10 dark:bg-black/20" />
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (worklogs.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center text-muted-foreground border border-white/10 dark:border-white/5 rounded-2xl glass-card p-8">
                <CalendarIcon className="h-10 w-10 mb-4 opacity-50 text-foreground" />
                <h3 className="text-lg font-semibold mb-1 text-foreground">No Daily Logs Yet</h3>
                <p className="text-muted-foreground">Start documenting progress by creating your first daily worklog.</p>
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
                        className="pl-9 glass border-white/10 dark:border-white/5 focus-visible:ring-primary"
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
                                "justify-center text-left font-normal glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5",
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
                    <PopoverContent className="w-auto p-0 glass border-white/10 dark:border-white/5" align="end">
                        <Calendar
                            mode="single"
                            selected={date}
                            onSelect={setDate}
                            initialFocus
                            className="bg-transparent"
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
                        className="glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5"
                    >
                        <X className="h-4 w-4" />
                    </Button>
                )}
            </div>

            {filteredWorklogs.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground border border-white/10 dark:border-white/5 rounded-2xl glass-card p-8">
                    <Search className="h-8 w-8 mb-3 opacity-50 text-foreground" />
                    <p className="text-muted-foreground">No worklogs found matching your filters</p>
                    <Button variant="link" className="text-primary hover:text-primary/80 font-medium" onClick={() => { setSearchTerm(''); setDate(undefined); }}>
                        Clear all filters
                    </Button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredWorklogs.map((log, index) => (
                        <div key={log.id} ref={(el) => { worklogRefs.current[log.id] = el; }}>
                            <WorklogFeedCard
                                worklog={log}
                                index={index}
                                onEdit={() => setEditingWorklog(log)}
                                onDelete={() => setDeletingWorklogId(log.id)}
                                initiallyExpanded={log.id === highlightWorklogId}
                                currentUserProfile={currentUserProfile}
                            />
                        </div>
                    ))}
                </div>
            )}

            {/* Hidden Edit Dialog Trigger removed */}

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

            {editingWorklog && (
                <CreateWorklogDialog
                    key={editingWorklog.id}
                    projectId={projectId === 'all' ? undefined : projectId}
                    initialData={editingWorklog}
                    worklogId={editingWorklog.id}
                    onSuccess={() => {
                        setEditingWorklog(null);
                        setFetchKey(prev => prev + 1);
                    }}
                    forceOpen={true}
                />
            )}
        </div>
    );
}

function WorklogFeedCard({ 
    worklog, 
    index, 
    onEdit, 
    onDelete, 
    initiallyExpanded = false,
    currentUserProfile
}: { 
    worklog: any; 
    index: number; 
    onEdit: () => void; 
    onDelete: () => void; 
    initiallyExpanded?: boolean; 
    currentUserProfile: { id: string; role: string } | null;
}) {
    const [expanded, setExpanded] = useState(initiallyExpanded);
    const [isFullscreenViewerOpen, setIsFullscreenViewerOpen] = useState(false);
    const [fullscreenPhotoIndex, setFullscreenPhotoIndex] = useState(0);

    useEffect(() => {
        if (initiallyExpanded) {
            setExpanded(true);
        }
    }, [initiallyExpanded]);

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

        const canEditOrDelete = currentUserProfile?.role === 'admin' || currentUserProfile?.id === worklog.created_by;

        return (
            <div className="group overflow-hidden glass-card flex flex-col h-full relative border-none bg-transparent">
                {/* Edit/Delete Menu */}
                {canEditOrDelete && (
                    <div className="absolute top-2 right-2 z-20 opacity-0 group-hover:opacity-100 transition-opacity">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="secondary" size="icon" className="h-8 w-8 rounded-full glass border border-white/10 dark:border-white/5 shadow-sm hover:bg-white/20">
                                    <MoreVertical className="h-4 w-4" />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="glass border border-white/10 dark:border-white/5">
                                <DropdownMenuItem onClick={onEdit} className="focus:bg-white/10 dark:focus:bg-white/5">
                                    <Edit className="mr-2 h-4 w-4" /> Edit
                                </DropdownMenuItem>
                                <DropdownMenuItem onClick={onDelete} className="text-destructive focus:text-destructive focus:bg-red-500/10">
                                    <Trash2 className="mr-2 h-4 w-4" /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                )}

            {/* Image Section */}
            <div className={cn(
                "relative bg-white/5 dark:bg-black/20 overflow-hidden w-full",
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
                                    <div 
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setFullscreenPhotoIndex(i);
                                            setIsFullscreenViewerOpen(true);
                                        }}
                                        className="relative w-full aspect-[4/3] cursor-pointer"
                                    >
                                        <Image
                                            src={photo.photo_url}
                                            alt={photo.caption || `Photo ${i + 1}`}
                                            fill
                                            className="object-cover"
                                            unoptimized
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
                    <div 
                        onClick={(e) => {
                            e.stopPropagation();
                            setFullscreenPhotoIndex(0);
                            setIsFullscreenViewerOpen(true);
                        }}
                        className="relative w-full h-full cursor-pointer"
                    >
                        <Image
                            src={worklog.photos[0].photo_url}
                            alt="Worklog update"
                            fill
                            className="object-cover transition-transform duration-500 group-hover:scale-105"
                            unoptimized
                        />
                    </div>
                ) : (
                    <div className="flex items-center justify-center h-full text-muted-foreground/30">
                        <ImageIcon className="h-10 w-10 text-muted-foreground/45" />
                    </div>
                )}

                {/* Fullscreen Overlay Button */}
                {totalPhotos > 0 && (
                    <Button
                        size="sm"
                        variant="ghost"
                        onClick={(e) => {
                            e.stopPropagation();
                            setFullscreenPhotoIndex(0);
                            setIsFullscreenViewerOpen(true);
                        }}
                        className="absolute bottom-3 right-3 z-30 h-7.5 px-2.5 rounded-xl bg-black/75 hover:bg-black text-white backdrop-blur-md border border-white/20 text-xs font-semibold flex items-center gap-1.5 shadow-lg transition-all"
                    >
                        <Maximize2 className="h-3.5 w-3.5" />
                        <span>Full Screen</span>
                    </Button>
                )}

                {/* Date Badge Overlay */}
                <div className="absolute top-3 left-3 z-10 glass border border-white/10 dark:border-white/5 px-3 py-1.5 rounded-xl shadow-sm text-xs font-semibold flex flex-col items-center pointer-events-none text-foreground">
                    <span className="text-muted-foreground uppercase text-[10px] leading-tight">{format(new Date(worklog.date), 'MMM')}</span>
                    <span className="text-lg leading-none font-bold text-foreground">{format(new Date(worklog.date), 'dd')}</span>
                </div>
            </div>

            {/* Content Section */}
            <div className="flex flex-col flex-1 p-5">
                <div className="flex items-center gap-2 mb-3">
                    {worklog.project?.name && (
                        <Badge variant="secondary" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-foreground font-semibold bg-white/10">
                            {worklog.project.name}
                        </Badge>
                    )}
                    <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-muted-foreground font-normal gap-1">
                        <Users className="h-3 w-3 text-muted-foreground" /> {totalWorkers} Workers
                    </Badge>
                    {totalMaterials > 0 && (
                        <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto glass border-white/10 dark:border-white/5 text-muted-foreground font-normal gap-1">
                            <Package className="h-3 w-3 text-muted-foreground" /> {totalMaterials} Mats
                        </Badge>
                    )}
                </div>

                <h3 className="font-bold text-lg mb-2 leading-tight group-hover:text-primary transition-colors line-clamp-2 text-foreground">
                    {title}
                </h3>

                <p className="text-sm text-muted-foreground/80 line-clamp-3 mb-4 flex-1">
                    {description}
                </p>

                {/* Detailed expanded sections */}
                {expanded && (
                    <div className="space-y-4 my-4 pt-4 border-t border-white/10 dark:border-white/5 animate-in fade-in-50 slide-in-from-top-2 duration-300">
                        {/* Labor section */}
                        {worklog.labor && worklog.labor.length > 0 && (
                            <div className="space-y-3">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                    <Users className="h-3.5 w-3.5" /> Labor & Activity
                                </h4>
                                <div className="space-y-2">
                                    {worklog.labor.map((entry: any, eIdx: number) => (
                                        <div key={eIdx} className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 space-y-2">
                                            <div className="flex justify-between items-start gap-2">
                                                <span className="font-semibold text-sm text-foreground">{entry.contractor_name}</span>
                                                {entry.category && (
                                                    <Badge variant="secondary" className="text-[10px] px-1.5 py-0 bg-white/10 text-foreground border-none">
                                                        {entry.category}
                                                    </Badge>
                                                )}
                                            </div>
                                            {entry.work_description && (
                                                <p className="text-xs text-muted-foreground/90">{entry.work_description}</p>
                                            )}
                                            {entry.work_done_quantity !== null && entry.work_done_quantity !== undefined && (
                                                <div className="text-[11px] font-medium text-primary bg-primary/10 border border-primary/20 px-2 py-0.5 rounded-md w-fit flex items-center gap-1">
                                                    <span className="text-muted-foreground/90">Work Done:</span>
                                                    <span className="font-bold text-foreground">{entry.work_done_quantity}</span>
                                                    {entry.work_done_unit && <span className="text-foreground">{entry.work_done_unit}</span>}
                                                </div>
                                            )}
                                            {entry.workers && entry.workers.length > 0 && (
                                                <div className="flex flex-wrap gap-1.5 pt-1">
                                                    {entry.workers.map((w: any, wIdx: number) => (
                                                        <Badge key={wIdx} variant="outline" className="text-[10px] py-0 px-2 glass border-white/5 text-muted-foreground font-normal">
                                                            {w.worker_type}: <span className="font-bold text-foreground ml-0.5">{w.count}</span>
                                                        </Badge>
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Materials section */}
                        {worklog.materials && worklog.materials.length > 0 && (
                            <div className="space-y-2">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                    <Package className="h-3.5 w-3.5" /> Materials Consumed
                                </h4>
                                <div className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 divide-y divide-white/5">
                                    {worklog.materials.map((m: any, mIdx: number) => (
                                        <div key={mIdx} className="flex justify-between items-center py-1.5 first:pt-0 last:pb-0 text-xs">
                                            <span className="text-foreground font-medium">{m.material_name}</span>
                                            <span className="text-muted-foreground font-semibold">
                                                {m.quantity_consumed} <span className="text-[10px] font-normal">{m.unit}</span>
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                <div className="pt-4 border-t border-white/10 dark:border-white/5 w-full flex items-center justify-between mt-auto">
                    <span className="text-xs text-muted-foreground flex flex-col items-start gap-0.5">
                        <span className="flex items-center gap-1">
                            <Clock className="h-3 w-3 text-muted-foreground" /> Posted {format(new Date(worklog.created_at || worklog.date), 'h:mm a')}
                        </span>
                        {worklog.creator && (
                            <span className="text-[10px] text-muted-foreground/75 ml-4">
                                By {worklog.creator.display_name || worklog.creator.email?.split('@')[0] || 'Unknown'}
                            </span>
                        )}
                    </span>
                    <Button 
                        variant="ghost" 
                        size="sm" 
                        onClick={() => setExpanded(!expanded)} 
                        className="text-xs text-primary hover:text-primary/80 font-semibold flex items-center gap-1 hover:bg-white/10 dark:hover:bg-white/5"
                    >
                        {expanded ? "Hide Details" : "Show Details"}
                        <ArrowRight className={cn("h-3.5 w-3.5 transition-transform", expanded ? "rotate-90" : "")} />
                    </Button>
                </div>
            </div>

            {/* Fullscreen Photo Viewer Lightbox */}
            <FullscreenPhotoViewer
                photos={worklog.photos}
                initialIndex={fullscreenPhotoIndex}
                isOpen={isFullscreenViewerOpen}
                onClose={() => setIsFullscreenViewerOpen(false)}
                title={title}
                dateLabel={format(new Date(worklog.date), 'MMM dd, yyyy')}
            />
        </div>
    );
}
