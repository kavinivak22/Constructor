'use client'

import { useEffect, useState } from 'react'
import { getContractorAccounts } from '@/app/actions/contractors'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
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
    AlertCircle
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
    const [accounts, setAccounts] = useState<ContractorAccount[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [searchQuery, setSearchQuery] = useState('')
    
    // Detailed ledger dialog state
    const [selectedAccount, setSelectedAccount] = useState<ContractorAccount | null>(null)
    const [isLedgerOpen, setIsLedgerOpen] = useState(false)

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

    useEffect(() => {
        loadAccounts()
    }, [])

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

    const handleOpenLedger = (account: ContractorAccount) => {
        setSelectedAccount(account)
        setIsLedgerOpen(true)
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

    return (
        <div className="flex-1 p-4 overflow-y-auto md:p-8 bg-transparent">
            <div className="max-w-6xl mx-auto space-y-6">
                
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

                {/* Summary Widgets */}
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

                {/* Dialog: Contractor Ledger Statement */}
                <Dialog open={isLedgerOpen} onOpenChange={setIsLedgerOpen}>
                    <DialogContent className="sm:max-w-[700px] border-muted/30 text-foreground bg-background/95 backdrop-blur-xl max-h-[85vh] overflow-y-auto">
                        {selectedAccount && (
                            <>
                                <DialogHeader>
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-xl flex items-center justify-center">
                                            <Building2 className="h-5 w-5" />
                                        </div>
                                        <div>
                                            <DialogTitle className="text-xl font-bold font-headline">
                                                {selectedAccount.contractor.name}
                                            </DialogTitle>
                                            <DialogDescription className="text-xs mt-0.5">
                                                Contractor Account statement for Rate Contract & NMR Ledger balances.
                                            </DialogDescription>
                                        </div>
                                    </div>
                                </DialogHeader>

                                {/* Profile info bar */}
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-3 bg-muted/15 border border-muted/20 rounded-xl text-xs">
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

                                {/* Tab view for sub accounts */}
                                <Tabs defaultValue="combined" className="w-full mt-4">
                                    <TabsList className="grid w-full grid-cols-3 bg-muted/20 border border-muted/10 p-1 rounded-xl">
                                        <TabsTrigger value="combined" className="text-xs rounded-lg py-1.5 font-medium">Combined Ledger</TabsTrigger>
                                        <TabsTrigger value="rate" className="text-xs rounded-lg py-1.5 font-medium">Rate Account (Sq.Ft)</TabsTrigger>
                                        <TabsTrigger value="nmr" className="text-xs rounded-lg py-1.5 font-medium">NMR Account (Labor)</TabsTrigger>
                                    </TabsList>

                                    {/* 1. Combined tab */}
                                    <TabsContent value="combined" className="space-y-4 mt-4">
                                        <div className="flex justify-between items-center bg-muted/5 border border-muted/15 p-3 rounded-lg">
                                            <div className="text-xs">
                                                <span className="text-muted-foreground">Total Paid: </span>
                                                <span className="font-bold text-foreground">₹{(selectedAccount.rateAccount.totalPaid + selectedAccount.nmrAccount.totalPaid).toLocaleString('en-IN')}</span>
                                            </div>
                                            <div className="text-xs">
                                                <span className="text-muted-foreground">Total Outstanding: </span>
                                                <span className="font-bold text-amber-400">₹{(selectedAccount.rateAccount.totalPending + selectedAccount.nmrAccount.totalPending).toLocaleString('en-IN')}</span>
                                            </div>
                                        </div>

                                        <div className="border border-muted/20 rounded-lg overflow-hidden max-h-[300px] overflow-y-auto bg-background/20">
                                            <Table>
                                                <TableHeader className="bg-muted/10">
                                                    <TableRow className="border-muted/20">
                                                        <TableHead className="text-xs">Date</TableHead>
                                                        <TableHead className="text-xs">Classification</TableHead>
                                                        <TableHead className="text-xs">Details</TableHead>
                                                        <TableHead className="text-xs text-right">Amount</TableHead>
                                                        <TableHead className="text-xs text-right">Status</TableHead>
                                                    </TableRow>
                                                </TableHeader>
                                                <TableBody>
                                                    {selectedAccount.allTransactions.length === 0 ? (
                                                        <TableRow>
                                                            <TableCell colSpan={5} className="text-center py-8 text-muted-foreground text-xs">
                                                                No transactions registered for this account.
                                                            </TableCell>
                                                        </TableRow>
                                                    ) : (
                                                        selectedAccount.allTransactions.map((tx) => (
                                                            <TableRow key={tx.id} className="border-muted/15 hover:bg-muted/5 text-xs">
                                                                <TableCell className="font-medium">
                                                                    {tx.status === 'paid' ? (
                                                                        <span>
                                                                            {(() => {
                                                                                const d = new Date(tx.paid_at || tx.created_at);
                                                                                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                            })()}
                                                                        </span>
                                                                    ) : (
                                                                        <span className="text-muted-foreground italic font-normal">Pending Settlement</span>
                                                                    )}
                                                                </TableCell>
                                                                <TableCell>
                                                                    {getPayoutClassBadge(tx.payout_class)}
                                                                </TableCell>
                                                                <TableCell className="max-w-[150px] truncate text-muted-foreground">
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
                                                        ))
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </div>
                                    </TabsContent>

                                    {/* 2. Rate account tab */}
                                    <TabsContent value="rate" className="space-y-4 mt-4">
                                        <div className="grid grid-cols-3 gap-2 text-center p-3 bg-purple-500/5 border border-purple-500/10 rounded-lg text-xs">
                                            <div>
                                                <div className="text-muted-foreground text-[10px]">TOTAL EARNED</div>
                                                <div className="font-bold text-foreground mt-0.5">₹{selectedAccount.rateAccount.totalDue.toLocaleString('en-IN')}</div>
                                            </div>
                                            <div>
                                                <div className="text-emerald-400 text-[10px]">TOTAL RECEIVED</div>
                                                <div className="font-bold text-emerald-400 mt-0.5">₹{selectedAccount.rateAccount.totalPaid.toLocaleString('en-IN')}</div>
                                            </div>
                                            <div>
                                                <div className="text-purple-400 text-[10px]">OUTSTANDING</div>
                                                <div className="font-bold text-purple-400 mt-0.5 font-mono">₹{selectedAccount.rateAccount.totalPending.toLocaleString('en-IN')}</div>
                                            </div>
                                        </div>

                                        <div className="border border-muted/20 rounded-lg overflow-hidden max-h-[300px] overflow-y-auto">
                                            <Table>
                                                <TableHeader className="bg-muted/10">
                                                    <TableRow className="border-muted/20">
                                                        <TableHead className="text-xs">Date</TableHead>
                                                        <TableHead className="text-xs">Reference</TableHead>
                                                        <TableHead className="text-xs text-right">Settled Amount</TableHead>
                                                        <TableHead className="text-xs text-right">Status</TableHead>
                                                    </TableRow>
                                                </TableHeader>
                                                <TableBody>
                                                    {selectedAccount.rateAccount.items.length === 0 ? (
                                                        <TableRow>
                                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                                                                No square-foot Rate contract items logged for this contractor.
                                                            </TableCell>
                                                        </TableRow>
                                                    ) : (
                                                        selectedAccount.rateAccount.items.map((tx) => (
                                                            <TableRow key={tx.id} className="border-muted/15 hover:bg-muted/5 text-xs">
                                                                <TableCell className="font-medium">
                                                                    {tx.status === 'paid' ? (
                                                                        <span>
                                                                            {(() => {
                                                                                const d = new Date(tx.paid_at || tx.created_at);
                                                                                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                            })()}
                                                                        </span>
                                                                    ) : (
                                                                        <span className="text-muted-foreground italic font-normal">Pending Settlement</span>
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
                                                        ))
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </div>
                                    </TabsContent>

                                    {/* 3. NMR account tab */}
                                    <TabsContent value="nmr" className="space-y-4 mt-4">
                                        <div className="grid grid-cols-3 gap-2 text-center p-3 bg-teal-500/5 border border-teal-500/10 rounded-lg text-xs">
                                            <div>
                                                <div className="text-muted-foreground text-[10px]">TOTAL NMR WAGES</div>
                                                <div className="font-bold text-foreground mt-0.5">₹{selectedAccount.nmrAccount.totalDue.toLocaleString('en-IN')}</div>
                                            </div>
                                            <div>
                                                <div className="text-emerald-400 text-[10px]">TOTAL NMR PAID</div>
                                                <div className="font-bold text-emerald-400 mt-0.5">₹{selectedAccount.nmrAccount.totalPaid.toLocaleString('en-IN')}</div>
                                            </div>
                                            <div>
                                                <div className="text-teal-400 text-[10px]">OUTSTANDING</div>
                                                <div className="font-bold text-teal-400 mt-0.5 font-mono">₹{selectedAccount.nmrAccount.totalPending.toLocaleString('en-IN')}</div>
                                            </div>
                                        </div>

                                        <div className="border border-muted/20 rounded-lg overflow-hidden max-h-[300px] overflow-y-auto">
                                            <Table>
                                                <TableHeader className="bg-muted/10">
                                                    <TableRow className="border-muted/20">
                                                        <TableHead className="text-xs">Date</TableHead>
                                                        <TableHead className="text-xs">Reference</TableHead>
                                                        <TableHead className="text-xs text-right">Paid Amount</TableHead>
                                                        <TableHead className="text-xs text-right">Status</TableHead>
                                                    </TableRow>
                                                </TableHeader>
                                                <TableBody>
                                                    {selectedAccount.nmrAccount.items.length === 0 ? (
                                                        <TableRow>
                                                            <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                                                                No Nominal Muster Roll daily labor wage items logged for this contractor.
                                                            </TableCell>
                                                        </TableRow>
                                                    ) : (
                                                        selectedAccount.nmrAccount.items.map((tx) => (
                                                            <TableRow key={tx.id} className="border-muted/15 hover:bg-muted/5 text-xs">
                                                                <TableCell className="font-medium">
                                                                    {tx.status === 'paid' ? (
                                                                        <span>
                                                                            {(() => {
                                                                                const d = new Date(tx.paid_at || tx.created_at);
                                                                                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                                                                            })()}
                                                                        </span>
                                                                    ) : (
                                                                        <span className="text-muted-foreground italic font-normal">Pending Settlement</span>
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
                                                        ))
                                                    )}
                                                </TableBody>
                                            </Table>
                                        </div>
                                    </TabsContent>
                                </Tabs>

                                <DialogFooter className="pt-4 border-t border-muted/10">
                                    <Button type="button" onClick={() => setIsLedgerOpen(false)} className="bg-primary hover:opacity-90">
                                        Close Statement
                                    </Button>
                                </DialogFooter>
                            </>
                        )}
                    </DialogContent>
                </Dialog>

            </div>
        </div>
    )
}
