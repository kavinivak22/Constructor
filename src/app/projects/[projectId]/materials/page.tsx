'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useSupabase } from '@/supabase/provider';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Plus, Package, Search, Filter } from 'lucide-react';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
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
import { Label } from "@/components/ui/label";

type ProjectMaterial = {
    id: string;
    project_id: string;
    name: string;
    category: string;
    quantity: number;
    unit: string;
    status: 'needed' | 'ordered' | 'delivered';
    supplier: string;
    cost: number;
    created_at: string;
};

export default function ProjectMaterialsPage() {
    const { projectId } = useParams();
    const router = useRouter();
    const { supabase } = useSupabase();
    const { toast } = useToast();

    const [materials, setMaterials] = useState<ProjectMaterial[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [statusFilter, setStatusFilter] = useState<string>('all');
    const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Form state
    const [newMaterial, setNewMaterial] = useState({
        name: '',
        category: '',
        quantity: '',
        unit: '',
        status: 'needed',
        supplier: '',
        cost: ''
    });

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
            // Don't show error toast on initial load if table doesn't exist yet
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
                    unit: newMaterial.unit,
                    status: newMaterial.status,
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
                unit: '',
                status: 'needed',
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

    const filteredMaterials = materials.filter(material => {
        const matchesSearch = material.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            material.category?.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesStatus = statusFilter === 'all' || material.status === statusFilter;
        return matchesSearch && matchesStatus;
    });

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'delivered': return 'default'; // primary color
            case 'ordered': return 'secondary';
            case 'needed': return 'destructive';
            default: return 'outline';
        }
    };

    return (
        <div className="flex flex-col h-full bg-secondary">
            <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
                <Button variant="ghost" size="icon" onClick={() => router.back()}>
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div className="flex-1">
                    <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
                        Project Materials
                    </h1>
                </div>
                <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
                    <DialogTrigger asChild>
                        <Button>
                            <Plus className="mr-2 h-4 w-4" />
                            Add Material
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[500px]">
                        <form onSubmit={handleAddMaterial}>
                            <DialogHeader>
                                <DialogTitle>Add New Material</DialogTitle>
                                <DialogDescription>
                                    Add a new material requirement to this project.
                                </DialogDescription>
                            </DialogHeader>
                            <div className="grid gap-4 py-4">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="name">Name</Label>
                                        <Input
                                            id="name"
                                            value={newMaterial.name}
                                            onChange={(e) => setNewMaterial({ ...newMaterial, name: e.target.value })}
                                            placeholder="e.g. Cement"
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
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="quantity">Quantity</Label>
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
                                        <Label htmlFor="unit">Unit</Label>
                                        <Input
                                            id="unit"
                                            value={newMaterial.unit}
                                            onChange={(e) => setNewMaterial({ ...newMaterial, unit: e.target.value })}
                                            placeholder="e.g. bags"
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="cost">Est. Cost</Label>
                                        <Input
                                            id="cost"
                                            type="number"
                                            value={newMaterial.cost}
                                            onChange={(e) => setNewMaterial({ ...newMaterial, cost: e.target.value })}
                                            placeholder="0.00"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label htmlFor="supplier">Supplier</Label>
                                        <Input
                                            id="supplier"
                                            value={newMaterial.supplier}
                                            onChange={(e) => setNewMaterial({ ...newMaterial, supplier: e.target.value })}
                                            placeholder="Supplier Name"
                                        />
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="status">Status</Label>
                                    <Select
                                        value={newMaterial.status}
                                        onValueChange={(value: any) => setNewMaterial({ ...newMaterial, status: value })}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select status" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="needed">Needed</SelectItem>
                                            <SelectItem value="ordered">Ordered</SelectItem>
                                            <SelectItem value="delivered">Delivered</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>
                            <DialogFooter>
                                <Button type="submit" disabled={isSubmitting}>
                                    {isSubmitting ? 'Adding...' : 'Add Material'}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </header>

            <main className="flex-1 p-4 overflow-y-auto md:p-6 space-y-6">
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                        <Input
                            placeholder="Search materials..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-10"
                        />
                    </div>
                    <Select value={statusFilter} onValueChange={setStatusFilter}>
                        <SelectTrigger className="w-full sm:w-[180px]">
                            <Filter className="mr-2 h-4 w-4" />
                            <SelectValue placeholder="Filter by status" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">All Statuses</SelectItem>
                            <SelectItem value="needed">Needed</SelectItem>
                            <SelectItem value="ordered">Ordered</SelectItem>
                            <SelectItem value="delivered">Delivered</SelectItem>
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
                                ? "No materials match your current filters."
                                : "No materials added to this project yet."}
                        </p>
                        {materials.length === 0 && (
                            <Button variant="outline" className="mt-4" onClick={() => setIsAddDialogOpen(true)}>
                                Add Your First Material
                            </Button>
                        )}
                    </Card>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filteredMaterials.map((material) => (
                            <Card key={material.id} className="overflow-hidden">
                                <CardHeader className="pb-2">
                                    <div className="flex justify-between items-start">
                                        <CardTitle className="text-lg font-bold truncate pr-2">
                                            {material.name}
                                        </CardTitle>
                                        <Badge variant={getStatusColor(material.status) as any} className="capitalize shrink-0">
                                            {material.status}
                                        </Badge>
                                    </div>
                                    <p className="text-sm text-muted-foreground capitalize">{material.category || 'Uncategorized'}</p>
                                </CardHeader>
                                <CardContent>
                                    <div className="space-y-2 text-sm">
                                        <div className="flex justify-between py-1 border-b">
                                            <span className="text-muted-foreground">Quantity</span>
                                            <span className="font-medium">{material.quantity} {material.unit}</span>
                                        </div>
                                        <div className="flex justify-between py-1 border-b">
                                            <span className="text-muted-foreground">Est. Cost</span>
                                            <span className="font-medium">
                                                {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(material.cost)}
                                            </span>
                                        </div>
                                        <div className="flex justify-between py-1">
                                            <span className="text-muted-foreground">Supplier</span>
                                            <span className="font-medium truncate max-w-[150px]">{material.supplier || '-'}</span>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}
            </main>
        </div>
    );
}
