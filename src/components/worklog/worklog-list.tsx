'use client';

import { useEffect, useState, useMemo, useRef } from 'react';
import { format, isSameDay } from 'date-fns';
import { Users, Package, Image as ImageIcon, Calendar as CalendarIcon, Clock, ArrowRight, Search, X, MoreVertical, Edit, Trash2, AlertTriangle, LayoutGrid, Table as TableIcon } from 'lucide-react';
import { FullscreenPhotoViewer } from '@/components/worklog/fullscreen-photo-viewer';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { getWorklogs, deleteWorklog } from '@/app/actions/worklogs';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Calendar } from '@/components/ui/calendar';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { WorklogTableView } from '@/components/worklog/worklog-table-view';
import { matchesWorkerType, calculateLaborEntryCost } from '@/lib/worklog-helpers';
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
import { getSalaryProfiles } from '@/app/actions/financials';
import { getProjectMaterials } from '@/app/actions/materials';
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
    const [selectedContractor, setSelectedContractor] = useState<string>('all');
    const [viewMode, setViewMode] = useState<'cards' | 'table'>('table');
    const [sortKey, setSortKey] = useState<string>('date');
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
    const [editingWorklog, setEditingWorklog] = useState<any | null>(null);
    const [deletingWorklogId, setDeletingWorklogId] = useState<string | null>(null);
    const [salaryProfiles, setSalaryProfiles] = useState<any[]>([]);
    const [projectMaterials, setProjectMaterials] = useState<any[]>([]);
    const { toast } = useToast();

    // Responsive default: cards on mobile (< 768px), table on desktop (>= 768px)
    useEffect(() => {
        const saved = localStorage.getItem('constructor_worklog_view_mode');
        if (saved === 'cards' || saved === 'table') {
            setViewMode(saved);
        } else {
            if (typeof window !== 'undefined' && window.innerWidth < 768) {
                setViewMode('cards');
            } else {
                setViewMode('table');
            }
        }
    }, []);

    const handleToggleViewMode = (mode: 'cards' | 'table') => {
        setViewMode(mode);
        localStorage.setItem('constructor_worklog_view_mode', mode);
    };

    const handleSort = (key: string) => {
        if (sortKey === key) {
            setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
        } else {
            setSortKey(key);
            setSortDirection(key === 'date' ? 'desc' : 'asc');
        }
    };

    // Extract unique available contractor names from worklogs
    const availableContractors = useMemo(() => {
        const names = new Set<string>();
        worklogs.forEach(log => {
            (log.labor || []).forEach((l: any) => {
                const name = l.contractor_name || l.contractorName;
                if (name && name.trim()) names.add(name.trim());
            });
        });
        return Array.from(names).sort();
    }, [worklogs]);

    // Key to force re-render/fetch
    const [fetchKey, setFetchKey] = useState(0);

    // Fetch salary profiles and materials for wage & job costing
    useEffect(() => {
        const loadRates = async () => {
            try {
                const [profilesRes, materialsRes] = await Promise.all([
                    getSalaryProfiles(),
                    projectId && projectId !== 'all' ? getProjectMaterials(projectId) : Promise.resolve({ success: false, data: [] })
                ]);
                if (Array.isArray(profilesRes)) setSalaryProfiles(profilesRes);
                if (materialsRes?.success && Array.isArray(materialsRes.data)) setProjectMaterials(materialsRes.data);
            } catch (e) {
                console.error("Failed to load rates for WorklogList:", e);
            }
        };
        loadRates();
    }, [projectId]);

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
        let list = [...worklogs];

        // 1. Filter by Date
        if (date) {
            list = list.filter(log => isSameDay(new Date(log.date), date));
        }

        // 2. Filter by Search Term
        if (searchTerm) {
            const query = searchTerm.toLowerCase();
            list = list.filter(log => {
                const title = log.title || '';
                const description = log.labor?.map((l: any) => l.work_description).join(' ') || '';
                const contractorNames = log.labor?.map((l: any) => l.contractor_name || l.contractorName).join(' ') || '';
                const materials = log.materials?.map((m: any) => m.material_name || m.materialName).join(' ') || '';
                return (
                    title.toLowerCase().includes(query) || 
                    description.toLowerCase().includes(query) ||
                    contractorNames.toLowerCase().includes(query) ||
                    materials.toLowerCase().includes(query)
                );
            });
        }

        // 3. Filter by Contractor: strictly isolate to that contractor's labor entries
        if (selectedContractor !== 'all') {
            const targetContractor = selectedContractor.toLowerCase().trim();
            const contractorFiltered: any[] = [];

            for (const log of list) {
                const matchingLabor = (log.labor || []).filter((l: any) => {
                    const cName = (l.contractor_name || l.contractorName || '').toLowerCase().trim();
                    return cName === targetContractor || cName.includes(targetContractor) || targetContractor.includes(cName);
                });

                // If this worklog contains this contractor, ONLY include this contractor's labor!
                if (matchingLabor.length > 0) {
                    contractorFiltered.push({
                        ...log,
                        labor: matchingLabor,
                    });
                }
            }

            list = contractorFiltered;
        }

        // 4. Sort by Contractor across All Contractors (flatten into 1 row per contractor entry)
        if (sortKey === 'contractor' && selectedContractor === 'all') {
            const flattened: any[] = [];
            for (const log of list) {
                if (!log.labor || log.labor.length === 0) {
                    flattened.push({
                        ...log,
                        labor: [],
                        _sortContractor: '—',
                    });
                } else {
                    for (const l of log.labor) {
                        flattened.push({
                            ...log,
                            id: `${log.id}-${l.id || l.contractor_name || Math.random()}`,
                            labor: [l],
                            _sortContractor: l.contractor_name || l.contractorName || 'Unknown',
                        });
                    }
                }
            }
            flattened.sort((a, b) => {
                const comp = (a._sortContractor || '').localeCompare(b._sortContractor || '');
                return sortDirection === 'asc' ? comp : -comp;
            });
            return flattened;
        }

        // 5. Standard Sorting (Date, Cost, Workers, Project)
        list.sort((a, b) => {
            if (sortKey === 'date') {
                const dateA = new Date(a.date).getTime();
                const dateB = new Date(b.date).getTime();
                if (dateA !== dateB) {
                    return sortDirection === 'asc' ? dateA - dateB : dateB - dateA;
                }
                const updatedA = new Date(a.updated_at || a.created_at || 0).getTime();
                const updatedB = new Date(b.updated_at || b.created_at || 0).getTime();
                return sortDirection === 'asc' ? updatedA - updatedB : updatedB - updatedA;
            }

            if (sortKey === 'project') {
                const projA = (a.project?.name || '').toLowerCase();
                const projB = (b.project?.name || '').toLowerCase();
                const comp = projA.localeCompare(projB);
                return sortDirection === 'asc' ? comp : -comp;
            }

            if (sortKey === 'workers') {
                const workersA = (a.labor || []).reduce((acc: number, entry: any) => 
                    acc + (entry.workers || []).reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0), 0);
                const workersB = (b.labor || []).reduce((acc: number, entry: any) => 
                    acc + (entry.workers || []).reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0), 0);
                return sortDirection === 'asc' ? workersA - workersB : workersB - workersA;
            }

            if (sortKey === 'cost') {
                const costA = (a.labor || []).reduce((acc: number, entry: any) => 
                    acc + calculateLaborEntryCost(entry, salaryProfiles).entryTotal, 0);
                const costB = (b.labor || []).reduce((acc: number, entry: any) => 
                    acc + calculateLaborEntryCost(entry, salaryProfiles).entryTotal, 0);
                return sortDirection === 'asc' ? costA - costB : costB - costA;
            }

            return 0;
        });

        return list;
    }, [worklogs, date, searchTerm, selectedContractor, sortKey, sortDirection, salaryProfiles]);

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
            {/* Filters Bar & View Mode Toggle */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
                {/* Search, Contractor & Date filters */}
                <div className="flex flex-wrap items-center gap-2 flex-1">
                    {/* Search Input */}
                    <div className="relative min-w-[180px] flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                            placeholder="Search logs, scope, contractors..."
                            className="pl-9 glass border-white/10 dark:border-white/5 focus-visible:ring-primary h-9 text-xs sm:text-sm"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    {/* Contractor Filter Dropdown */}
                    {availableContractors.length > 0 && (
                        <Select value={selectedContractor} onValueChange={setSelectedContractor}>
                            <SelectTrigger className="w-[140px] sm:w-[170px] h-9 text-xs glass border-white/10 dark:border-white/5 bg-transparent">
                                <SelectValue placeholder="All Contractors" />
                            </SelectTrigger>
                            <SelectContent className="glass border-white/10 dark:border-white/5">
                                <SelectItem value="all" className="focus:bg-white/10">All Contractors</SelectItem>
                                {availableContractors.map((cName) => (
                                    <SelectItem key={cName} value={cName} className="focus:bg-white/10">
                                        {cName}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    )}

                    {/* Date Filter */}
                    <Popover>
                        <PopoverTrigger asChild>
                            <Button
                                variant={"outline"}
                                className={cn(
                                    "justify-center text-left font-normal glass border-white/10 dark:border-white/5 hover:bg-white/10 dark:hover:bg-white/5 h-9 text-xs",
                                    "w-9 px-0 sm:w-auto sm:px-3 sm:justify-start",
                                    !date && "text-muted-foreground"
                                )}
                            >
                                <CalendarIcon className={cn("h-3.5 w-3.5", "sm:mr-1.5")} />
                                <span className="hidden sm:inline">
                                    {date ? format(date, "PPP") : "Date"}
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

                    {/* Clear Filters Button */}
                    {(searchTerm || date || selectedContractor !== 'all') && (
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                                setSearchTerm('');
                                setDate(undefined);
                                setSelectedContractor('all');
                            }}
                            title="Clear filters"
                            className="h-9 w-9 glass border-white/10 dark:border-white/5 hover:bg-white/10"
                        >
                            <X className="h-4 w-4" />
                        </Button>
                    )}
                </div>

                {/* View Mode Toggle Button Group */}
                <div className="flex items-center gap-1 p-1 rounded-xl glass border border-white/10 dark:border-white/5 self-end sm:self-auto shrink-0 bg-white/5">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleToggleViewMode('cards')}
                        className={cn(
                            "h-7 px-2.5 text-xs font-medium rounded-lg gap-1.5 transition-all",
                            viewMode === 'cards' 
                                ? "bg-primary text-primary-foreground shadow-sm" 
                                : "text-muted-foreground hover:text-foreground hover:bg-white/10"
                        )}
                        title="Card Feed View"
                    >
                        <LayoutGrid className="h-3.5 w-3.5" />
                        <span className="hidden xs:inline">Cards</span>
                    </Button>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleToggleViewMode('table')}
                        className={cn(
                            "h-7 px-2.5 text-xs font-medium rounded-lg gap-1.5 transition-all",
                            viewMode === 'table' 
                                ? "bg-primary text-primary-foreground shadow-sm" 
                                : "text-muted-foreground hover:text-foreground hover:bg-white/10"
                        )}
                        title="Table Ledger View"
                    >
                        <TableIcon className="h-3.5 w-3.5" />
                        <span className="hidden xs:inline">Table</span>
                    </Button>
                </div>
            </div>

            {filteredWorklogs.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground border border-white/10 dark:border-white/5 rounded-2xl glass-card p-8">
                    <Search className="h-8 w-8 mb-3 opacity-50 text-foreground" />
                    <p className="text-muted-foreground">No worklogs found matching your filters</p>
                    <Button variant="link" className="text-primary hover:text-primary/80 font-medium" onClick={() => { setSearchTerm(''); setDate(undefined); setSelectedContractor('all'); }}>
                        Clear all filters
                    </Button>
                </div>
            ) : viewMode === 'table' ? (
                <WorklogTableView
                    worklogs={filteredWorklogs}
                    salaryProfiles={salaryProfiles}
                    projectMaterials={projectMaterials}
                    selectedContractor={selectedContractor}
                    sortKey={sortKey}
                    sortDirection={sortDirection}
                    onSort={handleSort}
                    onEdit={(log) => setEditingWorklog(log)}
                    onDelete={(id) => setDeletingWorklogId(id)}
                    currentUserProfile={currentUserProfile}
                />
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
                                salaryProfiles={salaryProfiles}
                                projectMaterials={projectMaterials}
                            />
                        </div>
                    ))}
                </div>
            )}

            {/* Hidden Edit Dialog Trigger removed */}

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
    currentUserProfile,
    salaryProfiles = [],
    projectMaterials = []
}: { 
    worklog: any; 
    index: number; 
    onEdit: () => void; 
    onDelete: () => void; 
    initiallyExpanded?: boolean; 
    currentUserProfile: { id: string; role: string } | null;
    salaryProfiles?: any[];
    projectMaterials?: any[];
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
    const totalWorkers = (worklog.labor || []).reduce((acc: number, entry: any) => {
        return acc + (entry.workers || []).reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0);
    }, 0);
    const totalMaterials = (worklog.materials || []).length;
    const totalPhotos = (worklog.photos || []).length;

    // Helper to match contractor wage profile
    const findContractorProfile = (contractorName: string, profiles: any[]) => {
        if (!contractorName || !profiles?.length) return null;
        const cleanName = contractorName.toLowerCase().trim();
        return profiles.find((p: any) => {
            const pName = (p.contractors?.name || p.worker_name || '').toLowerCase().trim();
            if (!pName) return false;
            return cleanName === pName || cleanName.includes(pName) || pName.includes(cleanName);
        });
    };

    // Helper to find unit cost from project materials
    const getMaterialUnitCost = (m: any, materialsList: any[]) => {
        const mName = (m.material_name || m.materialName || '').toLowerCase().trim();
        const mId = m.project_material_id || m.projectMaterialId;
        const match = materialsList.find(pm => (mId && pm.id === mId) || (pm.name && pm.name.toLowerCase().trim() === mName));
        return match ? Number(match.cost || 0) : 0;
    };

    // Calculate Labor Cost based on wage profiles
    const laborCostBreakdown = useMemo(() => {
        let totalLaborCost = 0;
        let profilesMatchedCount = 0;

        const entries = (worklog.labor || []).map((entry: any) => {
            const contractorName = entry.contractor_name || entry.contractorName || '';
            const profile = findContractorProfile(contractorName, salaryProfiles);
            if (profile) profilesMatchedCount++;

            let entryTotal = 0;
            const workers = (entry.workers || []).map((w: any) => {
                const count = Number(w.count || 0);
                const wType = (w.worker_type || w.workerType || '').trim();
                const wTypeLower = wType.toLowerCase();

                let rate = 0;
                let hasProfileRate = false;

                if (profile) {
                    const rates = (profile.rates as Record<string, number>) || {};
                    const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
                    if (matchingKey && Number(rates[matchingKey]) > 0) {
                        rate = Number(rates[matchingKey]);
                        hasProfileRate = true;
                    } else if (Number(profile.rate) > 0) {
                        rate = Number(profile.rate);
                        hasProfileRate = true;
                    }
                }

                if (!hasProfileRate && salaryProfiles.length > 0) {
                    for (const p of salaryProfiles) {
                        const rates = (p.rates as Record<string, number>) || {};
                        const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
                        if (matchingKey && Number(rates[matchingKey]) > 0) {
                            rate = Number(rates[matchingKey]);
                            hasProfileRate = true;
                            break;
                        }
                    }
                }

                const subtotal = count * rate;
                entryTotal += subtotal;

                return {
                    type: wType,
                    count,
                    rate,
                    subtotal,
                    hasProfileRate,
                };
            });

            totalLaborCost += entryTotal;
            return {
                ...entry,
                contractorName,
                profileFound: !!profile,
                entryTotal,
                workersBreakdown: workers,
            };
        });

        return { totalLaborCost, entries, profilesMatchedCount };
    }, [worklog.labor, salaryProfiles]);

    // Calculate Materials Cost based on inventory cost per unit
    const materialsCostBreakdown = useMemo(() => {
        let totalMaterialsCost = 0;
        const items = (worklog.materials || []).map((m: any) => {
            const qty = Number(m.quantity_consumed || m.quantity || 0);
            const unitCost = getMaterialUnitCost(m, projectMaterials);
            const subtotal = qty * unitCost;
            totalMaterialsCost += subtotal;
            return {
                name: m.material_name || m.materialName || 'Material',
                quantity: qty,
                unit: m.unit || '',
                unitCost,
                subtotal,
                hasCost: unitCost > 0,
            };
        });
        return { totalMaterialsCost, items };
    }, [worklog.materials, projectMaterials]);

    const estimatedTotalCost = laborCostBreakdown.totalLaborCost + materialsCostBreakdown.totalMaterialsCost;

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

                {/* Date Badge Overlay */}
                <div className="absolute top-3 left-3 z-10 glass border border-white/10 dark:border-white/5 px-3 py-1.5 rounded-xl shadow-sm text-xs font-semibold flex flex-col items-center pointer-events-none text-foreground">
                    <span className="text-muted-foreground uppercase text-[10px] leading-tight">{format(new Date(worklog.date), 'MMM')}</span>
                    <span className="text-lg leading-none font-bold text-foreground">{format(new Date(worklog.date), 'dd')}</span>
                </div>
            </div>

            {/* Content Section */}
            <div className="flex flex-col flex-1 p-5">
                <div className="flex flex-wrap items-center gap-2 mb-3">
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
                    {estimatedTotalCost > 0 && (
                        <Badge variant="outline" className="text-[10px] px-2 py-0.5 h-auto glass border-emerald-500/30 text-emerald-600 dark:text-emerald-400 font-bold bg-emerald-500/10">
                            ₹{estimatedTotalCost.toLocaleString('en-IN')} Cost
                        </Badge>
                    )}
                </div>

                <h3 
                    onClick={() => setExpanded(!expanded)}
                    className="font-bold text-lg mb-2 leading-tight group-hover:text-primary transition-colors line-clamp-2 text-foreground cursor-pointer"
                >
                    {title}
                </h3>

                <p className="text-sm text-muted-foreground/80 line-clamp-3 mb-4 flex-1">
                    {description}
                </p>

                {/* Detailed expanded sections */}
                {expanded && (
                    <div className="space-y-4 my-4 pt-4 border-t border-white/10 dark:border-white/5 animate-in fade-in-50 slide-in-from-top-2 duration-300">
                        {/* Job-Costing Cost of Work Banner */}
                        <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex flex-col gap-2">
                            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
                                <div className="flex items-center gap-2.5">
                                    <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold text-xs shrink-0">
                                        ₹
                                    </div>
                                    <div>
                                        <div className="flex items-center gap-1">
                                            <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider leading-none">Job Cost of Work</p>
                                            <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold">• Wage Profile Linked</span>
                                        </div>
                                        <p className="text-base font-bold text-emerald-600 dark:text-emerald-400 leading-tight mt-0.5 font-mono">
                                            ₹{estimatedTotalCost.toLocaleString('en-IN')}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex flex-wrap items-center gap-1.5 text-xs font-semibold text-muted-foreground self-end sm:self-auto">
                                    <span className="bg-background/80 px-2 py-0.5 rounded-md border border-white/10 dark:border-white/5 flex items-center gap-1 text-[11px]">
                                        <Users className="h-3 w-3 text-primary" /> Labour: <span className="font-mono text-foreground font-bold">₹{laborCostBreakdown.totalLaborCost.toLocaleString('en-IN')}</span>
                                    </span>
                                    <span className="bg-background/80 px-2 py-0.5 rounded-md border border-white/10 dark:border-white/5 flex items-center gap-1 text-[11px]">
                                        <Package className="h-3 w-3 text-amber-500" /> Materials: <span className="font-mono text-foreground font-bold">₹{materialsCostBreakdown.totalMaterialsCost.toLocaleString('en-IN')}</span>
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Labor section */}
                        {laborCostBreakdown.entries.length > 0 && (
                            <div className="space-y-3">
                                <div className="flex justify-between items-center">
                                    <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                        <Users className="h-3.5 w-3.5 text-primary" /> Labor & Activity
                                    </h4>
                                    {laborCostBreakdown.totalLaborCost > 0 && (
                                        <span className="text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400">
                                            Total: ₹{laborCostBreakdown.totalLaborCost.toLocaleString('en-IN')}
                                        </span>
                                    )}
                                </div>
                                <div className="space-y-2">
                                    {laborCostBreakdown.entries.map((entry: any, eIdx: number) => (
                                        <div key={eIdx} className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 space-y-2">
                                            <div className="flex justify-between items-start gap-2">
                                                <div>
                                                    <span className="font-semibold text-sm text-foreground">{entry.contractorName}</span>
                                                    {entry.profileFound && (
                                                        <span className="ml-2 text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
                                                            (Wage Profile)
                                                        </span>
                                                    )}
                                                </div>
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
                                            {entry.workersBreakdown && entry.workersBreakdown.length > 0 && (
                                                <div className="flex flex-wrap gap-1.5 pt-1">
                                                    {entry.workersBreakdown.map((w: any, wIdx: number) => (
                                                        <Badge key={wIdx} variant="outline" className="text-[10px] py-0.5 px-2 glass border-white/5 text-foreground font-medium flex items-center gap-1">
                                                            <span className="text-muted-foreground">{w.type}:</span>
                                                            <span className="font-bold">{w.count}</span>
                                                            {w.hasProfileRate ? (
                                                                <span className="font-mono text-emerald-600 dark:text-emerald-400 font-semibold ml-0.5">
                                                                    (@ ₹{w.rate} = ₹{w.subtotal.toLocaleString('en-IN')})
                                                                </span>
                                                            ) : null}
                                                        </Badge>
                                                    ))}
                                                </div>
                                            )}
                                            {entry.entryTotal > 0 && (
                                                <div className="flex justify-end pt-1 border-t border-white/5 text-xs text-muted-foreground">
                                                    <span>Team Daily Wages: <strong className="font-mono text-foreground font-bold">₹{entry.entryTotal.toLocaleString('en-IN')}</strong></span>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Materials section */}
                        {materialsCostBreakdown.items.length > 0 && (
                            <div className="space-y-2">
                                <div className="flex justify-between items-center">
                                    <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                        <Package className="h-3.5 w-3.5 text-amber-500" /> Materials Consumed
                                    </h4>
                                    {materialsCostBreakdown.totalMaterialsCost > 0 && (
                                        <span className="text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400">
                                            Total: ₹{materialsCostBreakdown.totalMaterialsCost.toLocaleString('en-IN')}
                                        </span>
                                    )}
                                </div>
                                <div className="p-3 rounded-xl bg-white/5 dark:bg-black/20 border border-white/5 divide-y divide-white/5">
                                    {materialsCostBreakdown.items.map((m: any, mIdx: number) => (
                                        <div key={mIdx} className="flex justify-between items-center py-1.5 first:pt-0 last:pb-0 text-xs">
                                            <div className="flex flex-col">
                                                <span className="text-foreground font-medium">{m.name}</span>
                                                {m.hasCost && (
                                                    <span className="text-[10px] text-muted-foreground font-mono">
                                                        ₹{m.unitCost} per {m.unit || 'unit'}
                                                    </span>
                                                )}
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <span className="text-muted-foreground font-semibold">
                                                    {m.quantity} <span className="text-[10px] font-normal">{m.unit}</span>
                                                </span>
                                                {m.hasCost && (
                                                    <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">
                                                        ₹{m.subtotal.toLocaleString('en-IN')}
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
                    <div className="flex items-center gap-1.5">
                        <Button 
                            variant="ghost" 
                            size="sm" 
                            onClick={() => setExpanded(!expanded)} 
                            className="text-xs text-primary hover:text-primary/80 font-semibold flex items-center gap-1 hover:bg-white/10 dark:hover:bg-white/5 h-8 px-2"
                        >
                            {expanded ? "Hide Details" : "Show Details"}
                            <ArrowRight className={cn("h-3.5 w-3.5 transition-transform", expanded ? "rotate-90" : "")} />
                        </Button>
                    </div>
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
