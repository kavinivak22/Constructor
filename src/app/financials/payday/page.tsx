'use client'

import { useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import {
    getWeeklyPayouts,
    getPayoutItems,
    createWeeklyPayoutRun,
    updatePayoutItem,
    createCustomPayoutItem,
    processWeeklyPayout,
    deleteWeeklyPayout,
    getProjects,
    getPayoutItemBreakdown,
    splitPayoutItem,
    deletePayoutItem,
    bulkUpdatePayoutItems,
    bulkDeletePayoutItems
} from '@/app/actions/financials'
import { getContractors } from '@/app/actions/contractors'
import { cn } from '@/lib/utils'
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
    CreditCard,
    Building2,
    Briefcase,
    Users,
    Download,
    AlertTriangle
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
    payout_class?: 'rate' | 'nmr'
}

interface Project {
    id: string
    name: string
}

export default function PaydayPage() {
    const { toast } = useToast()
    const searchParams = useSearchParams()
    const payoutIdParam = searchParams?.get('payoutId')
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

    const [isCustomOpen, setIsCustomOpen] = useState(false)
    const [customName, setCustomName] = useState('')
    const [customType, setCustomType] = useState<'employee_salary' | 'labor_wage' | 'vendor_payment' | 'other'>('other')
    const [customAmount, setCustomAmount] = useState('')
    const [customProjectId, setCustomProjectId] = useState<string>('none')
    const [customRef, setCustomRef] = useState('')
    const [customNotes, setCustomNotes] = useState('')
    const [customPayoutClass, setCustomPayoutClass] = useState<'rate' | 'nmr'>('nmr')
    const [customContractorId, setCustomContractorId] = useState<string>('none')
    const [contractorsList, setContractorsList] = useState<any[]>([])

    // Inline edit row state
    const [editingItemId, setEditingItemId] = useState<string | null>(null)
    const [editAmountPaid, setEditAmountPaid] = useState('')
    const [editStatus, setEditStatus] = useState<'pending' | 'paid' | 'held'>('pending')
    const [editNotes, setEditNotes] = useState('')
    const [editPayoutClass, setEditPayoutClass] = useState<'rate' | 'nmr'>('nmr')
    const [editRecipientName, setEditRecipientName] = useState('')
    const [editReferenceDetails, setEditReferenceDetails] = useState('')
    const [editProjectId, setEditProjectId] = useState<string>('none')

    // Bulk selections
    const [selectedItemIds, setSelectedItemIds] = useState<string[]>([])
    const [statusFilter, setStatusFilter] = useState('all')

    // Dialog: Split Payout Item
    const [isSplitOpen, setIsSplitOpen] = useState(false)
    const [splitItem, setSplitItem] = useState<PayoutItem | null>(null)
    const [rateSplitAmount, setRateSplitAmount] = useState('')
    const [nmrSplitAmount, setNmrSplitAmount] = useState('')

    // Dialog: Payout Breakdown
    const [isOpenBreakdown, setIsOpenBreakdown] = useState(false)
    const [breakdownData, setBreakdownData] = useState<{
        type: 'labor' | 'vendor' | 'other' | 'error'
        recipientName: string
        details: any[]
    } | null>(null)
    const [isBreakdownLoading, setIsBreakdownLoading] = useState(false)

    const loadRuns = async () => {
        setIsLoading(true)
        try {
            const data = await getWeeklyPayouts()
            setRuns(data as PayoutRun[])
            
            if (payoutIdParam) {
                const matched = (data as PayoutRun[]).find(r => r.id === payoutIdParam)
                if (matched) {
                    handleSelectRun(matched)
                }
            }
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
        const loadContractors = async () => {
            try {
                const res = await getContractors()
                if (res.success && res.data) {
                    setContractorsList(res.data)
                }
            } catch (e) {
                console.error(e)
            }
        }
        loadContractors()
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

    const handleOpenBreakdown = async (itemId: string) => {
        setIsOpenBreakdown(true)
        setIsBreakdownLoading(true)
        setBreakdownData(null)
        try {
            const res = await getPayoutItemBreakdown(itemId)
            setBreakdownData(res as any)
        } catch (error) {
            console.error('Error loading breakdown:', error)
            toast({
                title: 'Error',
                description: 'Failed to load detailed breakdown logs.',
                variant: 'destructive'
            })
        } finally {
            setIsBreakdownLoading(false)
        }
    }

    const handleOpenSplitDialog = (item: PayoutItem) => {
        setSplitItem(item)
        const half = Math.round(Number(item.amount_due) / 2)
        setRateSplitAmount(half.toString())
        setNmrSplitAmount((Number(item.amount_due) - half).toString())
        setIsSplitOpen(true)
    }

    const handleSaveSplit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!splitItem) return
        
        const rateAmt = Number(rateSplitAmount)
        const nmrAmt = Number(nmrSplitAmount)
        const totalAmt = Number(splitItem.amount_due)
        
        if (isNaN(rateAmt) || isNaN(nmrAmt) || rateAmt <= 0 || nmrAmt <= 0) {
            toast({
                title: 'Error',
                description: 'Please enter valid positive amounts.',
                variant: 'destructive'
            })
            return
        }

        if (Math.abs((rateAmt + nmrAmt) - totalAmt) > 0.01) {
            toast({
                title: 'Error',
                description: `Split amounts (₹${rateAmt + nmrAmt}) must equal the total amount due (₹${totalAmt}).`,
                variant: 'destructive'
            })
            return
        }

        setIsActionLoading(true)
        try {
            const res = await splitPayoutItem(splitItem.id, rateAmt, nmrAmt)
            if (res.success) {
                toast({
                    title: 'Payout Split Successfully',
                    description: `Split into ₹${rateAmt.toLocaleString('en-IN')} (Rate) and ₹${nmrAmt.toLocaleString('en-IN')} (NMR).`
                })
                setIsSplitOpen(false)
                setSplitItem(null)
                
                // Refresh items
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to split payout item.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error splitting payout item:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
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
        setEditPayoutClass(item.payout_class || 'nmr')
        setEditRecipientName(item.recipient_name)
        setEditReferenceDetails(item.reference_details || '')
        setEditProjectId(item.project_id || 'none')
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
                notes: editNotes,
                payout_class: editPayoutClass,
                recipient_name: editRecipientName,
                reference_details: editReferenceDetails || null,
                project_id: editProjectId === 'none' ? null : editProjectId
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

    const handleDeleteItem = async (itemId: string) => {
        if (!confirm('Are you sure you want to remove this payout item? Associated labor log entries or purchase orders will be reset to unpaid.')) return

        setIsActionLoading(true)
        try {
            const res = await deletePayoutItem(itemId)
            if (res.success) {
                toast({
                    title: 'Success',
                    description: 'Payout item successfully removed.'
                })
                // Clear selection if it was selected
                setSelectedItemIds(prev => prev.filter(id => id !== itemId))
                // Refresh items
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                    
                    setSelectedRun(prev => {
                        if (!prev) return null
                        const newTotal = itemsData.reduce((sum, item) => sum + Number(item.amount_paid), 0)
                        return { ...prev, total_amount: newTotal }
                    })
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to remove payout item.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error removing item:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleToggleSelectItem = (id: string) => {
        setSelectedItemIds(prev => 
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        )
    }

    const handleSelectAllFiltered = (filteredList: PayoutItem[], isChecked: boolean) => {
        if (isChecked) {
            const ids = filteredList.map(item => item.id)
            setSelectedItemIds(prev => Array.from(new Set([...prev, ...ids])))
        } else {
            const ids = filteredList.map(item => item.id)
            setSelectedItemIds(prev => prev.filter(x => !ids.includes(x)))
        }
    }

    const handleBulkStatusUpdate = async (status: 'pending' | 'paid' | 'held') => {
        if (selectedItemIds.length === 0) return
        setIsActionLoading(true)
        try {
            const res = await bulkUpdatePayoutItems(selectedItemIds, { status })
            if (res.success) {
                toast({
                    title: 'Success',
                    description: `Successfully updated ${selectedItemIds.length} items.`
                })
                setSelectedItemIds([])
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                    
                    setSelectedRun(prev => {
                        if (!prev) return null
                        const newTotal = itemsData.reduce((sum, item) => sum + Number(item.amount_paid), 0)
                        return { ...prev, total_amount: newTotal }
                    })
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to update items.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error bulk updating:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleBulkProjectUpdate = async (projectId: string | null) => {
        if (selectedItemIds.length === 0) return
        setIsActionLoading(true)
        try {
            const res = await bulkUpdatePayoutItems(selectedItemIds, { project_id: projectId })
            if (res.success) {
                toast({
                    title: 'Success',
                    description: `Successfully re-routed ${selectedItemIds.length} items.`
                })
                setSelectedItemIds([])
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to re-route items.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error bulk updating project:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleBulkDelete = async () => {
        if (selectedItemIds.length === 0) return
        if (!confirm(`Are you sure you want to remove ${selectedItemIds.length} selected items? Linked logs will be reset to unpaid.`)) return
        setIsActionLoading(true)
        try {
            const res = await bulkDeletePayoutItems(selectedItemIds)
            if (res.success) {
                toast({
                    title: 'Success',
                    description: `Successfully removed ${selectedItemIds.length} items.`
                })
                setSelectedItemIds([])
                if (selectedRun) {
                    const itemsData = await getPayoutItems(selectedRun.id)
                    setItems(itemsData as PayoutItem[])
                    
                    setSelectedRun(prev => {
                        if (!prev) return null
                        const newTotal = itemsData.reduce((sum, item) => sum + Number(item.amount_paid), 0)
                        return { ...prev, total_amount: newTotal }
                    })
                }
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to remove items.',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error bulk deleting:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsActionLoading(false)
        }
    }

    const handleExportCSV = () => {
        if (!selectedRun || items.length === 0) return

        // Columns: Recipient Name, Type, Project, Class, Amount Due, Amount Paid, Status, Notes
        const headers = ['Recipient Name', 'Recipient Type', 'Project / Site', 'Classification', 'Amount Due (INR)', 'Amount Paid (INR)', 'Status', 'Notes']
        
        const rows = items.map(item => {
            const projName = item.project?.name || 'General / Head Office'
            const payoutClass = (item.payout_class || 'nmr').toUpperCase()
            const notesText = item.notes ? item.notes.replace(/"/g, '""') : ''
            const refDetails = item.reference_details ? item.reference_details.replace(/"/g, '""') : ''

            return [
                `"${item.recipient_name}"`,
                `"${item.recipient_type}"`,
                `"${projName}"`,
                `"${payoutClass}"`,
                item.amount_due,
                item.amount_paid,
                `"${item.status}"`,
                `"${notesText || refDetails}"`
            ]
        })

        const csvContent = [headers.join(','), ...rows.map(e => e.join(','))].join('\n')
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.setAttribute('href', url)
        const fileName = `payrun_${selectedRun.week_start_date}_to_${selectedRun.week_end_date}.csv`
        link.setAttribute('download', fileName)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        toast({
            title: 'CSV Exported',
            description: `File ${fileName} downloaded successfully.`
        })
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
                recipient_id: customContractorId === 'none' ? null : customContractorId,
                amount_due: Number(customAmount),
                project_id: customProjectId === 'none' ? null : customProjectId,
                reference_details: customRef,
                notes: customNotes,
                payout_class: customPayoutClass
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
                setCustomPayoutClass('nmr')
                setCustomContractorId('none')
                
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
        let referenceSearchText = item.reference_details || ''
        
        if (item.reference_details && item.reference_details.startsWith('{')) {
            try {
                const parsed = JSON.parse(item.reference_details)
                if (parsed && parsed.type === 'contractor_wages') {
                    // Combine descriptions and categories into a search-friendly string
                    const descriptions = (parsed.descriptions || []).join(' ')
                    const categories = (parsed.breakdown || []).map((b: any) => b.category).join(' ')
                    referenceSearchText = `${descriptions} ${categories}`
                }
            } catch (e) {
                // Ignore parse errors, fall back to raw string
            }
        }

        const matchesSearch = item.recipient_name.toLowerCase().includes(itemSearch.toLowerCase()) || 
            referenceSearchText.toLowerCase().includes(itemSearch.toLowerCase()) ||
            (item.notes || '').toLowerCase().includes(itemSearch.toLowerCase())
        
        const matchesType = typeFilter === 'all' || item.recipient_type === typeFilter
        const matchesStatus = statusFilter === 'all' || item.status === statusFilter
        return matchesSearch && matchesType && matchesStatus
    })

    // Group items by Project -> Wages, Vendor Payments, Others
    const projectGroups: Record<string, {
        projectName: string;
        wages: PayoutItem[];
        vendors: PayoutItem[];
        others: PayoutItem[];
    }> = {};

    for (const item of filteredItems) {
        const pId = item.project_id || 'unlinked';
        const pName = item.project?.name || 'General / Head Office';

        if (!projectGroups[pId]) {
            projectGroups[pId] = {
                projectName: pName,
                wages: [],
                vendors: [],
                others: []
            };
        }

        if (item.recipient_type === 'employee_salary' || item.recipient_type === 'labor_wage') {
            projectGroups[pId].wages.push(item);
        } else if (item.recipient_type === 'vendor_payment') {
            projectGroups[pId].vendors.push(item);
        } else {
            projectGroups[pId].others.push(item);
        }
    }

    // Sort items within each project category alphabetically by recipient name
    for (const pId in projectGroups) {
        const group = projectGroups[pId];
        group.wages.sort((a, b) => a.recipient_name.toLowerCase().localeCompare(b.recipient_name.toLowerCase()));
        group.vendors.sort((a, b) => a.recipient_name.toLowerCase().localeCompare(b.recipient_name.toLowerCase()));
        group.others.sort((a, b) => a.recipient_name.toLowerCase().localeCompare(b.recipient_name.toLowerCase()));
    }

    // Convert to sorted array of groups
    const sortedProjectGroups = Object.entries(projectGroups).filter(([_, group]) => {
        return group.wages.length > 0 || group.vendors.length > 0 || group.others.length > 0;
    });

    sortedProjectGroups.sort((a, b) => {
        if (a[0] === 'unlinked') return 1;
        if (b[0] === 'unlinked') return -1;
        return a[1].projectName.localeCompare(b[1].projectName);
    });

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

    const renderRow = (item: PayoutItem) => {
        const isEditing = editingItemId === item.id

        // Parse reference details if it contains aggregated contractor wages JSON
        let isContractorWages = false
        let contractorWagesData: {
            type: 'contractor_wages'
            descriptions: string[]
            breakdown: Array<{
                category: string
                days: number
                rate: number
                amount: number
            }>
        } | null = null

        if (item.reference_details && item.reference_details.startsWith('{')) {
            try {
                const parsed = JSON.parse(item.reference_details)
                if (parsed && parsed.type === 'contractor_wages') {
                    isContractorWages = true
                    contractorWagesData = parsed
                }
            } catch (e) {
                // Not JSON or parse failed
            }
        }

        const isMissingBank = (item.recipient_type === 'labor_wage' || item.recipient_type === 'employee_salary') && 
            (item.notes?.includes('Bank: N/A') || item.notes?.includes('Acc: N/A') || !item.notes)

        return (
            <TableRow key={item.id} className={cn(
                "border-muted/15 hover:bg-muted/5 transition-colors",
                selectedItemIds.includes(item.id) && "bg-primary/5"
            )}>
                    <TableCell className="w-[40px] py-3 pl-4">
                        <input 
                            type="checkbox"
                            checked={selectedItemIds.includes(item.id)}
                            onChange={() => handleToggleSelectItem(item.id)}
                            className="rounded border-muted/30 h-4 w-4 bg-background/50 text-primary focus:ring-primary focus:ring-offset-0 focus:ring-0 cursor-pointer"
                        />
                    </TableCell>
                <TableCell className="font-semibold text-foreground py-3 min-w-[320px]">
                    <div className="flex flex-col space-y-1.5">
                        <div className="flex items-center gap-2 flex-wrap">
                            {item.recipient_type === 'vendor_payment' ? (
                                <Store className="h-3.5 w-3.5 text-purple-400" />
                            ) : isContractorWages ? (
                                <Building2 className="h-3.5 w-3.5 text-amber-400" />
                            ) : (
                                <User className="h-3.5 w-3.5 text-sky-400" />
                            )}
                            
                            {isEditing ? (
                                <Input
                                    value={editRecipientName}
                                    onChange={(e) => setEditRecipientName(e.target.value)}
                                    className="h-8 bg-background border-muted/40 focus:ring-primary text-xs w-full max-w-[180px] px-2 py-1 font-semibold"
                                    placeholder="Recipient Name"
                                />
                            ) : (
                                <button
                                    type="button"
                                    onClick={() => handleOpenBreakdown(item.id)}
                                    className="hover:underline hover:text-primary text-left font-semibold text-xs"
                                >
                                    {item.recipient_name}
                                </button>
                            )}

                            {isEditing ? (
                                <Select onValueChange={(val: any) => setEditPayoutClass(val)} value={editPayoutClass}>
                                    <SelectTrigger className="h-7 w-20 text-[9px] bg-background border-muted/45 px-1 py-0 font-bold uppercase">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="glass">
                                        <SelectItem value="nmr" className="text-[10px]">NMR</SelectItem>
                                        <SelectItem value="rate" className="text-[10px]">RATE</SelectItem>
                                    </SelectContent>
                                </Select>
                            ) : (
                                <Badge 
                                    variant="outline" 
                                    className={cn(
                                        "text-[9px] uppercase px-1 py-0 font-bold",
                                        (item.payout_class || 'nmr') === 'rate'
                                            ? "bg-purple-500/10 text-purple-300 border-purple-500/20"
                                            : "bg-teal-500/10 text-teal-300 border-teal-500/20"
                                    )}
                                >
                                    {(item.payout_class || 'nmr').toUpperCase()}
                                </Badge>
                            )}

                            {isMissingBank && !isEditing && (
                                <Badge variant="outline" className="bg-amber-500/10 text-amber-300 border-amber-500/20 text-[9px] py-0 px-1 font-medium flex items-center gap-1 select-none">
                                    <AlertTriangle className="h-2.5 w-2.5 text-amber-400" />
                                    No Bank Details
                                </Badge>
                            )}
                        </div>

                        {isEditing && (
                            <div className="mt-1.5 space-y-1 w-full max-w-[200px]">
                                <Label className="text-[9px] uppercase font-bold tracking-wider text-muted-foreground/60">Route Project</Label>
                                <Select onValueChange={setEditProjectId} value={editProjectId}>
                                    <SelectTrigger className="h-7 text-[10px] bg-background border-muted/40 px-2 py-0.5">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="glass text-xs">
                                        <SelectItem value="none" className="text-xs">No project link (General expense)</SelectItem>
                                        {projects.map((proj) => (
                                            <SelectItem key={proj.id} value={proj.id} className="text-xs">
                                                {proj.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        )}

                        {isEditing && (
                            <div className="mt-1.5 space-y-1 w-full max-w-[200px]">
                                <Label className="text-[9px] uppercase font-bold tracking-wider text-muted-foreground/60">Reference Details</Label>
                                <Input
                                    value={editReferenceDetails}
                                    onChange={(e) => setEditReferenceDetails(e.target.value)}
                                    className="h-7 bg-background border-muted/40 text-[10px] px-2 py-0.5 font-normal"
                                    placeholder="Invoice, rent, utilities code..."
                                />
                            </div>
                        )}

                        {!isEditing && !isContractorWages && item.reference_details && (
                            <div className="pl-6 text-[11px] font-normal text-muted-foreground/80 flex items-center gap-1.5 flex-wrap">
                                <span className="font-semibold text-foreground/75">Ref:</span>
                                <span className="italic text-foreground/90">{item.reference_details}</span>
                            </div>
                        )}

                        {isContractorWages && contractorWagesData && (
                            <div className="pl-6 space-y-2 font-normal text-muted-foreground">
                                {/* Combined Work Descriptions */}
                                {contractorWagesData.descriptions && contractorWagesData.descriptions.length > 0 && (
                                    <div className="text-[11px] leading-relaxed">
                                        <span className="font-semibold text-foreground/75">Work: </span>
                                        <span className="italic">{contractorWagesData.descriptions.filter(Boolean).join(', ') || 'N/A'}</span>
                                    </div>
                                )}
                                {/* Worker Category & Payment Breakdown */}
                                <div className="space-y-1">
                                    <div className="text-[9px] uppercase font-bold tracking-wider text-muted-foreground/60">Worker Breakdown</div>
                                    <div className="flex flex-wrap gap-1.5">
                                        {contractorWagesData.breakdown.map((cat, idx) => (
                                            <Badge
                                                key={idx}
                                                variant="outline"
                                                className="text-[10px] bg-sky-500/5 text-sky-300 border-sky-500/10 font-normal px-2 py-0.5 whitespace-nowrap"
                                            >
                                                <span className="font-medium text-foreground">{cat.category}</span>: {cat.days} {cat.days === 1 ? 'day' : 'days'} @ ₹{cat.rate} = <span className="font-semibold text-emerald-400">₹{cat.amount.toLocaleString('en-IN')}</span>
                                            </Badge>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}

                        {!isEditing && (
                            <button
                                type="button"
                                onClick={() => handleOpenBreakdown(item.id)}
                                className="text-[10px] text-muted-foreground hover:text-primary text-left pl-6 w-max block font-normal cursor-pointer"
                            >
                                View detailed breakdown
                            </button>
                        )}
                    </div>
                </TableCell>
                


                <TableCell className="font-mono text-muted-foreground text-xs">
                    ₹{item.amount_due.toLocaleString('en-IN')}
                </TableCell>
                
                {/* Amount Paid field */}
                <TableCell>
                    <div className="flex flex-col space-y-1">
                        {isEditing ? (
                            <Input
                                type="number"
                                value={editAmountPaid}
                                onChange={(e) => setEditAmountPaid(e.target.value)}
                                className="h-8 bg-background border-muted/40 focus:ring-primary w-24 px-2 py-1 font-mono text-xs"
                            />
                        ) : (
                            <span className="font-mono font-semibold text-foreground text-xs">
                                ₹{item.amount_paid.toLocaleString('en-IN')}
                            </span>
                        )}

                        {(!isEditing && item.amount_paid < item.amount_due) && (
                            <Badge variant="outline" className="bg-rose-500/10 text-rose-300 border-rose-500/20 text-[9px] py-0 px-1 font-semibold w-max select-none">
                                Pending: ₹{(item.amount_due - item.amount_paid).toLocaleString('en-IN')}
                            </Badge>
                        )}
                    </div>
                </TableCell>

                {/* Status field */}
                <TableCell>
                    {isEditing ? (
                        <Select onValueChange={(val: any) => setEditStatus(val)} value={editStatus}>
                            <SelectTrigger className="h-8 bg-background border-muted/40 text-xs w-28">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent className="glass">
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
                        <span className="text-muted-foreground italic text-xs font-normal">
                            {item.notes || 'None'}
                        </span>
                    )}
                </TableCell>

                {/* Row Actions */}
                <TableCell className="text-right">
                    {isEditing ? (
                        <div className="flex items-center justify-end gap-1">
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
                        <div className="flex items-center justify-end gap-1.5">
                            <Button
                                variant="ghost"
                                size="sm"
                                className="h-7 px-2 hover:bg-muted/10 text-muted-foreground hover:text-foreground text-xs"
                                onClick={() => handleStartEditItem(item)}
                            >
                                Adjust
                            </Button>
                            {item.recipient_type === 'labor_wage' && item.amount_due > 1 && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-7 px-2 border-muted/30 hover:bg-purple-500/10 hover:text-purple-400 text-[10px] font-medium"
                                    onClick={() => handleOpenSplitDialog(item)}
                                >
                                    Split
                                </Button>
                            )}
                            <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-muted-foreground hover:text-rose-400 hover:bg-rose-500/10"
                                onClick={() => handleDeleteItem(item.id)}
                            >
                                <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                        </div>
                    )}
                </TableCell>
            </TableRow>
        )
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
                        <button
                            onClick={handleBackToList}
                            className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors self-start mb-4"
                        >
                            <ArrowLeft className="h-4 w-4" /> Back to Payout History
                        </button>

                        {/* Pay-Run Overview & Period Details Card */}
                        <Card className="glass-card border border-muted/20 shadow-xl rounded-2xl overflow-hidden bg-card/45 backdrop-blur-md">
                            <CardContent className="p-6 md:p-8 space-y-6">
                                {/* Top Row: Date period and main status */}
                                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-muted/15 pb-6">
                                    <div className="flex items-center gap-4">
                                        <div className="h-12 w-12 rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shadow-inner">
                                            <Calendar className="h-6 w-6" />
                                        </div>
                                        <div>
                                            <span className="text-[10px] uppercase font-bold tracking-widest text-muted-foreground/60 block">PAYROLL PERIOD</span>
                                            <h1 className="text-xl font-bold font-headline text-foreground mt-0.5">
                                                {selectedRun.week_start_date} <span className="text-muted-foreground font-normal text-base">to</span> {selectedRun.week_end_date}
                                            </h1>
                                            <div className="flex items-center gap-2 mt-1.5 text-xs text-muted-foreground">
                                                <span>Generated by <span className="font-semibold text-foreground">{selectedRun.creator?.display_name || 'Admin'}</span></span>
                                                <span>•</span>
                                                <span>{new Date(selectedRun.created_at).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })}</span>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 w-full md:w-auto">
                                        <div className="flex items-center justify-between sm:justify-start gap-3 bg-background/30 px-3 py-1.5 rounded-xl border border-muted/10">
                                            <span className="text-xs text-muted-foreground">Run Status:</span>
                                            {getStatusBadge(selectedRun.status)}
                                        </div>
                                        
                                        <div className="flex items-center gap-2">
                                            <Button
                                                onClick={handleExportCSV}
                                                variant="outline"
                                                className="border-muted/30 bg-background/50 hover:bg-muted/10 text-muted-foreground hover:text-foreground h-9 px-3 text-xs"
                                            >
                                                <Download className="mr-1.5 h-4 w-4" /> Export CSV
                                            </Button>

                                            {selectedRun.status !== 'paid' ? (
                                                <>
                                                    <Button
                                                        onClick={() => setIsCustomOpen(true)}
                                                        variant="outline"
                                                        className="border-muted/30 bg-background/50 hover:bg-muted/10 h-9 px-3 text-xs"
                                                    >
                                                        <Plus className="mr-1.5 h-4 w-4" /> Custom Payout
                                                    </Button>

                                                    {selectedRun.status === 'draft' && (
                                                        <Button
                                                            onClick={() => handleProcessRun('approved')}
                                                            variant="secondary"
                                                            className="border border-muted/20 hover:bg-muted/10 h-9 px-3 text-xs"
                                                        >
                                                            Approve Run
                                                        </Button>
                                                    )}

                                                    <Button
                                                        onClick={() => handleProcessRun('paid')}
                                                        className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium h-9 px-3 text-xs"
                                                        disabled={isActionLoading}
                                                    >
                                                        {isActionLoading ? (
                                                            <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
                                                        ) : (
                                                            <Play className="mr-1.5 h-4 w-4 fill-current" />
                                                        )}
                                                        Process & Pay Run
                                                    </Button>
                                                </>
                                            ) : (
                                                <Button
                                                    onClick={() => setIsCustomOpen(true)}
                                                    variant="outline"
                                                    className="border-muted/30 bg-background/50 hover:bg-muted/10 h-9 px-3 text-xs"
                                                >
                                                    <Plus className="mr-1.5 h-4 w-4" /> Custom Payout
                                                </Button>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* Metrics Grid */}
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div className="bg-background/25 border border-muted/10 rounded-2xl p-4 flex flex-col justify-between shadow-sm hover:bg-background/30 transition-all">
                                        <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-wider">Total Payout</span>
                                        <span className="text-2xl font-bold text-foreground mt-2 font-mono">₹{stats.totalDue.toLocaleString('en-IN')}</span>
                                        <span className="text-[10px] text-muted-foreground mt-1">{stats.itemCount} line items</span>
                                    </div>
                                    <div className="bg-emerald-500/5 border border-emerald-500/10 rounded-2xl p-4 flex flex-col justify-between shadow-sm hover:bg-emerald-500/10 transition-all">
                                        <span className="text-[10px] text-emerald-400 uppercase font-bold tracking-wider">Total Paid</span>
                                        <span className="text-2xl font-bold text-emerald-400 mt-2 font-mono">₹{stats.totalPaid.toLocaleString('en-IN')}</span>
                                        <span className="text-[10px] text-muted-foreground mt-1">Disbursed funds</span>
                                    </div>
                                    <div className="bg-amber-500/5 border border-amber-500/10 rounded-2xl p-4 flex flex-col justify-between shadow-sm hover:bg-amber-500/10 transition-all">
                                        <span className="text-[10px] text-amber-400 uppercase font-bold tracking-wider">Total Pending</span>
                                        <span className="text-2xl font-bold text-amber-400 mt-2 font-mono">₹{stats.totalPending.toLocaleString('en-IN')}</span>
                                        <span className="text-[10px] text-muted-foreground mt-1">Awaiting processing</span>
                                    </div>
                                    <div className="bg-rose-500/5 border border-rose-500/10 rounded-2xl p-4 flex flex-col justify-between shadow-sm hover:bg-rose-500/10 transition-all">
                                        <span className="text-[10px] text-rose-400 uppercase font-bold tracking-wider">On Hold</span>
                                        <span className="text-2xl font-bold text-rose-400 mt-2 font-mono">₹{stats.totalHeld.toLocaleString('en-IN')}</span>
                                        <span className="text-[10px] text-muted-foreground mt-1">Deferred items</span>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Search & Filter Toolbar */}
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-muted/5 border border-muted/15 p-4 rounded-2xl backdrop-blur-sm shadow-sm">
                            <div>
                                <h2 className="text-sm font-bold text-foreground">Filter Site Payments</h2>
                                <p className="text-[11px] text-muted-foreground">Search by recipient name, description, or notes.</p>
                            </div>
                            <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
                                <div className="relative flex-1 md:w-60 min-w-[180px]">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        placeholder="Search payments..."
                                        value={itemSearch}
                                        onChange={(e) => setItemSearch(e.target.value)}
                                        className="pl-9 w-full bg-background/50 border-muted/30 focus-visible:ring-primary h-9 text-xs"
                                    />
                                </div>
                                <Select onValueChange={setTypeFilter} value={typeFilter}>
                                    <SelectTrigger className="w-[140px] bg-background/50 border-muted/30 h-9 text-xs">
                                        <SelectValue placeholder="All types" />
                                    </SelectTrigger>
                                    <SelectContent className="glass">
                                        <SelectItem value="all" className="text-xs">All Types</SelectItem>
                                        <SelectItem value="employee_salary" className="text-xs">Employee Salary</SelectItem>
                                        <SelectItem value="labor_wage" className="text-xs">Labor Wage</SelectItem>
                                        <SelectItem value="vendor_payment" className="text-xs">Vendor PO</SelectItem>
                                        <SelectItem value="other" className="text-xs">Custom Payout</SelectItem>
                                    </SelectContent>
                                </Select>

                                <Select onValueChange={setStatusFilter} value={statusFilter}>
                                    <SelectTrigger className="w-[140px] bg-background/50 border-muted/30 h-9 text-xs">
                                        <SelectValue placeholder="All status" />
                                    </SelectTrigger>
                                    <SelectContent className="glass">
                                        <SelectItem value="all" className="text-xs">All Status</SelectItem>
                                        <SelectItem value="pending" className="text-xs">Pending</SelectItem>
                                        <SelectItem value="paid" className="text-xs">Paid</SelectItem>
                                        <SelectItem value="held" className="text-xs">Held</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        {/* Standalone Site Cards & Payments List */}
                        {isLoading ? (
                            <div className="flex flex-col items-center justify-center py-20 gap-3">
                                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                <p className="text-muted-foreground text-sm">Refreshing list items...</p>
                            </div>
                        ) : sortedProjectGroups.length === 0 ? (
                            <div className="text-center py-16 px-4 bg-muted/5 border border-muted/15 rounded-2xl">
                                <Layers className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                <h3 className="text-base font-bold text-foreground">No Items Match Filters</h3>
                                <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-xs">
                                    Try adjusting your filter settings or search terms.
                                </p>
                            </div>
                        ) : (
                            <div className="space-y-8">
                                {sortedProjectGroups.map(([projId, group]) => {
                                    const siteItems = [...group.wages, ...group.vendors, ...group.others];
                                    const siteTotalDue = siteItems.reduce((sum, item) => sum + Number(item.amount_due), 0);
                                    const siteTotalPaid = siteItems.reduce((sum, item) => sum + (item.status === 'paid' ? Number(item.amount_paid) : 0), 0);
                                    const siteItemCount = siteItems.length;

                                    // Contractor & non-contractor categorizing
                                    const contractorMap: Record<string, { contractorName: string; items: PayoutItem[] }> = {};
                                    const nonContractorVendors: PayoutItem[] = [];
                                    const nonContractorStaff: PayoutItem[] = [];
                                    const nonContractorOthers: PayoutItem[] = [];

                                    siteItems.forEach(item => {
                                        let isContractorWages = false;
                                        if (item.reference_details && item.reference_details.startsWith('{')) {
                                            try {
                                                const parsed = JSON.parse(item.reference_details);
                                                if (parsed && parsed.type === 'contractor_wages') {
                                                    isContractorWages = true;
                                                }
                                            } catch (e) {}
                                        }

                                        const matchedContractor = contractorsList.find(c => c.id === item.recipient_id) || 
                                            (isContractorWages ? { id: item.recipient_id || 'unknown_contractor', name: item.recipient_name } : null);

                                        if (matchedContractor) {
                                            const cId = matchedContractor.id;
                                            if (!contractorMap[cId]) {
                                                contractorMap[cId] = {
                                                    contractorName: matchedContractor.name,
                                                    items: []
                                                };
                                            }
                                            contractorMap[cId].items.push(item);
                                        } else {
                                            if (item.recipient_type === 'vendor_payment') {
                                                nonContractorVendors.push(item);
                                            } else if (item.recipient_type === 'employee_salary' || item.recipient_type === 'labor_wage') {
                                                nonContractorStaff.push(item);
                                            } else {
                                                nonContractorOthers.push(item);
                                            }
                                        }
                                    });

                                    // Sort contractors alphabetically
                                    const sortedContractors = Object.entries(contractorMap).sort((a, b) => 
                                        a[1].contractorName.toLowerCase().localeCompare(b[1].contractorName.toLowerCase())
                                    );

                                    return (
                                        <Card key={projId} className="glass-card bg-card/30 border-muted/20 shadow-xl rounded-2xl overflow-hidden">
                                            <CardHeader className="bg-muted/5 border-b border-muted/15 p-5 flex flex-row items-center justify-between gap-4 flex-wrap">
                                                <div className="flex items-center gap-3">
                                                    <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary border border-primary/15 shadow-inner">
                                                        <Building2 className="h-5 w-5" />
                                                    </div>
                                                    <div>
                                                        <CardTitle className="text-base font-bold text-foreground font-headline">
                                                            {group.projectName}
                                                        </CardTitle>
                                                        <CardDescription className="text-xs text-muted-foreground mt-0.5">
                                                            {siteItemCount} {siteItemCount === 1 ? 'payment' : 'payments'} compiled • Total Due: <span className="font-semibold text-foreground font-mono">₹{siteTotalDue.toLocaleString('en-IN')}</span> • Paid: <span className="font-semibold text-emerald-400 font-mono">₹{siteTotalPaid.toLocaleString('en-IN')}</span>
                                                        </CardDescription>
                                                    </div>
                                                </div>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    className="h-8 px-3 hover:bg-muted/15 text-xs text-muted-foreground hover:text-foreground font-semibold border border-muted/10 bg-muted/5 flex items-center gap-1.5 rounded-lg shadow-sm"
                                                    onClick={() => {
                                                        setCustomProjectId(projId);
                                                        setIsCustomOpen(true);
                                                    }}
                                                >
                                                    <Plus className="h-3.5 w-3.5 text-primary" /> Quick Payout
                                                </Button>
                                            </CardHeader>
                                            
                                            <CardContent className="p-6 space-y-6">
                                                {/* 1. CONTRACTORS */}
                                                {sortedContractors.length > 0 && (
                                                    <div className="space-y-4">
                                                        <h4 className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider pl-1 flex items-center gap-2">
                                                            <Briefcase className="h-3.5 w-3.5 text-amber-400" /> Contractor Accounts
                                                        </h4>
                                                        <div className="space-y-4">
                                                            {sortedContractors.map(([cId, cData]) => {
                                                                const contractorDue = cData.items.reduce((sum, item) => sum + Number(item.amount_due), 0);
                                                                const contractorPaid = cData.items.reduce((sum, item) => sum + (item.status === 'paid' ? Number(item.amount_paid) : 0), 0);
                                                                return (
                                                                    <div key={cId} className="border border-muted/15 rounded-xl p-4 bg-muted/5 space-y-3 shadow-inner">
                                                                        <div className="flex items-center justify-between border-b border-muted/10 pb-2 flex-wrap gap-2">
                                                                            <div className="flex items-center gap-2">
                                                                                <Building2 className="h-4 w-4 text-amber-500" />
                                                                                <span className="text-sm font-bold text-foreground">{cData.contractorName}</span>
                                                                                <Badge variant="outline" className="text-[9px] uppercase px-1.5 py-0 bg-primary/10 border-primary/20 text-primary font-bold">Contractor</Badge>
                                                                            </div>
                                                                            <div className="text-xs text-muted-foreground font-normal">
                                                                                {cData.items.length} {cData.items.length === 1 ? 'item' : 'items'} • Due: <span className="font-bold text-foreground font-mono">₹{contractorDue.toLocaleString('en-IN')}</span> • Paid: <span className="font-bold text-emerald-400 font-mono">₹{contractorPaid.toLocaleString('en-IN')}</span>
                                                                            </div>
                                                                        </div>
                                                                        
                                                                        <div className="overflow-x-auto rounded-lg border border-muted/10 bg-background/25">
                                                                            <Table>
                                                                                <TableHeader className="bg-muted/10">
                                                                                    <TableRow className="border-muted/10">
                                                                                        <TableHead className="w-[40px] pl-4">
                                                                                            <input 
                                                                                                type="checkbox"
                                                                                                checked={cData.items.length > 0 && cData.items.every(item => selectedItemIds.includes(item.id))}
                                                                                                onChange={(e) => handleSelectAllFiltered(cData.items, e.target.checked)}
                                                                                                className="rounded border-muted/30 h-4 w-4 bg-background/50 text-primary focus:ring-primary focus:ring-offset-0 focus:ring-0 cursor-pointer"
                                                                                            />
                                                                                        </TableHead>
                                                                                        <TableHead className="min-w-[320px]">Type / Particulars</TableHead>
                                                                                        <TableHead>Amount Due</TableHead>
                                                                                        <TableHead className="w-[140px]">Amount Paid (₹)</TableHead>
                                                                                        <TableHead className="w-[130px]">Status</TableHead>
                                                                                        <TableHead className="text-right">Notes</TableHead>
                                                                                        <TableHead className="w-[80px]"></TableHead>
                                                                                    </TableRow>
                                                                                </TableHeader>
                                                                                <TableBody>
                                                                                    {cData.items.map(item => renderRow(item))}
                                                                                </TableBody>
                                                                            </Table>
                                                                        </div>
                                                                    </div>
                                                                );
                                                            })}
                                                        </div>
                                                    </div>
                                                )}

                                                {/* 2. MATERIAL SUPPLIERS & VENDORS */}
                                                {nonContractorVendors.length > 0 && (
                                                    <div className="space-y-3 pt-2">
                                                        <h4 className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider pl-1 flex items-center gap-2">
                                                            <Store className="h-3.5 w-3.5 text-purple-400" /> Material Suppliers & Vendors
                                                        </h4>
                                                        <div className="overflow-x-auto rounded-xl border border-muted/10 bg-background/25">
                                                            <Table>
                                                                <TableHeader className="bg-muted/10">
                                                                    <TableRow className="border-muted/10">
                                                                        <TableHead className="w-[40px] pl-4">
                                                                            <input 
                                                                                type="checkbox"
                                                                                checked={nonContractorVendors.length > 0 && nonContractorVendors.every(item => selectedItemIds.includes(item.id))}
                                                                                onChange={(e) => handleSelectAllFiltered(nonContractorVendors, e.target.checked)}
                                                                                className="rounded border-muted/30 h-4 w-4 bg-background/50 text-primary focus:ring-primary focus:ring-offset-0 focus:ring-0 cursor-pointer"
                                                                            />
                                                                        </TableHead>
                                                                        <TableHead className="min-w-[320px]">Supplier Name</TableHead>
                                                                        <TableHead>Amount Due</TableHead>
                                                                        <TableHead className="w-[140px]">Amount Paid (₹)</TableHead>
                                                                        <TableHead className="w-[130px]">Status</TableHead>
                                                                        <TableHead className="text-right">Notes</TableHead>
                                                                        <TableHead className="w-[80px]"></TableHead>
                                                                    </TableRow>
                                                                </TableHeader>
                                                                <TableBody>
                                                                    {nonContractorVendors.map(item => renderRow(item))}
                                                                </TableBody>
                                                            </Table>
                                                        </div>
                                                    </div>
                                                )}

                                                {/* 3. DIRECT STAFF & DAILY LABOR */}
                                                {nonContractorStaff.length > 0 && (
                                                    <div className="space-y-3 pt-2">
                                                        <h4 className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider pl-1 flex items-center gap-2">
                                                            <Users className="h-3.5 w-3.5 text-sky-400" /> Direct Staff & Daily Labor
                                                        </h4>
                                                        <div className="overflow-x-auto rounded-xl border border-muted/10 bg-background/25">
                                                            <Table>
                                                                <TableHeader className="bg-muted/10">
                                                                    <TableRow className="border-muted/10">
                                                                        <TableHead className="w-[40px] pl-4">
                                                                            <input 
                                                                                type="checkbox"
                                                                                checked={nonContractorStaff.length > 0 && nonContractorStaff.every(item => selectedItemIds.includes(item.id))}
                                                                                onChange={(e) => handleSelectAllFiltered(nonContractorStaff, e.target.checked)}
                                                                                className="rounded border-muted/30 h-4 w-4 bg-background/50 text-primary focus:ring-primary focus:ring-offset-0 focus:ring-0 cursor-pointer"
                                                                            />
                                                                        </TableHead>
                                                                        <TableHead className="min-w-[320px]">Recipient Name</TableHead>
                                                                        <TableHead>Amount Due</TableHead>
                                                                        <TableHead className="w-[140px]">Amount Paid (₹)</TableHead>
                                                                        <TableHead className="w-[130px]">Status</TableHead>
                                                                        <TableHead className="text-right">Notes</TableHead>
                                                                        <TableHead className="w-[80px]"></TableHead>
                                                                    </TableRow>
                                                                </TableHeader>
                                                                <TableBody>
                                                                    {nonContractorStaff.map(item => renderRow(item))}
                                                                </TableBody>
                                                            </Table>
                                                        </div>
                                                    </div>
                                                )}

                                                {/* 4. OTHER GENERAL EXPENSES */}
                                                {nonContractorOthers.length > 0 && (
                                                    <div className="space-y-3 pt-2">
                                                        <h4 className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider pl-1 flex items-center gap-2">
                                                            <Activity className="h-3.5 w-3.5 text-amber-500" /> General Expenses & Utilities
                                                        </h4>
                                                        <div className="overflow-x-auto rounded-xl border border-muted/10 bg-background/25">
                                                            <Table>
                                                                <TableHeader className="bg-muted/10">
                                                                    <TableRow className="border-muted/10">
                                                                        <TableHead className="w-[40px] pl-4">
                                                                            <input 
                                                                                type="checkbox"
                                                                                checked={nonContractorOthers.length > 0 && nonContractorOthers.every(item => selectedItemIds.includes(item.id))}
                                                                                onChange={(e) => handleSelectAllFiltered(nonContractorOthers, e.target.checked)}
                                                                                className="rounded border-muted/30 h-4 w-4 bg-background/50 text-primary focus:ring-primary focus:ring-offset-0 focus:ring-0 cursor-pointer"
                                                                            />
                                                                        </TableHead>
                                                                        <TableHead className="min-w-[320px]">Recipient</TableHead>
                                                                        <TableHead>Amount Due</TableHead>
                                                                        <TableHead className="w-[140px]">Amount Paid (₹)</TableHead>
                                                                        <TableHead className="w-[130px]">Status</TableHead>
                                                                        <TableHead className="text-right">Notes</TableHead>
                                                                        <TableHead className="w-[80px]"></TableHead>
                                                                    </TableRow>
                                                                </TableHeader>
                                                                <TableBody>
                                                                    {nonContractorOthers.map(item => renderRow(item))}
                                                                </TableBody>
                                                            </Table>
                                                        </div>
                                                    </div>
                                                )}
                                            </CardContent>
                                        </Card>
                                    );
                                })}
                            </div>
                        )}

                        {/* Floating Bulk Actions Bar */}
                        {selectedItemIds.length > 0 && (
                            <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 w-full max-w-2xl px-4 animate-in fade-in slide-in-from-bottom-5 duration-300">
                                <div className="glass shadow-2xl border border-primary/20 rounded-2xl px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-background/85 backdrop-blur-xl">
                                    <div className="flex items-center gap-2">
                                        <Badge className="bg-primary hover:bg-primary/95 text-white font-mono rounded-full h-6 w-6 flex items-center justify-center p-0 text-xs">
                                            {selectedItemIds.length}
                                        </Badge>
                                        <span className="text-xs font-semibold text-foreground">Items selected</span>
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => setSelectedItemIds([])}
                                            className="text-[10px] text-muted-foreground hover:text-foreground h-6 px-1.5"
                                        >
                                            Clear
                                        </Button>
                                    </div>
                                    <div className="flex items-center gap-2 flex-wrap justify-end">
                                        <Select onValueChange={(val: any) => handleBulkStatusUpdate(val)}>
                                            <SelectTrigger className="h-8 text-[11px] bg-background border-muted/30 w-28">
                                                <SelectValue placeholder="Set Status" />
                                            </SelectTrigger>
                                            <SelectContent className="glass text-xs">
                                                <SelectItem value="pending">Pending</SelectItem>
                                                <SelectItem value="paid">Paid</SelectItem>
                                                <SelectItem value="held">Held</SelectItem>
                                            </SelectContent>
                                        </Select>

                                        <Select onValueChange={(val) => handleBulkProjectUpdate(val === 'none' ? null : val)}>
                                            <SelectTrigger className="h-8 text-[11px] bg-background border-muted/30 w-32 text-left">
                                                <SelectValue placeholder="Route Project" />
                                            </SelectTrigger>
                                            <SelectContent className="glass text-xs">
                                                <SelectItem value="none">No Project</SelectItem>
                                                {projects.map(p => (
                                                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>

                                        <Button
                                            variant="destructive"
                                            size="sm"
                                            onClick={handleBulkDelete}
                                            className="h-8 px-3 text-[11px] font-semibold bg-rose-600 hover:bg-rose-500"
                                            disabled={isActionLoading}
                                        >
                                            <Trash2 className="h-3.5 w-3.5 mr-1" /> Remove
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </>
                )}

                {/* ========================================== */}
                {/* DIALOGS */}
                {/* ========================================== */}

                {/* Dialog: Generate Weekly Payout Run */}
                <Dialog open={isGenOpen} onOpenChange={setIsGenOpen}>
                    <DialogContent className="sm:max-w-[450px] border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
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
                    <DialogContent className="sm:max-w-[480px] border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
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
                                <Label htmlFor="customContractor">Link to Contractor Account (Optional)</Label>
                                <Select 
                                    onValueChange={(val) => {
                                        setCustomContractorId(val);
                                        if (val !== 'none') {
                                            const contractor = contractorsList.find(c => c.id === val);
                                            if (contractor) {
                                                setCustomName(contractor.name);
                                                setCustomType('labor_wage');
                                            }
                                        }
                                    }} 
                                    value={customContractorId}
                                >
                                    <SelectTrigger className="bg-background border-muted/30 text-xs">
                                        <SelectValue placeholder="Do not link to contractor" />
                                    </SelectTrigger>
                                    <SelectContent className="glass text-xs">
                                        <SelectItem value="none">Independent Payout (No Contractor Link)</SelectItem>
                                        {contractorsList.map((c) => (
                                            <SelectItem key={c.id} value={c.id}>
                                                {c.name} ({c.category || 'Contractor'})
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

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
                                        <SelectTrigger className="bg-background border-muted/30 text-xs">
                                            <SelectValue placeholder="Select type" />
                                        </SelectTrigger>
                                        <SelectContent className="glass text-xs">
                                            <SelectItem value="employee_salary">Employee Salary</SelectItem>
                                            <SelectItem value="labor_wage">Labor Wage</SelectItem>
                                            <SelectItem value="vendor_payment">Vendor / Material</SelectItem>
                                            <SelectItem value="other">Other / Custom Utility</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="customClassification">Classification</Label>
                                    <Select onValueChange={(val: any) => setCustomPayoutClass(val)} value={customPayoutClass}>
                                        <SelectTrigger className="bg-background border-muted/30 text-xs font-semibold text-foreground">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent className="glass text-xs">
                                            <SelectItem value="nmr">NMR (Daily Wage / Day Work)</SelectItem>
                                            <SelectItem value="rate">RATE (Contract Sq.Ft / Work Rate)</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
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

                {/* Dialog: Payout Item Breakdown */}
                <Dialog open={isOpenBreakdown} onOpenChange={setIsOpenBreakdown}>
                    <DialogContent className="sm:max-w-[600px] border-muted/30 text-foreground bg-background/95 backdrop-blur-xl max-h-[85vh] overflow-y-auto">
                        <DialogHeader>
                            <DialogTitle className="text-xl font-bold font-headline flex items-center gap-2">
                                <Activity className="h-5 w-5 text-primary" /> Payout Detailed Breakdown
                            </DialogTitle>
                            <DialogDescription>
                                Detailed daily logs and items making up the payout for <span className="font-semibold text-foreground">{breakdownData?.recipientName}</span>.
                            </DialogDescription>
                        </DialogHeader>

                        {isBreakdownLoading ? (
                            <div className="flex flex-col items-center justify-center py-12 gap-3">
                                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                <p className="text-sm text-muted-foreground">Fetching detailed logs...</p>
                            </div>
                        ) : !breakdownData || breakdownData.details.length === 0 ? (
                            <div className="text-center py-8 text-muted-foreground text-sm">
                                No detailed logs or items found for this payout period.
                            </div>
                        ) : breakdownData.type === 'labor' ? (
                            <div className="space-y-4 py-2">
                                <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">Daily Attendance & Worklogs</div>
                                <div className="space-y-3">
                                    {breakdownData.details.map((log: any, idx: number) => (
                                        <div key={idx} className="bg-muted/10 border border-muted/20 p-3 rounded-lg space-y-2">
                                            <div className="flex items-center justify-between">
                                                <div className="flex items-center gap-2 text-xs font-semibold text-foreground">
                                                    <Calendar className="h-3.5 w-3.5 text-primary" />
                                                    <span>{log.date}</span>
                                                </div>
                                                <Badge variant="secondary" className="text-[10px] bg-primary/10 text-primary border-primary/20">
                                                    {log.projectName}
                                                </Badge>
                                            </div>
                                            <div className="text-xs font-medium text-foreground/90">
                                                <span className="text-muted-foreground">Log:</span> {log.worklogTitle}
                                            </div>
                                            {log.workDescription && (
                                                <div className="text-xs text-muted-foreground/80 leading-relaxed bg-background/40 p-2 rounded">
                                                    <span className="font-semibold text-foreground/80 block mb-0.5">Description:</span>
                                                    {log.workDescription}
                                                </div>
                                            )}
                                            <div className="flex flex-wrap gap-1.5 pt-1">
                                                {log.workers.map((w: any, wIdx: number) => (
                                                    <Badge key={wIdx} variant="outline" className="text-[10px] bg-background border-muted/30">
                                                        {w.type}: <span className="font-semibold text-foreground ml-1">{w.count}</span>
                                                    </Badge>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ) : breakdownData.type === 'vendor' ? (
                            <div className="space-y-3 py-2">
                                <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">Material Purchase Order Items</div>
                                <div className="border border-muted/20 rounded-lg overflow-hidden">
                                    <Table>
                                        <TableHeader className="bg-muted/5">
                                            <TableRow className="border-muted/20">
                                                <TableHead className="text-xs">Material</TableHead>
                                                <TableHead className="text-xs">Qty</TableHead>
                                                <TableHead className="text-xs">Unit Price</TableHead>
                                                <TableHead className="text-right text-xs">Total</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {breakdownData.details.map((item: any, idx: number) => (
                                                <TableRow key={idx} className="border-muted/15">
                                                    <TableCell className="text-xs font-medium text-foreground">{item.material_name}</TableCell>
                                                    <TableCell className="text-xs font-mono">{Number(item.quantity).toLocaleString()}</TableCell>
                                                    <TableCell className="text-xs font-mono">₹{Number(item.unit_price).toLocaleString('en-IN')}</TableCell>
                                                    <TableCell className="text-right text-xs font-mono font-semibold text-foreground">₹{Number(item.total_price).toLocaleString('en-IN')}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </div>
                            </div>
                        ) : (
                            <div className="text-center py-8 text-muted-foreground text-sm">
                                Standard or manual payout item. Detailed logs are only available for compiled labor logs and purchase orders.
                            </div>
                        )}

                        <DialogFooter className="pt-4 border-t border-muted/10">
                            <Button type="button" onClick={() => setIsOpenBreakdown(false)} className="bg-primary hover:opacity-90">
                                Close Breakdown
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>

                {/* Dialog: Split Payout Item */}
                <Dialog open={isSplitOpen} onOpenChange={setIsSplitOpen}>
                    <DialogContent className="sm:max-w-[420px] border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
                        <DialogHeader>
                            <DialogTitle className="text-xl font-bold font-headline flex items-center gap-2">
                                <Layers className="h-5 w-5 text-purple-400" /> Split Payout (Rate vs NMR)
                            </DialogTitle>
                            <DialogDescription>
                                Divide calculated wages for <span className="font-semibold text-foreground">{splitItem?.recipient_name}</span>. The total amount due is <span className="font-bold text-foreground">₹{splitItem?.amount_due.toLocaleString('en-IN')}</span>.
                            </DialogDescription>
                        </DialogHeader>

                        {splitItem && (
                            <form onSubmit={handleSaveSplit} className="space-y-4 py-2">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="rateSplitAmount">RATE Contract Amount (₹)</Label>
                                        <Input
                                            id="rateSplitAmount"
                                            type="number"
                                            value={rateSplitAmount}
                                            onChange={(e) => {
                                                setRateSplitAmount(e.target.value)
                                                const remain = Number(splitItem.amount_due) - Number(e.target.value)
                                                setNmrSplitAmount(remain >= 0 ? remain.toString() : '0')
                                            }}
                                            className="bg-background border-muted/30 font-mono text-purple-300 animate-pulse-once"
                                            required
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label htmlFor="nmrSplitAmount">NMR Labor Amount (₹)</Label>
                                        <Input
                                            id="nmrSplitAmount"
                                            type="number"
                                            value={nmrSplitAmount}
                                            onChange={(e) => {
                                                setNmrSplitAmount(e.target.value)
                                                const remain = Number(splitItem.amount_due) - Number(e.target.value)
                                                setRateSplitAmount(remain >= 0 ? remain.toString() : '0')
                                            }}
                                            className="bg-background border-muted/30 font-mono text-teal-300 animate-pulse-once"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="text-xs text-muted-foreground p-2 rounded-lg bg-muted/10 border border-muted/15 flex justify-between">
                                    <span>Allocated Total: <span className="font-semibold text-foreground">₹{(Number(rateSplitAmount) + Number(nmrSplitAmount)).toLocaleString('en-IN')}</span></span>
                                    <span>Target: <span className="font-semibold text-foreground">₹{splitItem.amount_due.toLocaleString('en-IN')}</span></span>
                                </div>

                                <DialogFooter className="pt-4 border-t border-muted/10">
                                    <Button type="button" variant="outline" onClick={() => setIsSplitOpen(false)} className="border-muted/30">
                                        Cancel
                                    </Button>
                                    <Button 
                                        type="submit" 
                                        disabled={isActionLoading || Math.abs((Number(rateSplitAmount) + Number(nmrSplitAmount)) - Number(splitItem.amount_due)) > 0.01} 
                                        className="bg-primary hover:opacity-90"
                                    >
                                        Confirm Split
                                    </Button>
                                </DialogFooter>
                            </form>
                        )}
                    </DialogContent>
                </Dialog>

            </div>
        </div>
    )
}
