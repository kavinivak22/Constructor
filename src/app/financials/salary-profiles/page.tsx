'use client'

import { useEffect, useState } from 'react'
import { getSalaryProfiles, saveSalaryProfile, deleteSalaryProfile, getCompanyUsers } from '@/app/actions/financials'
import { useToast } from '@/hooks/use-toast'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Loader2, Plus, Edit2, Trash2, Search, Building2, CreditCard, User, Landmark } from 'lucide-react'

interface SalaryProfile {
    id: string
    user_id: string | null
    worker_name: string | null
    payment_type: 'monthly' | 'daily_wage' | 'hourly'
    rate: number
    bank_name: string | null
    account_number: string | null
    ifsc_code: string | null
    users?: {
        display_name: string
        email: string
        role: string
    } | null
}

interface CompanyUser {
    id: string
    display_name: string
    email: string
    role: string
}

export default function SalaryProfilesPage() {
    const { toast } = useToast()
    const [profiles, setProfiles] = useState<SalaryProfile[]>([])
    const [users, setUsers] = useState<CompanyUser[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')
    
    // Dialog state
    const [isOpen, setIsOpen] = useState(false)
    const [editingProfile, setEditingProfile] = useState<SalaryProfile | null>(null)
    
    // Form state
    const [workerSource, setWorkerSource] = useState<'registered' | 'external'>('registered')
    const [selectedUserId, setSelectedUserId] = useState<string>('')
    const [externalName, setExternalName] = useState('')
    const [paymentType, setPaymentType] = useState<'monthly' | 'daily_wage' | 'hourly'>('daily_wage')
    const [rate, setRate] = useState('')
    const [bankName, setBankName] = useState('')
    const [accountNumber, setAccountNumber] = useState('')
    const [ifscCode, setIfscCode] = useState('')

    const loadData = async () => {
        setIsLoading(true)
        try {
            const [profilesData, usersData] = await Promise.all([
                getSalaryProfiles(),
                getCompanyUsers()
            ])
            setProfiles(profilesData as SalaryProfile[])
            setUsers(usersData as CompanyUser[])
        } catch (error) {
            console.error('Error loading financials data:', error)
            toast({
                title: 'Error',
                description: 'Failed to load data. Please refresh.',
                variant: 'destructive'
            })
        } finally {
            setIsLoading(false)
        }
    }

    useEffect(() => {
        loadData()
    }, [])

    const handleOpenDialog = (profile?: SalaryProfile) => {
        if (profile) {
            setEditingProfile(profile)
            if (profile.user_id) {
                setWorkerSource('registered')
                setSelectedUserId(profile.user_id)
                setExternalName('')
            } else {
                setWorkerSource('external')
                setSelectedUserId('')
                setExternalName(profile.worker_name || '')
            }
            setPaymentType(profile.payment_type)
            setRate(profile.rate.toString())
            setBankName(profile.bank_name || '')
            setAccountNumber(profile.account_number || '')
            setIfscCode(profile.ifsc_code || '')
        } else {
            setEditingProfile(null)
            setWorkerSource('registered')
            setSelectedUserId('')
            setExternalName('')
            setPaymentType('daily_wage')
            setRate('')
            setBankName('')
            setAccountNumber('')
            setIfscCode('')
        }
        setIsOpen(true)
    }

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault()
        
        if (workerSource === 'registered' && !selectedUserId) {
            toast({
                title: 'Error',
                description: 'Please select a registered worker/employee.',
                variant: 'destructive'
            })
            return
        }

        if (workerSource === 'external' && !externalName.trim()) {
            toast({
                title: 'Error',
                description: 'Please enter a worker name.',
                variant: 'destructive'
            })
            return
        }

        if (!rate || isNaN(Number(rate)) || Number(rate) <= 0) {
            toast({
                title: 'Error',
                description: 'Please enter a valid rate greater than 0.',
                variant: 'destructive'
            })
            return
        }

        setIsSubmitting(true)
        try {
            const res = await saveSalaryProfile({
                id: editingProfile?.id,
                user_id: workerSource === 'registered' ? selectedUserId : null,
                worker_name: workerSource === 'external' ? externalName : null,
                payment_type: paymentType,
                rate: Number(rate),
                bank_name: bankName,
                account_number: accountNumber,
                ifsc_code: ifscCode
            })

            if (res.success) {
                toast({
                    title: 'Success',
                    description: editingProfile ? 'Salary profile updated' : 'Salary profile created',
                })
                setIsOpen(false)
                loadData()
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to save salary profile',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error saving salary profile:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    const handleDelete = async (id: string) => {
        if (!confirm('Are you sure you want to delete this salary profile?')) return

        try {
            const res = await deleteSalaryProfile(id)
            if (res.success) {
                toast({
                    title: 'Success',
                    description: 'Salary profile deleted'
                })
                loadData()
            } else {
                toast({
                    title: 'Error',
                    description: res.error || 'Failed to delete salary profile',
                    variant: 'destructive'
                })
            }
        } catch (error) {
            console.error('Error deleting salary profile:', error)
            toast({
                title: 'Error',
                description: 'An unexpected error occurred.',
                variant: 'destructive'
            })
        }
    }

    const filteredProfiles = profiles.filter(profile => {
        const name = profile.worker_name || profile.users?.display_name || ''
        const bank = profile.bank_name || ''
        const acc = profile.account_number || ''
        return name.toLowerCase().includes(searchQuery.toLowerCase()) || 
            bank.toLowerCase().includes(searchQuery.toLowerCase()) || 
            acc.includes(searchQuery)
    })

    // Filter out users who already have a profile (unless editing that user)
    const availableUsers = users.filter(user => {
        const hasProfile = profiles.some(p => p.user_id === user.id)
        if (editingProfile && editingProfile.user_id === user.id) return true
        return !hasProfile
    })

    return (
        <div className="flex-1 p-4 overflow-y-auto md:p-8 bg-transparent">
            <div className="max-w-6xl mx-auto space-y-6">
                
                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight font-headline text-foreground bg-gradient-to-r from-foreground via-foreground/80 to-muted-foreground bg-clip-text text-transparent">
                            Salary & Wage Profiles
                        </h1>
                        <p className="text-muted-foreground mt-1">
                            Manage wage structures, payout types, and banking info for registered staff and external daily laborers.
                        </p>
                    </div>
                    <Button onClick={() => handleOpenDialog()} className="bg-gradient-to-r from-primary to-primary/80 hover:opacity-90 transition-all font-medium self-start sm:self-auto">
                        <Plus className="mr-2 h-4 w-4" /> Add Wage Profile
                    </Button>
                </div>

                {/* Main View Cards */}
                <div className="grid gap-6">
                    <Card className="glass-card border-muted/30 shadow-xl overflow-hidden backdrop-blur-md bg-card/60">
                        <CardHeader className="pb-3 border-b border-muted/20">
                            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                                <div>
                                    <CardTitle className="text-xl font-semibold">Active Profiles</CardTitle>
                                    <CardDescription>Wage templates used to auto-generate weekly payouts.</CardDescription>
                                </div>
                                <div className="relative w-full md:w-72">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        placeholder="Search by worker name, bank..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        className="pl-9 w-full bg-background/50 border-muted/30 focus-visible:ring-primary"
                                    />
                                </div>
                            </div>
                        </CardHeader>
                        <CardContent className="p-0">
                            {isLoading ? (
                                <div className="flex flex-col items-center justify-center py-20 gap-3">
                                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                                    <p className="text-muted-foreground text-sm">Loading profiles...</p>
                                </div>
                            ) : filteredProfiles.length === 0 ? (
                                <div className="text-center py-16 px-4">
                                    <User className="h-12 w-12 text-muted-foreground/50 mx-auto mb-3" />
                                    <h3 className="text-lg font-medium text-foreground">No Profiles Found</h3>
                                    <p className="text-muted-foreground max-w-sm mx-auto mt-1 text-sm">
                                        {searchQuery ? 'Try adjusting your search criteria.' : 'Create a profile to begin managing wages and weekly run values.'}
                                    </p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <Table>
                                        <TableHeader className="bg-muted/10">
                                            <TableRow className="border-muted/20">
                                                <TableHead>Recipient</TableHead>
                                                <TableHead>Type</TableHead>
                                                <TableHead>Payment structure</TableHead>
                                                <TableHead>Rate</TableHead>
                                                <TableHead>Bank details</TableHead>
                                                <TableHead className="w-[100px] text-right">Actions</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {filteredProfiles.map((profile) => {
                                                const isRegistered = !!profile.user_id
                                                const displayName = profile.worker_name || profile.users?.display_name || 'Worker'
                                                const email = profile.users?.email
                                                
                                                return (
                                                    <TableRow key={profile.id} className="border-muted/15 hover:bg-muted/5 transition-colors">
                                                        <TableCell className="font-medium">
                                                            <div className="flex flex-col">
                                                                <span>{displayName}</span>
                                                                {email && <span className="text-xs text-muted-foreground font-normal">{email}</span>}
                                                            </div>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Badge variant={isRegistered ? 'default' : 'outline'} className={isRegistered ? 'bg-primary/20 text-primary border-primary/30' : 'bg-muted/20 text-muted-foreground border-muted/30'}>
                                                                {isRegistered ? 'Registered User' : 'External Laborer'}
                                                            </Badge>
                                                        </TableCell>
                                                        <TableCell className="capitalize">
                                                            <Badge variant="secondary" className="font-normal border border-muted-foreground/10 bg-muted/30">
                                                                {profile.payment_type.replace('_', ' ')}
                                                            </Badge>
                                                        </TableCell>
                                                        <TableCell className="font-semibold">
                                                            ₹{profile.rate.toLocaleString('en-IN')}
                                                            <span className="text-xs text-muted-foreground font-normal">
                                                                {profile.payment_type === 'monthly' ? '/mo' : profile.payment_type === 'daily_wage' ? '/day' : '/hr'}
                                                            </span>
                                                        </TableCell>
                                                        <TableCell>
                                                            {profile.bank_name ? (
                                                                <div className="flex items-start gap-2 text-sm text-muted-foreground">
                                                                    <Landmark className="h-4 w-4 text-muted-foreground/75 mt-0.5" />
                                                                    <div className="flex flex-col">
                                                                        <span className="text-foreground/80 font-medium text-xs">{profile.bank_name}</span>
                                                                        <span className="font-mono text-xs">A/C: {profile.account_number}</span>
                                                                        {profile.ifsc_code && <span className="text-[10px] font-mono">IFSC: {profile.ifsc_code}</span>}
                                                                    </div>
                                                                </div>
                                                            ) : (
                                                                <span className="text-xs text-muted-foreground/60 italic">No bank info added</span>
                                                            )}
                                                        </TableCell>
                                                        <TableCell className="text-right">
                                                            <div className="flex items-center justify-end gap-1">
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8 hover:bg-muted/10 text-muted-foreground hover:text-foreground"
                                                                    onClick={() => handleOpenDialog(profile)}
                                                                >
                                                                    <Edit2 className="h-3.5 w-3.5" />
                                                                </Button>
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8 hover:bg-destructive/10 text-muted-foreground hover:text-destructive"
                                                                    onClick={() => handleDelete(profile.id)}
                                                                >
                                                                    <Trash2 className="h-3.5 w-3.5" />
                                                                </Button>
                                                            </div>
                                                        </TableCell>
                                                    </TableRow>
                                                )
                                            })}
                                        </TableBody>
                                    </Table>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Create/Edit Profile Dialog */}
                <Dialog open={isOpen} onOpenChange={setIsOpen}>
                    <DialogContent className="sm:max-w-[500px] glass-card border-muted/30 text-foreground bg-background/95 backdrop-blur-xl">
                        <DialogHeader>
                            <DialogTitle className="text-xl font-bold font-headline">
                                {editingProfile ? 'Edit Salary Profile' : 'Create Salary Profile'}
                            </DialogTitle>
                            <DialogDescription>
                                Set the base wage rate and bank info. This will auto-compile payout entries on payday.
                            </DialogDescription>
                        </DialogHeader>

                        <form onSubmit={handleSave} className="space-y-4 py-2">
                            
                            {/* Worker Source Toggle (Only editable for new profiles) */}
                            {!editingProfile && (
                                <div className="grid grid-cols-2 gap-2 bg-muted/20 p-1 rounded-lg border border-muted/10">
                                    <button
                                        type="button"
                                        className={`py-1.5 text-xs font-medium rounded-md transition-all ${workerSource === 'registered' ? 'bg-primary text-primary-foreground shadow' : 'text-muted-foreground hover:text-foreground'}`}
                                        onClick={() => setWorkerSource('registered')}
                                    >
                                        Registered User
                                    </button>
                                    <button
                                        type="button"
                                        className={`py-1.5 text-xs font-medium rounded-md transition-all ${workerSource === 'external' ? 'bg-primary text-primary-foreground shadow' : 'text-muted-foreground hover:text-foreground'}`}
                                        onClick={() => setWorkerSource('external')}
                                    >
                                        External Laborer
                                    </button>
                                </div>
                            )}

                            {/* Worker Selector */}
                            <div className="space-y-2">
                                <Label htmlFor="worker">Worker Name</Label>
                                {workerSource === 'registered' ? (
                                    editingProfile ? (
                                        <div className="p-2 border border-muted/20 rounded bg-muted/10 text-sm font-medium flex items-center gap-2">
                                            <User className="h-4 w-4 text-muted-foreground" />
                                            {editingProfile.users?.display_name || 'Worker'}
                                        </div>
                                    ) : (
                                        <Select onValueChange={setSelectedUserId} value={selectedUserId}>
                                            <SelectTrigger className="w-full bg-background border-muted/30">
                                                <SelectValue placeholder="Select registered user/employee" />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {availableUsers.map((user) => (
                                                    <SelectItem key={user.id} value={user.id}>
                                                        {user.display_name} ({user.role})
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    )
                                ) : (
                                    <Input
                                        id="worker_name"
                                        placeholder="Enter laborer/worker full name"
                                        value={externalName}
                                        onChange={(e) => setExternalName(e.target.value)}
                                        className="bg-background border-muted/30 focus-visible:ring-primary"
                                        disabled={!!editingProfile}
                                    />
                                )}
                            </div>

                            {/* Payment Type and Rate */}
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="payment_type">Payment Type</Label>
                                    <Select onValueChange={(val: any) => setPaymentType(val)} value={paymentType}>
                                        <SelectTrigger className="bg-background border-muted/30">
                                            <SelectValue placeholder="Select type" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="daily_wage">Daily Wage</SelectItem>
                                            <SelectItem value="monthly">Monthly Salary</SelectItem>
                                            <SelectItem value="hourly">Hourly Rate</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="rate">Rate (₹)</Label>
                                    <Input
                                        id="rate"
                                        type="number"
                                        placeholder="e.g. 500"
                                        value={rate}
                                        onChange={(e) => setRate(e.target.value)}
                                        className="bg-background border-muted/30 focus-visible:ring-primary"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="border-t border-muted/10 my-4" />

                            {/* Bank Details */}
                            <h3 className="text-sm font-semibold flex items-center gap-1.5 text-foreground">
                                <CreditCard className="h-4 w-4 text-muted-foreground" /> Bank Account Details
                            </h3>

                            <div className="space-y-3">
                                <div className="space-y-1.5">
                                    <Label htmlFor="bank_name" className="text-xs">Bank Name</Label>
                                    <Input
                                        id="bank_name"
                                        placeholder="State Bank of India, HDFC, etc."
                                        value={bankName}
                                        onChange={(e) => setBankName(e.target.value)}
                                        className="bg-background border-muted/30 focus-visible:ring-primary h-9"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-1.5">
                                        <Label htmlFor="account_number" className="text-xs">Account Number</Label>
                                        <Input
                                            id="account_number"
                                            placeholder="1234567890"
                                            value={accountNumber}
                                            onChange={(e) => setAccountNumber(e.target.value)}
                                            className="bg-background border-muted/30 focus-visible:ring-primary h-9"
                                        />
                                    </div>
                                    <div className="space-y-1.5">
                                        <Label htmlFor="ifsc_code" className="text-xs">IFSC Code</Label>
                                        <Input
                                            id="ifsc_code"
                                            placeholder="SBIN0001234"
                                            value={ifscCode}
                                            onChange={(e) => setIfscCode(e.target.value)}
                                            className="bg-background border-muted/30 focus-visible:ring-primary h-9"
                                        />
                                    </div>
                                </div>
                            </div>

                            <DialogFooter className="pt-4 border-t border-muted/10">
                                <Button type="button" variant="outline" onClick={() => setIsOpen(false)} className="border-muted/30 hover:bg-muted/10">
                                    Cancel
                                </Button>
                                <Button type="submit" disabled={isSubmitting} className="bg-primary hover:opacity-90">
                                    {isSubmitting ? (
                                        <>
                                            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Saving...
                                        </>
                                    ) : (
                                        'Save Profile'
                                    )}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>

            </div>
        </div>
    )
}
