'use client';

import { useState, useEffect, useMemo } from 'react';
import { useProjects } from '@/hooks/queries';
import { useSupabase } from '@/supabase/provider';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { 
  PlusCircle, 
  ShoppingCart, 
  ChevronDown, 
  ChevronUp, 
  Trash2, 
  Check, 
  FileText, 
  Calendar, 
  Truck, 
  User as UserIcon, 
  ClipboardList,
  Plus
} from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { getPurchaseOrders, createPurchaseOrder, approvePurchaseOrder } from '@/app/actions/purchase-orders';
import { getInventoryMaterials } from '@/app/actions/materials';
import { PurchaseOrder, Material } from '@/lib/data';

interface NewPOItem {
  materialId?: string | null;
  materialName: string;
  quantity: number;
  unitPrice: number;
}

export default function PurchaseOrdersPage() {
  const { data: projects = [], isLoading: isLoadingProjects } = useProjects();
  const { toast } = useToast();
  const { supabase, user } = useSupabase();
  const [currentUserProfile, setCurrentUserProfile] = useState<{ id: string; role: string } | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;
      const { data } = await supabase
        .from('users')
        .select('id, role')
        .eq('id', user.id)
        .single();
      if (data) {
        setCurrentUserProfile(data);
      }
    };
    fetchProfile();
  }, [user, supabase]);

  const isAdminOrManager = currentUserProfile?.role === 'admin' || currentUserProfile?.role === 'manager';

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([]);
  const [projectMaterials, setProjectMaterials] = useState<Material[]>([]);
  const [isLoadingPOs, setIsLoadingPOs] = useState(false);
  const [expandedPoId, setExpandedPoId] = useState<string | null>(null);

  // Create PO Dialog state
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [supplierName, setSupplierName] = useState('');
  const [supplierContact, setSupplierContact] = useState('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [specialInstructions, setSpecialInstructions] = useState('');
  const [items, setItems] = useState<NewPOItem[]>([
    { materialId: null, materialName: '', quantity: 1, unitPrice: 0 }
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Set default project on load
  useEffect(() => {
    if (projects && projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  // Load POs when selected project changes
  const loadPOs = async (projId: string) => {
    setIsLoadingPOs(true);
    try {
      const res = await getPurchaseOrders(projId);
      if (res.success && res.data) {
        setPurchaseOrders(res.data);
      } else {
        toast({
          title: 'Error',
          description: res.error || 'Failed to load purchase orders',
          variant: 'destructive',
        });
      }
    } catch (error: any) {
      console.error(error);
    } finally {
      setIsLoadingPOs(false);
    }
  };

  // Load project materials for the creation dropdown
  const loadProjectMaterials = async () => {
    try {
      const res = await getInventoryMaterials();
      if (res.success && res.data) {
        // filter materials by selected project
        const filtered = (res.data as Material[]).filter(m => m.projectId === selectedProjectId);
        setProjectMaterials(filtered);
      }
    } catch (error) {
      console.error('Failed to load project materials:', error);
    }
  };

  useEffect(() => {
    if (selectedProjectId) {
      loadPOs(selectedProjectId);
      loadProjectMaterials();
    }
  }, [selectedProjectId]);

  const handleProjectChange = (projectId: string) => {
    setSelectedProjectId(projectId);
    setExpandedPoId(null);
  };

  const selectedProject = useMemo(() => {
    return projects?.find(p => p.id === selectedProjectId) ?? null;
  }, [projects, selectedProjectId]);

  // Expand / collapse PO items list
  const toggleExpandPo = (id: string) => {
    setExpandedPoId(expandedPoId === id ? null : id);
  };

  // Handle PO approval
  const handleApprovePo = async (poId: string) => {
    try {
      const res = await approvePurchaseOrder(poId);
      if (res.success) {
        toast({
          title: 'Purchase Order Approved',
          description: 'Inventory levels updated successfully.',
        });
        if (selectedProjectId) {
          loadPOs(selectedProjectId);
        }
      } else {
        toast({
          title: 'Approval Failed',
          description: res.error || 'Could not approve purchase order.',
          variant: 'destructive',
        });
      }
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'An error occurred during approval.',
        variant: 'destructive',
      });
    }
  };

  // Manage create PO form item rows
  const handleAddItemRow = () => {
    setItems([...items, { materialId: null, materialName: '', quantity: 1, unitPrice: 0 }]);
  };

  const handleRemoveItemRow = (index: number) => {
    if (items.length === 1) return;
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemFieldChange = (index: number, field: keyof NewPOItem, value: any) => {
    const newItems = [...items];
    if (field === 'materialId') {
      if (value === 'custom') {
        newItems[index] = {
          ...newItems[index],
          materialId: null,
          materialName: '',
          unitPrice: 0,
        };
      } else {
        const selectedMat = projectMaterials.find(m => m.id === value);
        newItems[index] = {
          ...newItems[index],
          materialId: value,
          materialName: selectedMat ? selectedMat.name : '',
          unitPrice: selectedMat ? selectedMat.costPerUnit : 0,
        };
      }
    } else {
      newItems[index] = {
        ...newItems[index],
        [field]: value,
      };
    }
    setItems(newItems);
  };

  const grandTotal = useMemo(() => {
    return items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
  }, [items]);

  const resetForm = () => {
    setSupplierName('');
    setSupplierContact('');
    setDeliveryDate('');
    setSpecialInstructions('');
    setItems([{ materialId: null, materialName: '', quantity: 1, unitPrice: 0 }]);
  };

  const handleCreatePO = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProjectId) return;
    if (!supplierName.trim()) {
      toast({
        title: 'Validation Error',
        description: 'Supplier name is required.',
        variant: 'destructive',
      });
      return;
    }

    // validate items
    const invalidItem = items.find(item => !item.materialName.trim() || item.quantity <= 0 || item.unitPrice < 0);
    if (invalidItem) {
      toast({
        title: 'Validation Error',
        description: 'All items must have a name, quantity > 0, and non-negative price.',
        variant: 'destructive',
      });
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await createPurchaseOrder({
        projectId: selectedProjectId,
        supplierName,
        supplierContact,
        deliveryDate,
        specialInstructions,
        items,
      });

      if (res.success) {
        toast({
          title: 'Purchase Order Created',
          description: `PO ${res.data.po_number} has been created successfully.`,
        });
        setIsCreateDialogOpen(false);
        resetForm();
        loadPOs(selectedProjectId);
      } else {
        toast({
          title: 'Error',
          description: res.error || 'Failed to create purchase order',
          variant: 'destructive',
        });
      }
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.message || 'An unexpected error occurred.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper for Status Badge styling
  const getStatusBadge = (status: PurchaseOrder['status']) => {
    switch (status) {
      case 'approved':
        return <Badge className="bg-emerald-100 text-emerald-800 hover:bg-emerald-100 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-800">Approved</Badge>;
      case 'completed':
        return <Badge className="bg-blue-100 text-blue-800 hover:bg-blue-100 border border-blue-200 dark:bg-blue-950/40 dark:text-blue-400 dark:border-blue-800">Completed</Badge>;
      case 'rejected':
        return <Badge className="bg-rose-100 text-rose-800 hover:bg-rose-100 border border-rose-200 dark:bg-rose-950/40 dark:text-rose-400 dark:border-rose-800">Rejected</Badge>;
      default:
        return <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-100 border border-amber-200 dark:bg-amber-950/40 dark:text-amber-400 dark:border-amber-800">Pending</Badge>;
    }
  };

  return (
    <div className="flex flex-col h-full bg-transparent">
      <header className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 md:px-6 shrink-0 glass sticky top-0 z-10">
        <div className="flex items-center gap-2">
          <ShoppingCart className="h-6 w-6 text-primary" />
          <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
            Purchase Orders
          </h1>
        </div>
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full md:w-auto">
          {isLoadingProjects ? (
            <Skeleton className="h-10 w-full sm:w-48" />
          ) : (
            projects && projects.length > 0 && (
              <Select onValueChange={handleProjectChange} value={selectedProjectId ?? undefined}>
                <SelectTrigger className="w-full sm:w-[200px]">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map(project => (
                    <SelectItem key={project.id} value={project.id}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )
          )}
          {selectedProjectId && isAdminOrManager && (
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button className="w-full sm:w-auto shadow-sm">
                  <PlusCircle className="mr-2 h-4 w-4" />
                  Create PO
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
                <form onSubmit={handleCreatePO}>
                  <DialogHeader>
                    <DialogTitle>Create Purchase Order</DialogTitle>
                    <DialogDescription>
                      Issue a new purchase order for project <span className="font-semibold text-foreground">{selectedProject?.name}</span>.
                    </DialogDescription>
                  </DialogHeader>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 py-4">
                    <div className="space-y-2">
                      <Label htmlFor="supplier">Supplier Name <span className="text-destructive">*</span></Label>
                      <Input
                        id="supplier"
                        placeholder="e.g. BuildMart Ltd"
                        value={supplierName}
                        onChange={e => setSupplierName(e.target.value)}
                        required
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="contact">Supplier Contact</Label>
                      <Input
                        id="contact"
                        placeholder="e.g. sales@buildmart.com"
                        value={supplierContact}
                        onChange={e => setSupplierContact(e.target.value)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="delivery-date">Expected Delivery Date</Label>
                      <Input
                        id="delivery-date"
                        type="date"
                        value={deliveryDate}
                        onChange={e => setDeliveryDate(e.target.value)}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="instructions">Special Instructions</Label>
                      <Input
                        id="instructions"
                        placeholder="e.g. Deliver to rear loading dock"
                        value={specialInstructions}
                        onChange={e => setSpecialInstructions(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="border-t pt-4">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="font-semibold text-sm flex items-center gap-2"><ClipboardList className="h-4 w-4" /> Line Items</h3>
                      <Button type="button" variant="outline" size="sm" onClick={handleAddItemRow}>
                        <Plus className="h-4 w-4 mr-1" /> Add Item
                      </Button>
                    </div>

                    <div className="space-y-3">
                      {items.map((item, index) => (
                        <div key={index} className="flex flex-col md:flex-row gap-3 items-stretch md:items-end border p-3 rounded-lg bg-muted/30">
                          <div className="flex-1 space-y-1">
                            <Label className="text-[11px] text-muted-foreground">Select Material</Label>
                            <Select 
                              onValueChange={(val) => handleItemFieldChange(index, 'materialId', val)}
                              value={item.materialId || 'custom'}
                            >
                              <SelectTrigger className="bg-background">
                                <SelectValue placeholder="Custom item..." />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="custom">Custom item / Enter manually</SelectItem>
                                {projectMaterials.map(mat => (
                                  <SelectItem key={mat.id} value={mat.id}>{mat.name} ({mat.unit})</SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>

                          <div className="flex-1 space-y-1">
                            <Label className="text-[11px] text-muted-foreground">Item Name <span className="text-destructive">*</span></Label>
                            <Input
                              placeholder="Item name"
                              value={item.materialName}
                              onChange={e => handleItemFieldChange(index, 'materialName', e.target.value)}
                              disabled={!!item.materialId}
                              className="bg-background"
                              required
                            />
                          </div>

                          <div className="w-full md:w-24 space-y-1">
                            <Label className="text-[11px] text-muted-foreground">Quantity <span className="text-destructive">*</span></Label>
                            <Input
                              type="number"
                              min="0.01"
                              step="any"
                              value={item.quantity}
                              onChange={e => handleItemFieldChange(index, 'quantity', parseFloat(e.target.value) || 0)}
                              className="bg-background"
                              required
                            />
                          </div>

                          <div className="w-full md:w-28 space-y-1">
                            <Label className="text-[11px] text-muted-foreground">Unit Cost (₹) <span className="text-destructive">*</span></Label>
                            <Input
                              type="number"
                              min="0"
                              step="any"
                              value={item.unitPrice}
                              onChange={e => handleItemFieldChange(index, 'unitPrice', parseFloat(e.target.value) || 0)}
                              className="bg-background"
                              required
                            />
                          </div>

                          <div className="w-full md:w-28 space-y-1 text-right md:pb-2 self-center md:self-end">
                            <span className="text-xs text-muted-foreground block md:hidden">Total: </span>
                            <span className="font-semibold text-sm">₹{(item.quantity * item.unitPrice).toFixed(2)}</span>
                          </div>

                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onClick={() => handleRemoveItemRow(index)}
                            disabled={items.length === 1}
                            className="text-destructive hover:text-destructive hover:bg-destructive/10 h-10 w-10 shrink-0 self-end"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="flex items-center justify-between border-t mt-6 pt-4">
                    <span className="text-muted-foreground text-sm">Grand Total:</span>
                    <span className="text-xl font-bold text-primary">₹{grandTotal.toFixed(2)}</span>
                  </div>

                  <DialogFooter className="mt-6">
                    <Button type="button" variant="outline" onClick={() => { setIsCreateDialogOpen(false); resetForm(); }}>
                      Cancel
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                      Create Purchase Order
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          )}
        </div>
      </header>

      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {!selectedProjectId && !isLoadingProjects && (
          <div className="flex flex-col items-center justify-center h-64 text-center border-2 border-dashed glass-card p-6">
            <ShoppingCart className="h-10 w-10 text-muted-foreground mb-3" />
            <h2 className="text-lg font-bold font-headline">No Project Selected</h2>
            <p className="max-w-sm mt-1 text-sm text-muted-foreground">
              {projects && projects.length > 0 ? 'Please select a project to view its purchase orders.' : 'Create a project to get started.'}
            </p>
          </div>
        )}

        {selectedProjectId && (
          <div className="space-y-6">
            <Card className="glass-card">
              <CardHeader className="pb-4">
                <CardTitle className="text-lg flex items-center gap-2"><FileText className="h-5 w-5 text-muted-foreground" /> Orders List</CardTitle>
                <CardDescription>
                  List of purchase orders requested for <span className="font-medium text-foreground">{selectedProject?.name}</span>.
                </CardDescription>
              </CardHeader>
              <CardContent className="p-0">
                {isLoadingPOs ? (
                  <div className="p-6 space-y-4">
                    {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-16 w-full" />)}
                  </div>
                ) : purchaseOrders.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <ShoppingCart className="h-12 w-12 text-muted-foreground/40 mb-3" />
                    <h3 className="font-bold text-sm">No Purchase Orders</h3>
                    <p className="max-w-sm mt-1 text-xs text-muted-foreground">
                      No purchase orders have been created for this project yet.
                    </p>
                  </div>
                ) : (
                  <div className="divide-y divide-white/10 dark:divide-black/10 border-t border-white/10 dark:border-black/10">
                    {purchaseOrders.map((po) => {
                      const isExpanded = expandedPoId === po.id;
                      return (
                        <div key={po.id} className="transition-colors hover:bg-muted/5">
                          <div 
                            className="flex items-center justify-between p-4 cursor-pointer gap-4"
                            onClick={() => toggleExpandPo(po.id)}
                          >
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 flex-1">
                              <div>
                                <span className="text-xs text-muted-foreground block">PO Number</span>
                                <span className="font-semibold text-sm hover:underline">{po.po_number}</span>
                              </div>
                              <div>
                                <span className="text-xs text-muted-foreground block">Supplier</span>
                                <span className="font-medium text-sm">{po.supplier_name}</span>
                              </div>
                              <div>
                                <span className="text-xs text-muted-foreground block">Total Amount</span>
                                <span className="font-bold text-sm">₹{Number(po.total_amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                              </div>
                              <div>
                                <span className="text-xs text-muted-foreground block">Status</span>
                                <div className="mt-0.5">{getStatusBadge(po.status)}</div>
                              </div>
                            </div>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
                              {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                            </Button>
                          </div>

                          {isExpanded && (
                            <div className="p-4 bg-white/5 dark:bg-black/20 border-t border-white/10 dark:border-black/10 space-y-4 animate-in fade-in-50 duration-200">
                              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-sm">
                                <div className="space-y-1.5">
                                  <div className="flex items-center text-muted-foreground gap-1.5"><Calendar className="h-4 w-4" /> <span>Delivery Details</span></div>
                                  <p className="font-medium">
                                    {po.delivery_date ? new Date(po.delivery_date).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'No delivery date specified'}
                                  </p>
                                </div>
                                <div className="space-y-1.5">
                                  <div className="flex items-center text-muted-foreground gap-1.5"><UserIcon className="h-4 w-4" /> <span>Procurement Agents</span></div>
                                  <p className="text-xs">
                                    <span className="text-muted-foreground">Created by:</span> {po.creator?.display_name || 'System / Auto'}
                                  </p>
                                  {po.status === 'approved' && (
                                    <p className="text-xs">
                                      <span className="text-muted-foreground">Approved by:</span> {po.approver?.display_name || 'System / Auto'}
                                    </p>
                                  )}
                                </div>
                                <div className="space-y-1.5">
                                  <div className="flex items-center text-muted-foreground gap-1.5"><Truck className="h-4 w-4" /> <span>Special Instructions</span></div>
                                  <p className="italic text-muted-foreground text-xs">
                                    {po.special_instructions || 'None'}
                                  </p>
                                </div>
                              </div>

                              <div className="border border-white/10 dark:border-black/10 rounded-lg overflow-hidden bg-white/5 dark:bg-black/10 mt-4">
                                <Table>
                                  <TableHeader>
                                    <TableRow className="bg-muted/30">
                                      <TableHead className="h-9">Item Name</TableHead>
                                      <TableHead className="h-9 text-right">Quantity</TableHead>
                                      <TableHead className="h-9 text-right">Unit Price</TableHead>
                                      <TableHead className="h-9 text-right">Total Price</TableHead>
                                    </TableRow>
                                  </TableHeader>
                                  <TableBody>
                                    {po.purchase_order_items?.map((item) => (
                                      <TableRow key={item.id}>
                                        <TableCell className="py-2.5 font-medium">{item.material_name}</TableCell>
                                        <TableCell className="py-2.5 text-right">{item.quantity}</TableCell>
                                        <TableCell className="py-2.5 text-right">₹{Number(item.unit_price).toFixed(2)}</TableCell>
                                        <TableCell className="py-2.5 text-right font-semibold">₹{Number(item.total_price).toFixed(2)}</TableCell>
                                      </TableRow>
                                    ))}
                                  </TableBody>
                                </Table>
                              </div>

                              {po.status === 'pending' && isAdminOrManager && (
                                <div className="flex justify-end pt-2">
                                  <Button 
                                    className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm flex items-center gap-1.5"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleApprovePo(po.id);
                                    }}
                                  >
                                    <Check className="h-4 w-4" /> Approve Purchase Order
                                  </Button>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )}
      </main>
    </div>
  );
}
