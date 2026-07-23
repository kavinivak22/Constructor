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
import { Progress } from '@/components/ui/progress';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Plus, CheckCircle2, Clock, Calendar, ArrowLeft, Building2, Coins, CreditCard } from 'lucide-react';
import Link from 'next/link';

interface Milestone {
  id: string;
  projectId: string;
  stageName: string;
  stageNameTa?: string;
  pctShare: number;
  expectedAmount: number;
  collectedAmount: number;
  dueDate: string;
  status: 'collected' | 'pending' | 'upcoming';
}

const initialMilestones: Milestone[] = [
  {
    id: 'm1',
    projectId: 'p1',
    stageName: 'Booking Advance',
    stageNameTa: 'புக்கிங் அட்வான்ஸ்',
    pctShare: 10,
    expectedAmount: 850000,
    collectedAmount: 850000,
    dueDate: '2026-01-10',
    status: 'collected',
  },
  {
    id: 'm2',
    projectId: 'p1',
    stageName: 'Foundation Complete',
    stageNameTa: 'அஸ்திவாரம் முடிந்தது',
    pctShare: 15,
    expectedAmount: 1275000,
    collectedAmount: 1275000,
    dueDate: '2026-03-01',
    status: 'collected',
  },
  {
    id: 'm3',
    projectId: 'p1',
    stageName: 'Ground Floor Slab',
    stageNameTa: 'தரைதள ஸ்லாப்',
    pctShare: 20,
    expectedAmount: 1700000,
    collectedAmount: 1700000,
    dueDate: '2026-05-15',
    status: 'collected',
  },
  {
    id: 'm4',
    projectId: 'p1',
    stageName: 'First Floor Slab & Lintel',
    stageNameTa: 'முதல் தள ஸ்லாப்',
    pctShare: 20,
    expectedAmount: 1700000,
    collectedAmount: 1200000,
    dueDate: '2026-07-20',
    status: 'pending',
  },
  {
    id: 'm5',
    projectId: 'p1',
    stageName: 'Brickwork & Internal Plastering',
    stageNameTa: 'செங்கல் + பிளாஸ்டரிங்',
    pctShare: 20,
    expectedAmount: 1700000,
    collectedAmount: 0,
    dueDate: '2026-09-01',
    status: 'upcoming',
  },
  {
    id: 'm6',
    projectId: 'p1',
    stageName: 'Flooring, Painting & Handover',
    stageNameTa: 'பினிஷிங் + ஒப்படைப்பு',
    pctShare: 15,
    expectedAmount: 1275000,
    collectedAmount: 0,
    dueDate: '2026-11-15',
    status: 'upcoming',
  },
];

