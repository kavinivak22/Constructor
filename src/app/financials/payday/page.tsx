'use client'

import { useEffect, useState } from 'react'
import {
    getWeeklyPayouts,
    getPayoutItems,
    createWeeklyPayoutRun,
    updatePayoutItem,
    createCustomPayoutItem,
    processWeeklyPayout,
    deleteWeeklyPayout,
    getProjects
} from '@/app/actions/financials'
import { useToast } from '@/hooks/use-toast'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import {
    Loader2,
    Plus,
    Search,
    Trash2,
    ArrowLeft,
    Check,
    X,
    Calendar,
    DollarSign,
    Layers,
    FileSpreadsheet,
    ChevronRight,
    Play,
    AlertCircle,
    User,
    Store,
    Activity,
    CreditCard
} from 'lucide-react'

interface PayoutRun {
    id: string
    week_start_date: string
    week_end_date: string
    status: 'draft' | 'approved' | 'paid'
    total_amount: number
    created_by: string
    created_at: string
    creator?: {
        display_name: string
    }
}

interface PayoutItem {
    id: string
    payout_id: string
    recipient_type: 'employee_salary' | 'labor_wage' | 'vendor_payment' | 'other'
    recipient_id: string | null
    recipient_name: string
    amount_due: number
    amount_paid: number
    status: 'pending' | 'paid' | 'held'
    project_id: string | null
    reference_details: string | null
    notes: string | null
    created_at: string
    project?: {
        name: string
    } | null
}

interface Project {
    id: string
    name: string
}

