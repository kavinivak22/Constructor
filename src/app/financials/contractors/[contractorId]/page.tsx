'use client';

import { useEffect, useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getContractorAccounts } from '@/app/actions/contractors';
import { getProjects } from '@/app/actions/financials';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { useToast } from '@/hooks/use-toast';
import { EditContractorDialog } from '@/components/contractors/edit-contractor-dialog';
import {
    Loader2,
    ArrowLeft,
    Phone,
    Mail,
    User,
    Building2,
    Edit,
    FileText,
    Calendar,
    Coins,
    Hammer,
    ShieldCheck
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
    const [selectedTxModal, setSelectedTxModal] = useState<any | null>(null);
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
                return <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[9px] font-semibold rounded-full px-1.5 py-0">Paid</Badge>;
            case 'held':
                return <Badge className="bg-rose-500/15 text-rose-600 dark:text-rose-400 border-rose-500/30 text-[9px] font-semibold rounded-full px-1.5 py-0">Held</Badge>;
            default:
                return <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 text-[9px] font-semibold rounded-full px-1.5 py-0">Pending</Badge>;
        }
    };

    const getPayoutClassBadge = (payoutClass: string) => {
        return payoutClass === 'rate' ? (
            <Badge className="bg-purple-500/15 text-purple-700 dark:text-purple-300 border-purple-500/30 font-semibold text-[9px] rounded-full px-1.5 py-0">
                RATE
            </Badge>
        ) : (
            <Badge className="bg-teal-500/15 text-teal-700 dark:text-teal-300 border-teal-500/30 font-semibold text-[9px] rounded-full px-1.5 py-0">
                NMR
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

    // Combined Totals
    const totalPaidSum = useMemo(() => {
        return filteredTransactions.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredTransactions]);

    const totalPendingSum = useMemo(() => {
        return filteredTransactions.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredTransactions]);

    // Rate Totals
    const ratePaidSum = useMemo(() => {
        return filteredRateItems.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredRateItems]);

    const ratePendingSum = useMemo(() => {
        return filteredRateItems.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredRateItems]);

    // NMR Totals
    const nmrPaidSum = useMemo(() => {
        return filteredNmrItems.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredNmrItems]);

    const nmrPendingSum = useMemo(() => {
        return filteredNmrItems.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid || 0), 0);
    }, [filteredNmrItems]);

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
            } catch (e) {}
        }
        return { title: raw };
    };

    if (isLoading) {
        return (
            <div className="flex h-[60vh] items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                    <Loader2 className="h-6 w-6 animate-spin text-primary" />
                    <p className="text-xs text-muted-foreground">Loading ledger statement...</p>
                </div>
            </div>
        );
    }

    if (!account) {
        return (
            <div className="flex flex-col items-center justify-center h-[50vh] p-4 text-center">
                <Building2 className="h-8 w-8 text-muted-foreground/40 mb-2" />
                <h2 className="text-sm font-bold font-headline">Contractor Not Found</h2>
                <p className="text-xs text-muted-foreground mt-0.5 mb-3 max-w-sm">
                    The requested contractor ledger statement could not be loaded.
                </p>
                <Link href="/financials/contractors">
                    <Button size="sm" variant="outline" className="rounded-lg text-xs h-7">
                        <ArrowLeft className="mr-1 h-3.5 w-3.5" /> Back to Accounts
                    </Button>
                </Link>
            </div>
        );
    }

    return (
        <main className="flex-1 p-2.5 sm:p-4 md:p-5 overflow-y-auto bg-transparent">
            <div className="max-w-6xl mx-auto space-y-2.5 sm:space-y-3.5">

                {/* Compact Header */}
                <div className="flex items-center justify-between gap-2 pb-1 border-b border-border/40">
                    <div className="flex items-center gap-2 min-w-0">
                        <Link href="/financials/contractors">
                            <Button variant="ghost" size="icon" className="h-7 w-7 sm:h-8 sm:w-8 shrink-0 rounded-lg">
                                <ArrowLeft className="h-4 w-4" />
                            </Button>
                        </Link>
                        <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-1.5 flex-wrap">
                                <h1 className="text-sm sm:text-lg font-bold tracking-tight font-headline text-foreground truncate">
                                    {account.contractor.name}
                                </h1>
                                <Badge variant="outline" className="text-[9px] sm:text-[10px] bg-primary/10 border-primary/20 text-primary font-semibold rounded-full px-1.5 py-0 shrink-0">
                                    {account.contractor.category || 'General'}
                                </Badge>
                            </div>
                        </div>
                    </div>

                    <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setIsEditOpen(true)}
                        className="h-7 sm:h-8 text-xs rounded-lg gap-1 shrink-0 px-2.5"
                    >
                        <Edit className="h-3 w-3 text-primary" />
                        <span>Edit</span>
                    </Button>
                </div>

                {/* Compact Inline Contact Bar */}
                {(account.contractor.contactPerson || account.contractor.phone || account.contractor.email) && (
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 px-2.5 py-1.5 glass-card rounded-lg border border-white/10 text-[11px] text-muted-foreground">
                        {account.contractor.contactPerson && (
                            <span className="flex items-center gap-1 font-medium text-foreground">
                                <User className="h-3 w-3 text-primary/70 shrink-0" />
                                {account.contractor.contactPerson}
                            </span>
                        )}
                        {account.contractor.phone && (
                            <a
                                href={`tel:${account.contractor.phone}`}
                                className="flex items-center gap-1 hover:text-primary transition-colors"
                            >
                                <Phone className="h-3 w-3 text-primary/70 shrink-0" />
                                {account.contractor.phone}
                            </a>
                        )}
                        {account.contractor.email && (
                            <a
                                href={`mailto:${account.contractor.email}`}
                                className="flex items-center gap-1 hover:text-primary transition-colors truncate max-w-[200px]"
                            >
                                <Mail className="h-3 w-3 text-primary/70 shrink-0" />
                                <span className="truncate">{account.contractor.email}</span>
                            </a>
                        )}
                    </div>
                )}

                {/* Sub-account Tabs & Site Filter in one cohesive strip */}
                <Tabs defaultValue="combined" className="w-full space-y-2.5">
                    <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 p-1.5 glass-card rounded-xl border border-white/10">
                        {/* Compact Tab Switcher */}
                        <TabsList className="grid grid-cols-3 w-full sm:w-auto p-0.5 rounded-lg bg-muted/40 border border-border/30 h-7 sm:h-8">
                            <TabsTrigger value="combined" className="text-[11px] sm:text-xs rounded-md px-2.5 py-0.5 font-semibold">
                                Combined
                            </TabsTrigger>
                            <TabsTrigger value="rate" className="text-[11px] sm:text-xs rounded-md px-2.5 py-0.5 font-semibold">
                                Rate
                            </TabsTrigger>
                            <TabsTrigger value="nmr" className="text-[11px] sm:text-xs rounded-md px-2.5 py-0.5 font-semibold">
                                NMR
                            </TabsTrigger>
                        </TabsList>

                        {/* Building Selector */}
                        {projects.length > 0 && (
                            <div className="flex items-center gap-1.5 w-full sm:w-auto shrink-0">
                                <span className="text-[11px] text-muted-foreground font-medium hidden sm:inline">Site:</span>
                                <Select
                                    value={ledgerProjectFilter}
                                    onValueChange={(val) => setLedgerProjectFilter(val)}
                                >
                                    <SelectTrigger className="w-full sm:w-[180px] h-7 sm:h-8 text-xs rounded-lg bg-background/50">
                                        <SelectValue placeholder="All Sites" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="all" className="text-xs">All Sites</SelectItem>
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

                    {/* Combined Tab */}
                    <TabsContent value="combined" className="space-y-2 mt-0">
                        {/* Compact Metric Strip */}
                        <div className="flex items-center justify-between px-3 py-1.5 glass-card rounded-lg border border-white/10 text-xs">
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">Cleared:</span>
                                <span className="font-bold text-emerald-600 dark:text-emerald-400">₹{totalPaidSum.toLocaleString('en-IN')}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">Outstanding:</span>
                                <span className="font-bold text-amber-600 dark:text-amber-400">₹{totalPendingSum.toLocaleString('en-IN')}</span>
                            </div>
                        </div>

                        {/* Desktop Table View */}
                        <div className="hidden md:block glass-card rounded-xl border border-white/10 overflow-hidden">
                            <Table className="text-xs">
                                <TableHeader className="bg-muted/30">
                                    <TableRow className="border-border/40">
                                        <TableHead className="font-semibold py-2 px-3">Date</TableHead>
                                        <TableHead className="font-semibold py-2 px-3">Type</TableHead>
                                        <TableHead className="font-semibold py-2 px-3">Details</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Amount</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredTransactions.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={5} className="text-center py-8 text-muted-foreground text-xs">
                                                No transactions registered for this selection.
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredTransactions.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow
                                                    key={tx.id}
                                                    className="border-border/30 hover:bg-muted/20 text-xs cursor-pointer select-none"
                                                    onClick={() => setSelectedTxModal(tx)}
                                                >
                                                    <TableCell className="font-medium py-2 px-3">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </TableCell>
                                                    <TableCell className="py-2 px-3">{getPayoutClassBadge(tx.payout_class)}</TableCell>
                                                    <TableCell className="max-w-[280px] py-2 px-3">
                                                        <div className="font-medium text-foreground truncate">{details.title}</div>
                                                        {details.subtitle && <div className="text-[10px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground py-2 px-3">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </TableCell>
                                                    <TableCell className="text-right py-2 px-3">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden divide-y divide-border/30 glass-card rounded-xl border border-white/10 overflow-hidden">
                            {filteredTransactions.length === 0 ? (
                                <div className="text-center py-6 p-4 text-xs text-muted-foreground">
                                    No transactions registered for this selection.
                                </div>
                            ) : (
                                filteredTransactions.map((tx) => {
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            onClick={() => setSelectedTxModal(tx)}
                                            className="p-2.5 space-y-1.5 cursor-pointer active:bg-muted/20 transition-colors"
                                        >
                                            <div className="flex items-center justify-between gap-1.5">
                                                <div className="flex items-center gap-1.5">
                                                    {getPayoutClassBadge(tx.payout_class)}
                                                    <span className="text-[10px] text-muted-foreground">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-1.5">
                                                    <span className="font-mono font-bold text-xs text-foreground">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </span>
                                                    {getStatusBadge(tx.status)}
                                                </div>
                                            </div>

                                            <div className="min-w-0">
                                                <span className="text-[11px] font-semibold text-foreground block truncate">
                                                    {details.title}
                                                </span>
                                                {details.subtitle && (
                                                    <span className="text-[9px] text-muted-foreground block truncate">
                                                        {details.subtitle}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>

                    {/* Rate Account Tab */}
                    <TabsContent value="rate" className="space-y-2 mt-0">
                        <div className="flex items-center justify-between px-3 py-1.5 glass-card rounded-lg border border-white/10 text-xs">
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">Rate Cleared:</span>
                                <span className="font-bold text-emerald-600 dark:text-emerald-400">₹{ratePaidSum.toLocaleString('en-IN')}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">Rate Due:</span>
                                <span className="font-bold text-purple-600 dark:text-purple-400">₹{ratePendingSum.toLocaleString('en-IN')}</span>
                            </div>
                        </div>

                        {/* Desktop Table */}
                        <div className="hidden md:block glass-card rounded-xl border border-white/10 overflow-hidden">
                            <Table className="text-xs">
                                <TableHeader className="bg-muted/30">
                                    <TableRow className="border-border/40">
                                        <TableHead className="font-semibold py-2 px-3">Date</TableHead>
                                        <TableHead className="font-semibold py-2 px-3">Details</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Amount</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredRateItems.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                                                No rate contract transactions found.
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredRateItems.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow
                                                    key={tx.id}
                                                    className="border-border/30 hover:bg-muted/20 cursor-pointer select-none text-xs"
                                                    onClick={() => setSelectedTxModal(tx)}
                                                >
                                                    <TableCell className="font-medium py-2 px-3">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </TableCell>
                                                    <TableCell className="max-w-[280px] py-2 px-3">
                                                        <div className="font-medium text-foreground truncate">{details.title}</div>
                                                        {details.subtitle && <div className="text-[10px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground py-2 px-3">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </TableCell>
                                                    <TableCell className="text-right py-2 px-3">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden divide-y divide-border/30 glass-card rounded-xl border border-white/10 overflow-hidden">
                            {filteredRateItems.length === 0 ? (
                                <div className="text-center py-6 p-4 text-xs text-muted-foreground">
                                    No rate contract transactions found.
                                </div>
                            ) : (
                                filteredRateItems.map((tx) => {
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            onClick={() => setSelectedTxModal(tx)}
                                            className="p-2.5 space-y-1.5 cursor-pointer active:bg-muted/20 transition-colors"
                                        >
                                            <div className="flex items-center justify-between gap-1.5">
                                                <span className="text-[10px] text-muted-foreground">
                                                    {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                </span>
                                                <div className="flex items-center gap-1.5">
                                                    <span className="font-mono font-bold text-xs text-foreground">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </span>
                                                    {getStatusBadge(tx.status)}
                                                </div>
                                            </div>

                                            <div className="min-w-0">
                                                <span className="text-[11px] font-semibold text-foreground block truncate">
                                                    {details.title}
                                                </span>
                                                {details.subtitle && (
                                                    <span className="text-[9px] text-muted-foreground block truncate">
                                                        {details.subtitle}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </TabsContent>

                    {/* NMR Account Tab */}
                    <TabsContent value="nmr" className="space-y-2 mt-0">
                        <div className="flex items-center justify-between px-3 py-1.5 glass-card rounded-lg border border-white/10 text-xs">
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">NMR Cleared:</span>
                                <span className="font-bold text-emerald-600 dark:text-emerald-400">₹{nmrPaidSum.toLocaleString('en-IN')}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span className="text-[10px] text-muted-foreground uppercase font-semibold">NMR Due:</span>
                                <span className="font-bold text-teal-600 dark:text-teal-400">₹{nmrPendingSum.toLocaleString('en-IN')}</span>
                            </div>
                        </div>

                        {/* Desktop Table */}
                        <div className="hidden md:block glass-card rounded-xl border border-white/10 overflow-hidden">
                            <Table className="text-xs">
                                <TableHeader className="bg-muted/30">
                                    <TableRow className="border-border/40">
                                        <TableHead className="font-semibold py-2 px-3">Date</TableHead>
                                        <TableHead className="font-semibold py-2 px-3">Details</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Amount</TableHead>
                                        <TableHead className="text-right font-semibold py-2 px-3">Status</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredNmrItems.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                                                No NMR labor transactions found.
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        filteredNmrItems.map((tx) => {
                                            const details = parseReferenceDetails(tx.reference_details);
                                            return (
                                                <TableRow
                                                    key={tx.id}
                                                    className="border-border/30 hover:bg-muted/20 cursor-pointer select-none text-xs"
                                                    onClick={() => setSelectedTxModal(tx)}
                                                >
                                                    <TableCell className="font-medium py-2 px-3">
                                                        {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                    </TableCell>
                                                    <TableCell className="max-w-[280px] py-2 px-3">
                                                        <div className="font-medium text-foreground truncate">{details.title}</div>
                                                        {details.subtitle && <div className="text-[10px] text-muted-foreground truncate">{details.subtitle}</div>}
                                                    </TableCell>
                                                    <TableCell className="text-right font-mono font-bold text-foreground py-2 px-3">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </TableCell>
                                                    <TableCell className="text-right py-2 px-3">{getStatusBadge(tx.status)}</TableCell>
                                                </TableRow>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Mobile Transaction Cards */}
                        <div className="block md:hidden divide-y divide-border/30 glass-card rounded-xl border border-white/10 overflow-hidden">
                            {filteredNmrItems.length === 0 ? (
                                <div className="text-center py-6 p-4 text-xs text-muted-foreground">
                                    No NMR labor transactions found.
                                </div>
                            ) : (
                                filteredNmrItems.map((tx) => {
                                    const details = parseReferenceDetails(tx.reference_details);
                                    return (
                                        <div
                                            key={tx.id}
                                            onClick={() => setSelectedTxModal(tx)}
                                            className="p-2.5 space-y-1.5 cursor-pointer active:bg-muted/20 transition-colors"
                                        >
                                            <div className="flex items-center justify-between gap-1.5">
                                                <span className="text-[10px] text-muted-foreground">
                                                    {tx.paid_at || tx.created_at ? new Date(tx.paid_at || tx.created_at).toLocaleDateString('en-IN') : 'Pending'}
                                                </span>
                                                <div className="flex items-center gap-1.5">
                                                    <span className="font-mono font-bold text-xs text-foreground">
                                                        ₹{Number(tx.amount_paid || 0).toLocaleString('en-IN')}
                                                    </span>
                                                    {getStatusBadge(tx.status)}
                                                </div>
                                            </div>

                                            <div className="min-w-0">
                                                <span className="text-[11px] font-semibold text-foreground block truncate">
                                                    {details.title}
                                                </span>
                                                {details.subtitle && (
                                                    <span className="text-[9px] text-muted-foreground block truncate">
                                                        {details.subtitle}
                                                    </span>
                                                )}
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

                {/* Payment Detail Dialog Modal */}
                <Dialog open={!!selectedTxModal} onOpenChange={(open) => !open && setSelectedTxModal(null)}>
                    <DialogContent className="max-w-md rounded-2xl p-4 sm:p-5 glass-card border border-white/20 shadow-xl max-h-[85vh] overflow-y-auto">
                        {selectedTxModal && (() => {
                            const details = parseReferenceDetails(selectedTxModal.reference_details);
                            let parsedJson: any = null;
                            if (typeof selectedTxModal.reference_details === 'string' && selectedTxModal.reference_details.trim().startsWith('{')) {
                                try {
                                    parsedJson = JSON.parse(selectedTxModal.reference_details);
                                } catch (e) {}
                            }

                            return (
                                <div className="space-y-3">
                                    <DialogHeader className="space-y-1.5 pb-2 border-b border-border/40">
                                        <div className="flex items-center justify-between gap-2">
                                            {getPayoutClassBadge(selectedTxModal.payout_class)}
                                            {getStatusBadge(selectedTxModal.status)}
                                        </div>
                                        <DialogTitle className="text-base sm:text-lg font-bold font-headline text-foreground">
                                            {details.title}
                                        </DialogTitle>
                                        <DialogDescription className="text-xs text-muted-foreground flex items-center gap-1">
                                            <Calendar className="h-3 w-3 text-primary/80" />
                                            <span>
                                                {selectedTxModal.paid_at || selectedTxModal.created_at
                                                    ? new Date(selectedTxModal.paid_at || selectedTxModal.created_at).toLocaleDateString('en-IN', {
                                                        day: 'numeric',
                                                        month: 'short',
                                                        year: 'numeric'
                                                      })
                                                    : 'Pending Settlement'}
                                            </span>
                                        </DialogDescription>
                                    </DialogHeader>

                                    {/* Financial Summary Box */}
                                    <div className="p-3 rounded-xl bg-muted/40 border border-border/40 space-y-2">
                                        <div className="flex items-baseline justify-between">
                                            <span className="text-[11px] text-muted-foreground font-semibold uppercase tracking-wider">Amount Paid</span>
                                            <span className="text-lg sm:text-xl font-mono font-extrabold text-foreground">
                                                ₹{Number(selectedTxModal.amount_paid || 0).toLocaleString('en-IN')}
                                            </span>
                                        </div>

                                        {Number(selectedTxModal.amount_due || 0) > 0 && (
                                            <div className="flex items-baseline justify-between pt-1.5 border-t border-border/30">
                                                <span className="text-[11px] text-muted-foreground">Original Due Amount</span>
                                                <span className="text-xs font-mono font-semibold text-muted-foreground">
                                                    ₹{Number(selectedTxModal.amount_due || 0).toLocaleString('en-IN')}
                                                </span>
                                            </div>
                                        )}
                                    </div>

                                    {/* Metadata Grid */}
                                    <div className="grid grid-cols-2 gap-2 text-xs">
                                        <div className="p-2 rounded-lg bg-background/50 border border-border/30 flex flex-col gap-0.5">
                                            <span className="text-[9px] text-muted-foreground font-semibold uppercase flex items-center gap-1">
                                                <Building2 className="h-2.5 w-2.5 text-primary" /> Site
                                            </span>
                                            <span className="font-bold text-foreground truncate text-[11px]">
                                                {selectedTxModal.project?.name || 'All Sites'}
                                            </span>
                                        </div>

                                        <div className="p-2 rounded-lg bg-background/50 border border-border/30 flex flex-col gap-0.5">
                                            <span className="text-[9px] text-muted-foreground font-semibold uppercase flex items-center gap-1">
                                                <Calendar className="h-2.5 w-2.5 text-primary" /> Settlement
                                            </span>
                                            <span className="font-bold text-foreground truncate text-[11px]">
                                                {selectedTxModal.paid_at ? new Date(selectedTxModal.paid_at).toLocaleDateString('en-IN') : 'Pending'}
                                            </span>
                                        </div>
                                    </div>

                                    {/* NMR Daily Muster Breakdown Table if JSON */}
                                    {parsedJson?.type === 'contractor_wages' && Array.isArray(parsedJson.breakdown) && parsedJson.breakdown.length > 0 && (
                                        <div className="space-y-1.5 pt-1">
                                            <h4 className="text-[11px] font-bold text-foreground uppercase tracking-wider flex items-center gap-1">
                                                <FileText className="h-3 w-3 text-primary" />
                                                Labor Breakdown
                                            </h4>
                                            <div className="rounded-lg border border-border/40 overflow-hidden bg-background/40">
                                                <Table className="text-xs">
                                                    <TableHeader className="bg-muted/40">
                                                        <TableRow>
                                                            <TableHead className="py-1 px-2 text-[10px]">Worker</TableHead>
                                                            <TableHead className="py-1 px-2 text-[10px] text-center">Days</TableHead>
                                                            <TableHead className="py-1 px-2 text-[10px] text-right">Daily Rate</TableHead>
                                                            <TableHead className="py-1 px-2 text-[10px] text-right">Total</TableHead>
                                                        </TableRow>
                                                    </TableHeader>
                                                    <TableBody>
                                                        {parsedJson.breakdown.map((row: any, idx: number) => {
                                                            const lineTotal = Number(row.days || 0) * Number(row.rate || 0);
                                                            return (
                                                                <TableRow key={idx} className="border-border/30">
                                                                    <TableCell className="py-1 px-2 font-medium text-[11px]">{row.category}</TableCell>
                                                                    <TableCell className="py-1 px-2 text-center text-[11px]">{row.days}</TableCell>
                                                                    <TableCell className="py-1 px-2 text-right font-mono text-[11px]">₹{row.rate}</TableCell>
                                                                    <TableCell className="py-1 px-2 text-right font-mono font-semibold text-[11px]">₹{lineTotal.toLocaleString('en-IN')}</TableCell>
                                                                </TableRow>
                                                            );
                                                        })}
                                                    </TableBody>
                                                </Table>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            );
                        })()}
                    </DialogContent>
                </Dialog>

            </div>
        </main>
    );
}
