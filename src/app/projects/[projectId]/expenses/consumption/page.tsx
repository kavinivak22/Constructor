'use client';

import { useState, useEffect, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { ArrowLeft, Package as PackageIcon, Search, Calendar, IndianRupee, Layers, FileDown } from 'lucide-react';
import { format } from 'date-fns';
import { Skeleton } from '@/components/ui/skeleton';
import { useProject } from '@/hooks/queries';

export default function MaterialConsumptionLogPage() {
  const { projectId } = useParams();
  const projectIdString = Array.isArray(projectId) ? projectId[0] : projectId;
  const router = useRouter();
  const { supabase } = useSupabase();
  const { data: project } = useProject(projectIdString);

  const [consumptionLogs, setConsumptionLogs] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const fetchConsumption = async () => {
      if (!projectIdString) return;
      setIsLoading(true);

      try {
        // Fetch daily worklogs with worklog_materials
        const { data: worklogs, error } = await supabase
          .from('daily_worklogs')
          .select(`
            id,
            title,
            date,
            created_at,
            worklog_materials (
              id,
              material_name,
              quantity_consumed,
              unit,
              project_material_id
            )
          `)
          .eq('project_id', projectIdString)
          .order('date', { ascending: false });

        if (error) throw error;

        // Fetch project materials unit costs to calculate financial cost of consumption
        const { data: projectMaterials } = await supabase
          .from('materials')
          .select('id, name, unit_cost');

        const matCostMap: Record<string, number> = {};
        (projectMaterials || []).forEach((m: any) => {
          matCostMap[m.name.toLowerCase()] = Number(m.unit_cost || 0);
        });

        // Map and compute financial values
        const logs: any[] = [];
        (worklogs || []).forEach((wl: any) => {
          (wl.worklog_materials || []).forEach((wm: any) => {
            const unitCost = matCostMap[wm.material_name.toLowerCase()] || 350; // Fallback unit cost ₹350
            const totalCost = Number(wm.quantity_consumed || 0) * unitCost;

            logs.push({
              id: wm.id,
              worklogId: wl.id,
              worklogTitle: wl.title || 'Daily Log',
              date: wl.date,
              materialName: wm.material_name,
              quantityConsumed: Number(wm.quantity_consumed || 0),
              unit: wm.unit || 'Units',
              unitCost,
              totalCost,
            });
          });
        });

        setConsumptionLogs(logs);
      } catch (err) {
        console.error('Error fetching material consumption:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchConsumption();
  }, [projectIdString, supabase]);

  const filteredLogs = useMemo(() => {
    return consumptionLogs.filter(log =>
      log.materialName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.worklogTitle.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [consumptionLogs, searchQuery]);

  const totalConsumptionValue = useMemo(() => {
    return filteredLogs.reduce((sum, item) => sum + item.totalCost, 0);
  }, [filteredLogs]);

  const formatCurrency = (amt: number) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amt);
  };

  return (
    <div className="flex flex-col min-h-screen bg-transparent">
      {/* Header */}
      <header className="flex items-center gap-3 px-4 py-3 glass sticky top-0 z-10 border-b border-white/10 h-14">
        <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0" onClick={() => router.back()}>
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1 min-w-0">
          <h1 className="text-sm sm:text-base font-bold font-headline truncate leading-none">
            Material Consumption Log & Analysis
          </h1>
          <p className="text-[11px] text-muted-foreground truncate leading-none mt-0.5">
            {project?.name || 'Project Site'}
          </p>
        </div>
      </header>

      <main className="flex-1 p-4 sm:p-6 max-w-4xl mx-auto w-full space-y-4">
        {/* Total Stat Card */}
        <Card className="glass-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
                <PackageIcon className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground font-medium">Total Site Material Value Consumed</p>
                <p className="text-xl font-bold text-foreground">{formatCurrency(totalConsumptionValue)}</p>
              </div>
            </div>
            <Badge variant="outline" className="bg-amber-500/10 text-amber-500 border-amber-500/20 font-bold px-2.5 py-1">
              {filteredLogs.length} Logged Items
            </Badge>
          </CardContent>
        </Card>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search material or worklog task..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-9 text-xs bg-background/50 border-white/10"
          />
        </div>

        {/* List */}
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-16 w-full rounded-xl" />)}
          </div>
        ) : filteredLogs.length > 0 ? (
          <div className="space-y-2.5">
            {filteredLogs.map((log) => (
              <Card key={log.id} className="glass-card hover:border-white/20 transition-all p-3.5 sm:p-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="space-y-1 min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-bold text-sm text-foreground truncate">{log.materialName}</span>
                      <Badge variant="secondary" className="text-[10px] bg-white/5 text-muted-foreground">
                        {log.quantityConsumed} {log.unit}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground flex items-center gap-1.5 truncate">
                      <Calendar className="h-3 w-3" />
                      <span>{log.date ? format(new Date(log.date), 'MMM dd, yyyy') : 'Site Log'}</span>
                      <span>•</span>
                      <span className="truncate">{log.worklogTitle}</span>
                    </p>
                  </div>

                  <div className="text-right shrink-0">
                    <p className="text-sm font-bold text-amber-500">{formatCurrency(log.totalCost)}</p>
                    <p className="text-[10px] text-muted-foreground">@{formatCurrency(log.unitCost)}/{log.unit}</p>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <Card className="glass-card p-8 text-center text-muted-foreground text-xs">
            No material consumption records found for this project.
          </Card>
        )}
      </main>
    </div>
  );
}
