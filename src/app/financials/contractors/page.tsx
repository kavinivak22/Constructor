'use client'

import { useEffect, useState } from 'react'
import { getContractorAccounts } from '@/app/actions/contractors'
import { getProjects } from '@/app/actions/financials'
import { useRouter, useSearchParams } from 'next/navigation'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useToast } from '@/hooks/use-toast'
import { cn } from '@/lib/utils'
import {
    Loader2,
    Search,
    Building2,
    ArrowLeft,
    Phone,
    Mail,
    User,
    Wallet,
    Layers,
    Calendar,
    Briefcase,
    Activity,
    CreditCard,
    CheckCircle2,
    Clock,
    AlertCircle,
    ChevronDown,
    ChevronUp
} from 'lucide-react'

interface Contractor {
    id: string
    name: string
    category: string | null
    contactPerson: string | null
    phone: string | null
    email: string | null
}

interface AccountSummary {
    totalDue: number
    totalPaid: number
    totalPending: number
    items: any[]
}

interface ContractorAccount {
    contractor: Contractor
    rateAccount: AccountSummary
    nmrAccount: AccountSummary
    allTransactions: any[]
    bankDetails?: {
        bank_name: string | null
        account_number: string | null
        ifsc_code: string | null
    }
}

export default function ContractorAccountsPage() {
    const { toast } = useToast()
    const router = useRouter()
    const searchParams = useSearchParams()
    const contractorIdParam = searchParams?.get('contractorId')
    
    const [accounts, setAccounts] = useState<ContractorAccount[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [searchQuery, setSearchQuery] = useState('')
    
    // Detailed ledger state
    const [selectedAccount, setSelectedAccount] = useState<ContractorAccount | null>(null)

    // Project and building views state
    const [projects, setProjects] = useState<any[]>([])
    const [selectedBuildingId, setSelectedBuildingId] = useState<string>('all')
    const [ledgerProjectFilter, setLedgerProjectFilter] = useState<string>('all')
    const [expandedTxId, setExpandedTxId] = useState<string | null>(null)
    const [viewMode, setViewMode] = useState<'contractors' | 'buildings'>('contractors')

    const loadAccounts = async () => {
        setIsLoading(true)
        try {
            const res = await getContractorAccounts()
            if (res.success && res.data) {
                // Fetch salary profiles to extract bank details
                setAccounts(res.data as ContractorAccount[])
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to load contractor accounts.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error loading contractor accounts:', error)
            toast({
                title: 'Error',
                description: 'Failed to load accounts data.',
                variant: 'destructive'
            })
        } finally {
            setIsLoading(false)
        }
    }

    const loadProjects = async () => {
        try {
            const data = await getProjects()
            setProjects(data || [])
            if (data && data.length > 0) {
                setSelectedBuildingId(data[0].id)
            }
        } catch (error) {
            console.error('Error loading projects:', error)
        }
    }

    useEffect(() => {
        loadAccounts()
        loadProjects()
    }, [])

    useEffect(() => {
        if (contractorIdParam && accounts.length > 0) {
            const matched = accounts.find(acc => acc.contractor.id === contractorIdParam)
            if (matched) {
                setSelectedAccount(matched)
            }
        } else {
            setSelectedAccount(null)
        }
    }, [contractorIdParam, accounts])

    const filteredAccounts = accounts.filter(acc => 
        acc.contractor.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (acc.contractor.category || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (acc.contractor.contactPerson || '').toLowerCase().includes(searchQuery.toLowerCase())
    )

    // Summary calculations
    const stats = {
        totalRateOutstanding: accounts.reduce((sum, a) => sum + a.rateAccount.totalPending, 0),
        totalRateSettled: accounts.reduce((sum, a) => sum + a.rateAccount.totalPaid, 0),
        totalNmrOutstanding: accounts.reduce((sum, a) => sum + a.nmrAccount.totalPending, 0),
        totalNmrSettled: accounts.reduce((sum, a) => sum + a.nmrAccount.totalPaid, 0),
        contractorCount: accounts.length
    }

    // Building specific stats
    const buildingStats = {
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
    }

    const contractorsForSelectedBuilding = accounts.map(a => {
        const buildingTx = a.allTransactions.filter(tx => tx.project_id === selectedBuildingId);
        if (buildingTx.length === 0) return null;
        
        const ratePaid = buildingTx.filter(tx => tx.payout_class === 'rate' && tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
        const ratePending = buildingTx.filter(tx => tx.payout_class === 'rate' && tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
        
        const nmrPaid = buildingTx.filter(tx => (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
        const nmrPending = buildingTx.filter(tx => (tx.payout_class === 'nmr' || !tx.payout_class) && tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
        
        return {
            ...a,
            buildingStats: {
                ratePaid,
                ratePending,
                nmrPaid,
                nmrPending,
            }
        };
    }).filter(Boolean) as (ContractorAccount & { buildingStats: { ratePaid: number, ratePending: number, nmrPaid: number, nmrPending: number } })[];

    const handleOpenLedger = (account: ContractorAccount) => {
        setLedgerProjectFilter('all')
        setExpandedTxId(null)
        router.push(`/financials/contractors?contractorId=${account.contractor.id}`)
    }

    const handleOpenLedgerForBuilding = (account: ContractorAccount, buildingId: string) => {
        setLedgerProjectFilter(buildingId)
        setExpandedTxId(null)
        router.push(`/financials/contractors?contractorId=${account.contractor.id}`)
    }

    const handleCloseLedger = () => {
        router.push('/financials/contractors')
    }

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'paid':
                return <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">Paid</Badge>
            case 'held':
                return <Badge className="bg-rose-500/20 text-rose-400 border-rose-500/30">Held</Badge>
            default:
                return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30">Pending</Badge>
        }
    }

    const getPayoutClassBadge = (payoutClass: string) => {
        return payoutClass === 'rate' ? (
            <Badge className="bg-purple-500/20 text-purple-300 border-purple-500/30 font-semibold text-[10px]">
                RATE CONTRACT
            </Badge>
        ) : (
            <Badge className="bg-teal-500/20 text-teal-300 border-teal-500/30 font-semibold text-[10px]">
                NMR LABOR
            </Badge>
        )
    }

    const filteredAllTransactions = selectedAccount 
        ? (ledgerProjectFilter === 'all' 
            ? selectedAccount.allTransactions 
            : selectedAccount.allTransactions.filter(tx => tx.project_id === ledgerProjectFilter))
        : [];
        
    const filteredRateItems = selectedAccount 
        ? (ledgerProjectFilter === 'all' 
            ? selectedAccount.rateAccount.items 
            : selectedAccount.rateAccount.items.filter(tx => tx.project_id === ledgerProjectFilter))
        : [];
        
    const filteredNmrItems = selectedAccount 
        ? (ledgerProjectFilter === 'all' 
            ? selectedAccount.nmrAccount.items 
            : selectedAccount.nmrAccount.items.filter(tx => tx.project_id === ledgerProjectFilter))
        : [];

    const totalPaidSum = filteredAllTransactions.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
    const totalPendingSum = filteredAllTransactions.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

    const rateTotalDueSum = filteredRateItems.reduce((sum, tx) => sum + Number(tx.amount_due), 0);
    const rateTotalPaidSum = filteredRateItems.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
    const rateTotalPendingSum = filteredRateItems.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

    const nmrTotalDueSum = filteredNmrItems.reduce((sum, tx) => sum + Number(tx.amount_due), 0);
    const nmrTotalPaidSum = filteredNmrItems.filter(tx => tx.status === 'paid').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);
    const nmrTotalPendingSum = filteredNmrItems.filter(tx => tx.status === 'pending').reduce((sum, tx) => sum + Number(tx.amount_paid), 0);

    return (
        <div className="flex-1 p-4 overflow-y-auto md:p-8 bg-transparent">
            <div className="max-w-6xl mx-auto space-y-6">
                {!selectedAccount ? (
                    <>
                        {/* Header */}
                        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                            <div>
                                <h1 className="text-3xl font-bold tracking-tight font-headline text-foreground bg-gradient-to-r from-foreground via-foreground/80 to-muted-foreground bg-clip-text text-transparent">
                                    Contractor Accounts & Ledgers
                                </h1>
                                <p className="text-muted-foreground mt-1">
                                    Track and reconcile contractor square-footage Rate contracts and NMR labor wage accounts separately for settlements.
                                </p>
                            </div>
                        </div>

                        {/* View Mode Toggle */}
                        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-muted/5 border border-muted/15 p-2 rounded-xl">
                            <Tabs value={viewMode} onValueChange={(val) => setViewMode(val as 'contractors' | 'buildings')} className="w-full sm:w-auto">
                                <TabsList className="bg-background/40 border border-muted/10 p-1 rounded-lg">
                                    <TabsTrigger value="contractors" className="text-xs font-semibold px-4">
                                        View by Contractor
                                    </TabsTrigger>
                                    <TabsTrigger value="buildings" className="text-xs font-semibold px-4">
                                        View by Building / Site
                                    </TabsTrigger>
                                </TabsList>
                            </Tabs>
                            
                            {viewMode === 'buildings' && projects.length > 0 && (
                                <div className="flex items-center gap-2 w-full sm:w-auto">
                                    <span className="text-xs text-muted-foreground whitespace-nowrap font-medium">Select Building:</span>
                                    <Select 
                                        value={selectedBuildingId} 
                                        onValueChange={(val) => setSelectedBuildingId(val)}
                                    >
                                        <SelectTrigger className="w-full sm:w-[220px] bg-background/50 border-muted/30 h-9 text-xs">
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

                        {viewMode === 'contractors' ? (
                            <>
                                {/* Summary Widgets - Contractor View */}
                                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Rate Contract Outstanding</span>
                                            <span className="text-2xl font-bold text-purple-400 mt-1">₹{stats.totalRateOutstanding.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Pending rate settlements</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Rate Contract Settled</span>
                                            <span className="text-2xl font-bold text-emerald-400 mt-1">₹{stats.totalRateSettled.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Total contract payouts cleared</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">NMR Labor Outstanding</span>
                                            <span className="text-2xl font-bold text-amber-400 mt-1">₹{stats.totalNmrOutstanding.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Pending daily labor payouts</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">NMR Labor Settled</span>
                                            <span className="text-2xl font-bold text-sky-400 mt-1">₹{stats.totalNmrSettled.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Total day-work wages cleared</span>
                                        </CardContent>
                                    </Card>
                                </div>

                                {/* Main Accounts Table */}
                                <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
                                    <CardHeader className="pb-3 border-b border-muted/20">
                                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                            <div>
                                                <CardTitle className="text-xl font-semibold">Active Contractor Accounts</CardTitle>
                                                <CardDescription>Consolidated ledger view showing contract vs daily wage outstanding balances.</CardDescription>
                                            </div>
                                            <div className="relative w-full md:w-80">
                                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                                <Input
                                                    placeholder="Search contractor or category..."
                                                    value={searchQuery}
                                                    onChange={(e) => setSearchQuery(e.target.value)}
                                                    className="pl-9 w-full bg-background/50 border-muted/30 focus-visible:ring-primary h-9"
                                                />
                                            </div>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="p-0">
                                        {isLoading ? (
                                            <div className="flex flex-col items-center justify-center py-20 gap-3">
                                                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                                <p className="text-muted-foreground text-sm">Loading contractor accounts...</p>
                                            </div>
                                        ) : filteredAccounts.length === 0 ? (
                                            <div className="text-center py-16 px-4">
                                                <Building2 className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                                <h3 className="text-lg font-medium text-foreground">No Contractor Accounts Found</h3>
                                                <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                                    Contractors with payday transactions or active wage profiles will automatically appear here.
                                                </p>
                                            </div>
                                        ) : (
                                            <div className="overflow-x-auto">
                                                <Table>
                                                    <TableHeader className="bg-muted/10">
                                                        <TableRow className="border-muted/20">
                                                            <TableHead className="w-[220px]">Contractor</TableHead>
                                                            <TableHead>Category</TableHead>
                                                            <TableHead className="text-center bg-purple-500/5 border-x border-muted/10">Rate Contract Account (Sq.Ft)</TableHead>
                                                            <TableHead className="text-center bg-teal-500/5 border-r border-muted/10">NMR Account (Daily Wages)</TableHead>
                                                            <TableHead className="text-right w-[150px]">Actions</TableHead>
                                                        </TableRow>
                                                    </TableHeader>
                                                    <TableBody>
                                                        {filteredAccounts.map((account) => (
                                                            <TableRow key={account.contractor.id} className="border-muted/15 hover:bg-muted/5 transition-colors">
                                                                <TableCell className="font-semibold">
                                                                    <div className="flex flex-col">
                                                                        <span className="text-foreground text-sm">{account.contractor.name}</span>
                                                                        {account.contractor.contactPerson && (
                                                                            <span className="text-[10px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                        )}
                                                                    </div>
                                                                </TableCell>
                                                                <TableCell>
                                                                    <Badge variant="secondary" className="text-xs bg-muted/40 border-muted-foreground/10 text-foreground font-normal">
                                                                        {account.contractor.category || 'General Contractor'}
                                                                    </Badge>
                                                                </TableCell>
                                                                {/* Rate Balance Cell */}
                                                                <TableCell className="bg-purple-500/5 border-x border-muted/10 py-3">
                                                                    <div className="flex flex-col items-center justify-center space-y-0.5">
                                                                        <span className="text-xs font-semibold text-foreground">
                                                                            ₹{account.rateAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-400 font-normal">(Cleared)</span>
                                                                        </span>
                                                                        <span className="text-xs font-bold text-purple-400">
                                                                            ₹{account.rateAccount.totalPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Outstanding)</span>
                                                                        </span>
                                                                    </div>
                                                                </TableCell>
                                                                {/* NMR Balance Cell */}
                                                                <TableCell className="bg-teal-500/5 border-r border-muted/10 py-3">
                                                                    <div className="flex flex-col items-center justify-center space-y-0.5">
                                                                        <span className="text-xs font-semibold text-foreground">
                                                                            ₹{account.nmrAccount.totalPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-400 font-normal">(Cleared)</span>
                                                                        </span>
                                                                        <span className="text-xs font-bold text-teal-400">
                                                                            ₹{account.nmrAccount.totalPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Outstanding)</span>
                                                                        </span>
                                                                    </div>
                                                                </TableCell>
                                                                <TableCell className="text-right">
                                                                    <Button
                                                                        variant="outline"
                                                                        size="sm"
                                                                        onClick={() => handleOpenLedger(account)}
                                                                        className="border-muted/30 hover:bg-muted/10 text-xs"
                                                                    >
                                                                        View Ledger
                                                                    </Button>
                                                                </TableCell>
                                                            </TableRow>
                                                        ))}
                                                    </TableBody>
                                                </Table>
                                            </div>
                                        )}
                                    </CardContent>
                                </Card>
                            </>
                        ) : (
                            <>
                                {/* Summary Widgets - Building View */}
                                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Building Rate Outstanding</span>
                                            <span className="text-2xl font-bold text-purple-400 mt-1">₹{selectedBuildingId === 'all' ? 0 : buildingStats.rateOutstanding.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Pending rate settlements</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Building Rate Settled</span>
                                            <span className="text-2xl font-bold text-emerald-400 mt-1">₹{selectedBuildingId === 'all' ? 0 : buildingStats.rateSettled.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Total contract payouts cleared</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Building NMR Outstanding</span>
                                            <span className="text-2xl font-bold text-amber-400 mt-1">₹{selectedBuildingId === 'all' ? 0 : buildingStats.nmrOutstanding.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Pending daily labor payouts</span>
                                        </CardContent>
                                    </Card>
                                    <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                        <CardContent className="p-4 flex flex-col justify-center">
                                            <span className="text-xs text-muted-foreground uppercase font-medium">Building NMR Settled</span>
                                            <span className="text-2xl font-bold text-sky-400 mt-1">₹{selectedBuildingId === 'all' ? 0 : buildingStats.nmrSettled.toLocaleString('en-IN')}</span>
                                            <span className="text-[10px] text-muted-foreground mt-0.5">Total day-work wages cleared</span>
                                        </CardContent>
                                    </Card>
                                </div>

                                {/* Building-specific Accounts Table */}
                                <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
                                    <CardHeader className="pb-3 border-b border-muted/20">
                                        <div>
                                            <CardTitle className="text-xl font-semibold">Active Contractors on Site</CardTitle>
                                            <CardDescription>
                                                List of contractors with logged transactions or active work balances under the selected building.
                                            </CardDescription>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="p-0">
                                        {isLoading ? (
                                            <div className="flex flex-col items-center justify-center py-20 gap-3">
                                                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                                <p className="text-muted-foreground text-sm">Loading contractor accounts...</p>
                                            </div>
                                        ) : selectedBuildingId === 'all' ? (
                                            <div className="text-center py-16 px-4">
                                                <Building2 className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                                <h3 className="text-lg font-medium text-foreground">Select a Building</h3>
                                                <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                                    Please select a building from the dropdown to view its contractor list and active accounts.
                                                </p>
                                            </div>
                                        ) : contractorsForSelectedBuilding.length === 0 ? (
                                            <div className="text-center py-16 px-4">
                                                <Building2 className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                                <h3 className="text-lg font-medium text-foreground">No Contractors Found on Site</h3>
                                                <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                                    No contractor ledger balances or payday transactions are logged for this building yet.
                                                </p>
                                            </div>
                                        ) : (
                                            <div className="overflow-x-auto">
                                                <Table>
                                                    <TableHeader className="bg-muted/10">
                                                        <TableRow className="border-muted/20">
                                                            <TableHead className="w-[220px]">Contractor</TableHead>
                                                            <TableHead>Category</TableHead>
                                                            <TableHead className="text-center bg-purple-500/5 border-x border-muted/10">Rate Contract (Sq.Ft)</TableHead>
                                                            <TableHead className="text-center bg-teal-500/5 border-r border-muted/10">NMR Account (Daily Wages)</TableHead>
                                                            <TableHead className="text-right w-[150px]">Actions</TableHead>
                                                        </TableRow>
                                                    </TableHeader>
                                                    <TableBody>
                                                        {contractorsForSelectedBuilding.map((account) => (
                                                            <TableRow key={account.contractor.id} className="border-muted/15 hover:bg-muted/5 transition-colors">
                                                                <TableCell className="font-semibold">
                                                                    <div className="flex flex-col">
                                                                        <span className="text-foreground text-sm">{account.contractor.name}</span>
                                                                        {account.contractor.contactPerson && (
                                                                            <span className="text-[10px] text-muted-foreground font-normal">Contact: {account.contractor.contactPerson}</span>
                                                                        )}
                                                                    </div>
                                                                </TableCell>
                                                                <TableCell>
                                                                    <Badge variant="secondary" className="text-xs bg-muted/40 border-muted-foreground/10 text-foreground font-normal">
                                                                        {account.contractor.category || 'General Contractor'}
                                                                    </Badge>
                                                                </TableCell>
                                                                {/* Rate Balance Cell */}
                                                                <TableCell className="bg-purple-500/5 border-x border-muted/10 py-3">
                                                                    <div className="flex flex-col items-center justify-center space-y-0.5">
                                                                        <span className="text-xs font-semibold text-foreground">
                                                                            ₹{account.buildingStats.ratePaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-400 font-normal">(Cleared)</span>
                                                                        </span>
                                                                        <span className="text-xs font-bold text-purple-400">
                                                                            ₹{account.buildingStats.ratePending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Outstanding)</span>
                                                                        </span>
                                                                    </div>
                                                                </TableCell>
                                                                {/* NMR Balance Cell */}
                                                                <TableCell className="bg-teal-500/5 border-r border-muted/10 py-3">
                                                                    <div className="flex flex-col items-center justify-center space-y-0.5">
                                                                        <span className="text-xs font-semibold text-foreground">
                                                                            ₹{account.buildingStats.nmrPaid.toLocaleString('en-IN')} <span className="text-[10px] text-emerald-400 font-normal">(Cleared)</span>
                                                                        </span>
                                                                        <span className="text-xs font-bold text-teal-400">
                                                                            ₹{account.buildingStats.nmrPending.toLocaleString('en-IN')} <span className="text-[10px] text-muted-foreground font-normal">(Outstanding)</span>
                                                                        </span>
                                                                    </div>
                                                                </TableCell>
                                                                <TableCell className="text-right">
                                                                    <Button
                                                                        variant="outline"
                                                                        size="sm"
                                                                        onClick={() => handleOpenLedgerForBuilding(account, selectedBuildingId)}
                                                                        className="border-muted/30 hover:bg-muted/10 text-xs"
                                                                    >
                                                                        View Site Ledger
                                                                    </Button>
                                                                </TableCell>
                                                            </TableRow>
                                                        ))}
                                                    </TableBody>
                                                </Table>
                                            </div>
                                        )}
                                    </CardContent>
                                </Card>
                            </>
                        )}
                    </>
                ) : (
                    <>
                        {/* FULL-PAGE CONTRACTOR LEDGER STATEMENT */}
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-muted/15 pb-4">
                            <div className="flex items-center gap-3">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={handleCloseLedger}
                                    className="border-muted/30 hover:bg-muted/10 h-9 px-3 gap-1.5"
                                >
                                    <ArrowLeft className="h-4 w-4" />
                                    <span>Back to Accounts</span>
                                </Button>
                                <div>
                                    <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
                                        <span>{selectedAccount.contractor.name}</span>
                                        <Badge variant="secondary" className="text-xs bg-muted/40 border-muted-foreground/10 text-foreground font-normal">
                                            {selectedAccount.contractor.category || 'General Contractor'}
                                        </Badge>
                                    </h1>
                                    <p className="text-xs text-muted-foreground mt-0.5">
                                        Detailed statement of Rate contracts & Nominal Muster Roll wages.
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Profile Info Card */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-4 bg-muted/10 border border-muted/20 rounded-xl text-xs">
                            <div className="flex flex-col gap-0.5">
                                <span className="text-muted-foreground font-medium uppercase text-[9px] tracking-wider">Contact Phone</span>
                                <span className="font-semibold text-foreground flex items-center gap-1">
                                    <Phone className="h-3 w-3 text-primary" /> {selectedAccount.contractor.phone || 'N/A'}
                                </span>
                            </div>
                            <div className="flex flex-col gap-0.5">
                                <span className="text-muted-foreground font-medium uppercase text-[9px] tracking-wider">Email Address</span>
                                <span className="font-semibold text-foreground truncate flex items-center gap-1">
                                    <Mail className="h-3 w-3 text-primary" /> {selectedAccount.contractor.email || 'N/A'}
                                </span>
                            </div>
                            <div className="flex flex-col gap-0.5">
                                <span className="text-muted-foreground font-medium uppercase text-[9px] tracking-wider">Category</span>
                                <span className="font-semibold text-foreground">
                                    {selectedAccount.contractor.category || 'General'}
                                </span>
                            </div>
                            <div className="flex flex-col gap-0.5">
                                <span className="text-muted-foreground font-medium uppercase text-[9px] tracking-wider">Bank Details</span>
                                <span className="font-semibold text-foreground italic text-[10px]">
                                    Linked in Salary Profile
                                </span>
                            </div>
                        </div>

                        {/* Building Filter inside Full Page View */}
                        {projects.length > 0 && (
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3 bg-muted/5 border border-muted/15 rounded-xl">
                                <span className="text-xs text-muted-foreground font-medium">Filter statement entries by building / site:</span>
                                <Select 
                                    value={ledgerProjectFilter} 
                                    onValueChange={(val) => {
                                        setLedgerProjectFilter(val)
                                        setExpandedTxId(null)
                                    }}
                                >
                                    <SelectTrigger className="w-full sm:w-[220px] bg-background/50 border-muted/30 h-9 text-xs">
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

                        {/* Tab view for sub accounts */}
                        <Tabs defaultValue="combined" className="w-full mt-2">
                            <TabsList className="grid w-full grid-cols-3 bg-muted/20 border border-muted/10 p-1 rounded-xl">
                                <TabsTrigger value="combined" className="text-xs rounded-lg py-1.5 font-medium">Combined Ledger</TabsTrigger>
                                <TabsTrigger value="rate" className="text-xs rounded-lg py-1.5 font-medium">Rate Account (Sq.Ft)</TabsTrigger>
                                <TabsTrigger value="nmr" className="text-xs rounded-lg py-1.5 font-medium">NMR Account (Labor)</TabsTrigger>
                            </TabsList>

                            {/* 1. Combined tab */}
                            <TabsContent value="combined" className="space-y-4 mt-4">
                                <div className="flex justify-between items-center bg-muted/5 border border-muted/15 p-4 rounded-xl">
                                    <div className="text-xs">
                                        <span className="text-muted-foreground">Total Paid: </span>
                                        <span className="font-bold text-foreground text-sm">₹{totalPaidSum.toLocaleString('en-IN')}</span>
                                    </div>
                                    <div className="text-xs">
                                        <span className="text-muted-foreground">Total Outstanding: </span>
                                        <span className="font-bold text-amber-400 text-sm">₹{totalPendingSum.toLocaleString('en-IN')}</span>
                                    </div>
                                </div>

                                <Card className="glass-card border-muted/20 overflow-hidden bg-background/25">
                                    <Table>
                                        <TableHeader className="bg-muted/10">
                                            <TableRow className="border-muted/20">
                                                <TableHead className="text-xs w-[140px]">Date</TableHead>
                                                <TableHead className="text-xs w-[180px]">Classification</TableHead>
                                                <TableHead className="text-xs">Details</TableHead>
                                                <TableHead className="text-xs text-right w-[150px]">Amount</TableHead>
                                                <TableHead className="text-xs text-right w-[120px]">Status</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {filteredAllTransactions.length === 0 ? (
                                                <TableRow>
                                                    <TableCell colSpan={5} className="text-center py-12 text-muted-foreground text-xs">
                                                        No transactions registered for this selection.
                                                    </TableCell>
                                                </TableRow>
                                            ) : (
                                                filteredAllTransactions.map((tx) => (
                                                    <>
                                                        <TableRow 
                                                            key={tx.id} 
                                                            className="border-muted/15 hover:bg-muted/5 text-xs cursor-pointer select-none"
                                                            onClick={() => setExpandedTxId(expandedTxId === tx.id ? null : tx.id)}
                                                        >
                                                            <TableCell className="font-medium flex items-center gap-1.5 py-4">
                                                                {expandedTxId === tx.id ? (
                                                                    <ChevronUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                ) : (
                                                                    <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                )}
                                                                {tx.status === 'paid' ? (
                                                                    <span>
                                                                        {(() => {
                                                                            const d = new Date(tx.paid_at || tx.created_at);
                                                                            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                        })()}
                                                                    </span>
                                                                ) : (
                                                                    <span className="text-muted-foreground italic font-normal">Pending</span>
                                                                )}
                                                            </TableCell>
                                                            <TableCell>
                                                                {getPayoutClassBadge(tx.payout_class)}
                                                            </TableCell>
                                                            <TableCell className="max-w-[200px] truncate text-muted-foreground">
                                                                {tx.reference_details?.startsWith('{') 
                                                                    ? 'Daily wage logs aggregated' 
                                                                    : tx.reference_details || 'Manual payout entry'}
                                                            </TableCell>
                                                            <TableCell className="text-right font-mono font-bold">
                                                                ₹{tx.amount_paid.toLocaleString('en-IN')}
                                                            </TableCell>
                                                            <TableCell className="text-right">
                                                                {getStatusBadge(tx.status)}
                                                            </TableCell>
                                                        </TableRow>
                                                        {expandedTxId === tx.id && (
                                                            <TableRow className="bg-muted/10 hover:bg-muted/10 border-none">
                                                                <TableCell colSpan={5} className="p-4">
                                                                    <div className="bg-background/80 border border-muted/20 rounded-xl p-4 space-y-4 shadow-sm text-xs">
                                                                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Building / Site</span>
                                                                                <span className="font-semibold text-foreground text-sm">{tx.project?.name || 'Manual Settlement / Not Linked'}</span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Pay-Run Week</span>
                                                                                <span className="font-semibold text-foreground text-sm">
                                                                                    {tx.payout ? `${tx.payout.week_start_date} to ${tx.payout.week_end_date}` : 'N/A'}
                                                                                </span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Settlement status</span>
                                                                                <span className="font-semibold text-foreground flex items-center gap-1.5 mt-0.5">
                                                                                    {getStatusBadge(tx.status)}
                                                                                    {tx.status === 'paid' && tx.paid_at && (
                                                                                        <span className="text-[10px] text-muted-foreground font-normal">
                                                                                            on {new Date(tx.paid_at).toLocaleDateString('en-CA')}
                                                                                        </span>
                                                                                    )}
                                                                                </span>
                                                                            </div>
                                                                        </div>

                                                                        {tx.notes && (
                                                                            <div className="border-t border-muted/10 pt-3">
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Notes / Remarks</span>
                                                                                <p className="text-foreground/90 italic mt-0.5 text-xs">{tx.notes}</p>
                                                                            </div>
                                                                        )}

                                                                        {tx.reference_details && tx.reference_details.startsWith('{') && (() => {
                                                                            try {
                                                                                const parsed = JSON.parse(tx.reference_details);
                                                                                if (parsed.breakdown && parsed.breakdown.length > 0) {
                                                                                    return (
                                                                                        <div className="border-t border-muted/10 pt-3 space-y-2">
                                                                                            <span className="text-muted-foreground block font-semibold uppercase text-[9px] tracking-wider mb-1">
                                                                                                Wages Breakdown ({parsed.breakdown.length} worker categories)
                                                                                            </span>
                                                                                            <div className="border border-muted/10 rounded-lg overflow-hidden bg-background/50">
                                                                                                <Table>
                                                                                                    <TableHeader className="bg-muted/15">
                                                                                                        <TableRow className="h-8 border-muted/10">
                                                                                                            <TableHead className="text-[10px] py-1">Worker Category</TableHead>
                                                                                                            <TableHead className="text-[10px] text-center py-1">Man-Days</TableHead>
                                                                                                            <TableHead className="text-[10px] text-right py-1">Daily Rate</TableHead>
                                                                                                            <TableHead className="text-[10px] text-right py-1">Subtotal</TableHead>
                                                                                                        </TableRow>
                                                                                                    </TableHeader>
                                                                                                    <TableBody>
                                                                                                        {parsed.breakdown.map((row: any, i: number) => (
                                                                                                            <TableRow key={i} className="h-8 border-muted/10 hover:bg-transparent">
                                                                                                                <TableCell className="py-1 text-[11px] font-medium text-foreground/80">{row.category}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-center text-foreground/80">{row.days}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-right text-foreground/80">₹{row.rate.toLocaleString('en-IN')}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-right font-semibold text-foreground/90">₹{row.amount.toLocaleString('en-IN')}</TableCell>
                                                                                                            </TableRow>
                                                                                                        ))}
                                                                                                    </TableBody>
                                                                                                </Table>
                                                                                            </div>
                                                                                        </div>
                                                                                    );
                                                                                }
                                                                            } catch (e) {}
                                                                            return null;
                                                                        })()}

                                                                        {tx.payout_id && (
                                                                            <div className="flex justify-end border-t border-muted/10 pt-3">
                                                                                <Button
                                                                                    size="sm"
                                                                                    variant="secondary"
                                                                                    onClick={() => {
                                                                                        router.push(`/financials/payday?payoutId=${tx.payout_id}`);
                                                                                    }}
                                                                                    className="bg-muted/40 hover:bg-muted/60 text-[11px] h-8 px-3 gap-1.5"
                                                                                >
                                                                                    <span>Go to Pay-Run Details</span>
                                                                                    <ArrowLeft className="h-3.5 w-3.5 rotate-180" />
                                                                                </Button>
                                                                            </div>
                                                                        )}
                                                                    </div>
                                                                </TableCell>
                                                            </TableRow>
                                                        )}
                                                    </>
                                                ))
                                            )}
                                        </TableBody>
                                    </Table>
                                </Card>
                            </TabsContent>

                            {/* 2. Rate account tab */}
                            <TabsContent value="rate" className="space-y-4 mt-4">
                                <div className="grid grid-cols-3 gap-3 text-center p-4 bg-purple-500/5 border border-purple-500/10 rounded-xl text-xs">
                                    <div>
                                        <div className="text-muted-foreground text-[10px]">TOTAL EARNED</div>
                                        <div className="font-bold text-foreground mt-1 text-sm">₹{rateTotalDueSum.toLocaleString('en-IN')}</div>
                                    </div>
                                    <div>
                                        <div className="text-emerald-400 text-[10px]">TOTAL RECEIVED</div>
                                        <div className="font-bold text-emerald-400 mt-1 text-sm">₹{rateTotalPaidSum.toLocaleString('en-IN')}</div>
                                    </div>
                                    <div>
                                        <div className="text-purple-400 text-[10px]">OUTSTANDING</div>
                                        <div className="font-bold text-purple-400 mt-1 text-sm font-mono">₹{rateTotalPendingSum.toLocaleString('en-IN')}</div>
                                    </div>
                                </div>

                                <Card className="glass-card border-muted/20 overflow-hidden bg-background/25">
                                    <Table>
                                        <TableHeader className="bg-muted/10">
                                            <TableRow className="border-muted/20">
                                                <TableHead className="text-xs w-[140px]">Date</TableHead>
                                                <TableHead className="text-xs">Reference</TableHead>
                                                <TableHead className="text-xs text-right w-[150px]">Settled Amount</TableHead>
                                                <TableHead className="text-xs text-right w-[120px]">Status</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {filteredRateItems.length === 0 ? (
                                                <TableRow>
                                                    <TableCell colSpan={4} className="text-center py-12 text-muted-foreground text-xs">
                                                        No square-foot Rate contract items logged for this selection.
                                                    </TableCell>
                                                </TableRow>
                                            ) : (
                                                filteredRateItems.map((tx) => (
                                                    <>
                                                        <TableRow 
                                                            key={tx.id} 
                                                            className="border-muted/15 hover:bg-muted/5 text-xs cursor-pointer select-none"
                                                            onClick={() => setExpandedTxId(expandedTxId === tx.id ? null : tx.id)}
                                                        >
                                                            <TableCell className="font-medium flex items-center gap-1.5 py-4">
                                                                {expandedTxId === tx.id ? (
                                                                    <ChevronUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                ) : (
                                                                    <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                )}
                                                                {tx.status === 'paid' ? (
                                                                    <span>
                                                                        {(() => {
                                                                            const d = new Date(tx.paid_at || tx.created_at);
                                                                            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                        })()}
                                                                    </span>
                                                                ) : (
                                                                    <span className="text-muted-foreground italic font-normal">Pending</span>
                                                                )}
                                                            </TableCell>
                                                            <TableCell className="text-muted-foreground truncate">
                                                                {tx.reference_details || tx.notes || 'Rate Contract payout'}
                                                            </TableCell>
                                                            <TableCell className="text-right font-mono font-bold text-purple-300">
                                                                ₹{tx.amount_paid.toLocaleString('en-IN')}
                                                            </TableCell>
                                                            <TableCell className="text-right">
                                                                {getStatusBadge(tx.status)}
                                                            </TableCell>
                                                        </TableRow>
                                                        {expandedTxId === tx.id && (
                                                            <TableRow className="bg-muted/10 hover:bg-muted/10 border-none">
                                                                <TableCell colSpan={4} className="p-4">
                                                                    <div className="bg-background/80 border border-muted/20 rounded-xl p-4 space-y-3.5 shadow-sm text-xs">
                                                                        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Building / Site</span>
                                                                                <span className="font-semibold text-foreground text-sm">{tx.project?.name || 'Manual Settlement / Not Linked'}</span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Pay-Run Week</span>
                                                                                <span className="font-semibold text-foreground text-sm">
                                                                                    {tx.payout ? `${tx.payout.week_start_date} to ${tx.payout.week_end_date}` : 'N/A'}
                                                                                </span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Settlement status</span>
                                                                                <span className="font-semibold text-foreground flex items-center gap-1.5 mt-0.5">
                                                                                    {getStatusBadge(tx.status)}
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                        {tx.notes && (
                                                                            <div className="border-t border-muted/10 pt-2">
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Notes / Remarks</span>
                                                                                <p className="text-foreground/90 italic mt-0.5 text-xs">{tx.notes}</p>
                                                                            </div>
                                                                        )}
                                                                        {tx.payout_id && (
                                                                            <div className="flex justify-end border-t border-muted/10 pt-2.5">
                                                                                <Button
                                                                                    size="sm"
                                                                                    variant="secondary"
                                                                                    onClick={() => {
                                                                                        router.push(`/financials/payday?payoutId=${tx.payout_id}`);
                                                                                    }}
                                                                                    className="bg-muted/40 hover:bg-muted/60 text-[11px] h-8 px-3 gap-1.5"
                                                                                >
                                                                                    <span>Go to Pay-Run Details</span>
                                                                                    <ArrowLeft className="h-3.5 w-3.5 rotate-180" />
                                                                                </Button>
                                                                            </div>
                                                                        )}
                                                                    </div>
                                                                </TableCell>
                                                            </TableRow>
                                                        )}
                                                    </>
                                                ))
                                            )}
                                        </TableBody>
                                    </Table>
                                </Card>
                            </TabsContent>

                            {/* 3. NMR account tab */}
                            <TabsContent value="nmr" className="space-y-4 mt-4">
                                <div className="grid grid-cols-3 gap-3 text-center p-4 bg-teal-500/5 border border-teal-500/10 rounded-xl text-xs">
                                    <div>
                                        <div className="text-muted-foreground text-[10px]">TOTAL NMR WAGES</div>
                                        <div className="font-bold text-foreground mt-1 text-sm">₹{nmrTotalDueSum.toLocaleString('en-IN')}</div>
                                    </div>
                                    <div>
                                        <div className="text-emerald-400 text-[10px]">TOTAL NMR PAID</div>
                                        <div className="font-bold text-emerald-400 mt-1 text-sm">₹{nmrTotalPaidSum.toLocaleString('en-IN')}</div>
                                    </div>
                                    <div>
                                        <div className="text-teal-400 text-[10px]">OUTSTANDING</div>
                                        <div className="font-bold text-teal-400 mt-1 text-sm font-mono">₹{nmrTotalPendingSum.toLocaleString('en-IN')}</div>
                                    </div>
                                </div>

                                <Card className="glass-card border-muted/20 overflow-hidden bg-background/25">
                                    <Table>
                                        <TableHeader className="bg-muted/10">
                                            <TableRow className="border-muted/20">
                                                <TableHead className="text-xs w-[140px]">Date</TableHead>
                                                <TableHead className="text-xs">Reference</TableHead>
                                                <TableHead className="text-xs text-right w-[150px]">Paid Amount</TableHead>
                                                <TableHead className="text-xs text-right w-[120px]">Status</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {filteredNmrItems.length === 0 ? (
                                                <TableRow>
                                                    <TableCell colSpan={4} className="text-center py-12 text-muted-foreground text-xs">
                                                        No Nominal Muster Roll daily labor wage items logged for this selection.
                                                    </TableCell>
                                                </TableRow>
                                            ) : (
                                                filteredNmrItems.map((tx) => (
                                                    <>
                                                        <TableRow 
                                                            key={tx.id} 
                                                            className="border-muted/15 hover:bg-muted/5 text-xs cursor-pointer select-none"
                                                            onClick={() => setExpandedTxId(expandedTxId === tx.id ? null : tx.id)}
                                                        >
                                                            <TableCell className="font-medium flex items-center gap-1.5 py-4">
                                                                {expandedTxId === tx.id ? (
                                                                    <ChevronUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                ) : (
                                                                    <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                                                )}
                                                                {tx.status === 'paid' ? (
                                                                    <span>
                                                                        {(() => {
                                                                            const d = new Date(tx.paid_at || tx.created_at);
                                                                            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                        })()}
                                                                    </span>
                                                                ) : (
                                                                    <span className="text-muted-foreground italic font-normal">Pending</span>
                                                                )}
                                                            </TableCell>
                                                            <TableCell className="text-muted-foreground truncate">
                                                                {tx.reference_details?.startsWith('{') 
                                                                    ? 'Wages auto-compiled from worklogs' 
                                                                    : tx.reference_details || 'Daily wage payout'}
                                                            </TableCell>
                                                            <TableCell className="text-right font-mono font-bold text-teal-300">
                                                                ₹{tx.amount_paid.toLocaleString('en-IN')}
                                                            </TableCell>
                                                            <TableCell className="text-right">
                                                                {getStatusBadge(tx.status)}
                                                            </TableCell>
                                                        </TableRow>
                                                        {expandedTxId === tx.id && (
                                                            <TableRow className="bg-muted/10 hover:bg-muted/10 border-none">
                                                                <TableCell colSpan={4} className="p-4">
                                                                    <div className="bg-background/80 border border-muted/20 rounded-xl p-4 space-y-3.5 shadow-sm text-xs">
                                                                        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Building / Site</span>
                                                                                <span className="font-semibold text-foreground text-sm">{tx.project?.name || 'Manual Settlement / Not Linked'}</span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Pay-Run Week</span>
                                                                                <span className="font-semibold text-foreground text-sm">
                                                                                    {tx.payout ? `${tx.payout.week_start_date} to ${tx.payout.week_end_date}` : 'N/A'}
                                                                                </span>
                                                                            </div>
                                                                            <div>
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Settlement status</span>
                                                                                <span className="font-semibold text-foreground flex items-center gap-1.5 mt-0.5">
                                                                                    {getStatusBadge(tx.status)}
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                        {tx.notes && (
                                                                            <div className="border-t border-muted/10 pt-2">
                                                                                <span className="text-muted-foreground block font-medium uppercase text-[9px] tracking-wider">Notes / Remarks</span>
                                                                                <p className="text-foreground/90 italic mt-0.5 text-xs">{tx.notes}</p>
                                                                            </div>
                                                                        )}
                                                                        {tx.reference_details && tx.reference_details.startsWith('{') && (() => {
                                                                            try {
                                                                                const parsed = JSON.parse(tx.reference_details);
                                                                                if (parsed.breakdown && parsed.breakdown.length > 0) {
                                                                                    return (
                                                                                        <div className="border-t border-muted/10 pt-3 space-y-1.5">
                                                                                            <span className="text-muted-foreground block font-semibold uppercase text-[9px] tracking-wider mb-1">
                                                                                                Wages Breakdown ({parsed.breakdown.length} worker categories)
                                                                                            </span>
                                                                                            <div className="border border-muted/10 rounded-lg overflow-hidden bg-background/50">
                                                                                                <Table>
                                                                                                    <TableHeader className="bg-muted/15">
                                                                                                        <TableRow className="h-8 border-muted/10">
                                                                                                            <TableHead className="text-[10px] py-1">Worker Category</TableHead>
                                                                                                            <TableHead className="text-[10px] text-center py-1">Man-Days</TableHead>
                                                                                                            <TableHead className="text-[10px] text-right py-1">Daily Rate</TableHead>
                                                                                                            <TableHead className="text-[10px] text-right py-1">Subtotal</TableHead>
                                                                                                        </TableRow>
                                                                                                    </TableHeader>
                                                                                                    <TableBody>
                                                                                                        {parsed.breakdown.map((row: any, i: number) => (
                                                                                                            <TableRow key={i} className="h-8 border-muted/10 hover:bg-transparent">
                                                                                                                <TableCell className="py-1 text-[11px] font-medium text-foreground/80">{row.category}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-center text-foreground/80">{row.days}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-right text-foreground/80">₹{row.rate.toLocaleString('en-IN')}</TableCell>
                                                                                                                <TableCell className="py-1 text-[11px] text-right font-semibold text-foreground/90">₹{row.amount.toLocaleString('en-IN')}</TableCell>
                                                                                                            </TableRow>
                                                                                                        ))}
                                                                                                    </TableBody>
                                                                                                </Table>
                                                                                            </div>
                                                                                        </div>
                                                                                    );
                                                                                }
                                                                            } catch (e) {}
                                                                            return null;
                                                                        })()}
                                                                        {tx.payout_id && (
                                                                            <div className="flex justify-end border-t border-muted/10 pt-2.5">
                                                                                <Button
                                                                                    size="sm"
                                                                                    variant="secondary"
                                                                                    onClick={() => {
                                                                                        router.push(`/financials/payday?payoutId=${tx.payout_id}`);
                                                                                    }}
                                                                                    className="bg-muted/40 hover:bg-muted/60 text-[11px] h-8 px-3 gap-1.5"
                                                                                >
                                                                                    <span>Go to Pay-Run Details</span>
                                                                                    <ArrowLeft className="h-3.5 w-3.5 rotate-180" />
                                                                                </Button>
                                                                            </div>
                                                                        )}
                                                                    </div>
                                                                </TableCell>
                                                            </TableRow>
                                                        )}
                                                    </>
                                                ))
                                            )}
                                        </TableBody>
                                    </Table>
                                </Card>
                            </TabsContent>
                        </Tabs>
                    </>
                )}
            </div>
        </div>
    )
}
