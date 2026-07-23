'use client';

import { useEffect, useState } from 'react';
import { useI18n } from '@/lib/i18n-context';
import { getProjects } from '@/app/actions/financials';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Plus, TrendingDown, TrendingUp, AlertTriangle, CheckCircle2, Search, ArrowLeft, ShieldAlert } from 'lucide-react';
import Link from 'next/link';

interface MaterialItem {
  id: string;
  projectId: string;
  materialName: string;
  materialNameTa?: string;
  unit: string;
  committedQty: number;
  committedRate: number;
  actualQty: number;
  actualRate: number;
  severity: 'green' | 'amber' | 'red';
}

const initialMaterials: MaterialItem[] = [
  {
    id: 'm1',
    projectId: 'p1',
    materialName: 'Cement (OPC 53 Grade)',
    materialNameTa: 'சிமெண்ட் (OPC 53)',
    unit: 'bag',
    committedQty: 1200,
    committedRate: 395,
    actualQty: 1280,
    actualRate: 412,
    severity: 'amber',
  },
  {
    id: 'm2',
    projectId: 'p1',
    materialName: 'TMT Steel (Fe 550 D)',
    materialNameTa: 'TMT இரும்பு (Fe 550)',
    unit: 'kg',
    committedQty: 14000,
    committedRate: 64,
    actualQty: 13800,
    actualRate: 62,
    severity: 'green',
  },
  {
    id: 'm3',
    projectId: 'p1',
    materialName: 'M-Sand (Plastering & Masonry)',
    materialNameTa: 'M-மணல்',
    unit: 'unit',
    committedQty: 25,
    committedRate: 23000,
    actualQty: 28,
    actualRate: 25500,
    severity: 'red',
  },
  {
    id: 'm4',
    projectId: 'p1',
    materialName: 'AAC Blocks (600x200x150)',
    materialNameTa: 'AAC பிளாக்ஸ்',
    unit: 'nos',
    committedQty: 4500,
    committedRate: 210,
    actualQty: 4200,
    actualRate: 205,
    severity: 'green',
  },
];

