'use client';

import { useState } from 'react';
import { format } from 'date-fns';
import { 
    Calendar as CalendarIcon, 
    Clock, 
    Users, 
    Package, 
    Image as ImageIcon, 
    MoreVertical, 
    Edit, 
    Trash2, 
    ArrowUpDown, 
    ArrowUp, 
    ArrowDown,
    CheckCircle2,
    HardHat
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { FullscreenPhotoViewer } from '@/components/worklog/fullscreen-photo-viewer';
import Image from 'next/image';
import { cn } from '@/lib/utils';
import { calculateLaborEntryCost, getMaterialUnitCost } from '@/lib/worklog-helpers';

interface WorklogTableViewProps {
    worklogs: any[];
    salaryProfiles: any[];
    projectMaterials: any[];
    selectedContractor: string;
    sortKey: string;
    sortDirection: 'asc' | 'desc';
    onSort: (key: string) => void;
    onEdit: (worklog: any) => void;
    onDelete: (worklogId: string) => void;
    currentUserProfile: { id: string; role: string } | null;
}

export function WorklogTableView({
    worklogs,
    salaryProfiles,
    projectMaterials,
    selectedContractor,
    sortKey,
    sortDirection,
    onSort,
    onEdit,
    onDelete,
    currentUserProfile,
}: WorklogTableViewProps) {
    // Lightbox state
    const [viewerPhotos, setViewerPhotos] = useState<any[]>([]);
    const [viewerIndex, setViewerIndex] = useState(0);
    const [isViewerOpen, setIsViewerOpen] = useState(false);
    const [viewerTitle, setViewerTitle] = useState('');
    const [viewerDate, setViewerDate] = useState('');

    const openPhotoViewer = (photos: any[], index: number, title: string, date: string) => {
        setViewerPhotos(photos);
        setViewerIndex(index);
        setViewerTitle(title);
        setViewerDate(format(new Date(date), 'MMM dd, yyyy'));
        setIsViewerOpen(true);
    };

    const renderSortIcon = (key: string) => {
        if (sortKey !== key) {
            return <ArrowUpDown className="ml-1.5 h-3.5 w-3.5 opacity-40 group-hover:opacity-100" />;
        }
        return sortDirection === 'asc' 
            ? <ArrowUp className="ml-1.5 h-3.5 w-3.5 text-primary" />
            : <ArrowDown className="ml-1.5 h-3.5 w-3.5 text-primary" />;
    };

    // Check if contractor sorting is active (which triggers 1 row per contractor entry)
    const isContractorSorted = sortKey === 'contractor';

    // Calculate overall stats for filtered view
    let totalManDaysFiltered = 0;
    let totalCostFiltered = 0;

    return (
        <div className="space-y-4">
            {/* Filtered Contractor Summary Ribbon */}
            {selectedContractor !== 'all' && (
                <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 rounded-2xl glass-card border border-primary/20 bg-primary/5 text-sm">
                    <div className="flex items-center gap-2">
                        <HardHat className="h-4 w-4 text-primary" />
                        <span className="font-semibold text-foreground">
                            Contractor Ledger: <span className="text-primary">{selectedContractor}</span>
                        </span>
                    </div>
                    <div className="flex items-center gap-4 text-xs font-medium">
                        <span className="text-muted-foreground">
                            Logged Days: <strong className="text-foreground">{worklogs.length}</strong>
                        </span>
                        <span className="text-muted-foreground">
                            Total Wages: <strong className="text-emerald-500 font-bold">
                                ₹{worklogs.reduce((acc, log) => {
                                    const entry = (log.labor || [])[0];
                                    if (!entry) return acc;
                                    const cost = calculateLaborEntryCost(entry, salaryProfiles);
                                    return acc + cost.entryTotal;
                                }, 0).toLocaleString('en-IN')}
                            </strong>
                        </span>
                    </div>
                </div>
            )}

            {/* Table Container with Smooth Mobile Scroll */}
            <div className="rounded-2xl border border-white/10 dark:border-white/5 glass-card overflow-hidden shadow-sm">
                <div className="overflow-x-auto w-full max-w-full">
                    <Table className="w-full text-left border-collapse min-w-[850px] lg:min-w-full">
                        <TableHeader className="bg-white/5 dark:bg-black/20 border-b border-white/10 dark:border-white/5 sticky top-0 z-10 backdrop-blur-md">
                            <TableRow className="hover:bg-transparent border-white/10 dark:border-white/5">
                                <TableHead 
                                    onClick={() => onSort('date')}
                                    className="cursor-pointer select-none text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 group hover:text-foreground transition-colors w-[150px]"
                                >
                                    <div className="flex items-center">
                                        Date
                                        {renderSortIcon('date')}
                                    </div>
                                </TableHead>

                                <TableHead 
                                    onClick={() => onSort('project')}
                                    className="cursor-pointer select-none text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 group hover:text-foreground transition-colors w-[160px]"
                                >
                                    <div className="flex items-center">
                                        Project
                                        {renderSortIcon('project')}
                                    </div>
                                </TableHead>

                                <TableHead 
                                    onClick={() => onSort('contractor')}
                                    className="cursor-pointer select-none text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 group hover:text-foreground transition-colors min-w-[180px]"
                                >
                                    <div className="flex items-center">
                                        Contractor & Trade
                                        {renderSortIcon('contractor')}
                                    </div>
                                </TableHead>

                                <TableHead 
                                    onClick={() => onSort('workers')}
                                    className="cursor-pointer select-none text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 group hover:text-foreground transition-colors min-w-[220px]"
                                >
                                    <div className="flex items-center">
                                        Manpower & Scope
                                        {renderSortIcon('workers')}
                                    </div>
                                </TableHead>

                                <TableHead 
                                    onClick={() => onSort('cost')}
                                    className="cursor-pointer select-none text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 group hover:text-foreground transition-colors text-right w-[150px]"
                                >
                                    <div className="flex items-center justify-end">
                                        Labor Cost
                                        {renderSortIcon('cost')}
                                    </div>
                                </TableHead>

                                <TableHead className="text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 min-w-[150px]">
                                    Materials
                                </TableHead>

                                <TableHead className="text-xs font-semibold text-muted-foreground uppercase tracking-wider py-3.5 px-4 text-center w-[100px]">
                                    Photos
                                </TableHead>

                                <TableHead className="py-3.5 px-3 text-right w-[50px]">
                                    <span className="sr-only">Actions</span>
                                </TableHead>
                            </TableRow>
                        </TableHeader>

                        <TableBody className="divide-y divide-white/10 dark:divide-white/5">
                            {worklogs.map((log) => {
                                const laborEntries = log.labor || [];
                                const totalLaborCount = laborEntries.length;
                                const canEditOrDelete = currentUserProfile?.role === 'admin' || currentUserProfile?.id === log.created_by;

                                // Calculate Labor Entries costs
                                const processedLabor = laborEntries.map((entry: any) => calculateLaborEntryCost(entry, salaryProfiles));
                                const totalDayLaborCost = processedLabor.reduce((sum, item) => sum + item.entryTotal, 0);
                                const totalDayWorkers = laborEntries.reduce((acc: number, entry: any) => {
                                    return acc + (entry.workers || []).reduce((wAcc: number, w: any) => wAcc + Number(w.count || 0), 0);
                                }, 0);

                                return (
                                    <TableRow 
                                        key={log.id} 
                                        className="hover:bg-white/[0.03] transition-colors border-white/10 dark:border-white/5 group"
                                    >
                                        {/* Date & Creator */}
                                        <TableCell className="py-3 px-4 align-top">
                                            <div className="flex flex-col gap-1">
                                                <div className="flex items-center gap-1.5 font-bold text-foreground text-sm">
                                                    <CalendarIcon className="h-3.5 w-3.5 text-primary shrink-0" />
                                                    {format(new Date(log.date), 'dd MMM yyyy')}
                                                </div>
                                                <div className="text-[11px] text-muted-foreground flex items-center gap-1">
                                                    <Clock className="h-3 w-3 shrink-0" />
                                                    {format(new Date(log.created_at || log.date), 'h:mm a')}
                                                </div>
                                                {log.creator && (
                                                    <span className="text-[10px] text-muted-foreground/80 truncate max-w-[120px]">
                                                        By {log.creator.display_name || log.creator.email?.split('@')[0]}
                                                    </span>
                                                )}
                                            </div>
                                        </TableCell>

                                        {/* Project */}
                                        <TableCell className="py-3 px-4 align-top">
                                            {log.project?.name ? (
                                                <Badge 
                                                    variant="secondary" 
                                                    className="font-medium text-xs glass border-white/10 dark:border-white/5 bg-white/10 text-foreground truncate max-w-[140px]"
                                                >
                                                    {log.project.name}
                                                </Badge>
                                            ) : (
                                                <span className="text-xs text-muted-foreground">—</span>
                                            )}
                                        </TableCell>

                                        {/* Contractor & Trade (Divided Sub-Rows) */}
                                        <TableCell className="p-0 align-top">
                                            {totalLaborCount === 0 ? (
                                                <div className="p-3 text-xs text-muted-foreground italic">No labor logged</div>
                                            ) : (
                                                <div className="divide-y divide-white/10 dark:divide-white/5">
                                                    {laborEntries.map((entry: any, eIdx: number) => {
                                                        const pData = processedLabor[eIdx];
                                                        const contractorName = entry.contractor_name || entry.contractorName || 'Unknown';
                                                        const category = entry.category || 'General';

                                                        return (
                                                            <div key={eIdx} className="p-3 flex flex-col gap-1 min-h-[58px] justify-center">
                                                                <div className="flex items-center gap-1.5 flex-wrap">
                                                                    <span className="font-semibold text-sm text-foreground">
                                                                        {contractorName}
                                                                    </span>
                                                                    {pData?.profileFound && (
                                                                        <span className="inline-flex items-center gap-0.5 text-[9px] text-emerald-600 dark:text-emerald-400 font-medium bg-emerald-500/10 px-1.5 py-0.5 rounded-full border border-emerald-500/20">
                                                                            <CheckCircle2 className="h-2.5 w-2.5" /> Wage Profile
                                                                        </span>
                                                                    )}
                                                                </div>
                                                                <span className="text-[11px] text-muted-foreground">
                                                                    Trade: <strong className="text-foreground/90">{category}</strong>
                                                                </span>
                                                            </div>
                                                        );
                                                    })}
                                                    {totalLaborCount > 1 && selectedContractor === 'all' && (
                                                        <div className="px-3 py-1.5 bg-white/5 text-[11px] font-semibold text-muted-foreground">
                                                            {totalLaborCount} Contractors Total
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </TableCell>

                                        {/* Manpower & Scope (Divided Sub-Rows) */}
                                        <TableCell className="p-0 align-top">
                                            {totalLaborCount === 0 ? (
                                                <div className="p-3 text-xs text-muted-foreground italic">—</div>
                                            ) : (
                                                <div className="divide-y divide-white/10 dark:divide-white/5">
                                                    {laborEntries.map((entry: any, eIdx: number) => {
                                                        const pData = processedLabor[eIdx];
                                                        const workers = pData?.workersBreakdown || [];
                                                        const entryWorkersCount = workers.reduce((sum: number, w: any) => sum + Number(w.count || 0), 0);
                                                        const desc = entry.work_description || '';
                                                        const workDone = entry.work_done_quantity ? `${entry.work_done_quantity} ${entry.work_done_unit || ''}` : null;

                                                        return (
                                                            <div key={eIdx} className="p-3 flex flex-col gap-1 min-h-[58px] justify-center">
                                                                {/* Workers Badges */}
                                                                <div className="flex flex-wrap items-center gap-1.5">
                                                                    {workers.length > 0 ? (
                                                                        workers.map((w: any, wIdx: number) => (
                                                                            <Badge 
                                                                                key={wIdx} 
                                                                                variant="outline" 
                                                                                className="text-[10px] px-2 py-0.5 glass border-white/10 dark:border-white/5 font-medium"
                                                                            >
                                                                                {w.type}: <strong className="ml-1 text-foreground">{w.count}</strong>
                                                                                {w.rate > 0 && (
                                                                                    <span className="text-muted-foreground ml-1">(@ ₹{w.rate})</span>
                                                                                )}
                                                                            </Badge>
                                                                        ))
                                                                    ) : (
                                                                        <span className="text-xs text-muted-foreground">0 workers</span>
                                                                    )}
                                                                </div>

                                                                {/* Work Scope / Description */}
                                                                {(desc || workDone) && (
                                                                    <div className="text-xs text-muted-foreground line-clamp-1 mt-0.5">
                                                                        {workDone && (
                                                                            <span className="font-semibold text-primary mr-1">
                                                                                [{workDone}]
                                                                            </span>
                                                                        )}
                                                                        {desc}
                                                                    </div>
                                                                )}
                                                            </div>
                                                        );
                                                    })}
                                                    {totalLaborCount > 1 && selectedContractor === 'all' && (
                                                        <div className="px-3 py-1.5 bg-white/5 text-[11px] font-semibold text-foreground">
                                                            Total Manpower: {totalDayWorkers} Workers
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </TableCell>

                                        {/* Labor Cost (Divided Sub-Rows) */}
                                        <TableCell className="p-0 align-top text-right">
                                            {totalLaborCount === 0 ? (
                                                <div className="p-3 text-xs text-muted-foreground italic">—</div>
                                            ) : (
                                                <div className="divide-y divide-white/10 dark:divide-white/5">
                                                    {laborEntries.map((entry: any, eIdx: number) => {
                                                        const pData = processedLabor[eIdx];
                                                        const subtotal = pData?.entryTotal || 0;

                                                        return (
                                                            <div key={eIdx} className="p-3 flex items-center justify-end min-h-[58px]">
                                                                <span className={cn(
                                                                    "font-mono font-bold text-sm",
                                                                    subtotal > 0 ? "text-emerald-500" : "text-muted-foreground/60"
                                                                )}>
                                                                    {subtotal > 0 ? `₹${subtotal.toLocaleString('en-IN')}` : '₹0'}
                                                                </span>
                                                            </div>
                                                        );
                                                    })}
                                                    {totalLaborCount > 1 && selectedContractor === 'all' && (
                                                        <div className="px-3 py-1.5 bg-white/5 text-[11px] font-bold text-emerald-500 font-mono">
                                                            Day Total: ₹{totalDayLaborCost.toLocaleString('en-IN')}
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </TableCell>

                                        {/* Materials Consumed */}
                                        <TableCell className="py-3 px-4 align-top">
                                            {log.materials && log.materials.length > 0 ? (
                                                <div className="flex flex-col gap-1 max-w-[200px]">
                                                    {log.materials.slice(0, 2).map((m: any, mIdx: number) => (
                                                        <span key={mIdx} className="text-xs text-muted-foreground flex items-center gap-1 truncate">
                                                            <Package className="h-3 w-3 text-muted-foreground shrink-0" />
                                                            <strong className="text-foreground">{m.material_name || m.materialName}</strong>
                                                            <span>({m.quantity_consumed || m.quantity} {m.unit})</span>
                                                        </span>
                                                    ))}
                                                    {log.materials.length > 2 && (
                                                        <span className="text-[10px] text-primary font-medium">
                                                            +{log.materials.length - 2} more items
                                                        </span>
                                                    )}
                                                </div>
                                            ) : (
                                                <span className="text-xs text-muted-foreground/60">—</span>
                                            )}
                                        </TableCell>

                                        {/* Photos Thumbnail Badge */}
                                        <TableCell className="py-3 px-4 align-top text-center">
                                            {log.photos && log.photos.length > 0 ? (
                                                <button 
                                                    type="button"
                                                    onClick={() => openPhotoViewer(log.photos, 0, log.title || 'Daily Log', log.date)}
                                                    className="relative inline-block w-11 h-11 rounded-lg overflow-hidden border border-white/10 group-hover:border-primary/50 transition-all hover:scale-105 shadow-sm"
                                                    title="View site photos"
                                                >
                                                    <Image
                                                        src={log.photos[0].photo_url}
                                                        alt="Site photo thumbnail"
                                                        fill
                                                        className="object-cover"
                                                        unoptimized
                                                    />
                                                    {log.photos.length > 1 && (
                                                        <span className="absolute bottom-0 right-0 bg-black/75 text-[10px] font-bold text-white px-1 rounded-tl-md">
                                                            +{log.photos.length - 1}
                                                        </span>
                                                    )}
                                                </button>
                                            ) : (
                                                <span className="text-muted-foreground/40 text-xs">
                                                    <ImageIcon className="h-4 w-4 mx-auto" />
                                                </span>
                                            )}
                                        </TableCell>

                                        {/* Actions */}
                                        <TableCell className="py-3 px-3 align-top text-right">
                                            {canEditOrDelete && (
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button variant="ghost" size="icon" className="h-8 w-8 rounded-full hover:bg-white/10">
                                                            <MoreVertical className="h-4 w-4" />
                                                        </Button>
                                                    </DropdownMenuTrigger>
                                                    <DropdownMenuContent align="end" className="glass border-white/10 dark:border-white/5">
                                                        <DropdownMenuItem onClick={() => onEdit(log)} className="focus:bg-white/10">
                                                            <Edit className="mr-2 h-4 w-4" /> Edit
                                                        </DropdownMenuItem>
                                                        <DropdownMenuItem onClick={() => onDelete(log.id)} className="text-destructive focus:text-destructive focus:bg-red-500/10">
                                                            <Trash2 className="mr-2 h-4 w-4" /> Delete
                                                        </DropdownMenuItem>
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </div>
            </div>

            {/* Lightbox for Site Photos */}
            <FullscreenPhotoViewer
                photos={viewerPhotos}
                initialIndex={viewerIndex}
                isOpen={isViewerOpen}
                onClose={() => setIsViewerOpen(false)}
                title={viewerTitle}
                dateLabel={viewerDate}
            />
        </div>
    );
}
