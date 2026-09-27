'use client';

import { useEffect, useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getContractorAccounts } from '@/app/actions/contractors';
import { getProjects, getSalaryProfiles, saveSalaryProfile } from '@/app/actions/financials';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
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
    ShieldCheck,
    Landmark,
    Plus,
    Trash2,
    Check,
    CreditCard
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

interface SalaryProfile {
    id: string;
    contractor_id: string | null;
    rates: Record<string, number>;
    payment_type: string;
    bank_name: string | null;
    account_number: string | null;
    ifsc_code: string | null;
}

const COMMON_WORKER_TYPES = [
    'Mason',
    'MC (Mason Coolie)',
    'FC (Female Coolie)',
    'Carpenter',
    'Bar Bender',
    'Electrician',
    'Plumber',
    'Helper',
    'Painter',
    'Supervisor'
];

export default function ContractorDetailPage() {
    const params = useParams();
    const router = useRouter();
    const { toast } = useToast();
    const contractorId = typeof params?.contractorId === 'string' ? params.contractorId : '';

    const [account, setAccount] = useState<ContractorAccount | null>(null);
    const [salaryProfile, setSalaryProfile] = useState<SalaryProfile | null>(null);
    const [projects, setProjects] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [ledgerProjectFilter, setLedgerProjectFilter] = useState<string>('all');
    const [selectedTxModal, setSelectedTxModal] = useState<any | null>(null);
    const [isEditOpen, setIsEditOpen] = useState(false);

    // Rate card & bank modal state
    const [isRateBankModalOpen, setIsRateBankModalOpen] = useState(false);
    const [isSavingRates, setIsSavingRates] = useState(false);
    const [bankName, setBankName] = useState('');
    const [accountNumber, setAccountNumber] = useState('');
    const [ifscCode, setIfscCode] = useState('');
    const [contractorRates, setContractorRates] = useState<Record<string, number>>({});
    const [newWorkerTypeSelect, setNewWorkerTypeSelect] = useState('Mason');
    const [newWorkerTypeCustom, setNewWorkerTypeCustom] = useState('');
    const [newWorkerRate, setNewWorkerRate] = useState('');

    const loadData = async () => {
        setIsLoading(true);
        try {
            const [accRes, projData, profilesData] = await Promise.all([
                getContractorAccounts(),
                getProjects(),
                getSalaryProfiles()
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

            // Match salary profile
            const matchedProfile = (profilesData as SalaryProfile[] || []).find(
                (p) => p.contractor_id === contractorId
            );
            if (matchedProfile) {
                setSalaryProfile(matchedProfile);
                setBankName(matchedProfile.bank_name || '');
                setAccountNumber(matchedProfile.account_number || '');
                setIfscCode(matchedProfile.ifsc_code || '');
                setContractorRates(matchedProfile.rates || {});
            } else {
                setSalaryProfile(null);
                setBankName('');
                setAccountNumber('');
                setIfscCode('');
                setContractorRates({});
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

    const handleOpenRateBankModal = () => {
        if (salaryProfile) {
            setBankName(salaryProfile.bank_name || '');
            setAccountNumber(salaryProfile.account_number || '');
            setIfscCode(salaryProfile.ifsc_code || '');
            setContractorRates(salaryProfile.rates || {});
        } else {
            setBankName('');
            setAccountNumber('');
            setIfscCode('');
            setContractorRates({});
        }
        setNewWorkerTypeSelect('Mason');
        setNewWorkerTypeCustom('');
        setNewWorkerRate('');
        setIsRateBankModalOpen(true);
    };

    const handleAddRate = () => {
        const type = newWorkerTypeSelect === 'custom' ? newWorkerTypeCustom.trim() : newWorkerTypeSelect;
        if (!type) {
            toast({
                title: 'Error',
                description: 'Please specify a worker type.',
                variant: 'destructive'
            });
            return;
        }

        if (!newWorkerRate || isNaN(Number(newWorkerRate)) || Number(newWorkerRate) <= 0) {
            toast({
                title: 'Error',
                description: 'Please enter a valid daily wage rate.',
                variant: 'destructive'
            });
            return;
        }

        setContractorRates(prev => ({
            ...prev,
            [type]: Number(newWorkerRate)
        }));

        setNewWorkerRate('');
        setNewWorkerTypeCustom('');
    };

    const handleRemoveRate = (key: string) => {
        setContractorRates(prev => {
            const copy = { ...prev };
            delete copy[key];
            return copy;
        });
    };

    const handleSaveRateBankDetails = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSavingRates(true);
        try {
            const res = await saveSalaryProfile({
                id: salaryProfile?.id,
                contractor_id: contractorId,
                rates: contractorRates,
                payment_type: 'daily_wage',
                rate: 0,
                bank_name: bankName.trim() || undefined,
                account_number: accountNumber.trim() || undefined,
                ifsc_code: ifscCode.trim() || undefined
            });

            if (res.success) {
                toast({
                    title: 'Saved Successfully',
                    description: 'Contractor rate card and bank details updated.'
                });
                setIsRateBankModalOpen(false);
                await loadData();
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to save rate card.',
                    variant: 'destructive'
                });
            }
        } catch (error: any) {
            console.error('Error saving contractor rates:', error);
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            });
        } finally {
            setIsSavingRates(false);
        }
    };

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

    const ratesEntries = Object.entries(salaryProfile?.rates || {});

    return (
        <main className="flex-1 p-2.5 sm:p-4 md:p-5 overflow-y-auto bg-transparent">
            <div className="max-w-6xl mx-auto space-y-2.5 sm:space-y-3">

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
                        <span>Edit Info</span>
                    </Button>
                </div>

                {/* Compact Inline Contact Strip */}
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

                {/* Unified Rate Card & Bank Payout Account Bar */}
                <div className="glass-card rounded-xl p-2.5 sm:p-3 border border-white/10 dark:border-white/5 space-y-2">
                    <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5">
                            <Hammer className="h-3.5 w-3.5 text-primary" />
                            <span className="text-xs font-bold text-foreground">
                                Daily Wage Rates & Payout Account
                            </span>
                        </div>
                        <Button
                            size="sm"
                            variant="ghost"
                            onClick={handleOpenRateBankModal}
                            className="h-6 text-xs text-primary hover:text-primary hover:bg-primary/10 rounded-md px-2"
                        >
                            <Edit className="h-3 w-3 mr-1" />
                            <span>Edit Rates & Bank</span>
                        </Button>
                    </div>

                    {/* Rates Badges + Bank Details in compact view */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1 border-t border-border/30 text-xs">
                        {/* Wage Rates Badges */}
                        <div className="flex flex-wrap items-center gap-1.5 flex-1 min-w-0">
                            {ratesEntries.length === 0 ? (
                                <span className="text-[11px] text-muted-foreground italic">
                                    No daily wage rates configured yet. Click edit to set trade wages.
                                </span>
                            ) : (
                                ratesEntries.map(([workerType, rate]) => (
                                    <div
                                        key={workerType}
                                        className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-muted/40 border border-border/40 text-[10px]"
                                    >
                                        <span className="text-foreground font-medium">{workerType}:</span>
                                        <span className="font-mono font-bold text-primary">₹{Number(rate).toLocaleString('en-IN')}/d</span>
                                    </div>
                                ))
                            )}
                        </div>

                        {/* Bank Account Summary */}
                        <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground shrink-0 sm:border-l sm:border-border/30 sm:pl-3">
                            <Landmark className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                            {salaryProfile?.account_number ? (
                                <span className="truncate">
                                    <span className="font-semibold text-foreground">{salaryProfile.bank_name || 'Bank'}</span>: ••••{salaryProfile.account_number.slice(-4)}
                                    {salaryProfile.ifsc_code && <span className="opacity-75 ml-1">({salaryProfile.ifsc_code})</span>}
                                </span>
                            ) : (
                                <span className="italic text-muted-foreground/80">No bank account linked</span>
                            )}
                        </div>
                    </div>
                </div>

                {/* Sub-account Tabs & Site Filter in one cohesive strip */}
                <Tabs defaultValue="combined" className="w-full space-y-2">
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

                {/* Edit Contractor General Details Modal */}
                <EditContractorDialog
                    open={isEditOpen}
                    onOpenChange={setIsEditOpen}
                    contractor={account.contractor}
                    onSuccess={() => loadData()}
                />

                {/* Edit Rate Card & Bank Details Modal */}
                <Dialog open={isRateBankModalOpen} onOpenChange={setIsRateBankModalOpen}>
                    <DialogContent className="max-w-md sm:max-w-lg rounded-2xl p-4 sm:p-5 glass-card border border-white/20 shadow-2xl max-h-[88vh] overflow-y-auto">
                        <DialogHeader className="pb-2 border-b border-border/40">
                            <DialogTitle className="text-base sm:text-lg font-bold font-headline text-foreground flex items-center gap-2">
                                <Hammer className="h-4 w-4 text-primary" />
                                <span>Wage Rates & Bank Details</span>
                            </DialogTitle>
                            <DialogDescription className="text-xs text-muted-foreground">
                                Configure {account.contractor.name}&apos;s daily wage rate card and bank account for settlements.
                            </DialogDescription>
                        </DialogHeader>

                        <form onSubmit={handleSaveRateBankDetails} className="space-y-4 pt-1">
                            {/* Section 1: Trade Daily Wage Rates */}
                            <div className="space-y-2">
                                <Label className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                                    <Coins className="h-3.5 w-3.5 text-primary" />
                                    Daily Wage Rates (₹ / day)
                                </Label>

                                {/* Existing configured rates */}
                                <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                                    {Object.entries(contractorRates).length === 0 ? (
                                        <div className="text-xs text-muted-foreground italic p-2 rounded-lg bg-muted/30 border border-border/30 text-center">
                                            No rates configured yet. Add worker types below.
                                        </div>
                                    ) : (
                                        Object.entries(contractorRates).map(([type, rate]) => (
                                            <div
                                                key={type}
                                                className="flex items-center justify-between p-1.5 px-2.5 rounded-lg bg-background/50 border border-border/30 text-xs"
                                            >
                                                <span className="font-semibold text-foreground">{type}</span>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-mono font-bold text-primary">₹{Number(rate).toLocaleString('en-IN')}/day</span>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleRemoveRate(type)}
                                                        className="text-muted-foreground hover:text-destructive p-0.5"
                                                    >
                                                        <Trash2 className="h-3.5 w-3.5" />
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>

                                {/* Add new rate row */}
                                <div className="flex items-center gap-1.5 pt-1">
                                    <div className="w-[140px] sm:w-[160px] shrink-0">
                                        <Select
                                            value={newWorkerTypeSelect}
                                            onValueChange={(val) => setNewWorkerTypeSelect(val)}
                                        >
                                            <SelectTrigger className="h-8 text-xs rounded-lg bg-background/50">
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {COMMON_WORKER_TYPES.map(type => (
                                                    <SelectItem key={type} value={type} className="text-xs">{type}</SelectItem>
                                                ))}
                                                <SelectItem value="custom" className="text-xs font-semibold">+ Custom Type</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    {newWorkerTypeSelect === 'custom' && (
                                        <Input
                                            placeholder="Type name"
                                            value={newWorkerTypeCustom}
                                            onChange={(e) => setNewWorkerTypeCustom(e.target.value)}
                                            className="h-8 text-xs rounded-lg bg-background/50 flex-1 min-w-[90px]"
                                        />
                                    )}

                                    <Input
                                        placeholder="₹ Rate"
                                        type="number"
                                        value={newWorkerRate}
                                        onChange={(e) => setNewWorkerRate(e.target.value)}
                                        className="h-8 text-xs rounded-lg bg-background/50 flex-1"
                                    />

                                    <Button
                                        type="button"
                                        size="sm"
                                        variant="secondary"
                                        onClick={handleAddRate}
                                        className="h-8 px-2.5 rounded-lg text-xs shrink-0"
                                    >
                                        <Plus className="h-3.5 w-3.5 mr-0.5" />
                                        <span>Add</span>
                                    </Button>
                                </div>
                            </div>

                            {/* Section 2: Bank Account Details */}
                            <div className="space-y-2 pt-2 border-t border-border/40">
                                <Label className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                                    <Landmark className="h-3.5 w-3.5 text-primary" />
                                    Bank Payout Account
                                </Label>

                                <div className="space-y-2 text-xs">
                                    <div>
                                        <Label className="text-[11px] text-muted-foreground">Bank Name</Label>
                                        <Input
                                            placeholder="e.g. HDFC Bank, SBI, ICICI"
                                            value={bankName}
                                            onChange={(e) => setBankName(e.target.value)}
                                            className="h-8 text-xs rounded-lg bg-background/50 mt-0.5"
                                        />
                                    </div>

                                    <div className="grid grid-cols-2 gap-2">
                                        <div>
                                            <Label className="text-[11px] text-muted-foreground">Account Number</Label>
                                            <Input
                                                placeholder="A/C Number"
                                                value={accountNumber}
                                                onChange={(e) => setAccountNumber(e.target.value)}
                                                className="h-8 text-xs rounded-lg bg-background/50 font-mono mt-0.5"
                                            />
                                        </div>
                                        <div>
                                            <Label className="text-[11px] text-muted-foreground">IFSC Code</Label>
                                            <Input
                                                placeholder="IFSC Code"
                                                value={ifscCode}
                                                onChange={(e) => setIfscCode(e.target.value.toUpperCase())}
                                                className="h-8 text-xs rounded-lg bg-background/50 uppercase font-mono mt-0.5"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <DialogFooter className="flex-row items-center justify-end gap-2 pt-2 border-t border-border/30">
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => setIsRateBankModalOpen(false)}
                                    className="h-8 text-xs rounded-lg"
                                >
                                    Cancel
                                </Button>
                                <Button
                                    type="submit"
                                    size="sm"
                                    disabled={isSavingRates}
                                    className="h-8 text-xs rounded-lg gap-1"
                                >
                                    {isSavingRates ? (
                                        <>
                                            <Loader2 className="h-3 w-3 animate-spin" />
                                            <span>Saving...</span>
                                        </>
                                    ) : (
                                        <>
                                            <Check className="h-3 w-3" />
                                            <span>Save Rates & Bank</span>
                                        </>
                                    )}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>

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
