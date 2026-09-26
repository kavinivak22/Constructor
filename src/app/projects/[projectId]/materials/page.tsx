'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Plus, Package, Search, AlertTriangle, ArrowDownToLine, ArrowUpToLine, Filter, Tag, Layers, IndianRupee, Truck, History, Calendar, User, Edit3, Receipt } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/react-query';
import { Switch } from '@/components/ui/switch';
import { SupplierCombobox } from '@/components/materials/supplier-combobox';
import { EditMaterialDialog, EditableMaterial } from '@/components/materials/edit-material-dialog';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Label } from "@/components/ui/label";
import { updateMaterialStock, getMaterialLogs } from '@/app/actions/materials';
import { Separator } from '@/components/ui/separator';
import { format } from 'date-fns';
import { ScrollArea } from "@/components/ui/scroll-area";

type ProjectMaterial = {
    id: string;
    project_id: string;
    name: string;
    category: string;
    quantity: number;
    min_quantity: number;
    unit: string;
    supplier: string;
    cost: number;
    created_at: string;
};

type MaterialLog = {
    id: string;
    change_amount: number;
    purpose: string;
    created_at: string;
    users: {
        displayName: string;
        email: string;
    };
};

export default function ProjectMaterialsPage() {
    const { projectId } = useParams();
    const projectIdString = Array.isArray(projectId) ? projectId[0] : projectId;
    const router = useRouter();
    const { supabase } = useSupabase();
    const { toast } = useToast();
    const queryClient = useQueryClient();

    const [materials, setMaterials] = useState<ProjectMaterial[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [stockFilter, setStockFilter] = useState<string>('all');

    // Add Material Dialog State
    const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [newMaterial, setNewMaterial] = useState({
        name: '',
        category: '',
        quantity: '',
        min_quantity: '',
        unit: '',
        supplier: '',
        cost: ''
    });

    // Expense Logging State for Add Item
    const [logExpense, setLogExpense] = useState(true);
    const [totalPurchaseCost, setTotalPurchaseCost] = useState('');
    const [expenseDate, setExpenseDate] = useState(new Date().toISOString().split('T')[0]);
    const [paymentStatus, setPaymentStatus] = useState<'paid' | 'pending'>('paid');
    const [invoiceNumber, setInvoiceNumber] = useState('');

    // Edit Material Dialog State
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [materialToEdit, setMaterialToEdit] = useState<EditableMaterial | null>(null);

    // Stock Update Dialog State
    const [isUpdateDialogOpen, setIsUpdateDialogOpen] = useState(false);
    const [selectedMaterial, setSelectedMaterial] = useState<ProjectMaterial | null>(null);
    const [updateType, setUpdateType] = useState<'add' | 'use'>('add');
    const [updateAmount, setUpdateAmount] = useState('');
    const [updatePurpose, setUpdatePurpose] = useState('');
    const [isUpdating, setIsUpdating] = useState(false);

    // Restock Expense Logging State
    const [logRestockExpense, setLogRestockExpense] = useState(true);
    const [restockAmount, setRestockAmount] = useState('');
    const [restockPaymentStatus, setRestockPaymentStatus] = useState<'paid' | 'pending'>('paid');

    // History Dialog State
    const [isHistoryDialogOpen, setIsHistoryDialogOpen] = useState(false);
    const [materialLogs, setMaterialLogs] = useState<MaterialLog[]>([]);
    const [isLoadingLogs, setIsLoadingLogs] = useState(false);

    // Auto-compute total purchase cost when adding item
    useEffect(() => {
        const qty = Number(newMaterial.quantity) || 0;
        const c = Number(newMaterial.cost) || 0;
        if (qty > 0 && c > 0) {
            setTotalPurchaseCost((qty * c).toString());
        }
    }, [newMaterial.quantity, newMaterial.cost]);

    // Auto-compute restock amount when restocking
    useEffect(() => {
        if (selectedMaterial && updateType === 'add') {
            const qty = Number(updateAmount) || 0;
            const c = Number(selectedMaterial.cost) || 0;
            setRestockAmount(qty > 0 && c > 0 ? (qty * c).toString() : '');
        }
    }, [updateAmount, selectedMaterial, updateType]);

    const fetchMaterials = async () => {
        if (!projectIdString) return;
        setIsLoading(true);
        try {
            // Fetch project's site_id first
            const { data: project, error: projectError } = await supabase
                .from('projects')
                .select('site_id')
                .eq('id', projectIdString)
                .single();

            if (projectError || !project) throw projectError || new Error('Project not found');
            const siteId = project.site_id;

            const { data, error } = await supabase
                .from('materials')
                .select('*')
                .eq('site_id', siteId)
                .order('created_at', { ascending: false });

            if (error) throw error;

            const mappedData = (data || []).map((m: any) => ({
                id: m.id,
                project_id: projectIdString, // Map for UI compatibility
                site_id: m.site_id,
                name: m.name,
                category: m.category || '',
                quantity: Number(m.current_stock || 0),
                min_quantity: Number(m.minimum_stock_level || 0),
                unit: m.unit_of_measurement,
                supplier: m.supplier_name || '',
                cost: Number(m.unit_cost || 0),
                created_at: m.created_at
            }));

            setMaterials(mappedData);
        } catch (error) {
            console.error('Error fetching materials:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchMaterials();
    }, [projectIdString, supabase]);

    const openEditDialog = (mat: ProjectMaterial) => {
        setMaterialToEdit({
            id: mat.id,
            name: mat.name,
            category: mat.category,
            quantity: mat.quantity,
            min_quantity: mat.min_quantity,
            unit: mat.unit,
            supplier: mat.supplier,
            cost: mat.cost,
        });
        setIsEditDialogOpen(true);
    };

    const handleAddMaterial = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!projectIdString) return;
        setIsSubmitting(true);

        try {
            // Fetch project's site_id first
            const { data: project, error: projectError } = await supabase
                .from('projects')
                .select('site_id')
                .eq('id', projectIdString)
                .single();

            if (projectError || !project || !project.site_id) {
                throw projectError || new Error('Project or associated Site not found');
            }
            const siteId = project.site_id;

            const { data: insertedMat, error } = await supabase
                .from('materials')
                .insert({
                    site_id: siteId,
                    name: newMaterial.name,
                    category: newMaterial.category,
                    current_stock: Number(newMaterial.quantity) || 0,
                    minimum_stock_level: Number(newMaterial.min_quantity) || 0,
                    unit_of_measurement: newMaterial.unit,
                    supplier_name: newMaterial.supplier,
                    unit_cost: Number(newMaterial.cost) || 0
                })
                .select()
                .single();

            if (error) throw error;

            // Optional Expense Logging
            const expenseAmount = Number(totalPurchaseCost) || ((Number(newMaterial.quantity) || 0) * (Number(newMaterial.cost) || 0));
            let expenseLogged = false;

            if (logExpense && expenseAmount > 0) {
                const { data: { user } } = await supabase.auth.getUser();
                if (user) {
                    const { error: expError } = await supabase.from('expenses').insert({
                        project_id: projectIdString,
                        user_id: user.id,
                        created_by: user.id,
                        amount: expenseAmount,
                        category: 'materials',
                        description: `Initial purchase: ${newMaterial.quantity} ${newMaterial.unit} of ${newMaterial.name}${newMaterial.supplier ? ` from ${newMaterial.supplier}` : ''}`,
                        expense_date: expenseDate || new Date().toISOString().split('T')[0],
                        payment_status: paymentStatus,
                        receiver: newMaterial.supplier || null,
                        notes: invoiceNumber ? `Bill/Inv: ${invoiceNumber}` : null,
                        created_at: new Date().toISOString(),
                    });

                    if (expError) {
                        console.error('Error logging expense for material:', expError);
                    } else {
                        expenseLogged = true;
                        queryClient.invalidateQueries({
                            queryKey: queryKeys.projectExpenses(projectIdString),
                        });
                    }
                }
            }

            toast({
                title: "Material Added",
                description: `The material has been added to project inventory${expenseLogged ? ` & ₹${expenseAmount.toLocaleString('en-IN')} logged to Expenses.` : '.'}`,
            });

            setIsAddDialogOpen(false);
            setNewMaterial({
                name: '',
                category: '',
                quantity: '',
                min_quantity: '',
                unit: '',
                supplier: '',
                cost: ''
            });
            setTotalPurchaseCost('');
            setInvoiceNumber('');
            fetchMaterials();
        } catch (error: any) {
            console.error('Error adding material:', error);
            toast({
                title: "Error",
                description: "Failed to add material. " + (error.message || ""),
                variant: "destructive",
            });
        } finally {
            setIsSubmitting(false);
        }
    };

    const openUpdateDialog = (material: ProjectMaterial, type: 'add' | 'use') => {
        setSelectedMaterial(material);
        setUpdateType(type);
        setUpdateAmount('');
        setUpdatePurpose('');
        setLogRestockExpense(true);
        setRestockPaymentStatus('paid');
        setRestockAmount('');
        setIsUpdateDialogOpen(true);
    };

    const handleStockUpdateSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedMaterial) return;

        setIsUpdating(true);
        const amount = Number(updateAmount);
        if (isNaN(amount) || amount <= 0) {
            toast({ title: "Invalid Amount", description: "Please enter a valid positive number.", variant: "destructive" });
            setIsUpdating(false);
            return;
        }

        const change = updateType === 'add' ? amount : -amount;
        const currentQty = Number(selectedMaterial.quantity);
        const newQuantity = Math.max(0, currentQty + change);

        // Optimistic update
        setMaterials(prev => prev.map(m => m.id === selectedMaterial.id ? { ...m, quantity: newQuantity } : m));

        const result = await updateMaterialStock(selectedMaterial.id, newQuantity, updatePurpose);

        if (!result.success) {
            // Revert on failure
            setMaterials(prev => prev.map(m => m.id === selectedMaterial.id ? { ...m, quantity: currentQty } : m));
            toast({
                variant: "destructive",
                title: "Update failed",
                description: result.error
            });
        } else {
            // Record restock expense if enabled
            let restockExpenseLogged = false;
            if (updateType === 'add' && logRestockExpense) {
                const finalCost = Number(restockAmount) || (amount * Number(selectedMaterial.cost || 0));
                if (finalCost > 0) {
                    const { data: { user } } = await supabase.auth.getUser();
                    if (user) {
                        const { error: expError } = await supabase.from('expenses').insert({
                            project_id: projectIdString,
                            user_id: user.id,
                            created_by: user.id,
                            amount: finalCost,
                            category: 'materials',
                            description: `Restock: ${amount} ${selectedMaterial.unit} of ${selectedMaterial.name}${selectedMaterial.supplier ? ` from ${selectedMaterial.supplier}` : ''}${updatePurpose ? ` (${updatePurpose})` : ''}`,
                            expense_date: new Date().toISOString().split('T')[0],
                            payment_status: restockPaymentStatus,
                            receiver: selectedMaterial.supplier || null,
                            created_at: new Date().toISOString(),
                        });

                        if (expError) {
                            console.error('Error logging restock expense:', expError);
                        } else {
                            restockExpenseLogged = true;
                            queryClient.invalidateQueries({
                                queryKey: queryKeys.projectExpenses(projectIdString),
                            });
                        }
                    }
                }
            }

            toast({
                title: "Stock Updated",
                description: `${updateType === 'add' ? 'Added' : 'Used'} ${amount} ${selectedMaterial.unit}${restockExpenseLogged ? ` & logged to Expenses.` : '.'}`,
            });
            setIsUpdateDialogOpen(false);

            if (newQuantity <= selectedMaterial.min_quantity && currentQty > selectedMaterial.min_quantity) {
                toast({
                    variant: "destructive",
                    title: "Low Stock Alert",
                    description: `${selectedMaterial.name} is running low!`,
                });
            }
        }
        setIsUpdating(false);
    };

    const openHistoryDialog = async (material: ProjectMaterial) => {
        setSelectedMaterial(material);
        setIsHistoryDialogOpen(true);
        setIsLoadingLogs(true);
        const result = await getMaterialLogs(material.id);
        if (result.success) {
            setMaterialLogs((result.data || []) as MaterialLog[]);
        } else {
            toast({ title: "Error", description: "Failed to fetch logs.", variant: "destructive" });
        }
        setIsLoadingLogs(false);
    };

    const filteredMaterials = materials.filter(material => {
        const matchesSearch = material.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            material.category?.toLowerCase().includes(searchQuery.toLowerCase());

        let matchesFilter = true;
        if (stockFilter === 'low') {
            matchesFilter = material.quantity <= material.min_quantity;
        } else if (stockFilter === 'out') {
            matchesFilter = material.quantity === 0;
        } else if (stockFilter === 'in') {
            matchesFilter = material.quantity > material.min_quantity;
        }

        return matchesSearch && matchesFilter;
    });

    return (
        <div className="flex flex-col h-full bg-transparent">
            <header className="flex items-center justify-between gap-3 p-3.5 sm:p-4 md:px-6 shrink-0 glass sticky top-0 z-10 border-b border-white/10">
                <div className="flex items-center gap-2.5 min-w-0">
                    <Button variant="ghost" size="icon" onClick={() => router.back()} className="h-8 w-8 shrink-0 rounded-xl">
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                    <div className="min-w-0 flex-1">
                        <h1 className="text-base sm:text-2xl font-bold tracking-tight font-headline truncate">
                            Project Inventory
                        </h1>
                    </div>
                </div>
                <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
                    <DialogTrigger asChild>
                        <Button size="sm" className="h-8 sm:h-9 text-xs sm:text-sm rounded-xl font-semibold gap-1.5 shrink-0">
                            <Plus className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
                            Add Item
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="w-[95vw] max-w-[600px] max-h-[90vh] p-4 sm:p-6 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl overflow-y-auto">
                        <form onSubmit={handleAddMaterial}>
                            <DialogHeader className="space-y-1 pb-2">
                                <DialogTitle className="flex items-center gap-2 text-base sm:text-xl font-bold font-headline">
                                    <Package className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
                                    Add New Item
                                </DialogTitle>
                                <DialogDescription className="text-xs text-muted-foreground">
                                    Add a new material to track in your project inventory.
                                </DialogDescription>
                            </DialogHeader>

                            <div className="grid gap-4 sm:gap-6 py-3 sm:py-4">
                                {/* Section 1: Item Details */}
                                <div className="space-y-3">
                                    <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                                        <Tag className="h-3.5 w-3.5 text-primary" />
                                        Item Details
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                                        <div className="space-y-1.5">
                                            <Label htmlFor="name" className="text-xs sm:text-sm font-semibold">Item Name <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="name"
                                                value={newMaterial.name}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, name: e.target.value })}
                                                placeholder="e.g. Portland Cement"
                                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                required
                                            />
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label htmlFor="category" className="text-xs sm:text-sm font-semibold">Category</Label>
                                            <Input
                                                id="category"
                                                value={newMaterial.category}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, category: e.target.value })}
                                                placeholder="e.g. Structural"
                                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <Separator />

                                {/* Section 2: Inventory Control */}
                                <div className="space-y-3">
                                    <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                                        <Layers className="h-3.5 w-3.5 text-primary" />
                                        Inventory Control
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
                                        <div className="space-y-1.5">
                                            <Label htmlFor="quantity" className="text-xs sm:text-sm font-semibold">Initial Stock <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="quantity"
                                                type="number"
                                                value={newMaterial.quantity}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, quantity: e.target.value })}
                                                placeholder="0"
                                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                required
                                                step="any"
                                            />
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label htmlFor="min_quantity" className="text-xs sm:text-sm font-semibold">Min Alert Qty <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="min_quantity"
                                                type="number"
                                                value={newMaterial.min_quantity}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, min_quantity: e.target.value })}
                                                placeholder="5"
                                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                required
                                                step="any"
                                            />
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label htmlFor="unit" className="text-xs sm:text-sm font-semibold">Unit <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="unit"
                                                value={newMaterial.unit}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, unit: e.target.value })}
                                                placeholder="e.g. bags"
                                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                required
                                            />
                                        </div>
                                    </div>
                                </div>

                                <Separator />

                                {/* Section 3: Procurement & Supplier */}
                                <div className="space-y-3">
                                    <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                                        <Truck className="h-3.5 w-3.5 text-primary" />
                                        Procurement & Supplier
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                                        <div className="space-y-1.5">
                                            <Label className="text-xs sm:text-sm font-semibold">Supplier Name</Label>
                                            <SupplierCombobox
                                                value={newMaterial.supplier}
                                                onChange={(name) => setNewMaterial({ ...newMaterial, supplier: name })}
                                            />
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label htmlFor="cost" className="text-xs sm:text-sm font-semibold">Cost per Unit</Label>
                                            <div className="relative">
                                                <IndianRupee className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                                                <Input
                                                    id="cost"
                                                    type="number"
                                                    className="pl-8 h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                    value={newMaterial.cost}
                                                    onChange={(e) => setNewMaterial({ ...newMaterial, cost: e.target.value })}
                                                    placeholder="0.00"
                                                    step="any"
                                                />
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <Separator />

                                {/* Section 4: Purchase & Expense Logging */}
                                <div className="space-y-3">
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-2 text-xs sm:text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                                            <Receipt className="h-3.5 w-3.5 text-primary" />
                                            Expense Logging
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <Label htmlFor="log-expense-toggle" className="text-xs text-muted-foreground cursor-pointer">
                                                Record as Project Expense
                                            </Label>
                                            <Switch
                                                id="log-expense-toggle"
                                                checked={logExpense}
                                                onCheckedChange={setLogExpense}
                                            />
                                        </div>
                                    </div>

                                    {logExpense && (
                                        <div className="p-3 sm:p-4 rounded-xl border border-primary/20 bg-primary/5 space-y-3 animate-in fade-in-50 duration-200">
                                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                                <div className="space-y-1.5">
                                                    <Label htmlFor="total_cost" className="text-xs font-semibold">
                                                        Total Purchase Cost (₹)
                                                    </Label>
                                                    <div className="relative">
                                                        <IndianRupee className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                                                        <Input
                                                            id="total_cost"
                                                            type="number"
                                                            value={totalPurchaseCost}
                                                            onChange={(e) => setTotalPurchaseCost(e.target.value)}
                                                            placeholder="0.00"
                                                            step="any"
                                                            className="pl-8 h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                        />
                                                    </div>
                                                    <span className="text-[10px] text-muted-foreground">
                                                        Auto: {newMaterial.quantity || 0} {newMaterial.unit || 'units'} × ₹{newMaterial.cost || 0}
                                                    </span>
                                                </div>
                                                <div className="space-y-1.5">
                                                    <Label htmlFor="expense_date" className="text-xs font-semibold">
                                                        Purchase Date
                                                    </Label>
                                                    <Input
                                                        id="expense_date"
                                                        type="date"
                                                        value={expenseDate}
                                                        onChange={(e) => setExpenseDate(e.target.value)}
                                                        className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                    />
                                                </div>
                                            </div>

                                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                                <div className="space-y-1.5">
                                                    <Label className="text-xs font-semibold">Payment Status</Label>
                                                    <Select
                                                        value={paymentStatus}
                                                        onValueChange={(val: 'paid' | 'pending') => setPaymentStatus(val)}
                                                    >
                                                        <SelectTrigger className="h-9 text-xs sm:text-sm rounded-xl bg-background/50">
                                                            <SelectValue />
                                                        </SelectTrigger>
                                                        <SelectContent className="glass rounded-xl">
                                                            <SelectItem value="paid">Paid (Cash / Bank)</SelectItem>
                                                            <SelectItem value="pending">Pending (Credit / Due)</SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                </div>
                                                <div className="space-y-1.5">
                                                    <Label htmlFor="invoice_no" className="text-xs font-semibold">
                                                        Invoice / Bill # (Optional)
                                                    </Label>
                                                    <Input
                                                        id="invoice_no"
                                                        placeholder="e.g. INV-2026-081"
                                                        value={invoiceNumber}
                                                        onChange={(e) => setInvoiceNumber(e.target.value)}
                                                        className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <DialogFooter className="pt-2">
                                <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto h-9 text-xs sm:text-sm rounded-xl font-semibold">
                                    {isSubmitting ? 'Adding Item...' : 'Add Item'}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </header>

            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6">
                <div className="flex flex-row gap-2">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                        <Input
                            placeholder="Search..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-10"
                        />
                    </div>
                    <Select value={stockFilter} onValueChange={setStockFilter}>
                        <SelectTrigger className="w-[130px] sm:w-[180px] shrink-0">
                            <Filter className="mr-2 h-4 w-4" />
                            <SelectValue placeholder="Filter" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">All Items</SelectItem>
                            <SelectItem value="low">Low Stock</SelectItem>
                            <SelectItem value="out">Out of Stock</SelectItem>
                            <SelectItem value="in">In Stock</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                {isLoading ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {[1, 2, 3, 4, 5, 6].map((i) => (
                            <Skeleton key={i} className="h-40 w-full" />
                        ))}
                    </div>
                ) : filteredMaterials.length === 0 ? (
                    <Card className="glass-card flex flex-col items-center justify-center h-64 text-center p-6 border-2 border-dashed">
                        <div className="h-12 w-12 rounded-full bg-muted flex items-center justify-center mb-4">
                            <Package className="h-6 w-6 text-muted-foreground" />
                        </div>
                        <h3 className="text-xl font-bold font-headline">No Materials Found</h3>
                        <p className="max-w-sm mt-2 text-muted-foreground">
                            {materials.length > 0
                                ? "No materials match your search."
                                : "No materials added to this project yet."}
                        </p>
                        {materials.length === 0 && (
                            <Button variant="outline" className="mt-4" onClick={() => setIsAddDialogOpen(true)}>
                                Add Your First Item
                            </Button>
                        )}
                    </Card>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filteredMaterials.map((material) => {
                            const isLowStock = material.quantity <= material.min_quantity;
                            return (
                                <Card
                                    key={material.id}
                                    className={`glass-card overflow-hidden transition-all cursor-pointer ${isLowStock ? 'border-destructive/50 bg-destructive/10' : ''}`}
                                    onClick={() => openHistoryDialog(material)}
                                >
                                    <CardHeader className="pb-2">
                                        <div className="flex justify-between items-start">
                                            <CardTitle className="text-lg font-bold truncate pr-2">
                                                {material.name}
                                            </CardTitle>
                                            <div className="flex items-center gap-1.5 shrink-0">
                                                {isLowStock && (
                                                    <Badge variant="destructive" className="shrink-0 animate-pulse">
                                                        <AlertTriangle className="h-3 w-3 mr-1" />
                                                        Low Stock
                                                    </Badge>
                                                )}
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-7 w-7 rounded-lg text-muted-foreground hover:text-foreground hover:bg-white/10"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        openEditDialog(material);
                                                    }}
                                                    title="Edit Material"
                                                >
                                                    <Edit3 className="h-3.5 w-3.5" />
                                                </Button>
                                            </div>
                                        </div>
                                        <p className="text-sm text-muted-foreground capitalize">{material.category || 'Uncategorized'}</p>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="space-y-4">
                                            <div className="flex items-center justify-between p-3 bg-white/10 dark:bg-black/10 rounded-lg border border-white/10 dark:border-black/10">
                                                <div className="text-center">
                                                    <p className="text-xs text-muted-foreground uppercase tracking-wider">Stock</p>
                                                    <p className={`text-xl font-bold ${isLowStock ? 'text-destructive' : ''}`}>
                                                        {material.quantity} <span className="text-sm font-normal text-muted-foreground">{material.unit}</span>
                                                    </p>
                                                </div>
                                                <div className="h-8 w-px bg-border" />
                                                <div className="text-center">
                                                    <p className="text-xs text-muted-foreground uppercase tracking-wider">Min Qty</p>
                                                    <p className="text-lg font-medium">
                                                        {material.min_quantity} <span className="text-xs text-muted-foreground">{material.unit}</span>
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    className="flex-1 px-2"
                                                    onClick={() => openUpdateDialog(material, 'add')}
                                                >
                                                    <ArrowUpToLine className="h-4 w-4 mr-1.5 text-green-500" />
                                                    Add <span className="hidden xs:inline ml-1">Stock</span>
                                                </Button>
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    className="flex-1 px-2"
                                                    onClick={() => openUpdateDialog(material, 'use')}
                                                    disabled={material.quantity <= 0}
                                                >
                                                    <ArrowDownToLine className="h-4 w-4 mr-1.5 text-red-500" />
                                                    Use <span className="hidden xs:inline ml-1">Stock</span>
                                                </Button>
                                            </div>

                                            <div className="text-xs text-muted-foreground flex justify-between items-center pt-2 border-t">
                                                <span className="truncate max-w-[120px]" title={material.supplier}>
                                                    {material.supplier ? `Supplier: ${material.supplier}` : 'No Supplier'}
                                                </span>
                                                <span className="whitespace-nowrap">
                                                    {new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(material.cost)} / unit
                                                </span>
                                            </div>
                                        </div>
                                    </CardContent>
                                </Card>
                            );
                        })}
                    </div>
                )}
            </main>

            {/* Stock Update Dialog */}
            <Dialog open={isUpdateDialogOpen} onOpenChange={setIsUpdateDialogOpen}>
                <DialogContent className="w-[92vw] max-w-[425px] p-4 sm:p-6 rounded-2xl glass border border-white/20 dark:border-white/10 shadow-2xl">
                    <DialogHeader className="space-y-1">
                        <DialogTitle className="text-base sm:text-lg font-bold font-headline">{updateType === 'add' ? 'Add Stock' : 'Use Stock'}</DialogTitle>
                        <DialogDescription className="text-xs text-muted-foreground">
                            {updateType === 'add' ? 'Add new inventory to stock.' : 'Record material consumption.'}
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleStockUpdateSubmit} className="space-y-3.5 py-3">
                        <div className="space-y-1.5">
                            <Label htmlFor="amount" className="text-xs sm:text-sm font-semibold">Quantity ({selectedMaterial?.unit})</Label>
                            <Input
                                id="amount"
                                type="number"
                                value={updateAmount}
                                onChange={(e) => setUpdateAmount(e.target.value)}
                                placeholder="0"
                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                required
                                min="0.01"
                                step="any"
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label htmlFor="purpose" className="text-xs sm:text-sm font-semibold">Purpose / Reason</Label>
                            <Input
                                id="purpose"
                                value={updatePurpose}
                                onChange={(e) => setUpdatePurpose(e.target.value)}
                                placeholder={updateType === 'add' ? "e.g. New shipment from supplier" : "e.g. Foundation work"}
                                className="h-9 text-xs sm:text-sm rounded-xl bg-background/50"
                                required
                            />
                        </div>

                        {/* Optional Restock Expense Logging */}
                        {updateType === 'add' && (
                            <div className="p-3 rounded-xl border border-primary/20 bg-primary/5 space-y-2.5 animate-in fade-in-50">
                                <div className="flex items-center justify-between">
                                    <Label htmlFor="restock-expense-toggle" className="text-xs font-semibold flex items-center gap-1.5 cursor-pointer">
                                        <Receipt className="h-3.5 w-3.5 text-primary" />
                                        Record Purchase as Expense
                                    </Label>
                                    <Switch
                                        id="restock-expense-toggle"
                                        checked={logRestockExpense}
                                        onCheckedChange={setLogRestockExpense}
                                    />
                                </div>
                                {logRestockExpense && (
                                    <div className="grid grid-cols-2 gap-2 pt-1">
                                        <div className="space-y-1">
                                            <Label className="text-[11px] font-medium text-muted-foreground">Amount (₹)</Label>
                                            <div className="relative">
                                                <IndianRupee className="absolute left-2 top-2 h-3.5 w-3.5 text-muted-foreground" />
                                                <Input
                                                    type="number"
                                                    value={restockAmount}
                                                    onChange={(e) => setRestockAmount(e.target.value)}
                                                    placeholder="0.00"
                                                    step="any"
                                                    className="pl-7 h-8 text-xs rounded-lg bg-background/50"
                                                />
                                            </div>
                                        </div>
                                        <div className="space-y-1">
                                            <Label className="text-[11px] font-medium text-muted-foreground">Payment Status</Label>
                                            <Select
                                                value={restockPaymentStatus}
                                                onValueChange={(val: 'paid' | 'pending') => setRestockPaymentStatus(val)}
                                            >
                                                <SelectTrigger className="h-8 text-xs rounded-lg bg-background/50">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent className="glass rounded-xl">
                                                    <SelectItem value="paid">Paid</SelectItem>
                                                    <SelectItem value="pending">Pending</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        <DialogFooter className="pt-2">
                            <Button type="submit" disabled={isUpdating} className="w-full sm:w-auto h-9 text-xs sm:text-sm rounded-xl font-semibold">
                                {isUpdating ? 'Updating...' : 'Confirm Update'}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* History Dialog */}
            <Dialog open={isHistoryDialogOpen} onOpenChange={setIsHistoryDialogOpen}>
                <DialogContent className="w-[95vw] max-w-[600px] max-h-[85vh] p-4 sm:p-6 rounded-2xl glass border border-white/20 dark:border-white/10 flex flex-col shadow-2xl">
                    <DialogHeader className="space-y-1 pb-2">
                        <DialogTitle className="flex items-center gap-2 text-base sm:text-lg font-bold font-headline">
                            <History className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
                            Stock History: {selectedMaterial?.name}
                        </DialogTitle>
                        <DialogDescription className="text-xs text-muted-foreground">
                            View the history of stock changes for this item.
                        </DialogDescription>
                    </DialogHeader>

                    <ScrollArea className="flex-1 pr-4 -mr-4">
                        {isLoadingLogs ? (
                            <div className="space-y-4 py-4">
                                {[1, 2, 3].map(i => <Skeleton key={i} className="h-16 w-full" />)}
                            </div>
                        ) : materialLogs.length === 0 ? (
                            <div className="text-center py-8 text-muted-foreground text-xs">
                                No history available for this item.
                            </div>
                        ) : (
                            <div className="space-y-3 py-3">
                                {materialLogs.map((log) => (
                                    <div key={log.id} className="flex items-start justify-between p-3 sm:p-4 rounded-xl border border-white/10 dark:border-white/5 bg-background/40">
                                        <div className="space-y-1">
                                            <p className="font-semibold text-xs sm:text-sm text-foreground">{log.purpose}</p>
                                            <div className="flex flex-wrap items-center gap-3 text-[10px] sm:text-xs text-muted-foreground">
                                                <span className="flex items-center gap-1">
                                                    <Calendar className="h-3 w-3" />
                                                    {format(new Date(log.created_at), 'MMM d, yyyy h:mm a')}
                                                </span>
                                                <span className="flex items-center gap-1">
                                                    <User className="h-3 w-3" />
                                                    {log.users?.displayName || 'Unknown User'}
                                                </span>
                                            </div>
                                        </div>
                                        <div className={`text-xs sm:text-sm font-mono font-bold ${log.change_amount > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                                            {log.change_amount > 0 ? '+' : ''}{log.change_amount}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </ScrollArea>
                </DialogContent>
            </Dialog>

            {/* Edit Material Dialog */}
            <EditMaterialDialog
                open={isEditDialogOpen}
                onOpenChange={setIsEditDialogOpen}
                material={materialToEdit}
                onSuccess={fetchMaterials}
            />
        </div>
    );
}
