'use client';

import { useEffect, useState, useMemo } from 'react';
import { getContractorAccounts } from '@/app/actions/contractors';
import { getProjects } from '@/app/actions/financials';
import { useRouter } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { useI18n } from '@/lib/i18n-context';
import { CreateContractorDialog } from '@/components/contractors/create-contractor-dialog';
import { EditContractorDialog } from '@/components/contractors/edit-contractor-dialog';
import {
    Loader2,
    Search,
    Building2,
    Phone,
    User,
    UserPlus,
    ArrowRight,
    Edit3,
    X,
    ChevronRight,
    Coins,
    Hammer
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

    // Modal states
    const [editingContractor, setEditingContractor] = useState<Contractor | null>(null);
    const [isEditOpen, setIsEditOpen] = useState(false);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

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

    const currentRateDue = viewMode === 'contractors' ? stats.totalRateOutstanding : (selectedBuildingId === 'all' ? 0 : buildingStats.rateOutstanding);
    const currentRatePaid = viewMode === 'contractors' ? stats.totalRateSettled : (selectedBuildingId === 'all' ? 0 : buildingStats.rateSettled);
    const currentNmrDue = viewMode === 'contractors' ? stats.totalNmrOutstanding : (selectedBuildingId === 'all' ? 0 : buildingStats.nmrOutstanding);
    const currentNmrPaid = viewMode === 'contractors' ? stats.totalNmrSettled : (selectedBuildingId === 'all' ? 0 : buildingStats.nmrSettled);

    return (
        <main className="flex-1 p-2.5 sm:p-4 md:p-5 overflow-y-auto bg-transparent">
            <div className="max-w-6xl mx-auto space-y-2.5 sm:space-y-3.5">

                {/* Compact Header */}
                <div className="flex items-center justify-between gap-2 pb-1">
                    <div className="min-w-0">
                        <h1 className="text-base sm:text-xl font-bold tracking-tight font-headline text-foreground truncate">
                            {t('contractorAccounts', 'Contractor Accounts')}
                        </h1>
                        <p className="text-[11px] sm:text-xs text-muted-foreground truncate">
                            Rate contracts & NMR daily wage ledgers
                        </p>
                    </div>
                    <Button
                        onClick={() => setIsCreateOpen(true)}
                        size="sm"
                        className="gap-1 rounded-lg h-7 sm:h-8 text-xs font-semibold shrink-0 shadow-2xs px-2.5 sm:px-3"
                    >
                        <UserPlus className="h-3.5 w-3.5" />
                        <span className="hidden xs:inline">{t('addContractor', 'Add Contractor')}</span>
                        <span className="xs:hidden">Add</span>
                    </Button>
                </div>

                {/* Compact 2-Tile KPI Summary Strip (Saves ~50% vertical space on mobile) */}
                <div className="grid grid-cols-2 gap-2">
                    {/* Rate Contract Card */}
                    <div className="glass-card rounded-xl p-2 sm:p-2.5 border border-purple-500/20 bg-purple-500/[0.03]">
                        <div className="flex items-center justify-between mb-1">
                            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider text-purple-600 dark:text-purple-300 flex items-center gap-1">
                                <Coins className="w-3 h-3 text-purple-500" />
                                Rate (Sq.Ft)
                            </span>
                        </div>
                        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-0.5">
                            <div>
                                <span className="text-sm sm:text-lg font-bold font-headline text-purple-600 dark:text-purple-400 block leading-tight">
                                    ₹{currentRateDue.toLocaleString('en-IN')}
                                </span>
                                <span className="text-[9px] text-muted-foreground">Due</span>
                            </div>
                            <div className="sm:text-right">
                                <span className="text-xs sm:text-sm font-semibold text-emerald-600 dark:text-emerald-400 block leading-tight">
                                    ₹{currentRatePaid.toLocaleString('en-IN')}
                                </span>
                                <span className="text-[9px] text-muted-foreground">Cleared</span>
                            </div>
                        </div>
                    </div>

                    {/* NMR Labor Card */}
                    <div className="glass-card rounded-xl p-2 sm:p-2.5 border border-teal-500/20 bg-teal-500/[0.03]">
                        <div className="flex items-center justify-between mb-1">
                            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider text-teal-600 dark:text-teal-300 flex items-center gap-1">
                                <Hammer className="w-3 h-3 text-teal-500" />
                                NMR (Labor)
                            </span>
                        </div>
                        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-0.5">
                            <div>
                                <span className="text-sm sm:text-lg font-bold font-headline text-amber-600 dark:text-amber-400 block leading-tight">
                                    ₹{currentNmrDue.toLocaleString('en-IN')}
                                </span>
                                <span className="text-[9px] text-muted-foreground">Due</span>
                            </div>
                            <div className="sm:text-right">
                                <span className="text-xs sm:text-sm font-semibold text-emerald-600 dark:text-emerald-400 block leading-tight">
                                    ₹{currentNmrPaid.toLocaleString('en-IN')}
                                </span>
                                <span className="text-[9px] text-muted-foreground">Cleared</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Compact Single Unified Toolbar (Search + View Switcher + Building Selector) */}
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 p-1.5 sm:p-2 glass-card rounded-xl border border-white/10">
                    <div className="flex items-center gap-1.5 flex-1 min-w-0">
                        {/* Search Input with quick clear */}
                        <div className="relative flex-1 min-w-0">
                            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
                            <Input
                                placeholder="Search contractor or trade..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="pl-8 pr-7 h-7 sm:h-8 text-xs rounded-lg bg-background/50 border-border/40 w-full"
                            />
                            {searchQuery && (
                                <button
                                    onClick={() => setSearchQuery('')}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                >
                                    <X className="h-3.5 w-3.5" />
                                </button>
                            )}
                        </div>

                        {/* Segmented View Toggle Pills */}
                        <div className="flex items-center bg-muted/40 p-0.5 rounded-lg shrink-0 border border-border/30">
                            <button
                                onClick={() => setViewMode('contractors')}
                                className={`text-[11px] px-2 py-1 rounded-md font-medium transition-colors ${
                                    viewMode === 'contractors'
                                        ? 'bg-background shadow-xs text-foreground font-semibold'
                                        : 'text-muted-foreground hover:text-foreground'
                                }`}
                            >
                                Contractors
                            </button>
                            <button
                                onClick={() => setViewMode('buildings')}
                                className={`text-[11px] px-2 py-1 rounded-md font-medium transition-colors ${
                                    viewMode === 'buildings'
                                        ? 'bg-background shadow-xs text-foreground font-semibold'
                                        : 'text-muted-foreground hover:text-foreground'
                                }`}
                            >
                                By Site
                            </button>
                        </div>
                    </div>

                    {/* Site Dropdown when in By Site view */}
                    {viewMode === 'buildings' && projects.length > 0 && (
                        <div className="flex items-center gap-1.5 w-full sm:w-auto shrink-0 pt-1 sm:pt-0 border-t sm:border-t-0 border-border/30">
                            <span className="text-[11px] text-muted-foreground font-medium shrink-0">Site:</span>
                            <Select
                                value={selectedBuildingId}
                                onValueChange={(val) => setSelectedBuildingId(val)}
                            >
                                <SelectTrigger className="w-full sm:w-[180px] h-7 sm:h-8 text-xs rounded-lg bg-background/50">
                                    <SelectValue placeholder="Choose a building" />
                                </SelectTrigger>
                                <SelectContent>
                                    {projects.map(proj => (
                                        <SelectItem key={proj.id} value={proj.id} className="text-xs">
                                            {proj.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    )}
                </div>

                {/* Accounts Content Container */}
                <Card className="glass-card rounded-xl border border-white/10 dark:border-white/5 overflow-hidden shadow-xs">
                    <CardContent className="p-0">
                        {isLoading ? (
                            <div className="flex flex-col items-center justify-center py-10 gap-2">
                                <Loader2 className="h-6 w-6 animate-spin text-primary" />
                                <p className="text-xs text-muted-foreground">Loading accounts...</p>
                            </div>
                        ) : viewMode === 'contractors' ? (
                            filteredAccounts.length === 0 ? (
                                <div className="text-center py-8 px-4">
                                    <Building2 className="h-7 w-7 text-muted-foreground/40 mx-auto mb-1" />
                                    <h3 className="text-xs sm:text-sm font-bold text-foreground">No Contractors Found</h3>
                                    <p className="text-[11px] text-muted-foreground max-w-sm mx-auto mt-0.5 mb-2.5">
                                        {searchQuery ? 'No contractors match your search criteria.' : 'Add your first contractor to start tracking ledgers.'}
                                    </p>
                                    {!searchQuery && (
                                        <Button
                                            onClick={() => setIsCreateOpen(true)}
                                            size="sm"
                                            className="gap-1 rounded-lg text-xs font-semibold h-7 px-3"
                                        >
                                            <UserPlus className="h-3 w-3" />
                                            <span>Add Contractor</span>
                                        </Button>
                                    )}
                                </div>
                            ) : (
                                <>
                                    {/* Desktop Compact Table */}
                                    <div className="hidden md:block overflow-x-auto">
                                        <Table className="text-xs">
                                            <TableHeader className="bg-muted/30">
                                                <TableRow className="border-border/40">
                                                    <TableHead className="font-semibold py-2 px-3">Contractor</TableHead>
                                                    <TableHead className="font-semibold py-2 px-3">Category</TableHead>
                                                    <TableHead className="text-center font-semibold bg-purple-500/5 py-2 px-3">Rate Contract (Sq.Ft)</TableHead>
                                                    <TableHead className="text-center font-semibold bg-teal-500/5 py-2 px-3">NMR Labor (Daily)</TableHead>
                                                    <TableHead className="text-right font-semibold py-2 px-3">Action</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {filteredAccounts.map((account) => (
                                                    <TableRow key={account.contractor.id} className="border-border/30 hover:bg-muted/20 transition-colors">
                                                        <TableCell className="font-medium py-2 px-3">
                                                            <div className="flex flex-col">
                                                                <span className="font-bold text-foreground text-xs sm:text-sm">{account.contractor.name}</span>
                                                                {account.contractor.contactPerson && (
                                                                    <span className="text-[10px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                )}
                                                            </div>
                                                        </TableCell>

                                                        <TableCell className="py-2 px-3">
                                                            <Badge variant="secondary" className="text-[10px] rounded-full px-2 py-0">
                                                                {account.contractor.category || 'General'}
                                                            </Badge>
                                                        </TableCell>

                                                        <TableCell className="bg-purple-500/5 text-center py-2 px-3">
                                                            <div className="flex items-center justify-center gap-2">
                                                                <span className="text-[11px] font-bold text-purple-600 dark:text-purple-400">
                                                                    ₹{account.rateAccount.totalPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                                </span>
                                                                <span className="text-muted-foreground/30">•</span>
                                                                <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                                                                    ₹{account.rateAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Paid</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>

                                                        <TableCell className="bg-teal-500/5 text-center py-2 px-3">
                                                            <div className="flex items-center justify-center gap-2">
                                                                <span className="text-[11px] font-bold text-teal-600 dark:text-teal-400">
                                                                    ₹{account.nmrAccount.totalPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                                </span>
                                                                <span className="text-muted-foreground/30">•</span>
                                                                <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                                                                    ₹{account.nmrAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Paid</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>

                                                        <TableCell className="text-right py-2 px-3">
                                                            <div className="flex items-center justify-end gap-1">
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    onClick={(e) => handleEditClick(e, account.contractor)}
                                                                    className="h-7 w-7 text-muted-foreground hover:text-foreground"
                                                                >
                                                                    <Edit3 className="h-3.5 w-3.5" />
                                                                </Button>
                                                                <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                                    <Button size="sm" variant="outline" className="h-7 text-xs rounded-lg gap-1 px-2.5">
                                                                        <span>Ledger</span>
                                                                        <ArrowRight className="h-3 w-3" />
                                                                    </Button>
                                                                </Link>
                                                            </div>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>

                                    {/* Mobile Compact High-Density Cards */}
                                    <div className="block md:hidden divide-y divide-border/30">
                                        {filteredAccounts.map((account) => (
                                            <div
                                                key={account.contractor.id}
                                                className="p-2.5 hover:bg-muted/10 transition-colors space-y-1.5"
                                            >
                                                {/* Header row: Name + Category + Edit + Arrow Link */}
                                                <div className="flex items-center justify-between gap-1.5">
                                                    <div className="min-w-0 flex-1 flex items-center gap-1.5">
                                                        <Link
                                                            href={`/financials/contractors/${account.contractor.id}`}
                                                            className="font-bold text-xs text-foreground truncate hover:text-primary transition-colors"
                                                        >
                                                            {account.contractor.name}
                                                        </Link>
                                                        <Badge variant="outline" className="text-[9px] font-medium bg-muted/40 text-muted-foreground rounded-full px-1.5 py-0 shrink-0">
                                                            {account.contractor.category || 'General'}
                                                        </Badge>
                                                    </div>

                                                    <div className="flex items-center gap-0.5 shrink-0">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            onClick={(e) => handleEditClick(e, account.contractor)}
                                                            className="h-6 w-6 text-muted-foreground"
                                                        >
                                                            <Edit3 className="h-3 w-3" />
                                                        </Button>
                                                        <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="h-6 w-6 text-primary"
                                                            >
                                                                <ChevronRight className="h-4 w-4" />
                                                            </Button>
                                                        </Link>
                                                    </div>
                                                </div>

                                                {/* Contact info subtext if present */}
                                                {account.contractor.contactPerson && (
                                                    <div className="text-[10px] text-muted-foreground truncate flex items-center gap-1">
                                                        <User className="h-2.5 w-2.5 shrink-0" />
                                                        <span className="truncate">{account.contractor.contactPerson}</span>
                                                        {account.contractor.phone && (
                                                            <>
                                                                <span>•</span>
                                                                <Phone className="h-2.5 w-2.5 shrink-0" />
                                                                <span>{account.contractor.phone}</span>
                                                            </>
                                                        )}
                                                    </div>
                                                )}

                                                {/* Compact Dual-Tone Pill Row: Rate vs NMR */}
                                                <div className="grid grid-cols-2 gap-1.5 text-[10px]">
                                                    <div className="bg-purple-500/10 px-2 py-1 rounded-md flex items-center justify-between">
                                                        <span className="text-purple-700 dark:text-purple-300 font-semibold">Rate</span>
                                                        <div className="text-right">
                                                            <span className="font-bold text-purple-600 dark:text-purple-300">
                                                                ₹{account.rateAccount.totalPending.toLocaleString('en-IN')}
                                                            </span>
                                                            <span className="text-[8px] text-muted-foreground ml-1">Due</span>
                                                        </div>
                                                    </div>

                                                    <div className="bg-teal-500/10 px-2 py-1 rounded-md flex items-center justify-between">
                                                        <span className="text-teal-700 dark:text-teal-300 font-semibold">NMR</span>
                                                        <div className="text-right">
                                                            <span className="font-bold text-teal-600 dark:text-teal-300">
                                                                ₹{account.nmrAccount.totalPending.toLocaleString('en-IN')}
                                                            </span>
                                                            <span className="text-[8px] text-muted-foreground ml-1">Due</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )
                        ) : (
                            /* Building View */
                            contractorsForSelectedBuilding.length === 0 ? (
                                <div className="text-center py-8 px-4">
                                    <Building2 className="h-7 w-7 text-muted-foreground/40 mx-auto mb-1" />
                                    <h3 className="text-xs sm:text-sm font-bold text-foreground">No Contractors Found on Site</h3>
                                    <p className="text-[11px] text-muted-foreground max-w-sm mx-auto mt-0.5">
                                        No transactions logged for this site yet.
                                    </p>
                                </div>
                            ) : (
                                <>
                                    {/* Desktop Table */}
                                    <div className="hidden md:block overflow-x-auto">
                                        <Table className="text-xs">
                                            <TableHeader className="bg-muted/30">
                                                <TableRow className="border-border/40">
                                                    <TableHead className="font-semibold py-2 px-3">Contractor</TableHead>
                                                    <TableHead className="font-semibold py-2 px-3">Category</TableHead>
                                                    <TableHead className="text-center font-semibold bg-purple-500/5 py-2 px-3">Rate Contract (Sq.Ft)</TableHead>
                                                    <TableHead className="text-center font-semibold bg-teal-500/5 py-2 px-3">NMR Labor (Daily)</TableHead>
                                                    <TableHead className="text-right font-semibold py-2 px-3">Action</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {contractorsForSelectedBuilding.map((account) => (
                                                    <TableRow key={account.contractor.id} className="border-border/30 hover:bg-muted/20 transition-colors">
                                                        <TableCell className="font-medium py-2 px-3">
                                                            <div className="flex flex-col">
                                                                <span className="font-bold text-foreground text-xs sm:text-sm">{account.contractor.name}</span>
                                                                {account.contractor.contactPerson && (
                                                                    <span className="text-[10px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                )}
                                                            </div>
                                                        </TableCell>
                                                        <TableCell className="py-2 px-3">
                                                            <Badge variant="secondary" className="text-[10px] rounded-full px-2 py-0">
                                                                {account.contractor.category || 'General'}
                                                            </Badge>
                                                        </TableCell>
                                                        <TableCell className="bg-purple-500/5 text-center py-2 px-3">
                                                            <div className="flex items-center justify-center gap-2">
                                                                <span className="text-[11px] font-bold text-purple-600 dark:text-purple-400">
                                                                    ₹{account.buildingStats.ratePending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                                </span>
                                                                <span className="text-muted-foreground/30">•</span>
                                                                <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                                                                    ₹{account.buildingStats.ratePaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Paid</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell className="bg-teal-500/5 text-center py-2 px-3">
                                                            <div className="flex items-center justify-center gap-2">
                                                                <span className="text-[11px] font-bold text-teal-600 dark:text-teal-400">
                                                                    ₹{account.buildingStats.nmrPending.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Due</span>
                                                                </span>
                                                                <span className="text-muted-foreground/30">•</span>
                                                                <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                                                                    ₹{account.buildingStats.nmrPaid.toLocaleString('en-IN')} <span className="text-[9px] font-normal text-muted-foreground">Paid</span>
                                                                </span>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell className="text-right py-2 px-3">
                                                            <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                                <Button size="sm" variant="outline" className="h-7 text-xs rounded-lg gap-1 px-2.5">
                                                                    <span>Ledger</span>
                                                                    <ArrowRight className="h-3 w-3" />
                                                                </Button>
                                                            </Link>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>

                                    {/* Mobile Cards for Building View */}
                                    <div className="block md:hidden divide-y divide-border/30">
                                        {contractorsForSelectedBuilding.map((account) => (
                                            <div
                                                key={account.contractor.id}
                                                className="p-2.5 hover:bg-muted/10 transition-colors space-y-1.5"
                                            >
                                                <div className="flex items-center justify-between gap-1.5">
                                                    <div className="min-w-0 flex-1 flex items-center gap-1.5">
                                                        <Link
                                                            href={`/financials/contractors/${account.contractor.id}`}
                                                            className="font-bold text-xs text-foreground truncate hover:text-primary transition-colors"
                                                        >
                                                            {account.contractor.name}
                                                        </Link>
                                                        <Badge variant="outline" className="text-[9px] font-medium bg-muted/40 text-muted-foreground rounded-full px-1.5 py-0 shrink-0">
                                                            {account.contractor.category || 'General'}
                                                        </Badge>
                                                    </div>
                                                    <Link href={`/financials/contractors/${account.contractor.id}`}>
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="h-6 w-6 text-primary shrink-0"
                                                        >
                                                            <ChevronRight className="h-4 w-4" />
                                                        </Button>
                                                    </Link>
                                                </div>

                                                <div className="grid grid-cols-2 gap-1.5 text-[10px]">
                                                    <div className="bg-purple-500/10 px-2 py-1 rounded-md flex items-center justify-between">
                                                        <span className="text-purple-700 dark:text-purple-300 font-semibold">Rate</span>
                                                        <div className="text-right">
                                                            <span className="font-bold text-purple-600 dark:text-purple-300">
                                                                ₹{account.buildingStats.ratePending.toLocaleString('en-IN')}
                                                            </span>
                                                            <span className="text-[8px] text-muted-foreground ml-1">Due</span>
                                                        </div>
                                                    </div>

                                                    <div className="bg-teal-500/10 px-2 py-1 rounded-md flex items-center justify-between">
                                                        <span className="text-teal-700 dark:text-teal-300 font-semibold">NMR</span>
                                                        <div className="text-right">
                                                            <span className="font-bold text-teal-600 dark:text-teal-300">
                                                                ₹{account.buildingStats.nmrPending.toLocaleString('en-IN')}
                                                            </span>
                                                            <span className="text-[8px] text-muted-foreground ml-1">Due</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )
                        )}
                    </CardContent>
                </Card>

                {/* Create Contractor Modal */}
                <CreateContractorDialog
                    open={isCreateOpen}
                    onOpenChange={setIsCreateOpen}
                    onSuccess={() => loadAccounts()}
                />

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
