'use client';

import { useEffect, useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getContractorAccounts } from '@/app/actions/contractors';
import { getProjects } from '@/app/actions/financials';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { EditContractorDialog } from '@/components/contractors/edit-contractor-dialog';
import {
    Loader2,
    ArrowLeft,
    Phone,
    Mail,
    User,
    Building2,
    ChevronDown,
    ChevronUp,
    Edit,
    FileText,
    Wallet
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

export default function ContractorDetailPage() {
    const params = useParams();
    const router = useRouter();
    const { toast } = useToast();
    const contractorId = typeof params?.contractorId === 'string' ? params.contractorId : '';

    const [account, setAccount] = useState<ContractorAccount | null>(null);
    const [projects, setProjects] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [ledgerProjectFilter, setLedgerProjectFilter] = useState<string>('all');
    const [expandedTxId, setExpandedTxId] = useState<string | null>(null);

    // Edit modal state
    const [isEditOpen, setIsEditOpen] = useState(false);

    const loadData = async () => {
        setIsLoading(true);
        try {
            const [accRes, projData] = await Promise.all([
                getContractorAccounts(),
                getProjects()
            ]);

            setProjects(projData || []);

            if (accRes.success && accRes.data) {
                const matched = (accRes.data as ContractorAccount[]).find(a => a.contractor.id === contractorId);
                if (matched) {
                    setAccount(matched);
                } else {
                    toast({
                        title: 'Contractor Not Found',
                        description: 'Could not locate the requested contractor account.',
                        variant: 'destructive'
                    });
                }
            } else {
                toast({
                    title: 'Error',
                    description: accRes.error || 'Failed to load contractor ledger.',
                    variant: 'destructive'
                });
            }
        } catch (err) {
            console.error('Failed to load contractor detail:', err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (contractorId) {
            loadData();
        }
    }, [contractorId]);

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'paid':
                return <Badge className="bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[10px] font-semibold rounded-full px-2 py-0.5">Paid</Badge>;
            case 'held':
                return <Badge className="bg-rose-500/20 text-rose-600 dark:text-rose-400 border-rose-500/30 text-[10px] font-semibold rounded-full px-2 py-0.5">Held</Badge>;
            default:
                return <Badge className="bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30 text-[10px] font-semibold rounded-full px-2 py-0.5">Pending</Badge>;
        }
    };

    const getPayoutClassBadge = (payoutClass: string) => {
        return payoutClass === 'rate' ? (
            <Badge className="bg-purple-500/15 text-purple-600 dark:text-purple-300 border-purple-500/30 font-semibold text-[10px] rounded-full px-2 py-0.5">
                RATE CONTRACT
            </Badge>
        ) : (
            <Badge className="bg-teal-500/15 text-teal-600 dark:text-teal-300 border-teal-500/30 font-semibold text-[10px] rounded-full px-2 py-0.5">
                NMR LABOR
            </Badge>
        );
    };

    const filteredTransactions = useMemo(() => {
        if (!account) return [];
        if (ledgerProjectFilter === 'all') return account.allTransactions;
        return account.allTransactions.filter(tx => tx.project_id === ledgerProjectFilter);
    }, [account, ledgerProjectFilter]);

    const filteredRateItems = useMemo(() => {
        if (!account) return [];
        if (ledgerProjectFilter === 'all') return account.rateAccount.items;
        return account.rateAccount.items.filter(tx => tx.project_id === ledgerProjectFilter);
    }, [account, ledgerProjectFilter]);

    const filteredNmrItems = useMemo(() => {
        if (!account) return [];
        if (ledgerProjectFilter === 'all') return account.nmrAccount.items;
        return account.nmrAccount.items.filter(tx => tx.project_id === ledgerProjectFilter);
    }, [account, ledgerProjectFilter]);

    const totalPaidSum = filteredTransactions.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
    const totalPendingSum = filteredTransactions.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

    // Helper to safely parse reference_details JSON payload into human readable text
    const parseReferenceDetails = (raw: string | null | undefined): { title: string; subtitle?: string | null } => {
        if (!raw) return { title: 'Payout entry' };
        if (typeof raw === 'string' && raw.trim().startsWith('{')) {
            try {
                const parsed = JSON.parse(raw);
                if (parsed.type === 'contractor_wages') {
                    const titleParts = [parsed.category, parsed.description].filter(Boolean);
                    const titleText = titleParts.length > 0 ? titleParts.join(' • ') : 'Daily Labor Wages';
                    let breakdownParts: string[] = [];
                    if (Array.isArray(parsed.breakdown) && parsed.breakdown.length > 0) {
                        breakdownParts = parsed.breakdown.map((b: any) => `${b.category}: ${b.days}d @ ₹${b.rate}`);
                    }
                    const subtitleText = breakdownParts.length > 0 ? breakdownParts.join(' | ') : null;
                    return {
                        title: titleText,
                        subtitle: subtitleText
                    };
                }
                if (parsed.description || parsed.title) {
                    return { title: parsed.description || parsed.title, subtitle: parsed.category || null };
                }
            } catch (e) {
                // fallback to raw text if parsing fails
            }
        }
        return { title: raw };
    };

    if (isLoading) {
        return (
            <div className="flex h-[70vh] items-center justify-center">
                <div className="flex flex-col items-center gap-3">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    <p className="text-xs text-muted-foreground">Loading contractor ledger statement...</p>
                </div>
            </div>
        );
    }

    if (!account) {
        return (
            <div className="flex flex-col items-center justify-center h-[60vh] p-6 text-center">
                <Building2 className="h-12 w-12 text-muted-foreground/40 mb-3" />
                <h2 className="text-lg font-bold font-headline">Contractor Not Found</h2>
                <p className="text-xs text-muted-foreground mt-1 mb-4 max-w-sm">
                    The requested contractor ledger statement could not be loaded.
                </p>
                <Link href="/financials/contractors">
                    <Button size="sm" variant="outline" className="rounded-xl">
                        <ArrowLeft className="mr-1.5 h-4 w-4" /> Back to Accounts
                    </Button>
                </Link>
            </div>
        );
    }

    return (
        <main className="flex-1 p-3.5 sm:p-4 md:p-6 overflow-y-auto bg-transparent">
            <div className="max-w-6xl mx-auto space-y-4 sm:space-y-6">

                {/* Header & Back Navigation */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border/40 pb-4">
                    <div className="flex items-center gap-3 min-w-0">
                        <Link href="/financials/contractors">
                            <Button variant="ghost" size="icon" className="h-8 w-8 sm:h-9 sm:w-9 shrink-0 rounded-xl">
                                <ArrowLeft className="h-4 w-4 sm:h-5 sm:w-5" />
                            </Button>
                        </Link>
                        <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <h1 className="text-lg sm:text-2xl font-bold tracking-tight font-headline text-foreground truncate">
                                    {account.contractor.name}
                                </h1>
                                <Badge variant="outline" className="text-[10px] sm:text-xs bg-primary/10 border-primary/20 text-primary font-semibold rounded-full px-2 py-0.5 shrink-0">
                                    {account.contractor.category || 'General Contractor'}
                                </Badge>
                            </div>
                            <p className="text-xs text-muted-foreground mt-0.5 truncate">
                                Contractor ledger statement & payout records
                            </p>
                        </div>
                    </div>

                    <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setIsEditOpen(true)}
                        className="h-8 sm:h-9 text-xs sm:text-sm rounded-xl gap-1.5 shrink-0 w-full sm:w-auto"
                    >
                        <Edit className="h-3.5 w-3.5 text-primary" />
                        Edit Details
                    </Button>
                </div>

                {/* Profile Contact Cards */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-4 p-3 sm:p-4 glass-card rounded-2xl border border-white/10 dark:border-white/5 text-xs">
                    <div className="flex flex-col gap-0.5">
                        <span className="text-muted-foreground font-semibold uppercase text-[9px] tracking-wider">Contact Person</span>
                        <span className="font-bold text-foreground truncate flex items-center gap-1">
                            <User className="h-3 w-3 text-primary/80 shrink-0" />
                            {account.contractor.contactPerson || 'N/A'}
                        </span>
                    </div>

                    <div className="flex flex-col gap-0.5">
                        <span className="text-muted-foreground font-semibold uppercase text-[9px] tracking-wider">Phone</span>
                        <span className="font-bold text-foreground truncate flex items-center gap-1">
                            <Phone className="h-3 w-3 text-primary/80 shrink-0" />
                            {account.contractor.phone || 'N/A'}
                        </span>
                    </div>

                    <div className="flex flex-col gap-0.5">
                        <span className="text-muted-foreground font-semibold uppercase text-[9px] tracking-wider">Email</span>
                        <span className="font-bold text-foreground truncate flex items-center gap-1">
                            <Mail className="h-3 w-3 text-primary/80 shrink-0" />
                            {account.contractor.email || 'N/A'}
                        </span>
                    </div>

                    <div className="flex flex-col gap-0.5">
                        <span className="text-muted-foreground font-semibold uppercase text-[9px] tracking-wider">Trade</span>
                        <span className="font-bold text-foreground truncate">
                            {account.contractor.category || 'General'}
                        </span>
                    </div>
                </div>

                {/* Building Filter Dropdown */}
                {projects.length > 0 && (
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3 glass-card rounded-2xl border border-white/10">
                        <span className="text-xs text-muted-foreground font-medium">Filter statement entries by building / site:</span>
                        <Select
                            value={ledgerProjectFilter}
                            onValueChange={(val) => {
                                setLedgerProjectFilter(val);
                                setExpandedTxId(null);
                            }}
                        >
                            <SelectTrigger className="w-full sm:w-[220px] h-9 text-xs rounded-xl bg-background/50">
                                <SelectValue placeholder="All Buildings" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="all">All Buildings</SelectItem>
                                {projects.map(proj => (
                                    <SelectItem key={proj.id} value={proj.id}>{proj.name}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                )}

                {/* Sub-account Tabs */}
                <Tabs defaultValue="combined" className="w-full space-y-4">
                    <TabsList className="grid grid-cols-3 w-full sm:w-auto p-1 rounded-2xl bg-muted/30 border border-border/40">
                        <TabsTrigger value="combined" className="text-xs sm:text-sm rounded-xl py-1.5 font-semibold">Combined</TabsTrigger>
                        <TabsTrigger value="rate" className="text-xs sm:text-sm rounded-xl py-1.5 font-semibold">Rate (Sq.Ft)</TabsTrigger>
                        <TabsTrigger value="nmr" className="text-xs sm:text-sm rounded-xl py-1.5 font-semibold">NMR (Labor)</TabsTrigger>
                    </TabsList>

                    {/* Combined Tab */}
                    <TabsContent value="combined" className="space-y-4">
                        <div className="flex items-center justify-between p-3.5 sm:p-4 glass-card rounded-2xl border border-white/10">
                            <div className="text-xs sm:text-sm">
                                <span className="text-muted-foreground">Total Cleared: </span>
                                <span className="font-bold text-emerald-600 dark:text-emerald-400">₹{totalPaidSum.toLocaleString('en-IN')}</span>
                            </div>
                            <div className="text-xs sm:text-sm">
                                <span className="text-muted-foreground">Outstanding: </span>
                                <span className="font-bold text-amber-600 dark:text-amber-400">₹{totalPendingSum.toLocaleString('en-IN')}</span>
                            </div>
                        </div>

                        {/* Desktop Table View */}
                        <div className="hidden md:block glass-card rounded-2xl border border-white/10 overflow-hidden">
                            <Table>
                                <TableHeader className="bg-muted/30">
                                    <TableRow className="border-border/40">
                                        <TableHead className="text-xs font-semibold">Date</TableHead>
                                        <TableHead className="text-xs font-semibold">Classification</TableHead>
                                        <TableHead className="text-xs font-semibold">Details</TableHead>
                                        <TableHead className="text-xs font-semibold text-right">Amount</TableHead>
                                        <TableHead className="text-xs font-semibold text-right">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredTransactions.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={5} className="text-center py-12 text-muted-foreground text-xs">
                                                No transactions registered for this selection.
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredTransactions.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow
                                                    key={tx.id}
                                                    className="border-border/30 hover:bg-muted/30 text-xs cursor-pointer select-none"
                                                    onClick={() => setExpandedTxId(expandedTxId === tx.id ? null : tx.id)}
                                                >
                                                    <TableCell className="font-medium flex items-center gap-1.5 py-3">
                                                        {expandedTxId === tx.id ? (
                                                            <ChevronUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                        ) : (
                                                            <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                        )}
                                                        <span>
                                                            {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                        </span>
                                                    </TableCell>
                                                    <TableCell>{getPayoutClassBadge(tx.payout_class)}</TableCell>
                                                    <TableCell className="max-w-[280px]">
                                                        <div className="font-medium text-foreground">{details.title}</div>
                                                        {details.subtitle && <div className="text-[11px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </TableCell>
                                                    <TableCell className="text-right">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden space-y-2.5">
                            {filteredTransactions.length === 0 ? (
                                <div className="text-center py-10 p-4 glass-card rounded-2xl text-xs text-muted-foreground">
                                    No transactions registered for this selection.
                                </div>
                            ) : (
                                filteredTransactions.map((tx) => {
                                    const isExpanded = expandedTxId === tx.id;
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            onClick={() => setExpandedTxId(isExpanded ? null : tx.id)}
                                            className="glass-card rounded-2xl p-3.5 border border-white/10 space-y-2.5 cursor-pointer active:scale-[0.99] transition-transform"
                                        >
                                            <div className="flex items-center justify-between gap-2">
                                                <div className="flex items-center gap-1.5">
                                                    {getPayoutClassBadge(tx.payout_class)}
                                                    <span className="text-[11px] text-muted-foreground">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </span>
                                                </div>
                                                {getStatusBadge(tx.status)}
                                            </div>

                                            <div className="flex items-baseline justify-between gap-2 pt-1 border-t border-border/30">
                                                <div className="min-w-0 flex-1">
                                                    <span className="text-xs font-semibold text-foreground block truncate">
                                                        {details.title}
                                                    </span>
                                                    {details.subtitle && (
                                                        <span className="text-[10px] text-muted-foreground block truncate">
                                                            {details.subtitle}
                                                        </span>
                                                    )}
                                                </div>
                                                <span className="font-mono font-bold text-sm text-foreground shrink-0">
                                                    ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                </span>
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>

                    {/* Rate Account Tab */}
                    <TabsContent value="rate" className="space-y-4">
                        {/* Desktop Table */}
                        <div className="hidden md:block glass-card rounded-2xl border border-white/10 overflow-hidden">
                            <Table className="text-xs sm:text-sm">
                                <TableHeader className="bg-muted/30">
                                    <TableRow>
                                        <TableHead className="font-semibold">Date</TableHead>
                                        <TableHead className="font-semibold">Details</TableHead>
                                        <TableHead className="font-semibold text-right">Amount</TableHead>
                                        <TableHead className="font-semibold text-right">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredRateItems.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">No rate contract transactions found.</TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredRateItems.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow key={tx.id} className="border-border/30 hover:bg-muted/30">
                                                    <TableCell className="font-medium">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </TableCell>
                                                    <TableCell className="max-w-[280px]">
                                                        <div className="font-medium text-foreground">{details.title}</div>
                                                        {details.subtitle && <div className="text-[11px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground">₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}</TableCell>
                                                    <TableCell className="text-right">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden space-y-2.5">
                            {filteredRateItems.length === 0 ? (
                                <div className="text-center py-10 p-4 glass-card rounded-2xl text-xs text-muted-foreground">
                                    No rate contract transactions found.
                                </div>
                            ) : (
                                filteredRateItems.map((tx) => {
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            className="glass-card rounded-2xl p-3.5 border border-white/10 space-y-2.5"
                                        >
                                            <div className="flex items-center justify-between gap-2">
                                                <span className="text-[11px] text-muted-foreground">
                                                    {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                </span>
                                                {getStatusBadge(tx.status)}
                                            </div>

                                            <div className="flex items-baseline justify-between gap-2 pt-1 border-t border-border/30">
                                                <div className="min-w-0 flex-1">
                                                    <span className="text-xs font-semibold text-foreground block truncate">
                                                        {details.title}
                                                    </span>
                                                    {details.subtitle && (
                                                        <span className="text-[10px] text-muted-foreground block truncate">
                                                            {details.subtitle}
                                                        </span>
                                                    )}
                                                </div>
                                                <span className="font-mono font-bold text-sm text-foreground shrink-0">
                                                    ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                </span>
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>

                    {/* NMR Account Tab */}
                    <TabsContent value="nmr" className="space-y-4">
                        {/* Desktop Table */}
                        <div className="hidden md:block glass-card rounded-2xl border border-white/10 overflow-hidden">
                            <Table className="text-xs sm:text-sm">
                                <TableHeader className="bg-muted/30">
                                    <TableRow>
                                        <TableHead className="font-semibold">Date</TableHead>
                                        <TableHead className="font-semibold">Details</TableHead>
                                        <TableHead className="font-semibold text-right">Amount</TableHead>
                                        <TableHead className="font-semibold text-right">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredNmrItems.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">No NMR labor transactions found.</TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredNmrItems.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow key={tx.id} className="border-border/30 hover:bg-muted/30">
                                                    <TableCell className="font-medium">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </TableCell>
                                                    <TableCell className="max-w-[280px]">
                                                        <div className="font-medium text-foreground">{details.title}</div>
                                                        {details.subtitle && <div className="text-[11px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground">₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}</TableCell>
                                                    <TableCell className="text-right">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden space-y-2.5">
                            {filteredNmrItems.length === 0 ? (
                                <div className="text-center py-10 p-4 glass-card rounded-2xl text-xs text-muted-foreground">
                                    No NMR labor transactions found.
                                </div>
                            ) : (
                                filteredNmrItems.map((tx) => {
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            className="glass-card rounded-2xl p-3.5 border border-white/10 space-y-2.5"
                                        >
                                            <div className="flex items-center justify-between gap-2">
                                                <span className="text-[11px] text-muted-foreground">
                                                    {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                </span>
                                                {getStatusBadge(tx.status)}
                                            </div>

                                            <div className="flex items-baseline justify-between gap-2 pt-1 border-t border-border/30">
                                                <div className="min-w-0 flex-1">
                                                    <span className="text-xs font-semibold text-foreground block truncate">
                                                        {details.title}
                                                    </span>
                                                    {details.subtitle && (
                                                        <span className="text-[10px] text-muted-foreground block truncate">
                                                            {details.subtitle}
                                                        </span>
                                                    )}
                                                </div>
                                                <span className="font-mono font-bold text-sm text-foreground shrink-0">
                                                    ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                </span>
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>
                </Tabs>

                {/* Edit Contractor Modal */}
                <EditContractorDialog
                    open={isEditOpen}
                    onOpenChange={setIsEditOpen}
                    contractor={account.contractor}
                    onSuccess={() => loadData()}
                />

            </div>
        </main>
    );
}

