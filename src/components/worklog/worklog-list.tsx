'use client';

import { useEffect, useState } from 'react';
import { format } from 'date-fns';
import { ChevronDown, ChevronUp, Users, Package, Image as ImageIcon, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { getWorklogs } from '@/app/actions/worklogs';
import { Skeleton } from '@/components/ui/skeleton';

interface WorklogListProps {
    projectId: string;
    refreshTrigger?: number; // Prop to trigger refresh
}

export function WorklogList({ projectId, refreshTrigger }: WorklogListProps) {
    const [worklogs, setWorklogs] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchWorklogs = async () => {
            setLoading(true);
            const result = await getWorklogs(projectId);
            if (result.success && result.data) {
                setWorklogs(result.data);
            }
            setLoading(false);
        };

        if (projectId) {
            fetchWorklogs();
        }
    }, [projectId, refreshTrigger]);

    if (loading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-32 w-full" />
                <Skeleton className="h-32 w-full" />
                <Skeleton className="h-32 w-full" />
            </div>
        );
    }

    if (worklogs.length === 0) {
        return (
            <div className="text-center py-10 text-muted-foreground">
                No worklogs found for this project.
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {worklogs.map((log) => (
                <WorklogCard key={log.id} worklog={log} />
            ))}
        </div>
    );
}

function WorklogCard({ worklog }: { worklog: any }) {
    const [isOpen, setIsOpen] = useState(false);

    // Calculate totals for summary
    const totalWorkers = worklog.labor.reduce((acc: number, entry: any) => {
        return acc + entry.workers.reduce((wAcc: number, w: any) => wAcc + Number(w.count), 0);
    }, 0);

    const totalMaterials = worklog.materials.length;
    const totalPhotos = worklog.photos.length;

    return (
        <Card>
            <Collapsible open={isOpen} onOpenChange={setIsOpen}>
                <CardHeader className="py-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                            <div className="flex flex-col">
                                <span className="text-sm text-muted-foreground">Date</span>
                                <span className="font-bold text-lg">{format(new Date(worklog.date), 'PPP')}</span>
                            </div>
                            <div className="hidden md:flex gap-2">
                                <Badge variant="secondary" className="flex gap-1">
                                    <Users className="h-3 w-3" /> {totalWorkers} Workers
                                </Badge>
                                <Badge variant="secondary" className="flex gap-1">
                                    <Package className="h-3 w-3" /> {totalMaterials} Materials
                                </Badge>
                                {totalPhotos > 0 && (
                                    <Badge variant="secondary" className="flex gap-1">
                                        <ImageIcon className="h-3 w-3" /> {totalPhotos} Photos
                                    </Badge>
                                )}
                            </div>
                        </div>
                        <CollapsibleTrigger asChild>
                            <Button variant="ghost" size="sm">
                                {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                            </Button>
                        </CollapsibleTrigger>
                    </div>
                    {/* Mobile Summary Badges */}
                    <div className="flex md:hidden gap-2 mt-2">
                        <Badge variant="secondary" className="flex gap-1">
                            <Users className="h-3 w-3" /> {totalWorkers}
                        </Badge>
                        <Badge variant="secondary" className="flex gap-1">
                            <Package className="h-3 w-3" /> {totalMaterials}
                        </Badge>
                        {totalPhotos > 0 && (
                            <Badge variant="secondary" className="flex gap-1">
                                <ImageIcon className="h-3 w-3" /> {totalPhotos}
                            </Badge>
                        )}
                    </div>
                </CardHeader>
                <CollapsibleContent>
                    <CardContent className="pt-0 space-y-6">

                        {/* Labor Details */}
                        {worklog.labor.length > 0 && (
                            <div>
                                <h4 className="font-semibold mb-2 flex items-center gap-2">
                                    <Users className="h-4 w-4" /> Labor Teams
                                </h4>
                                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                                    {worklog.labor.map((entry: any) => (
                                        <div key={entry.id} className="border rounded-md p-3 bg-muted/20 text-sm">
                                            <div className="flex justify-between items-start mb-2">
                                                <span className="font-medium">{entry.contractor_name}</span>
                                                <Badge variant={entry.payment_status === 'Paid' ? 'default' : 'outline'}>
                                                    {entry.payment_status}
                                                </Badge>
                                            </div>
                                            {entry.category && <div className="text-muted-foreground mb-2">{entry.category}</div>}
                                            <div className="space-y-1">
                                                {entry.workers.map((w: any) => (
                                                    <div key={w.id} className="flex justify-between text-xs">
                                                        <span>{w.worker_type}</span>
                                                        <span className="font-mono">{w.count}</span>
                                                    </div>
                                                ))}
                                            </div>
                                            {entry.work_description && (
                                                <div className="mt-2 pt-2 border-t text-xs text-muted-foreground italic">
                                                    "{entry.work_description}"
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Material Details */}
                        {worklog.materials.length > 0 && (
                            <div>
                                <h4 className="font-semibold mb-2 flex items-center gap-2">
                                    <Package className="h-4 w-4" /> Materials Consumed
                                </h4>
                                <div className="border rounded-md overflow-hidden">
                                    <table className="w-full text-sm text-left">
                                        <thead className="bg-muted text-muted-foreground">
                                            <tr>
                                                <th className="p-2 font-medium">Material</th>
                                                <th className="p-2 font-medium text-right">Quantity</th>
                                                <th className="p-2 font-medium">Unit</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y">
                                            {worklog.materials.map((mat: any) => (
                                                <tr key={mat.id}>
                                                    <td className="p-2">{mat.material_name}</td>
                                                    <td className="p-2 text-right font-mono">{mat.quantity_consumed}</td>
                                                    <td className="p-2 text-muted-foreground">{mat.unit}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Photos */}
                        {worklog.photos.length > 0 && (
                            <div>
                                <h4 className="font-semibold mb-2 flex items-center gap-2">
                                    <ImageIcon className="h-4 w-4" /> Photos
                                </h4>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    {worklog.photos.map((photo: any) => (
                                        <div key={photo.id} className="relative group rounded-md overflow-hidden border">
                                            <img
                                                src={photo.photo_url}
                                                alt={photo.caption || "Worklog photo"}
                                                className="w-full h-32 object-cover transition-transform hover:scale-105"
                                            />
                                            {photo.caption && (
                                                <div className="absolute bottom-0 left-0 right-0 bg-black/60 text-white text-xs p-1 truncate">
                                                    {photo.caption}
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                    </CardContent>
                </CollapsibleContent>
            </Collapsible>
        </Card>
    );
}