export default function ClientMilestonesPage() {
  const { t, language } = useI18n();
  const { toast } = useToast();
  const [projects, setProjects] = useState<any[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('all');
  const [isLoading, setIsLoading] = useState(true);
  const [milestones, setMilestones] = useState<Milestone[]>(initialMilestones);

  // Payment Recording Dialog
  const [selectedMilestone, setSelectedMilestone] = useState<Milestone | null>(null);
  const [paymentAmount, setPaymentAmount] = useState('');

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

  const filteredMilestones = milestones.filter(
    m => selectedProjectId === 'all' || m.projectId === selectedProjectId
  );

  const totalContractValue = filteredMilestones.reduce((sum, m) => sum + m.expectedAmount, 0);
  const totalCollected = filteredMilestones.reduce((sum, m) => sum + m.collectedAmount, 0);
  const totalPending = totalContractValue - totalCollected;
  const overallProgressPct = totalContractValue > 0 ? Math.round((totalCollected / totalContractValue) * 100) : 0;

  const handleRecordPayment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMilestone || !paymentAmount) return;

    const amt = Number(paymentAmount);
    setMilestones(prev =>
      prev.map(m => {
        if (m.id === selectedMilestone.id) {
          const newCollected = m.collectedAmount + amt;
          const newStatus = newCollected >= m.expectedAmount ? 'collected' : 'pending';
          return {
            ...m,
            collectedAmount: newCollected,
            status: newStatus,
          };
        }
        return m;
      })
    );

    toast({
      title: 'Payment Recorded',
      description: `₹${amt.toLocaleString('en-IN')} added to ${selectedMilestone.stageName}.`,
    });
    setSelectedMilestone(null);
    setPaymentAmount('');
  };

  const getStatusBadge = (status: 'collected' | 'pending' | 'upcoming') => {
    switch (status) {
      case 'collected':
        return <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">Fully Collected</Badge>;
      case 'pending':
        return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30">Partially Due</Badge>;
      case 'upcoming':
        return <Badge className="bg-sky-500/20 text-sky-400 border-sky-500/30">Upcoming</Badge>;
    }
  };

  return (
    <div className="flex-1 p-4 md:p-8 space-y-6 max-w-7xl mx-auto overflow-y-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link href="/projects">
              <Button variant="outline" size="icon" className="h-8 w-8 border-muted/30">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <h1 className="text-2xl font-bold tracking-tight font-headline text-foreground bg-gradient-to-r from-foreground via-foreground/80 to-muted-foreground bg-clip-text text-transparent">
              {t('clientMilestones', 'Client Payment Milestones')}
            </h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Track stage-wise progress payments collected from house owners and building clients.
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-3 bg-muted/10 border border-muted/20 rounded-xl">
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <span className="text-xs font-medium text-muted-foreground whitespace-nowrap">Choose Building / Site:</span>
          <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
            <SelectTrigger className="w-full sm:w-[240px] bg-background/50 border-muted/30 h-9 text-xs">
              <SelectValue placeholder="All Buildings" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Buildings</SelectItem>
              {projects.map(p => (
                <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>Overall Collection Progress:</span>
          <span className="font-bold text-foreground text-sm">{overallProgressPct}%</span>
        </div>
      </div>

      {/* Summary KPI Cards & Progress Bar */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Total Contract Value</span>
            <span className="text-2xl font-bold text-foreground mt-1">₹{totalContractValue.toLocaleString('en-IN')}</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Sum of all stage milestones</span>
          </CardContent>
        </Card>

        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Collected Income</span>
            <span className="text-2xl font-bold text-emerald-400 mt-1">₹{totalCollected.toLocaleString('en-IN')}</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Received to bank/cash</span>
          </CardContent>
        </Card>

        <Card className="glass-card border-muted/20">
          <CardContent className="p-4 flex flex-col justify-center">
            <span className="text-xs text-muted-foreground font-medium uppercase tracking-wider">Pending Collections</span>
            <span className="text-2xl font-bold text-amber-400 mt-1">₹{totalPending.toLocaleString('en-IN')}</span>
            <span className="text-[10px] text-muted-foreground mt-0.5">Remaining stage balance</span>
          </CardContent>
        </Card>
      </div>

      {/* Collection Progress Card */}
      <Card className="glass-card border-muted/20 p-4 space-y-2">
        <div className="flex justify-between items-center text-xs font-semibold">
          <span>Stage Payment Collection Bar</span>
          <span className="text-emerald-400">{overallProgressPct}% Completed</span>
        </div>
        <Progress value={overallProgressPct} className="h-3 bg-muted/30" />
      </Card>

      {/* Milestone Stages Stepper Table */}
      <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
        <CardHeader className="pb-3 border-b border-muted/20">
          <CardTitle className="text-lg font-semibold">Stage-wise Payment Schedule</CardTitle>
          <CardDescription>
            Milestone stages and payment breakdown for the building client.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Loader2 className="h-7 w-7 animate-spin text-primary" />
              <p className="text-xs text-muted-foreground">Loading milestone schedule...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-muted/10">
                  <TableRow className="border-muted/20 text-xs">
                    <TableHead className="w-[60px]">Stage</TableHead>
                    <TableHead>Construction Milestone</TableHead>
                    <TableHead className="text-center">% Share</TableHead>
                    <TableHead className="text-right">Expected Amount</TableHead>
                    <TableHead className="text-right">Collected Amount</TableHead>
                    <TableHead className="text-right">Balance Due</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredMilestones.map((m, idx) => {
                    const balance = m.expectedAmount - m.collectedAmount;
                    return (
                      <TableRow key={m.id} className="border-muted/15 hover:bg-muted/5 text-xs">
                        <TableCell className="font-bold text-muted-foreground text-center py-4">
                          #{idx + 1}
                        </TableCell>
                        <TableCell className="font-semibold">
                          <div className="flex flex-col">
                            <span className="text-foreground text-sm">{m.stageName}</span>
                            {language === 'ta' && m.stageNameTa && (
                              <span className="text-[11px] text-primary font-normal">{m.stageNameTa}</span>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-center font-semibold text-muted-foreground">
                          {m.pctShare}%
                        </TableCell>
                        <TableCell className="text-right font-mono font-semibold">
                          ₹{m.expectedAmount.toLocaleString('en-IN')}
                        </TableCell>
                        <TableCell className="text-right font-mono font-bold text-emerald-400">
                          ₹{m.collectedAmount.toLocaleString('en-IN')}
                        </TableCell>
                        <TableCell className="text-right font-mono font-bold text-amber-400">
                          ₹{balance > 0 ? balance.toLocaleString('en-IN') : 0}
                        </TableCell>
                        <TableCell className="text-center">
                          {getStatusBadge(m.status)}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={m.status === 'collected'}
                            onClick={() => {
                              setSelectedMilestone(m);
                              setPaymentAmount(String(balance > 0 ? balance : ''));
                            }}
                            className="h-8 text-xs border-muted/30 hover:bg-muted/10"
                          >
                            <CreditCard className="mr-1.5 h-3.5 w-3.5" />
                            Record Collection
                          </Button>
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

      {/* Record Payment Dialog */}
      <Dialog open={!!selectedMilestone} onOpenChange={() => setSelectedMilestone(null)}>
        <DialogContent className="sm:max-w-md bg-background border-muted/30">
          <DialogHeader>
            <DialogTitle>Record Milestone Collection</DialogTitle>
            <DialogDescription>
              Record client payment received for <span className="font-semibold text-foreground">{selectedMilestone?.stageName}</span>.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleRecordPayment} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label className="text-xs">Collection Amount (₹)</Label>
              <Input
                type="number"
                placeholder="500000"
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(e.target.value)}
                className="h-9 text-xs"
                required
              />
            </div>
            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setSelectedMilestone(null)} className="h-9 text-xs">
                Cancel
              </Button>
              <Button type="submit" className="h-9 text-xs bg-primary hover:opacity-90">
                Confirm Payment
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