export default function MaterialReconciliationPage() {
  const { t, language } = useI18n();
  const { toast } = useToast();
  const [projects, setProjects] = useState<any[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('all');
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [materials, setMaterials] = useState<MaterialItem[]>(initialMaterials);

  // New Material Dialog State
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [newMaterial, setNewMaterial] = useState({
    materialName: '',
    unit: 'bag',
    committedQty: '',
    committedRate: '',
    actualQty: '',
    actualRate: '',
  });

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      try {
        const data = await getProjects();
        setProjects(data || []);
      } catch (err) {
        console.error('Failed to load projects:', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, []);

  const filteredMaterials = materials.filter(m => {
    const matchesProject = selectedProjectId === 'all' || m.projectId === selectedProjectId;
    const matchesSearch =
      m.materialName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (m.materialNameTa || '').includes(searchQuery);
    return matchesProject && matchesSearch;
  });

  // KPI Calculations
  const totalCommitted = filteredMaterials.reduce((sum, m) => sum + m.committedQty * m.committedRate, 0);
  const totalActual = filteredMaterials.reduce((sum, m) => sum + m.actualQty * m.actualRate, 0);
  const netDrift = totalActual - totalCommitted;
  const driftPct = totalCommitted > 0 ? (netDrift / totalCommitted) * 100 : 0;

  const greenCount = filteredMaterials.filter(m => m.severity === 'green').length;
  const amberCount = filteredMaterials.filter(m => m.severity === 'amber').length;
  const redCount = filteredMaterials.filter(m => m.severity === 'red').length;

  const handleAddMaterial = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMaterial.materialName || !newMaterial.committedQty || !newMaterial.committedRate) {
      toast({
        title: 'Missing Fields',
        description: 'Please fill in all mandatory fields.',
        variant: 'destructive',
      });
      return;
    }

    const cQty = Number(newMaterial.committedQty);
    const cRate = Number(newMaterial.committedRate);
    const aQty = Number(newMaterial.actualQty || newMaterial.committedQty);
    const aRate = Number(newMaterial.actualRate || newMaterial.committedRate);

    const rateDriftPct = cRate > 0 ? ((aRate - cRate) / cRate) * 100 : 0;
    let severity: 'green' | 'amber' | 'red' = 'green';
    if (rateDriftPct > 10 || aQty > cQty * 1.1) {
      severity = 'red';
    } else if (rateDriftPct > 0 || aQty > cQty) {
      severity = 'amber';
    }

    const item: MaterialItem = {
      id: 'm_' + Date.now(),
      projectId: selectedProjectId === 'all' && projects.length > 0 ? projects[0].id : selectedProjectId,
      materialName: newMaterial.materialName,
      unit: newMaterial.unit,
      committedQty: cQty,
      committedRate: cRate,
      actualQty: aQty,
      actualRate: aRate,
      severity,
    };

    setMaterials([item, ...materials]);
    setIsAddOpen(false);
    setNewMaterial({ materialName: '', unit: 'bag', committedQty: '', committedRate: '', actualQty: '', actualRate: '' });
    toast({
      title: 'Material Added',
      description: `${item.materialName} added to price drift tracking.`,
    });
  };

  const getSeverityBadge = (severity: 'green' | 'amber' | 'red') => {
    switch (severity) {
      case 'green':
        return <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">On Budget</Badge>;
      case 'amber':
        return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30">Mild Drift (&lt;10%)</Badge>;
      case 'red':
        return <Badge className="bg-rose-500/20 text-rose-400 border-rose-500/30">Cost Overrun (&gt;10%)</Badge>;
    }
  };

  return (
    <div className="flex-1 p-4 md:p-8 space-y-6 max-w-7xl mx-auto overflow-y-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link href="/inventory">
              <Button variant="outline" size="icon" className="h-8 w-8 border-muted/30">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <h1 className="text-2xl font-bold tracking-tight font-headline text-foreground bg-gradient-to-r from-foreground via-foreground/80 to-muted-foreground bg-clip-text text-transparent">
              {t('materialReconciliation', 'Material Quantity & Rate Reconciliation')}
            </h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Compare budgeted material quantities and rates against actual purchases to prevent cost overruns and site wastage.
          </p>
        </div>

        <Button onClick={() => setIsAddOpen(true)} className="bg-primary hover:opacity-90 gap-1.5 self-start sm:self-auto">
          <Plus className="h-4 w-4" />
          <span>Add Material Drift Track</span>
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-3 bg-muted/10 border border-muted/20 rounded-xl">
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <span className="text-xs font-medium text-muted-foreground whitespace-nowrap">Filter Project:</span>
          <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
            <SelectTrigger className="w-full sm:w-[220px] bg-background/50 border-muted/30 h-9 text-xs">
              <SelectValue placeholder="All Projects" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Projects</SelectItem>
              {projects.map(p => (
                <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            placeholder={t('search', 'Search material...')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 bg-background/50 border-muted/30 h-9 text-xs"
          />
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Total Committed Budget</span>
            <span className="text-2xl font-bold text-foreground mt-1">₹{totalCommitted.toLocaleString('en-IN')}</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Estimated material cost</span>
          </CardContent>
        </Card>

        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Actual Purchased Spend</span>
            <span className="text-2xl font-bold text-sky-400 mt-1">₹{totalActual.toLocaleString('en-IN')}</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Realized material cost</span>
          </CardContent>
        </Card>

        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Net Cost Drift / Overrun</span>
            <div className="flex items-center gap-2 mt-1">
              <span className={`text-2xl font-bold ${netDrift > 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                ₹{Math.abs(netDrift).toLocaleString('en-IN')}
              </span>
              {netDrift > 0 ? (
                <TrendingUp className="h-5 w-5 text-rose-400" />
              ) : (
                <TrendingDown className="h-5 w-5 text-emerald-400" />
              )}
            </div>
            <span className="text-[10px] text-muted-foreground mt-0.5">
              {netDrift > 0 ? `+${driftPct.toFixed(1)}% over budget` : 'Within estimated budget'}
            </span>
          </CardContent>
        </Card>

        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Health Status Breakdown</span>
            <div className="flex items-center gap-3 mt-2">
              <div className="flex items-center gap-1 text-xs text-emerald-400 font-semibold">
                <CheckCircle2 className="h-3.5 w-3.5" />
                <span>{greenCount}</span>
              </div>
              <div className="flex items-center gap-1 text-xs text-amber-400 font-semibold">
                <AlertTriangle className="h-3.5 w-3.5" />
                <span>{amberCount}</span>
              </div>
              <div className="flex items-center gap-1 text-xs text-rose-400 font-semibold">
                <ShieldAlert className="h-3.5 w-3.5" />
                <span>{redCount}</span>
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground mt-1">On Budget / Mild Drift / Overrun</span>
          </CardContent>
        </Card>
      </div>

      {/* Main Reconciliation Table */}
      <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
        <CardHeader className="pb-3 border-b border-muted/20">
          <CardTitle className="text-lg font-semibold">Material Rate & Quantity Drift Table</CardTitle>
          <CardDescription>
            Live comparison of estimated quantities and rates versus actual site deliveries.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Loader2 className="h-7 w-7 animate-spin text-primary" />
              <p className="text-xs text-muted-foreground">Loading material reconciliation data...</p>
            </div>
          ) : filteredMaterials.length === 0 ? (
            <div className="text-center py-12 px-4">
              <p className="text-sm text-muted-foreground">No material items found matching your selection.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-muted/10">
                  <TableRow className="border-muted/20 text-xs">
                    <TableHead>Material Name</TableHead>
                    <TableHead className="text-center">Committed (Est.)</TableHead>
                    <TableHead className="text-center">Actual (Delivered)</TableHead>
                    <TableHead className="text-right">Qty Drift</TableHead>
                    <TableHead className="text-right">Rate Drift</TableHead>
                    <TableHead className="text-right">Net Financial Variance</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredMaterials.map((item) => {
                    const cCost = item.committedQty * item.committedRate;
                    const aCost = item.actualQty * item.actualRate;
                    const itemDrift = aCost - cCost;
                    const qtyDriftPct = ((item.actualQty - item.committedQty) / item.committedQty) * 100;
                    const rateDriftPct = ((item.actualRate - item.committedRate) / item.committedRate) * 100;

                    return (
                      <TableRow key={item.id} className="border-muted/15 hover:bg-muted/5 text-xs">
                        <TableCell className="font-semibold py-4">
                          <div className="flex flex-col">
                            <span className="text-foreground text-sm">{item.materialName}</span>
                            {language === 'ta' && item.materialNameTa && (
                              <span className="text-[11px] text-primary font-normal">{item.materialNameTa}</span>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex flex-col">
                            <span className="font-medium text-foreground">{item.committedQty} {item.unit}</span>
                            <span className="text-[10px] text-muted-foreground">@ ₹{item.committedRate.toLocaleString('en-IN')}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex flex-col">
                            <span className="font-medium text-foreground">{item.actualQty} {item.unit}</span>
                            <span className="text-[10px] text-muted-foreground">@ ₹{item.actualRate.toLocaleString('en-IN')}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          <span className={qtyDriftPct > 0 ? 'text-amber-400' : 'text-emerald-400'}>
                            {qtyDriftPct > 0 ? `+${qtyDriftPct.toFixed(1)}%` : `${qtyDriftPct.toFixed(1)}%`}
                          </span>
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          <span className={rateDriftPct > 0 ? 'text-rose-400' : 'text-emerald-400'}>
                            {rateDriftPct > 0 ? `+${rateDriftPct.toFixed(1)}%` : `${rateDriftPct.toFixed(1)}%`}
                          </span>
                        </TableCell>
                        <TableCell className="text-right font-bold font-mono">
                          <span className={itemDrift > 0 ? 'text-rose-400' : 'text-emerald-400'}>
                            {itemDrift > 0 ? `+₹${itemDrift.toLocaleString('en-IN')}` : `₹${itemDrift.toLocaleString('en-IN')}`}
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          {getSeverityBadge(item.severity)}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Dialog for adding new material track */}
      <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
        <DialogContent className="sm:max-w-md bg-background border-muted/30">
          <DialogHeader>
            <DialogTitle>Track Material Quantity & Rate Drift</DialogTitle>
            <DialogDescription>
              Enter estimated budget values alongside actual purchase rates to monitor price drift.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddMaterial} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label className="text-xs">Material Name</Label>
              <Input
                placeholder="e.g. UltraTech Cement, JSW TMT Steel"
                value={newMaterial.materialName}
                onChange={(e) => setNewMaterial({ ...newMaterial, materialName: e.target.value })}
                className="h-9 text-xs"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label className="text-xs">Unit</Label>
                <Select value={newMaterial.unit} onValueChange={(val) => setNewMaterial({ ...newMaterial, unit: val })}>
                  <SelectTrigger className="h-9 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="bag">bag</SelectItem>
                    <SelectItem value="kg">kg</SelectItem>
                    <SelectItem value="ton">ton</SelectItem>
                    <SelectItem value="unit">unit</SelectItem>
                    <SelectItem value="nos">nos</SelectItem>
                    <SelectItem value="sqft">sqft</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Committed Qty</Label>
                <Input
                  type="number"
                  placeholder="1000"
                  value={newMaterial.committedQty}
                  onChange={(e) => setNewMaterial({ ...newMaterial, committedQty: e.target.value })}
                  className="h-9 text-xs"
                  required
                />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1.5">
                <Label className="text-xs">Committed Rate (₹)</Label>
                <Input
                  type="number"
                  placeholder="395"
                  value={newMaterial.committedRate}
                  onChange={(e) => setNewMaterial({ ...newMaterial, committedRate: e.target.value })}
                  className="h-9 text-xs"
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Actual Qty</Label>
                <Input
                  type="number"
                  placeholder="1050"
                  value={newMaterial.actualQty}
                  onChange={(e) => setNewMaterial({ ...newMaterial, actualQty: e.target.value })}
                  className="h-9 text-xs"
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Actual Rate (₹)</Label>
                <Input
                  type="number"
                  placeholder="415"
                  value={newMaterial.actualRate}
                  onChange={(e) => setNewMaterial({ ...newMaterial, actualRate: e.target.value })}
                  className="h-9 text-xs"
                />
              </div>
            </div>
            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setIsAddOpen(false)} className="h-9 text-xs">
                Cancel
              </Button>
              <Button type="submit" className="h-9 text-xs bg-primary hover:opacity-90">
                Save Tracking Item
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