export default function PaydayPage() {
    const { toast } = useToast()
    const [runs, setRuns] = useState<PayoutRun[]>([])
    const [selectedRun, setSelectedRun] = useState<PayoutRun | null>(null)
    const [items, setItems] = useState<PayoutItem[]>([])
    const [projects, setProjects] = useState<Project[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isActionLoading, setIsActionLoading] = useState(false)
    const [itemSearch, setItemSearch] = useState('')
    const [typeFilter, setTypeFilter] = useState('all')

    // Dialog: Generate Run
    const [isGenOpen, setIsGenOpen] = useState(false)
    const [weekStart, setWeekStart] = useState('')
    const [weekEnd, setWeekEnd] = useState('')

    // Dialog: Custom Item
    const [isCustomOpen, setIsCustomOpen] = useState(false)
    const [customName, setCustomName] = useState('')
    const [customType, setCustomType] = useState<'employee_salary' | 'labor_wage' | 'vendor_payment' | 'other'>('other')
    const [customAmount, setCustomAmount] = useState('')
    const [customProjectId, setCustomProjectId] = useState<string>('none')
    const [customRef, setCustomRef] = useState('')
    const [customNotes, setCustomNotes] = useState('')

    // Inline edit row state
    const [editingItemId, setEditingItemId] = useState<string | null>(null)
    const [editAmountPaid, setEditAmountPaid] = useState('')
    const [editStatus, setEditStatus] = useState<'pending' | 'paid' | 'held'>('pending')
    const [editNotes, setEditNotes] = useState('')

    const loadRuns = async () => {
        setIsLoading(true)
        try {
            const data = await getWeeklyPayouts()
            setRuns(data as PayoutRun[])
        } catch (error) {
            console.error('Error loading payout runs:', error)
            toast({
                title: 'Error',
                description: 'Failed to load weekly runs.',
                variant: 'destructive'
            })
        } finally {
            setIsLoading(false)
        }
    }

    const loadProjectsList = async () => {
        try {
            const data = await getProjects()
            setProjects(data as Project[])
        } catch (error) {
            console.error('Error loading projects list:', error)
        }
    }

    useEffect(() => {
        loadRuns()
        loadProjectsList()
    }, [])

    const handleSelectRun = async (run: PayoutRun) => {
        setIsLoading(true)
        setSelectedRun(run)
        try {
            const itemsData = await getPayoutItems(run.id)
            setItems(itemsData as PayoutItem[])
        } catch (error) {
            console.error('Error loading payout items:', error)
            toast({
                title: 'Error',
                description: 'Failed to load details for this weekly run.',
                variant: 'destructive'
            })
        } finally {
            setIsLoading(false)
        }
    }

    const handleBackToList = () => {
        setSelectedRun(null)
        setItems([])
        loadRuns()
    }

    const handleGenerateRun = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!weekStart || !weekEnd) {
            toast({
                title: 'Error',
                description: 'Please select both start and end dates.',
                variant: 'destructive'
            })
            return
        }

        setIsActionLoading(true)
        try {
            const res = await createWeeklyPayoutRun(weekStart, weekEnd)
            if (res.success) {
                toast({
                    title: 'Success',
                    description: 'Weekly payout run successfully generated!'
                })
                setIsGenOpen(false)
                setWeekStart('')
                setWeekEnd('')
                loadRuns()
                if (res.payoutId) {
                    // Navigate directly into it
                    const foundRun = runs.find(r => r.id === res.payoutId)
                    if (foundRun) {
                        handleSelectRun(foundRun)
                    } else {
                        // Reload and load run manually
                        const freshRuns = await getWeeklyPayouts()
                        const newRun = freshRuns.find((r: any) => r.id === res.payoutId)
                        if (newRun) handleSelectRun(newRun as PayoutRun)
                    }
                }
            } else {
                toast({
                    title: 'Generation Failed',
                    description: res.error || 'A problem occurred generating the run.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error generating payout run:', error)
            toast({
                title: 'Error',
                description: 'Failed to generate payout run.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleStartEditItem = (item: PayoutItem) => {
        setEditingItemId(item.id)
        setEditAmountPaid(item.amount_paid.toString())
        setEditStatus(item.status)
        setEditNotes(item.notes || '')
    }

    const handleSaveItemEdit = async (itemId: string) => {
        if (!editAmountPaid || isNaN(Number(editAmountPaid)) || Number(editAmountPaid) < 0) {
            toast({
                title: 'Error',
                description: 'Please enter a valid amount paid.',
                variant: 'destructive'
            })
            return
        }

        setIsActionLoading(true)
        try {
            const res = await updatePayoutItem(itemId, {
                amount_paid: Number(editAmountPaid),
                status: editStatus,
                notes: editNotes
            })

            if (res.success) {
                toast({
                    title: 'Item Updated',
                    description: 'Payout line item updated successfully.'
                })
                setEditingItemId(null)
                // Refresh items
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                    
                    // Update current selected run header total in state
                    setSelectedRun(prev => {
                        if (!prev) return null
                        const newTotal = itemsData.reduce((sum, item) => sum + Number(item.amount_paid), 0)
                        return { ...prev, total_amount: newTotal }
                    })
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to update payout item.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error updating item:', error)
            toast({
                title: 'Error',
                description: 'Failed to save changes.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleAddCustomItem = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!selectedRun) return
        if (!customName.trim()) {
            toast({
                title: 'Error',
                description: 'Please enter a recipient name.',
                variant: 'destructive'
            })
            return
        }
        if (!customAmount || isNaN(Number(customAmount)) || Number(customAmount) <= 0) {
            toast({
                title: 'Error',
                description: 'Please enter a valid amount greater than 0.',
                variant: 'destructive'
            })
            return
        }

        setIsActionLoading(true)
        try {
            const res = await createCustomPayoutItem(selectedRun.id, {
                recipient_type: customType,
                recipient_name: customName,
                amount_due: Number(customAmount),
                project_id: customProjectId === 'none' ? null : customProjectId,
                reference_details: customRef,
                notes: customNotes
            })

            if (res.success) {
                toast({
                    title: 'Custom Item Added',
                    description: 'Added to weekly run payout successfully.'
                })
                setIsCustomOpen(false)
                setCustomName('')
                setCustomAmount('')
                setCustomProjectId('none')
                setCustomRef('')
                setCustomNotes('')
                
                // Refresh items
                const itemsData = await getPayoutItems(selectedRun.id)
                setItems(itemsData as PayoutItem[])
                
                setSelectedRun(prev => {
                    if (!prev) return null
                    const newTotal = itemsData.reduce((sum, item) => sum + Number(item.amount_paid), 0)
                    return { ...prev, total_amount: newTotal }
                })
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to add custom item.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error adding custom item:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleProcessRun = async (status: 'approved' | 'paid') => {
        if (!selectedRun) return
        const confirmMsg = status === 'paid' 
            ? 'Process payment? This will set all pending payout items to PAID and automatically create corresponding Project Expenses for items linked to projects.'
            : 'Approve this payment run?'

        if (!confirm(confirmMsg)) return

        setIsActionLoading(true)
        try {
            const res = await processWeeklyPayout(selectedRun.id, status)
            if (res.success) {
                toast({
                    title: 'Run Settled',
                    description: `Weekly payout run is now marked as ${status}.`
                })
                
                // Refresh run and items
                const freshRuns = await getWeeklyPayouts()
                setRuns(freshRuns as PayoutRun[])
                
                const updatedRun = freshRuns.find((r: any) => r.id === selectedRun.id)
                if (updatedRun) {
                    setSelectedRun(updatedRun as PayoutRun)
                }
                const itemsData = await getPayoutItems(selectedRun.id)
                setItems(itemsData as PayoutItem[])
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to process run.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error processing run:', error)
            toast({
                title: 'Error',
                description: 'Failed to complete transaction.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleDeleteRun = async (id: string) => {
        if (!confirm('Are you sure you want to delete this weekly run? This will delete all generated payout items under it.')) return

        setIsActionLoading(true)
        try {
            const res = await deleteWeeklyPayout(id)
            if (res.success) {
                toast({
                    title: 'Run Deleted',
                    description: 'Weekly payout run successfully removed.'
                })
                if (selectedRun?.id === id) {
                    setSelectedRun(null)
                }
                loadRuns()
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to delete run.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error deleting run:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    // Filter items
    const filteredItems = items.filter(item => {
        const matchesSearch = item.recipient_name.toLowerCase().includes(itemSearch.toLowerCase()) || 
            (item.reference_details || '').toLowerCase().includes(itemSearch.toLowerCase()) ||
            (item.notes || '').toLowerCase().includes(itemSearch.toLowerCase())
        
        const matchesType = typeFilter === 'all' || item.recipient_type === typeFilter
        return matchesSearch && matchesType
    })

    // Compute stats for detailed view
    const stats = {
        totalDue: items.reduce((sum, i) => sum + Number(i.amount_due), 0),
        totalPaid: items.reduce((sum, i) => sum + (i.status === 'paid' ? Number(i.amount_paid) : 0), 0),
        totalPending: items.reduce((sum, i) => sum + (i.status === 'pending' ? Number(i.amount_paid) : 0), 0),
        totalHeld: items.reduce((sum, i) => sum + (i.status === 'held' ? Number(i.amount_paid) : 0), 0),
        itemCount: items.length
    }

    // Color mapper for statuses
    const getStatusBadge = (status: 'draft' | 'approved' | 'paid') => {
        switch (status) {
            case 'paid':
                return <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30">Paid</Badge>
            case 'approved':
                return <Badge className="bg-sky-500/20 text-sky-400 border-sky-500/30">Approved</Badge>
            default:
                return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30">Draft</Badge>
        }
    }

    const getItemStatusBadge = (status: 'pending' | 'paid' | 'held') => {
        switch (status) {
            case 'paid':
                return <Badge className="bg-emerald-500/25 text-emerald-400 border-emerald-500/20">Paid</Badge>
            case 'held':
                return <Badge className="bg-rose-500/25 text-rose-400 border-rose-500/20">Held</Badge>
            default:
                return <Badge className="bg-amber-500/25 text-amber-400 border-amber-500/20">Pending</Badge>
        }
    }

    return (
        <div className="flex-1 p-4 overflow-y-auto md:p-8 bg-transparent">
            <div className="max-w-6xl mx-auto space-y-6">
                
                {/* 1. RUNS LIST VIEW */}
                {!selectedRun ? (
                    <>
                        {/* Header */}
                        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                            <div>
                                <h1 className="text-3xl font-bold tracking-tight font-headline text-foreground bg-gradient-to-r from-foreground via-foreground/80 to-muted-foreground bg-clip-text text-transparent">
                                    Weekly Pay-Day runs
                                </h1>
                                <p className="text-muted-foreground mt-1">
                                    Compile laborer wages, material deliveries, and custom utilities to clear payments on pay-day.
                                </p>
                            </div>
                            <Button onClick={() => setIsGenOpen(true)} className="bg-gradient-to-r from-primary to-primary/80 hover:opacity-90 transition-all font-medium self-start sm:self-auto">
                                <Plus className="mr-2 h-4 w-4" /> Run Weekly Pay-Day
                            </Button>
                        </div>

                        {/* List Card */}
                        <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
                            <CardHeader className="pb-3 border-b border-muted/20">
                                <CardTitle className="text-xl font-semibold">Payment History</CardTitle>
                                <CardDescription>View, modify, and process generated runs.</CardDescription>
                            </CardHeader>
                            <CardContent className="p-0">
                                {isLoading ? (
                                    <div className="flex flex-col items-center justify-center py-20 gap-3">
                                        <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                        <p className="text-muted-foreground text-sm">Loading payruns...</p>
                                    </div>
                                ) : runs.length === 0 ? (
                                    <div className="text-center py-16 px-4">
                                        <FileSpreadsheet className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                        <h3 className="text-lg font-medium text-foreground">No Payout Runs</h3>
                                        <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                            Run a Pay-Day process to automatically compile and distribute weekly expenses.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="overflow-x-auto">
                                        <Table>
                                            <TableHeader className="bg-muted/10">
                                                <TableRow className="border-muted/20">
                                                    <TableHead>Week Period (YYYY-MM-DD)</TableHead>
                                                    <TableHead>Status</TableHead>
                                                    <TableHead>Total Run Amount</TableHead>
                                                    <TableHead>Generated By</TableHead>
                                                    <TableHead>Created At</TableHead>
                                                    <TableHead className="w-[120px] text-right">Actions</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {runs.map((run) => (
                                                    <TableRow 
                                                        key={run.id} 
                                                        className="border-muted/15 hover:bg-muted/5 transition-colors cursor-pointer"
                                                        onClick={() => handleSelectRun(run)}
                                                    >
                                                        <TableCell className="font-semibold">
                                                            <div className="flex items-center gap-2">
                                                                <Calendar className="h-4 w-4 text-primary/75" />
                                                                <span>{run.week_start_date}</span>
                                                                <span className="text-muted-foreground/60 font-normal">to</span>
                                                                <span>{run.week_end_date}</span>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell onClick={(e) => e.stopPropagation()}>
                                                            {getStatusBadge(run.status)}
                                                        </TableCell>
                                                        <TableCell className="font-bold text-foreground">
                                                            ₹{run.total_amount.toLocaleString('en-IN')}
                                                        </TableCell>
                                                        <TableCell className="text-sm text-muted-foreground">
                                                            {run.creator?.display_name || 'Admin'}
                                                        </TableCell>
                                                        <TableCell className="text-xs text-muted-foreground/80 font-mono">
                                                            {new Date(run.created_at).toLocaleDateString('en-IN', {
                                                                year: 'numeric',
                                                                month: 'short',
                                                                day: 'numeric'
                                                            })}
                                                        </TableCell>
                                                        <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                                                            <div className="flex items-center justify-end gap-1">
                                                                <Button
                                                                    variant="ghost"
                                                                    className="h-8 pl-2 pr-1.5 hover:bg-muted/10 text-muted-foreground hover:text-foreground"
                                                                    onClick={() => handleSelectRun(run)}
                                                                >
                                                                    Details <ChevronRight className="ml-1 h-3.5 w-3.5" />
                                                                </Button>
                                                                {run.status !== 'paid' && (
                                                                    <Button
                                                                        variant="ghost"
                                                                        size="icon"
                                                                        className="h-8 w-8 hover:bg-destructive/10 text-muted-foreground hover:text-destructive"
                                                                        onClick={() => handleDeleteRun(run.id)}
                                                                    >
                                                                        <Trash2 className="h-3.5 w-3.5" />
                                                                    </Button>
                                                                )}
                                                            </div>
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
                    
                    // 2. DETAILED RUN VIEW
                    <>
                        {/* Detail Header & Action Buttons */}
                        <div className="flex flex-col gap-4 border-b border-muted/20 pb-4">
                            <button
                                onClick={handleBackToList}
                                className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors self-start"
                            >
                                <ArrowLeft className="h-4 w-4" /> Back to Payout History
                            </button>
                            
                            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                                <div>
                                    <div className="flex items-center gap-3">
                                        <h1 className="text-2xl font-bold font-headline">
                                            Pay-Run: {selectedRun.week_start_date} - {selectedRun.week_end_date}
                                        </h1>
                                        {getStatusBadge(selectedRun.status)}
                                    </div>
                                    <p className="text-muted-foreground text-sm mt-1">
                                        Adjust individual payments or add custom bills before resolving the payroll run.
                                    </p>
                                </div>

                                <div className="flex items-center gap-2 self-start md:self-auto">
                                    {selectedRun.status !== 'paid' && (
                                        <>
                                            <Button
                                                onClick={() => setIsCustomOpen(true)}
                                                variant="outline"
                                                className="border-muted/30 bg-background/50 hover:bg-muted/10"
                                            >
                                                <Plus className="mr-2 h-4 w-4" /> Custom Payout
                                            </Button>

                                            {selectedRun.status === 'draft' && (
                                                <Button
                                                    onClick={() => handleProcessRun('approved')}
                                                    variant="secondary"
                                                    className="border border-muted/20 hover:bg-muted/10"
                                                >
                                                    Approve Run
                                                </Button>
                                            )}

                                            <Button
                                                onClick={() => handleProcessRun('paid')}
                                                className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium"
                                                disabled={isActionLoading}
                                            >
                                                {isActionLoading ? (
                                                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                                                ) : (
                                                    <Play className="mr-2 h-4 w-4 fill-current" />
                                                )}
                                                Process & Pay Run
                                            </Button>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Summary Widgets */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                <CardContent className="p-4 flex flex-col justify-center">
                                    <span className="text-xs text-muted-foreground uppercase font-medium">Total Payout</span>
                                    <span className="text-2xl font-bold text-foreground mt-1">₹{stats.totalDue.toLocaleString('en-IN')}</span>
                                    <span className="text-[10px] text-muted-foreground mt-0.5">{stats.itemCount} line items</span>
                                </CardContent>
                            </Card>
                            <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                <CardContent className="p-4 flex flex-col justify-center">
                                    <span className="text-xs text-muted-foreground uppercase font-medium">Total Paid</span>
                                    <span className="text-2xl font-bold text-emerald-400 mt-1">₹{stats.totalPaid.toLocaleString('en-IN')}</span>
                                    <span className="text-[10px] text-muted-foreground mt-0.5">Disbursed funds</span>
                                </CardContent>
                            </Card>
                            <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                <CardContent className="p-4 flex flex-col justify-center">
                                    <span className="text-xs text-muted-foreground uppercase font-medium">Total Pending</span>
                                    <span className="text-2xl font-bold text-amber-400 mt-1">₹{stats.totalPending.toLocaleString('en-IN')}</span>
                                    <span className="text-[10px] text-muted-foreground mt-0.5">Awaiting payout processing</span>
                                </CardContent>
                            </Card>
                            <Card className="glass-card bg-card/40 border-muted/20 shadow-md">
                                <CardContent className="p-4 flex flex-col justify-center">
                                    <span className="text-xs text-muted-foreground uppercase font-medium">On Hold</span>
                                    <span className="text-2xl font-bold text-rose-400 mt-1">₹{stats.totalHeld.toLocaleString('en-IN')}</span>
                                    <span className="text-[10px] text-muted-foreground mt-0.5">Deferred items</span>
                                </CardContent>
                            </Card>
                        </div>

                        {/* Items Table Card */}
                        <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
                            <CardHeader className="pb-3 border-b border-muted/20">
                                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                    <div>
                                        <CardTitle className="text-lg font-semibold">Payroll & Vendor Deliveries</CardTitle>
                                        <CardDescription>Generated from labor worklogs, PO receipts, and custom entries.</CardDescription>
                                    </div>
                                    <div className="flex items-center gap-3 w-full md:w-auto">
                                        <div className="relative flex-1 md:w-60">
                                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                            <Input
                                                placeholder="Filter recipient..."
                                                value={itemSearch}
                                                onChange={(e) => setItemSearch(e.target.value)}
                                                className="pl-9 w-full bg-background/50 border-muted/30 focus-visible:ring-primary h-9"
                                            />
                                        </div>
                                        <Select onValueChange={setTypeFilter} value={typeFilter}>
                                            <SelectTrigger className="w-[140px] bg-background/50 border-muted/30 h-9">
                                                <SelectValue placeholder="All types" />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="all">All Types</SelectItem>
                                                <SelectItem value="employee_salary">Employee Salary</SelectItem>
                                                <SelectItem value="labor_wage">Labor Wage</SelectItem>
                                                <SelectItem value="vendor_payment">Vendor PO</SelectItem>
                                                <SelectItem value="other">Custom Payout</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>
                                </div>
                            </CardHeader>
                            <CardContent className="p-0">
                                {isLoading ? (
                                    <div className="flex flex-col items-center justify-center py-20 gap-3">
                                        <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                        <p className="text-muted-foreground text-sm">Refreshing list items...</p>
                                    </div>
                                ) : filteredItems.length === 0 ? (
                                    <div className="text-center py-16 px-4">
                                        <Layers className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                        <h3 className="text-lg font-medium text-foreground">No Items Match Filters</h3>
                                        <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                            Try adjusting your filter settings or search terms.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="overflow-x-auto">
                                        <Table>
                                            <TableHeader className="bg-muted/10">
                                                <TableRow className="border-muted/20">
                                                    <TableHead>Recipient</TableHead>
                                                    <TableHead>Type</TableHead>
                                                    <TableHead>Reference Details</TableHead>
                                                    <TableHead>Linked Project</TableHead>
                                                    <TableHead>Amount Due</TableHead>
                                                    <TableHead className="w-[140px]">Amount Paid (₹)</TableHead>
                                                    <TableHead className="w-[130px]">Status</TableHead>
                                                    <TableHead className="text-right">Notes</TableHead>
                                                    {selectedRun.status !== 'paid' && <TableHead className="w-[80px]"></TableHead>}
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {filteredItems.map((item) => {
                                                    const isEditing = editingItemId === item.id
                                                    let badgeColor = 'bg-muted/20 text-muted-foreground border-muted/30'
                                                    if (item.recipient_type === 'employee_salary') {
                                                        badgeColor = 'bg-blue-500/20 text-blue-400 border-blue-500/30'
                                                    } else if (item.recipient_type === 'labor_wage') {
                                                        badgeColor = 'bg-orange-500/20 text-orange-400 border-orange-500/30'
                                                    } else if (item.recipient_type === 'vendor_payment') {
                                                        badgeColor = 'bg-purple-500/20 text-purple-400 border-purple-500/30'
                                                    }

                                                    return (
                                                        <TableRow key={item.id} className="border-muted/15 hover:bg-muted/5 transition-colors">
                                                            <TableCell className="font-semibold text-foreground">
                                                                <div className="flex items-center gap-2">
                                                                    {item.recipient_type === 'vendor_payment' ? (
                                                                        <Store className="h-3.5 w-3.5 text-purple-400" />
                                                                    ) : (
                                                                        <User className="h-3.5 w-3.5 text-sky-400" />
                                                                    )}
                                                                    <span>{item.recipient_name}</span>
                                                                </div>
                                                            </TableCell>
                                                            <TableCell>
                                                                <Badge className={`${badgeColor} font-normal capitalize`}>
                                                                    {item.recipient_type.replace('_', ' ')}
                                                                </Badge>
                                                            </TableCell>
                                                            <TableCell className="text-xs text-muted-foreground max-w-[200px] truncate">
                                                                {item.reference_details || 'N/A'}
                                                            </TableCell>
                                                            <TableCell className="text-xs font-medium">
                                                                {item.project?.name ? (
                                                                    <Badge variant="outline" className="text-[10px] text-muted-foreground border-muted/30">
                                                                        {item.project.name}
                                                                    </Badge>
                                                                ) : (
                                                                    <span className="text-muted-foreground/60 italic text-xs">Unlinked</span>
                                                                )}
                                                            </TableCell>
                                                            <TableCell className="font-mono text-muted-foreground text-sm">
                                                                ₹{item.amount_due.toLocaleString('en-IN')}
                                                            </TableCell>
                                                            
                                                            {/* Amount Paid field */}
                                                            <TableCell>
                                                                {isEditing ? (
                                                                    <Input
                                                                        type="number"
                                                                        value={editAmountPaid}
                                                                        onChange={(e) => setEditAmountPaid(e.target.value)}
                                                                        className="h-8 bg-background border-muted/40 focus:ring-primary w-24 px-2 py-1 font-mono text-xs"
                                                                    />
                                                                ) : (
                                                                    <span className="font-mono font-semibold text-foreground">
                                                                        ₹{item.amount_paid.toLocaleString('en-IN')}
                                                                    </span>
                                                                )}
                                                            </TableCell>

                                                            {/* Status field */}
                                                            <TableCell>
                                                                {isEditing ? (
                                                                    <Select onValueChange={(val: any) => setEditStatus(val)} value={editStatus}>
                                                                        <SelectTrigger className="h-8 bg-background border-muted/40 text-xs w-28">
                                                                            <SelectValue />
                                                                        </SelectTrigger>
                                                                        <SelectContent>
                                                                            <SelectItem value="pending">Pending</SelectItem>
                                                                            <SelectItem value="paid">Paid</SelectItem>
                                                                            <SelectItem value="held">Held</SelectItem>
                                                                        </SelectContent>
                                                                    </Select>
                                                                ) : (
                                                                    getItemStatusBadge(item.status)
                                                                )}
                                                            </TableCell>

                                                            {/* Notes */}
                                                            <TableCell className="text-xs max-w-[200px] truncate">
                                                                {isEditing ? (
                                                                    <Input
                                                                        value={editNotes}
                                                                        onChange={(e) => setEditNotes(e.target.value)}
                                                                        placeholder="Add comments..."
                                                                        className="h-8 bg-background border-muted/40 text-xs py-1"
                                                                    />
                                                                ) : (
                                                                    <span className="text-muted-foreground italic text-xs">
                                                                        {item.notes || 'None'}
                                                                    </span>
                                                                )}
                                                            </TableCell>

                                                            {/* Row Actions */}
                                                            {selectedRun.status !== 'paid' && (
                                                                <TableCell className="text-right">
                                                                    {isEditing ? (
                                                                        <div className="flex items-center gap-1">
                                                                            <Button
                                                                                size="icon"
                                                                                variant="ghost"
                                                                                className="h-7 w-7 text-emerald-500 hover:bg-emerald-500/10"
                                                                                onClick={() => handleSaveItemEdit(item.id)}
                                                                            >
                                                                                <Check className="h-4 w-4" />
                                                                            </Button>
                                                                            <Button
                                                                                size="icon"
                                                                                variant="ghost"
                                                                                className="h-7 w-7 text-muted-foreground hover:bg-muted/10"
                                                                                onClick={() => setEditingItemId(null)}
                                                                            >
                                                                                <X className="h-4 w-4" />
                                                                            </Button>
                                                                        </div>
                                                                    ) : (
                                                                        <Button
                                                                            variant="ghost"
                                                                            size="sm"
                                                                            className="h-7 px-2 hover:bg-muted/10 text-muted-foreground hover:text-foreground text-xs"
                                                                            onClick={() => handleStartEditItem(item)}
                                                                        >
                                                                            Adjust
                                                                        </Button>
                                                                    )}
                                                                </TableCell>
                                                            )}
                                                        </TableRow>
                                                    )
                                                })}
                                            </TableBody>
                                        </Table>
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </>
                )}

                {/* ========================================== */}
                {/* DIALOGS */}
                {/* ========================================== */}

                {/* Dialog: Generate Weekly Payout Run */}
                <Dialog open={isGenOpen} onOpenChange={setIsGenOpen}>
                    <DialogContent className="sm:max-w-[450px] glass-card border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
                        <DialogHeader>
                            <DialogTitle className="text-xl font-bold font-headline flex items-center gap-2">
                                <Calendar className="h-5 w-5 text-primary" /> Run Weekly Pay-Day
                            </DialogTitle>
                            <DialogDescription>
                                Set the start and end dates (YYYY-MM-DD) for this payroll run. The system compiles daily labor logs and supplier purchase orders.
                            </DialogDescription>
                        </DialogHeader>

                        <form onSubmit={handleGenerateRun} className="space-y-4 py-2">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="weekStart">Start Date</Label>
                                    <Input
                                        id="weekStart"
                                        type="date"
                                        value={weekStart}
                                        onChange={(e) => setWeekStart(e.target.value)}
                                        className="bg-background border-muted/30 focus-visible:ring-primary"
                                        required
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="weekEnd">End Date</Label>
                                    <Input
                                        id="weekEnd"
                                        type="date"
                                        value={weekEnd}
                                        onChange={(e) => setWeekEnd(e.target.value)}
                                        className="bg-background border-muted/30 focus-visible:ring-primary"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="bg-muted/10 border border-muted/20 p-3 rounded-lg flex items-start gap-2.5">
                                <AlertCircle className="h-5 w-5 text-primary/80 mt-0.5 flex-shrink-0" />
                                <div className="text-xs text-muted-foreground leading-relaxed">
                                    <span className="font-semibold text-foreground">Auto-Compilation details:</span>
                                    <ul className="list-disc pl-4 mt-1 space-y-1">
                                        <li>Calculates labor payouts matching worklog attendance in date range.</li>
                                        <li>Imports supplier purchase orders delivered or approved in this range.</li>
                                        <li>Pulls monthly staff profiles for employee salary payout entries.</li>
                                    </ul>
                                </div>
                            </div>

                            <DialogFooter className="pt-4 border-t border-muted/10">
                                <Button type="button" variant="outline" onClick={() => setIsGenOpen(false)} className="border-muted/30 hover:bg-muted/10">
                                    Cancel
                                </Button>
                                <Button type="submit" disabled={isActionLoading} className="bg-primary hover:opacity-90">
                                    {isActionLoading ? (
                                        <>
                                            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Compiling...
                                        </>
                                    ) : (
                                        'Compile Payouts'
                                    )}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>

                {/* Dialog: Create Custom Payout Item */}
                <Dialog open={isCustomOpen} onOpenChange={setIsCustomOpen}>
                    <DialogContent className="sm:max-w-[480px] glass-card border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
                        <DialogHeader>
                            <DialogTitle className="text-xl font-bold font-headline flex items-center gap-2">
                                <Plus className="h-5 w-5 text-primary" /> Add Custom Payout Item
                            </DialogTitle>
                            <DialogDescription>
                                Insert an extra payout line manually, such as site rent, power bills, or machinery rental.
                            </DialogDescription>
                        </DialogHeader>

                        <form onSubmit={handleAddCustomItem} className="space-y-4 py-1">
                            <div className="space-y-2">
                                <Label htmlFor="customName">Recipient Name</Label>
                                <Input
                                    id="customName"
                                    placeholder="e.g. Ram Landlords, Power Grid corp"
                                    value={customName}
                                    onChange={(e) => setCustomName(e.target.value)}
                                    className="bg-background border-muted/30"
                                    required
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="customType">Type</Label>
                                    <Select onValueChange={(val: any) => setCustomType(val)} value={customType}>
                                        <SelectTrigger className="bg-background border-muted/30">
                                            <SelectValue placeholder="Select type" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="employee_salary">Employee Salary</SelectItem>
                                            <SelectItem value="labor_wage">Labor Wage</SelectItem>
                                            <SelectItem value="vendor_payment">Vendor / Material</SelectItem>
                                            <SelectItem value="other">Other / Custom Utility</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="customAmount">Amount (₹)</Label>
                                    <Input
                                        id="customAmount"
                                        type="number"
                                        placeholder="Amount in Rupees"
                                        value={customAmount}
                                        onChange={(e) => setCustomAmount(e.target.value)}
                                        className="bg-background border-muted/30 font-mono"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="customProjectId">Associate to Project (Optional)</Label>
                                <Select onValueChange={setCustomProjectId} value={customProjectId}>
                                    <SelectTrigger className="bg-background border-muted/30">
                                        <SelectValue placeholder="No project link" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="none">No project link (General expense)</SelectItem>
                                        {projects.map((proj) => (
                                            <SelectItem key={proj.id} value={proj.id}>
                                                {proj.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <p className="text-[10px] text-muted-foreground">
                                    If linked, an expense entry will be automatically generated in project expenses upon payout run settlement.
                                </p>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="customRef">Reference Info</Label>
                                <Input
                                    id="customRef"
                                    placeholder="Invoice details, Rent agreement date, utilities code..."
                                    value={customRef}
                                    onChange={(e) => setCustomRef(e.target.value)}
                                    className="bg-background border-muted/30 text-sm"
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="customNotes">Notes / Bank info</Label>
                                <Input
                                    id="customNotes"
                                    placeholder="Add payment notes, IFSC, bank acc..."
                                    value={customNotes}
                                    onChange={(e) => setCustomNotes(e.target.value)}
                                    className="bg-background border-muted/30 text-sm"
                                />
                            </div>

                            <DialogFooter className="pt-4 border-t border-muted/10">
                                <Button type="button" variant="outline" onClick={() => setIsCustomOpen(false)} className="border-muted/30">
                                    Cancel
                                </Button>
                                <Button type="submit" disabled={isActionLoading} className="bg-primary hover:opacity-90">
                                    {isActionLoading ? 'Processing...' : 'Add Item'}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>

            </div>
        </div>
    )
}
