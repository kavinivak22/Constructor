'use client';

import { useEffect, useState, useMemo } from 'react';
import { getContractorAccounts } from '@/app/actions/contractors';
import { getProjects } from '@/app/actions/financials';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { useI18n } from '@/lib/i18n-context';
import { EditContractorDialog } from '@/components/contractors/edit-contractor-dialog';
import {
    Loader2,
    Search,
    Building2,
    Phone,
    User,
    Wallet,
    ArrowRight,
    Edit3
} from 'lucide-react';
import Link from 'next/link';

interface Contractor {
    id: string;
    name: string;
    category: string | null;
    contactPerson: string | null;
    phone: string | null;
    email: string | null;
}

interface AccountSummary {
    totalDue: number;
    totalPaid: number;
    totalPending: number;
    items: any[];
}

interface ContractorAccount {
    contractor: Contractor;
    rateAccount: AccountSummary;
    nmrAccount: AccountSummary;
    allTransactions: any[];
}

export default function ContractorAccountsPage() {
    const { t } = useI18n();
    const { toast } = useToast();
    const router = useRouter();

    const [accounts, setAccounts] = useState<ContractorAccount[]>([]);
    const [projects, setProjects] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedBuildingId, setSelectedBuildingId] = useState<string>('all');
    const [viewMode, setViewMode] = useState<'contractors' | 'buildings'>('contractors');

    // Edit modal state
    const [editingContractor, setEditingContractor] = useState<Contractor | null>(null);
    const [isEditOpen, setIsEditOpen] = useState(false);

    const loadAccounts = async () => {
        setIsLoading(true);
        try {
            const res = await getContractorAccounts();
            if (res.success && res.data) {
                setAccounts(res.data as ContractorAccount[]);
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to load contractor accounts.',
                    variant: 'destructive',
                });
            }
        } catch (error) {
            console.error('Error loading contractor accounts:', error);
            toast({
                title: 'Error',
                description: 'Failed to load accounts data.',
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
        }
    };

    const loadProjects = async () => {
        try {
            const data = await getProjects();
            setProjects(data || []);
            if (data && data.length > 0) {
                setSelectedBuildingId(data[0].id);
            }
        } catch (error) {
            console.error('Error loading projects:', error);
        }
    };

    useEffect(() => {
        loadAccounts();
        loadProjects();
    }, []);

    const filteredAccounts = useMemo(() => {
        return accounts.filter(acc =>
            acc.contractor.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            (acc.contractor.category || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
            (acc.contractor.contactPerson || '').toLowerCase().includes(searchQuery.toLowerCase())
        );
    }, [accounts, searchQuery]);

    // Summary calculations
    const stats = useMemo(() => {
        return {
            totalRateOutstanding: accounts.reduce((sum, a) => sum + a.rateAccount.totalPending, 0),
            totalRateSettled: accounts.reduce((sum, a) => sum + a.rateAccount.totalPaid, 0),
            totalNmrOutstanding: accounts.reduce((sum, a) => sum + a.nmrAccount.totalPending, 0),
            totalNmrSettled: accounts.reduce((sum, a) => sum + a.nmrAccount.totalPaid, 0),
        };
    }, [accounts]);

    const buildingStats = useMemo(() => {
        if (selectedBuildingId === 'all') return { rateSettled: 0, rateOutstanding: 0, nmrSettled: 0, nmrOutstanding: 0 };
        return {
            rateSettled: accounts.reduce((sum, a) => {
                const matchedTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId && tx.payout_class === 'rate' && tx.status === 'paid');
                return sum + matchedTx.reduce((s, t) => s + Number(t.amount_paid), 0);
            }, 0),
            rateOutstanding: accounts.reduce((sum, a) => {
                const matchedTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId && tx.payout_class === 'rate' && tx.status === 'pending');
                return sum + matchedTx.reduce((s, t) => s + Number(t.amount_paid), 0);
            }, 0),
            nmrSettled: accounts.reduce((sum, a) => {
                const matchedTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId && (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'paid');
                return sum + matchedTx.reduce((s, t) => s + Number(t.amount_paid), 0);
            }, 0),
            nmrOutstanding: accounts.reduce((sum, a) => {
                const matchedTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId && (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'pending');
                return sum + matchedTx.reduce((s, t) => s + Number(t.amount_paid), 0);
            }, 0),
        };
    }, [accounts, selectedBuildingId]);

    const contractorsForSelectedBuilding = useMemo(() => {
        return accounts.map(a => {
            const buildingTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId);
            if (buildingTx.length === 0) return null;

            const ratePaid = buildingTx.filter(tx => tx.payout_class === 'rate' && tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
            const ratePending = buildingTx.filter(tx => tx.payout_class === 'rate' && tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

            const nmrPaid = buildingTx.filter(tx => (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
            const nmrPending = buildingTx.filter(tx => (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

            return {
                ...a,
                buildingStats: { ratePaid, ratePending, nmrPaid, nmrPending }
            };
        }).filter(Boolean) as (ContractorAccount & { buildingStats: { ratePaid: number, ratePending: number, nmrPaid: number, nmrPending: number } })[];
    }, [accounts, selectedBuildingId]);

    const handleEditClick = (e: React.MouseEvent, contractor: Contractor) => {
        e.stopPropagation();
        setEditingContractor(contractor);
        setIsEditOpen(true);
    };

    return (
        <main className="flex-1 p-3.5 sm:p-4 md:p-6 overflow-y-auto bg-transparent">
            <div className="max-w-6xl mx-auto space-y-4 sm:space-y-6">

                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-border/40 pb-4">
                    <div>
                        <h1 className="text-xl sm:text-3xl font-bold tracking-tight font-headline text-foreground">
                            {t('contractorAccounts', 'Contractor Accounts & Ledgers')}
                        </h1>
                        <p className="text-xs sm:text-sm text-muted-foreground mt-0.5">
                            Track Rate contract settlements & NMR daily wage accounts across sites.
                        </p>
                    </div>
                </div>

                {/* View Switcher & Building Filter Bar */}
                <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3 p-2 sm:p-3 glass-card rounded-2xl border border-white/10">
                    <Tabs value={viewMode} onValueChange={(val) => setViewMode(val as 'contractors' | 'buildings')} className="w-full sm:w-auto">
                        <TabsList className="grid grid-cols-2 w-full sm:w-auto bg-muted/30 p-1 rounded-xl">
                            <TabsTrigger value="contractors" className="text-xs font-semibold px-3 py-1.5 rounded-lg">
                                By Contractor
                            </TabsTrigger>
                            <TabsTrigger value="buildings" className="text-xs font-semibold px-3 py-1.5 rounded-lg">
                                By Building / Site
                            </TabsTrigger>
                        </TabsList>
                    </Tabs>

                    {viewMode === 'buildings' && projects.length > 0 && (
                        <div className="flex items-center gap-2 w-full sm:w-auto">
                            <span className="text-xs text-muted-foreground whitespace-nowrap font-medium hidden sm:inline">Building:</span>
                            <Select
                                value={selectedBuildingId}
                                onValueChange={(val) => setSelectedBuildingId(val)}
                            >
                                <SelectTrigger className="w-full sm:w-[220px] h-9 text-xs rounded-xl bg-background/50">
                                    <SelectValue placeholder="Choose a building" />
                                </SelectTrigger>
                                <SelectContent>
                                    {projects.map(proj => (
                                        <SelectItem key={proj.id} value={proj.id}>{proj.name}</SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    )}
                </div>

                {/* Summary Stat Cards (2x2 Grid on Mobile) */}
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-4">
                    <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 transition-all duration-200">
                        <CardContent className="p-3 sm:p-4 flex flex-col justify-between">
                            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase font-semibold tracking-wider">Rate Outstanding</span>
                            <span className="text-lg sm:text-2xl font-bold font-headline text-purple-600 dark:text-purple-400 mt-1">
                                ₹{(viewMode === 'contractors' ? stats.totalRateOutstanding : (selectedBuildingId === 'all' ? 0 : buildingStats.rateOutstanding)).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] sm:text-xs text-muted-foreground mt-0.5 truncate">Pending sq.ft rates</span>
                        </CardContent>
                    </Card>

                    <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 transition-all duration-200">
                        <CardContent className="p-3 sm:p-4 flex flex-col justify-between">
                            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase font-semibold tracking-wider">Rate Settled</span>
                            <span className="text-lg sm:text-2xl font-bold font-headline text-emerald-600 dark:text-emerald-400 mt-1">
                                ₹{(viewMode === 'contractors' ? stats.totalRateSettled : (selectedBuildingId === 'all' ? 0 : buildingStats.rateSettled)).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] sm:text-xs text-muted-foreground mt-0.5 truncate">Cleared contract payouts</span>
                        </CardContent>
                    </Card>

                    <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 transition-all duration-200">
                        <CardContent className="p-3 sm:p-4 flex flex-col justify-between">
                            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase font-semibold tracking-wider">NMR Outstanding</span>
                            <span className="text-lg sm:text-2xl font-bold font-headline text-amber-600 dark:text-amber-400 mt-1">
                                ₹{(viewMode === 'contractors' ? stats.totalNmrOutstanding : (selectedBuildingId === 'all' ? 0 : buildingStats.nmrOutstanding)).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] sm:text-xs text-muted-foreground mt-0.5 truncate">Pending labor wages</span>
                        </CardContent>
                    </Card>

                    <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 transition-all duration-200">
                        <CardContent className="p-3 sm:p-4 flex flex-col justify-between">
                            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase font-semibold tracking-wider">NMR Settled</span>
                            <span className="text-lg sm:text-2xl font-bold font-headline text-sky-600 dark:text-sky-400 mt-1">
                                ₹{(viewMode === 'contractors' ? stats.totalNmrSettled : (selectedBuildingId === 'all' ? 0 : buildingStats.nmrSettled)).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] sm:text-xs text-muted-foreground mt-0.5 truncate">Cleared labor wages</span>
                        </CardContent>
                    </Card>
                </div>

                {/* Accounts Container */}
                <Card className="glass-card rounded-2xl border border-white/10 dark:border-white/5 overflow-hidden shadow-xl">
                    <CardHeader className="p-3.5 sm:p-5 border-b border-border/40">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                            <div>
                                <CardTitle className="text-base sm:text-xl font-bold font-headline text-foreground">
                                    {viewMode === 'contractors' ? 'Active Contractor Accounts' : 'Active Contractors on Site'}
                                </CardTitle>
                                <CardDescription className="text-xs sm:text-sm">
                                    Consolidated ledger showing Rate contracts vs NMR daily wages.
                                </CardDescription>
                            </div>

                            <div className="relative w-full sm:w-72">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                <Input
                                    placeholder="Search contractor..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="pl-9 w-full h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                />
                            </div>
                        </div>
                    </CardHeader>

                    <CardContent className="p-0">
                        {isLoading ? (
                            <div className="flex flex-col items-center justify-center py-16 gap-3">
                                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                <p className="text-xs text-muted-foreground">Loading contractor accounts...</p>
                            </div>
                        ) : viewMode === 'contractors' ? (
                            filteredAccounts.length === 0 ? (
                                <div className="text-center py-12 px-4">
                                    <Building2 className="h-10 w-10 text-muted-foreground/40 mx-auto mb-2" />
                                    <h3 className="text-base font-bold text-foreground">No Contractor Accounts Found</h3>
                                    <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                                        Contractors with payday transactions or active wage profiles will appear here.
                                    </p>
                                </div>
                            ) : (
                                <>
                                    {/* Desktop Table View */}
                                    <div className="hidden md:block overflow-x-auto">
                                        <Table className="text-xs sm:text-sm">
                                            <TableHeader className="bg-muted/30">
                                                <TableRow className="border-border/40">
                                                    <TableHead className="font-semibold">Contractor</TableHead>
                                                    <TableHead className="font-semibold">Category</TableHead>
                                                    <TableHead className="text-center font-semibold bg-purple-500/5">Rate Contract (Sq.Ft)</TableHead>
                                                    <TableHead className="text-center font-semibold bg-teal-500/5">NMR Account (Labor)</TableHead>
                                                    <TableHead className="text-right font-semibold">Actions</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {filteredAccounts.map((account) => (
                                                    <TableRow key={account.contractor.id} className="border-border/30 hover:bg-muted/20 transition-colors">
                                                        <TableCell className="font-medium">
                                                            <div className="flex flex-col">
                                                                <span className="font-bold text-foreground text-sm">{account.contractor.name}</span>
                                                                {account.contractor.contactPerson && (
                                                                    <span className="text-[11px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                )}
                                                            </div>
                                                        </TableCell>

                                                        <TableCell>
                                                            <Badge variant="secondary" className="text-[11px] rounded-full px-2 py-0.5">
                                                                {account.contractor.category || 'General Contractor'}
                                                            </Badge>
                                                        </TableCell>

                                                        <TableCell className="bg-purple-500/5 text-center py-2.5">
                                                            <div className="flex flex-col items-center space-y-0.5">
                                                                <span className="text-xs font-semibold text-foreground">
                                                                    ₹{account.rateAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-500 font-normal">(Cleared)</span>
                                                                </span>
                                                                <span className="text-xs font-bold text-purple-600 dark:text-purple-400">
                                                                    ₹{account.rateAccount.totalPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Due)</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>

                                                        <TableCell className="bg-teal-500/5 text-center py-2.5">
                                                            <div className="flex flex-col items-center space-y-0.5">
                                                                <span className="text-xs font-semibold text-foreground">
                                                                    ₹{account.nmrAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-500 font-normal">(Cleared)</span>
                                                                </span>
                                                                <span className="text-xs font-bold text-teal-600 dark:text-teal-400">
                                                                    ₹{account.nmrAccount.totalPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Due)</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>

                                                        <TableCell className="text-right">
                                                            <div className="flex items-center justify-end gap-1.5">
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    onClick={(e) => handleEditClick(e, account.contractor)}
                                                                    className="h-8 w-8 text-muted-foreground hover:text-foreground"
                                                                >
                                                                    <Edit3 className="h-4 w-4" />
                                                                </Button>
                                                                <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                                    <Button size="sm" variant="outline" className="h-8 text-xs rounded-xl gap-1">
                                                                        <span>View Ledger</span>
                                                                        <ArrowRight className="h-3.5 w-3.5" />
                                                                    </Button>
                                                                </Link>
                                                            </div>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>

                                    {/* Mobile Contractor Account Cards */}
                                    <div className="block md:hidden p-3.5 space-y-3">
                                        {filteredAccounts.map((account) => (
                                            <div
                                                key={account.contractor.id}
                                                className="glass-card rounded-2xl p-3.5 border border-white/10 dark:border-white/5 space-y-3"
                                            >
                                                <div className="flex items-start justify-between gap-2">
                                                    <div className="min-w-0 flex-1">
                                                        <h3 className="font-bold text-sm text-foreground truncate">{account.contractor.name}</h3>
                                                        {account.contractor.contactPerson && (
                                                            <p className="text-[11px] text-muted-foreground truncate">Contact: {account.contractor.contactPerson}</p>
                                                        )}
                                                    </div>

                                                    <div className="flex items-center gap-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            onClick={(e) => handleEditClick(e, account.contractor)}
                                                            className="h-7 w-7 text-muted-foreground"
                                                        >
                                                            <Edit3 className="h-3.5 w-3.5" />
                                                        </Button>
                                                        <Badge variant="outline" className="text-[10px] font-semibold bg-primary/10 border-primary/20 text-primary rounded-full px-2 py-0.5 shrink-0">
                                                            {account.contractor.category || 'General'}
                                                        </Badge>
                                                    </div>
                                                </div>

                                                <div className="grid grid-cols-2 gap-2 text-xs border-t border-b border-border/30 py-2.5">
                                                    <div className="bg-purple-500/10 p-2 rounded-xl flex flex-col justify-center">
                                                        <span className="text-[10px] text-muted-foreground font-semibold uppercase">Rate Contract</span>
                                                        <span className="font-bold text-purple-600 dark:text-purple-300 mt-0.5 text-xs">
                                                            ₹{account.rateAccount.totalPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                        </span>
                                                        <span className="text-[10px] text-emerald-600 dark:text-emerald-400">
                                                            ₹{account.rateAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Cleared</span>
                                                        </span>
                                                    </div>

                                                    <div className="bg-teal-500/10 p-2 rounded-xl flex flex-col justify-center">
                                                        <span className="text-[10px] text-muted-foreground font-semibold uppercase">NMR Labor</span>
                                                        <span className="font-bold text-teal-600 dark:text-teal-300 mt-0.5 text-xs">
                                                            ₹{account.nmrAccount.totalPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                        </span>
                                                        <span className="text-[10px] text-emerald-600 dark:text-emerald-400">
                                                            ₹{account.nmrAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Cleared</span>
                                                        </span>
                                                    </div>
                                                </div>

                                                <Link href={`/financials/contractors/${account.contractor.id}`} className="block">
                                                    <Button size="sm" variant="outline" className="w-full h-8 text-xs rounded-xl gap-1.5 justify-center">
                                                        <span>View Ledger Statement</span>
                                                        <ArrowRight className="h-3.5 w-3.5" />
                                                    </Button>
                                                </Link>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )
                        ) : (
                            /* Building View */
                            contractorsForSelectedBuilding.length === 0 ? (
                                <div className="text-center py-12 px-4">
                                    <Building2 className="h-10 w-10 text-muted-foreground/40 mx-auto mb-2" />
                                    <h3 className="text-base font-bold text-foreground">No Contractors Found on Site</h3>
                                    <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                                        No transactions logged for this building yet.
                                    </p>
                                </div>
                            ) : (
                                <>
                                    {/* Desktop Table */}
                                    <div className="hidden md:block overflow-x-auto">
                                        <Table className="text-xs sm:text-sm">
                                            <TableHeader className="bg-muted/30">
                                                <TableRow className="border-border/40">
                                                    <TableHead className="font-semibold">Contractor</TableHead>
                                                    <TableHead className="font-semibold">Category</TableHead>
                                                    <TableHead className="text-center font-semibold bg-purple-500/5">Rate Contract (Sq.Ft)</TableHead>
                                                    <TableHead className="text-center font-semibold bg-teal-500/5">NMR Account (Labor)</TableHead>
                                                    <TableHead className="text-right font-semibold">Actions</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {contractorsForSelectedBuilding.map((account) => (
                                                    <TableRow key={account.contractor.id} className="border-border/30 hover:bg-muted/20 transition-colors">
                                                        <TableCell className="font-medium">
                                                            <div className="flex flex-col">
                                                                <span className="font-bold text-foreground text-sm">{account.contractor.name}</span>
                                                                {account.contractor.contactPerson && (
                                                                    <span className="text-[11px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                )}
                                                            </div>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Badge variant="secondary" className="text-[11px] rounded-full px-2 py-0.5">
                                                                {account.contractor.category || 'General Contractor'}
                                                            </Badge>
                                                        </TableCell>
                                                        <TableCell className="bg-purple-500/5 text-center py-2.5">
                                                            <div className="flex flex-col items-center space-y-0.5">
                                                                <span className="text-xs font-semibold text-foreground">
                                                                    ₹{account.buildingStats.ratePaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-500 font-normal">(Cleared)</span>
                                                                </span>
                                                                <span className="text-xs font-bold text-purple-600 dark:text-purple-400">
                                                                    ₹{account.buildingStats.ratePending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Due)</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell className="bg-teal-500/5 text-center py-2.5">
                                                            <div className="flex flex-col items-center space-y-0.5">
                                                                <span className="text-xs font-semibold text-foreground">
                                                                    ₹{account.buildingStats.nmrPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-500 font-normal">(Cleared)</span>
                                                                </span>
                                                                <span className="text-xs font-bold text-teal-600 dark:text-teal-400">
                                                                    ₹{account.buildingStats.nmrPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Due)</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell className="text-right">
                                                            <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                                <Button size="sm" variant="outline" className="h-8 text-xs rounded-xl gap-1">
                                                                    <span>View Site Ledger</span>
                                                                    <ArrowRight className="h-3.5 w-3.5" />
                                                                </Button>
                                                            </Link>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>

                                    {/* Mobile Cards for Building View */}
                                    <div className="block md:hidden p-3.5 space-y-3">
                                        {contractorsForSelectedBuilding.map((account) => (
                                            <div
                                                key={account.contractor.id}
                                                className="glass-card rounded-2xl p-3.5 border border-white/10 dark:border-white/5 space-y-3"
                                            >
                                                <div className="flex items-start justify-between gap-2">
                                                    <div className="min-w-0 flex-1">
                                                        <h3 className="font-bold text-sm text-foreground truncate">{account.contractor.name}</h3>
                                                        {account.contractor.contactPerson && (
                                                            <p className="text-[11px] text-muted-foreground truncate">Contact: {account.contractor.contactPerson}</p>
                                                        )}
                                                    </div>
                                                    <Badge variant="outline" className="text-[10px] font-semibold bg-primary/10 border-primary/20 text-primary rounded-full px-2 py-0.5 shrink-0">
                                                        {account.contractor.category || 'General'}
                                                    </Badge>
                                                </div>

                                                <div className="grid grid-cols-2 gap-2 text-xs border-t border-b border-border/30 py-2.5">
                                                    <div className="bg-purple-500/10 p-2 rounded-xl flex flex-col justify-center">
                                                        <span className="text-[10px] text-muted-foreground font-semibold uppercase">Rate Contract</span>
                                                        <span className="font-bold text-purple-600 dark:text-purple-300 mt-0.5 text-xs">
                                                            ₹{account.buildingStats.ratePending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                        </span>
                                                        <span className="text-[10px] text-emerald-600 dark:text-emerald-400">
                                                            ₹{account.buildingStats.ratePaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Cleared</span>
                                                        </span>
                                                    </div>

                                                    <div className="bg-teal-500/10 p-2 rounded-xl flex flex-col justify-center">
                                                        <span className="text-[10px] text-muted-foreground font-semibold uppercase">NMR Labor</span>
                                                        <span className="font-bold text-teal-600 dark:text-teal-300 mt-0.5 text-xs">
                                                            ₹{account.buildingStats.nmrPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                        </span>
                                                        <span className="text-[10px] text-emerald-600 dark:text-emerald-400">
                                                            ₹{account.buildingStats.nmrPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Cleared</span>
                                                        </span>
                                                    </div>
                                                </div>

                                                <Link href={`/financials/contractors/${account.contractor.id}`} className="block">
                                                    <Button size="sm" variant="outline" className="w-full h-8 text-xs rounded-xl gap-1.5 justify-center">
                                                        <span>View Site Ledger</span>
                                                        <ArrowRight className="h-3.5 w-3.5" />
                                                    </Button>
                                                </Link>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )
                        )}
                    </CardContent>
                </Card>

                {/* Edit Contractor Modal */}
                <EditContractorDialog
                    open={isEditOpen}
                    onOpenChange={setIsEditOpen}
                    contractor={editingContractor}
                    onSuccess={() => loadAccounts()}
                />

            </div>
        </main>
    );
}
