'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Plus, Package, Search, AlertTriangle, ArrowDownToLine, ArrowUpToLine, Filter, Tag, Layers, DollarSign, Truck, History, Calendar, User } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/hooks/use-toast';
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
    const router = useRouter();
    const { supabase } = useSupabase();
    const { toast } = useToast();

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

    // Stock Update Dialog State
    const [isUpdateDialogOpen, setIsUpdateDialogOpen] = useState(false);
    const [selectedMaterial, setSelectedMaterial] = useState<ProjectMaterial | null>(null);
    const [updateType, setUpdateType] = useState<'add' | 'use'>('add');
    const [updateAmount, setUpdateAmount] = useState('');
    const [updatePurpose, setUpdatePurpose] = useState('');
    const [isUpdating, setIsUpdating] = useState(false);

    // History Dialog State
    const [isHistoryDialogOpen, setIsHistoryDialogOpen] = useState(false);
    const [materialLogs, setMaterialLogs] = useState<MaterialLog[]>([]);
    const [isLoadingLogs, setIsLoadingLogs] = useState(false);

    const fetchMaterials = async () => {
        if (!projectId) return;
        setIsLoading(true);
        try {
            const { data, error } = await supabase
                .from('project_materials')
                .select('*')
                .eq('project_id', projectId)
                .order('created_at', { ascending: false });

            if (error) throw error;
            setMaterials(data || []);
        } catch (error) {
            console.error('Error fetching materials:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchMaterials();
    }, [projectId, supabase]);

    const handleAddMaterial = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!projectId) return;
        setIsSubmitting(true);

        try {
            const { error } = await supabase
                .from('project_materials')
                .insert({
                    project_id: projectId,
                    name: newMaterial.name,
                    category: newMaterial.category,
                    quantity: Number(newMaterial.quantity) || 0,
                    min_quantity: Number(newMaterial.min_quantity) || 0,
                    unit: newMaterial.unit,
                    supplier: newMaterial.supplier,
                    cost: Number(newMaterial.cost) || 0
                });

            if (error) throw error;

            toast({
                title: "Material Added",
                description: "The material has been successfully added to the project.",
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
            toast({
                title: "Stock Updated",
                description: `${updateType === 'add' ? 'Added' : 'Used'} ${amount} ${selectedMaterial.unit}.`,
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
        <div className="flex flex-col h-full bg-secondary">
            <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                <Button variant="ghost" size="icon" onClick={() => router.back()}>
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div className="flex-1">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
                        Project Inventory
                    </h1>
                </div>
                <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
                    <DialogTrigger asChild>
                        <Button>
                            <Plus className="mr-2 h-4 w-4" />
                            Add Item
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
                        <form onSubmit={handleAddMaterial}>
                            <DialogHeader>
                                <DialogTitle className="flex items-center gap-2 text-xl">
                                    <Package className="h-5 w-5 text-primary" />
                                    Add New Item
                                </DialogTitle>
                                <DialogDescription>
                                    Add a new material to track in your project inventory.
                                </DialogDescription>
                            </DialogHeader>

                            <div className="grid gap-6 py-4">
                                {/* Section 1: Item Details */}
                                <div className="space-y-4">
                                    <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                                        <Tag className="h-4 w-4" />
                                        Item Details
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div className="space-y-2">
                                            <Label htmlFor="name">Item Name <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="name"
                                                value={newMaterial.name}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, name: e.target.value })}
                                                placeholder="e.g. Portland Cement"
                                                required
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <Label htmlFor="category">Category</Label>
                                            <Input
                                                id="category"
                                                value={newMaterial.category}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, category: e.target.value })}
                                                placeholder="e.g. Structural"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <Separator />

                                {/* Section 2: Inventory Control */}
                                <div className="space-y-4">
                                    <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                                        <Layers className="h-4 w-4" />
                                        Inventory Control
                                    </div>
                                    <div className="grid grid-cols-3 gap-4">
                                        <div className="space-y-2">
                                            <Label htmlFor="quantity">Initial Stock <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="quantity"
                                                type="number"
                                                value={newMaterial.quantity}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, quantity: e.target.value })}
                                                placeholder="0"
                                                required
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <Label htmlFor="min_quantity">Min Alert Qty <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="min_quantity"
                                                type="number"
                                                value={newMaterial.min_quantity}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, min_quantity: e.target.value })}
                                                placeholder="5"
                                                required
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <Label htmlFor="unit">Unit <span className="text-destructive">*</span></Label>
                                            <Input
                                                id="unit"
                                                value={newMaterial.unit}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, unit: e.target.value })}
                                                placeholder="e.g. bags"
                                                required
                                            />
                                        </div>
                                    </div>
                                </div>

                                <Separator />

                                {/* Section 3: Procurement */}
                                <div className="space-y-4">
                                    <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                                        <Truck className="h-4 w-4" />
                                        Procurement
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div className="space-y-2">
                                            <Label htmlFor="supplier">Supplier Name</Label>
                                            <Input
                                                id="supplier"
                                                value={newMaterial.supplier}
                                                onChange={(e) => setNewMaterial({ ...newMaterial, supplier: e.target.value })}
                                                placeholder="e.g. ABC Supplies"
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <Label htmlFor="cost">Cost per Unit</Label>
                                            <div className="relative">
                                                <DollarSign className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                                                <Input
                                                    id="cost"
                                                    type="number"
                                                    className="pl-8"
                                                    value={newMaterial.cost}
                                                    onChange={(e) => setNewMaterial({ ...newMaterial, cost: e.target.value })}
                                                    placeholder="0.00"
                                                />
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <DialogFooter>
                                <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto">
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
                    <Card className="flex flex-col items-center justify-center h-64 text-center p-6 bg-card/80 border-2 border-dashed">
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
                                    className={`overflow-hidden transition-all cursor-pointer hover:shadow-md ${isLowStock ? 'border-destructive/50 bg-destructive/5' : ''}`}
                                    onClick={() => openHistoryDialog(material)}
                                >
                                    <CardHeader className="pb-2">
                                        <div className="flex justify-between items-start">
                                            <CardTitle className="text-lg font-bold truncate pr-2">
                                                {material.name}
                                            </CardTitle>
                                            {isLowStock && (
                                                <Badge variant="destructive" className="shrink-0 animate-pulse">
                                                    <AlertTriangle className="h-3 w-3 mr-1" />
                                                    Low Stock
                                                </Badge>
                                            )}
                                        </div>
                                        <p className="text-sm text-muted-foreground capitalize">{material.category || 'Uncategorized'}</p>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="space-y-4">
                                            <div className="flex items-center justify-between p-3 bg-background/50 rounded-lg border">
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
                                                    {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(material.cost)} / unit
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
                <DialogContent className="sm:max-w-[425px]">
                    <DialogHeader>
                        <DialogTitle>{updateType === 'add' ? 'Add Stock' : 'Use Stock'}</DialogTitle>
                        <DialogDescription>
                            {updateType === 'add' ? 'Add new inventory to stock.' : 'Record material consumption.'}
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleStockUpdateSubmit} className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="amount">Quantity ({selectedMaterial?.unit})</Label>
                            <Input
                                id="amount"
                                type="number"
                                value={updateAmount}
                                onChange={(e) => setUpdateAmount(e.target.value)}
                                placeholder="0"
                                required
                                min="0.01"
                                step="0.01"
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="purpose">Purpose / Reason</Label>
                            <Input
                                id="purpose"
                                value={updatePurpose}
                                onChange={(e) => setUpdatePurpose(e.target.value)}
                                placeholder={updateType === 'add' ? "e.g. New shipment from supplier" : "e.g. Foundation work"}
                                required
                            />
                        </div>
                        <DialogFooter>
                            <Button type="submit" disabled={isUpdating}>
                                {isUpdating ? 'Updating...' : 'Confirm Update'}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* History Dialog */}
            <Dialog open={isHistoryDialogOpen} onOpenChange={setIsHistoryDialogOpen}>
                <DialogContent className="sm:max-w-[600px] max-h-[80vh] flex flex-col">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <History className="h-5 w-5" />
                            Stock History: {selectedMaterial?.name}
                        </DialogTitle>
                        <DialogDescription>
                            View the history of stock changes for this item.
                        </DialogDescription>
                    </DialogHeader>

                    <ScrollArea className="flex-1 pr-4 -mr-4">
                        {isLoadingLogs ? (
                            <div className="space-y-4 py-4">
                                {[1, 2, 3].map(i => <Skeleton key={i} className="h-16 w-full" />)}
                            </div>
                        ) : materialLogs.length === 0 ? (
                            <div className="text-center py-8 text-muted-foreground">
                                No history available for this item.
                            </div>
                        ) : (
                            <div className="space-y-4 py-4">
                                {materialLogs.map((log) => (
                                    <div key={log.id} className="flex items-start justify-between p-4 rounded-lg border bg-card">
                                        <div className="space-y-1">
                                            <p className="font-medium">{log.purpose}</p>
                                            <div className="flex items-center gap-4 text-xs text-muted-foreground">
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
                                        <div className={`text-sm font-bold ${log.change_amount > 0 ? 'text-green-600' : 'text-red-600'}`}>
                                            {log.change_amount > 0 ? '+' : ''}{log.change_amount}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </ScrollArea>
                </DialogContent>
            </Dialog>
        </div>
    );
}
